package ru.yandex.market.notification.notifications;

import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.order.resupply.ResupplyOrderDao;
import ru.yandex.market.core.order.resupply.UnredeemedNotificationParameter;
import ru.yandex.market.core.partner.PartnerCommonInfoService;
import ru.yandex.market.core.supplier.SupplierBasicAttributes;
import ru.yandex.market.core.xml.impl.NamedContainer;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.shop.FunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Теста на {@link ResupplyDailyNotification}
 */
class ResupplyDailyNotificationTest extends FunctionalTest {
    private ResupplyDailyNotification notification;
    @Autowired
    private EnvironmentService environmentService;

    @BeforeEach
    void setUp() throws ParseException {
        ResupplyOrderDao resupplyOrderDao = mock(ResupplyOrderDao.class);
        PartnerCommonInfoService partnerCommonInfoService = mock(PartnerCommonInfoService.class);
        CampaignService campaignService = mock(CampaignService.class);

        CampaignInfo campaignInfo1 = mock(CampaignInfo.class);
        CampaignInfo campaignInfo2 = mock(CampaignInfo.class);

        SupplierBasicAttributes supplierBasicAttributes1 = mock(SupplierBasicAttributes.class);
        SupplierBasicAttributes supplierBasicAttributes2 = mock(SupplierBasicAttributes.class);

        when(campaignInfo1.getId()).thenReturn(11L);
        when(campaignInfo2.getId()).thenReturn(22L);

        when(resupplyOrderDao.getTotalUnredeemedCises(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(
                        new UnredeemedNotificationParameter(1L, 3L, 5L),
                        new UnredeemedNotificationParameter(2L, 4L, 6L)));

        environmentService.setValue(ResupplyDailyNotification.RESUPPLY_DAILY_NOTIFICATION_ENABLED, "true");

        when(campaignService.getCampaignByDatasource(1L)).thenReturn(campaignInfo1);
        when(campaignService.getCampaignByDatasource(2L)).thenReturn(campaignInfo2);

        when(partnerCommonInfoService.getPartnerBasicAttributes(eq(campaignInfo1)))
                .thenReturn(supplierBasicAttributes1);
        when(partnerCommonInfoService.getPartnerBasicAttributes(eq(campaignInfo2)))
                .thenReturn(supplierBasicAttributes2);

        when(supplierBasicAttributes1.name()).thenReturn("First");
        when(supplierBasicAttributes2.name()).thenReturn("Second");

        CronNotificationSchedule schedule = new CronNotificationSchedule(
                "0 0 10 ? * MON,TUE,WED,THU,FRI",
                ZoneId.of("Europe/Moscow"));
        notification = new ResupplyDailyNotification(
                schedule,
                resupplyOrderDao,
                campaignService,
                environmentService,
                partnerCommonInfoService
        );
    }

    @Test
    void testGetNotificationId() {
        assertEquals("ResupplyDailyNotification", notification.getNotificationId());
    }

    @Test
    void testGetPartnerIds() {
        assertThat(notification.getPartnerIds(), containsInAnyOrder(1L, 2L));
    }

    @Test
    void testGetNextTime() {
        Instant time = ZonedDateTime.parse("2021-02-06T14:41:45+03:00").toInstant();

        assertEquals(ZonedDateTime.parse("2021-02-08T10:00+03:00").toInstant(),
                notification.getNextNotificationTimeAfter(time));
    }

    @Test
    void testGetNotification() {
        long partnerId = 1L;

        assertEquals(List.of(1L, 2L).toString(), notification.getPartnerIds().toString());
        var ctx = notification.getPartnerNotification(partnerId).orElseThrow();

        List<Object> expectedData = new ArrayList<>();
        expectedData.add(new NamedContainer("campaign-id", 11L));
        expectedData.add(new NamedContainer("supplier-name", "First"));
        expectedData.add(new NamedContainer("total", 8));
        expectedData.add(new NamedContainer("fit", 3));
        expectedData.add(new NamedContainer("defect", 5));

        assertEquals(1622611699, ctx.getTypeId());
        assertEquals(partnerId, ctx.getShopId());
        assertEquals(expectedData.toString(), ctx.getData().toString());
    }
}
