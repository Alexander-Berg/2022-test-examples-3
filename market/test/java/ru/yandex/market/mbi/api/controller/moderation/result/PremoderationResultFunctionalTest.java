package ru.yandex.market.mbi.api.controller.moderation.result;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.cutoff.model.AboScreenshotDto;
import ru.yandex.market.core.ds.DatasourceNotFoundException;
import ru.yandex.market.core.testing.PremoderationResultStatus;
import ru.yandex.market.core.testing.TestingType;
import ru.yandex.market.mbi.api.client.entity.moderation.MessageRequest;
import ru.yandex.market.mbi.api.client.entity.moderation.ModerationResultResponse;
import ru.yandex.market.mbi.api.client.entity.moderation.PremoderationResultRequest;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static ru.yandex.market.core.moderation.qc.result.PremoderationResult.Status.FAILED;
import static ru.yandex.market.core.moderation.qc.result.PremoderationResult.Status.HALTED;
import static ru.yandex.market.core.moderation.qc.result.PremoderationResult.Status.PASSED;
import static ru.yandex.market.core.moderation.qc.result.PremoderationResult.Status.SKIPPED;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

/**
 * Функциональные тесты на
 * {@link ru.yandex.market.mbi.api.controller.ModerationController#acceptModerationResult}.
 */
@DbUnitDataSet(before = "PremoderationResultFunctionalTest.csv")
class PremoderationResultFunctionalTest extends FunctionalTest {
    private static final long SHOP_ID = 774L;

    private static final ModerationResultResponse OK_RESPONSE =
            new ModerationResultResponse(ModerationResultResponse.Status.OK);

    private static final ModerationResultResponse SANDBOX_NOT_FOUND_RESPONSE =
            new ModerationResultResponse(ModerationResultResponse.Status.IGNORED,
                    "Sandbox state not found: shop_id = " + SHOP_ID);

    private static final ModerationResultResponse ILLEGAL_TESTING_STATUS_RESPONSE =
            new ModerationResultResponse(ModerationResultResponse.Status.IGNORED,
                    "Shop's testing status is illegal for requested action. Status: 3");
    public static final MessageRequest DEFAULT_MESSAGE = new MessageRequest(
            105,
            "subject",
            "body",
            List.of(new AboScreenshotDto(17L, "hash1"), new AboScreenshotDto(19L, "hash2")));

    /**
     * Попытка сообщить результат модерации о несуществующем магазине
     */
    @Test
    @DbUnitDataSet
    void testNonExistentShop() {
        PremoderationResultRequest request = new PremoderationResultRequest.Builder()
                .setShopId(700)
                .setTestingType(TestingType.CPC_PREMODERATION)
                .setQualityCheckStatus(PASSED)
                .setCloneCheckStatus(HALTED)
                .setOrderCheckStatus(FAILED)
                .setOrderCheckStatus(PASSED)
                .setMessage(new MessageRequest()).build();


        ModerationResultResponse response = mbiApiClient.registerPremoderationResult(request);
        assertThat(response).isEqualTo(new ModerationResultResponse(
                ModerationResultResponse.Status.ERROR, new DatasourceNotFoundException(700).getMessage()));
        checkMessageNotSend();
    }

    /**
     * Попытка сообщить данные о магазине не находящимся на модерации
     */
    @Test
    @DbUnitDataSet
    void testNonExistentModeration() {
        PremoderationResultRequest request = new PremoderationResultRequest.Builder()
                .setShopId(SHOP_ID)
                .setTestingType(TestingType.CPC_PREMODERATION)
                .setQualityCheckStatus(PASSED)
                .setCloneCheckStatus(HALTED)
                .setOrderCheckStatus(FAILED)
                .setOrderCheckStatus(PASSED)
                .setMessage(new MessageRequest()).build();

        ModerationResultResponse response = mbiApiClient.registerPremoderationResult(request);
        assertThat(response).isEqualTo(SANDBOX_NOT_FOUND_RESPONSE);
        checkMessageNotSend();
    }

