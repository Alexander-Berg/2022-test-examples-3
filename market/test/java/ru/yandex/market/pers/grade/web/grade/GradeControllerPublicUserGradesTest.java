package ru.yandex.market.pers.grade.web.grade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.cache.GradeCacher;
import ru.yandex.market.pers.grade.client.dto.GradePager;
import ru.yandex.market.pers.grade.client.dto.grade.GradeResponseDto;
import ru.yandex.market.pers.grade.client.dto.grade.ModelGradeResponseDto;
import ru.yandex.market.pers.grade.client.model.Anonymity;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.DbGradeAdminService;
import ru.yandex.market.pers.grade.core.model.core.AbstractGrade;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.grade.core.moderation.GradeModeratorModificationProxy;
import ru.yandex.market.pers.grade.core.ugc.PhotoService;
import ru.yandex.market.pers.grade.core.ugc.model.Photo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.grade.web.grade.UserGradesTest.EXPECTED_VOTES_AGREE;
import static ru.yandex.market.pers.grade.web.grade.UserGradesTest.EXPECTED_VOTES_REJECT;
import static ru.yandex.market.pers.grade.web.grade.UserGradesTest.assertGradeVotes;

public class GradeControllerPublicUserGradesTest extends GradeControllerBaseTest {

    private static int i = 0;
    private static final Long MODEL_ID = 4L;
    private static final String WHITE_MARKET_PHOTO_NAMESPACE = "market-ugc";

    @Autowired
    private DbGradeAdminService gradeAdminService;

    @Autowired
    private GradeCacher gradeCacher;

    @Autowired
    private PhotoService photoService;

    @Autowired
    private GradeModeratorModificationProxy gradeModeratorModificationProxy;

    @Autowired
    private GradeCreator gradeCreator;

    @Test
    public void testPublicUserGrades() throws Exception {
        int approvedGrades = 3;
        checkNoGrades();

        gradeCacher.cleanForAuthorId(FAKE_USER);
        List<Long> approved = createApproved(approvedGrades);
        createGarbage();

        GradePager<GradeResponseDtoImpl> userGrades = performGetUserGradesByUid(FAKE_USER,
                null, null);
        assertEquals(approved.size(), userGrades.getData().size());
        assertEquals(approved.size(), userGrades.getPager().getCount());
        assertTrue(userGrades.getData().stream().map(GradeResponseDto::getId).allMatch(approved::contains));
    }

    @Test
    public void testPublicUserGradesWithPager() throws Exception {
        int pageSize = 4;
        checkNoGrades();

        gradeCacher.cleanForAuthorId(FAKE_USER);
        List<Long> approved = createApproved(pageSize * 2);
        createGarbage();

        GradePager<GradeResponseDtoImpl> userGrades = performGetUserGradesByUid(FAKE_USER,
                1, pageSize);
        assertEquals(pageSize, userGrades.getData().size());
        assertEquals(approved.size(), userGrades.getPager().getCount());
    }

    @Test
    public void testKarmaVotesInPublicUserGrades() throws Exception {
        checkNoGrades();

        gradeCacher.cleanForAuthorId(FAKE_USER);
        long gradeId = createModelGrade(ModState.APPROVED, Anonymity.NONE);
        createTestVotesForGrade(gradeId, EXPECTED_VOTES_AGREE, EXPECTED_VOTES_REJECT);

        GradePager<GradeResponseDtoImpl> userGrades = performGetUserGradesByUid(FAKE_USER, null, null);

        assertEquals(1, userGrades.getData().size());
        assertEquals(1, userGrades.getPager().getCount());
        GradeResponseDtoImpl receivedGrade = userGrades.getData().get(0);
        assertEquals(gradeId, (long) receivedGrade.getId());

        assertGradeVotes(receivedGrade, EXPECTED_VOTES_AGREE, EXPECTED_VOTES_REJECT);
    }

