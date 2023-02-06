package ru.yandex.market.partner.mvc.controller.program.cpc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.core.status.StatusService;
import ru.yandex.market.core.status.model.CampaignState;
import ru.yandex.market.core.status.model.ProgramState;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.partner.mvc.controller.program.ProgramController;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тесты для {@link ProgramController}.
 *
 * @author Kirill Lakhtin (klaktin@yandex-team.ru)
 */
class CpcProgramControllerFunctionalTest extends FunctionalTest {

    @Autowired
    private StatusService statusService;

    @Test
    @DbUnitDataSet(before = "db/ProgramController.new.missing_params.before.csv")
    @DisplayName("Не заполнил необходимые поля. Активен")
    void testGetProgramMissingFieldsEnabled() {
        // Новичок. Не дошел до модерации. Нажал кнопку "Сохранить", но не заполнил все поля
        requestAndCheckCpcStatus(1, "/mvc/program/missing_params_new.json");

        // Аналогично 1, но вручную выключил программу
        requestAndCheckCpcStatus(2, "/mvc/program/missing_params_suspended.json");

        // Новичок. Прошел модерацию, не успел запуститься, появлился новый обязательный параметр
        requestAndCheckCpcStatus(3, "/mvc/program/testing_missing_params_new.json");

        // Магазин уже работал, но появились новые обязательные параметры
        requestAndCheckCpcStatus(4, "/mvc/program/testing_missing_params.json");
    }

    @Test
    @DbUnitDataSet(before = "db/ProgramController.new.ready_for_testing.before.csv")
    @DisplayName("Новичок. Все заполнил. Готов к первой модерации")
    void testGetProgramReadyForTesting() {
        requestAndCheckCpcStatus(1, "/mvc/program/ready_for_testing_new.json");
    }

    @Test
    @DbUnitDataSet(before = "db/ProgramController.testing.before.csv")
    @DisplayName("На модерации")
    void testGetProgramTesting() {
        requestAndCheckCpcStatus(1, "/mvc/program/testing_new.json");

        requestAndCheckCpcStatus(2, "/mvc/program/testing_old.json");

        requestAndCheckCpcStatus(3, "/mvc/program/testing_new.json");
    }

    @Test
    @DbUnitDataSet(before = "db/ProgramController.testing.need.info.before.csv")
    @DisplayName("Модерация на паузе")
    void cpcModerationNeedInfo() {
        requestAndCheckCpcStatus(1, "/mvc/program/testing_need_info_new.json");

        requestAndCheckCpcStatus(2, "/mvc/program/testing_need_info_old.json");

        requestAndCheckCpcStatus(3, "/mvc/program/testing_need_info_new.json");
    }

    @Test
    @DbUnitDataSet(before = "db/ProgramController.moderation.feed_trouble.before.csv")
    @DisplayName("Модерация дропнулась из-за проблем с фидом")
    void testGetProgramDroppedModeration() {
        requestAndCheckCpcStatus(1, "/mvc/program/moderation_feed_trouble_new.json");

        requestAndCheckCpcStatus(2, "/mvc/program/moderation_feed_trouble_old.json");

        requestAndCheckCpcStatus(3, "/mvc/program/moderation_feed_trouble_old_blocked.json");
    }

    @Test
    @DbUnitDataSet(before = "db/ProgramController.moderation.failed.before.csv")
    @DisplayName("Модерация не пройдена")
    void testGetProgramModerationFailed() {
        requestAndCheckCpcStatus(1, "/mvc/program/moderation_failed_new.json");

        requestAndCheckCpcStatus(2, "/mvc/program/moderation_failed_new.json");

        requestAndCheckCpcStatus(3, "/mvc/program/moderation_failed_old.json");

        requestAndCheckCpcStatus(4, "/mvc/program/moderation_failed_old.json");

        requestAndCheckCpcStatus(5, "/mvc/program/moderation_failed_new_blocked.json");

        requestAndCheckCpcStatus(6, "/mvc/program/moderation_failed_old_blocked.json");
    }

    @Test
    @DbUnitDataSet(before = "db/ProgramController.moderation.hold.before.csv")
    @DisplayName("Модерация приостановлена")
    void testGetProgramModerationHold() {
        requestAndCheckCpcStatus(1, "/mvc/program/testing_need_info_new.json");

        requestAndCheckCpcStatus(2, "/mvc/program/testing_need_info_old.json");
    }