    /**
     * Магазин находится на CPA-модерации, а информация сообщается по CPC-модерации, ожидается игнор
     */
    @Test
    @DbUnitDataSet(
            before = "testIncorectModerationType.before.csv",
            after = "testIncorectModerationType.after.csv"
    )
    void testIncorectModerationType() {
        PremoderationResultRequest request = new PremoderationResultRequest.Builder()
                .setShopId(SHOP_ID)
                .setTestingType(TestingType.CPC_PREMODERATION)
                .setQualityCheckStatus(PASSED)
                .setCloneCheckStatus(PASSED)
                .setOrderCheckStatus(SKIPPED)
                .setOrderCheckStatus(PASSED)
                .setMessage(new MessageRequest()).build();

        ModerationResultResponse response = mbiApiClient.registerPremoderationResult(request);
        assertThat(response).isEqualTo(SANDBOX_NOT_FOUND_RESPONSE);
        checkMessageNotSend();
    }

    /**
     * Магазин находится на CPC-модерации, успешно проходит все проверки
     * before:
     * datasources_in_testing:
     * status: CHECKING
     * after:
     * datasources_in_testing:
     * status: WAITING_FEED_LAST_LOAD
     */
    @Test
    @DbUnitDataSet(
            before = "testSuccessCpcModeration.before.csv",
            after = "testSuccessCpcModeration.after.csv"
    )
    void testSuccessCpcModeration() {
        PremoderationResultRequest request = new PremoderationResultRequest.Builder()
                .setShopId(SHOP_ID)
                .setTestingType(TestingType.CPC_PREMODERATION)
                .setQualityCheckStatus(PASSED)
                .setCloneCheckStatus(PASSED)
                .setOrderCheckStatus(SKIPPED)
                .setOffersCheckStatus(SKIPPED)
                .setMessage(new MessageRequest()).build();

        ModerationResultResponse response = mbiApiClient.registerPremoderationResult(request);
        assertThat(response).isEqualTo(OK_RESPONSE);
        checkMessageNotSend();
    }

    /**
     * Магазин находится на CPA-модерации, успешно проходит все проверки
     * before:
     * datasources_in_testing:
     * status: CHECKING
     * after:
     * datasources_in_testing:
     * status: WAITING_FEED_LAST_LOAD
     */
    @Test
    @DbUnitDataSet(
            before = "testSuccessCpaModeration.before.csv",
            after = "testSuccessCpaModeration.after.csv"
    )
    void testSuccessCpaModeration() {
        PremoderationResultRequest request = new PremoderationResultRequest.Builder()
                .setShopId(SHOP_ID)
                .setTestingType(TestingType.CPA_PREMODERATION)
                .setQualityCheckStatus(PASSED)
                .setCloneCheckStatus(PASSED)
                .setOrderCheckStatus(PASSED)
                .setOffersCheckStatus(PASSED)
                .setMessage(new MessageRequest()).build();

        ModerationResultResponse response = mbiApiClient.registerPremoderationResult(request);
        assertThat(response).isEqualTo(OK_RESPONSE);
        checkMessageNotSend();
    }

    /**
     * Магазин находится и на CPC-модерации, и на CPA, приходит положительный результат по CPA, проверяем, что
     * результат проверки не аффектит CPC
     * before:
     * datasources_in_testing:
     * status: CHECKING, testing_type: CPC_PREMODERATION
     * status: CHECKING, testing_type: CPA_PREMODERATION
     * after:
     * datasources_in_testing:
     * status: WAITING_FEED_LAST_LOAD, testing_type: CPA_PREMODERATION
     * status: CHECKING, testing_type: CPC_PREMODERATION
     */
    @Test
    @DbUnitDataSet(
            before = "testSuccessCpaParallel.before.csv",
            after = "testSuccessCpaParallel.after.csv"
    )
    void testSuccessCpaParallel() {
        PremoderationResultRequest request = new PremoderationResultRequest.Builder()
                .setShopId(SHOP_ID)
                .setTestingType(TestingType.CPA_PREMODERATION)
                .setQualityCheckStatus(PASSED)
                .setCloneCheckStatus(PASSED)
                .setOrderCheckStatus(PASSED)
                .setOffersCheckStatus(PASSED)
                .setMessage(new MessageRequest()).build();

        ModerationResultResponse response = mbiApiClient.registerPremoderationResult(request);
        assertThat(response).isEqualTo(OK_RESPONSE);
        checkMessageNotSend();
    }

