package ru.yandex.market.logistics.nesu.jobs;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.message.model.MessageRecipients;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.SenderSearchFilterDto;
import ru.yandex.market.logistics.lom.model.enums.PlatformClient;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.executor.FirstSentShopOrderNotificationsExecutor;
import ru.yandex.market.logistics.nesu.jobs.producer.SendNotificationProducer;
import ru.yandex.market.logistics.nesu.service.mbi.NotificationService;
import ru.yandex.market.logistics.nesu.service.sender.SenderService;
import ru.yandex.market.logistics.nesu.service.shop.ShopService;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DatabaseSetup("/jobs/executors/database_prepare.xml")
@DisplayName("Оповещения об отправке магазином своего первого заказа")
class FirstSentShopOrderNotificationsExecutorTest extends AbstractContextualTest {

    private static final Integer MBI_SALES_NOTIFICATION_ID = 1581398796;
    private static final String YADO_SALES_EMAIL = "sales@delivery.yandex.ru";

    @Autowired
    private LomClient lomClient;

    @Autowired
    private ShopService shopService;

    @Autowired
    private SenderService senderService;

    @Autowired
    private SendNotificationProducer sendNotificationProducer;

    @Autowired
    private NotificationService notificationService;

    private FirstSentShopOrderNotificationsExecutor firstSentShopOrderNotificationsExecutor;

    @BeforeEach
    public void setup() {
        firstSentShopOrderNotificationsExecutor = new FirstSentShopOrderNotificationsExecutor(
            shopService,
            senderService,
            lomClient,
            notificationService
        );
        doNothing().when(sendNotificationProducer).produceTask(anyInt(), any(MessageRecipients.class), anyString());
    }

    @Test
    @DisplayName("Успешная отправка письма")
    @JpaQueriesCount(5)
    public void success() {
        mockLomClient(List.of(1L, 3L));
        firstSentShopOrderNotificationsExecutor.doJob(null);

        MessageRecipients messageRecipients = new MessageRecipients();
        messageRecipients.setToAddressList(List.of(YADO_SALES_EMAIL));

        ArgumentCaptor<String> xmlCaptor = ArgumentCaptor.forClass(String.class);

        verify(sendNotificationProducer, times(1)).produceTask(
            eq(MBI_SALES_NOTIFICATION_ID),
            eq(messageRecipients),
            xmlCaptor.capture()
        );
        softly.assertThat(xmlCaptor.getValue()).isXmlEqualTo(extractFileContent("jobs/executors/shop.xml"));
    }

    @Test
    @DisplayName("Отсутствуют сендеры с необходимым статусом заказов")
    public void noSendersWithStatus() {
        mockLomClient(List.of());
        firstSentShopOrderNotificationsExecutor.doJob(null);

        verify(sendNotificationProducer, never()).produceTask(
            anyInt(),
            any(MessageRecipients.class),
            anyString()
        );
    }

    @DatabaseSetup("/jobs/executors/database_without_required_shops.xml")
    @Test
    @DisplayName("Отсутствуют магазины")
    public void noShops() {
        firstSentShopOrderNotificationsExecutor.doJob(null);

        verify(sendNotificationProducer, never()).produceTask(
            anyInt(),
            any(MessageRecipients.class),
            anyString()
        );
    }

    private void mockLomClient(List<Long> returnIds) {
        doReturn(returnIds)
            .when(lomClient)
            .searchSenders(
                SenderSearchFilterDto.builder()
                    .senderIds(Set.of(1L, 3L))
                    .platformClient(PlatformClient.YANDEX_DELIVERY)
                    .haveWaybillSegmentStatuses(Set.of(SegmentStatus.IN))
                    .build()
            );
    }
}