    @Test
    @DbUnitDataSet(before = "db/ProgramController.moderation.repeat.before.csv")
    @DisplayName("Нужна повторная модерация")
    void testGetProgramModerationRepeat() {
        requestAndCheckCpcStatus(1, "/mvc/program/moderation_repeat.json");

        requestAndCheckCpcStatus(2, "/mvc/program/moderation_repeat_quality.json");

        // Новичок. Не первая попытка в рамках одной модерации
        requestAndCheckCpcStatus(3, "/mvc/program/moderation_repeat_new.json");

        // Новичок. Не первая модерация
        requestAndCheckCpcStatus(4, "/mvc/program/moderation_repeat_new.json");
    }

    @Test
    @DbUnitDataSet(before = "db/ProgramController.need_money.before.csv")
    @DisplayName("Новый магазин. Проверка пройдена. Нужны деньги")
    void testGetProgramNewNeedMoney() {
        requestAndCheckCpcStatus(1, "/mvc/program/need_money_new.json");

        requestAndCheckCpcStatus(2, "/mvc/program/need_money_old.json");
    }

    @Test
    @DbUnitDataSet(before = "db/ProgramController.testGetProgramCpcOverdraftControl.before.csv")
    @DisplayName("Отключение за просроченный овердрафт. Показываем статус, что нужны деньги")
    void testGetProgramCpcOverdraftControl() {
        requestAndCheckCpcStatus(2, "/mvc/program/need_money_old.json");
    }

    @Test
    @DbUnitDataSet(before = "db/ProgramController.enabling.before.csv")
    @DisplayName("В процессе подключения")
    void testGetProgramEnabling() {
        requestAndCheckCpcStatus(1, "/mvc/program/enabling_new.json");

        requestAndCheckCpcStatus(2, "/mvc/program/enabling_old.json");
    }

    @Test
    @DbUnitDataSet(before = "db/ProgramController.full.before.csv")
    @DisplayName("Все пройдено. Магазин в проде")
    void testGetProgramCPCOK() {
        requestAndCheckCpcStatus(1, "/mvc/program/full.json");
    }

    @Test
    @DbUnitDataSet(before = "db/ProgramController.manager.before.csv")
    @DisplayName("Выключено менеджером Маркета")
    void testGetProgramManager() {
        // Новичок прошел первый шаг
        requestAndCheckCpcStatus(1, "/mvc/program/yamanager_new.json");

        // Новичок не прошел первй шаг
        requestAndCheckCpcStatus(7, "/mvc/program/yamanager_feed_new.json");

        // Завалил модерацию и есть катоф YAMANAGER
        requestAndCheckCpcStatus(8, "/mvc/program/yamanager_testing_failed.json");

        requestAndCheckCpcStatus(2, "/mvc/program/yamanager_old.json");

        requestAndCheckCpcStatus(3, "/mvc/program/qmanager_fraud_new.json");

        requestAndCheckCpcStatus(4, "/mvc/program/qmanager_fraud_old.json");

        requestAndCheckCpcStatus(5, "/mvc/program/qmanager_clone_new.json");

        requestAndCheckCpcStatus(6, "/mvc/program/qmanager_clone_old.json");
    }

    @Test
    @DbUnitDataSet(before = "db/ProgramController.quality.before.csv")
    @DisplayName("Выключено за качество")
    void testGetProgramQuality() {
        requestAndCheckCpcStatus(1, "/mvc/program/quality_failed_blocked_new.json");

        requestAndCheckCpcStatus(2, "/mvc/program/quality_failed_blocked_old.json");

        requestAndCheckCpcStatus(3, "/mvc/program/quality_failed_new.json");

        requestAndCheckCpcStatus(4, "/mvc/program/quality_failed_old.json");

        requestAndCheckCpcStatus(5, "/mvc/program/quality_failed_blocked_new.json");

        requestAndCheckCpcStatus(6, "/mvc/program/quality_failed_blocked_old.json");

        requestAndCheckCpcStatus(7, "/mvc/program/quality_failed_new.json");

        requestAndCheckCpcStatus(8, "/mvc/program/quality_failed_old.json");

        requestAndCheckCpcStatus(9, "/mvc/program/quality_failed_old.json");
    }

    @Test
    @DbUnitDataSet(before = "db/ProgramController.manual_quality.before.csv")
    @DisplayName("QMANAGER_CHEESY + проваленная модерация + новичок")
    void testGetProgramManualQuality01() {
        requestAndCheckCpcStatus(1, "/mvc/program/manual_quality_failed_blocked_new.json");
    }

    @Test
    @DbUnitDataSet(before = "db/ProgramController.manual_quality.before.csv")
    @DisplayName("QMANAGER_FRAUD + проваленная модерация + новичок")
    void testGetProgramManualQuality02() {
        requestAndCheckCpcStatus(2, "/mvc/program/qmanager_fraud_new.json");
    }

