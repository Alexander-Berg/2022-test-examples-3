package ru.yandex.market.pers.grade.client;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.pers.grade.client.dto.DataListWithToken;
import ru.yandex.market.pers.grade.client.dto.GradeForCommentDto;
import ru.yandex.market.pers.grade.client.dto.GradeUpdatesToken;
import ru.yandex.market.pers.grade.client.dto.UserAchievementDto;
import ru.yandex.market.pers.grade.client.dto.grade.ShopGradeResponseDto;
import ru.yandex.market.pers.grade.client.dto.mailer.MailerModelGrade;
import ru.yandex.market.pers.grade.client.dto.mailer.MailerShopGrade;
import ru.yandex.market.pers.grade.client.mock.HttpClientMockHelpers;
import ru.yandex.market.pers.grade.client.model.Delivery;
import ru.yandex.market.pers.grade.client.model.GradeState;
import ru.yandex.market.pers.grade.client.model.GradeType;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.client.model.achievements.UserAchievement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpStatus.OK;
import static ru.yandex.market.pers.grade.client.mock.HttpClientMockHelpers.withQuery;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 09.08.2019
 */
public class GradeClientTest {
    public static final int UID = 1;
    private final HttpClient httpClient = mock(HttpClient.class);
    private final GradeClient gradeClient = new GradeClient("localhost", 1234,
        new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient)));

    @Test
    public void testPapiResponse() {
        HttpClientMockHelpers.mockResponseWithFile(httpClient, HttpStatus.SC_OK, "/data/papi_grades.json");

        DataListWithToken<ShopGradeResponseDto, GradeUpdatesToken> response = gradeClient
            .getPapiGradeUpdates(42, null);

        assertEquals(2, response.getData().size());
        assertEquals(1565348221000L, response.getToken().getLastTimestamp().longValue());
        assertEquals(59526622L, response.getToken().getLastGradeId().longValue());

        ShopGradeResponseDto grade = response.getData().get(0);
        assertEquals(53550752L, grade.getId().longValue());
        assertEquals(GradeType.SHOP_GRADE, grade.getType());
        assertEquals(GradeState.LAST, grade.getState());
        assertEquals(ModState.APPROVED, grade.getModState());
        assertEquals(Delivery.INSTORE, grade.getDelivery());

        grade = response.getData().get(1);
        assertEquals(59526622L, grade.getId().longValue());
        assertEquals(GradeType.SHOP_GRADE, grade.getType());
        assertEquals(GradeState.DELETED, grade.getState());
        assertEquals(ModState.REJECTED, grade.getModState());
        assertEquals(Delivery.PICKUP, grade.getDelivery());
    }

    @Test
    public void testPapiResponseWithToken() {
        HttpClientMockHelpers.mockResponseWithFile(httpClient, HttpStatus.SC_OK, "/data/papi_grades.json");

        GradeUpdatesToken token = new GradeUpdatesToken(12345L, 987L);

        DataListWithToken<ShopGradeResponseDto, GradeUpdatesToken> response = gradeClient
            .getPapiGradeUpdates(42, token);

        assertEquals(2, response.getData().size());
        assertEquals(1565348221000L, response.getToken().getLastTimestamp().longValue());
        assertEquals(59526622L, response.getToken().getLastGradeId().longValue());
    }

    @Test
    public void testPapiResponseWithTokenAndSize() {
        HttpClientMockHelpers.mockResponseWithFile(httpClient, HttpStatus.SC_OK, "/data/papi_grades.json");

        GradeUpdatesToken token = new GradeUpdatesToken(12345L, 987L);

        DataListWithToken<ShopGradeResponseDto, GradeUpdatesToken> response = gradeClient
            .getPapiGradeUpdates(42, token, 30);

        assertEquals(2, response.getData().size());
        assertEquals(1565348221000L, response.getToken().getLastTimestamp().longValue());
        assertEquals(59526622L, response.getToken().getLastGradeId().longValue());
    }

    @Test
    public void testDeleteCommentResponse() {
        long gradeId = 100500;
        long deletedComment = 666;
        long lastComment = 111;

        HttpClientMockHelpers.mockResponseWithFile(httpClient, HttpStatus.SC_OK, "");

        gradeClient.removeGradeComment(gradeId, deletedComment, lastComment);
        gradeClient.removeGradeComment(gradeId, lastComment, null);
    }

    @Test
    public void testCommentSignal() {
        long commentId = 666;
        HttpClientMockHelpers.mockResponseWithFile(httpClient, HttpStatus.SC_OK, "");

        gradeClient.sendGradeCommentSignal(Collections.singletonList(commentId));
    }

    @Test
    public void testClosestUserAchievement() {
        HttpClientMockHelpers.mockResponseWithFile(httpClient, HttpStatus.SC_OK, "/data/closest_user_achievement.json");

        ResponseEntity<UserAchievementDto> response = gradeClient.getClosestUserAchievement(152);

        assertEquals(OK, response.getStatusCode());
        assertEquals(new UserAchievement(1, 1, 0),
                response.getBody().getUserAchievement());
        assertEquals(2, response.getBody().getAchievementDistance());
        assertEquals(2, response.getBody().getAchievementLevel().getLevelId());
    }

    @Test
    public void testClosestUserAchievementWithoutAnyGrade() {
        HttpClientMockHelpers.mockResponseWithFile(httpClient, HttpStatus.SC_OK, "/data/closest_user_achievement_without_any_grade.json");

        ResponseEntity<UserAchievementDto> response = gradeClient.getClosestUserAchievement(152);

        assertEquals(OK, response.getStatusCode());
        assertEquals(new UserAchievement(0, 0, 0),
                response.getBody().getUserAchievement());
        assertEquals(1, response.getBody().getAchievementDistance());
        assertEquals(1, response.getBody().getAchievementLevel().getLevelId());
    }

    @Test
    public void testClosestUserAchievementWithFullyFilledLevels() {
        HttpClientMockHelpers.mockResponseWithFile(httpClient, HttpStatus.SC_NOT_FOUND, "");

        try {
            gradeClient.getClosestUserAchievement(152);
            Assert.fail("Http Status 404");
        } catch (Exception expected) {
        }
    }

    @Test
    public void testGetAllUserAchievement() {
        Map<String, Integer> expectedUserAchievementDto = Stream.of(new Object[][]{
                {"Библия Маркета", 43}, {"Гуру фотографии", 46}, {"Золотой компас", 43}, {"Именитый магазиновед", 45},
                {"Магазинный критик", 0}, {"Новобранец", 0}, {"Ода покупкам", 0}, {"Первая вершина", 0}, {"Ревизор", 5},
                {"Рюкзак первопроходца", 0}, {"Тайный покупатель", 0}, {"Товарная эпопея", 3}, {"Трофейный ледоруб", 3},
                {"Фотолюбитель", 0}, {"Фотоохотник", 0}, {"Фоторепортёр", 6}, {"Эссе о товарах", 0}
        }).collect(Collectors.toMap(data -> (String) data[0], data -> (Integer) data[1]));

        checkAllUserAchievement(expectedUserAchievementDto, "/data/all_user_achievement.json");
    }

    @Test
    public void testGetAllUserAchievementWithoutAnyGrade() {
        Map<String, Integer> expectedUserAchievementDto = Stream.of(new Object[][]{
                {"Библия Маркета", 50}, {"Гуру фотографии", 50}, {"Золотой компас", 50}, {"Именитый магазиновед", 50},
                {"Магазинный критик", 5}, {"Новобранец", 1}, {"Ода покупкам", 5}, {"Первая вершина", 1}, {"Ревизор", 10},
                {"Рюкзак первопроходца", 3}, {"Тайный покупатель", 3}, {"Товарная эпопея", 10}, {"Трофейный ледоруб", 10},
                {"Фотолюбитель", 1}, {"Фотоохотник", 3}, {"Фоторепортёр", 10}, {"Эссе о товарах", 3}
        }).collect(Collectors.toMap(data -> (String) data[0], data -> (Integer) data[1]));

        checkAllUserAchievement(expectedUserAchievementDto, "/data/all_user_achievement_without_any_grade.json");
    }

    private void checkAllUserAchievement(Map<String, Integer> expectedUserAchievementDto, String responseFile) {
        HttpClientMockHelpers.mockResponseWithFile(httpClient, HttpStatus.SC_OK, responseFile);

        ResponseEntity<List<UserAchievementDto>> response = gradeClient.getAllUserAchievement(152);

        assertEquals(OK, response.getStatusCode());
        assertEquals(17, response.getBody().size());
        assertEquals(expectedUserAchievementDto, response.getBody().stream().collect(Collectors
                .toMap(x -> x.getAchievementLevel().getAchievementLevelName(), UserAchievementDto::getAchievementDistance)));
    }

    @Test
    public void testGetAllUserAchievementWithError() {
        HttpClientMockHelpers.mockResponseWithFile(httpClient, HttpStatus.SC_NOT_FOUND, "");

        try {
            gradeClient.getAllUserAchievement(152);
            Assert.fail("Http Status 404");
        } catch (Exception expected) {
        }
    }

    @Test
    public void testGetModelGradeComment() {
        HttpClientMockHelpers.mockResponseWithFile(httpClient, HttpStatus.SC_OK, "/data/model_grade_comment.json",
            withQuery("/api/grade/66735810/for/comments", null)
        );
        GradeForCommentDto grade = gradeClient.getGradeForComments(66735810);

        checkGradeComment(grade, 66735810, 66735341L, GradeType.MODEL_GRADE.value(), 12345, 53453);
    }

    @Test
    public void testGetModelLastGradeComment() {
        HttpClientMockHelpers.mockResponseWithFile(httpClient, HttpStatus.SC_OK, "/data/model_grade_comment.json",
            withQuery("/api/grade/66735810/for/comments/last", null)
        );
        GradeForCommentDto grade = gradeClient.getLastGradeForComments(66735810);

        checkGradeComment(grade, 66735810, 66735341L, GradeType.MODEL_GRADE.value(), 12345, 53453);
    }

    @Test
    public void testGetShopGradeComment() {
        HttpClientMockHelpers.mockResponseWithFile(httpClient, HttpStatus.SC_OK, "/data/shop_grade_comment.json",
            withQuery("/api/grade/66735810/for/comments", null)
        );
        GradeForCommentDto grade = gradeClient.getGradeForComments(66735810);

        checkGradeComment(grade, 66739256, null, GradeType.SHOP_GRADE.value(), 12345, 53455);
    }

    @Test
    public void testGetModelGradeListComment() {
        HttpClientMockHelpers.mockResponseWithFile(httpClient, HttpStatus.SC_OK, "/data/model_grade_comment_list.json",
            withQuery("/api/grade/for/comments", "gradeId=66735810&gradeId=43423415")
        );
        List<GradeForCommentDto> grade = gradeClient.getGradesForComments(Arrays.asList(66735810L, 43423415L));

        assertEquals(2, grade.size());
        checkGradeComment(grade.get(0), 66735810, 66735341L, GradeType.MODEL_GRADE.value(), 12345, 53453);
        checkGradeComment(grade.get(1), 43423415L, null, GradeType.MODEL_GRADE.value(), 12345, 45245);
    }

    private void checkGradeComment(GradeForCommentDto grade, long id, Long fixId, int type, long uid, long resourceId) {
        assertEquals(id, grade.getId());
        assertEquals(fixId, grade.getFixId());
        assertEquals(type, grade.getType());
        assertEquals(uid, grade.getUser().getPassportUid().longValue());
        assertEquals(resourceId, grade.getResourceId());
    }

    @Test
    public void testGetGradeCommentWithError() {
        HttpClientMockHelpers.mockResponseWithFile(httpClient, HttpStatus.SC_NOT_FOUND, "");

        try {
            gradeClient.getGradeForComments(66735810);
            Assert.fail("Http Status 404");
        } catch (Exception expected) {
        }
    }

    @Test
    public void testGetMailerModelGrade() {
        HttpClientMockHelpers.mockResponseWithFile(httpClient, HttpStatus.SC_OK,
            "/data/mailer_model_grade.json",
            withQuery("/api/grade/mailer/model", "gradeId=81374618348")
        );

        MailerModelGrade grade = gradeClient.getModelGradeForMailer(81374618348L);
        assertEquals(12345, grade.getId());
        assertEquals(99999, grade.getUid());
        assertEquals(ModState.APPROVED, grade.getModState());
        assertEquals(GradeType.MODEL_GRADE, grade.getType());
        assertEquals("some text", grade.getText());
        assertEquals(11223344, grade.getModelId());
        assertEquals(1234567891, grade.getCrTimeMs());
    }

    @Test
    public void testGetMailerModelGradeEmpty() {
        HttpClientMockHelpers.mockResponse(httpClient, HttpStatus.SC_OK,
            invocation -> new ByteArrayInputStream("[]".getBytes()),
            withQuery("/api/grade/mailer/model", "gradeId=81374618348")
        );

        assertNull(gradeClient.getModelGradeForMailer(81374618348L));
    }

    @Test
    public void testGetMailerShopGrade() {
        HttpClientMockHelpers.mockResponseWithFile(httpClient, HttpStatus.SC_OK,
            "/data/mailer_shop_grade.json",
            withQuery("/api/grade/mailer/shop", "gradeId=81374618348&onlyPublic=false")
        );

        MailerShopGrade grade = gradeClient.getShopGradeForMailer(81374618348L, false);
        assertEquals(12345, grade.getId());
        assertEquals(99999, grade.getUid());
        assertEquals(ModState.APPROVED, grade.getModState());
        assertEquals(GradeType.SHOP_GRADE, grade.getType());
        assertEquals("some text", grade.getText());
        assertEquals(11223344, grade.getShopId());
        assertEquals(1234567891, grade.getCrTimeMs());
        assertEquals("31341341", grade.getOrderId());
    }

    @Test
    public void testGetMailerShopGradeEmpty() {
        HttpClientMockHelpers.mockResponse(httpClient, HttpStatus.SC_OK,
            invocation -> new ByteArrayInputStream("[]".getBytes()),
            withQuery("/api/grade/mailer/shop", "gradeId=81374618348&onlyPublic=false")
        );

        assertNull(gradeClient.getShopGradeForMailer(81374618348L, false));
    }

    @Test
    public void testCheckMailerShopGrade() {
        HttpClientMockHelpers.mockResponse(httpClient, HttpStatus.SC_OK,
            invocation -> new ByteArrayInputStream("true".getBytes()),
            withQuery("/api/grade/mailer/shop/exists", "userId=12345&shopId=56789")
        );

        assertTrue(gradeClient.checkShopGradeExistsForMailer(12345, 56789));
    }

    @Test
    public void testCheckMailerVote() {
        HttpClientMockHelpers.mockResponse(httpClient, HttpStatus.SC_OK,
            invocation -> new ByteArrayInputStream("false".getBytes()),
            withQuery("/api/grade/mailer/vote/exists", "voteId=12345")
        );

        assertFalse(gradeClient.checkVoteExistsForMailer(12345));
    }

}
