package ru.yandex.market.tpl.carrier.tms.dbqueue.push_carrier;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.push_carrier.notification.PushCarrierNotificationRepository;
import ru.yandex.market.tpl.carrier.core.domain.push_carrier.notification.PushCarrierNotificationService;
import ru.yandex.market.tpl.carrier.core.domain.push_carrier.notification.model.PushCarrierEvent;
import ru.yandex.market.tpl.carrier.core.domain.push_carrier.notification.model.PushCarrierNotification;
import ru.yandex.market.tpl.carrier.core.domain.push_carrier.notification.model.PushCarrierNotificationStatus;
import ru.yandex.market.tpl.carrier.core.domain.push_carrier.notification.model.PushCarrierPayload;
import ru.yandex.market.tpl.carrier.core.domain.push_carrier.notification.model.command.PushCarrierNotificationCommand;
import ru.yandex.market.tpl.carrier.core.domain.user.AppProvider;
import ru.yandex.market.tpl.carrier.tms.TmsIntTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.xiva.send.XivaSendTvmClient;
import ru.yandex.mj.generated.client.taxi_client_notify.api.TaxiClientNotifyApiClient;

@TmsIntTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PushCarrierNotificationSendServiceTest {

    private final PushCarrierNotificationService pushCarrierNotificationService;
    private final XivaSendTvmClient xivaSendTvmClient;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final PushCarrierNotificationRepository pushCarrierNotificationRepository;

    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final TaxiClientNotifyApiClient taxiClientNotifyApiClient;


    @Test
    @Deprecated(since = "pro-integration")
    void shouldSaveTransitId() {
        PushCarrierNotification pushCarrierNotification = pushCarrierNotificationService.sendPushAsync(
                PushCarrierNotificationCommand.Create.builder()
                        .event(PushCarrierEvent.DRIVER_LOST)
                        .payload(PushCarrierPayload.builder().build())
                        .xivaUserId("123")
                        .ttlSec(3600)
                        .build());

        Mockito.when(xivaSendTvmClient.send(Mockito.any()))
                .thenReturn("transitId");

        dbQueueTestUtil.assertQueueHasSize(QueueType.PUSH_CARRIER_NOTIFICATION, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.PUSH_CARRIER_NOTIFICATION);

        pushCarrierNotification = pushCarrierNotificationRepository.findByIdOrThrow(pushCarrierNotification.getId());
        Assertions.assertThat(pushCarrierNotification.getTransitId()).isEqualTo("transitId");
        Assertions.assertThat(pushCarrierNotification.getSendTime()).isNotNull();
        Assertions.assertThat(pushCarrierNotification.getStatus()).isEqualTo(PushCarrierNotificationStatus.SENT);
        Assertions.assertThat(pushCarrierNotification.getClientNotifyId()).isNull();
    }

    @Test
    void shouldSaveClientNotifyId() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.TAXI_PUSHES_SERVICE, "TAXI_PUSHES_SERVICE_VALUE");
        configurationServiceAdapter.mergeValue(ConfigurationProperties.TAXI_PUSHES_INTENT, "TAXI_PUSHES_INTENT_VALUE");


        ExecuteCall<Map<String, Object>, RetryStrategy> call = Mockito.mock(ExecuteCall.class);
        Mockito.when(call.schedule())
                .thenReturn(CompletableFuture.completedFuture(Map.of("notification_id", "clientNotifyIdValue")));
        Mockito.when(taxiClientNotifyApiClient.v2PushPost(Mockito.any(), Mockito.any()))
                .thenReturn(call);


        PushCarrierNotification pushCarrierNotification = pushCarrierNotificationService.sendPushAsync(
                PushCarrierNotificationCommand.Create.builder()
                        .event(PushCarrierEvent.DRIVER_LOST)
                        .payload(PushCarrierPayload.builder().build())
                        .xivaUserId("123")
                        .taxiId("123_456")
                        .appProvider(AppProvider.FLUTTER)
                        .ttlSec(3600)
                        .build());

        dbQueueTestUtil.assertQueueHasSize(QueueType.PUSH_CARRIER_NOTIFICATION, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.PUSH_CARRIER_NOTIFICATION);

        pushCarrierNotification = pushCarrierNotificationRepository.findByIdOrThrow(pushCarrierNotification.getId());
        Assertions.assertThat(pushCarrierNotification.getClientNotifyId()).isEqualTo("clientNotifyIdValue");
        Assertions.assertThat(pushCarrierNotification.getTransitId()).isNull();
        Assertions.assertThat(pushCarrierNotification.getSendTime()).isNotNull();
        Assertions.assertThat(pushCarrierNotification.getStatus()).isEqualTo(PushCarrierNotificationStatus.SENT);
    }

    @Test
    void shouldSaveTransitIdIfStillKotlin() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.TAXI_PUSHES_SERVICE, "TAXI_PUSHES_SERVICE_VALUE");
        configurationServiceAdapter.mergeValue(ConfigurationProperties.TAXI_PUSHES_INTENT, "TAXI_PUSHES_INTENT_VALUE");


        PushCarrierNotification pushCarrierNotification = pushCarrierNotificationService.sendPushAsync(
                PushCarrierNotificationCommand.Create.builder()
                        .event(PushCarrierEvent.DRIVER_LOST)
                        .payload(PushCarrierPayload.builder().build())
                        .xivaUserId("123")
                        .appProvider(AppProvider.KOTLIN)
                        .ttlSec(3600)
                        .build());

        Mockito.when(xivaSendTvmClient.send(Mockito.any()))
                .thenReturn("transitId");

        dbQueueTestUtil.assertQueueHasSize(QueueType.PUSH_CARRIER_NOTIFICATION, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.PUSH_CARRIER_NOTIFICATION);

        pushCarrierNotification = pushCarrierNotificationRepository.findByIdOrThrow(pushCarrierNotification.getId());
        Assertions.assertThat(pushCarrierNotification.getTransitId()).isEqualTo("transitId");
        Assertions.assertThat(pushCarrierNotification.getClientNotifyId()).isNull();
        Assertions.assertThat(pushCarrierNotification.getSendTime()).isNotNull();
        Assertions.assertThat(pushCarrierNotification.getStatus()).isEqualTo(PushCarrierNotificationStatus.SENT);
    }

}