    @Test
    @DbUnitDataSet(before = "db/ProgramController.manual_quality.before.csv")
    @DisplayName("QMANAGER_CLONE + проваленная модерация + новичок")
    void testGetProgramManualQuality03() {
        requestAndCheckCpcStatus(3, "/mvc/program/qmanager_clone_new.json");
    }

    @Test
    @DbUnitDataSet(before = "db/ProgramController.manual_quality.before.csv")
    @DisplayName("QMANAGER_OTHER + проваленная модерация + новичок")
    void testGetProgramManualQuality04() {
        requestAndCheckCpcStatus(4, "/mvc/program/manual_quality_failed_new.json");
    }

    @Test
    @DbUnitDataSet(before = "db/ProgramController.manual_quality.before.csv")
    @DisplayName("QMANAGER_CHEESY + проваленная модерация + старичок")
    void testGetProgramManualQuality05() {
        requestAndCheckCpcStatus(5, "/mvc/program/quality_failed_blocked_old.json");
    }

    @Test
    @DbUnitDataSet(before = "db/ProgramController.manual_quality.before.csv")
    @DisplayName("QMANAGER_FRAUD + проваленная модерация + старичок")
    void testGetProgramManualQuality06() {
        requestAndCheckCpcStatus(6, "/mvc/program/qmanager_fraud_old.json");
    }

    @Test
    @DbUnitDataSet(before = "db/ProgramController.manual_quality.before.csv")
    @DisplayName("QMANAGER_CLONE + проваленная модерация + старичок")
    void testGetProgramManualQuality07() {
        requestAndCheckCpcStatus(7, "/mvc/program/qmanager_clone_old.json");
    }

    @Test
    @DbUnitDataSet(before = "db/ProgramController.manual_quality.before.csv")
    @DisplayName("QMANAGER_OTHER + проваленная модерация + старичок")
    void testGetProgramManualQuality08() {
        requestAndCheckCpcStatus(8, "/mvc/program/quality_failed_old.json");
    }

    @Test
    @DbUnitDataSet(before = "db/ProgramController.moderation.feed_trouble.before.csv")
    @DisplayName("Ошибка в прайс-листе")
    void testGetProgramTechnicalYml() {
        requestAndCheckCpcStatus(4, "/mvc/program/technical_yml_new.json");

        requestAndCheckCpcStatus(5, "/mvc/program/technical_yml_old.json");
    }

    @Test
    @DbUnitDataSet(before = "db/ProgramController.site_disabled.before.csv")
    @DisplayName("Сайт недоступен")
    void testGetProgramSiteDisabled() {
        requestAndCheckCpcStatus(1, "/mvc/program/site_disabled_new.json");

        requestAndCheckCpcStatus(2, "/mvc/program/site_disabled_old.json");
    }

    @Test
    @DbUnitDataSet(before = "db/ProgramController.finance_limit.before.csv")
    @DisplayName("Выключен по лимиту финансам")
    void testGetProgramFinanceLimit() {
        requestAndCheckCpcStatus(1, "/mvc/program/finance_limit.json");
    }

    @Test
    @DbUnitDataSet(before = "db/ProgramController.suspended.before.csv")
    @DisplayName("Выключен вручную/по расписанию/по лимиту")
    void testGetProgramSuspendedByProgram() {
        requestAndCheckCpcStatus(1, "/mvc/program/suspended_by_placement.trouble.json");

        requestAndCheckCpcStatus(2, "/mvc/program/suspended_by_schedule.trouble.json");

        requestAndCheckCpcStatus(3, "/mvc/program/suspended_by_program.trouble.json");
    }

    /**
     * Тест, проверяющий, что ручка корректно агрегирует подстатусы при выдаче ошибки для "новичка".
     */
    @Test
    @DbUnitDataSet(before = "db/testGetProgramsOK.before.csv")
    void testGetProgramCPCFailed() {
        requestAndCheckCpcStatus(2, "/mvc/program/clone_new.json");
    }

    /**
     * Тест, проверяющий, что ручка корректно агрегирует подстатусы при выдаче ошибки для "старичка".
     */
    @Test
    @DbUnitDataSet(before = "db/testGetProgramsOK.before.csv")
    void testGetProgramCPCFailedOld() {
        requestAndCheckCpcStatus(3, "/mvc/program/clone_old.json");
    }

    /**
     * Тест, проверяющий работу ручки для кейса, в котором магазин достиг дневного лимита и ручка должна отдать
     * сабстатус daily_finance_limit.
     */
    @Test
    @DbUnitDataSet(before = "db/testGetProgramsOK.before.csv")
    void testGetProgramsDailyFinanceLimit() {
        requestAndCheckCpcStatus(5, "/mvc/program/daily_finance_limit.json");
    }

