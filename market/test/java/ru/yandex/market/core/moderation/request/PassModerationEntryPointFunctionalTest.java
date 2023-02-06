package ru.yandex.market.core.moderation.request;

import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.delivery.DeliverySourceType;
import ru.yandex.market.core.moderation.TestingShop;
import ru.yandex.market.core.moderation.recommendation.SettingType;
import ru.yandex.market.core.param.model.BooleanParamValue;
import ru.yandex.market.core.param.model.NumberParamValue;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.param.model.StringParamValue;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.core.protocol.model.SystemActionContext;
import ru.yandex.market.core.protocol.model.UIDActionContext;
import ru.yandex.market.core.shop.ShopActionContext;
import ru.yandex.market.core.testing.ShopProgram;
import ru.yandex.market.core.testing.TestingStatus;
import ru.yandex.market.core.validation.ConstraintViolationsException;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

@DbUnitDataSet(before = "AbstractModerationFunctionalTest.csv")
@ParametersAreNonnullByDefault
class PassModerationEntryPointFunctionalTest extends AbstractModerationFunctionalTest {
    @Autowired
    EnvironmentService environmentService;

    /**
     * Успешное принятие результатов модерации.
     *
     * <ul>
     * <li>Открываем катоф QMANAGER_OTHER
     * <li>Запрашиваем модерацию
     * <li>Подтверждаем запрос
     * <li>Пропускаем задёржку повторной модерации
     * <li>Поддтверждаем загрузку фида в тестовый индекс
     * <li>Отправляем результаты модерации
     * <li>Проверяем, что результаты успешно обработались
     * </ul>
     */
    @Test
    void testPassModerationEntryPointShouldAcceptResultWhenChecking() {
        withinAction(actionId -> {
            var datasourceID = createDatasource(actionId, Set.of(ShopProgram.CPC));

            var context = new ShopActionContext(actionId, datasourceID);
            cutoffService.openCutoff(context, CutoffType.QMANAGER_OTHER);

            moderationRequestEntryPoint.requestCPCModeration(context);
            skipModerationDelayAndConfirmModerationRequest(datasourceID, ShopProgram.CPC);
            skipModerationDelayAndStartMainModerationProcess(datasourceID, ShopProgram.CPC);
            confirmModerationSandboxFeedLoad(datasourceID, ShopProgram.CPC);
            submitModerationResult(datasourceID, ShopProgram.CPC, ModerationResult.PASSED);

            var testingState = testingService.getTestingStatus(datasourceID, ShopProgram.CPC);
            assertThat(testingState.getStatus()).isEqualTo(TestingStatus.WAITING_FEED_LAST_LOAD);
        });
    }

    /**
     * Исключение при обработке результатов.
     *
     * <ul>
     * <li>Открываем катоф QMANAGER_OTHER
     * <li>Запрашиваем модерацию
     * <li>Подтверждаем запрос
     * <li>Пропускаем задёржку повторной модерации
     * <li>Отправляем результаты модерации
     * <li>Проверяем, что кинулось исключение ConstraintViolationsException
     * </ul>
     */
    @Test
    void testPassModerationEntryPointShouldThrowConstraitViolationExceptionWhenNotChecking1() {
        var datasourceID = protocolService.actionInTransaction(
                new UIDActionContext(ActionType.START_MODERATION, USER_ID),
                (status, actionId) -> {
                    var datasourceId = createDatasource(actionId, Set.of(ShopProgram.CPC));
                    var context = new ShopActionContext(actionId, datasourceId);
                    cutoffService.openCutoff(context, CutoffType.QMANAGER_OTHER);
                    moderationRequestEntryPoint.requestCPCModeration(context);
                    skipModerationDelayAndConfirmModerationRequest(datasourceId, ShopProgram.CPC);
                    skipModerationDelayAndStartMainModerationProcess(datasourceId, ShopProgram.CPC);
                    return datasourceId;
                }
        );

        assertThatExceptionOfType(ConstraintViolationsException.class)
                .isThrownBy(() -> submitModerationResult(datasourceID, ShopProgram.CPC, ModerationResult.PASSED));
    }

