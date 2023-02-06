package ru.yandex.market.pvz.internal.controller.pi.order.mapper;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.pvz.client.model.order.DeliveryServiceType;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType;
import ru.yandex.market.pvz.core.domain.order.model.OrderType;
import ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus;
import ru.yandex.market.pvz.core.domain.order_delivery_result.PartialDeliveryStatus;
import ru.yandex.market.pvz.core.domain.sms.SmsLogParams;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionDto;
import ru.yandex.market.pvz.internal.controller.pi.order.mapper.order_action.OrderActionsGetter;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.domain.order.model.Order.DEFAULT_EXTEND_STORAGE_PERIOD_MAX_DAYS;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.ARRIVED_TO_PICKUP_POINT;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.CREATED;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.DELIVERED_TO_RECIPIENT;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.STORAGE_PERIOD_EXPIRED;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.STORAGE_PERIOD_EXTENDED;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.TRANSMITTED_TO_RECIPIENT;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.TRANSPORTATION_RECIPIENT;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_VERIFICATION_CODE;
import static ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionType.CANCEL_DELIVERY;
import static ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionType.CASHBOX_PAYMENT;
import static ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionType.CONTINUE_PARTIAL_DELIVERY;
import static ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionType.DELIVER;
import static ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionType.DO_NOT_USE_VERIFICATION_CODE;
import static ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionType.EXTEND_STORAGE_PERIOD;
import static ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionType.PARTIAL_DELIVERY;
import static ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionType.PRINT_FORM;
import static ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionType.SEND_CODE_VIA_SMS;
import static ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionType.SHOW_NOT_ACCEPTED_BY_COURIER;
import static ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionType.SHOW_SEND_SMS_BUTTON;
import static ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionType.SIMPLIFIED_DELIVERY;

class OrderActionsGetterTest {

    private final Clock clock = Clock.systemDefaultZone();
    private final Instant twoHoursAgo = Instant.now(clock).minus(2, ChronoUnit.HOURS);

    private final OrderActionsGetter defaultValuesActionsGetter = OrderActionsGetter.builder()
            .type(OrderType.CLIENT)
            .paymentType(OrderPaymentType.PREPAID)
            .status(PvzOrderStatus.ARRIVED_TO_PICKUP_POINT)
            .deliveredAt(null)
            .verificationAccepted(false)
            .maxOrderStorageDate(LocalDate.ofInstant(Instant.now(clock)
                    .plus(DEFAULT_EXTEND_STORAGE_PERIOD_MAX_DAYS, ChronoUnit.DAYS), ZoneId.systemDefault()))
            .verificationCodeInfo(new VerificationCodeInfo(DEFAULT_VERIFICATION_CODE, 3, true, true))
            .storagePeriodExtended(false)
            .smsLogs(List.of())
            .fashion(false)
            .partialDeliveryStatus(PartialDeliveryStatus.NOT_STARTED)
            .acceptedByCourier(true)
            .deliveryServiceType(DeliveryServiceType.MARKET_COURIER)
            .cashboxPaymentAllowed(false)
            .clock(clock)
            .build();

    @Test
    void cancelDeliveryActionForTransmittedClientOrder() {
        var actions = defaultValuesActionsGetter.toBuilder()
                .status(TRANSMITTED_TO_RECIPIENT)
                .deliveredAt(twoHoursAgo)
                .build()
                .toOrderActions();

        assertThat(actions).contains(new OrderActionDto(CANCEL_DELIVERY));
    }

    @Test
    void cancelDeliveryActionForTransmittedOnDemandOrder() {
        var actions = defaultValuesActionsGetter.toBuilder()
                .type(OrderType.ON_DEMAND)
                .status(TRANSPORTATION_RECIPIENT)
                .deliveredAt(twoHoursAgo)
                .build()
                .toOrderActions();

        assertThat(actions).contains(new OrderActionDto(CANCEL_DELIVERY));
    }

    @Test
    void noCancelDeliveryActionForNotYetTransmittedOrder() {
        var actions = defaultValuesActionsGetter.toBuilder()
                .status(ARRIVED_TO_PICKUP_POINT)
                .build()
                .toOrderActions();

        assertThat(actions).doesNotContain(new OrderActionDto(CANCEL_DELIVERY));
    }

    @Test
    void noCancelDeliveryActionForDeliveredOrder() {
        var actions = defaultValuesActionsGetter.toBuilder()
                .status(DELIVERED_TO_RECIPIENT)
                .deliveredAt(twoHoursAgo)
                .build()
                .toOrderActions();

        assertThat(actions).doesNotContain(new OrderActionDto(CANCEL_DELIVERY));
    }

