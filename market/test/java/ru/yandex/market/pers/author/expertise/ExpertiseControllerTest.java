package ru.yandex.market.pers.author.expertise;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.author.PersAuthorTest;
import ru.yandex.market.pers.author.agitation.AgitationService;
import ru.yandex.market.pers.author.agitation.model.Agitation;
import ru.yandex.market.pers.author.agitation.model.AgitationCancel;
import ru.yandex.market.pers.author.agitation.model.AgitationUser;
import ru.yandex.market.pers.author.client.api.PersAuthorApiConstants;
import ru.yandex.market.pers.author.client.api.dto.UserExpertiseMailDto;
import ru.yandex.market.pers.author.client.api.model.AgitationCancelReason;
import ru.yandex.market.pers.author.client.api.model.AgitationEntity;
import ru.yandex.market.pers.author.client.api.model.AgitationType;
import ru.yandex.market.pers.author.expertise.dto.ExpertiseGainDto;
import ru.yandex.market.pers.author.expertise.dto.UserExpertiseDto;
import ru.yandex.market.pers.author.expertise.dto.UserExpertiseDtoListWithHidMapping;
import ru.yandex.market.pers.author.expertise.model.Expertise;
import ru.yandex.market.pers.author.expertise.model.ExpertiseCost;
import ru.yandex.market.pers.author.mock.PersAuthorSaasMocks;
import ru.yandex.market.pers.author.mock.mvc.AgitationMvcMocks;
import ru.yandex.market.pers.author.mock.mvc.ExpertiseMvcMocks;
import ru.yandex.market.saas.search.SaasKvSearchRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.pers.author.client.api.model.AgitationCancelReason.COMPLETED;
import static ru.yandex.market.pers.author.expertise.ExpertiseTestCostData.ANSWER_COST;
import static ru.yandex.market.pers.author.expertise.ExpertiseTestCostData.GRADE_PHOTO_COST;
import static ru.yandex.market.pers.author.expertise.ExpertiseTestCostData.MODEL_GRADE_CREATE_COST;
import static ru.yandex.market.pers.author.expertise.ExpertiseTestCostData.MODEL_GRADE_TEXT_COST;
import static ru.yandex.market.pers.author.expertise.ExpertiseTestCostData.SHOP_GRADE_CREATE_COST;
import static ru.yandex.market.pers.author.expertise.ExpertiseTestCostData.VIDEO_COST;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 14.05.2020
 */
public class ExpertiseControllerTest extends PersAuthorTest {
    public static final long USER_ID = 123;
    public static final String MODEL_ID = "1231412";
    public static final String SHOP_ID = "4525245";
    public static final int EXPECTED_EXPERTISE_COUNT = 17;
    //this is not the best solution. Better to initialize separate expertise and levels for test context
    public static final long[] STD_EXP_NID_MOCKED = {15, 16};
    public static final Map<Long, Long> TOP_EXPERTISE_ID_LEVEL_MAPPING =
        Map.of(1L, 69L, 2L, 420L, 0L, 322L);

    @Autowired
    private ExpertiseMvcMocks expertiseMvc;

    @Autowired
    private AgitationMvcMocks agitationMvc;

    @Autowired
    private PersAuthorSaasMocks authorSaasMocks;

    @Autowired
    private AgitationService agitationService;

    @Test
    public void testDictionary() {
        List<Expertise> dictionary = expertiseMvc.getDictionary();

        assertEquals(EXPECTED_EXPERTISE_COUNT, dictionary.size());
        for (Expertise expertise : dictionary) {
            assertEquals(11, expertise.getLevels().size());
            assertNotNull(expertise.getName());
            assertNotNull(expertise.getDescription());
            assertNotNull(expertise.getImages());
            assertNotNull(expertise.getImages().get(PersAuthorApiConstants.IMAGE_DEFAULT_KEY));
            assertNotNull(expertise.getImages().get(PersAuthorApiConstants.IMAGE_SMALL_KEY));
            assertNotNull(expertise.getImages().get(PersAuthorApiConstants.IMAGE_PNG_KEY));
        }

        assertEquals("Гурман", dictionary.get(1).getName());
        assertEquals("Продукты", dictionary.get(1).getDescription());

        // test cache works
        jdbcTemplate.update("insert into pers.expertise(id, name, image, small_image) values (100, 'test', 't:t:t', 't:t:t')");

        dictionary = expertiseMvc.getDictionary();
        assertEquals(EXPECTED_EXPERTISE_COUNT, dictionary.size());

        invalidateCache();

        dictionary = expertiseMvc.getDictionary();
        assertEquals(EXPECTED_EXPERTISE_COUNT + 1, dictionary.size());

        jdbcTemplate.update("delete from pers.expertise where id = 100");
    }

    @Test
    public void testCostDictionary() {
        List<ExpertiseCost> dictionary = expertiseMvc.getCostDictionary();

        List<AgitationType> expected = Arrays.stream(AgitationType.values())
            .filter(x -> x.getEntity() != AgitationEntity.ORDER)
            .collect(Collectors.toList());

        assertEquals(expected.size(), dictionary.size());
        for (int i = 0; i < expected.size(); i++) {
            ExpertiseCost expertiseCost = dictionary.get(i);
            assertEquals(expected.get(i), expertiseCost.getAgitationTypeEnum());
            assertTrue(expertiseCost.getCost() > 0);
        }
    }

    @Test
    public void testDictionaryExpertiseByHid() {
        int hid = 1231;
        mockStdHidNids(1231);
        invalidateCache();

        List<Long> dictionary = expertiseMvc.getDictionaryExpertiseByHid(hid);
        dictionary.sort(Long::compareTo);

        assertEquals(Arrays.stream(STD_EXP_NID_MOCKED).boxed().collect(Collectors.toList()), dictionary);
    }

    @Test
    public void testDictionaryExpertiseByHidsEveryHidExists() {
        List<Long> hids = List.of(1231L, 1232L);
        hids.forEach(hid -> mockStdHidNids(hid.intValue()));
        invalidateCache();

        Map<Long, Long> expertiseByHidsMap = expertiseMvc.getDictionaryExpertiseByHids(hids);
        for (Long hid : expertiseByHidsMap.keySet()) {
            Long expertise = expertiseByHidsMap.get(hid);
            assertTrue(Arrays.stream(STD_EXP_NID_MOCKED).boxed().anyMatch(expertise::equals));
        }
    }

