package ru.yandex.market.tpl.core.domain.order.validator;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.common.util.exception.TplOrderValidationException;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderDelivery;
import ru.yandex.market.tpl.core.domain.routing.schedule.RoutingRequestWaveService;
import ru.yandex.market.tpl.core.test.ClockUtil;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class TplOrderRescheduleValidatorTest {
    public static final long ORDER_DS_ID = 1L;
    public static final LocalDateTime ORDER_DELIVERY_DATE = LocalDateTime.of(LocalDate.of(2021, 6, 1),
            LocalTime.of(21, 0));
    @Mock
    private RoutingRequestWaveService routingRequestWaveService;
    @Mock
    private Clock clock;
    @Mock
    private ConfigurationProviderAdapter configurationProviderAdapter;

    @InjectMocks
    private TplOrderRescheduleValidator orderRescheduleValidator;

    @Test
    void canUpdateDeliveryDate_success_whenUpdateBeforeRoutingStarts() {
        //given
        Order mockedOrder = buildMockedOrder(ORDER_DELIVERY_DATE);
        doReturn(ORDER_DS_ID).when(mockedOrder).getDeliveryServiceId();

        LocalDateTime firstWaveDate = ORDER_DELIVERY_DATE.minusDays(1L);
        initWaveServiceMock(ORDER_DELIVERY_DATE, firstWaveDate);

        ClockUtil.initFixed(clock, firstWaveDate.minusHours(1L));

        doReturn(true).when(configurationProviderAdapter)
                .isBooleanEnabled(ConfigurationProperties.UPDATE_ORDER_VALIDATOR_DEPENDS_ROUTING_ENABLED);

        Interval renewalInterval = buildIntervalForDay(ORDER_DELIVERY_DATE.toLocalDate());

        //when
        assertDoesNotThrow(() -> orderRescheduleValidator.validate(mockedOrder, renewalInterval));
    }

    private Interval buildIntervalForDay(LocalDate deliveryDate) {
        return new Interval(
                DateTimeUtil.atDefaultZone(LocalDateTime.of(deliveryDate, LocalTime.of(11,0))),
                DateTimeUtil.atDefaultZone(LocalDateTime.of(deliveryDate, LocalTime.of(13,0)))
        );
    }

    @Test
    void canUpdateDeliveryDate_success_whenDeliveryDateEmpty() {
        //given
        Order mockedOrder = buildMockedOrder(null);

        LocalDateTime firstWaveDate = LocalDateTime.of(2021,6,1,21,0);


        ClockUtil.initFixed(clock, firstWaveDate.minusHours(1L));

        doReturn(true).when(configurationProviderAdapter)
                .isBooleanEnabled(ConfigurationProperties.UPDATE_ORDER_VALIDATOR_DEPENDS_ROUTING_ENABLED);


        Interval renewalInterval = buildIntervalForDay(ORDER_DELIVERY_DATE.toLocalDate());

        //when
        assertDoesNotThrow(() -> orderRescheduleValidator.validate(mockedOrder, renewalInterval));
    }

    @Test
    void canUpdateDeliveryDate_failure_whenUpdateAfterRoutingStartsAndSameDay() {
        //given
        Order mockedOrder = buildMockedOrder(ORDER_DELIVERY_DATE);
        doReturn(ORDER_DS_ID).when(mockedOrder).getDeliveryServiceId();

        LocalDateTime firstWaveDate = ORDER_DELIVERY_DATE.minusDays(1L);
        initWaveServiceMock(ORDER_DELIVERY_DATE, firstWaveDate);

        ClockUtil.initFixed(clock, firstWaveDate.plusHours(1L));

        doReturn(true).when(configurationProviderAdapter)
                .isBooleanEnabled(ConfigurationProperties.UPDATE_ORDER_VALIDATOR_DEPENDS_ROUTING_ENABLED);

        Interval renewalInterval = buildIntervalForDay(ORDER_DELIVERY_DATE.toLocalDate());

        //when
        assertThrows(TplOrderValidationException.class,
                () -> orderRescheduleValidator.validate(mockedOrder, renewalInterval));
    }

    @Test
    void canUpdateDeliveryDate_success_whenOrderForPickupPoint() {
        //given
        Order mockedOrder = buildPickupMockedOrder();

        doReturn(true).when(configurationProviderAdapter)
                .isBooleanEnabled(ConfigurationProperties.UPDATE_ORDER_VALIDATOR_DEPENDS_ROUTING_ENABLED);

        Interval renewalInterval = buildIntervalForDay(ORDER_DELIVERY_DATE.toLocalDate());

        //when
        assertDoesNotThrow(() -> orderRescheduleValidator.validate(mockedOrder, renewalInterval));
    }


    @Test
    void canUpdateDeliveryDate_success_whenUpdateAfterRoutingStartsAndNotSameDay() {
        //given
        Order mockedOrder = buildMockedOrder(ORDER_DELIVERY_DATE);

        LocalDateTime firstWaveDate = ORDER_DELIVERY_DATE.minusDays(1L);

        ClockUtil.initFixed(clock, firstWaveDate.plusHours(1L));

        doReturn(true).when(configurationProviderAdapter)
                .isBooleanEnabled(ConfigurationProperties.UPDATE_ORDER_VALIDATOR_DEPENDS_ROUTING_ENABLED);

        Interval renewalInterval = buildIntervalForDay(ORDER_DELIVERY_DATE.toLocalDate().plusDays(1));

        //when
        assertDoesNotThrow(() -> orderRescheduleValidator.validate(mockedOrder, renewalInterval));
    }

    @Test
    void canUpdateDeliveryDate_success_whenUpdateAfterRoutingStartsAndNextDayOrderUpdate() {
        //given
        Order mockedOrder = buildMockedOrder(ORDER_DELIVERY_DATE);
        lenient().doReturn(true).when(mockedOrder).isPickup();

        LocalDateTime firstWaveDate = ORDER_DELIVERY_DATE.minusDays(1L);

        ClockUtil.initFixed(clock, firstWaveDate.plusHours(1L));

        doReturn(true).when(configurationProviderAdapter)
                .isBooleanEnabled(ConfigurationProperties.UPDATE_ORDER_VALIDATOR_DEPENDS_ROUTING_ENABLED);

        Interval renewalInterval = buildIntervalForDay(ORDER_DELIVERY_DATE.toLocalDate().plusDays(1));

        //when
        assertDoesNotThrow(() -> orderRescheduleValidator.validateForAddressChange(mockedOrder, renewalInterval));
    }

    @Test
    void canUpdateDeliveryDate_failure_whenUpdateAfterRoutingStartsAndSameDayOrderUpdate() {
        //given
        Order mockedOrder = buildMockedOrder(ORDER_DELIVERY_DATE);
        lenient().doReturn(true).when(mockedOrder).isPickup();
        lenient().doReturn(ORDER_DS_ID).when(mockedOrder).getDeliveryServiceId();

        LocalDateTime firstWaveDate = ORDER_DELIVERY_DATE.minusDays(1L);
        initWaveServiceMock(ORDER_DELIVERY_DATE, firstWaveDate);

        ClockUtil.initFixed(clock, firstWaveDate.plusHours(1L));

        doReturn(true).when(configurationProviderAdapter)
                .isBooleanEnabled(ConfigurationProperties.UPDATE_ORDER_VALIDATOR_DEPENDS_ROUTING_ENABLED);

        Interval renewalInterval = buildIntervalForDay(ORDER_DELIVERY_DATE.toLocalDate());

        //when
        assertThrows(TplOrderValidationException.class,
                () -> orderRescheduleValidator.validateForAddressChange(mockedOrder, renewalInterval));
    }

    private void initWaveServiceMock(LocalDateTime orderDeliveryDate, LocalDateTime firstWaveDate) {
        doReturn(Optional.of(firstWaveDate)).when(routingRequestWaveService).getFirstRoutingWaveTime(eq(ORDER_DS_ID),
                eq(orderDeliveryDate.toLocalDate()));
    }

    private Order buildMockedOrder(LocalDateTime orderDeliveryDate) {
        OrderDelivery mockedOrderDelivery = mock(OrderDelivery.class);
        Optional.ofNullable(orderDeliveryDate)
                .map(DateTimeUtil::atDefaultZone)
                .ifPresent(date -> doReturn(date).when(mockedOrderDelivery).getDeliveryIntervalTo());


        Order mockedOrder = mock(Order.class);
        doReturn(mockedOrderDelivery).when(mockedOrder).getDelivery();

        return mockedOrder;
    }

    private Order buildPickupMockedOrder() {
        Order mockedOrder = mock(Order.class);
        doReturn(true).when(mockedOrder).isPickup();
        return mockedOrder;
    }
}