    /**
     * Магазин находится и на CPC-модерации, и на CPA, приходит положительный результат по CPC, проверяем, что
     * результат проверки не аффектит CPA
     * before:
     * datasources_in_testing:
     * status: CHECKING, testing_type: CPC_PREMODERATION
     * status: CHECKING, testing_type: CPA_PREMODERATION
     * after:
     * datasources_in_testing:
     * status: WAITING_FEED_LAST_LOAD, testing_type: CPC_PREMODERATION
     * status: CHECKING, testing_type: CPA_PREMODERATION
     */
    @Test
    @DbUnitDataSet(
            before = "testSuccessCpcParallel.before.csv",
            after = "testSuccessCpcParallel.after.csv"
    )
    void testSuccessCpcParallel() {
        PremoderationResultRequest request = new PremoderationResultRequest.Builder()
                .setShopId(SHOP_ID)
                .setTestingType(TestingType.CPC_PREMODERATION)
                .setQualityCheckStatus(PASSED)
                .setCloneCheckStatus(PASSED)
                .setOrderCheckStatus(SKIPPED)
                .setOffersCheckStatus(PASSED)
                .setMessage(new MessageRequest()).build();

        ModerationResultResponse response = mbiApiClient.registerPremoderationResult(request);
        assertThat(response).isEqualTo(OK_RESPONSE);
        checkMessageNotSend();
    }

    /**
     * Переходим к негативным сценариям :) CPC-модерация, провалено качество
     * before:
     * datasources_in_testing:
     * status: CHECKING, testing_type: CPC_PREMODERATION, in_progress: 1
     * after:
     * datasources_in_testing:
     * status: READY_TO_FAIL, in_progress: 0, ready: 0, cancelled: 1, quality_check_required: 1
     */
    @Test
    @DbUnitDataSet(
            before = "testCpcQualityFailed.before.csv",
            after = "testCpcQualityFailed.after.csv"
    )
    void testCpcQualityFailed() {
        PremoderationResultRequest request = new PremoderationResultRequest.Builder()
                .setShopId(SHOP_ID)
                .setTestingType(TestingType.CPC_PREMODERATION)
                .setQualityCheckStatus(FAILED)
                .setCloneCheckStatus(PASSED)
                .setOrderCheckStatus(SKIPPED)
                .setOffersCheckStatus(PASSED)
                .setMessage(DEFAULT_MESSAGE)
                .build();

        ModerationResultResponse response = mbiApiClient.registerPremoderationResult(request);
        checkOkResultAndMessageSend(response, 105L);
    }

    /**
     * CPC-модерация halt за клоновость, открывается
     * <ul>
     * <li>{@link ru.yandex.market.core.cutoff.model.CutoffType#QMANAGER_CLONE}</li>
     * <li>{@link ru.yandex.market.core.cutoff.model.CutoffType#FORTESTING}</li>
     * </ul>
     */
    @Test
    @DbUnitDataSet(
            before = "testCpcCloneHalt.before.csv",
            after = "testCpcCloneHalt.after.csv"
    )
    void testCpcCloneHalt() {
        PremoderationResultRequest request = new PremoderationResultRequest.Builder()
                .setShopId(SHOP_ID)
                .setTestingType(TestingType.CPC_PREMODERATION)
                .setQualityCheckStatus(FAILED)
                .setCloneCheckStatus(HALTED)
                .setOrderCheckStatus(SKIPPED)
                .setOffersCheckStatus(SKIPPED)
                .setMessage(DEFAULT_MESSAGE)
                .build();

        ModerationResultResponse response = mbiApiClient.registerPremoderationResult(request);
        checkOkResultAndMessageSend(response, 105L);

    }

    /**
     * CPA-модерация завершается с halt за качество.
     */
    @Test
    @DbUnitDataSet(
            before = "testCpaHaltQuality.before.csv",
            after = "testCpaHaltQuality.after.csv"
    )
    void testCpaHaltQuality() {
        PremoderationResultRequest request = new PremoderationResultRequest.Builder()
                .setShopId(SHOP_ID)
                .setTestingType(TestingType.CPA_PREMODERATION)
                .setQualityCheckStatus(HALTED)
                .setCloneCheckStatus(HALTED)
                .setOrderCheckStatus(SKIPPED)
                .setOffersCheckStatus(HALTED)
                .setMessage(DEFAULT_MESSAGE)
                .setCutoffComment("Провалено все").build();

        ModerationResultResponse response = mbiApiClient.registerPremoderationResult(request);
        checkOkResultAndMessageSend(response, 1614360790L);
    }