    @Test
    public void testDictionaryExpertiseByHidsIfNotEveryHidExists() {
        Long existHid = 1231L;
        Long notExistsHid = 1232L;

        mockStdHidNids(existHid.intValue());
        invalidateCache();

        Map<Long, Long> expertiseByHidsMap = expertiseMvc.getDictionaryExpertiseByHids(List.of(existHid, notExistsHid));

        assertNotNull(expertiseByHidsMap.get(existHid));
        assertNull(expertiseByHidsMap.get(notExistsHid));

        Long expertiseByExistHid = expertiseByHidsMap.get(existHid);
        assertTrue(Arrays.stream(STD_EXP_NID_MOCKED).boxed().anyMatch(expertiseByExistHid::equals));
    }

    @Test
    public void testGetTopUserExpertiseByHidsWithoutShop() {
        authorSaasMocks.mockExpertise("1-69|2-420|0-322");
        long hid1 = 1231L;
        long hid2 = 1232L;
        addRootNids(hid1, 54434);
        addRootNids(hid2, 54734);

        UserExpertiseDtoListWithHidMapping topUserExpertiseByHids =
            expertiseMvc.getTopUserExpertiseByHids(USER_ID, List.of(hid1, hid2), false);

        //check expertise mapping
        assertEquals(Map.of(hid1, 1L, hid2, 2L), topUserExpertiseByHids.getMapping());

        //check data list
        assertEquals(2, topUserExpertiseByHids.getData().size());
        topUserExpertiseByHids.getData().forEach(expertise ->
            //we already know that exactly this expertise in response
            assertLevel(expertise, USER_ID, expertise.getExpertiseId(),
                TOP_EXPERTISE_ID_LEVEL_MAPPING.get(expertise.getExpertiseId())));
    }

    @Test
    public void testGetTopUserExpertiseByHidsWithShop() {
        authorSaasMocks.mockExpertise("1-69|2-420|0-322");
        long hid1 = 1231L;
        long hid2 = 1232L;
        addRootNids(hid1, 54434);
        addRootNids(hid2, 54734);

        UserExpertiseDtoListWithHidMapping topUserExpertiseByHids =
            expertiseMvc.getTopUserExpertiseByHids(USER_ID, List.of(hid1, hid2), true);

        assertEquals(Map.of(hid1, 1L, hid2, 2L), topUserExpertiseByHids.getMapping());
        assertEquals(3, topUserExpertiseByHids.getData().size());
        topUserExpertiseByHids.getData().forEach(expertise ->
            //we already know that exactly this expertise in response
            assertLevel(expertise, USER_ID, expertise.getExpertiseId(),
                TOP_EXPERTISE_ID_LEVEL_MAPPING.get(expertise.getExpertiseId())));
    }

    @Test
    public void testGetTopUserExpertiseByHidsEmptyExpertise() {
        long hid1 = 1231L;
        long hid2 = 1232L;
        addRootNids(hid1, 54434);
        addRootNids(hid2, 54734);

        UserExpertiseDtoListWithHidMapping topUserExpertiseByHids =
                expertiseMvc.getTopUserExpertiseByHids(USER_ID, List.of(hid1, hid2), true);

        assertEquals(Map.of(hid1, 1L, hid2, 2L), topUserExpertiseByHids.getMapping());
        assertEquals(3, topUserExpertiseByHids.getData().size());
        topUserExpertiseByHids.getData().forEach(expertise ->
                //we already know that exactly this expertise in response
                assertLevel(expertise, USER_ID, expertise.getExpertiseId(), 0));
    }

    @Test
    public void testGetTopUserExpertiseByHidsWhenHidIsAbsent() {
        authorSaasMocks.mockExpertise("1-69|2-420|0-322");
        long existsHid = 1231L;
        long notExistsHid = 1232L;
        addRootNids(existsHid, 54434);

        UserExpertiseDtoListWithHidMapping topUserExpertiseByHids =
            expertiseMvc.getTopUserExpertiseByHids(USER_ID, List.of(existsHid, notExistsHid), false);

        //check mapping
        assertEquals(Map.of(existsHid, 1L), topUserExpertiseByHids.getMapping());
        //check the one user expertise
        assertEquals(1, topUserExpertiseByHids.getData().size());
        assertLevel(topUserExpertiseByHids.getData().get(0), USER_ID, 1, 69);
    }

    @Test
    public void testExpertiseSimple() {
        authorSaasMocks.mockExpertise("1-42|4-12|6-99999");

        List<UserExpertiseDto> expertiseList = expertiseMvc.getExpertiseList(USER_ID);

        assertAllExpertisesLoaded(expertiseList);
        assertLevel(expertiseList.get(0), USER_ID, 6, 99999);
        assertLevel(expertiseList.get(1), USER_ID, 1, 42);
        assertLevel(expertiseList.get(2), USER_ID, 4, 12);
        assertNoExpertise(expertiseList, 3);

        // test cache works
        resetMocks();
        authorSaasMocks.mockExpertise(null);
        expertiseList = expertiseMvc.getExpertiseList(USER_ID);

        assertAllExpertisesLoaded(expertiseList);
        assertLevel(expertiseList.get(0), USER_ID, 6, 99999);
        assertLevel(expertiseList.get(1), USER_ID, 1, 42);
        assertLevel(expertiseList.get(2), USER_ID, 4, 12);
        assertNoExpertise(expertiseList, 3);

        // clean cache - now no content
        invalidateCache();

        expertiseList = expertiseMvc.getExpertiseList(USER_ID);

        assertAllExpertisesLoaded(expertiseList);
        assertNoExpertise(expertiseList, 0);
    }

