package ru.yandex.market.pers.grade.web.grade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.pers.grade.cache.AchievementCacher;
import ru.yandex.market.pers.grade.client.dto.UserAchievementDto;
import ru.yandex.market.pers.grade.client.dto.grade.ModelGradeResponseDto;
import ru.yandex.market.pers.grade.client.dto.grade.ShopGradeResponseDto;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.client.model.achievements.UserAchievement;
import ru.yandex.market.pers.grade.core.achievements.AchievementEntityType;
import ru.yandex.market.pers.grade.core.achievements.AchievementEventType;
import ru.yandex.market.pers.grade.core.achievements.AchievementType;
import ru.yandex.market.pers.grade.core.moderation.GradeModeratorModificationProxy;
import ru.yandex.market.pers.grade.core.service.AchievementsResolverService;
import ru.yandex.market.pers.grade.core.ugc.PhotoService;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author korolyov
 * 22.09.17
 */
public class AchievementsControllerTest extends AchievementsBaseControllerTest {

    @Autowired
    private GradeModeratorModificationProxy gradeModeratorModificationProxy;

    @Autowired
    private AchievementsResolverService achievementsResolverService;

    @Autowired
    private AchievementCacher achievementCacher;

    @Autowired
    private PhotoService photoService;

    private ModelGradeResponseDto achievementsOnCreateModelGrade(String body, List<UserAchievement> expectedAchievements) throws Exception {
        expectedAchievements.sort(Comparator.comparingInt(UserAchievement::getAchievementId));
        ModelGradeResponseDto modelGradeResponseDto = objectMapper.readValue(
            addModelGrade("UID", String.valueOf(FAKE_USER), body, status().is2xxSuccessful()),
            ModelGradeResponseDto.class);
        List<UserAchievement> userAchievements = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(expectedAchievements, userAchievements);
        return modelGradeResponseDto;
    }

