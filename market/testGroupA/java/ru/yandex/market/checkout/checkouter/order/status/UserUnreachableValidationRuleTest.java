package ru.yandex.market.checkout.checkouter.order.status;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.geo.GeoRegionService;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.service.communicationproxy.CommunicationProxyClient;
import ru.yandex.market.checkout.checkouter.service.communicationproxy.CommunicationProxyService;
import ru.yandex.market.checkout.checkouter.service.communicationproxy.api.CallInfo;
import ru.yandex.market.checkout.checkouter.service.communicationproxy.api.CallResolution;
import ru.yandex.market.checkout.checkouter.service.communicationproxy.api.CallsResponseDto;
import ru.yandex.market.checkout.checkouter.shop.OrderVisibility;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class UserUnreachableValidationRuleTest {

    private CheckouterFeatureReader checkouterFeatureReader;
    private UserUnreachableValidationRule userUnreachableValidationRule;
    private CommunicationProxyService communicationProxyService;
    private CommunicationProxyClient communicationProxyClient;
    private GeoRegionService geoRegionService;
    private ShopService shopService;
    private ShopMetaData shopMetaData;

    @BeforeEach
    public void setUp() {
        checkouterFeatureReader = mock(CheckouterFeatureReader.class);
        communicationProxyClient = mock(CommunicationProxyClient.class);
        geoRegionService = mock(GeoRegionService.class);
        shopService = mock(ShopService.class);
        shopMetaData = mock(ShopMetaData.class);
        when(checkouterFeatureReader.getBoolean(eq(BooleanFeatureType.ENABLE_USER_UNREACHABLE_VALIDATION)))
                .thenReturn(true);
        lenient().when(geoRegionService.getRegionZone(anyInt(), any(ZoneId.class)))
                .thenReturn(ZoneId.of("Europe/Moscow"));

        communicationProxyService = new CommunicationProxyService(communicationProxyClient, geoRegionService);
        userUnreachableValidationRule = new UserUnreachableValidationRule(checkouterFeatureReader,
                communicationProxyService, shopService);
        lenient().when(shopService.getMeta(anyLong())).thenReturn(shopMetaData);
        lenient().when(shopMetaData.getOrderVisibility(eq(OrderVisibility.BUYER_PHONE)))
                .thenReturn(Optional.of(Boolean.FALSE));
    }

    @Test
    public void shouldNotThrowExceptionWhenStatusNotUserUnreachable() {
        var order = createOrder();
        try {
            userUnreachableValidationRule.validate(order, new ClientInfo(ClientRole.SHOP, 1L), OrderStatus.CANCELLED,
                    OrderSubstatus.USER_CHANGED_MIND);
        } catch (Exception e) {
            Assertions.fail("Rule should be validated!", e);
        }
    }

    @Test
    public void shouldNotThrowExceptionWhenInvalidNumberCallExists() {
        when(communicationProxyClient.calls(any(), any(), any(), any()))
                .thenReturn(new CallsResponseDto(1, 10, 10, 1, List.of(
                        new CallInfo(CallResolution.INVALID_NUMBER, ZonedDateTime.now(), ZonedDateTime.now(), null)
                )));
        var order = createOrder();
        try {
            userUnreachableValidationRule.validate(order, new ClientInfo(ClientRole.SHOP, 1L), OrderStatus.CANCELLED,
                    OrderSubstatus.USER_UNREACHABLE);
        } catch (Exception e) {
            Assertions.fail("Rule should be validated!", e);
        }
    }

    @Test
    public void shouldThrowExceptionWhenCallsAbsent() {
        when(communicationProxyClient.calls(any(), any(), any(), any()))
                .thenReturn(new CallsResponseDto(1, 10, 10, 1, Collections.emptyList()));
        var order = createOrder();
        try {
            userUnreachableValidationRule.validate(order, new ClientInfo(ClientRole.SHOP, 1L), OrderStatus.CANCELLED,
                    OrderSubstatus.USER_UNREACHABLE);
            Assertions.fail("Rule should not be validated!");
        } catch (Exception e) {
        }
    }

    @Test
    public void shouldThrowExceptionWhenLessThenThreeCalls() {
        when(communicationProxyClient.calls(any(), any(), any(), any()))
                .thenReturn(new CallsResponseDto(1, 10, 10, 1, List.of(
                        new CallInfo(CallResolution.NO_ANSWER, ZonedDateTime.now(), ZonedDateTime.now(), null)
                )));
        var order = createOrder();
        try {
            userUnreachableValidationRule.validate(order, new ClientInfo(ClientRole.SHOP, 1L), OrderStatus.CANCELLED,
                    OrderSubstatus.USER_UNREACHABLE);
            Assertions.fail("Rule should not be validated!");
        } catch (Exception e) {
        }
    }

    @Test
    public void shouldThrowExceptionWhenLessThenThreeCallsWasAtWorkingTime() {
        when(communicationProxyClient.calls(any(), any(), any(), any()))
                .thenReturn(new CallsResponseDto(1, 10, 10, 1, List.of(
                        new CallInfo(CallResolution.NO_ANSWER, ZonedDateTime.now(), ZonedDateTime.now(), null)
                )));
        var order = createOrder();
        try {
            userUnreachableValidationRule.validate(order, new ClientInfo(ClientRole.SHOP, 1L), OrderStatus.CANCELLED,
                    OrderSubstatus.USER_UNREACHABLE);
            Assertions.fail("Rule should not be validated!");
        } catch (Exception e) {
        }
    }

    @Test
    public void shouldNotThrowExceptionWhenConnectedCallExists() {
        when(communicationProxyClient.calls(any(), any(), any(), any()))
                .thenReturn(new CallsResponseDto(1, 10, 10, 1, List.of(
                        new CallInfo(CallResolution.CONNECTED,
                                ZonedDateTime.now(),
                                ZonedDateTime.now().plusSeconds(10),
                                null)))
                );
        var order = createOrder();
        try {
            userUnreachableValidationRule.validate(order, new ClientInfo(ClientRole.SHOP, 1L), OrderStatus.CANCELLED,
                    OrderSubstatus.USER_UNREACHABLE);
        } catch (Exception e) {
            Assertions.fail("Rule should be validated!");
        }
    }

    @Test
    public void shouldThrowExceptionWhenThreeCallInShortPeriodExists() {
        when(communicationProxyClient.calls(any(), any(), any(), any()))
                .thenReturn(new CallsResponseDto(1, 10, 10, 1, List.of(
                        new CallInfo(CallResolution.NO_ANSWER,
                                ZonedDateTime.parse("2022-03-01T10:15:30+01:00[Europe/Moscow]"),
                                ZonedDateTime.parse("2022-03-01T10:15:31+01:00[Europe/Moscow]"),
                                null),
                        new CallInfo(CallResolution.UNAVAILABLE,
                                ZonedDateTime.parse("2022-03-01T10:35:30+01:00[Europe/Moscow]"),
                                ZonedDateTime.parse("2022-03-01T10:35:31+01:00[Europe/Moscow]"),
                                null),
                        new CallInfo(CallResolution.REJECTED,
                                ZonedDateTime.parse("2022-03-01T10:55:30+01:00[Europe/Moscow]"),
                                ZonedDateTime.parse("2022-03-01T10:55:31+01:00[Europe/Moscow]"),
                                null)
                )));
        var order = createOrder();
        try {
            userUnreachableValidationRule.validate(order, new ClientInfo(ClientRole.SHOP, 1L), OrderStatus.CANCELLED,
                    OrderSubstatus.USER_UNREACHABLE);
            Assertions.fail("Rule should not be validated!");
        } catch (Exception e) {
        }
    }

    @Test
    public void shouldNotThrowExceptionWhenThreeCallInLongPeriodExists() {
        when(communicationProxyClient.calls(any(), any(), any(), any()))
                .thenReturn(new CallsResponseDto(1, 10, 10, 1, List.of(
                        new CallInfo(CallResolution.NO_ANSWER,
                                ZonedDateTime.parse("2022-03-01T10:15:30+01:00[Europe/Moscow]"),
                                ZonedDateTime.parse("2022-03-01T10:15:31+01:00[Europe/Moscow]"),
                                null),
                        new CallInfo(CallResolution.UNAVAILABLE,
                                ZonedDateTime.parse("2022-03-01T10:35:30+01:00[Europe/Moscow]"),
                                ZonedDateTime.parse("2022-03-01T10:35:31+01:00[Europe/Moscow]"),
                                null),
                        new CallInfo(CallResolution.REJECTED,
                                ZonedDateTime.parse("2022-03-01T12:55:30+01:00[Europe/Moscow]"),
                                ZonedDateTime.parse("2022-03-01T12:55:31+01:00[Europe/Moscow]"),
                                null)
                )));
        var order = createOrder();
        try {
            userUnreachableValidationRule.validate(order, new ClientInfo(ClientRole.SHOP, 1L), OrderStatus.CANCELLED,
                    OrderSubstatus.USER_UNREACHABLE);
        } catch (Exception e) {
            Assertions.fail("Rule should be validated!");
        }
    }

    @Test
    public void shouldThrowExceptionWhenTwoCancelledCallsExists() {
        when(communicationProxyClient.calls(any(), any(), any(), any()))
                .thenReturn(new CallsResponseDto(1, 10, 10, 1, List.of(
                        new CallInfo(CallResolution.CANCELLED, // больше 5 сек дозвона (ended - dialStarted)
                                ZonedDateTime.parse("2022-03-01T10:15:20+01:00[Europe/Moscow]"),
                                ZonedDateTime.parse("2022-03-01T10:25:31+01:00[Europe/Moscow]"),
                                ZonedDateTime.parse("2022-03-01T10:25:25+01:00[Europe/Moscow]")),
                        new CallInfo(CallResolution.CANCELLED, // больше 5 сек дозвона (ended - dialStarted)
                                ZonedDateTime.parse("2022-03-01T10:35:20+01:00[Europe/Moscow]"),
                                ZonedDateTime.parse("2022-03-01T10:35:31+01:00[Europe/Moscow]"),
                                ZonedDateTime.parse("2022-03-01T10:35:25+01:00[Europe/Moscow]")),
                        new CallInfo(CallResolution.CANCELLED, // меньше 5 сек дозвона (ended - dialStarted)
                                ZonedDateTime.parse("2022-03-01T12:55:30+01:00[Europe/Moscow]"),
                                ZonedDateTime.parse("2022-03-01T12:55:31+01:00[Europe/Moscow]"),
                                ZonedDateTime.parse("2022-03-01T12:55:29+01:00[Europe/Moscow]"))
                )));
        var order = createOrder();
        try {
            userUnreachableValidationRule.validate(order, new ClientInfo(ClientRole.SHOP, 1L), OrderStatus.CANCELLED,
                    OrderSubstatus.USER_UNREACHABLE);
            Assertions.fail("Rule should not be validated!");
        } catch (Exception e) {
        }
    }

    @Test
    public void shouldNotThrowExceptionWhenThreeCancelledCallsExists() {
        when(communicationProxyClient.calls(any(), any(), any(), any()))
                .thenReturn(new CallsResponseDto(1, 10, 10, 1, List.of(
                        new CallInfo(CallResolution.CANCELLED, // больше 5 сек дозвона (ended - dialStarted)
                                ZonedDateTime.parse("2022-03-01T10:25:20+01:00[Europe/Moscow]"),
                                ZonedDateTime.parse("2022-03-01T10:25:31+01:00[Europe/Moscow]"),
                                ZonedDateTime.parse("2022-03-01T10:25:25+01:00[Europe/Moscow]")),
                        new CallInfo(CallResolution.CANCELLED, // больше 5 сек дозвона (ended - dialStarted)
                                ZonedDateTime.parse("2022-03-01T10:35:20+01:00[Europe/Moscow]"),
                                ZonedDateTime.parse("2022-03-01T10:35:31+01:00[Europe/Moscow]"),
                                ZonedDateTime.parse("2022-03-01T10:35:25+01:00[Europe/Moscow]")),
                        new CallInfo(CallResolution.CANCELLED, // больше 5 сек дозвона (ended - dialStarted)
                                ZonedDateTime.parse("2022-03-01T12:55:20+01:00[Europe/Moscow]"),
                                ZonedDateTime.parse("2022-03-01T12:55:31+01:00[Europe/Moscow]"),
                                ZonedDateTime.parse("2022-03-01T12:55:25+01:00[Europe/Moscow]"))
                )));
        var order = createOrder();
        try {
            userUnreachableValidationRule.validate(order, new ClientInfo(ClientRole.SHOP, 1L), OrderStatus.CANCELLED,
                    OrderSubstatus.USER_UNREACHABLE);
        } catch (Exception e) {
            Assertions.fail("Rule should be validated!");
        }
    }

    @Test
    public void shouldNotThrowExceptionWhenBuyerPhoneIsVisible() {
        var order = createOrder();
        when(shopMetaData.getOrderVisibility(eq(OrderVisibility.BUYER_PHONE))).thenReturn(Optional.of(Boolean.TRUE));
        try {
            userUnreachableValidationRule.validate(order, new ClientInfo(ClientRole.SHOP, 1L), OrderStatus.CANCELLED,
                    OrderSubstatus.USER_UNREACHABLE);
        } catch (Exception e) {
            Assertions.fail("Rule should be validated!", e);
        }
    }

    private Order createOrder() {
        var order = OrderProvider.getBluePostPaidOrder();
        order.setId(1L);
        order.setStatus(OrderStatus.PROCESSING);
        order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        order.setFulfilment(false);

        return order;
    }
}