    @Test
    public void testExpertiseWithFreshSimple() {
        int hid = 1;
        mockStdHidNids(hid);

        authorSaasMocks.mockExpertise(STD_EXP_NID_MOCKED[0] + "-42|4-12|" + STD_EXP_NID_MOCKED[1] + "-99999");
        expertiseMvc.updateExpertise(USER_ID, AgitationType.MODEL_QUESTION_ANSWER, MODEL_ID, hid);

        List<UserExpertiseDto> expertiseList = expertiseMvc.getExpertiseList(USER_ID);

        assertAllExpertisesLoaded(expertiseList);
        assertLevel(expertiseList.get(0), USER_ID, STD_EXP_NID_MOCKED[1], 99999 + ANSWER_COST);
        assertLevel(expertiseList.get(1), USER_ID, STD_EXP_NID_MOCKED[0], 42 + ANSWER_COST);
        assertLevel(expertiseList.get(2), USER_ID, 4, 12);
        assertNoExpertise(expertiseList, 3);
    }

    @Test
    public void testExpertiseWithFreshSimpleRepeated() {
        int hid = 1;
        mockStdHidNids(hid);

        // content was repeated, this should not be counted twice
        authorSaasMocks.mockExpertise(STD_EXP_NID_MOCKED[0] + "-42|4-12|" + STD_EXP_NID_MOCKED[1] + "-99999");
        expertiseMvc.updateExpertise(USER_ID, AgitationType.MODEL_QUESTION_ANSWER, MODEL_ID, hid);
        expertiseMvc.updateExpertise(USER_ID, AgitationType.MODEL_QUESTION_ANSWER, MODEL_ID, hid);

        long gain = ANSWER_COST;
        List<UserExpertiseDto> expertiseList = expertiseMvc.getExpertiseList(USER_ID);

        assertAllExpertisesLoaded(expertiseList);
        assertLevel(expertiseList.get(0), USER_ID, STD_EXP_NID_MOCKED[1], 99999 + gain);
        assertLevel(expertiseList.get(1), USER_ID, STD_EXP_NID_MOCKED[0], 42 + gain);
        assertLevel(expertiseList.get(2), USER_ID, 4, 12);
        assertNoExpertise(expertiseList, 3);
    }

    @Test
    public void testExpertiseWithFreshSimpleMultiple() {
        int hid = 1;
        mockStdHidNids(hid);

        // lot of different content was created
        authorSaasMocks.mockExpertise(STD_EXP_NID_MOCKED[0] + "-42|4-12|" + STD_EXP_NID_MOCKED[1] + "-99999");
        expertiseMvc.updateExpertise(USER_ID, AgitationType.MODEL_QUESTION_ANSWER, MODEL_ID, hid);
        expertiseMvc.updateExpertise(USER_ID, AgitationType.MODEL_QUESTION_ANSWER, MODEL_ID + 1, hid);
        expertiseMvc.updateExpertise(USER_ID, AgitationType.MODEL_VIDEO, MODEL_ID, hid);

        long gain = 2 * ANSWER_COST + VIDEO_COST;
        List<UserExpertiseDto> expertiseList = expertiseMvc.getExpertiseList(USER_ID);

        assertAllExpertisesLoaded(expertiseList);
        assertLevel(expertiseList.get(0), USER_ID, STD_EXP_NID_MOCKED[1], 99999 + gain);
        assertLevel(expertiseList.get(1), USER_ID, STD_EXP_NID_MOCKED[0], 42 + gain);
        assertLevel(expertiseList.get(2), USER_ID, 4, 12);
        assertNoExpertise(expertiseList, 3);
    }

    @Test
    public void testExpertiseWithFreshMultipleOldIndex() {
        int hid = 1;
        mockStdHidNids(hid);

        // lot of different content was created
        authorSaasMocks.mockExpertise(STD_EXP_NID_MOCKED[0] + "-42|4-12|" + STD_EXP_NID_MOCKED[1] + "-99999");
        expertiseMvc.updateExpertise(USER_ID, AgitationType.MODEL_QUESTION_ANSWER, MODEL_ID, hid);

        makeUserExpDiffOlderThanIndex(USER_ID);
        expertiseMvc.updateExpertise(USER_ID, AgitationType.MODEL_QUESTION_ANSWER, MODEL_ID + 1, hid);
        expertiseMvc.updateExpertise(USER_ID, AgitationType.MODEL_VIDEO, MODEL_ID, hid);

        // only one answer should be counted from diff list
        long gain = ANSWER_COST + VIDEO_COST;
        List<UserExpertiseDto> expertiseList = expertiseMvc.getExpertiseList(USER_ID);

        assertAllExpertisesLoaded(expertiseList);
        assertLevel(expertiseList.get(0), USER_ID, STD_EXP_NID_MOCKED[1], 99999 + gain);
        assertLevel(expertiseList.get(1), USER_ID, STD_EXP_NID_MOCKED[0], 42 + gain);
        assertLevel(expertiseList.get(2), USER_ID, 4, 12);
        assertNoExpertise(expertiseList, 3);
    }

    @Test
    public void testExpertiseWithFreshComplex() {
        int hid = 1;
        mockStdHidNids(hid);

        // grade existed before. Added text, removed photos
        authorSaasMocks.mockExpertise(STD_EXP_NID_MOCKED[0] + "-42|4-12|" + STD_EXP_NID_MOCKED[1] + "-99999");
        expertiseMvc.updateExpertise(USER_ID, AgitationEntity.MODEL, MODEL_ID, hid,
            List.of(AgitationType.MODEL_GRADE_TEXT), List.of(AgitationType.MODEL_GRADE_PHOTO));

        long gain = MODEL_GRADE_TEXT_COST - GRADE_PHOTO_COST;
        List<UserExpertiseDto> expertiseList = expertiseMvc.getExpertiseList(USER_ID);

        assertAllExpertisesLoaded(expertiseList);
        assertLevel(expertiseList.get(0), USER_ID, STD_EXP_NID_MOCKED[1], 99999 + gain);
        assertLevel(expertiseList.get(1), USER_ID, STD_EXP_NID_MOCKED[0], 42 + gain);
        assertLevel(expertiseList.get(2), USER_ID, 4, 12);
        assertNoExpertise(expertiseList, 3);
    }

    @Test
    public void testExpertiseLeveled() {
        authorSaasMocks.mockExpertise("1-42|4-41|6-43");

        List<UserExpertiseDto> expertiseList = expertiseMvc.getExpertiseList(USER_ID);

        assertAllExpertisesLoaded(expertiseList);
        assertLevel(expertiseList.get(0), USER_ID, 6, 43);
        assertLevel(expertiseList.get(1), USER_ID, 1, 42);
        assertLevel(expertiseList.get(2), USER_ID, 4, 41);
        assertNoExpertise(expertiseList, 3);
    }

