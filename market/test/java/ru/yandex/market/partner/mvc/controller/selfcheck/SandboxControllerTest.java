package ru.yandex.market.partner.mvc.controller.selfcheck;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

@DbUnitDataSet(before = "SandboxControllerTest.before.csv")
public class SandboxControllerTest extends FunctionalTest {

    /**
     * Проверяем, что ДСБС магазин успешно смог запросить загрузку в ПШ через API_DEBUG.
     * Магазин прошел визард, на него наложен катофф процессе работы, он выбивается из индекса,
     * нажимает на кнопку "Загрзузиться в ПШ".
     */
    @Test
    @DbUnitDataSet(after = "dsbs.plainshift.apiDebug.new.after.csv")
    void testShopLoadedToInApiDebugMode() {
        FunctionalTestHelper.post(
                baseUrl + "/loadFeedsToSandbox?_user_id=100500&datasource_id=1");
    }

    /**
     * ДСБС магазин, когда уже находится в DEBUG_API, попросил туда повторную загрузку.
     */
    @Test
    @DbUnitDataSet(after = "dsbs.plainshift.apiDebug.duplicated.after.csv")
    void testShopLoadedToInApiDebugModeDuplicated() {
        FunctionalTestHelper.post(
                baseUrl + "/loadFeedsToSandbox?_user_id=100500&datasource_id=2");
    }

    /**
     * Проверяем, что ДСБС магазин успешно смог запросить загрузку в ПШ через API_DEBUG.
     * Магазин зафейлил модерацию, на него наложен катофф, он выбивается из индекса,
     * нажимает на кнопку "Загрзузиться в ПШ".
     */
    @Test
    @DbUnitDataSet(after = "dsbs.plainshift.apiDebug.new.failingModeration.after.csv")
    void testShopFailingModeration() {
        FunctionalTestHelper.post(
                baseUrl + "/loadFeedsToSandbox?_user_id=100500&datasource_id=3");
    }

    /**
     * ДСБС магазин еще не проходил самопроверку, нажимает на кнопку "Загрузиться в ПШ".
     */
    @Test
    @DbUnitDataSet(after = "dsbs.plainshift.selfcheckStarted.after.csv")
    void testShopLoadedInSelfCheckMode() {
        FunctionalTestHelper.post(
                baseUrl + "/loadFeedsToSandbox?_user_id=100500&datasource_id=4");
    }

    /**
     * ДСБС магазин, когда уже находится в DEBUG_API, попросил туда повторную загрузку..
     */
    @Test
    @DbUnitDataSet(after = "dsbs.plainshift.selfcheckStarted.duplicated.after.csv")
    void testShopLoadedInSelfCheckModeDuplicated() {
        FunctionalTestHelper.post(
                baseUrl + "/loadFeedsToSandbox?_user_id=100500&datasource_id=5");
    }

    /**
     * Не ДСБС магазин запрашивает загрузку в ПШ.
     */
    @Test
    void testNotDsbsShop() {
        Assertions.assertThrows(
                HttpClientErrorException.BadRequest.class,
                () -> FunctionalTestHelper.post(
                        baseUrl + "/loadFeedsToSandbox?_user_id=100500&datasource_id=6"));
    }

    /**
     * ДСБС магазин, который недоконфигурен, запрашивает загрузку в ПШ.
     */
    @Test
    void testNotСonfiguredDsbsShop() {
        Assertions.assertThrows(
                HttpClientErrorException.BadRequest.class,
                () -> FunctionalTestHelper.post(
                        baseUrl + "/loadFeedsToSandbox?_user_id=100500&datasource_id=7"));
    }

    /**
     * У ДСБС магазина закэнселена проверка API_DEBUG.
     */
    @Test
    @DbUnitDataSet(after = "dsbs.plainshift.apiDebug.canceled.after.csv")
    void testRestartCanceledApiDebug() {
        FunctionalTestHelper.post(
                baseUrl + "/loadFeedsToSandbox?_user_id=100500&datasource_id=8");
    }

    /**
     * Тест отправки на самопроверку, когда магазин уже в ПШ
     * 1. у него создался datasource_in_testing с корректным статусом (4)
     */
    @Test
    @DbUnitDataSet(after = "dsbs.plainshift.selfcheckStarted.InIndex.after.csv")
    void testSelfcheckWhenAlreadyInPlainshift() {
        FunctionalTestHelper.post(
                baseUrl + "/loadFeedsToSandbox?_user_id=100500&datasource_id=9");
    }

}