    @Test
    void deliveryActionForArrivedClientOrder() {
        var actions = defaultValuesActionsGetter.toBuilder()
                .status(ARRIVED_TO_PICKUP_POINT)
                .build()
                .toOrderActions();

        assertThat(actions).contains(new OrderActionDto(DELIVER));
    }

    @Test
    void deliveryActionForExtendedOnDemandOrder() {
        var actions = defaultValuesActionsGetter.toBuilder()
                .type(OrderType.ON_DEMAND)
                .status(STORAGE_PERIOD_EXTENDED)
                .build()
                .toOrderActions();

        assertThat(actions).contains(new OrderActionDto(DELIVER));
    }

    @Test
    void deliveryActionForExpiredClientOrder() {
        var actions = defaultValuesActionsGetter.toBuilder()
                .status(STORAGE_PERIOD_EXPIRED)
                .build()
                .toOrderActions();

        assertThat(actions).contains(new OrderActionDto(DELIVER));
    }

    @Test
    void noDeliveryActionForTransmittedClientOrder() {
        var actions = defaultValuesActionsGetter.toBuilder()
                .status(TRANSMITTED_TO_RECIPIENT)
                .deliveredAt(twoHoursAgo)
                .build()
                .toOrderActions();

        assertThat(actions).doesNotContain(new OrderActionDto(DELIVER));
    }

    @Test
    void noDeliveryActionForDeliveredClientOrder() {
        var actions = defaultValuesActionsGetter.toBuilder()
                .status(DELIVERED_TO_RECIPIENT)
                .deliveredAt(twoHoursAgo)
                .build()
                .toOrderActions();

        assertThat(actions).doesNotContain(new OrderActionDto(DELIVER));
    }

    @Test
    void noDeliveryActionForTransportationOnDemandOrder() {
        var actions = defaultValuesActionsGetter.toBuilder()
                .type(OrderType.ON_DEMAND)
                .status(TRANSPORTATION_RECIPIENT)
                .deliveredAt(twoHoursAgo)
                .build()
                .toOrderActions();

        assertThat(actions).doesNotContain(new OrderActionDto(DELIVER));
    }

    @ParameterizedTest
    @EnumSource(value = PvzOrderStatus.class, names = {"TRANSMITTED_TO_RECIPIENT", "DELIVERED_TO_RECIPIENT"})
    void printFormActionForClientOrder(PvzOrderStatus orderStatus) {
        var actions = defaultValuesActionsGetter.toBuilder()
                .status(orderStatus)
                .deliveredAt(twoHoursAgo)
                .build()
                .toOrderActions();

        assertThat(actions).contains(new OrderActionDto(PRINT_FORM));
    }

    @Test
    void noPrintFormActionForTransportationOnDemandOrder() {
        var actions = defaultValuesActionsGetter.toBuilder()
                .type(OrderType.ON_DEMAND)
                .status(TRANSPORTATION_RECIPIENT)
                .deliveredAt(twoHoursAgo)
                .build()
                .toOrderActions();

        assertThat(actions).doesNotContain(new OrderActionDto(PRINT_FORM));
    }

    @Test
    void noPrintFormActionForVerificationAccepted() {
        var actions = defaultValuesActionsGetter.toBuilder()
                .status(DELIVERED_TO_RECIPIENT)
                .deliveredAt(twoHoursAgo)
                .verificationAccepted(true)
                .build()
                .toOrderActions();

        assertThat(actions).doesNotContain(new OrderActionDto(PRINT_FORM));
    }

    @ParameterizedTest
    @EnumSource(value = OrderPaymentType.class, names = {"CARD", "CASH"})
    void noPrintFormActionForPostPaidOrder(OrderPaymentType paymentType) {
        var actions = defaultValuesActionsGetter.toBuilder()
                .status(DELIVERED_TO_RECIPIENT)
                .paymentType(paymentType)
                .deliveredAt(twoHoursAgo)
                .build()
                .toOrderActions();

        assertThat(actions).doesNotContain(new OrderActionDto(PRINT_FORM));
    }

    @ParameterizedTest
    @EnumSource(value = PvzOrderStatus.class, names = {"ARRIVED_TO_PICKUP_POINT", "STORAGE_PERIOD_EXPIRED"})
    void extendStoragePeriodActionForClientOrder(PvzOrderStatus orderStatus) {
        var actions = defaultValuesActionsGetter.toBuilder()
                .status(orderStatus)
                .build()
                .toOrderActions();

        assertThat(actions).contains(new OrderActionDto(EXTEND_STORAGE_PERIOD));
    }

    @Test
    void noExtendStoragePeriodActionForTransmittedClientOrder() {
        var actions = defaultValuesActionsGetter.toBuilder()
                .status(TRANSMITTED_TO_RECIPIENT)
                .deliveredAt(twoHoursAgo)
                .build()
                .toOrderActions();

        assertThat(actions).doesNotContain(new OrderActionDto(EXTEND_STORAGE_PERIOD));
    }