    @Test
    public void testExpertiseEmpty() {
        authorSaasMocks.mockSaasKvResponseEmpty();

        List<UserExpertiseDto> expertiseList = expertiseMvc.getExpertiseList(USER_ID);

        assertAllExpertisesLoaded(expertiseList);
        assertNoExpertise(expertiseList, 0);
    }

    @Test
    public void testExpertiseMissing() {
        authorSaasMocks.mockExpertise(null);

        List<UserExpertiseDto> expertiseList = expertiseMvc.getExpertiseList(USER_ID);

        assertAllExpertisesLoaded(expertiseList);
        assertNoExpertise(expertiseList, 0);
    }

    private void addRootNids(long hid, long... rootNids) {
        for (long rootNid : rootNids) {
            jdbcTemplate.update(
                "insert into pers.hid_to_root_nid(hid, root_nid) values (?,?)",
                hid, rootNid);
        }
    }

    @Test
    public void testExpertiseByHid() {
        authorSaasMocks.mockExpertise("413-42|1-66|54418-134");
        addRootNids(0, 54434);

        List<UserExpertiseDto> expertiseList = expertiseMvc.getExpertiseByHid(0, USER_ID);

        assertEquals(1, expertiseList.size(), "Only one expertise per hid");
        assertLevel(expertiseList.get(0), USER_ID, 1, 66);
    }

    @Test
    public void testExpertiseByHidBulk() {
        authorSaasMocks.mockExpertise(Map.of(
            USER_ID, "4-42|1-66|2-99999",
            USER_ID + 1, "1-99999"), null);

        addRootNids(0, 54434);

        List<UserExpertiseDto> expertiseList = expertiseMvc.getExpertiseByHid(0, USER_ID, USER_ID + 1);

        assertEquals(2, expertiseList.size(), "Two users - two expertises");
        assertLevel(expertiseList.get(0), USER_ID, 1, 66);
        assertLevel(expertiseList.get(1), USER_ID + 1, 1, 99999);
    }

    @NotNull
    private ArgumentMatcher<SaasKvSearchRequest> userMatch(int userId) {
        return argument -> argument != null && argument.getKey().equals("" + userId);
    }

    @Test
    public void testExpertiseByHidMultiple() {
        authorSaasMocks.mockExpertise("1-42|2-66|6-99999|3-31");
        addRootNids(1, 54734, 54421);

        List<UserExpertiseDto> expertiseList = expertiseMvc.getExpertiseByHid(1, USER_ID);

        assertEquals(1, expertiseList.size(), "Only one can be the best");
        assertLevel(expertiseList.get(0), USER_ID, 2, 66);
    }

    @Test
    public void testExpertiseByHidMultipleDefault() {
        authorSaasMocks.mockExpertise("1-66|2-99999|3-31");
        addRootNids(1, 547340, 544210);

        List<UserExpertiseDto> expertiseList = expertiseMvc.getExpertiseByHid(1, USER_ID);

        assertEquals(0, expertiseList.size(), "Do not show any when they are all empty");
    }

    @Test
    public void testExpertiseByHidEmpty() {
        authorSaasMocks.mockExpertise("1-42|2-66|3-99999|4-31");

        List<UserExpertiseDto> expertiseList = expertiseMvc.getExpertiseByHid(2, USER_ID);

        assertEquals(0, expertiseList.size(), "no expertises per hid");
    }

    @Test
    public void testExpertiseGainSimple() {
        int hid = 1;
        mockStdHidNids(hid);

        expertiseMvc.updateExpertise(USER_ID, AgitationType.MODEL_QUESTION_ANSWER, MODEL_ID, hid);

        ExpertiseGainDto expertiseGain = expertiseMvc.getExpertiseGain(USER_ID);
        assertGain(expertiseGain, AgitationType.MODEL_QUESTION_ANSWER, ANSWER_COST, ANSWER_COST, STD_EXP_NID_MOCKED);

        assertSimpleGainAdded(STD_EXP_NID_MOCKED, ANSWER_COST);
        assertAgitationCancelSize(USER_ID, 1);
        assertAgitations(USER_ID, COMPLETED, MODEL_ID, AgitationType.MODEL_QUESTION_ANSWER);
    }

    @Test
    public void testExpertiseGainModelText() {
        int hid = 1;
        mockStdHidNids(hid);

        expertiseMvc.updateExpertise(USER_ID, AgitationType.MODEL_GRADE, MODEL_ID, hid);
        expertiseMvc.updateExpertise(USER_ID, AgitationType.MODEL_GRADE_TEXT, MODEL_ID, hid);

        ExpertiseGainDto expertiseGain = expertiseMvc.getExpertiseGain(USER_ID);
        long gain =MODEL_GRADE_CREATE_COST + MODEL_GRADE_TEXT_COST;
        assertGain(expertiseGain, AgitationType.MODEL_GRADE_TEXT, gain, gain, STD_EXP_NID_MOCKED);

        assertSimpleGainAdded(STD_EXP_NID_MOCKED, gain);
    }

    @Test
    public void testExpertiseGainModelTextInverted() {
        int hid = 1;
        mockStdHidNids(hid);

        expertiseMvc.updateExpertise(USER_ID, AgitationType.MODEL_GRADE_TEXT, MODEL_ID, hid);
        expertiseMvc.updateExpertise(USER_ID, AgitationType.MODEL_GRADE, MODEL_ID, hid);

        ExpertiseGainDto expertiseGain = expertiseMvc.getExpertiseGain(USER_ID);
        long gain =MODEL_GRADE_CREATE_COST + MODEL_GRADE_TEXT_COST;
        assertGain(expertiseGain, AgitationType.MODEL_GRADE_TEXT, gain, gain, STD_EXP_NID_MOCKED);

        assertSimpleGainAdded(STD_EXP_NID_MOCKED, gain);
    }

