package ru.yandex.market.pers.grade.web.grade;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.pers.grade.api.AchievementsController;
import ru.yandex.market.pers.grade.client.model.achievements.UserAchievement;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author korolyov
 * 22.09.17
 */
public class AchievementsBaseControllerTest extends GradeControllerBaseTest {
    static final long ANOTHER_MODEL_ID = 11111111111L;
    static final long FIRST_BIRD_MODEL_ID = 21111111111L;
    static final long ANOTHER_SHOP_ID = 1L;
    private static final long FIRST_BIRD_SHOP_ID = 2L;
    static final String ADD_SHOP_GRADE_FOR_ACHIEVEMENTS_BODY =
        ADD_SHOP_GRADE_BODY.replace(String.valueOf(SHOP_ID), String.valueOf(FIRST_BIRD_SHOP_ID));
    static final String ADD_MODEL_GRADE_FOR_ACHIEVEMENTS_BODY =
        ADD_MODEL_GRADE_BODY.replace(String.valueOf(MODEL_ID), String.valueOf(FIRST_BIRD_MODEL_ID));
    static final String NEW_PRO_MODEL_GRADE = "вообще не красивенькая";
    static final String NEW_PRO_SHOP_GRADE = "вообще не супер";

    static final String ADD_MODEL_GRADE_WITHOUT_TEXT_BODY = "{\n" +
        "    \"entity\": \"opinion\",\n" +
        "    \"anonymous\": 0,\n" +
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
        "    \"photos\": [\n" +
        "        {\n" +
        "            \"entity\": \"photo\",\n" +
        "            \"groupId\": \"abcde\",\n" +
        "            \"imageName\": \"iuirghreg\"\n" +
        "        }\n" +
        "    ],\n" +
        "    \"product\": {\n" +
        "        \"entity\": \"product\",\n" +
        "        \"id\": " + MODEL_ID + " \n" +
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

    static final String ADD_SHOP_GRADE_WITHOUT_TEXT_BODY = "{\n" +
        "    \"entity\": \"opinion\",\n" +
        "    \"anonymous\": 0,\n" +
        "    \"recommend\": true,\n" +
        "    \"averageGrade\": 5,\n" +
        "    \"factors\": [\n" +
        "    ],\n" +
        "    \"photos\": [\n" +
        "    ],\n" +
        "    \"shop\": {\n" +
        "        \"entity\": \"shop\",\n" +
        "        \"id\": " + SHOP_ID + " \n" +
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


    @Test
    public void getBulkUserAchievementsTest() throws Exception {
        invokeAndRetrieveResponse(
            get("/api/achievements/users?userId=" + FAKE_USER + "&userId=" + FAKE_USER + 1)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
    }

    @Test
    public void getBulkUserAchievementsTooManyUsersTest() throws Exception {
        invokeAndRetrieveResponse(
            get("/api/achievements/users?" +
                String.join(
                    "&",
                    Collections.nCopies(AchievementsController.DEFAULT_MAX_BATCH_SIZE + 1, "userId=123")))
                .accept(MediaType.APPLICATION_JSON),
            status().isBadRequest());
    }

    @Test
    public void getAllAchievementsTest() throws Exception {
        invokeAndRetrieveResponse(
            get("/api/achievements/dictionary")
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
    }

    @Test
    public void resetAllAchievementsCacheTest() throws Exception {
        invokeAndRetrieveResponse(
            put("/api/achievements/dictionary/reset")
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
    }

    protected void assertListAchievementsEqualsWithOrder(List<UserAchievement> expected, List<UserAchievement> actual, boolean exactOrder) {
        if (!exactOrder) {
            expected.sort(Comparator.comparingInt(UserAchievement::getAchievementId));
            actual.sort(Comparator.comparingInt(UserAchievement::getAchievementId));
        }
        assertEquals(expected, actual);
    }

    protected void assertListAchievementsEquals(List<UserAchievement> expected, List<UserAchievement> actual) {
        assertListAchievementsEqualsWithOrder(expected, actual, false);
    }

}
