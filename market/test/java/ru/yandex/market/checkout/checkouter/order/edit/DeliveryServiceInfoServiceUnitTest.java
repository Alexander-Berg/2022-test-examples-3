package ru.yandex.market.checkout.checkouter.order.edit;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.market.checkout.checkouter.order.changerequest.AvailableOptionType;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.checkout.checkouter.order.changerequest.DeliveryServiceCustomerInfo;
import ru.yandex.market.checkout.checkouter.order.changerequest.MethodOfChange;
import ru.yandex.market.checkout.checkouter.order.changerequest.TrackOrderSource;
import ru.yandex.market.checkout.checkouter.order.edit.customerinfo.DeliveryServiceSubtype;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.checkout.checkouter.order.edit.customerinfo.DeliveryServiceSubtype.CONTRACT_COURIER;
import static ru.yandex.market.checkout.checkouter.order.edit.customerinfo.DeliveryServiceSubtype.CONTRACT_OUTLET;
import static ru.yandex.market.checkout.checkouter.order.edit.customerinfo.DeliveryServiceSubtype.COURIER_PLATFORM_PARTNER;
import static ru.yandex.market.checkout.checkouter.order.edit.customerinfo.DeliveryServiceSubtype.GO_PLATFORM;
import static ru.yandex.market.checkout.checkouter.order.edit.customerinfo.DeliveryServiceSubtype.MARKET_BRANDED_OUTLET;
import static ru.yandex.market.checkout.checkouter.order.edit.customerinfo.DeliveryServiceSubtype.MARKET_COURIER;
import static ru.yandex.market.checkout.checkouter.order.edit.customerinfo.DeliveryServiceSubtype.MARKET_LOCKER;
import static ru.yandex.market.checkout.checkouter.order.edit.customerinfo.DeliveryServiceSubtype.SANDBOX_LOCKER;
import static ru.yandex.market.checkout.checkouter.order.edit.customerinfo.DeliveryServiceSubtype.UNKNOWN;
import static ru.yandex.market.checkout.checkouter.order.edit.customerinfo.DeliveryServiceSubtype.YA_TAXI_AVIA;
import static ru.yandex.market.checkout.checkouter.order.edit.customerinfo.DeliveryServiceSubtype.YA_TAXI_EXPRESS;
import static ru.yandex.market.checkout.checkouter.order.edit.customerinfo.DeliveryServiceSubtype.YA_TAXI_LAVKA;

public class DeliveryServiceInfoServiceUnitTest {

    private static final long FIRST_DELIVERY_SERVICE_ID = 1L;
    private static final long SECOND_DELIVERY_SERVICE_ID = 2L;

    private DeliveryServiceInfoServiceImpl deliveryServiceCustomerInfoService;

    public static Stream<Arguments> partnerData() {
        return Stream.of(
                new Object[]{2, CONTRACT_COURIER},
                new Object[]{239, MARKET_COURIER},
                new Object[]{1000011, MARKET_BRANDED_OUTLET},
                new Object[]{1000012, CONTRACT_OUTLET},
                new Object[]{1000013, YA_TAXI_LAVKA},
                new Object[]{1000014, YA_TAXI_EXPRESS},
                new Object[]{1000015, MARKET_LOCKER},
                new Object[]{1000016, YA_TAXI_AVIA},
                new Object[]{1000017, SANDBOX_LOCKER},
                new Object[]{1000018, COURIER_PLATFORM_PARTNER},
                new Object[]{1000019, GO_PLATFORM},
                new Object[]{1000020, UNKNOWN}
        ).map(Arguments::of);
    }

    @BeforeEach
    void setUp() throws Exception {
        ClassPathResource resource = new ClassPathResource("/partner_customer_info.json",
                DeliveryServiceInfoServiceUnitTest.class);
        deliveryServiceCustomerInfoService = new DeliveryServiceInfoServiceImpl(
                resource, "239, 1005357", "1005288, 1005111, 1005363, 1005453, 1005393");
        deliveryServiceCustomerInfoService.reload();
    }

    @Test
    void shouldNotLoadPhonesAndSite() {
        DeliveryServiceCustomerInfo expected = new DeliveryServiceCustomerInfo(
                "PartnerOne",
                Arrays.asList("+7-(912)-345-67-89", "+7-(912)-345-67-88"),
                "www.partner1-site.ru",
                TrackOrderSource.ORDER_NO,
                UNKNOWN);

        DeliveryServiceCustomerInfo actual = deliveryServiceCustomerInfoService.getDeliveryServiceCustomerInfoById(
                FIRST_DELIVERY_SERVICE_ID);
        assertNotNull(actual);
        assertEquals(expected, actual);
    }

    @Test
    void shouldLoadWithoutPhones() {
        DeliveryServiceCustomerInfo expected = new DeliveryServiceCustomerInfo(
                "MarketCourier",
                Collections.emptyList(),
                "www.partner239-site.ru",
                TrackOrderSource.ORDER_NO,
                MARKET_COURIER);

        DeliveryServiceCustomerInfo actual = deliveryServiceCustomerInfoService.getDeliveryServiceCustomerInfoById(
                239L);
        assertNotNull(actual);
        assertEquals(expected, actual);
    }