    @Test
    void noExtendStoragePeriodActionForClientOrderWhenMaxStorageDateExpired() {
        var actions = defaultValuesActionsGetter.toBuilder()
                .status(ARRIVED_TO_PICKUP_POINT)
                .maxOrderStorageDate(LocalDate.ofInstant(twoHoursAgo, ZoneId.systemDefault()))
                .build()
                .toOrderActions();

        assertThat(actions).doesNotContain(new OrderActionDto(EXTEND_STORAGE_PERIOD));
    }

    @Test
    void noExtendStoragePeriodActionForAlreadyExtendedStoragePeriodClientOrder() {
        var actions = defaultValuesActionsGetter.toBuilder()
                .status(STORAGE_PERIOD_EXPIRED)
                .storagePeriodExtended(true)
                .build()
                .toOrderActions();

        assertThat(actions).doesNotContain(new OrderActionDto(EXTEND_STORAGE_PERIOD));
    }

    @Test
    void sendCodeViaSmsAction() {
        var actions = defaultValuesActionsGetter.toBuilder()
                .status(ARRIVED_TO_PICKUP_POINT)
                .verificationCodeInfo(new VerificationCodeInfo(DEFAULT_VERIFICATION_CODE, 3, true, true))
                .smsLogs(List.of(new SmsLogParams(), new SmsLogParams()))
                .build()
                .toOrderActions();

        assertThat(actions).contains(new OrderActionDto(SEND_CODE_VIA_SMS));
    }

    @Test
    void noSendCodeViaSmsActionForExceededSmsLimit() {
        var actions = defaultValuesActionsGetter.toBuilder()
                .status(ARRIVED_TO_PICKUP_POINT)
                .verificationCodeInfo(new VerificationCodeInfo(DEFAULT_VERIFICATION_CODE, 3, true, true))
                .smsLogs(List.of(new SmsLogParams(), new SmsLogParams(), new SmsLogParams()))
                .build()
                .toOrderActions();

        assertThat(actions).doesNotContain(new OrderActionDto(SEND_CODE_VIA_SMS));
    }

    @Test
    void noShowSendSmsButtonActionAndSendCodeViaSmsActionForOnDemandOrder() {
        var actions = defaultValuesActionsGetter.toBuilder()
                .type(OrderType.ON_DEMAND)
                .status(ARRIVED_TO_PICKUP_POINT)
                .build()
                .toOrderActions();

        assertThat(actions)
                .doesNotContain(new OrderActionDto(SHOW_SEND_SMS_BUTTON), new OrderActionDto(SEND_CODE_VIA_SMS));
    }

    @ParameterizedTest
    @EnumSource(value = OrderPaymentType.class, names = {"CARD", "CASH"})
    void noShowSendSmsButtonActionAndSendCodeViaSmsActionForPostPaidOrder(OrderPaymentType paymentType) {
        var actions = defaultValuesActionsGetter.toBuilder()
                .paymentType(paymentType)
                .status(ARRIVED_TO_PICKUP_POINT)
                .build()
                .toOrderActions();

        assertThat(actions)
                .doesNotContain(new OrderActionDto(SHOW_SEND_SMS_BUTTON), new OrderActionDto(SEND_CODE_VIA_SMS));
    }

    @ParameterizedTest
    @EnumSource(value = PvzOrderStatus.class,
            names = {"TRANSMITTED_TO_RECIPIENT", "TRANSPORTATION_RECIPIENT", "DELIVERED_TO_RECIPIENT"})
    void noShowSendSmsButtonActionAndSendCodeViaSmsActionForDeliveredOrder(PvzOrderStatus orderStatus) {
        var actions = defaultValuesActionsGetter.toBuilder()
                .status(orderStatus)
                .build()
                .toOrderActions();

        assertThat(actions)
                .doesNotContain(new OrderActionDto(SHOW_SEND_SMS_BUTTON), new OrderActionDto(SEND_CODE_VIA_SMS));
    }

    @Test
    void noDotNotUseVerificationCodeActionForClientOrder() {
        var actions = defaultValuesActionsGetter.toBuilder()
                .type(OrderType.CLIENT)
                .verificationCodeInfo(new VerificationCodeInfo(DEFAULT_VERIFICATION_CODE, 3, false, true))
                .build()
                .toOrderActions();

        assertThat(actions).contains(new OrderActionDto(DO_NOT_USE_VERIFICATION_CODE));
    }

    @Test
    void noDotNotUseVerificationCodeActionForOnDemandOrder() {
        var actions = defaultValuesActionsGetter.toBuilder()
                .type(OrderType.ON_DEMAND)
                .verificationCodeInfo(new VerificationCodeInfo(DEFAULT_VERIFICATION_CODE, 3, true, false))
                .build()
                .toOrderActions();

        assertThat(actions).contains(new OrderActionDto(DO_NOT_USE_VERIFICATION_CODE));
    }

