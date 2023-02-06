package ru.yandex.market.partner.mvc.controller.moderation;

import java.util.List;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.message.PartnerNotificationMessageServiceTest;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.partner.notification.client.model.WebUINotificationResponse;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.mbi.util.MbiMatchers.jsonPath;
import static ru.yandex.market.mbi.util.MoreMbiMatchers.jsonPropertyMatches;
import static ru.yandex.market.partner.mvc.controller.util.ResponseJsonUtil.getResult;

/**
 * Тесты для {@link ModerationController}
 *
 * @author moskovkin@yandex-team.ru
 * @since 23.09.2020
 */
@DbUnitDataSet(before = "moderationShopProgramStates.before.csv")
class ModerationControllerFunctionalTest extends FunctionalTest {
    private static final long SHOP_ID = 200L;

    @BeforeEach
    void setUp() {
        PartnerNotificationMessageServiceTest.mockPN(
                partnerNotificationClient,
                new WebUINotificationResponse()
                        .subject("Вас отключили")
                        .body("Вас отключили насовсем и не пытайтесь возвращаться")
                        .shopId(200L)
                        .priority(2L)
                        .groupId(1L),
                new WebUINotificationResponse()
                        .subject("Вас отключили пингером")
                        .body("Почините АПИ и мы вас ждём")
                        .shopId(200L)
                        .priority(1L)
                        .groupId(2L)
        );
    }

    @Test
    @DbUnitDataSet(before = "moderationCutoffs.before.csv")
    void testReturnCorrectState() {
        String response = FunctionalTestHelper.get(
                baseUrl + "/moderationShopProgramStates?id={datasourceId}", String.class, SHOP_ID
        ).getBody();

        assertThat((List<?>) JsonPath.parse(response).read("$.result", List.class), hasSize(2));

        //PINGER
        assertThat(response, jsonPath("$.result[0].type", "DROPSHIP_BY_SELLER"));
        assertThat(response, jsonPath("$.result[0].details.moderationEnabled", "false"));
        assertThat(response, jsonPath("$.result[0].details.moderationCause", "PINGER"));

        //ORDER_NOT_ACCEPTED
        assertThat(response, jsonPath("$.result[1].type", "DROPSHIP_BY_SELLER"));
        assertThat(response, jsonPath("$.result[1].details.moderationEnabled", "true"));
        assertThat(response, jsonPath("$.result[1].details.moderationCause", "ORDER_NOT_ACCEPTED"));
    }

    @Test
    @DbUnitDataSet(before = "moderationCutoffsNoCause.before.csv")
    void testReturnNoResults() {
        String response = FunctionalTestHelper.get(
                baseUrl + "/moderationShopProgramStates?id={datasourceId}", String.class, SHOP_ID
        ).getBody();

        assertThat((List<?>) JsonPath.parse(response).read("$.result", List.class), empty());
    }

    @Test
    @DbUnitDataSet(before = "moderationShopProgramStates.otherType.before.csv")
    @DisplayName("Прочие проблемы качества не должны зависить от количества попыток отправиться на модерацию")
    void testShopStateWithOtherModerationCause() {
        checkModerationState("json/moderationShopProgramStates.otherType.response.json");
    }

    @Test
    @DbUnitDataSet(before = "moderationShopProgramStates.noAttemptsLeft.before.csv")
    @DisplayName(
            "Если количество оставшихся попыток пройти модерацию равно нулю, то нужно добавить NO_MORE_ATTEMPTS в " +
                    "disabledReasons")
    void testShopStateWithNoAttemptsLeft() {
        checkModerationState("json/moderationShopProgramStates.noAttemptsLeft.response.json");
    }

    @Test
    @DbUnitDataSet(before = "moderationShopProgramStates.pinger.before.csv")
    @DisplayName("PINGER не должен блочить возможность прохождения модерации")
    void testModerationWithPinger() {
        checkModerationState("json/moderationShopProgramStates.pinger.response.json");
    }

    @Test
    @DbUnitDataSet(before = "moderationShopProgramStates.moderationInProgress.before.csv")
    @DisplayName("Нельзя уйти на модерацию, если она уже в процессе")
    void testModerationInProgress() {
        checkModerationState("json/moderationShopProgramStates.moderationInProgress.response.json");
    }

    @Test
    @DbUnitDataSet(before = "moderationShopProgramStates.multipleFatalCutoffs.before.csv")
    @DisplayName("Для каждого фатального катоффа своя плашечка")
    void testMultipleFatalCutoffs() {
        checkModerationState("json/moderationShopProgramStates.multipleFatalCutoffs.response.json");
    }

