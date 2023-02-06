package ru.yandex.market.mbi.api.controller.moderation.result.lite;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.moderation.qc.result.LiteCheckResult;
import ru.yandex.market.core.testing.TestingType;
import ru.yandex.market.mbi.api.client.entity.moderation.LiteCheckResultRequest;
import ru.yandex.market.mbi.api.client.entity.moderation.MessageRequest;
import ru.yandex.market.mbi.api.client.entity.moderation.ModerationResultResponse;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

/**
 * Тест ручки {@code /qc/shops/light-checks/result} принятия результата лайтовой CPC или GENERAL проверки.
 *
 * @author Vadim Lyalin
 */
class RegisterLiteCheckResultFunctionalTest extends FunctionalTest {

    /**
     * Тест принятия результата для магазина не на проверке. Должен вернуться статус
     * {@link ModerationResultResponse.Status#IGNORED}.
     */
    @Test
    @DbUnitDataSet(before = "RegisterLiteCheckResultFunctionalTest.ignored.before.csv")
    void testResultForNonTested() {
        var request = new LiteCheckResultRequest(1, TestingType.CPC_LITE_CHECK, LiteCheckResult.Status.OK, null);
        var response = mbiApiClient.registerLiteCheckResult(request);
        assertThat(response.getStatus()).isEqualTo(ModerationResultResponse.Status.IGNORED);
        assertThat(response.getMessage()).isEqualTo("Testing status not found. SHOP_ID: 1. SHOP_PROGRAM: CPC");
    }

    /**
     * Тест принятия результата OK для магазина, у которого проверка не в статусе
     * {@link ru.yandex.market.core.testing.TestingStatus#CHECKING}. Должен вернуться статус
     * {@link ModerationResultResponse.Status#IGNORED}.
     */
    @Test
    @DbUnitDataSet(before = "RegisterLiteCheckResultFunctionalTest.ignored.before.csv")
    void testResultOkForNonChecked() {
        var request = new LiteCheckResultRequest(2, TestingType.CPC_LITE_CHECK, LiteCheckResult.Status.OK, null);
        var response = mbiApiClient.registerLiteCheckResult(request);
        assertThat(response.getStatus()).isEqualTo(ModerationResultResponse.Status.IGNORED);
        assertThat(response.getMessage()).isEqualTo("Shop's testing status is illegal for requested action. Status: 3");
        verifyNoInteractions(partnerNotificationClient);
    }

    /**
     * Тест принятия результата проверки ОК для магазина.
     */
    @Test
    @DbUnitDataSet(before = "RegisterLiteCheckResultFunctionalTest.ok.before.csv",
            after = "RegisterLiteCheckResultFunctionalTest.ok.after.csv")
    void testResultOk() {
        var request = new LiteCheckResultRequest(2, TestingType.CPC_LITE_CHECK, LiteCheckResult.Status.OK,
                new MessageRequest(105, "subject", "body"));
        var response = mbiApiClient.registerLiteCheckResult(request);
        assertThat(response.getStatus()).isEqualTo(ModerationResultResponse.Status.OK);
        assertThat(response.getMessage()).isNull();
        verifyNoInteractions(partnerNotificationClient);
    }

    /**
     * Тест принятия результата проверки FAILED для магазина. Должно отправиться письмо.
     */
    @Test
    @DbUnitDataSet(before = "RegisterLiteCheckResultFunctionalTest.ok.before.csv",
            after = "RegisterLiteCheckResultFunctionalTest.failed.after.csv")
    void testResultFailed() {
        var request = new LiteCheckResultRequest(2, TestingType.CPC_LITE_CHECK, LiteCheckResult.Status.FAILED,
                new MessageRequest(105, "subject", "body"));
        var response = mbiApiClient.registerLiteCheckResult(request);
        assertThat(response.getStatus()).isEqualTo(ModerationResultResponse.Status.OK);
        assertThat(response.getMessage()).isNull();
        verifySentNotificationType(partnerNotificationClient, 1, 105L);
    }

}