    /**
     * Исключение при обработке результатов.
     *
     * <ul>
     * <li>Открываем катоф QMANAGER_OTHER
     * <li>Запрашиваем модерацию
     * <li>Подтверждаем запрос
     * <li>Отправляем результаты модерации
     * <li>Проверяем, что кинулось исключение ConstraintViolationsException
     * </ul>
     */
    @Test
    void testPassModerationEntryPointShouldThrowConstraitViolationExceptionWhenNotChecking2() {
        var datasourceID = protocolService.actionInTransaction(
                new UIDActionContext(ActionType.START_MODERATION, USER_ID),
                (status, actionId) -> {
                    var datasourceId = createDatasource(actionId, Set.of(ShopProgram.CPC));
                    var context = new ShopActionContext(actionId, datasourceId);
                    cutoffService.openCutoff(context, CutoffType.QMANAGER_OTHER);
                    moderationRequestEntryPoint.requestCPCModeration(context);
                    skipModerationDelayAndConfirmModerationRequest(datasourceId, ShopProgram.CPC);
                    return datasourceId;
                }
        );

        assertThatExceptionOfType(ConstraintViolationsException.class)
                .isThrownBy(() -> submitModerationResult(datasourceID, ShopProgram.CPC, ModerationResult.PASSED));
    }

    /**
     * Исключение при обработке результатов.
     *
     * <ul>
     * <li>Открываем катоф QMANAGER_OTHER
     * <li>Запрашиваем модерацию
     * <li>Отправляем результаты модерации
     * <li>Проверяем, что кинулось исключение ConstraintViolationsException
     * </ul>
     */
    @Test
    void testPassModerationEntryPointShouldThrowConstraitViolationExceptionWhenNotChecking3() {
        var datasourceID = protocolService.actionInTransaction(
                new UIDActionContext(ActionType.START_MODERATION, USER_ID),
                (status, actionId) -> {
                    var datasourceId = createDatasource(actionId, Set.of(ShopProgram.CPC));
                    var context = new ShopActionContext(actionId, datasourceId);
                    cutoffService.openCutoff(context, CutoffType.QMANAGER_OTHER);
                    moderationRequestEntryPoint.requestCPCModeration(context);
                    return datasourceId;
                }
        );

        assertThatExceptionOfType(ConstraintViolationsException.class)
                .isThrownBy(() -> submitModerationResult(datasourceID, ShopProgram.CPC, ModerationResult.PASSED));
    }

    /**
     * Исключение при обработке результатов.
     *
     * <ul>
     * <li>Открываем катоф QMANAGER_OTHER
     * <li>Отправляем результаты модерации
     * <li>Проверяем, что кинулось исключение ConstraintViolationsException
     * </ul>
     */
    @Test
    void testPassModerationEntryPointShouldThrowConstraitViolationExceptionWhenNotChecking4() {
        var datasourceID = protocolService.actionInTransaction(
                new UIDActionContext(ActionType.START_MODERATION, USER_ID),
                (status, actionId) -> {
                    var datasourceId = createDatasource(actionId, Set.of(ShopProgram.CPC));
                    var context = new ShopActionContext(actionId, datasourceId);
                    cutoffService.openCutoff(context, CutoffType.QMANAGER_OTHER);
                    return datasourceId;
                }
        );

        assertThatExceptionOfType(ConstraintViolationsException.class)
                .isThrownBy(() -> submitModerationResult(datasourceID, ShopProgram.CPC, ModerationResult.PASSED));
    }

