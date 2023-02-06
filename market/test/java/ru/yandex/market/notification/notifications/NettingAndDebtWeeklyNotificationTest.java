package ru.yandex.market.notification.notifications;

import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.api.cpa.yam.entity.PrepayRequest;
import ru.yandex.market.api.cpa.yam.service.PrepayRequestService;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.order.BankOrderInfoDao;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramService;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramStatus;
import ru.yandex.market.core.supplier.SupplierExposedActService;
import ru.yandex.market.core.supplier.SupplierService;
import ru.yandex.market.core.xml.impl.NamedContainer;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.shop.FunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NettingAndDebtWeeklyNotificationTest extends FunctionalTest {

    private PeriodicNotificationWithoutPreparation notification;
    private Clock clockMock;
    private PartnerPlacementProgramService partnerPlacementProgramServiceMock;
    private PrepayRequestService prepayRequestServiceMock;

    @Autowired
    private SupplierExposedActService supplierExposedActService;

    @Autowired
    private BankOrderInfoDao bankOrderInfoDao;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private EnvironmentService environmentService;

    @BeforeEach
    void setUp() throws ParseException {
        var schedule = new CronNotificationSchedule("0 0 9 ? *  MON", ZoneId.of("Europe/Moscow"));

        clockMock = mock(Clock.class);
        partnerPlacementProgramServiceMock = mock(PartnerPlacementProgramService.class);
        prepayRequestServiceMock = mock(PrepayRequestService.class);

        notification = new NettingAndDebtWeeklyNotification(
                schedule,
                clockMock,
                partnerPlacementProgramServiceMock,
                prepayRequestServiceMock,
                supplierExposedActService,
                bankOrderInfoDao,
                environmentService,
                supplierService);
    }

    @Test
    void testGetNotificationId() {
        assertEquals("NettingAndDebtWeeklyNotification", notification.getNotificationId());
    }

    @Test
    void testGetPartnerIdsWhenNotificationEnabled() {
        environmentService.setValue("partner.notification.netting_and_debt.enable", "true");
        when(partnerPlacementProgramServiceMock.findPartnersByProgramTypesAndStatus(
                any(), eq(PartnerPlacementProgramStatus.SUCCESS)))
                .thenReturn(Set.of(1L, 101L));

        assertThat(notification.getPartnerIds(), containsInAnyOrder(1L, 101L));
    }

    @Test
    void testGetPartnerIdsWhenNotificationDisabled() {
        environmentService.setValue("partner.notification.netting_and_debt.enable", "false");
        when(partnerPlacementProgramServiceMock.findPartnersByProgramTypesAndStatus(
                any(), eq(PartnerPlacementProgramStatus.SUCCESS)))
                .thenReturn(Set.of(1L, 101L));

        assertTrue(notification.getPartnerIds().isEmpty());
    }

    @Test
    void testGetNextTime() {
        Instant time = ZonedDateTime.parse("2021-02-04T14:41:45+03:00").toInstant();

        assertEquals(ZonedDateTime.parse("2021-02-08T09:00+03:00").toInstant(),
                notification.getNextNotificationTimeAfter(time));
    }

    @Test
    @DbUnitDataSet(before = "NettingAndDebtWeeklyNotificationTest.before.csv")
    void testGetPartnerNotificationWithoutFulfillmentDebtAndWithAwardDebt() {
        long partnerId = 501L;
        String organizationName = "Ромашка";
        String email = "somebody@someshop.ru";
        Instant now = ZonedDateTime.parse("2019-04-01T09:01:45+03:00").toInstant();

        setDocumentsFlagState(true);
        when(clockMock.instant()).thenReturn(now);
        when(prepayRequestServiceMock.findLastRequest(partnerId))
                .thenReturn(getPrepayRequest(organizationName, email));

        var ctx = notification.getPartnerNotification(partnerId).orElseThrow();

        var expectedData = List.of(

                new NamedContainer("supplier-campaign-id", 701L),
                new NamedContainer("supplier-name", "Ромашка"),
                new NamedContainer("supplier-email", email),
                new NamedContainer("fulfillment-debt", "0"),
                new NamedContainer("fulfillment-last-week-debt", "0"),
                new NamedContainer("fulfillment-held-payments-amount", "0"),
                new NamedContainer("fulfillment-payments-amount", "700"),
                new NamedContainer("fulfillment-account", ""),
                new NamedContainer("award-debt", "1 980"),
                new NamedContainer("award-account", "account100"),
                new NamedContainer("subsidy-paid", "600"),
                new NamedContainer("subsidy-missing-documents-months", "")
        );

        assertEquals(1612359815, ctx.getTypeId());
        assertEquals(partnerId, ctx.getShopId());
        assertEquals(expectedData, ctx.getData());
    }

    @Test
    @DbUnitDataSet(before = "NettingAndDebtWeeklyNotificationTest.before.csv")
    void testGetPartnerNotificationWithFulfillmentDebtAndWithoutAwardDebt() {
        long partnerId = 502L;
        String organizationName = "Ромашка";
        String email = "somebody@someshop.ru";
        Instant now = ZonedDateTime.parse("2019-04-01T09:01:45+03:00").toInstant();

        setDocumentsFlagState(true);
        when(clockMock.instant()).thenReturn(now);
        when(prepayRequestServiceMock.findLastRequest(partnerId))
                .thenReturn(getPrepayRequest(organizationName, email));

        var ctx = notification.getPartnerNotification(partnerId).orElseThrow();

        var expectedData = List.of(
                new NamedContainer("supplier-campaign-id", 702L),
                new NamedContainer("supplier-name", "Василёк"),
                new NamedContainer("supplier-email", email),
                new NamedContainer("fulfillment-debt", "100"),
                new NamedContainer("fulfillment-last-week-debt", "1 698"),
                new NamedContainer("fulfillment-held-payments-amount", "1 598"),
                new NamedContainer("fulfillment-payments-amount", "0"),
                new NamedContainer("fulfillment-account", "account101"),
                new NamedContainer("award-debt", "0"),
                new NamedContainer("award-account", ""),
                new NamedContainer("subsidy-paid", "100"),
                new NamedContainer("subsidy-missing-documents-months", "ноябрь, декабрь и февраль")
        );

        assertEquals(1612359815, ctx.getTypeId());
        assertEquals(partnerId, ctx.getShopId());
        assertEquals(expectedData, ctx.getData());
    }


    @Test
    @DbUnitDataSet(before = "NettingAndDebtWeeklyNotificationTest.before.csv")
    void testGetPartnerNotificationWithFulfillmentDebtAndWithoutAwardDebtWhenDocumentsFlagDisabled() {
        long partnerId = 502L;
        String organizationName = "Ромашка";
        String email = "somebody@someshop.ru";
        Instant now = ZonedDateTime.parse("2019-04-01T09:01:45+03:00").toInstant();

        setDocumentsFlagState(false);
        when(clockMock.instant()).thenReturn(now);
        when(prepayRequestServiceMock.findLastRequest(partnerId))
                .thenReturn(getPrepayRequest(organizationName, email));

        var ctx = notification.getPartnerNotification(partnerId).orElseThrow();

        var expectedData = List.of(
                new NamedContainer("supplier-campaign-id", 702L),
                new NamedContainer("supplier-name", "Василёк"),
                new NamedContainer("supplier-email", email),
                new NamedContainer("fulfillment-debt", "100"),
                new NamedContainer("fulfillment-last-week-debt", "1 698"),
                new NamedContainer("fulfillment-held-payments-amount", "1 598"),
                new NamedContainer("fulfillment-payments-amount", "0"),
                new NamedContainer("fulfillment-account", "account101"),
                new NamedContainer("award-debt", "0"),
                new NamedContainer("award-account", ""),
                new NamedContainer("subsidy-paid", "100"),
                new NamedContainer("subsidy-missing-documents-months", "")
        );

        assertEquals(1612359815, ctx.getTypeId());
        assertEquals(partnerId, ctx.getShopId());
        assertEquals(expectedData, ctx.getData());
    }

    @Test
    @DbUnitDataSet(before = "NettingAndDebtWeeklyNotificationTest.before.csv")
    void testNoEmptyNotification() {
        long partnerId = 5000L;
        long campaignId = 100L;
        String organizationName = "Ромашка";
        String email = "somebody@someshop.ru";
        Instant now = ZonedDateTime.parse("2019-04-01T09:01:45+03:00").toInstant();

        setDocumentsFlagState(false);
        when(clockMock.instant()).thenReturn(now);
        when(prepayRequestServiceMock.findLastRequest(partnerId))
                .thenReturn(getPrepayRequest(organizationName, email));

        assertTrue(notification.getPartnerNotification(partnerId).isEmpty());
    }

    private PrepayRequest getPrepayRequest(String organizationName, String email) {
        var result = new PrepayRequest(0, PrepayType.UNKNOWN, PartnerApplicationStatus.COMPLETED, 0);
        result.setOrganizationName(organizationName);
        result.setEmail(email);
        return result;
    }

    private void setDocumentsFlagState(boolean enabled) {
        environmentService.setValue("partner.notification.netting_and_debt.enable", "true");
        String flagName = "partner.notification.netting_and_debt.documents_section_enable";
        environmentService.setValue(flagName, String.valueOf(enabled));
        /* значение флага секции документов запоминается в getPartnerIds(), чтоб не читать на каждого партнера */
        notification.getPartnerIds();
    }
}
