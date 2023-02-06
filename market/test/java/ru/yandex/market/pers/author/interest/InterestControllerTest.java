package ru.yandex.market.pers.author.interest;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.author.PersAuthorTest;
import ru.yandex.market.pers.author.cache.InterestCache;
import ru.yandex.market.pers.author.client.api.dto.InterestDto;
import ru.yandex.market.pers.author.mock.mvc.InterestMvcMocks;
import ru.yandex.market.pers.author.client.api.model.VideoUserType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Damir Shagaev / damir-vvlpx@ / 31.01.2020
 */
public class InterestControllerTest extends PersAuthorTest {

    private static final String USER_ID = String.valueOf(538);
    private static final int INTERESTS_COUNT = 19;
    private static final int INTEREST_ID = 500;

    @Autowired
    private InterestMvcMocks interestMvcMocks;

    @Autowired
    private InterestCache interestCache;

    @Test
    public void testGetUserInterests() throws Exception {
        long[] expectedInterestIds = {2, 0, 4};
        interestMvcMocks.saveUserInterestsByIds(USER_ID, expectedInterestIds);
        interestMvcMocks.saveUserInterestsByIds(USER_ID + 1, new long[]{4});
        interestMvcMocks.saveUserInterestsByIds(USER_ID + 2, new long[]{3, 2});

        List<Long> userInterests = interestMvcMocks.getUserInterests(USER_ID);

        assertSelectedInterest(userInterests, expectedInterestIds);
    }

    @Test
    public void testGetUserInterestsWithUserType() throws Exception {
        // add interest for user with other type
        jdbcTemplate.update("INSERT INTO PERS.USER_INTEREST(USER_ID, USER_TYPE, INTEREST_ID) " +
                "values (?, ?, ?)", USER_ID, 1, 2);
        long[] expectedInterestIds = {0, 1};
        interestMvcMocks.saveUserInterestsByIds(USER_ID, expectedInterestIds);
        interestMvcMocks.saveUserInterestsByIds(USER_ID + 1, new long[]{2});

        List<Long> userInterests = interestMvcMocks.getUserInterests(USER_ID);

        assertSelectedInterest(userInterests, expectedInterestIds);
    }

    @Test
    public void testGetUserInterestsWithoutSelectedInterests() throws Exception {
        List<Long> userInterests = interestMvcMocks.getUserInterests(USER_ID);

        assertEquals(0, userInterests.size());
    }

    @Test
    public void testSaveUserInterests() throws Exception {
        long[] expectedInterestIds = {2, 1, 4};
        interestMvcMocks.saveUserInterestsByIds(USER_ID, expectedInterestIds);

        List<Long> userInterests = interestMvcMocks.getUserInterests(USER_ID);
        assertSelectedInterest(userInterests, expectedInterestIds);
    }

    @Test
    public void testSaveUserInterestsWithNewInterests() throws Exception {
        long[] expectedInterestIds = new long[]{2, 1, 4};
        interestMvcMocks.saveUserInterestsByIds(USER_ID, expectedInterestIds);
        List<Long> userInterests = interestMvcMocks.getUserInterests(USER_ID);
        assertSelectedInterest(userInterests, expectedInterestIds);

        expectedInterestIds = new long[]{0, 4};
        interestMvcMocks.saveUserInterestsByIds(USER_ID, expectedInterestIds);
        userInterests = interestMvcMocks.getUserInterests(USER_ID);
        assertSelectedInterest(userInterests, expectedInterestIds);

        expectedInterestIds = new long[]{3};
        interestMvcMocks.saveUserInterestsByIds(USER_ID, expectedInterestIds);
        userInterests = interestMvcMocks.getUserInterests(USER_ID);
        assertSelectedInterest(userInterests, expectedInterestIds);
    }

    private void assertSelectedInterest(List<Long> userInterests, long[] ids) {
        assertEquals(ids.length, userInterests.size());
        List<Long> interestIds = Arrays.stream(ids).boxed().sorted().collect(Collectors.toList());
        assertEquals(interestIds, userInterests);
    }