    /**
     * Тест, проверяющий работу ручки получения программ, когда магазин с указанным ид не существует.
     */
    @Test
    @DbUnitDataSet(before = "db/testGetProgramsOK.before.csv")
    void testGetProgramUnknownShop() {
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(cpcStatusUrl(999_999)));
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.errorListMatchesInAnyOrder(
                HttpClientErrorMatcher.errorMatches("BAD_PARAM", "datasourceId", "INVALID")
        ));
    }

    /**
     * Тест, проверяющий работу ручки получения полей. Тест проверяет, что незаполненность полей определяется корректно.
     */
    @Test
    @DbUnitDataSet(before = "db/ProgramController.fields.missing.before.csv")
    void testGetFieldsNotFilled() {
        ResponseEntity<String> entity = FunctionalTestHelper.get(cpcFieldUrl(1));
        JsonTestUtil.assertEquals(entity, this.getClass(), "/mvc/program/field/missing_fields.json");

        entity = FunctionalTestHelper.get(cpcFieldUrl(2));
        JsonTestUtil.assertEquals(entity, this.getClass(), "/mvc/program/field/missing_fields_global.json");
    }

    /**
     * Тест, проверяющий работу ручки получения фидов. Тест проверяет, что заполненность полей определяется корректно.
     */
    @Test
    @DbUnitDataSet(before = "db/ProgramController.fields.filled.before.csv")
    void testGetFieldsFilled() {
        ResponseEntity<String> entity = FunctionalTestHelper.get(cpcFieldUrl(1));
        JsonTestUtil.assertEquals(entity, this.getClass(), "/mvc/program/field/filled_fields.json");

        entity = FunctionalTestHelper.get(cpcFieldUrl(2));
        JsonTestUtil.assertEquals(entity, this.getClass(), "/mvc/program/field/filled_fields_global.json");
    }

    /**
     * Тест, проверяющий работу ручки получения полей, когда магазин с указанным ид не существует.
     */
    @Test
    @DbUnitDataSet(before = "db/testGetProgramsOK.before.csv")
    void testGetFieldsUnknownShop() {
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(cpcFieldUrl(999_999)));
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.errorListMatchesInAnyOrder(
                HttpClientErrorMatcher.errorMatches("BAD_PARAM", "datasourceId", "INVALID")
        ));
    }

    /**
     * Тест, проверяющий выключение программы.
     */
    @Test
    @DbUnitDataSet(before = "db/testGetProgramsOK.before.csv", after = "db/testDisableShop.after.csv")
    void testDisableShop() {
        FunctionalTestHelper.post(cpcDisable(7));
        //смотрим, что после выключения isEnabled = false
        requestAndCheckCpcStatus(7, "/mvc/program/disable.json");
    }

    /**
     * Тест, проверяющий работу ручки выключения программы, когда магазин с указанным ид не существует.
     */
    @Test
    @DbUnitDataSet(before = "db/testGetProgramsOK.before.csv")
    void testDisableUnknownShop() {
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(cpcDisable(999_999)));
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.errorListMatchesInAnyOrder(
                HttpClientErrorMatcher.errorMatches("BAD_PARAM", "datasourceId", "INVALID")
        ));
    }

    /**
     * Тест, проверяющий включение программы.
     */
    @Test
    @DbUnitDataSet(before = "db/ProgramController.cpc_enable.before.csv", after = "db/ProgramController.cpc_enable.after.csv")
    void testEnableShop() {
        FunctionalTestHelper.post(cpcEnable(1));
        //смотрим, что после включения isEnabled = true
        requestAndCheckCpcStatus(1, "/mvc/program/enable.json");

        FunctionalTestHelper.post(cpcEnable(2));
        requestAndCheckCpcStatus(2, "/mvc/program/enable.json");
    }

    /**
     * Тест, проверяющий работу ручки выключения программы, когда магазин с указанным ид не существует.
     */
    @Test
    @DbUnitDataSet(before = "db/testGetProgramsOK.before.csv")
    void testEnableUnknownShop() {
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(cpcEnable(999_999)));
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.errorListMatchesInAnyOrder(
                HttpClientErrorMatcher.errorMatches("BAD_PARAM", "datasourceId", "INVALID")
        ));
    }

    /**
     * Тест, проверяющий включение программы, когда часть полей не заполнена.
     */
    @Test
    @DbUnitDataSet(before = "db/testGetProgramsOK.before.csv")
    void testEnableShopMissingFields() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(cpcEnable(9))
        );

        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.bodyMatches(MbiMatchers.jsonPropertyMatches("errors",
                MbiMatchers.jsonArrayEquals(
                        "{" +
                                "\"code\":\"MISSING_FIELDS\"," +
                                "\"details\":{\"fields\":[\"cpc_domain\"]}" +
                                "}"))));
    }

    /**
     * Тест, проверяющий работу ручки на получение статусов всех программ для указанного магазина.
     */
    @Test
    @DbUnitDataSet(before = "db/testGetProgramsOK.before.csv")
    void testGetProgramsOK() {
        ResponseEntity<String> entity = FunctionalTestHelper.get(statusUrl(1));
        JsonTestUtil.assertEquals(entity, this.getClass(), "/mvc/program/all_programs.json");
    }

    @Test
    @DbUnitDataSet(before = "db/StatusService.old.before.csv")
    @DisplayName("Методы для обратной совместимости. Схлопывают несколько состояний в одно")
    void testOld() {
        List<CampaignState> actual = statusService.getStatesBulkOld(Collections.singletonList(1L)).get(1L);

        List<CampaignState> expected = new ArrayList<>();
        expected.add(new CampaignState(ProgramState.DISABLED));

        expected.get(0).addChild(new CampaignState(ProgramState.YAMANAGER));
        expected.get(0).addChild(new CampaignState(ProgramState.QMANAGER));
        expected.get(0).addChild(new CampaignState(ProgramState.FINANCE));
        expected.get(0).addChild(new CampaignState(ProgramState.TECHNICAL_DLV_REGIONS));
        expected.get(0).addChild(new CampaignState(ProgramState.TECHNICAL_YML));
        expected.get(0).addChild(new CampaignState(ProgramState.PARTNER));
        expected.get(0).addChild(new CampaignState(ProgramState.QMANAGER_CHEESY));
        expected.get(0).addChild(new CampaignState(ProgramState.QMANAGER_CLONE));
        expected.get(0).addChild(new CampaignState(ProgramState.QMANAGER_OTHER));
        expected.get(0).addChild(new CampaignState(ProgramState.TECHNICAL_ORG_INFO));
        expected.get(0).addChild(new CampaignState(ProgramState.TECHNICAL_OWN_REGION));
        expected.get(0).addChild(new CampaignState(ProgramState.TECHNICAL_SHIPPING_INFO));
        expected.get(0).addChild(new CampaignState(ProgramState.PARTNER_SCHEDULE));
        expected.get(0).addChild(new CampaignState(ProgramState.QUALITY_PINGER));
        expected.get(0).addChild(new CampaignState(ProgramState.TECHNICAL_NEED_INFO));
        expected.get(0).addChild(new CampaignState(ProgramState.CPC_FINANCE_LIMIT));
        expected.get(0).addChild(new CampaignState(ProgramState.CPC_PARTNER));
        expected.get(0).addChild(new CampaignState(ProgramState.COMMON_QUALITY));
        expected.get(0).addChild(new CampaignState(ProgramState.COMMON_OTHER));

        // ProgramState.NEED_TESTING -> NEED_TESTING_BY_PREMOD
        expected.add(new CampaignState(ProgramState.NEED_TESTING_BY_PREMOD));

        // Этих не должно быть:
        // expected.get(0).addChild(new CampaignState(ProgramState.QMANAGER_FRAUD));
        // expected.add(new CampaignState(ProgramState.NEED_TESTING));

        MatcherAssert.assertThat(actual, Matchers.containsInAnyOrder(expected.toArray()));
    }

    private String cpcEnable(int campaignId) {
        return baseUrl + String.format("/campaigns/programs/cpc/enable?_user_id=12345&id=%d", campaignId);
    }

    private String cpcDisable(int campaignId) {
        return baseUrl + String.format("/campaigns/programs/cpc/disable?_user_id=12345&id=%d", campaignId);
    }

    private String cpcFieldUrl(int campaignId) {
        return baseUrl + String.format("/campaigns/programs/cpc/fields?_user_id=12345&id=%d", campaignId);
    }

    private String cpcStatusUrl(int campaignId) {
        return baseUrl + String.format("/campaigns/programs/cpc?_user_id=12345&id=%d", campaignId);
    }

    private String statusUrl(int campaignId) {
        return baseUrl + String.format("/campaigns/programs?_user_id=12345&id=%d", campaignId);
    }

    private void requestAndCheckCpcStatus(final int campaignId, final String jsonFile) {
        final ResponseEntity<String> entity = FunctionalTestHelper.get(cpcStatusUrl(campaignId));
        JsonTestUtil.assertEquals(entity, this.getClass(), jsonFile);
    }
}
