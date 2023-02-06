package ru.yandex.market.pers.grade.web.grade;

import java.util.List;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Test;

import ru.yandex.market.pers.grade.client.dto.GradePager;
import ru.yandex.market.pers.grade.client.dto.grade.GradeResponseDto;
import ru.yandex.market.pers.grade.client.model.GradeType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserGradesTest extends GradeControllerBaseTest {
    // can check grade votes since mat.view is changed with view for tests
    public static final int EXPECTED_VOTES_AGREE = 1;
    public static final int EXPECTED_VOTES_REJECT = 3;
    private static int i = 0;

    @Test
    public void getUserGradesByUidTest() throws Exception {
        final int gradesCount = 3;
        List<GradeResponseDtoImpl> userGrades = performGetUserGradesByUid();
        assertTrue(userGrades.isEmpty());
        createUserGradesForUid(String.valueOf(FAKE_USER), gradesCount);
        List<GradeResponseDtoImpl> grades = performGetUserGradesByUid();
        assertEquals(gradesCount, grades.size());
    }

    @Test
    public void getUserGradesByYandexUidTest() throws Exception {
        final int gradesCount = 3;
        List<GradeResponseDtoImpl> userGrades = performGetUserGradesByYandexUid();
        assertTrue(userGrades.isEmpty());
        createUserGradesForYandexUid(FAKE_YANDEXUID, gradesCount);
        List<GradeResponseDtoImpl> grades =  performGetUserGradesByYandexUid();
        assertEquals(gradesCount, grades.size());
    }

    @Test
    public void getUserPagedGradesByUidTest() throws Exception {
        final int gradesCount = 3;
        final int defaultPageSize = 2;
        List<GradeResponseDtoImpl> userGrades = performGetUserGradesByUid();
        assertTrue(userGrades.isEmpty());
        createUserGradesForUid(String.valueOf(FAKE_USER), gradesCount);
        String response = invokeAndRetrieveResponse(
                get("/api/grade/user/pager?uid=" + FAKE_USER + "&page_size=" + defaultPageSize),
                status().is2xxSuccessful()
        );
        GradePager<GradeResponseDtoImpl> grades = objectMapper.readValue(response, new TypeReference<GradePager<GradeResponseDtoImpl>>() {
        });
        assertEquals(defaultPageSize, grades.getData().size());
    }

    @Test
    public void getUserPagedGradesByYandexUidTest() throws Exception {
        final int gradesCount = 3;
        final int defaultPageSize = 2;
        List<GradeResponseDtoImpl> userGrades = performGetUserGradesByYandexUid();
        assertTrue(userGrades.isEmpty());
        createUserGradesForYandexUid(FAKE_YANDEXUID, gradesCount);
        String response = invokeAndRetrieveResponse(
                get("/api/grade/user/pager?yandexuid=" + FAKE_YANDEXUID + "&page_size=" + defaultPageSize),
                status().is2xxSuccessful()
        );
        GradePager<GradeResponseDtoImpl> grades = objectMapper.readValue(response, new TypeReference<GradePager<GradeResponseDtoImpl>>() {
        });
        assertEquals(defaultPageSize, grades.getData().size());
    }

    @Test
    public void getUserPagedGradesByUidWithFirstReviewIdTest() throws Exception {
        final int gradesCount = 5;
        final int defaultPageSize = 5;
        List<GradeResponseDtoImpl> userGrades = performGetUserGradesByUid();
        assertTrue(userGrades.isEmpty());
        createUserGradesForUid(String.valueOf(FAKE_USER), gradesCount);

        // get with first review id
        long firstReviewId = performGetUserGradesByUid().get(gradesCount - 1).getId();
        GradePager<GradeResponseDtoImpl> grades = getUserPagedGradesWithFirstReviewId("UID",
                String.valueOf(FAKE_USER), firstReviewId, null, defaultPageSize);

        assertEquals(defaultPageSize, grades.getData().size());
        assertEquals(firstReviewId, grades.getData().get(0).getId().longValue());
        assertEquals(1, grades.getData().stream().filter(x -> x.getId().equals(firstReviewId)).count());
        // check sorted order by date
        assertTrue(grades.getData().get(0).getCreationDate().compareTo(grades.getData().get(1).getCreationDate()) <= 0);
        assertSortedOrderByCreationDate(grades, 2);
    }

    @Test
    public void getUserPagedGradesByYandexUidWithFirstReviewIdTest() throws Exception {
        final int gradesCount = 5;
        final int defaultPageSize = 5;
        List<GradeResponseDtoImpl> userGrades = performGetUserGradesByUid();
        assertTrue(userGrades.isEmpty());
        createUserGradesForYandexUid(FAKE_YANDEXUID, gradesCount);

        // get with first review id
        long firstReviewId = performGetUserGradesByYandexUid().get(gradesCount - 1).getId();
        GradePager<GradeResponseDtoImpl> grades = getUserPagedGradesWithFirstReviewId("YANDEXUID", FAKE_YANDEXUID,
                firstReviewId, null, defaultPageSize);

        assertEquals(defaultPageSize, grades.getData().size());
        assertEquals(firstReviewId, grades.getData().get(0).getId().longValue());
        assertEquals(1, grades.getData().stream().filter(x -> x.getId().equals(firstReviewId)).count());
        // check sorted order by date
        assertTrue(grades.getData().get(0).getCreationDate().compareTo(grades.getData().get(1).getCreationDate()) <= 0);
        assertSortedOrderByCreationDate(grades, 2);
    }

    @Test
    public void getUserPagedGradesByUidOrYandexUidWithFirstReviewIdTest() throws Exception {
        final int gradesCount = 5;
        final int defaultPageSize = 5;

        createUserGradesForUid(String.valueOf(FAKE_USER), gradesCount);
        createUserGradesForYandexUid(FAKE_YANDEXUID, gradesCount);

        // without first review id
        assertSortedOrderByCreationDate(
                getUserPagedGradesWithFirstReviewId("UID", String.valueOf(FAKE_USER), null, null, defaultPageSize), 1);
        assertSortedOrderByCreationDate(
                getUserPagedGradesWithFirstReviewId("YANDEXUID", FAKE_YANDEXUID, null, null, defaultPageSize), 1);
    }

    @Test
    public void getUserPagedGradesWithFilteredFirstReviewIdTest() throws Exception {
        final int gradesCount = 5;
        final int defaultPageSize = 5;

        // by uid
        assertTrue(performGetUserGradesByUid().isEmpty());
        createUserGradesForUid(String.valueOf(FAKE_USER), gradesCount - 1);
        long firstReviewIdByUid = addModelGradeForUid(String.valueOf(FAKE_USER)).getId();
        GradePager<GradeResponseDtoImpl> grades = getUserPagedGradesWithFirstReviewId("UID",
                String.valueOf(FAKE_USER), firstReviewIdByUid, GradeType.SHOP_GRADE, defaultPageSize);
        assertNotEquals(firstReviewIdByUid, grades.getData().get(0).getId().longValue());
        assertEquals(0, grades.getData().stream().filter(x -> x.getId().equals(firstReviewIdByUid)).count());

        // by yandexuid
        assertTrue(performGetUserGradesByYandexUid().isEmpty());
        createUserGradesForYandexUid(FAKE_YANDEXUID, gradesCount - 1);
        long firstReviewIdByYandexUid = addModelGradeForYandexUid(FAKE_YANDEXUID).getId();
        grades = getUserPagedGradesWithFirstReviewId("YANDEXUID", FAKE_YANDEXUID,
                firstReviewIdByYandexUid, GradeType.SHOP_GRADE, defaultPageSize);
        assertNotEquals(firstReviewIdByYandexUid, grades.getData().get(0).getId().longValue());
        assertEquals(0, grades.getData().stream().filter(x -> x.getId().equals(firstReviewIdByYandexUid)).count());
    }

    private void assertSortedOrderByCreationDate(GradePager<GradeResponseDtoImpl> grades, int startIndex) {
        assertTrue(IntStream.range(startIndex, grades.getData().size()).anyMatch(i ->
                grades.getData().get(i - 1).getCreationDate().compareTo(grades.getData().get(i).getCreationDate()) >= 0));
    }

    @Test
    public void getLastPageOfUserPagedGradesTest() throws Exception {
        final int gradesCount = 5;
        final int pageSize = 2;
        List<GradeResponseDtoImpl> userGrades = performGetUserGradesByYandexUid();
        assertTrue(userGrades.isEmpty());
        createUserGradesForYandexUid(FAKE_YANDEXUID, gradesCount);
        String response = invokeAndRetrieveResponse(
                get("/api/grade/user/pager?yandexuid=" + FAKE_YANDEXUID + "&page_num=3&page_size=" + pageSize),
                status().is2xxSuccessful()
        );
        GradePager<GradeResponseDtoImpl> grades = objectMapper.readValue(response, new TypeReference<GradePager<GradeResponseDtoImpl>>() {
        });
        assertEquals(gradesCount % pageSize, grades.getData().size());
    }

    @Test
    public void getNonexistentPageOfUserPagedGradesTest() throws Exception {
        final int gradesCount = 5;
        final int pageSize = 2;
        List<GradeResponseDtoImpl> userGrades = performGetUserGradesByYandexUid();
        assertTrue(userGrades.isEmpty());
        createUserGradesForYandexUid(FAKE_YANDEXUID, gradesCount);
        String response = invokeAndRetrieveResponse(
                get("/api/grade/user/pager?yandexuid=" + FAKE_YANDEXUID + "&page_num=100&page_size=" + pageSize),
                status().is2xxSuccessful()
        );
        GradePager<GradeResponseDtoImpl> grades = objectMapper.readValue(response, new TypeReference<GradePager<GradeResponseDtoImpl>>() {
        });
        assertEquals(gradesCount % pageSize, grades.getData().size());
    }

    @Test
    public void testKarmaVotesInUserGradesUid() throws Exception {
        assertTrue(performGetUserGradesByUid().isEmpty());
        long gradeId = addModelGradeForUid(Long.toString(FAKE_USER)).getId();
        createVotesForGrade(gradeId);

        String response = invokeAndRetrieveResponse(
            get("/api/grade/user/pager?uid=" + FAKE_USER),
            status().is2xxSuccessful()
        );
        GradePager<GradeResponseDtoImpl> userGrades = objectMapper.readValue(response, new TypeReference<GradePager<GradeResponseDtoImpl>>() {
        });

        assertEquals(1, userGrades.getData().size());
        assertEquals(1, userGrades.getPager().getCount());
        assertEquals(gradeId, (long) userGrades.getData().get(0).getId());
        assertEquals(EXPECTED_VOTES_AGREE, userGrades.getData().get(0).getVotes().getPositive());
        assertEquals(EXPECTED_VOTES_REJECT, userGrades.getData().get(0).getVotes().getNegative());
    }

    @Test
    public void testKarmaVotesInUserGradesYandexUid() throws Exception {
        assertTrue(performGetUserGradesByYandexUid().isEmpty());
        long gradeId = addModelGradeForYandexUid(FAKE_YANDEXUID).getId();
        createVotesForGrade(gradeId);

        String response = invokeAndRetrieveResponse(
            get("/api/grade/user/pager?yandexuid=" + FAKE_YANDEXUID),
            status().is2xxSuccessful()
        );
        GradePager<GradeResponseDtoImpl> userGrades = objectMapper.readValue(response, new TypeReference<GradePager<GradeResponseDtoImpl>>() {
        });

        assertEquals(1, userGrades.getData().size());
        assertEquals(1, userGrades.getPager().getCount());
        assertEquals(gradeId, (long) userGrades.getData().get(0).getId());
        assertEquals(EXPECTED_VOTES_AGREE, userGrades.getData().get(0).getVotes().getPositive());
        assertEquals(EXPECTED_VOTES_REJECT, userGrades.getData().get(0).getVotes().getNegative());
    }

    private void createVotesForGrade(long gradeId) throws Exception {
        createTestVotesForGrade(gradeId, EXPECTED_VOTES_AGREE, EXPECTED_VOTES_REJECT);
    }

    private GradeResponseDtoImpl addModelGradeForUid(String userId) throws Exception {
        return objectMapper.readValue(addModelGrade("UID", userId, generateModelGradeBody(), status().is2xxSuccessful()), GradeResponseDtoImpl.class);
    }

    private GradeResponseDtoImpl addModelGradeForYandexUid(String yandexUid) throws Exception {
        return objectMapper.readValue(addModelGrade("YANDEXUID", yandexUid, generateModelGradeBody(), status().is2xxSuccessful()), GradeResponseDtoImpl.class);
    }

    private void createUserGradesForUid(String user, int count) throws Exception {
        createUserGrades("UID", user, count);
    }

    private void createUserGradesForYandexUid(String user, int count) throws Exception {
        createUserGrades("YANDEXUID", user, count);
    }

    private void createUserGrades(String userType, String user, int count) throws Exception {
        for (int i = 0; i < count; i++) {
            if (i % 2 == 0) {
                createShopGrade(userType, user);
            } else {
                createModelGrade(userType, user);
            }
        }
    }

    private void createShopGrade(String userType, String user) throws Exception {
        addShopGrade(userType, user, generateShopGradeBody(), status().is2xxSuccessful());
    }

    private void createModelGrade(String userType, String user) throws Exception {
        addModelGrade(userType, user, generateModelGradeBody(), status().is2xxSuccessful());
    }

    public static void assertGradeVotes(GradeResponseDto response, int expectedAgree, int expectedReject){
        assertEquals(expectedAgree, response.getVotes().getPositive());
        assertEquals(expectedReject, response.getVotes().getNegative());
    }

    private String generateShopGradeBody() {
        return "{\n" +
                "    \"entity\": \"opinion\",\n" +
                "    \"anonymous\": 0,\n" +
                "    \"comment\": \"Магазин просто супер!!!\",\n" +
                "    \"pro\": \"супер\",\n" +
                "    \"contra\": \"не супер\",\n" +
                "    \"recommend\": true,\n" +
                "    \"averageGrade\": 5,\n" +
                "    \"factors\": [\n" +
                "    ],\n" +
                "    \"photos\": [\n" +
                "    ],\n" +
                "    \"shop\": {\n" +
                "        \"entity\": \"shop\",\n" +
                "        \"id\": " + (SHOP_ID + i++) + " \n" +
                "    },\n" +
                "    \"type\": 0,\n" +
                "    \"region\": {\n" +
                "        \"entity\": \"region\",\n" +
                "        \"id\": 213\n" +
                "    },\n" +
                "    \"ipRegion\": {\n" +
                "        \"entity\": \"region\",\n" +
                "        \"id\": 214\n" +
                "    }\n" +
                "}";
    }

    private String generateModelGradeBody() {
        return "{\n" +
                "    \"entity\": \"opinion\",\n" +
                "    \"anonymous\": 0,\n" +
                "    \"comment\": \"Модель просто супер!!!\",\n" +
                "    \"pro\": \"Красивенькая\",\n" +
                "    \"contra\": \"Сломалась\",\n" +
                "    \"usage\": 1,\n" +
                "    \"recommend\": true,\n" +
                "    \"averageGrade\": 5,\n" +
                "    \"factors\": [\n" +
                "        {\n" +
                "            \"entity\": \"opinionFactor\",\n" +
                "            \"id\": 0,\n" +
                "            \"value\": 1\n" +
                "        },\n" +
                "        {\n" +
                "            \"entity\": \"opinionFactor\",\n" +
                "            \"id\": 1,\n" +
                "            \"value\": 1\n" +
                "        },\n" +
                "        {\n" +
                "            \"entity\": \"opinionFactor\",\n" +
                "            \"id\": 2,\n" +
                "            \"value\": 5\n" +
                "        }\n" +
                "    ],\n" +
                "    \"product\": {\n" +
                "        \"entity\": \"product\",\n" +
                "        \"id\": " + (MODEL_ID + i++) + " \n" +
                "    },\n" +
                "    \"categoryId\": null,\n" +
                "    \"type\": 1,\n" +
                "    \"region\": {\n" +
                "        \"entity\": \"region\",\n" +
                "        \"id\": 213\n" +
                "    },\n" +
                "    \"ipRegion\": {\n" +
                "        \"entity\": \"region\",\n" +
                "        \"id\": 214\n" +
                "    }\n" +
                "}";
    }


}