    @Test
    public void testSaveUserInterestsWithDeleteAllInterests() throws Exception {
        long[] expectedInterestIds = new long[]{0, 1, 2, 3, 4};
        interestMvcMocks.saveUserInterestsByIds(USER_ID, expectedInterestIds);
        List<Long> userInterests = interestMvcMocks.getUserInterests(USER_ID);
        assertSelectedInterest(userInterests, expectedInterestIds);

        expectedInterestIds = new long[]{};
        interestMvcMocks.saveUserInterestsByIds(USER_ID, expectedInterestIds);
        userInterests = interestMvcMocks.getUserInterests(USER_ID);
        assertSelectedInterest(userInterests, expectedInterestIds);
    }
    @Test
    public void testSaveUserInterestsWithAllIds() throws Exception {
        Long[] allIds = interestMvcMocks.getAllInterests().stream().map(InterestDto::getInterestId).toArray(Long[]::new);
        long[] expectedInterestIds = new long[allIds.length];
        for (int i = 0; i < allIds.length; i++) {
            expectedInterestIds[i] = allIds[i];
        }
        interestMvcMocks.saveUserInterestsByIds(USER_ID, expectedInterestIds);
        List<Long> userInterests = interestMvcMocks.getUserInterests(USER_ID);
        assertSelectedInterest(userInterests, expectedInterestIds);
    }

    @Test
    public void testGetAllInterestsWithHids() throws Exception {
        List<InterestDto> interests = interestMvcMocks.getAllInterests();

        assertEquals(2761, interests.stream().mapToInt(x -> x.getInterestHids().size()).sum());
        assertEquals("Дети", interests.get(2).getInterestName());
        assertEquals(2, interests.get(2).getInterestId());
        assertEquals(310, interests.get(2).getInterestHids().size());
        assertEquals(90748, interests.get(2).getInterestHids().get(0));
        assertEquals(90783, interests.get(2).getInterestHids().get(1));
    }

    @Test
    public void testGetAllInterestsWithNullIcons() throws Exception {
        List<InterestDto> interests = interestMvcMocks.getAllInterests();
        assertEquals(INTERESTS_COUNT, interests.stream().filter(x -> x.getIcon() == null).count());

        // add icon for each interest
        updateAllInterestIcons("INSERT INTO pers.interest_icon(interest_id, namespace, group_id, image_name) " +
                "SELECT id, id::text, id::text, id::text FROM pers.interest");

        // delete content for interest icon
        updateAllInterestIcons("DELETE FROM pers.interest_icon where interest_id > 0");

        String response = interestMvcMocks.getAllInterestResponse(status().is2xxSuccessful());
        assertEquals(2, response.split("icon").length); // only 1 occurrence

        interests = interestMvcMocks.getAllInterests();
        assertEquals(1, interests.stream().filter(x -> x.getIcon() != null).count());

        // delete all content for interest icon
        updateAllInterestIcons("DELETE FROM pers.interest_icon");

        response = interestMvcMocks.getAllInterestResponse(status().is2xxSuccessful());
        assertFalse(response.contains("icon"));

        interests = interestMvcMocks.getAllInterests();
        assertEquals(0, interests.stream().filter(x -> x.getIcon() != null).count());
    }

    @Test
    public void testGetAllInterestsWithCorrectIcon() throws Exception {
        updateAllInterestIcons("DELETE FROM pers.interest_icon");

        List<InterestDto> interests = interestMvcMocks.getAllInterests();
        assertEquals(0, interests.stream().filter(x -> x.getIcon() != null).count());

        // add icon for each interest
        updateAllInterestIcons("INSERT INTO pers.interest_icon(interest_id, namespace, group_id, image_name) " +
                "SELECT id, id::text, id::text, id::text FROM pers.interest");

        interests = interestMvcMocks.getAllInterests();
        assertEquals(0, interests.stream()
                .filter(x -> {
                    String interestId = String.valueOf(x.getInterestId());
                    return !(interestId.equals(x.getIcon().getNamespace()) &&
                           interestId.equals(x.getIcon().getGroupId()) &&
                           interestId.equals(x.getIcon().getImageName()));
                }).count());
    }

    private void updateAllInterestIcons(String query) {
        jdbcTemplate.update(query);
        invalidateCache();
    }