    @Test
    @DbUnitDataSet(before = "moderationShopProgramStates.moderationAfterFatalCutoffs.before.csv")
    @DisplayName("После снятия фатальных ограничений должна появиться плашка QUALITY_MODERATION_REQUIRED")
    void testModerationRequiredAfterFatalCutoffs() {
        checkModerationState("json/moderationShopProgramStates.moderationAfterFatalCutoffs.response.json");
    }

    @Test
    @DbUnitDataSet(before = "moderationShopProgramStates.notifications.before.csv")
    @DisplayName("Должны корректно прокидываться уведомления на плашки")
    void testNotifications() {
        checkModerationState("json/moderationShopProgramStates.notifications.json");
    }

    @Test
    @DbUnitDataSet(before = "moderationCutoffs.lowRating.before.csv")
    @DisplayName("Проверка отключения за низкий  рейтинг")
    void testLowRating() {
        checkModerationState("json/moderationShopProgramsStates.lowRating.response.json");
    }

    @Test
    @DbUnitDataSet(before = "moderationCutoffs.limitOrders.before.csv")
    @DisplayName("Проверка отключения за кол-во заказов")
    void testLimitOrders() {
        checkModerationState("json/moderationShopProgramsStates.limitOrder.response.json");
    }


    @Test
    @DbUnitDataSet(before = "moderationCutoffs.byDSBSPartner.before.csv")
    @DisplayName("Проверка отключения по инициативе партнера")
    void testModerationNoRequiredForDisablingByPartner() {
        checkModerationState("json/moderationShopProgramsStates.byDSBSPartner.response.json");
    }

    @Test
    @DbUnitDataSet(before = "moderationCutoffs.cartDiffs.before.csv")
    @DisplayName("Проверка отключения за массовые карт диффы")
    void testCartDiff() {
        checkModerationState("json/moderationShopProgramsStates.cartDiff.response.json");
    }

    @Test
    @DbUnitDataSet(before = "moderationCutoffs.orderNotAccepted.before.csv")
    @DisplayName("Проверка отключения за непринятый заказ")
    void testOrderNotAccepted() {
        checkModerationState("json/moderationShopProgramsStates.orderNotAccepted.response.json");
    }

    @Test
    @DbUnitDataSet(before = "moderationCutoffs.moderationHold.before.csv")
    @DisplayName("Проверка плашки с приостановкой модерации")
    void testModerationHold() {
        checkModerationState("json/moderationShopProgramsStates.testModerationHold.response.json");
    }

    @Test
    @DbUnitDataSet(before = "moderationCutoffs.feedError.before.csv")
    @DisplayName("Проверка присутствия катоффа с ошибками фида")
    void testFeedErrors() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/moderationRequestState?id={datasourceId}", String.class, SHOP_ID);

        String expected = StringTestUtil.getString(getClass(), "json/moderationRequestStates.feedErrors.response.json");
        JSONAssert.assertEquals(expected, getResult(response), false);
    }

    @Test
    @DbUnitDataSet(before = "moderationCutoffs.orderFinalStatusNotSet.before.csv")
    @DisplayName("Проверка отключения за невыставленные финальные статусы заказов")
    void testOrderFinalStatusNotSet() {
        checkModerationState("json/moderationShopProgramsStates.orderFinalStatusNotSet.response.json");
    }

    @Test
    @DbUnitDataSet(before = "moderationCutoffs.needInfo.before.csv")
    @DisplayName("Модерация не может начаться, так как нет настроек доставки")
    void testNeedInfoModerationReason() {
        checkModerationState(300L, "json/moderationShopProgramStates.needInfo.json");
    }

    private void checkModerationState(String expectedFile) {
        checkModerationState(SHOP_ID, expectedFile);
    }

    private void checkModerationState(long shopId, String expectedFile) {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/moderationShopProgramStates?id={datasourceId}", String.class, shopId
        );

        String expected = StringTestUtil.getString(getClass(), expectedFile);
        assertThat(response, MoreMbiMatchers.responseBodyMatches(jsonPropertyMatches("result",
                MbiMatchers.jsonEquals(
                        expected,
                        List.of(
                                new Customization("[*].details.startDate", (v1, v2) -> true),
                                new Customization("details.startDate", (v1, v2) -> true),
                                new Customization("[*].details.modificationDate", (v1, v2) -> true),
                                new Customization("details.modificationDate", (v1, v2) -> true)
                        )))));
    }
}