    /**
     * CPС-модерация завершается с halt за качество и открываются
     * <ul>
     * <li>{@link ru.yandex.market.core.cutoff.model.CutoffType#COMMON_QUALITY}</li>
     * <li>{@link ru.yandex.market.core.cutoff.model.CutoffType#FORTESTING}</li>
     * </ul>
     */
    @Test
    @DbUnitDataSet(
            before = "testCpcHaltQuality.before.csv",
            after = "testCpcHaltQuality.after.csv"
    )
    void testCpcHaltQuality() {
        PremoderationResultRequest request = new PremoderationResultRequest.Builder()
                .setShopId(SHOP_ID)
                .setTestingType(TestingType.CPC_PREMODERATION)
                .setQualityCheckStatus(HALTED)
                .setCloneCheckStatus(SKIPPED)
                .setOrderCheckStatus(SKIPPED)
                .setOffersCheckStatus(SKIPPED)
                .setMessage(DEFAULT_MESSAGE)
                .build();

        ModerationResultResponse response = mbiApiClient.registerPremoderationResult(request);
        checkOkResultAndMessageSend(response, 105L);
    }

    /**
     * CPС-модерация завершается с halt за качество и открываются
     * <ul>
     * <li>{@link ru.yandex.market.core.cutoff.model.CutoffType#QMANAGER_FRAUD}</li>
     * <li>{@link ru.yandex.market.core.cutoff.model.CutoffType#FORTESTING}</li>
     * </ul>
     */
    @Test
    @DbUnitDataSet(
            before = "testCpcHaltOffers.before.csv",
            after = "testCpcHaltOffers.after.csv"
    )
    void testCpcHaltOffers() {
        PremoderationResultRequest request = new PremoderationResultRequest.Builder()
                .setShopId(SHOP_ID)
                .setTestingType(TestingType.CPC_PREMODERATION)
                .setQualityCheckStatus(SKIPPED)
                .setCloneCheckStatus(SKIPPED)
                .setOrderCheckStatus(SKIPPED)
                .setOffersCheckStatus(HALTED)
                .setMessage(DEFAULT_MESSAGE)
                .build();

        ModerationResultResponse response = mbiApiClient.registerPremoderationResult(request);
        checkOkResultAndMessageSend(response, 105L);
    }

    /**
     * Проверяем что можно перевести CPC магазин из состояния CHECKING в состояние NEED_INFO
     * Открытия/закрытия катофов не происходит
     * <p>
     * before:
     * datasources_in_testing:
     * status: CHECKING(4)
     * after:
     * datasources_in_testing:
     * status: NEED_INFO(13)
     */
    @Test
    @DbUnitDataSet(
            before = "testCpcOnHold.before.csv",
            after = "testCpcOnHold.after.csv"
    )
    void testCpcOnHold() {
        PremoderationResultRequest request = new PremoderationResultRequest.Builder()
                .setShopId(SHOP_ID)
                .setTestingType(TestingType.CPC_PREMODERATION)
                .setQualityCheckStatus(HALTED)
                .setCloneCheckStatus(HALTED)
                .setOrderCheckStatus(HALTED)
                .setOffersCheckStatus(HALTED)
                .setMessage(DEFAULT_MESSAGE)
                .setStatus(PremoderationResultStatus.NEED_INFO).build();

        ModerationResultResponse response = mbiApiClient.registerPremoderationResult(request);
        checkOkResultAndMessageSend(response, 105L);
    }