    @Test
    public void testGetAllInterestsName() throws Exception {
        List<String> names = Arrays.asList("Авто", "Гаджеты", "Дети", "Для дачи", "Домашний декор",
            "Здоровье", "Книги", "Компьютерные игры", "Кошки", "Красота", "Кулинария", "Мода, одежда",
            "Музыка", "Охота, рыбалка", "Путешествия", "Ремонт", "Творчество, рукоделие", "Собаки",
            "Спорт и активный отдых");

        List<InterestDto> interests = interestMvcMocks.getAllInterests();

        assertEquals(names, interests.stream().map(InterestDto::getInterestName).collect(Collectors.toList()));
    }

    @Test
    public void testGetAllInterestsWithCache() throws Exception {
        List<InterestDto> interests = interestMvcMocks.getAllInterests();
        assertEquals(INTERESTS_COUNT, interests.size());

        addNewInterest(INTEREST_ID, "Python", 62807);

        // cache is working
        interests = interestMvcMocks.getAllInterests();
        assertEquals(INTERESTS_COUNT, interests.size());

        interestCache.cleanInterestsCache();

        interests = interestMvcMocks.getAllInterests();
        assertEquals(INTERESTS_COUNT + 1, interests.size());

        cleanInterestHidTable(INTEREST_ID);
    }

    private void addNewInterest(long interestId, String interestName, long interestHid) {
        jdbcTemplate.update("INSERT INTO PERS.INTEREST(ID, NAME) " +
                "values (?, ?)", interestId, interestName);
        jdbcTemplate.update("INSERT INTO PERS.INTEREST_HID_POLICY(INTEREST_ID, HID) " +
                "values (?, ?)", interestId, interestHid);
    }

    private void cleanInterestHidTable(long interestId) {
        jdbcTemplate.update("DELETE FROM PERS.INTEREST WHERE ID=?", interestId);
        jdbcTemplate.update("DELETE FROM PERS.INTEREST_HID_POLICY " +
                "WHERE INTEREST_ID=?", interestId);
        interestCache.cleanInterestsCache();
    }

    @Test
    public void testCleanInterestsDictionary() throws Exception {
        List<InterestDto> interests = interestMvcMocks.getAllInterests();
        assertEquals(INTERESTS_COUNT, interests.size());

        addNewInterest(INTEREST_ID, "Jedi Force", 62807);

        interests = interestMvcMocks.getAllInterests();
        assertEquals(INTERESTS_COUNT, interests.size());

        interestMvcMocks.cleanInterestsDictionary();

        interests = interestMvcMocks.getAllInterests();
        assertEquals(INTERESTS_COUNT + 1, interests.size());

        cleanInterestHidTable(INTEREST_ID);
    }

    @Test
    public void testGetUserInterestsWithCacheByUserId() throws Exception {
        List<Long> userInterests = interestMvcMocks.getUserInterests(USER_ID);
        assertEquals(0, userInterests.size());

        // cache invalidated
        interestMvcMocks.saveUserInterestsByIds(USER_ID, new long[]{2, 1, 4});

        userInterests = interestMvcMocks.getUserInterests(USER_ID);
        assertEquals(3, userInterests.size());
    }

    @Test
    public void testGetUserInterestsWithCacheInvalidationByUserId() throws Exception {
        long[] expectedInterestIds = new long[]{2, 1, 4};
        interestMvcMocks.saveUserInterestsByIds(USER_ID, expectedInterestIds);

        List<Long> userInterests = interestMvcMocks.getUserInterests(USER_ID);
        assertSelectedInterest(userInterests, expectedInterestIds);

        // delete all user interests
        jdbcTemplate.update("DELETE FROM pers.user_interest WHERE user_type=? AND user_id=?",
                VideoUserType.UID.getValue(), USER_ID);

        // cache is working
        userInterests = interestMvcMocks.getUserInterests(USER_ID);
        assertSelectedInterest(userInterests, expectedInterestIds);

        invalidateCache();

        userInterests = interestMvcMocks.getUserInterests(USER_ID);
        assertSelectedInterest(userInterests, new long[]{});
    }

    @Test
    public void testSaveUserInterestsWithIllegalId() throws Exception {
        interestMvcMocks.saveUserInterestsByIds(USER_ID, new long[]{2, 1, 1000}, status().is4xxClientError());
        interestMvcMocks.saveUserInterestsByIds(USER_ID, new long[]{-2, 1, 3}, status().is4xxClientError());
        interestMvcMocks.saveUserInterestsByIds(USER_ID, new String[]{"Java", "Forever"}, status().is4xxClientError());
    }
}