    /**
     * Проверка нотификации с рекомендациями по настройкам размещения,
     * отправляемой при первичном прохождении премодерации.
     */
    @Test
    void checkNotificationWithSettingsRecommendations() {
        environmentService.setValue("need.send.settings.recommendations", "true");
        withinAction(actionId -> {
            var datasourceID = createDatasource(actionId, Set.of(ShopProgram.CPC), false, true);

            paramService.setParam(new BooleanParamValue(ParamType.IS_NEWBIE, datasourceID, true), actionId);
            //устанавливаем источник доставки yml, чтобы shops_web.v_bad_shipping_info не мешал отправке уведомлений
            paramService.setParam(new StringParamValue(ParamType.DELIVERY_SOURCE, datasourceID,
                    DeliverySourceType.YML.toString()), actionId);

            when(partnerSettingsRecommendationService.getSettingsRecommendations(datasourceID))
                    .thenReturn(Map.of(
                            SettingType.ROUND_THE_CLOCK, true,
                            SettingType.DELIVERY_REGION_GROUP, true,
                            SettingType.PICKUP_POINT, false,
                            SettingType.PROMO, false
                    ));

            passModerationAndCheckNotifications(actionId, datasourceID, 1580381393L);
        });
    }

    /**
     * Проверка нотификации с рекомендациями по настройкам размещения,
     * отправляемой при первичном прохождении премодерации, со всеми возможными рекомендациями.
     */
    @Test
    void checkNotificationWithSettingsRecommendationsAll() {
        environmentService.setValue("need.send.settings.recommendations", "true");
        withinAction(actionId -> {
            var datasourceID = createDatasource(actionId, Set.of(ShopProgram.CPC), false, true);

            paramService.setParam(new BooleanParamValue(ParamType.NEVER_PAID, datasourceID, true), actionId);
            paramService.setParam(new NumberParamValue(ParamType.LOCAL_DELIVERY_REGION, datasourceID, 213), actionId);
            //устанавливаем источник доставки yml, чтобы shops_web.v_bad_shipping_info не мешал отправке уведомлений
            paramService.setParam(new StringParamValue(ParamType.DELIVERY_SOURCE, datasourceID,
                    DeliverySourceType.YML.toString()), actionId);

            when(partnerSettingsRecommendationService.getSettingsRecommendations(datasourceID))
                    .thenReturn(Map.of(
                            SettingType.ROUND_THE_CLOCK, true,
                            SettingType.DELIVERY_REGION_GROUP, true,
                            SettingType.PICKUP_POINT, true,
                            SettingType.PROMO, true
                    ));

            passModerationAndCheckNotifications(actionId, datasourceID, 1580381393L);
        });
    }

    /**
     * Проверка нотификации для SMB.
     */
    @Test
    void checkSmbNotification() {
        long datasourceID = 102;

        when(partnerSettingsRecommendationService.getSettingsRecommendations(datasourceID))
                .thenReturn(Map.of(
                        SettingType.ROUND_THE_CLOCK, true,
                        SettingType.DELIVERY_REGION_GROUP, true,
                        SettingType.PICKUP_POINT, false,
                        SettingType.PROMO, false
                ));

        withinAction(actionId -> passModerationAndCheckNotifications(actionId, datasourceID, 1590661518L));
    }

    /**
     * Успешное принятие результатов CPA модерации.
     * <p>
     * Самопроверка при этом не пройдена. Фича в SUCCESS не переводится.
     */
    @Test
    @DbUnitDataSet(
            before = "passCpaModeration.noSelfCheck.before.csv",
            after = "passCpaModeration.noSelfCheck.after.csv"
    )
    void testPassCpaModerationSelfCheckNotPassed() {
        var systemActionContext = new SystemActionContext(ActionType.START_MODERATION);
        protocolService.operationInTransaction(systemActionContext, (transactionStatus, actionId) -> {
            var context = new ShopActionContext(actionId, 200L);
            moderationRequestEntryPoint.requestCPAModeration(context);
            skipModerationDelayAndConfirmModerationRequest(200L, ShopProgram.CPA);
            skipModerationDelayAndStartMainModerationProcess(200L, ShopProgram.CPA);
            confirmModerationSandboxFeedLoad(200L, ShopProgram.CPA);
            submitModerationResult(200L, ShopProgram.CPA, ModerationResult.PASSED);
            confirmModerationSandboxFeedLoad(200L, ShopProgram.CPA);
            passModerationEntryPoint.pass(systemActionContext, new TestingShop(2001L, 200L));
        });

        verifyNoInteractions(partnerNotificationClient);
    }