    @Test
    void partialDeliveryActionsForFashionOrder() {
        var actions = defaultValuesActionsGetter.toBuilder()
                .fashion(true)
                .build()
                .toOrderActions();

        assertThat(actions).contains(new OrderActionDto(PARTIAL_DELIVERY));
        assertThat(actions).doesNotContain(new OrderActionDto(CONTINUE_PARTIAL_DELIVERY));
    }

    @ParameterizedTest
    @EnumSource(value = PartialDeliveryStatus.class, names = {"CREATED", "PAYED"})
    void continueDeliveryActionForStartedFashionOrder(PartialDeliveryStatus partialDeliveryStatus) {
        var actions = defaultValuesActionsGetter.toBuilder()
                .fashion(true)
                .partialDeliveryStatus(partialDeliveryStatus)
                .build()
                .toOrderActions();

        assertThat(actions).doesNotContain(new OrderActionDto(PARTIAL_DELIVERY));
        assertThat(actions).contains(new OrderActionDto(CONTINUE_PARTIAL_DELIVERY));
    }

    @Test
    void noDeliverActionForPackagedFashionOrder() {
        var actions = defaultValuesActionsGetter.toBuilder()
                .fashion(true)
                .partialDeliveryStatus(PartialDeliveryStatus.PACKAGED)
                .build()
                .toOrderActions();

        assertThat(actions).doesNotContain(new OrderActionDto(PARTIAL_DELIVERY));
        assertThat(actions).doesNotContain(new OrderActionDto(CONTINUE_PARTIAL_DELIVERY));
    }

    @Test
    void whenReceiveNotAcceptedByCourierOrder() {
        var actions = defaultValuesActionsGetter.toBuilder()
                .status(CREATED)
                .acceptedByCourier(false)
                .build()
                .toOrderActions();

        assertThat(actions).contains(new OrderActionDto(SHOW_NOT_ACCEPTED_BY_COURIER));
    }

    @Test
    void whenReceiveAcceptedByCourierOrder() {
        var actions = defaultValuesActionsGetter.toBuilder()
                .status(CREATED)
                .acceptedByCourier(true)
                .build()
                .toOrderActions();

        assertThat(actions).doesNotContain(new OrderActionDto(SHOW_NOT_ACCEPTED_BY_COURIER));
    }

    @Test
    void whenDbsReceiveNotAcceptedByCourierOrder() {
        var actions = defaultValuesActionsGetter.toBuilder()
                .status(CREATED)
                .acceptedByCourier(false)
                .deliveryServiceType(DeliveryServiceType.DBS)
                .build()
                .toOrderActions();

        assertThat(actions).doesNotContain(new OrderActionDto(SHOW_NOT_ACCEPTED_BY_COURIER));
    }

    @Test
    void testCashboxPaymentAction() {
        var actions = defaultValuesActionsGetter.toBuilder()
                .cashboxPaymentAllowed(true)
                .build()
                .toOrderActions();

        assertThat(actions).contains(new OrderActionDto(CASHBOX_PAYMENT));
    }

    @ParameterizedTest
    @EnumSource(value = PartialDeliveryStatus.class)
    void testHasNotCancelDeliveryActionForFashionOrderWithFitting(PartialDeliveryStatus partialDeliveryStatus) {
        var actions = defaultValuesActionsGetter.toBuilder()
                .status(TRANSMITTED_TO_RECIPIENT)
                .fashion(true)
                .partialDeliveryStatus(partialDeliveryStatus)
                .build()
                .toOrderActions();

        assertThat(actions).doesNotContain(new OrderActionDto(CANCEL_DELIVERY));
    }

    @ParameterizedTest
    @EnumSource(value = PvzOrderStatus.class,
            names = {"ARRIVED_TO_PICKUP_POINT", "STORAGE_PERIOD_EXTENDED", "STORAGE_PERIOD_EXPIRED"})
    void testHasSimplifiedDeliveryActionForRecentlyDeliveredSiblingState(PvzOrderStatus status) {
        var actions = defaultValuesActionsGetter.toBuilder()
                .status(status)
                .simplifiedDeliveryAvailable(true)
                .build()
                .toOrderActions();

        assertThat(actions).contains(new OrderActionDto(SIMPLIFIED_DELIVERY));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testHasSimplifiedDeliveryActionForRecentlyDeliveredSiblingFashion(boolean fashion) {
        var actions = defaultValuesActionsGetter.toBuilder()
                .status(ARRIVED_TO_PICKUP_POINT)
                .fashion(fashion)
                .simplifiedDeliveryAvailable(true)
                .build()
                .toOrderActions();

        assertThat(actions).contains(new OrderActionDto(SIMPLIFIED_DELIVERY));
    }
}