    private List<UserAchievement> getUserAchievementsByGradeId(long userId, long gradeId) throws Exception {
        return objectMapper.readValue(
            invokeAndRetrieveResponse(
                get("/api/achievements/UID/" + userId + "/gradeId/" + gradeId)
                    .accept(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()),
            new TypeReference<List<UserAchievement>>() {
            });
    }

    @Test
    public void getAllUserAchievementsOnCreateModelGradeTest() throws Exception {
        ModelGradeResponseDto modelGradeResponseDto = objectMapper.readValue(
            addModelGrade("UID", String.valueOf(FAKE_USER), ADD_MODEL_GRADE_FOR_ACHIEVEMENTS_BODY, status().is2xxSuccessful()),
            ModelGradeResponseDto.class);

        List<UserAchievementDto> allUserAchievements = getAllUserAchievements(FAKE_USER);
        long achievementsCount = allUserAchievements.stream()
            .filter(userAchievementDto -> userAchievementDto.getAchievementDistance() == 0)
            .count();
        Assert.assertEquals(3, achievementsCount);
    }

    @Test
    public void testGetAllUserAchievementsWithDifferentGrades() throws Exception {
        Map<String, Integer> expectedUserAchievementDto = Stream.of(new Object[][]{
                {"???????????? ??????????????", 47}, {"???????? ????????????????????", 49}, {"?????????????? ????????????", 46}, {"???????????????? ??????????????????????", 49},
                {"???????????????????? ????????????", 4}, {"????????????????????", 0}, {"?????? ????????????????", 2}, {"???????????? ??????????????", 0}, {"??????????????", 9},
                {"???????????? ??????????????????????????", 0}, {"???????????? ????????????????????", 2}, {"???????????????? ????????????", 7}, {"?????????????????? ??????????????", 6},
                {"????????????????????????", 0}, {"??????????????????????", 2}, {"????????????????????????", 9}, {"???????? ?? ??????????????", 0}
        }).collect(Collectors.toMap(data -> (String) data[0], data -> (Integer) data[1]));

        List<UserAchievement> expectedUserAchievements = Arrays.asList(
                new UserAchievement(AchievementType.DEBUT.value(), 4, 0),
                new UserAchievement(AchievementType.PAPARAZZI.value(), 1, 0),
                new UserAchievement(AchievementType.FIRST_BIRD.value(), 4, 0),
                new UserAchievement(AchievementType.TOVAROVED.value(), 3, 0),
                new UserAchievement(AchievementType.REVIZOR.value(), 1, 0)
        );

        addModelGrade("UID", String.valueOf(FAKE_USER), ADD_MODEL_GRADE_BODY_WITHOUT_PHOTO, status().is2xxSuccessful());
        addModelGrade("UID", String.valueOf(FAKE_USER),
                ADD_MODEL_GRADE_BODY_WITHOUT_PHOTO.replace(String.valueOf(MODEL_ID), String.valueOf(ANOTHER_MODEL_ID)),
                status().is2xxSuccessful());
        addModelGrade("UID", String.valueOf(FAKE_USER), ADD_MODEL_GRADE_FOR_ACHIEVEMENTS_BODY, status().is2xxSuccessful());
        addShopGrade("UID", String.valueOf(FAKE_USER), ADD_SHOP_GRADE_FOR_ACHIEVEMENTS_BODY, status().is2xxSuccessful());

        List<UserAchievementDto> allUserAchievements = getAllUserAchievements(FAKE_USER);

        assertEquals(17, allUserAchievements.size());
        assertEquals(expectedUserAchievementDto, allUserAchievements.stream().collect(Collectors
                .toMap(x -> x.getAchievementLevel().getAchievementLevelName(), UserAchievementDto::getAchievementDistance)));
        assertEquals(expectedUserAchievements, allUserAchievements.stream()
                .map(UserAchievementDto::getUserAchievement).distinct().collect(Collectors.toList()));
    }

    @Test
    public void testGetAllUserAchievementsWithReceivedDate() throws Exception {
        List<UserAchievement> expectedUserAchievements = Arrays.asList(
            new UserAchievement(AchievementType.DEBUT.value(), 2, 2),
            new UserAchievement(AchievementType.PAPARAZZI.value(), 0, 0),
            new UserAchievement(AchievementType.FIRST_BIRD.value(), 2, 2),
            new UserAchievement(AchievementType.TOVAROVED.value(), 2, 1),
            new UserAchievement(AchievementType.REVIZOR.value(), 0, 1)
        );

        ModelGradeResponseDto grade1 = objectMapper.readValue(
            addModelGrade("UID", String.valueOf(FAKE_USER), ADD_MODEL_GRADE_BODY_WITHOUT_PHOTO, status().is2xxSuccessful()),
            ModelGradeResponseDto.class);
        ModelGradeResponseDto grade2 = objectMapper.readValue(addModelGrade("UID", String.valueOf(FAKE_USER),
            ADD_MODEL_GRADE_BODY_WITHOUT_PHOTO.replace(String.valueOf(MODEL_ID), String.valueOf(ANOTHER_MODEL_ID)),
            status().is2xxSuccessful()), ModelGradeResponseDto.class);
        ModelGradeResponseDto grade3 = objectMapper.readValue(addModelGrade("UID", String.valueOf(FAKE_USER), ADD_MODEL_GRADE_FOR_ACHIEVEMENTS_BODY, status().is2xxSuccessful()), ModelGradeResponseDto.class);
        ShopGradeResponseDto grade4 = objectMapper.readValue(addShopGrade("UID", String.valueOf(FAKE_USER), ADD_SHOP_GRADE_FOR_ACHIEVEMENTS_BODY, status().is2xxSuccessful()), ShopGradeResponseDto.class);


        moderateGrade(grade3.getId(), ModState.APPROVED);
        moderateGrade(grade4.getId(), ModState.APPROVED);

        achievementsModerationAndCacheClean();

        List<UserAchievementDto> allUserAchievements = getAllUserAchievements(FAKE_USER);

        assertEquals(17, allUserAchievements.size());
        assertEquals(expectedUserAchievements, allUserAchievements.stream()
            .map(UserAchievementDto::getUserAchievement).distinct().collect(Collectors.toList()));

        long expectedReceivedAchievementsCountByDate = 2;
        long receivedAchievementsByDate = allUserAchievements.stream()
            .filter(userAchievementDto -> userAchievementDto.getReceivedDate() != null)
            .count();
        assertEquals(expectedReceivedAchievementsCountByDate, receivedAchievementsByDate);

        long expectedReceivedAchievementsCountByDistance = 4;
        long receivedAchievementsByDistance = allUserAchievements.stream()
            .filter(userAchievementDto -> userAchievementDto.getAchievementDistance() == 0)
            .count();
        assertEquals(expectedReceivedAchievementsCountByDistance, receivedAchievementsByDistance);
    }

    @Test
    public void testGetAllUserAchievementsWithoutAnyGrades() throws Exception {
        Map<String, Integer> expectedUserAchievementDto = Stream.of(new Object[][]{
                {"???????????? ??????????????", 50}, {"???????? ????????????????????", 50}, {"?????????????? ????????????", 50}, {"???????????????? ??????????????????????", 50},
                {"???????????????????? ????????????", 5}, {"????????????????????", 1}, {"?????? ????????????????", 5}, {"???????????? ??????????????", 1}, {"??????????????", 10},
                {"???????????? ??????????????????????????", 3}, {"???????????? ????????????????????", 3}, {"???????????????? ????????????", 10}, {"?????????????????? ??????????????", 10},
                {"????????????????????????", 1}, {"??????????????????????", 3}, {"????????????????????????", 10}, {"???????? ?? ??????????????", 3}
        }).collect(Collectors.toMap(data -> (String) data[0], data -> (Integer) data[1]));

        List<UserAchievementDto> allUserAchievements = getAllUserAchievements(FAKE_USER);

        assertEquals(17, allUserAchievements.size());
        assertEquals(expectedUserAchievementDto, allUserAchievements.stream().collect(Collectors
                .toMap(x -> x.getAchievementLevel().getAchievementLevelName(), UserAchievementDto::getAchievementDistance)));
    }

    @Test
    public void achievementsOnCreateModelGradeTest() throws Exception {
        List<UserAchievement> expectedAchievements = new ArrayList<>();
        expectedAchievements.add(new UserAchievement(AchievementType.DEBUT.value(), 1, 0));
        expectedAchievements.add(new UserAchievement(AchievementType.PAPARAZZI.value(), 1, 0));
        expectedAchievements.add(new UserAchievement(AchievementType.FIRST_BIRD.value(), 1, 0));
        expectedAchievements.add(new UserAchievement(AchievementType.TOVAROVED.value(), 1, 0));
        achievementsOnCreateModelGrade(
            ADD_MODEL_GRADE_FOR_ACHIEVEMENTS_BODY,
            expectedAchievements);
    }

    @Test
    public void achievementsOnCreateModelGradeWithoutTextTest() throws Exception {
        List<UserAchievement> expectedAchievements = Collections.emptyList();
        achievementsOnCreateModelGrade(
            ADD_MODEL_GRADE_WITHOUT_TEXT_BODY,
            expectedAchievements);
    }

    @Test
    public void achievementsOnChangeModelGradeTest() throws Exception {
        addModelGrade("UID", String.valueOf(FAKE_USER), ADD_MODEL_GRADE_FOR_ACHIEVEMENTS_BODY, status().is2xxSuccessful());
        List<UserAchievement> achievementsAfterCreate = getUserAchievements(FAKE_USER);
        addModelGrade(
            "UID",
            String.valueOf(FAKE_USER),
            ADD_MODEL_GRADE_FOR_ACHIEVEMENTS_BODY.replace(PRO_MODEL_GRADE, NEW_PRO_MODEL_GRADE),
            status().is2xxSuccessful());
        List<UserAchievement> achievementsAfterChange = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(achievementsAfterCreate, achievementsAfterChange);
    }

    @Test
    public void achievementsOnRetryCreateModelGradeTest() throws Exception {
        addModelGrade("UID", String.valueOf(FAKE_USER), ADD_MODEL_GRADE_FOR_ACHIEVEMENTS_BODY, status().is2xxSuccessful());
        List<UserAchievement> achievementsAfterCreate = getUserAchievements(FAKE_USER);
        addModelGrade(
            "UID",
            String.valueOf(FAKE_USER),
            ADD_MODEL_GRADE_FOR_ACHIEVEMENTS_BODY,
            status().is2xxSuccessful());
        List<UserAchievement> achievementsAfterChange = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(achievementsAfterCreate, achievementsAfterChange);
    }

    @Test
    public void achievementsSecondLevelModelTest() throws Exception {
        List<UserAchievement> expectedAchievements = Arrays.asList(
            new UserAchievement(AchievementType.DEBUT.value(), 1, 0),
            new UserAchievement(AchievementType.FIRST_BIRD.value(), 1, 0),
            new UserAchievement(AchievementType.PAPARAZZI.value(), 1, 0),
            new UserAchievement(AchievementType.TOVAROVED.value(), 1, 0));
        achievementsOnCreateModelGrade(
            ADD_MODEL_GRADE_FOR_ACHIEVEMENTS_BODY,
            expectedAchievements);
        addModelGrade("UID", String.valueOf(FAKE_USER),
            ADD_MODEL_GRADE_BODY.replace(String.valueOf(MODEL_ID), String.valueOf(ANOTHER_MODEL_ID)),
            status().is2xxSuccessful());
        expectedAchievements = Arrays.asList(
            new UserAchievement(AchievementType.DEBUT.value(), 2, 0),
            new UserAchievement(AchievementType.FIRST_BIRD.value(), 2, 0),
            new UserAchievement(AchievementType.PAPARAZZI.value(), 2, 0),
            new UserAchievement(AchievementType.TOVAROVED.value(), 2, 0));
        expectedAchievements.sort(Comparator.comparingInt(UserAchievement::getAchievementId));
        List<UserAchievement> achievementTypes = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(expectedAchievements, achievementTypes);
    }

    @Test
    public void achievementsOnCreateModelGradeWithoutPhotoTest() throws Exception {
        List<UserAchievement> expectedAchievements = Arrays.asList(
            new UserAchievement(AchievementType.DEBUT.value(), 1, 0),
            new UserAchievement(AchievementType.FIRST_BIRD.value(), 1, 0),
            new UserAchievement(AchievementType.TOVAROVED.value(), 1, 0));
        achievementsOnCreateModelGrade(
            ADD_MODEL_GRADE_BODY_WITHOUT_PHOTO.replace(String.valueOf(MODEL_ID), String.valueOf(FIRST_BIRD_MODEL_ID)),
            expectedAchievements);
    }

    private long achievementsOnCreateShopGrade(String body, List<UserAchievement> expectedAchievements) throws Exception {
        expectedAchievements.sort(Comparator.comparingInt(UserAchievement::getAchievementId));
        ShopGradeResponseDto shopGradeResponseDto = objectMapper.readValue(
            addShopGrade(
                "UID",
                String.valueOf(FAKE_USER),
                body,
                status().is2xxSuccessful()),
            ShopGradeResponseDto.class);
        List<UserAchievement> achievements = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(expectedAchievements, achievements);
        return shopGradeResponseDto.getId();
    }

    @Test
    public void achievementsOnCreateShopGradeWithoutTextTest() throws Exception {
        List<UserAchievement> expectedAchievements = Collections.emptyList();
        achievementsOnCreateShopGrade(
            ADD_SHOP_GRADE_WITHOUT_TEXT_BODY,
            expectedAchievements);
    }

    @Test
    public void achievementsOnCreateShopGradeTest() throws Exception {
        List<UserAchievement> expectedAchievements = Arrays.asList(
            new UserAchievement(AchievementType.DEBUT.value(), 1, 0),
            new UserAchievement(AchievementType.FIRST_BIRD.value(), 1, 0),
            new UserAchievement(AchievementType.REVIZOR.value(), 1, 0));
        achievementsOnCreateShopGrade(
            ADD_SHOP_GRADE_FOR_ACHIEVEMENTS_BODY,
            expectedAchievements);
    }

    @Test
    public void achievementsOnChangeShopGradeTest() throws Exception {
        addShopGrade("UID", String.valueOf(FAKE_USER), ADD_SHOP_GRADE_FOR_ACHIEVEMENTS_BODY, status().is2xxSuccessful());
        List<UserAchievement> achievementsAfterCreate = getUserAchievements(FAKE_USER);
        addShopGrade(
            "UID",
            String.valueOf(FAKE_USER),
            ADD_SHOP_GRADE_FOR_ACHIEVEMENTS_BODY.replace(PRO_SHOP_GRADE, NEW_PRO_SHOP_GRADE),
            status().is2xxSuccessful());
        List<UserAchievement> achievementsAfterChange = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(achievementsAfterCreate, achievementsAfterChange);
    }

    @Test
    public void achievementsOnRetryCreateShopGradeTest() throws Exception {
        addShopGrade("UID", String.valueOf(FAKE_USER), ADD_SHOP_GRADE_FOR_ACHIEVEMENTS_BODY, status().is2xxSuccessful());
        List<UserAchievement> achievementsAfterCreate = getUserAchievements(FAKE_USER);
        addShopGrade(
            "UID",
            String.valueOf(FAKE_USER),
            ADD_SHOP_GRADE_FOR_ACHIEVEMENTS_BODY,
            status().is2xxSuccessful());
        List<UserAchievement> achievementsAfterChange = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(achievementsAfterCreate, achievementsAfterChange);
    }

    @Test
    public void achievementsSecondLevelShopTest() throws Exception {
        List<UserAchievement> expectedAchievements = Arrays.asList(
            new UserAchievement(AchievementType.DEBUT.value(), 1, 0),
            new UserAchievement(AchievementType.FIRST_BIRD.value(), 1, 0),
            new UserAchievement(AchievementType.REVIZOR.value(), 1, 0));
        achievementsOnCreateShopGrade(
            ADD_SHOP_GRADE_FOR_ACHIEVEMENTS_BODY,
            expectedAchievements);
        addShopGrade("UID", String.valueOf(FAKE_USER),
            ADD_SHOP_GRADE_BODY.replace(String.valueOf(SHOP_ID), String.valueOf(ANOTHER_SHOP_ID)),
            status().is2xxSuccessful());
        expectedAchievements = Arrays.asList(
            new UserAchievement(AchievementType.DEBUT.value(), 2, 0),
            new UserAchievement(AchievementType.FIRST_BIRD.value(), 2, 0),
            new UserAchievement(AchievementType.REVIZOR.value(), 2, 0));
        List<UserAchievement> achievements = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(expectedAchievements, achievements);
    }

    @Test
    public void achievementsByGradeTest() throws Exception {
        List<UserAchievement> expectedAchievements = Arrays.asList(
            new UserAchievement(AchievementType.DEBUT.value(), 1, 0),
            new UserAchievement(AchievementType.FIRST_BIRD.value(), 1, 0));
        expectedAchievements.sort(Comparator.comparingInt(UserAchievement::getAchievementId).reversed());
        ShopGradeResponseDto shopGradeResponseDto = objectMapper.readValue(
            addShopGrade(
                "UID",
                String.valueOf(FAKE_USER),
                ADD_SHOP_GRADE_FOR_ACHIEVEMENTS_BODY,
                status().is2xxSuccessful()),
            ShopGradeResponseDto.class);
        List<UserAchievement> achievements = getUserAchievementsByGradeId(FAKE_USER, shopGradeResponseDto.getId());
        assertListAchievementsEqualsWithOrder(expectedAchievements, achievements, true);
    }

    private void moderateGradeWithPhoto(long gradeId, ModState gradeModState, ModState photoModState) {
        gradeModeratorModificationProxy.moderateGradeReplies(Collections.singletonList(gradeId), Collections.emptyList(), -1L, gradeModState);
        photoService.moderatePhotosByGradeIds(Collections.singletonList(gradeId), -1L, photoModState);
    }

    private void achievementsModerationAndCacheClean() throws Exception {
        achievementsResolverService.resolvePendingSync();
        achievementsResolverService.resolveRejectedSync();
        achievementCacher.cleanForUser(FAKE_USER);
    }

    private void markGrade(long gradeId, boolean spam) {
        dbGradeAdminService.setGradeState(List.of(gradeId), spam);
    }

    private List<UserAchievement> allPending() {
        List<UserAchievement> achievements = new ArrayList<>();
        achievements.add(new UserAchievement(AchievementType.DEBUT.value(), 1, 0));
        achievements.add(new UserAchievement(AchievementType.PAPARAZZI.value(), 1, 0));
        achievements.add(new UserAchievement(AchievementType.FIRST_BIRD.value(), 1, 0));
        achievements.add(new UserAchievement(AchievementType.TOVAROVED.value(), 1, 0));
        return achievements;
    }

    private List<UserAchievement> allConfirmed() {
        List<UserAchievement> achievements = new ArrayList<>();
        achievements.add(new UserAchievement(AchievementType.DEBUT.value(), 0, 1));
        achievements.add(new UserAchievement(AchievementType.PAPARAZZI.value(), 0, 1));
        achievements.add(new UserAchievement(AchievementType.FIRST_BIRD.value(), 0, 1));
        achievements.add(new UserAchievement(AchievementType.TOVAROVED.value(), 0, 1));
        return achievements;
    }

    private List<UserAchievement> allConfirmedExceptPaparazzi() {
        List<UserAchievement> achievements = new ArrayList<>();
        achievements.add(new UserAchievement(AchievementType.DEBUT.value(), 0, 1));
        achievements.add(new UserAchievement(AchievementType.PAPARAZZI.value(), 0, 0));
        achievements.add(new UserAchievement(AchievementType.FIRST_BIRD.value(), 0, 1));
        achievements.add(new UserAchievement(AchievementType.TOVAROVED.value(), 0, 1));
        return achievements;
    }

    private List<UserAchievement> allRejected() {
        List<UserAchievement> achievements = new ArrayList<>();
        achievements.add(new UserAchievement(AchievementType.DEBUT.value(), 0, 0));
        achievements.add(new UserAchievement(AchievementType.PAPARAZZI.value(), 0, 0));
        achievements.add(new UserAchievement(AchievementType.FIRST_BIRD.value(), 0, 0));
        achievements.add(new UserAchievement(AchievementType.TOVAROVED.value(), 0, 0));
        return achievements;
    }

    /**
     * ?????????? ???????????? ?????????????????? - ?????????????????? ????????????????????.
     */
    @Test
    public void achievementsOnSuccessfullGradeModeration() throws Exception {
        List<UserAchievement> expectedAchievements = allPending();
        ModelGradeResponseDto modelGradeResponseDto = achievementsOnCreateModelGrade(
            ADD_MODEL_GRADE_FOR_ACHIEVEMENTS_BODY,
            expectedAchievements);
        long gradeId = modelGradeResponseDto.getId();
        moderateGradeWithPhoto(gradeId, ModState.APPROVED, ModState.APPROVED);
        achievementsModerationAndCacheClean();
        expectedAchievements = allConfirmed();
        List<UserAchievement> userAchievements = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(expectedAchievements, userAchievements);
    }

    /**
     * ?????????? ???? ???????????? ??????????????????
     */
    @Test
    public void achievementsOnUnsuccessfullGradeModeration() throws Exception {
        List<UserAchievement> expectedAchievements = allPending();
        ModelGradeResponseDto modelGradeResponseDto = achievementsOnCreateModelGrade(
            ADD_MODEL_GRADE_FOR_ACHIEVEMENTS_BODY,
            expectedAchievements);
        long gradeId = modelGradeResponseDto.getId();
        moderateGradeWithPhoto(gradeId, ModState.REJECTED, ModState.APPROVED);
        achievementsModerationAndCacheClean();
        expectedAchievements = allRejected();
        List<UserAchievement> userAchievements = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(expectedAchievements, userAchievements);
    }

    /**
     * ?????????? ???????????? ??????????????????, ???????? - ??????
     */
    @Test
    public void achievementsOnUnsuccessfullPhotoModeration() throws Exception {
        List<UserAchievement> expectedAchievements = allPending();
        ModelGradeResponseDto modelGradeResponseDto = achievementsOnCreateModelGrade(
            ADD_MODEL_GRADE_FOR_ACHIEVEMENTS_BODY,
            expectedAchievements);
        long gradeId = modelGradeResponseDto.getId();
        moderateGradeWithPhoto(gradeId, ModState.APPROVED, ModState.REJECTED);
        achievementsModerationAndCacheClean();
        expectedAchievements.clear();
        expectedAchievements.add(new UserAchievement(AchievementType.DEBUT.value(), 0, 1));
        expectedAchievements.add(new UserAchievement(AchievementType.PAPARAZZI.value(), 0, 0));
        expectedAchievements.add(new UserAchievement(AchievementType.FIRST_BIRD.value(), 0, 1));
        expectedAchievements.add(new UserAchievement(AchievementType.TOVAROVED.value(), 0, 1));
        List<UserAchievement> userAchievements = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(expectedAchievements, userAchievements);
    }

    /**
     * ???????????????????????? ?????????????? ?????????????????????????? ?????????? = ???????????? ???? ????????????????
     */
    @Test
    public void achievementsOnDeleteModeratedGrade() throws Exception {
        List<UserAchievement> expectedAchievements = allPending();
        ModelGradeResponseDto modelGradeResponseDto = achievementsOnCreateModelGrade(
            ADD_MODEL_GRADE_FOR_ACHIEVEMENTS_BODY,
            expectedAchievements);
        long gradeId = modelGradeResponseDto.getId();
        moderateGradeWithPhoto(gradeId, ModState.APPROVED, ModState.APPROVED);

        achievementsModerationAndCacheClean();

        expectedAchievements = allConfirmed();
        List<UserAchievement> userAchievements = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(expectedAchievements, userAchievements);

        deleteGrade(gradeId);
        achievementsModerationAndCacheClean();

        userAchievements = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(expectedAchievements, userAchievements);
    }

    /**
     * ?????????????? ???? ?????????????????????? ?????????? = ???????????? ?????????????????? ( ???????? ????????????????, ?????????? ?????????????? )
     */
    @Test
    public void achievementsOnDeleteUnmoderatedGrade() throws Exception {
        List<UserAchievement> expectedAchievements = allPending();
        ModelGradeResponseDto modelGradeResponseDto = achievementsOnCreateModelGrade(
            ADD_MODEL_GRADE_FOR_ACHIEVEMENTS_BODY,
            expectedAchievements);
        long gradeId = modelGradeResponseDto.getId();
        deleteGrade(gradeId);

        achievementsModerationAndCacheClean();

        expectedAchievements = allRejected();
        List<UserAchievement> userAchievements = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(expectedAchievements, userAchievements);
    }

    /**
     * ?????????? ??????????????, ?????????? ???????????????????? ??????????????????????, ?????????? ?????????????????? = ????????????
     */
    @Test
    public void achievementsOnSuccessfulPostModerationGrade() throws Exception {
        List<UserAchievement> expectedAchievements = allPending();
        ModelGradeResponseDto modelGradeResponseDto = achievementsOnCreateModelGrade(
            ADD_MODEL_GRADE_FOR_ACHIEVEMENTS_BODY,
            expectedAchievements);
        long gradeId = modelGradeResponseDto.getId();
        markGrade(gradeId, true);

        achievementsModerationAndCacheClean();

        expectedAchievements = allRejected();
        List<UserAchievement> userAchievements = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(expectedAchievements, userAchievements);
        markGrade(gradeId, false);
        moderateGradeWithPhoto(gradeId, ModState.APPROVED, ModState.APPROVED);
        achievementsModerationAndCacheClean();
        expectedAchievements = allConfirmed();
        userAchievements = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(expectedAchievements, userAchievements);
    }

    /**
     * ?????? ?????????? ?????????????????? ?? ????????, ?????????? ???????????? ??????????????????, ???????? - ??????, ?????????? ???????? ???????????????? ??????????????????
     */
    @Test
    public void achievementsOnSuccessfulPhotosModerationAfterUnsuccessful() throws Exception {
        List<UserAchievement> expectedAchievements = allPending();
        ModelGradeResponseDto modelGradeResponseDto = achievementsOnCreateModelGrade(
            ADD_MODEL_GRADE_FOR_ACHIEVEMENTS_BODY,
            expectedAchievements);
        long gradeId = modelGradeResponseDto.getId();
        moderateGradeWithPhoto(gradeId, ModState.APPROVED, ModState.REJECTED);
        achievementsModerationAndCacheClean();
        expectedAchievements = allConfirmedExceptPaparazzi();
        List<UserAchievement> userAchievements = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(expectedAchievements, userAchievements);
        moderateGradeWithPhoto(gradeId, ModState.APPROVED, ModState.APPROVED);
        achievementsModerationAndCacheClean();
        expectedAchievements = allConfirmed();
        userAchievements = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(expectedAchievements, userAchievements);
    }

    /**
     * ?????? ?????????? ??????????????????, ???????????????? ???????????????? ???????????? ???????????????? ???? ???????? ????????????
     */
    @Test
    public void achievementsOnAddPhotosToGrade() throws Exception {
        List<UserAchievement> expectedAchievements = new ArrayList<>();
        expectedAchievements.add(new UserAchievement(AchievementType.DEBUT.value(), 1, 0));
        expectedAchievements.add(new UserAchievement(AchievementType.FIRST_BIRD.value(), 1, 0));
        expectedAchievements.add(new UserAchievement(AchievementType.TOVAROVED.value(), 1, 0));
        ModelGradeResponseDto modelGradeResponseDto1 = achievementsOnCreateModelGrade(
            ADD_MODEL_GRADE_BODY_WITHOUT_PHOTO.replace(String.valueOf(MODEL_ID), String.valueOf(FIRST_BIRD_MODEL_ID)),
            expectedAchievements);
        long gradeId1 = modelGradeResponseDto1.getId();
        moderateGradeWithPhoto(gradeId1, ModState.APPROVED, ModState.REJECTED);
        achievementsModerationAndCacheClean();
        expectedAchievements.clear();
        expectedAchievements.add(new UserAchievement(AchievementType.DEBUT.value(), 0, 1));
        expectedAchievements.add(new UserAchievement(AchievementType.FIRST_BIRD.value(), 0, 1));
        expectedAchievements.add(new UserAchievement(AchievementType.TOVAROVED.value(), 0, 1));
        List<UserAchievement> userAchievements = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(expectedAchievements, userAchievements);
        expectedAchievements.clear();
        expectedAchievements.add(new UserAchievement(AchievementType.DEBUT.value(), 0, 1));
        expectedAchievements.add(new UserAchievement(AchievementType.PAPARAZZI.value(), 1, 0));
        expectedAchievements.add(new UserAchievement(AchievementType.FIRST_BIRD.value(), 0, 1));
        expectedAchievements.add(new UserAchievement(AchievementType.TOVAROVED.value(), 0, 1));
        ModelGradeResponseDto modelGradeResponseDto2 = achievementsOnCreateModelGrade(
            ADD_MODEL_GRADE_FOR_ACHIEVEMENTS_BODY,
            expectedAchievements);
        long gradeId2 = modelGradeResponseDto2.getId();
        moderateGradeWithPhoto(gradeId2, ModState.APPROVED, ModState.APPROVED);
        achievementsModerationAndCacheClean();
        expectedAchievements = allConfirmed();
        userAchievements = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(expectedAchievements, userAchievements);
    }

    @Test
    public void achievementsOnBindGradesSimpleTest() throws Exception {
        objectMapper.readValue(
            addModelGrade("YANDEXUID", String.valueOf(FAKE_YANDEXUID), ADD_MODEL_GRADE_FOR_ACHIEVEMENTS_BODY, status().is2xxSuccessful()),
            ModelGradeResponseDto.class);
        List<UserAchievement> userAchievements = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(Collections.emptyList(), userAchievements);
        achievementCacher.cleanForUser(FAKE_USER);
        bindGrades(FAKE_USER, FAKE_YANDEXUID);
        userAchievements = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(allPending(), userAchievements);
    }

    private void achievementsOnBindGradesSkeleton(ModState uidGrade, ModState uidPhoto, ModState yuidGrade, ModState yuidPhoto, List<UserAchievement> expectedUserAchievements) throws Exception {
        ModelGradeResponseDto modelGradeResponseDtoUid = objectMapper.readValue(
            addModelGrade("UID", String.valueOf(FAKE_USER), ADD_MODEL_GRADE_FOR_ACHIEVEMENTS_BODY, status().is2xxSuccessful()),
            ModelGradeResponseDto.class);
        ModelGradeResponseDto modelGradeResponseDtoYuid = objectMapper.readValue(
            addModelGrade("YANDEXUID", String.valueOf(FAKE_YANDEXUID), ADD_MODEL_GRADE_FOR_ACHIEVEMENTS_BODY, status().is2xxSuccessful()),
            ModelGradeResponseDto.class);
        List<UserAchievement> userAchievements = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(allPending(), userAchievements);
        moderateGradeWithPhoto(modelGradeResponseDtoUid.getId(), uidGrade, uidPhoto);
        moderateGradeWithPhoto(modelGradeResponseDtoYuid.getId(), yuidGrade, yuidPhoto);
        achievementsModerationAndCacheClean();
        bindGrades(FAKE_USER, FAKE_YANDEXUID);
        achievementsModerationAndCacheClean();
        achievementCacher.cleanForUser(FAKE_USER);
        userAchievements = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(expectedUserAchievements, userAchievements);
    }

    /**
     * ?????????????? ?????? ????????????: ???? ???????????????? ?? ???? ???? ????????????????.
     * ???????????? ??????????????????, ???? ?????? ????????, ???????????? ?????????????????? ??????????????????.
     * ???????????? yuid ???? uid
     * ?????????????? ?????????????? ?????? ????????????
     * @throws Exception
     */
    @Test
    public void achievementsOnBindGradesTest1() throws Exception {
        achievementsOnBindGradesSkeleton(ModState.APPROVED, ModState.REJECTED, ModState.APPROVED, ModState.APPROVED, allConfirmed());
    }

    /**
     * ?????????????? ?????? ????????????: ???? ???????????????? ?? ???? ???? ????????????????.
     * ???????????? ??????????????????, ???? ?????? ????????, ???????????? ??????????????????.
     * ???????????? yuid ???? uid
     * ?????????????? ?????????????? ?????? ????????????, ?????????? ???????????? ???? ????????
     * @throws Exception
     */
    @Test
    public void achievementsOnBindGradesTest2() throws Exception {
        achievementsOnBindGradesSkeleton(ModState.APPROVED, ModState.REJECTED, ModState.REJECTED, ModState.REJECTED, allConfirmedExceptPaparazzi());
    }

    /**
     * ?????????????? ?????? ????????????: ???? ???????????????? ?? ???? ???? ????????????????.
     * ???????????? ??????????????????, ???????????? ?????????????????? ??????????????????.
     * ???????????? yuid ???? uid
     * ?????????????? ?????????????? ?????? ????????????
     * @throws Exception
     */
    @Test
    public void achievementsOnBindGradesTest3() throws Exception {
        achievementsOnBindGradesSkeleton(ModState.REJECTED, ModState.REJECTED, ModState.APPROVED, ModState.APPROVED, allConfirmed());
    }

    @Test
    public void testClosestUserAchievementWithPaparazziSecondLevel() throws Exception {
        addModelGrade("UID", String.valueOf(FAKE_USER),
                ADD_MODEL_GRADE_BODY_WITHOUT_PHOTO,
                status().is2xxSuccessful());
        addModelGrade("UID", String.valueOf(FAKE_USER),
                ADD_MODEL_GRADE_BODY_WITHOUT_PHOTO.replace(String.valueOf(MODEL_ID), String.valueOf(ANOTHER_MODEL_ID)),
                status().is2xxSuccessful());
        addModelGrade("UID", String.valueOf(FAKE_USER),
                ADD_MODEL_GRADE_FOR_ACHIEVEMENTS_BODY,
                status().is2xxSuccessful());

        UserAchievementDto achievement = getClosestUserAchievement(FAKE_USER);

        assertEquals(new UserAchievement(AchievementType.PAPARAZZI.value(), 1, 0), achievement.getUserAchievement());
        assertEquals(2, achievement.getAchievementDistance());
        assertEquals(2, achievement.getAchievementLevel().getLevelId());
    }

    @Test
    public void testClosestUserAchievementWithDebut() throws Exception {
        UserAchievementDto achievement = getClosestUserAchievement(FAKE_USER);

        assertEquals(new UserAchievement(AchievementType.DEBUT.value(), 0, 0), achievement.getUserAchievement());
        assertEquals(1, achievement.getAchievementDistance());
    }

    @Test
    public void testClosestUserAchievementWithFullyFilledAchievementLevels() throws Exception {
        // generate grades to fill all achievement levels
        int shift = 50;
        List<Object[]> params = new ArrayList<>();
        for (AchievementType type : AchievementType.values()) {
            for (int i = 1; i <= 50; i++) {
                List<Object> row = new ArrayList<>();
                row.add(type.value());
                row.add(FAKE_USER);
                row.add(shift + i);
                row.add(AchievementEntityType.GRADE.getValue());
                row.add(AchievementEventType.CONFIRMED.value());
                params.add(row.toArray());
            }
            shift *= 3;
        }

        pgJdbcTemplate.batchUpdate(
            "INSERT INTO ACHIEVEMENT_EVENT " +
                "(ACHIEVEMENT_ID, AUTHOR_ID, ENTITY_ID, ENTITY_TYPE, EVENT_TYPE) " +
                "VALUES (?, ?, ?, ?, ?)",
            params);

        invokeAndRetrieveResponse(
                get("/api/achievements/UID/" + FAKE_USER + "/closest")
                        .accept(MediaType.APPLICATION_JSON),
                status().isNotFound());
    }

    @Test
    public void testAwaitsPhotoModerationModState() throws Exception {
        List<UserAchievement> expectedAchievements = allPending();
        ModelGradeResponseDto modelGradeResponseDto = objectMapper.readValue(
            addModelGrade("UID", String.valueOf(FAKE_USER), ADD_MODEL_GRADE_FOR_ACHIEVEMENTS_BODY, status().is2xxSuccessful()),
            ModelGradeResponseDto.class);
        List<UserAchievement> userAchievements = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(expectedAchievements, userAchievements);

        long gradeId = modelGradeResponseDto.getId();
        moderateGradeWithPhoto(gradeId, ModState.AWAITS_PHOTO_MODERATION, ModState.UNMODERATED);
        achievementsModerationAndCacheClean();
        expectedAchievements = allPending();
        userAchievements = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(expectedAchievements, userAchievements);

        moderateGradeWithPhoto(gradeId, ModState.APPROVED, ModState.APPROVED);
        achievementsModerationAndCacheClean();
        expectedAchievements = allConfirmed();
        userAchievements = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(expectedAchievements, userAchievements);
    }

    @Test
    public void testDelayedModState() throws Exception {
        List<UserAchievement> expectedAchievements = allPending();
        ModelGradeResponseDto modelGradeResponseDto = objectMapper.readValue(
            addModelGrade("UID", String.valueOf(FAKE_USER), ADD_MODEL_GRADE_FOR_ACHIEVEMENTS_BODY, status().is2xxSuccessful()),
            ModelGradeResponseDto.class);
        List<UserAchievement> userAchievements = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(expectedAchievements, userAchievements);

        long gradeId = modelGradeResponseDto.getId();
        moderateGradeWithPhoto(gradeId, ModState.DELAYED, ModState.UNMODERATED);
        achievementsModerationAndCacheClean();
        expectedAchievements = allPending();
        userAchievements = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(expectedAchievements, userAchievements);


        moderateGradeWithPhoto(gradeId, ModState.APPROVED, ModState.APPROVED);
        achievementsModerationAndCacheClean();
        expectedAchievements = allConfirmed();
        userAchievements = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(expectedAchievements, userAchievements);
    }

    private UserAchievementDto getClosestUserAchievement(long userId) throws Exception {
        return objectMapper.readValue(
                invokeAndRetrieveResponse(
                        get("/api/achievements/UID/" + userId + "/closest")
                                .accept(MediaType.APPLICATION_JSON),
                        status().is2xxSuccessful()),
                new TypeReference<UserAchievementDto>() {
                });
    }

}