    @Test
    public void testExpertiseGainRepeat() {
        int hid = 1;
        mockStdHidNids(hid);

        expertiseMvc.updateExpertise(USER_ID, AgitationType.MODEL_QUESTION_ANSWER, MODEL_ID, hid);
        expertiseMvc.updateExpertise(USER_ID, AgitationType.MODEL_VIDEO, MODEL_ID, hid);

        ExpertiseGainDto expertiseGain = expertiseMvc.getExpertiseGain(USER_ID);
        assertGain(expertiseGain, AgitationType.MODEL_VIDEO, VIDEO_COST, VIDEO_COST + ANSWER_COST, STD_EXP_NID_MOCKED);

        assertSimpleGainAdded(STD_EXP_NID_MOCKED, VIDEO_COST + ANSWER_COST);
        assertAgitationCancelSize(USER_ID, 2);
        assertAgitations(USER_ID, COMPLETED, MODEL_ID, AgitationType.MODEL_QUESTION_ANSWER, AgitationType.MODEL_VIDEO);
    }

    @Test
    public void testExpertiseGainShop() {
        long[] expectedExpertises = {ExpertiseService.SHOP_EXPERTISE_ID};

        expertiseMvc.updateExpertise(USER_ID, AgitationType.SHOP_GRADE, SHOP_ID, null);

        ExpertiseGainDto expertiseGain = expertiseMvc.getExpertiseGain(USER_ID);
        assertGain(expertiseGain,
            AgitationType.SHOP_GRADE,
            SHOP_GRADE_CREATE_COST,
            SHOP_GRADE_CREATE_COST,
            expectedExpertises);

        assertSimpleGainAdded(expectedExpertises, SHOP_GRADE_CREATE_COST);
        assertAgitationCancelSize(USER_ID, 1);
        assertAgitations(USER_ID, COMPLETED, SHOP_ID, AgitationType.SHOP_GRADE);
    }

    @Test
    public void testExpertiseGainShopNoIndex() {
        long[] expectedExpertises = {ExpertiseService.SHOP_EXPERTISE_ID};

        authorSaasMocks.mockExpertise(Collections.emptyMap(), null);

        expertiseMvc.updateExpertise(USER_ID, AgitationType.SHOP_GRADE, SHOP_ID, null);

        ExpertiseGainDto expertiseGain = expertiseMvc.getExpertiseGain(USER_ID);
        assertGain(expertiseGain,
            AgitationType.SHOP_GRADE,
            SHOP_GRADE_CREATE_COST,
            SHOP_GRADE_CREATE_COST,
            expectedExpertises);

        assertSimpleGainAdded(expectedExpertises, SHOP_GRADE_CREATE_COST);
    }

    @Test
    public void testExpertiseGainShopEmptyIndex() {
        long[] expectedExpertises = {ExpertiseService.SHOP_EXPERTISE_ID};

        authorSaasMocks.mockExpertise(Collections.emptyMap(), "");

        expertiseMvc.updateExpertise(USER_ID, AgitationType.SHOP_GRADE, SHOP_ID, null);

        ExpertiseGainDto expertiseGain = expertiseMvc.getExpertiseGain(USER_ID);
        assertGain(expertiseGain,
            AgitationType.SHOP_GRADE,
            SHOP_GRADE_CREATE_COST,
            SHOP_GRADE_CREATE_COST,
            expectedExpertises);

        assertSimpleGainAdded(expectedExpertises, SHOP_GRADE_CREATE_COST);
    }

    @Test
    public void testExpertiseGainShopModel() {
        int hid = 1;
        mockStdHidNids(hid);
        long[] expectedShopExpertises = {ExpertiseService.SHOP_EXPERTISE_ID};

        // create shop and model grade with same resourceId
        // should count them separately
        expertiseMvc.updateExpertise(USER_ID, AgitationType.MODEL_GRADE, MODEL_ID, hid);
        expertiseMvc.updateExpertise(USER_ID, AgitationType.SHOP_GRADE, MODEL_ID, null);

        ExpertiseGainDto expertiseGain = expertiseMvc.getExpertiseGain(USER_ID);
        assertGain(expertiseGain,
            AgitationType.SHOP_GRADE,
            SHOP_GRADE_CREATE_COST,
            SHOP_GRADE_CREATE_COST,
            expectedShopExpertises);

        assertSimpleGainAdded(List.of(
            expectLevel(STD_EXP_NID_MOCKED[0], MODEL_GRADE_CREATE_COST),
            expectLevel(STD_EXP_NID_MOCKED[1], MODEL_GRADE_CREATE_COST),
            expectLevel(ExpertiseService.SHOP_EXPERTISE_ID, SHOP_GRADE_CREATE_COST)
        ), true);

        assertAgitationCancelSize(USER_ID, 2);
        assertAgitations(USER_ID, COMPLETED, MODEL_ID, AgitationType.MODEL_GRADE, AgitationType.SHOP_GRADE);
    }

    @Test
    public void testExpertiseGainGranulated() {
        int hid = 1;
        mockStdHidNids(hid);

        expertiseMvc.updateExpertise(USER_ID, AgitationType.MODEL_GRADE, MODEL_ID, hid);
        expertiseMvc.updateExpertise(USER_ID, AgitationType.MODEL_GRADE_TEXT, MODEL_ID, hid);

        long gain = MODEL_GRADE_CREATE_COST + MODEL_GRADE_TEXT_COST;
        ExpertiseGainDto expertiseGain = expertiseMvc.getExpertiseGain(USER_ID);
        assertGain(expertiseGain, AgitationType.MODEL_GRADE_TEXT, gain, gain, STD_EXP_NID_MOCKED);

        assertSimpleGainAdded(STD_EXP_NID_MOCKED, gain);

        // check no more gain exists
        assertNoGain(USER_ID);

        // check agitations completed, added photo agitation as next
        assertAgitationCancelSize(USER_ID, 2);
        assertAgitations(USER_ID, COMPLETED, MODEL_ID, AgitationType.MODEL_GRADE, AgitationType.MODEL_GRADE_TEXT);
        assertEquals(1, agitationMvc.getPopup(AgitationUser.uid(USER_ID), AgitationType.MODEL_GRADE_PHOTO).size());

        // add photo
        expertiseMvc.updateExpertise(USER_ID, AgitationType.MODEL_GRADE_PHOTO, MODEL_ID, hid);

        expertiseGain = expertiseMvc.getExpertiseGain(USER_ID);
        assertGain(expertiseGain,
            AgitationType.MODEL_GRADE_PHOTO,
            GRADE_PHOTO_COST,
            GRADE_PHOTO_COST + gain,
            STD_EXP_NID_MOCKED);

        assertSimpleGainAdded(STD_EXP_NID_MOCKED, gain + GRADE_PHOTO_COST);

        // check no more gain exists
        assertNoGain(USER_ID);

        // check all agitation are completed, popup was reset
        assertAgitationCancelSize(USER_ID, 3);
        assertAgitations(USER_ID, COMPLETED, MODEL_ID,
            AgitationType.MODEL_GRADE, AgitationType.MODEL_GRADE_TEXT, AgitationType.MODEL_GRADE_PHOTO);
        assertEquals(0, agitationMvc.getPopup(AgitationUser.uid(USER_ID), AgitationType.MODEL_GRADE_PHOTO).size());
    }

