package ru.yandex.market.logistics.nesu.jobs.consumer;

import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.PartnerExternalParamRequest;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.model.PushDbsShopLicenseToLmsPayload;
import ru.yandex.market.logistics.nesu.jobs.model.QueueType;
import ru.yandex.market.logistics.nesu.model.entity.type.ShopLicenseType;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Изменение лицензии партнера в LMS")
class PushDbsShopLicenseToLmsConsumerTest extends AbstractContextualTest {
    private static final long PARTNER_ID = 900L;
    private static final PartnerExternalParamRequest CAN_SELL_MEDICINE_REQUEST =
        new PartnerExternalParamRequest(PartnerExternalParamType.CAN_SELL_MEDICINE, "true");
    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private PushDbsShopLicenseToLmsConsumer pushDbsShopLicenseToLmsConsumer;

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Нет записи в shop_partner_settings - последняя попытка запуска таски (таска будет удалена)")
    void noShopPartnerSettingsLastAttempt() {
        TaskExecutionResult result = pushDbsShopLicenseToLmsConsumer.execute(
            createTask(QueueType.PUSH_SHOP_LICENSE_TO_LMS.getAttemptsNumber())
        );
        softly.assertThat(result.getActionType()).isEqualTo(TaskExecutionResult.Type.FINISH);
    }

    @Test
    @DisplayName("Нет записи в shop_partner_settings - не последняя попытка запуска таски (таска будет перевыставлена)")
    void noShopPartnerSettingsNotLastAttempt() {
        TaskExecutionResult result = pushDbsShopLicenseToLmsConsumer.execute(createTask());
        softly.assertThat(result.getActionType()).isEqualTo(TaskExecutionResult.Type.FAIL);
    }

    @Test
    @DatabaseSetup("/jobs/consumer/push_dbs_license_to_lms/before/shop_partner_settings.xml")
    @DisplayName("Нет записи в shop_license")
    void noShopLicense() {
        TaskExecutionResult result = pushDbsShopLicenseToLmsConsumer.execute(createTask());
        softly.assertThat(result.getActionType()).isEqualTo(TaskExecutionResult.Type.FAIL);
    }

    @Test
    @DatabaseSetup({
        "/jobs/consumer/push_dbs_license_to_lms/before/shop_partner_settings.xml",
        "/jobs/consumer/push_dbs_license_to_lms/before/shop_license.xml",
    })
    @DisplayName("LMS ответил ошибкой")
    void lmsClientException() {
        when(lmsClient.addOrUpdatePartnerExternalParams(eq(PARTNER_ID), eq(List.of(CAN_SELL_MEDICINE_REQUEST))))
            .thenThrow(new RuntimeException("500 - Internal server error"));

        TaskExecutionResult result = pushDbsShopLicenseToLmsConsumer.execute(createTask());
        softly.assertThat(result.getActionType()).isEqualTo(TaskExecutionResult.Type.FAIL);

        verify(lmsClient).addOrUpdatePartnerExternalParams(eq(PARTNER_ID), eq(List.of(CAN_SELL_MEDICINE_REQUEST)));
    }

    @Test
    @DatabaseSetup({
        "/jobs/consumer/push_dbs_license_to_lms/before/shop_partner_settings.xml",
        "/jobs/consumer/push_dbs_license_to_lms/before/shop_license.xml",
    })
    @DisplayName("Успешное обновление в LMS")
    void success() {
        TaskExecutionResult result = pushDbsShopLicenseToLmsConsumer.execute(createTask());
        softly.assertThat(result.getActionType()).isEqualTo(TaskExecutionResult.Type.FINISH);
        verify(lmsClient).addOrUpdatePartnerExternalParams(eq(PARTNER_ID), eq(List.of(CAN_SELL_MEDICINE_REQUEST)));
    }

    @Nonnull
    private Task<PushDbsShopLicenseToLmsPayload> createTask() {
        return createTask(0);
    }

    @Nonnull
    private Task<PushDbsShopLicenseToLmsPayload> createTask(long attemptsCount) {
        return new Task<>(
            new QueueShardId("1"),
            new PushDbsShopLicenseToLmsPayload(REQUEST_ID, 200, ShopLicenseType.CAN_SELL_MEDICINE),
            attemptsCount,
            clock.instant().atZone(DateTimeUtils.MOSCOW_ZONE),
            null,
            null
        );
    }
}