    /**
     * Проверяем что можно перевести CPA магазин из состояния CHECKING в состояние NEED_INFO
     * Открытия/закрытия катофов не происходит
     * <p>
     * before:
     * datasources_in_testing:
     * status: CHECKING(4)
     * after:
     * datasources_in_testing:
     * status: NEED_INFO(13)
     */
    @Test
    @DbUnitDataSet(
            before = "testCpaOnHold.before.csv",
            after = "testCpaOnHold.after.csv"
    )
    void testCpaOnHold() {
        PremoderationResultRequest request = new PremoderationResultRequest.Builder()
                .setShopId(SHOP_ID)
                .setTestingType(TestingType.CPA_PREMODERATION)
                .setQualityCheckStatus(HALTED)
                .setCloneCheckStatus(HALTED)
                .setOrderCheckStatus(HALTED)
                .setOffersCheckStatus(HALTED)
                .setMessage(DEFAULT_MESSAGE)
                .setStatus(PremoderationResultStatus.NEED_INFO).build();

        ModerationResultResponse response = mbiApiClient.registerPremoderationResult(request);
        checkOkResultAndMessageSend(response, 105L);
    }

    /**
     * Проверяем что из состояния WAITING_FEED_FIRST_LOAD(3) (отличного от CHECKING(4))
     * не получается перейти в состояние NEED_INFO (ожидается игнор)
     */
    @Test
    @DbUnitDataSet(
            before = "testIncorectStatusToSetOnHold.before.csv",
            after = "testIncorectStatusToSetOnHold.after.csv"
    )
    void testIncorectStatusToSetOnHold() {
        PremoderationResultRequest request = new PremoderationResultRequest.Builder()
                .setShopId(SHOP_ID)
                .setTestingType(TestingType.CPA_PREMODERATION)
                .setQualityCheckStatus(HALTED)
                .setCloneCheckStatus(HALTED)
                .setOrderCheckStatus(HALTED)
                .setOffersCheckStatus(HALTED)
                .setMessage(new MessageRequest())
                .setStatus(PremoderationResultStatus.NEED_INFO).build();

        ModerationResultResponse response = mbiApiClient.registerPremoderationResult(request);
        assertThat(response).isEqualTo(ILLEGAL_TESTING_STATUS_RESPONSE);
        checkMessageNotSend();
    }

    /**
     * Проверяем что можно успешно завершить CPC премодерацию из из состояния NEED_INFO
     * <p>
     * before:
     * datasources_in_testing:
     * status: NEED_INFO(13)
     * after:
     * datasources_in_testing:
     * status: PASSED(6)
     */
    @Test
    @DbUnitDataSet(
            before = "testSuccessCpcFromOnHold.before.csv",
            after = "testSuccessCpcFromOnHold.after.csv"
    )
    void testSuccessCpcFromOnHold() {
        PremoderationResultRequest request = new PremoderationResultRequest.Builder()
                .setShopId(SHOP_ID)
                .setTestingType(TestingType.CPC_PREMODERATION)
                .setQualityCheckStatus(PASSED)
                .setCloneCheckStatus(PASSED)
                .setOrderCheckStatus(SKIPPED)
                .setOffersCheckStatus(PASSED)
                .setMessage(new MessageRequest())
                .setStatus(PremoderationResultStatus.PASSED)
                .build();

        ModerationResultResponse response = mbiApiClient.registerPremoderationResult(request);
        assertThat(response).isEqualTo(OK_RESPONSE);
        checkMessageNotSend();
    }

    /**
     * Проверяем что можно успешно завершить CPA премодерацию из из состояния NEED_INFO
     * <p>
     * before:
     * datasources_in_testing:
     * status: NEED_INFO(13)
     * after:
     * datasources_in_testing:
     * status: PASSED(6)
     */
    @Test
    @DbUnitDataSet(
            before = "testSuccessCpaFromOnHold.before.csv",
            after = "testSuccessCpaFromOnHold.after.csv"
    )
    void testSuccessCpaFromOnHold() {
        PremoderationResultRequest request = new PremoderationResultRequest.Builder()
                .setShopId(SHOP_ID)
                .setTestingType(TestingType.CPA_PREMODERATION)
                .setQualityCheckStatus(PASSED)
                .setCloneCheckStatus(PASSED)
                .setOrderCheckStatus(PASSED)
                .setOffersCheckStatus(PASSED)
                .setMessage(new MessageRequest())
                .setStatus(PremoderationResultStatus.PASSED)
                .build();

        ModerationResultResponse response = mbiApiClient.registerPremoderationResult(request);
        assertThat(response).isEqualTo(OK_RESPONSE);
        checkMessageNotSend();
    }