    /**
     * Проверяем, что с публичными отзывами отдаются только фотографии, прошедшие модерацию.
     */
    @Test
    public void testPhotoModerationPublicUserGrades() throws Exception{
        //given
        checkNoGrades();

        gradeCacher.cleanForAuthorId(FAKE_USER);
        String res = addModelGrade("UID", String.valueOf(FAKE_USER), ADD_MODEL_GRADE_BODY
            .replace(
                "        {\n" +
                "            \"entity\": \"photo\",\n" +
                "            \"groupId\": \"abcde\",\n" +
                "            \"imageName\": \"iuirghreg\"\n" +
                "        }\n",
                getTestPhotoJsonString("photo1") + ",\n" +
                getTestPhotoJsonString("photo2") + ",\n" +
                getTestPhotoJsonString("photo3") + ",\n" +
                getTestPhotoJsonString("photo4") + ",\n" +
                getTestPhotoJsonString("photo5") + "\n"
        ), status().is2xxSuccessful());
        ModelGradeResponseDto gradeResponseDto = objectMapper.readValue(res, ModelGradeResponseDto.class);
        gradeModeratorModificationProxy.moderateGradeReplies(
            Collections.singletonList(gradeResponseDto.getId()), Collections.emptyList(), -1L, ModState.APPROVED);
        List<Photo> photos = photoService.getPhotosByGrade(gradeResponseDto.getId());

        moderateTestPhotosByImageName(photos, "photo1", ModState.APPROVED);
        moderateTestPhotosByImageName(photos, "photo2", ModState.REJECTED);
        moderateTestPhotosByImageName(photos, "photo3", ModState.SPAMMER);
        moderateTestPhotosByImageName(photos, "photo4", ModState.AUTOMATICALLY_REJECTED);
        moderateTestPhotosByImageName(photos, "photo5", ModState.APPROVED);

        //when
        GradePager<GradeResponseDtoImpl> userGrades = performGetUserGradesByUid(FAKE_USER, null, null);

        //then
        assertEquals(userGrades.getData().stream().flatMap(obj -> obj.getPhotos().stream()).filter(
            obj -> obj.getModStatusEnum() != ModState.APPROVED).count(), 0);
        assertEquals(userGrades.getData().stream().flatMap(obj -> obj.getPhotos().stream()).filter(
            obj -> obj.getImageName().equals("photo1") || obj.getImageName().equals("photo5")).count(), 2);
        assertEquals(WHITE_MARKET_PHOTO_NAMESPACE, userGrades.getData().get(0).getPhotos().get(0).getNamespace());

    }

    @NotNull
    private String getTestPhotoJsonString(String imageName) {
        return "        {\n" +
            "            \"entity\": \"photo\",\n" +
            "            \"groupId\": \"abcde\",\n" +
            "            \"imageName\": \"" + imageName + "\"\n" +
            "        }";
    }

    private void moderateTestPhotosByImageName(List<Photo> photos, String imageName, ModState modState) {
        photoService.moderatePhotos(
            photos.stream().filter(obj -> obj.getImageName().equals(imageName)).map(Photo::getId).collect(
                Collectors.toList()), 1L, modState);
    }

    private List<Long> createApproved(int size) {
        List<Long> approved = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            approved.add(createModelGrade(ModState.APPROVED, Anonymity.NONE));
        }
        return approved;
    }

    private void createGarbage() {
        createModelGrade(ModState.REJECTED, Anonymity.NONE);
        createModelGrade(ModState.AUTOMATICALLY_REJECTED, Anonymity.NONE);
        createModelGrade(ModState.SPAMMER, Anonymity.NONE);
        createModelGrade(ModState.APPROVED, Anonymity.HIDE_ALL);
        createModelGrade(ModState.APPROVED, Anonymity.HIDE_NAME);
        createModelGrade(ModState.APPROVED, Anonymity.NONE, true);
    }

    private void checkNoGrades() throws Exception {
        GradePager<GradeResponseDtoImpl> userGrades = performGetUserGradesByUid(FAKE_USER, null, null);
        assertTrue(userGrades.getData().isEmpty());
        assertEquals(0, userGrades.getPager().getCount());
    }

    private GradePager<GradeResponseDtoImpl> performGetUserGradesByUid(Long uid, Integer pageNum, Integer pageSize) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
                get(createUrl(uid, pageNum, pageSize)),
                status().is2xxSuccessful()
        ), new TypeReference<GradePager<GradeResponseDtoImpl>>() {
        });
    }

    private ModelGrade createTestModelGrade(long modelId, ModState modState, Anonymity anonymity) {
        ModelGrade grade = GradeCreator.constructModelGrade(modelId, FAKE_USER);
        grade.setText(UUID.randomUUID().toString());
        grade.setModState(modState);
        grade.setAnonymous(anonymity);
        grade.setGr0(-2);
        return grade;
    }

    private long createModelGrade(ModState modState, Anonymity anonymity, boolean spam) {
        AbstractGrade grade = createTestModelGrade(MODEL_ID + i++, modState, anonymity);
        long gradeId = gradeCreator.createGrade(grade);
        if (spam) {
            gradeAdminService.setGradeState(List.of(gradeId), true);
        }
        return gradeId;
    }

    private long createModelGrade(ModState modState, Anonymity anonymity) {
        return createModelGrade(modState, anonymity, false);
    }

    private String createUrl(Long uid, Integer pageNum, Integer pageSize) {
        String url = "/api/grade/public/user/pager?uid=" + uid;
        if (pageNum != null && pageSize != null) {
            url += String.format("&page_num=%s&page_size=%s", pageNum, pageSize);
        }
        return url;
    }
}