    @Test
    void shouldLoadOnePhone() {
        DeliveryServiceCustomerInfo expected = new DeliveryServiceCustomerInfo(
                "MarketMoscowPickupTerminal",
                Collections.singletonList("+7-(912)-345-67-11"),
                "www.partner1005111-site.ru",
                TrackOrderSource.ORDER_NO,
                DeliveryServiceSubtype.UNKNOWN);

        DeliveryServiceCustomerInfo actual = deliveryServiceCustomerInfoService.getDeliveryServiceCustomerInfoById(
                1005111L);
        assertNotNull(actual);
        assertEquals(expected, actual);
    }

    @Test
    void shouldLoadSeveralPhones() {
        DeliveryServiceCustomerInfo expected = new DeliveryServiceCustomerInfo(
                "PartnerTwo",
                Arrays.asList("+7-(912)-345-67-80", "+7-(912)-345-67-81"),
                "www.partner2-site.ru",
                TrackOrderSource.DS_TRACK_CODE,
                CONTRACT_COURIER);

        DeliveryServiceCustomerInfo actual = deliveryServiceCustomerInfoService.getDeliveryServiceCustomerInfoById(
                SECOND_DELIVERY_SERVICE_ID);
        assertNotNull(actual);
        assertEquals(expected, actual);
    }

    @Test
    void shouldLoadPossibleOrderChanges() {
        List<PossibleOrderChange> possibleOrderChangeForFirstDS =
                deliveryServiceCustomerInfoService.getPossibleOrderChanges(FIRST_DELIVERY_SERVICE_ID);

        assertThat(possibleOrderChangeForFirstDS, empty());

        List<PossibleOrderChange> possibleOrderChangesForSecondDS =
                deliveryServiceCustomerInfoService.getPossibleOrderChanges(SECOND_DELIVERY_SERVICE_ID);

        assertThat(possibleOrderChangesForSecondDS, hasSize(4));
        assertThat(possibleOrderChangesForSecondDS,
                containsInAnyOrder(
                        new PossibleOrderChange(ChangeRequestType.DELIVERY_DATES, MethodOfChange.PARTNER_API,
                                null, 10),
                        new PossibleOrderChange(ChangeRequestType.DELIVERY_DATES, MethodOfChange.PARTNER_SITE,
                                null, 48),
                        new PossibleOrderChange(ChangeRequestType.DELIVERY_DATES, MethodOfChange.PARTNER_PHONE,
                                null, 48),
                        new PossibleOrderChange(ChangeRequestType.EXTEND_PICKUP_STORAGE_TIME,
                                MethodOfChange.PARTNER_SITE, null, 48)));
    }

    @Test
    void shouldLoadPossibleOrderOption() {
        final List<PossibleOrderOption> possibleOrderOptionsForFirstDS =
                deliveryServiceCustomerInfoService.getPossibleOrderOptions(FIRST_DELIVERY_SERVICE_ID);
        assertThat(possibleOrderOptionsForFirstDS, empty());

        final List<PossibleOrderOption> possibleOrderOptionsForSecondDS =
                deliveryServiceCustomerInfoService.getPossibleOrderOptions(SECOND_DELIVERY_SERVICE_ID);
        assertThat(possibleOrderOptionsForSecondDS, hasSize(3));
        assertThat(possibleOrderOptionsForSecondDS,
                containsInAnyOrder(
                        new PossibleOrderOption(AvailableOptionType.SHOW_RUNNING_COURIER, 48, 49),
                        new PossibleOrderOption(AvailableOptionType.OPEN_PICKUP_TERMINAL, 45, 50),
                        new PossibleOrderOption(AvailableOptionType.CALL_COURIER, 45, 31)
                ));
    }

    @Test
    void shouldEnrichMarketCourier() {
        final List<PossibleOrderOption> possibleOrderOptions =
                deliveryServiceCustomerInfoService.getPossibleOrderOptions(239L);
        assertThat(possibleOrderOptions, hasSize(1));
        assertThat(possibleOrderOptions, contains(
                new PossibleOrderOption(AvailableOptionType.SHOW_RUNNING_COURIER, 48, 49)));
    }

    @Test
    void shouldEnrichMarketPickupTerminal() {
        final List<PossibleOrderOption> possibleOrderOptions =
                deliveryServiceCustomerInfoService.getPossibleOrderOptions(1005111L);
        assertThat(possibleOrderOptions, hasSize(1));
        assertThat(possibleOrderOptions, contains(
                new PossibleOrderOption(AvailableOptionType.OPEN_PICKUP_TERMINAL, 45, 50)));
    }

    @Test
    void shouldNotEnrichMarketPickupTerminalIfAlreadyExists() {
        final List<PossibleOrderOption> possibleOrderOptions =
                deliveryServiceCustomerInfoService.getPossibleOrderOptions(1005288L);
        assertThat(possibleOrderOptions, hasSize(1));
        assertThat(possibleOrderOptions, contains(
                new PossibleOrderOption(AvailableOptionType.OPEN_PICKUP_TERMINAL, 44, 49)));
    }

    @ParameterizedTest(name = "Partner with id {0} should have subtype {1}")
    @MethodSource("partnerData")
    void shouldLoadPartnerSubtype(long partnerId, DeliveryServiceSubtype expectedSubtype) {
        DeliveryServiceCustomerInfo actual = deliveryServiceCustomerInfoService.getDeliveryServiceCustomerInfoById(
                partnerId);

        assertNotNull(actual);
        assertEquals(expectedSubtype, actual.getSubtype());
    }
}