    /**
     * Успешное принятие результатов CPA модерации.
     * <p>
     * Самопроверка пройдена. Фича MARKETPLACE_SELF_DELIVERY переводится в SUCCESS.
     */
    @Test
    @DbUnitDataSet(before = "passCpaModeration.selfCheckPassed.before.csv",
            after = "passCpaModeration.selfCheckPassed.after.csv")
    void testPassCpaModerationSelfCheckPassed() {
        var systemActionContext = new SystemActionContext(ActionType.START_MODERATION);
        protocolService.operationInTransaction(systemActionContext, (transactionStatus, actionId) -> {
            var context = new ShopActionContext(actionId, 200L);
            moderationRequestEntryPoint.requestCPAModeration(context);
            skipModerationDelayAndConfirmModerationRequest(200L, ShopProgram.CPA);
            skipModerationDelayAndStartMainModerationProcess(200L, ShopProgram.CPA);
            confirmModerationSandboxFeedLoad(200L, ShopProgram.CPA);
            submitModerationResult(200L, ShopProgram.CPA, ModerationResult.PASSED);
            confirmModerationSandboxFeedLoad(200L, ShopProgram.CPA);
            passModerationEntryPoint.pass(systemActionContext, new TestingShop(2001L, 200L));
        });

        verifySentNotificationType(partnerNotificationClient, 1, 1614169151L);
    }

    /**
     * Успешное принятие результатов CPA модерации, когда магазин параллельно находится на API_DEBUG проверке.
     * Фича MARKETPLACE_SELF_DELIVERY переводится в SUCCESS.
     */
    @Test
    @DbUnitDataSet(
            before = "passCpaModeration.duringApiDebug.before.csv",
            after = "passCpaModeration.duringApiDebug.after.csv"
    )
    void testPassCpaModerationSelfCheckPassedDuringApiDebug() {
        var systemActionContext = new SystemActionContext(ActionType.START_MODERATION);
        protocolService.operationInTransaction(systemActionContext, (transactionStatus, actionId) -> {
            var context = new ShopActionContext(actionId, 200L);
            moderationRequestEntryPoint.requestCPAModeration(context);
            skipModerationDelayAndConfirmModerationRequest(200L, ShopProgram.CPA);
            skipModerationDelayAndStartMainModerationProcess(200L, ShopProgram.CPA);
            confirmModerationSandboxFeedLoad(200L, ShopProgram.CPA);
            submitModerationResult(200L, ShopProgram.CPA, ModerationResult.PASSED);
            confirmModerationSandboxFeedLoad(200L, ShopProgram.CPA);
            passModerationEntryPoint.pass(systemActionContext, new TestingShop(2001L, 200L));
        });

        verifySentNotificationType(partnerNotificationClient, 1, 1614169151L);
    }

    /**
     * Успешное завершение модерации для магазина с фичей в саксесс.
     */
    @Test
    @DbUnitDataSet(
            before = "passCpaModeration.withoutTesting.before.csv",
            after = "passCpaModeration.withoutTesting.after.csv"
    )
    void testPassWithoutModeration() {
        var systemActionContext = new SystemActionContext(ActionType.TURN_SHOP_ON_WITHOUT_TESTING);
        protocolService.operationInTransaction(systemActionContext, (transactionStatus, actionId) -> {
            passModerationEntryPoint.passWithoutModerationTransactional(systemActionContext, 200L, ShopProgram.CPA);
        });
    }

    private void passModerationAndCheckNotifications(long actionId, long datasourceID, long notificationType) {
        var context = new ShopActionContext(actionId, datasourceID);
        passModeration(context, Set.of(ShopProgram.CPC));
        verifySentNotificationType(partnerNotificationClient, 1, notificationType);
    }
}