    /**
     * Проверяем корректную обработку ошибки CPA модерации.
     */
    @Test
    @DbUnitDataSet(
            before = "testFailCpaModeration.before.csv",
            after = "testFailCpaModeration.after.csv"
    )
    void testFailCpaModeration() {
        MessageRequest messageRequest = new MessageRequest(1,
                "что-то сломалось",
                "совсем сломалось");

        PremoderationResultRequest request = new PremoderationResultRequest.Builder()
                .setShopId(SHOP_ID)
                .setTestingType(TestingType.CPA_PREMODERATION)
                .setQualityCheckStatus(FAILED)
                .setCloneCheckStatus(FAILED)
                .setOrderCheckStatus(PASSED)
                .setOffersCheckStatus(PASSED)
                .setMessage(messageRequest)
                .setStatus(PremoderationResultStatus.FAILED)
                .build();

        ModerationResultResponse response = mbiApiClient.registerPremoderationResult(request);
        checkOkResultAndMessageSend(response, 1613843664L);
    }

    /**
     * Проверяем корректную обработку фатальной ошибки CPA модерации.
     */
    @Test
    @DbUnitDataSet(
            before = "testFailCpaModeration.before.csv",
            after = "testHaltCpaModeration.after.csv"
    )
    void testHaltCpaModeration() {
        PremoderationResultRequest request = new PremoderationResultRequest.Builder()
                .setShopId(SHOP_ID)
                .setTestingType(TestingType.CPA_PREMODERATION)
                .setQualityCheckStatus(PASSED)
                .setCloneCheckStatus(HALTED)
                .setOrderCheckStatus(PASSED)
                .setOffersCheckStatus(PASSED)
                .setMessage(new MessageRequest())
                .setStatus(PremoderationResultStatus.FAILED)
                .build();

        ModerationResultResponse response = mbiApiClient.registerPremoderationResult(request);
        assertThat(response).isEqualTo(OK_RESPONSE);
    }

    /**
     * Проверяем наложение катофа MODERATION_NEED_INFO
     */
    @Test
    @DbUnitDataSet(
            before = "testHoldDBSModeration.before.csv",
            after = "testHoldDBSModeration.after.csv"
    )
    void testHoldDBSModeration() {
        PremoderationResultRequest request = new PremoderationResultRequest.Builder()
                .setShopId(SHOP_ID)
                .setTestingType(TestingType.CPA_PREMODERATION)
                .setQualityCheckStatus(PASSED)
                .setCloneCheckStatus(SKIPPED)
                .setOrderCheckStatus(SKIPPED)
                .setOffersCheckStatus(SKIPPED)
                .setMessage(new MessageRequest(52, "Это тема", "Это тело"))
                .setStatus(PremoderationResultStatus.NEED_INFO)
                .build();

        ModerationResultResponse response = mbiApiClient.registerPremoderationResult(request);
        checkOkResultAndMessageSend(response, 52L);
    }

    /**
     * Проверяем снятие катофа MODERATION_NEED_INFO при фейле модерации
     */
    @Test
    @DbUnitDataSet(
            before = "testFailDBSModerationAfterHold.before.csv",
            after = "testFailDBSModerationAfterHold.after.csv"
    )
    void testFailDBSModerationAfterHold() {
        PremoderationResultRequest request = new PremoderationResultRequest.Builder()
                .setShopId(SHOP_ID)
                .setTestingType(TestingType.CPA_PREMODERATION)
                .setQualityCheckStatus(FAILED)
                .setCloneCheckStatus(SKIPPED)
                .setOrderCheckStatus(SKIPPED)
                .setOffersCheckStatus(SKIPPED)
                .setMessage(new MessageRequest())
                .setStatus(PremoderationResultStatus.FAILED)
                .build();

        ModerationResultResponse response = mbiApiClient.registerPremoderationResult(request);
        checkOkResultAndMessageSend(response, 1613843664L);
    }

    private void checkOkResultAndMessageSend(ModerationResultResponse response, long notificationType) {
        assertThat(response).isEqualTo(OK_RESPONSE);
        verifySentNotificationType(partnerNotificationClient, 1, notificationType);
    }

    private void checkMessageNotSend() {
        verifyNoInteractions(partnerNotificationClient);
    }
}