    @Test
    public void testExpertiseGainExpired() {
        int hid = 1;
        mockStdHidNids(hid);

        expertiseMvc.updateExpertise(USER_ID, AgitationType.MODEL_GRADE, MODEL_ID, hid);
        expertiseMvc.updateExpertise(USER_ID, AgitationType.MODEL_GRADE_TEXT, MODEL_ID, hid);

        // expire gain
        makeUserExpDiffOlder(USER_ID);

        // add photo
        expertiseMvc.updateExpertise(USER_ID, AgitationType.MODEL_GRADE_PHOTO, MODEL_ID, hid);

        long gain = GRADE_PHOTO_COST;
        long total = gain + MODEL_GRADE_CREATE_COST + MODEL_GRADE_TEXT_COST;
        ExpertiseGainDto expertiseGain = expertiseMvc.getExpertiseGain(USER_ID);
        assertGain(expertiseGain,
            AgitationType.MODEL_GRADE_PHOTO,
            gain,
            total,
            STD_EXP_NID_MOCKED);

        assertSimpleGainAdded(STD_EXP_NID_MOCKED, total);

        // check no more gain exists
        assertNoGain(USER_ID);
    }

    private void makeUserExpDiffOlderThanIndex(long userId) {
        jdbcTemplate.update(
            "update pers.expertise_diff \n" +
                "set cr_time = now() - interval '" + (PersAuthorSaasMocks.INDEX_AGE_DAYS + 1) + "' day \n" +
                "where user_id = ? ",
            userId);
    }

    private void makeUserExpDiffOlder(long userId) {
        jdbcTemplate.update(
            "update pers.expertise_diff \n" +
                "set cr_time = now() - interval '2' day \n" +
                "where user_id = ? ",
            userId);
    }

    @Test
    public void testExpertiseGainNegative() {
        int hid = 1;
        mockStdHidNids(hid);

        // add grade
        expertiseMvc.updateExpertise(USER_ID, AgitationEntity.MODEL, MODEL_ID, hid,
            List.of(AgitationType.MODEL_GRADE, AgitationType.MODEL_GRADE_TEXT), null);

        // check regular gain
        long gain = MODEL_GRADE_CREATE_COST + MODEL_GRADE_TEXT_COST;
        ExpertiseGainDto expertiseGain = expertiseMvc.getExpertiseGain(USER_ID);
        assertGain(expertiseGain, AgitationType.MODEL_GRADE_TEXT, gain, gain, STD_EXP_NID_MOCKED);

        assertSimpleGainAdded(STD_EXP_NID_MOCKED, gain);

        // check no more gain exists
        assertNoGain(USER_ID);

        assertAgitationCancelSize(USER_ID, 2);
        assertAgitations(USER_ID, COMPLETED, MODEL_ID, AgitationType.MODEL_GRADE, AgitationType.MODEL_GRADE_TEXT);

        // remove grade text
        expertiseMvc.updateExpertise(USER_ID, AgitationEntity.MODEL, MODEL_ID, hid,
            null, List.of(AgitationType.MODEL_GRADE_TEXT));

        assertSimpleGainAdded(STD_EXP_NID_MOCKED, MODEL_GRADE_CREATE_COST);

        // now gain is really negative
        // check no more gain exists
        assertNoGain(USER_ID);

        assertAgitationCancelSize(USER_ID, 1);
        assertAgitations(USER_ID, COMPLETED, MODEL_ID, AgitationType.MODEL_GRADE);
    }

    @Test
    public void testExpertiseGainComplexMixed() {
        int hid = 1;
        mockStdHidNids(hid);

        // +base+text
        // -text
        // +text+photo
        // -photo
        // result = base + text
        expertiseMvc.updateExpertise(USER_ID, AgitationEntity.MODEL, MODEL_ID, hid,
            List.of(AgitationType.MODEL_GRADE, AgitationType.MODEL_GRADE_TEXT), null);
        expertiseMvc.updateExpertise(USER_ID, AgitationEntity.MODEL, MODEL_ID, hid,
            null, List.of(AgitationType.MODEL_GRADE_TEXT));
        expertiseMvc.updateExpertise(USER_ID, AgitationEntity.MODEL, MODEL_ID, hid,
            List.of(AgitationType.MODEL_GRADE_TEXT, AgitationType.MODEL_GRADE_PHOTO), null);
        expertiseMvc.updateExpertise(USER_ID, AgitationEntity.MODEL, MODEL_ID, hid,
            null, List.of(AgitationType.MODEL_GRADE_PHOTO));

        // check gain
        assertNoGain(USER_ID);
    }

    @Test
    public void testExpertiseGainComplexMixed2() {
        int hid = 1;
        mockStdHidNids(hid);

        // +base+text
        // -text
        // +text+photo
        // result = base + text
        expertiseMvc.updateExpertise(USER_ID, AgitationEntity.MODEL, MODEL_ID, hid,
            List.of(AgitationType.MODEL_GRADE, AgitationType.MODEL_GRADE_TEXT), null);
        expertiseMvc.updateExpertise(USER_ID, AgitationEntity.MODEL, MODEL_ID, hid,
            null, List.of(AgitationType.MODEL_GRADE_TEXT));
        expertiseMvc.updateExpertise(USER_ID, AgitationEntity.MODEL, MODEL_ID, hid,
            List.of(AgitationType.MODEL_GRADE_TEXT, AgitationType.MODEL_GRADE_PHOTO), null);

        // check gain
        long gain = MODEL_GRADE_TEXT_COST + GRADE_PHOTO_COST;
        long total = gain + MODEL_GRADE_CREATE_COST;
        ExpertiseGainDto expertiseGain = expertiseMvc.getExpertiseGain(USER_ID);
        assertGain(expertiseGain, AgitationType.MODEL_GRADE_PHOTO, gain, total, STD_EXP_NID_MOCKED);

        assertSimpleGainAdded(STD_EXP_NID_MOCKED, total);
    }

