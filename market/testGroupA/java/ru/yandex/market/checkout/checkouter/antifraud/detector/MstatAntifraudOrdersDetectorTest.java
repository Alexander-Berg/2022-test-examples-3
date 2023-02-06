package ru.yandex.market.checkout.checkouter.antifraud.detector;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.qos.logback.classic.Logger;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.entity.AntifraudCheckResult;
import ru.yandex.market.antifraud.orders.entity.OrderVerdict;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemResponseDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderResponseDto;
import ru.yandex.market.antifraud.orders.web.entity.OrderItemChange;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.antifraud.FraudItemChangeActionMutation;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.CartFetchingContext;
import ru.yandex.market.checkout.checkouter.antifraud.entity.FraudCheckResult;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.degradation.AbstractDegradationTest;
import ru.yandex.market.checkout.checkouter.degradation.strategy.antifraud.AntifraudDetectDegradationStrategy;
import ru.yandex.market.checkout.checkouter.log.Loggers;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableOrder;
import ru.yandex.market.checkout.common.util.TestAppender;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Created by max-samoylov on 04.07.2019.
 */
@ContextConfiguration(classes = {
        AntifraudDetectDegradationStrategy.class,
        MstatAntifraudOrdersDetector.class,
        FraudItemChangeActionMutation.class
})
public class MstatAntifraudOrdersDetectorTest extends AbstractDegradationTest {

    @Autowired
    private RestTemplate antifraudRestTemplate;
    @Autowired
    private MstatAntifraudOrdersDetector detector;
    @Autowired
    private FraudItemChangeActionMutation itemChangeActionMutation;

    @BeforeEach
    void reset() {
        Mockito.reset(antifraudRestTemplate);
    }

    @Test
    public void noFraud() {
        Order order = OrderProvider.getBlueOrder();
        OrderVerdict orderVerdict = OrderVerdict.builder().checkResults(new HashSet<>()).build();

        when(antifraudRestTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.eq(OrderVerdict.class)))
                .thenReturn(new ResponseEntity<>(orderVerdict, HttpStatus.OK));

        FraudCheckResult expected = FraudCheckResult.NO_FRAUD;
        FraudCheckResult actual = detector.detectFraud(ImmutableOrder.from(order));

        assertEquals(expected, actual);
        verify(antifraudRestTemplate).exchange(Mockito.any(RequestEntity.class), Mockito.eq(OrderVerdict.class));
        verifyNoMoreInteractions(antifraudRestTemplate);
    }

    @Test
    public void fraud() {
        final Order order = OrderProvider.getBlueOrder();
        order.setFulfilment(null);
        OrderVerdict orderVerdict = OrderVerdict.builder()
                .checkResults(Collections.singleton(
                        new AntifraudCheckResult(AntifraudAction.CANCEL_ORDER, "", "fraud")
                ))
                .build();

        when(antifraudRestTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.eq(OrderVerdict.class)))
                .thenReturn(new ResponseEntity<>(orderVerdict, HttpStatus.OK));

        final FraudCheckResult expected = FraudCheckResult.fraud(OrderVerdict.EMPTY, "fraud");
        final FraudCheckResult actual = detector.detectFraud(ImmutableOrder.from(order));

        assertEquals(expected, actual);
        verify(antifraudRestTemplate).exchange(Mockito.any(RequestEntity.class), Mockito.eq(OrderVerdict.class));
        verifyNoMoreInteractions(antifraudRestTemplate);
    }

    @Test
    public void fraudFixedWithActions() {
        long itemId = 1L;

        Order originalOrder = OrderProvider.getBlueOrder();
        originalOrder.getItems().forEach(item -> item.setId(itemId));

        Set<AntifraudCheckResult> checkResults = new HashSet<>(
                Arrays.asList(new AntifraudCheckResult(AntifraudAction.PREPAID_ONLY, "", ""),
                        new AntifraudCheckResult(AntifraudAction.ORDER_ITEM_CHANGE, "", "")
                )
        );

        OrderItem orderItem = originalOrder.getItems().iterator().next();
        OrderItemResponseDto orderItemResponse = OrderItemResponseDto.builder()
                .offerId(orderItem.getOfferId())
                .feedId(orderItem.getFeedId())
                .bundleId(orderItem.getBundleId())
                .count(0)
                .changes(Sets.newHashSet(OrderItemChange.FRAUD_FIXED, OrderItemChange.MISSING))
                .build();
        OrderResponseDto fixedOrderResponse = new OrderResponseDto(Collections.singletonList(orderItemResponse));

        OrderVerdict orderVerdict = OrderVerdict.builder()
                .checkResults(checkResults)
                .fixedOrder(fixedOrderResponse)
                .build();

        when(antifraudRestTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.eq(OrderVerdict.class)))
                .thenReturn(new ResponseEntity<>(orderVerdict, HttpStatus.OK));


        final FraudCheckResult actual = detector.detectFraud(ImmutableOrder.from(originalOrder));

        var ctx = mock(CartFetchingContext.class);

        when(ctx.getOrder()).thenReturn(originalOrder);

        itemChangeActionMutation.onSuccess(List.of(actual), ctx);
        assertTrue(actual.isFraudAction());
        assertThat(originalOrder.getItem(itemId).getCount(), is(orderItemResponse.getCount()));
        assertThat(
                originalOrder.getItem(itemId).getChanges(),
                containsInAnyOrder(ItemChange.FRAUD_FIXED, ItemChange.MISSING)
        );

        verify(antifraudRestTemplate).exchange(Mockito.any(RequestEntity.class), Mockito.eq(OrderVerdict.class));
        verifyNoMoreInteractions(antifraudRestTemplate);
    }

    @Test
    public void fraudDegradation() {
        TestAppender appender = new TestAppender();

        Logger logger = ((Logger) LoggerFactory.getLogger(Loggers.KEY_VALUE_LOG));
        logger.addAppender(appender);

        Order order = OrderProvider.getBlueOrder();
        OrderRequestDto orderRequest = detector.makeOrderRequestFromOrder(ImmutableOrder.from(order));

        when(antifraudRestTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.eq(OrderVerdict.class)))
                .thenThrow(new RuntimeException("some exception"));

        FraudCheckResult actual = detector.detectFraud(ImmutableOrder.from(order));
        assertEquals(FraudCheckResult.NO_FRAUD, actual);

        assertEquals(2, appender.getLog().size());
        assertThat(appender.getLog().get(0).getMessage(), containsString("checkouter_degradation\tantifraud\t1.0"));
    }
}
