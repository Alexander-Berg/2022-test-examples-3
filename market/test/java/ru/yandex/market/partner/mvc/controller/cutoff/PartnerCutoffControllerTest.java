package ru.yandex.market.partner.mvc.controller.cutoff;

import java.util.stream.Stream;

import org.hamcrest.MatcherAssert;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.core.message.PartnerNotificationMessageServiceTest;
import ru.yandex.market.core.testing.ShopProgram;
import ru.yandex.market.partner.notification.client.model.WebUINotificationResponse;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для {@link PartnerCutoffController}.
 */
@DbUnitDataSet(before = "PartnerCutoffControllerTest.before.csv")
class PartnerCutoffControllerTest extends FunctionalTest {

    @BeforeEach
    void setUp() {
        PartnerNotificationMessageServiceTest.mockPN(
                partnerNotificationClient,
                new WebUINotificationResponse()
                        .subject("SUBJECT_1")
                        .body("BODY_1")
                        .priority(1L)
                        .groupId(1L),
                new WebUINotificationResponse()
                        .subject("SUBJECT_2")
                        .body("BODY_2")
                        .priority(1L)
                        .groupId(2L)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("params")
    @DisplayName("Проверить получение отключений")
    void testGetCutoffs(String description, long partnerId, ShopProgram program, String expected) {
        var response = getGeneralCutoffs(partnerId, program);
        var result = new JSONObject(response.getBody()).getJSONArray("result").toString();

        JSONAssert.assertEquals(expected, result, false);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = ShopProgram.class, names = {"SELF_CHECK", "CPA"})
    @DisplayName("Проверить запрещенные типы программ при получении отключений")
    void testGetCutoffsWrongProgram(ShopProgram program) {
        var ex = Assertions.assertThrows(HttpClientErrorException.class,
                () -> getGeneralCutoffs(100, program));

        MatcherAssert.assertThat(ex, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
    }

    private ResponseEntity<String> getGeneralCutoffs(long partnerId, ShopProgram program) {
        var url = String.format(
                "%s/cutoffs?shopId=%d" + (program == null ? "" : "&program=%s"), baseUrl, partnerId, program);
        return FunctionalTestHelper.get(url);
    }

    private static Stream<Arguments> params() {
        return Stream.of(
                Arguments.of("Проверить отсутствие общих отключений", 1, ShopProgram.GENERAL, "[]"),
                Arguments.of("Проверить корректность получения общих отключений", 100, ShopProgram.GENERAL, "[" +
                        "{\"id\":1,\"shopId\":100,\"type\":\"3\",\"comment\":\"comment-3\"}," +
                        "{\"id\":2,\"shopId\":100,\"type\":\"5\",\"comment\":\"comment-5\"}" +
                        "]"),
                Arguments.of("Проверить корректность получения CPC отключений", 100, ShopProgram.CPC, "[" +
                        "{\"id\":3,\"shopId\":100,\"type\":\"6\",\"comment\":\"comment-6\"}," +
                        "{\"id\":4,\"shopId\":100,\"type\":\"8\",\"comment\":\"comment-8\"}" +
                        "]"),
                Arguments.of("Проверить корректность получения всех отключений", 100, null, "[" +
                        "{\"id\":1,\"shopId\":100,\"type\":\"3\",\"comment\":\"comment-3\"}," +
                        "{\"id\":2,\"shopId\":100,\"type\":\"5\",\"comment\":\"comment-5\"}," +
                        "{\"id\":3,\"shopId\":100,\"type\":\"6\",\"comment\":\"comment-6\"}," +
                        "{\"id\":4,\"shopId\":100,\"type\":\"8\",\"comment\":\"comment-8\"}" +
                        "]")
        );
    }

    @DisplayName("проверка взятия cutoff сообщений")
    @Test
    @DbUnitDataSet(before = "cutoffmessages.before.csv")
    void getCutoffMessages() {
        var result = FunctionalTestHelper.get(baseUrl + "/cutoff-messages?_user_id=10&id=10774");
        JsonTestUtil.assertEquals(result, getClass(), "get_cutoff_messages.json");
    }

    @DisplayName("Получение информации о последнем закрытом фича-катофе")
    @Test
    @DbUnitDataSet(before = "PartnerCutoffControllerTest.closedFeatureCutoff.before.csv")
    void getLastClosedFeatureCutoff() {
        var result = FunctionalTestHelper.get(
                baseUrl + "/feature/closedCutoff?id=100&feature_type=DROPSHIP&feature_cutoff_type=EXPERIMENT"
        );

        //language=json
        String expected = "" +
                "{\n" +
                "  \"id\": 1,\n" +
                "  \"datasourceId\": 10,\n" +
                "  \"featureType\": \"112\",\n" +
                "  \"featureCutoffType\": \"1006\",\n" +
                "  \"fromTime\": \"2016-06-01T00:00:00\",\n" +
                "  \"toTime\": \"2016-06-02T00:00:00\",\n" +
                "  \"comment\": \"mbi-api\"\n" +
                "}";
        JsonTestUtil.assertEquals(result, expected);
    }

    @DisplayName("Получение информации о последнем закрытом фича-катофе")
    @Test
    void lastClosedFeatureCutoffNotFount() {
        var e = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(
                        baseUrl + "/feature/closedCutoff?id=100&feature_type=DROPSHIP&feature_cutoff_type=EXPERIMENT"
                )
        );
        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
