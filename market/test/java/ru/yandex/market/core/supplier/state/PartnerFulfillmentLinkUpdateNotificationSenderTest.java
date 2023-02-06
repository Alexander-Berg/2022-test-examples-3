package ru.yandex.market.core.supplier.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.business.BusinessService;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.contact.ContactService;
import ru.yandex.market.core.delivery.DeliveryInfoService;
import ru.yandex.market.core.delivery.tariff.db.dao.CalculatorLogDao;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.supplier.SupplierService;
import ru.yandex.market.partner.notification.client.model.SendNotificationRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "PartnerFulfillmentLinkUpdateNotificationSenderTest.before.csv")
class PartnerFulfillmentLinkUpdateNotificationSenderTest extends FunctionalTest {
    @Autowired
    NotificationService notificationService;

    @Autowired
    private ContactService contactService;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private CalculatorLogDao calculatorLogDao;

    @Autowired
    private DeliveryInfoService deliveryInfoService;

    @Autowired
    private BusinessService businessService;

    private PartnerFulfillmentLinkUpdateListener partnerFulfillmentLinkUpdateListener;

    @BeforeEach
    void init() {
        var sender = new PartnerFulfillmentLinkUpdateNotificationSender(notificationService,
                campaignService,
                supplierService,
                contactService
        );
        var awareService = mock(PartnerTypeAwareService.class);
        when(awareService.isDropship(anyLong())).thenReturn(true);

        partnerFulfillmentLinkUpdateListener = new PartnerFulfillmentLinkUpdateListener(
                calculatorLogDao,
                awareService,
                deliveryInfoService,
                businessService,
                sender
        );
    }

    @DisplayName("Проверка формирования данных для сообщения о регистрации 1 склада FBS экспресс")
    @Test
    void dataNotificationTest() {
        var event = new PartnerFulfillmentLinkChangeEvent(1159L, 10L, 199L, PartnerFulfillmentLinkChangeEvent.ChangeType.UPDATE);
        partnerFulfillmentLinkUpdateListener.handleLinkChangedEvent(event);
        var captor = ArgumentCaptor.forClass(SendNotificationRequest.class);
        verify(partnerNotificationClient, atLeastOnce()).sendNotification(captor.capture());
        String expectedData = "<supplier-name>Star of Death</supplier-name><supplier-id>1159</supplier-id>";
        String actualData = captor.getValue().getData().replaceAll("<data[^>]*>(.*)</data>", "$1");
        assertEquals(actualData, expectedData);
    }
}
