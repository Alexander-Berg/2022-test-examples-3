package ru.yandex.travel.orders.services.avia.aeroflot;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mockito;

import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotOrderCreateResult;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotTicketCoupon;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotTicketCouponStatusCode;
import ru.yandex.travel.orders.entities.AeroflotLastRequest;
import ru.yandex.travel.orders.entities.AeroflotOrder;
import ru.yandex.travel.orders.mocks.AeroflotMocks;
import ru.yandex.travel.orders.repository.AeroflotLastRequestRepository;
import ru.yandex.travel.orders.workflow.order.aeroflot.proto.EAeroflotOrderState;
import ru.yandex.travel.orders.workflows.orderitem.aeroflot.provider.AeroflotService;
import ru.yandex.travel.orders.workflows.orderitem.aeroflot.provider.AeroflotServiceProvider;
import ru.yandex.travel.testing.time.SettableClock;
import ru.yandex.travel.workflow.WorkflowMessageSender;
import ru.yandex.travel.workflow.entities.Workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class AeroflotOrderStateSyncTest {
    private AeroflotOrderStateSyncLimit aeroflotOrderStateSyncLimit;
    private AeroflotServiceProvider aeroflotServiceProvider;
    private AeroflotService aeroflotService;
    private WorkflowMessageSender workflowMessageSender;
    private AeroflotOrderStateSync aeroflotOrderStateSync;
    private AeroflotLastRequestRepository aeroflotLastRequestRepository;
    private AeroflotOrderStateSyncProperties properties;

    private final static String COUPON_ID = "couponId";
    private final static String TICKET_ID = "ticketId";
    private final static String COUPON_ID2 = "couponId2";
    private final static String TICKET_ID2 = "ticketId2";
    private final static UUID WORKFLOW_ID = UUID.randomUUID();
    private final static UUID ORDER_ID = UUID.randomUUID();
    private final static Instant DATE = Instant.parse("2022-01-21T00:00:00Z");
    private final static Duration REQUEST_DELAY = Duration.ZERO;
    private final static AeroflotOrderCreateResult RESULT = new AeroflotOrderCreateResult();

    @BeforeEach
    public void init() {
        SettableClock clock = new SettableClock();
        clock.setCurrentTime(DATE);
        aeroflotService = Mockito.mock(AeroflotService.class);
        aeroflotServiceProvider = Mockito.mock(AeroflotServiceProvider.class);
        workflowMessageSender = Mockito.mock(WorkflowMessageSender.class);
        aeroflotOrderStateSyncLimit = Mockito.mock(AeroflotOrderStateSyncLimit.class);
        aeroflotLastRequestRepository = Mockito.mock(AeroflotLastRequestRepository.class);
        properties = AeroflotOrderStateSyncProperties.builder()
                .responseExpiration(REQUEST_DELAY)
                .build();
        aeroflotOrderStateSync = new AeroflotOrderStateSync(aeroflotOrderStateSyncLimit,
                aeroflotServiceProvider, workflowMessageSender, properties, clock, aeroflotLastRequestRepository);
    }

    @Test
    public void sync_nullOrder_false() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> aeroflotOrderStateSync.sync(null));
    }

    @ParameterizedTest
    @EnumSource(value = EAeroflotOrderState.class, mode = EnumSource.Mode.INCLUDE, names = {"OS_CANCELLED","OS_EXTERNALLY_CANCELLED"})
    public void sync_cancelledOrder_false(EAeroflotOrderState state) {
        var order = createOrder(state);

        assertThat(aeroflotOrderStateSync.sync(order)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = EAeroflotOrderState.class, mode = EnumSource.Mode.EXCLUDE, names = {"OS_CANCELLED","OS_EXTERNALLY_CANCELLED"})
    public void sync_noCancelledOrderWithFinishLimit_false(EAeroflotOrderState state) {
        when(aeroflotOrderStateSyncLimit.need(1)).thenReturn(false);
        var order = createOrder(state);

        assertThat(aeroflotOrderStateSync.sync(order)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = EAeroflotOrderState.class, mode = EnumSource.Mode.EXCLUDE, names = {"OS_CANCELLED", "OS_EXTERNALLY_CANCELLED"})
    public void sync_noCancelledOrderWithNullCoupons_false(EAeroflotOrderState state) {
        when(aeroflotLastRequestRepository.findById(any())).thenReturn(Optional.empty());
        when(aeroflotLastRequestRepository.getOne(any())).thenReturn(new AeroflotLastRequest());
        when(aeroflotOrderStateSyncLimit.need(1)).thenReturn(true);
        when(aeroflotServiceProvider.getAeroflotServiceForProfile(any())).thenReturn(aeroflotService);
        var result = new AeroflotOrderCreateResult();
        when(aeroflotService.getOrderStatus(any(), any())).thenReturn(result);
        var order = createOrder(state);

        assertThat(aeroflotOrderStateSync.sync(order)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = EAeroflotOrderState.class, mode = EnumSource.Mode.EXCLUDE, names = {"OS_CANCELLED", "OS_EXTERNALLY_CANCELLED"})
    public void sync_noCancelledOrderWithCoupons_true(EAeroflotOrderState state) {
        var result = new AeroflotOrderCreateResult();
        Map<String, List<AeroflotTicketCoupon>> ticketCoupons = new HashMap<>();
        var coupons = new ArrayList<AeroflotTicketCoupon>();
        AeroflotTicketCoupon coupon = new AeroflotTicketCoupon(COUPON_ID, AeroflotTicketCouponStatusCode.OK);
        coupons.add(coupon);
        ticketCoupons.put(TICKET_ID, coupons);
        result.setCouponStatusCodes(ticketCoupons);

        when(aeroflotOrderStateSyncLimit.need(1)).thenReturn(true);
        when(aeroflotServiceProvider.getAeroflotServiceForProfile(any())).thenReturn(aeroflotService);
        when(aeroflotService.getOrderStatus(any(), any())).thenReturn(result);
        doNothing().when(workflowMessageSender).scheduleEvent(any(), any());
        when(aeroflotLastRequestRepository.getOne(any())).thenReturn(new AeroflotLastRequest());
        var order = createOrder(state);

        assertThat(aeroflotOrderStateSync.sync(order)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void canCancel_nullAndEmpty_false(Map<String, List<AeroflotTicketCoupon>> ticketCouponStatus) {
        assertThat(AeroflotOrderStateSync.canCancel(ticketCouponStatus)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(AeroflotTicketCouponStatusCode.class)
    public void canCancel_hasCouponStatusOkInOtherTicket_false(AeroflotTicketCouponStatusCode state) {
        var ticketCouponStatus = getTicketCouponStatus(state);
        ticketCouponStatus.put(TICKET_ID2, getCoupons(AeroflotTicketCouponStatusCode.OK));

        assertThat(AeroflotOrderStateSync.canCancel(ticketCouponStatus)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(AeroflotTicketCouponStatusCode.class)
    public void canCancel_hasCouponStatusOk_false(AeroflotTicketCouponStatusCode state) {
        var ticketCouponStatus = getTicketCouponStatus(state);
        var couponOk = getCoupons(AeroflotTicketCouponStatusCode.OK);
        ticketCouponStatus.get(TICKET_ID).add(couponOk.get(0));

        assertThat(AeroflotOrderStateSync.canCancel(ticketCouponStatus)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = AeroflotTicketCouponStatusCode.class, mode = EnumSource.Mode.EXCLUDE, names = {"VOIDED", "REFUNDED"})
    public void canCancel_allCouponStatusWithoutVoidedOrRefunded_false(AeroflotTicketCouponStatusCode state) {
        var ticketCouponStatus = getTicketCouponStatus(state);

        assertThat(AeroflotOrderStateSync.canCancel(ticketCouponStatus)).isFalse();
    }

    @ParameterizedTest
    @ArgumentsSource(StatusProvider.class)
    public void canCancel_allCouponStatusVoidedOrRefunded_true(AeroflotTicketCouponStatusCode state, AeroflotTicketCouponStatusCode state2) {
        var ticketCouponStatus = getTicketCouponStatus(state);
        ticketCouponStatus.put(TICKET_ID2, getCoupons(state2));

        assertThat(AeroflotOrderStateSync.canCancel(ticketCouponStatus)).isTrue();
    }

    static class StatusProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(AeroflotTicketCouponStatusCode.VOIDED, AeroflotTicketCouponStatusCode.VOIDED),
                    Arguments.of(AeroflotTicketCouponStatusCode.REFUNDED, AeroflotTicketCouponStatusCode.REFUNDED),
                    Arguments.of(AeroflotTicketCouponStatusCode.VOIDED, AeroflotTicketCouponStatusCode.REFUNDED)
            );
        }
    }

    @ParameterizedTest
    @EnumSource(AeroflotTicketCouponStatusCode.class)
    public void couponEquals_equals_true(AeroflotTicketCouponStatusCode state) {
        assertThat(AeroflotOrderStateSync.couponEquals(getTicketCouponStatus(state), getTicketCouponStatus(state))).isTrue();
        assertThat(AeroflotOrderStateSync.couponEquals(null, null)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(AeroflotTicketCouponStatusCode.class)
    public void couponEquals_oneNull_false(AeroflotTicketCouponStatusCode state) {
        assertThat(AeroflotOrderStateSync.couponEquals(getTicketCouponStatus(state), null)).isFalse();
        assertThat(AeroflotOrderStateSync.couponEquals(null, getTicketCouponStatus(state))).isFalse();
    }

    @ParameterizedTest
    @EnumSource(AeroflotTicketCouponStatusCode.class)
    public void couponEquals_oneEmpty_false(AeroflotTicketCouponStatusCode state) {
        assertThat(AeroflotOrderStateSync.couponEquals(getTicketCouponStatus(state), new HashMap<>())).isFalse();
        assertThat(AeroflotOrderStateSync.couponEquals(new HashMap<>(), getTicketCouponStatus(state))).isFalse();
    }

    @ParameterizedTest
    @EnumSource(AeroflotTicketCouponStatusCode.class)
    public void couponEquals_differentCountTicket_false(AeroflotTicketCouponStatusCode state) {
        var c2 = getTicketCouponStatus(state);
        c2.put(TICKET_ID2, getCoupons(state));

        assertThat(AeroflotOrderStateSync.couponEquals(getTicketCouponStatus(state), c2)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(AeroflotTicketCouponStatusCode.class)
    public void couponEquals_differentCountCoupon_false(AeroflotTicketCouponStatusCode state) {
        var c2 = getTicketCouponStatus(state);
        c2.get(TICKET_ID).add(getCoupons(state).get(0));

        assertThat(AeroflotOrderStateSync.couponEquals(getTicketCouponStatus(state), c2)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(AeroflotTicketCouponStatusCode.class)
    public void couponEquals_differentCouponId_false(AeroflotTicketCouponStatusCode state) {
        var c2 = getTicketCouponStatus(state);
        c2.get(TICKET_ID).get(0).setCouponId(COUPON_ID2);

        assertThat(AeroflotOrderStateSync.couponEquals(getTicketCouponStatus(state), c2)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("statusCodeProvider")
    public void couponEquals_differentCouponStatusCode_false(AeroflotTicketCouponStatusCode state,
                                                             AeroflotTicketCouponStatusCode state2) {
        var c2 = getTicketCouponStatus(state);
        c2.get(TICKET_ID).get(0).setStatusCode(state2);

        assertThat(AeroflotOrderStateSync.couponEquals(getTicketCouponStatus(state), c2)).isFalse();
    }

    @Test
    public void isExpireLastRequest_true() {
        properties.setResponseExpiration(Duration.ofSeconds(1));

        assertThat(aeroflotOrderStateSync.isLastRequestExpired(DATE.minusSeconds(5))).isTrue();
    }

    @Test
    public void isExpireLastRequest_false() {
        properties.setResponseExpiration(Duration.ofSeconds(1));

        assertThat(aeroflotOrderStateSync.isLastRequestExpired(DATE.minusSeconds(1))).isFalse();
    }

    @Test
    public void updateEntity_hasInDb_updateInCache() {
        when(aeroflotLastRequestRepository.getOne(any())).thenReturn(new AeroflotLastRequest());
        assertThat(aeroflotOrderStateSync.lastUpdate.size()).isEqualTo(0);

        aeroflotOrderStateSync.createOrUpdateEntityAndUpdateLocalCache(ORDER_ID, DATE, RESULT);

        assertThat(aeroflotOrderStateSync.lastUpdate.size()).isEqualTo(1);
        var item = aeroflotOrderStateSync.lastUpdate.get(ORDER_ID);
        assertThat(item.getLeft()).isEqualTo(DATE);
        assertThat(item.getRight()).isEqualTo(RESULT);
    }

    @Test
    public void updateEntity_hasNotInDb_addInCacheAndThrow() {
        when(aeroflotLastRequestRepository.getOne(any())).thenReturn(null);
        assertThat(aeroflotOrderStateSync.lastUpdate.size()).isEqualTo(0);

        assertThatThrownBy(() -> aeroflotOrderStateSync.createOrUpdateEntityAndUpdateLocalCache(ORDER_ID, DATE, RESULT));

        assertThat(aeroflotOrderStateSync.lastUpdate.size()).isEqualTo(1);
        var item = aeroflotOrderStateSync.lastUpdate.get(ORDER_ID);
        assertThat(item.getLeft()).isEqualTo(DATE);
        assertThat(item.getRight()).isEqualTo(RESULT);
    }

    @Test
    public void createEntity_hasInDb_updateCacheFromDb() {
        var lastResult = createLastRequest();
        when(aeroflotLastRequestRepository.findById(any())).thenReturn(Optional.of(lastResult));
        assertThat(aeroflotOrderStateSync.lastUpdate.size()).isEqualTo(0);

        aeroflotOrderStateSync.getOrCreateEntityAndUpdateLocalCache(ORDER_ID);

        assertThat(aeroflotOrderStateSync.lastUpdate.size()).isEqualTo(1);
        var item = aeroflotOrderStateSync.lastUpdate.get(ORDER_ID);
        assertThat(item.getLeft()).isEqualTo(DATE);
        assertThat(item.getRight()).isEqualTo(RESULT);
    }

    @Test
    public void createEntity_hasNotInDb_updateInCache() {
        when(aeroflotLastRequestRepository.findById(any())).thenReturn(Optional.empty());
        assertThat(aeroflotOrderStateSync.lastUpdate.size()).isEqualTo(0);

        aeroflotOrderStateSync.getOrCreateEntityAndUpdateLocalCache(ORDER_ID);

        assertThat(aeroflotOrderStateSync.lastUpdate.size()).isEqualTo(1);
        var item = aeroflotOrderStateSync.lastUpdate.get(ORDER_ID);
        assertThat(item.getLeft()).isEqualTo(Instant.ofEpochSecond(0));
        assertThat(item.getRight()).isEqualTo(null);
    }

    @ParameterizedTest
    @EnumSource(EAeroflotOrderState.class)
    public void getAeroflotOrderStateFromCache_noExpireInCache_returnFromCache(EAeroflotOrderState state) {
        properties.setResponseExpiration(Duration.ofSeconds(1));
        var order = createOrder(state);
        var res = createCreateResult();
        aeroflotOrderStateSync.lastUpdate.put(order.getId(), Pair.of(DATE, res));


        assertThat(aeroflotOrderStateSync.getAeroflotOrderStateFromCache(order)).isEqualTo(res);
    }

    @ParameterizedTest
    @EnumSource(EAeroflotOrderState.class)
    public void getAeroflotOrderStateFromCache_expireInCache_returnFromDb(EAeroflotOrderState state) {
        properties.setResponseExpiration(Duration.ofSeconds(1));
        var order = createOrder(state);
        var result = createCreateResult();
        var lastResult = createLastRequest();
        lastResult.setLastRequestAt(DATE);
        aeroflotOrderStateSync.lastUpdate.put(order.getId(), Pair.of(DATE.minusSeconds(5), result));
        when(aeroflotLastRequestRepository.findById(any())).thenReturn(Optional.of(lastResult));

        assertThat(aeroflotOrderStateSync.getAeroflotOrderStateFromCache(order)).isEqualTo(lastResult.getResult());
    }

    @ParameterizedTest
    @EnumSource(EAeroflotOrderState.class)
    public void getAeroflotOrderStateFromCache_expireInCacheAndEmptyInDb_null(EAeroflotOrderState state) {
        properties.setResponseExpiration(Duration.ofSeconds(1));
        var order = createOrder(state);
        var result = createCreateResult();
        var lastResult = createLastRequest();
        lastResult.setLastRequestAt(DATE);
        aeroflotOrderStateSync.lastUpdate.put(order.getId(), Pair.of(DATE.minusSeconds(5), result));
        when(aeroflotLastRequestRepository.findById(any())).thenReturn(Optional.empty());

        assertThat(aeroflotOrderStateSync.getAeroflotOrderStateFromCache(order)).isNull();
    }

    @ParameterizedTest
    @EnumSource(EAeroflotOrderState.class)
    public void getAeroflotOrderStateFromCache_expireInCacheAndDb_null(EAeroflotOrderState state) {
        properties.setResponseExpiration(Duration.ofSeconds(1));
        var order = createOrder(state);
        var result = createCreateResult();
        var lastResult = createLastRequest();
        lastResult.setLastRequestAt(DATE.minusSeconds(5));
        aeroflotOrderStateSync.lastUpdate.put(order.getId(), Pair.of(DATE.minusSeconds(5), result));
        when(aeroflotLastRequestRepository.findById(any())).thenReturn(Optional.of(lastResult));

        assertThat(aeroflotOrderStateSync.getAeroflotOrderStateFromCache(order)).isNull();
    }

    @NotNull
    private Map<String, List<AeroflotTicketCoupon>> getTicketCouponStatus(AeroflotTicketCouponStatusCode state) {
        List<AeroflotTicketCoupon> couponmap = getCoupons(state);
        Map<String, List<AeroflotTicketCoupon>> map = new HashMap<>();
        map.put(TICKET_ID, couponmap);
        return map;
    }

    @NotNull
    private List<AeroflotTicketCoupon> getCoupons(AeroflotTicketCouponStatusCode state) {
        List<AeroflotTicketCoupon> couponmap = new ArrayList<>();
        couponmap.add(new AeroflotTicketCoupon(COUPON_ID, state));
        return couponmap;
    }

    static Iterable<Arguments> statusCodeProvider() {
        var list = new ArrayList<Arguments>();
        for (var state : AeroflotTicketCouponStatusCode.values()) {
            for (var state2 : AeroflotTicketCouponStatusCode.values()) {
                if (state != state2) {
                    list.add(arguments(state, state2));
                }
            }
        }
        return list;
    }

    private AeroflotOrder createOrder(EAeroflotOrderState state) {
        var order = AeroflotMocks.testOrder();
        order.setState(state);
        order.getAeroflotOrderItem().setWorkflow(new Workflow());
        order.getAeroflotOrderItem().setOrderWorkflowId(WORKFLOW_ID);
        return order;
    }

    private AeroflotOrderCreateResult createCreateResult() {
        return new AeroflotOrderCreateResult();
    }

    private AeroflotLastRequest createLastRequest() {
        var lastResult = new AeroflotLastRequest();
        lastResult.setResult(createCreateResult());
        lastResult.setResult(RESULT);
        lastResult.setOrderId(ORDER_ID);
        lastResult.setLastRequestAt(DATE);
        return lastResult;
    }
}