    @Test
    public void testExpertiseGainComplexMixed3() {
        // add some start points, then -text+photo - should calculate well
        int hid = 1;
        mockStdHidNids(hid);

        long basePoints = 100;
        authorSaasMocks.mockExpertise(authorSaasMocks.buildSaasExp(basePoints, STD_EXP_NID_MOCKED));

        // -text
        // +photo
        // result = base - text + photo
        expertiseMvc.updateExpertise(USER_ID, AgitationEntity.MODEL, MODEL_ID, hid,
            null, List.of(AgitationType.MODEL_GRADE_TEXT));
        expertiseMvc.updateExpertise(USER_ID, AgitationEntity.MODEL, MODEL_ID, hid,
            List.of(AgitationType.MODEL_GRADE_PHOTO), null);

        // check gain
        long gain = GRADE_PHOTO_COST;
        long total = basePoints + gain - MODEL_GRADE_TEXT_COST;
        ExpertiseGainDto expertiseGain = expertiseMvc.getExpertiseGain(USER_ID);
        assertGain(expertiseGain, AgitationType.MODEL_GRADE_PHOTO, gain, total, STD_EXP_NID_MOCKED);

        assertSimpleGainAdded(STD_EXP_NID_MOCKED, total, false);
    }

    @Test
    public void testExpertiseGainComplexMixed4() {
        // pretend saas index is not ready, but we store only -text +photo +text
        // then gain would be +photo +text, and expertise 0->20
        int hid = 1;
        mockStdHidNids(hid);

        // -text
        // +photo
        // +text
        // result = base + photo, gain = +photo+text
        expertiseMvc.updateExpertise(USER_ID, AgitationEntity.MODEL, MODEL_ID, hid,
            null, List.of(AgitationType.MODEL_GRADE_TEXT));
        expertiseMvc.updateExpertise(USER_ID, AgitationEntity.MODEL, MODEL_ID, hid,
            List.of(AgitationType.MODEL_GRADE_PHOTO), null);
        expertiseMvc.updateExpertise(USER_ID, AgitationEntity.MODEL, MODEL_ID, hid,
            List.of(AgitationType.MODEL_GRADE_TEXT), null);

        // check gain
        long gain = GRADE_PHOTO_COST + MODEL_GRADE_TEXT_COST;
        long total = gain - MODEL_GRADE_TEXT_COST;
        ExpertiseGainDto expertiseGain = expertiseMvc.getExpertiseGain(USER_ID);
        assertGainBase(expertiseGain, AgitationType.MODEL_GRADE_PHOTO, gain, total, STD_EXP_NID_MOCKED);
        assertLevel(expertiseGain.getInitialExpertise(), USER_ID, STD_EXP_NID_MOCKED[0], 0);

        assertSimpleGainAdded(STD_EXP_NID_MOCKED, total, false);
    }

    @Test
    public void testExpertiseGainComplexMixed5() {
        int hid = 1;
        mockStdHidNids(hid);

        // +base+text
        // -text+photo
        // result = base +photo
        expertiseMvc.updateExpertise(USER_ID, AgitationEntity.MODEL, MODEL_ID, hid,
            List.of(AgitationType.MODEL_GRADE, AgitationType.MODEL_GRADE_TEXT), null);
        expertiseMvc.updateExpertise(USER_ID, AgitationEntity.MODEL, MODEL_ID, hid,
            List.of(AgitationType.MODEL_GRADE_PHOTO), List.of(AgitationType.MODEL_GRADE_TEXT));

        // check gain
        long gain = GRADE_PHOTO_COST;
        long total = gain + MODEL_GRADE_CREATE_COST;
        ExpertiseGainDto expertiseGain = expertiseMvc.getExpertiseGain(USER_ID);
        assertGain(expertiseGain, AgitationType.MODEL_GRADE_PHOTO, gain, total, STD_EXP_NID_MOCKED);

        assertSimpleGainAdded(STD_EXP_NID_MOCKED, total);

        //then
        // +text-photo
        // result = base +text
        expertiseMvc.updateExpertise(USER_ID, AgitationEntity.MODEL, MODEL_ID, hid,
            List.of(AgitationType.MODEL_GRADE_TEXT), List.of(AgitationType.MODEL_GRADE_PHOTO));

        // check gain
        gain = MODEL_GRADE_TEXT_COST;
        total = gain + MODEL_GRADE_CREATE_COST;
        expertiseGain = expertiseMvc.getExpertiseGain(USER_ID);
        assertGain(expertiseGain, AgitationType.MODEL_GRADE_TEXT, gain, total, STD_EXP_NID_MOCKED);

        assertSimpleGainAdded(STD_EXP_NID_MOCKED, total);
    }

    @Test
    public void testExpertiseGainTooBad() {
        // zero start points, then -text+photo - should be 0 in all expertise and no gain
        int hid = 1;
        mockStdHidNids(hid);

        // -text
        // +photo
        // result = base - text + photo
        expertiseMvc.updateExpertise(USER_ID, AgitationEntity.MODEL, MODEL_ID, hid,
            null, List.of(AgitationType.MODEL_GRADE_TEXT));
        expertiseMvc.updateExpertise(USER_ID, AgitationEntity.MODEL, MODEL_ID, hid,
            List.of(AgitationType.MODEL_GRADE_PHOTO), null);

        // check gain
        assertNoGain(USER_ID);

        assertSimpleGainAdded(new long[0], 0, true);
    }

    @Test
    public void testUserExpertiseForMail() {
        authorSaasMocks.mockExpertise("1-42|4-12|6-99999");

        List<UserExpertiseMailDto> expertiseList = expertiseMvc.getUserExpertiseMail(USER_ID);

        assertExpertiseMail(expertiseList.get(0), 11, 98809, null, "Друг зверей");
        assertExpertiseMail(expertiseList.get(1), 2, 22, 30L, "Гурман");
        assertExpertiseMail(expertiseList.get(2), 1, 12, 20L, "Знаток уюта");
        assertExpertiseMail(expertiseList.get(3), 1, 0, 20L, "Магазинный критик");
    }

    public void assertExpertiseMail(UserExpertiseMailDto userExpertise,
                                    long level,
                                    long levelValue,
                                    Long levelTotalValue,
                                    String expertiseName) {
        assertEquals(level, userExpertise.getLevel());
        assertEquals(levelValue, userExpertise.getLevelValue());
        assertEquals(levelTotalValue, userExpertise.getLevelTotalValue());
        assertEquals(expertiseName, userExpertise.getName());
    }

    private void assertSimpleGainAdded(long[] expertises, long value) {
        assertSimpleGainAdded(expertises, value, true);
    }

    private void assertSimpleGainAdded(long[] expertises, long value, boolean needMockSaas) {
        assertSimpleGainAdded(Arrays.stream(expertises)
                .mapToObj(x -> expectLevel(x, value))
                .collect(Collectors.toList()),
            needMockSaas);
    }

    private void assertSimpleGainAdded(List<UserExpertiseDto> expectedList, boolean needMockSaas) {
        // check, that gained content used in total user expertises
        // simplify by removing saas data
        if (needMockSaas) {
            authorSaasMocks.mockExpertise(null);
        }

        List<UserExpertiseDto> expertiseList = expertiseMvc.getExpertiseList(USER_ID);
        assertAllExpertisesLoaded(expertiseList);
        for (int idx = 0; idx < expectedList.size(); idx++) {
            UserExpertiseDto expected = expectedList.get(idx);
            assertLevel(expertiseList.get(idx), expected.getUserId(), expected.getExpertiseId(), expected.getValue());
        }
        assertNoExpertise(expertiseList, expertiseList.size());
    }

    private void assertAgitationCancelSize(long userId, int count){
        List<AgitationCancel> agitations = agitationService.getCancelledAgitations(AgitationUser.uid(userId), null);
        assertEquals(count, agitations.size());
    }

    private void assertAgitations(long userId, AgitationCancelReason reason, String entityId, AgitationType... types) {
        List<AgitationCancel> agitations = agitationService.getCancelledAgitations(AgitationUser.uid(userId), null);
        List<AgitationType> foundTypes = agitations.stream()
            .filter(x -> x.getReason() == reason)
            .map(AgitationCancel::getAgitation)
            .filter(x -> x.getEntityId().equals(entityId))
            .map(Agitation::getTypeEnum)
            .collect(Collectors.toList());
        assertEquals(types.length, foundTypes.size());
        Set.of(types).containsAll(foundTypes);
    }

    private UserExpertiseDto expectLevel(long expertiseId, long value) {
        return new UserExpertiseDto(USER_ID, expertiseId, value, value, 1);
    }

    private void assertAllExpertisesLoaded(List<UserExpertiseDto> expertiseList) {
        assertEquals(EXPECTED_EXPERTISE_COUNT, expertiseList.size(), "All expertises loaded");
    }

    private void assertGain(ExpertiseGainDto expertiseGain,
                            AgitationType type,
                            long gain,
                            long result,
                            long[] expertises) {
        assertGainBase(expertiseGain, type, gain, result, expertises);
        assertLevel(expertiseGain.getInitialExpertise(), USER_ID, expertises[0], result - gain);
    }

    private void assertGainBase(ExpertiseGainDto expertiseGain,
                                AgitationType type,
                                long gain,
                                long result,
                                long[] expertises) {
        assertEquals(gain, expertiseGain.getGain());
        assertEquals(type.value(), expertiseGain.getAgitationType());
        assertLevel(expertiseGain.getExpertise(), USER_ID, expertises[0], result);
    }

    private void assertNoGain(long userId) {
        ExpertiseGainDto expertiseGain = expertiseMvc.getExpertiseGain(userId);
        assertEquals(0L, expertiseGain.getGain());
        assertNull(expertiseGain.getAgitationType());
        assertNull(expertiseGain.getExpertise());
        assertNull(expertiseGain.getInitialExpertise());
        assertNull(expertiseGain.getExpertiseSet());
    }

    private void assertLevel(UserExpertiseDto userExpertise, long userId, long expertiseId, long value) {
        assertEquals(userId, userExpertise.getUserId());
        assertEquals(expertiseId, userExpertise.getExpertiseId());
        assertEquals(value, userExpertise.getValue());
        assertEquals(calcLevel(userExpertise.getValue()), userExpertise.getLevel());
    }

    private void assertNoExpertise(List<UserExpertiseDto> data, int start) {
        for (int i = start; i < data.size(); i++) {
            UserExpertiseDto userExpertise = data.get(i);
            assertEquals(0, userExpertise.getValue());
            assertEquals(1, userExpertise.getLevel());
        }
    }

    private void mockStdHidNids(int hid) {
        addRootNids(hid, 54430, 54432);
    }

    @Test
    public void testLevels() {
        assertEquals(1, calcLevel(-5));
        assertEquals(1, calcLevel(0));
        assertEquals(1, calcLevel(10));
        assertEquals(1, calcLevel(19));
        assertEquals(2, calcLevel(20));
        assertEquals(2, calcLevel(49));
        assertEquals(3, calcLevel(50));
        assertEquals(3, calcLevel(51));
        assertEquals(7, calcLevel(429));
        assertEquals(8, calcLevel(430));
        assertEquals(8, calcLevel(431));
        assertEquals(8, calcLevel(599));
        assertEquals(9, calcLevel(600));
        assertEquals(10, calcLevel(1000));
        assertEquals(11, calcLevel(1200));
    }

    private int calcLevel(long value) {
        long[] levelBounds = {0, 20, 50, 90, 140, 210, 300, 430, 600, 830, 1190};
        return ExpertiseDictionaryService.findLevel(levelBounds, value);
    }
}
