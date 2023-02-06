package ru.yandex.market.antifraud.orders.detector;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.antifraud.orders.entity.ItemLimitRule;
import ru.yandex.market.antifraud.orders.entity.ItemLimitRuleWithHistory;
import ru.yandex.market.antifraud.orders.entity.MarketUserId;
import ru.yandex.market.antifraud.orders.model.OrderDataContainer;
import ru.yandex.market.antifraud.orders.model.OrderDetectorResult;
import ru.yandex.market.antifraud.orders.service.ItemLimitRulesService;
import ru.yandex.market.antifraud.orders.storage.entity.limits.LimitRuleConverter;
import ru.yandex.market.antifraud.orders.storage.entity.limits.LimitRuleType;
import ru.yandex.market.antifraud.orders.storage.entity.limits.PgItemLimitRule;
import ru.yandex.market.antifraud.orders.test.providers.OrderDeliveryProvider;
import ru.yandex.market.antifraud.orders.test.providers.OrderItemRequestProvider;
import ru.yandex.market.antifraud.orders.test.providers.OrderRequestProvider;
import ru.yandex.market.antifraud.orders.util.concurrent.FutureValueHolder;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.CartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.MultiCartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemResponseDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderResponseDto;
import ru.yandex.market.crm.platform.commons.RGBType;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.Order;
import ru.yandex.market.crm.platform.models.OrderItem;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.ITERABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;
import static ru.yandex.market.antifraud.orders.entity.AntifraudAction.ORDER_ITEM_CHANGE;
import static ru.yandex.market.antifraud.orders.web.entity.OrderItemChange.COUNT;
import static ru.yandex.market.antifraud.orders.web.entity.OrderItemChange.FRAUD_FIXED;
import static ru.yandex.market.antifraud.orders.web.entity.OrderItemChange.MISSING;

/**
 * @author dzvyagin
 */
@RunWith(MockitoJUnitRunner.class)
public class PgItemLimitRuleDetectorTest {
    @Mock
    private ItemLimitRulesService rulesService;
    private PgItemLimitRuleDetector detector;

    private final long TEST_MSKU = 1L;
    private final int TEST_CATEGORY_ID = 123;
    private final long TEST_ITEM_ID = 1L;

    @Before
    public void setUp() {
        detector = new PgItemLimitRuleDetector(rulesService);
    }

    @Test
    public void noFraud_emptyItems() {
        var order = OrderRequestProvider.getFulfillmentOrderRequest(emptyList());
        final OrderDetectorResult expected = OrderDetectorResult.empty(detector.getUniqName());
        final OrderDetectorResult actual = detector.detectFraud(simpleOrderContainer(order));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void noFraud_noItemRules() {
        when(rulesService.getDirectRulesForItems(anyCollection(), any(), any()))
                .thenReturn(emptyList());

        var order = OrderRequestProvider.getFulfillmentOrderRequest(
                Collections.singletonList(OrderItemRequestProvider.getPreparedBuilder().msku(0L).build()));

        final OrderDetectorResult expected = OrderDetectorResult.empty(detector.getUniqName());
        final OrderDetectorResult actual = detector.detectFraud(simpleOrderContainer(order));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void fraudFixed_msku_maxCountPerOrder() {
        PgItemLimitRule rule = PgItemLimitRule.builder()
                .msku(TEST_MSKU)
                .maxCount(1L)
                .ruleType(LimitRuleType.ORDER)
                .build();
        when(rulesService.getDirectRulesForItems(anyCollection(), any(), any()))
                .thenReturn(List.of(testRule(rule)));

        OrderItemRequestDto item =
                OrderItemRequestProvider.getPreparedBuilder()
                        .id(TEST_ITEM_ID).offerId("1L").msku(TEST_MSKU).count(2).build();
        var order = OrderRequestProvider.getFulfillmentOrderRequest(
                Collections.singletonList(item));


        OrderItemResponseDto expectedItem = OrderItemResponseDto.builder()
                .id(1L)
                .feedId(1L)
                .offerId("1L")
                .count(1)
                .changes(new HashSet<>(asList(FRAUD_FIXED, COUNT)))
                .build();
        OrderResponseDto expectedOrder = new OrderResponseDto(singletonList(expectedItem));
        OrderDetectorResult expected = OrderDetectorResult.builder()
                .ruleName(detector.getUniqName())
                .answerText("Состав корзины был изменен в связи с категорийными ограничениями")
                .reason("Состав корзины был изменен в связи с категорийными ограничениями")
                .actions(Set.of(ORDER_ITEM_CHANGE))
                .fixedOrder(expectedOrder)
                .build();
        OrderDetectorResult actual = detector.detectFraud(simpleOrderContainer(order));

        assertThat(actual.getRuleName()).isEqualTo(expected.getRuleName());
        assertThat(actual.getReason()).isEqualTo(expected.getReason());
        assertThat(actual.getActions()).containsAll(expected.getActions());
        assertThat(actual.getFixedOrder().getItems()).containsAll(expected.getFixedOrder().getItems());
    }

    @Test
    public void fraudFixedCount_msku_maxCountPerUser_noHistory() {
        PgItemLimitRule rule = PgItemLimitRule.builder()
                .msku(TEST_MSKU)
                .maxCount(1L)
                .ruleType(LimitRuleType.USER)
                .build();
        when(rulesService.getDirectRulesForItems(anyCollection(), any(), any()))
                .thenReturn(List.of(testRule(rule)));
        OrderItemRequestDto item =
                OrderItemRequestProvider.getPreparedBuilder().id(TEST_ITEM_ID)
                        .offerId("1L").msku(TEST_MSKU).count(2).build();
        var order = OrderRequestProvider.getFulfillmentOrderRequest(
                Collections.singletonList(item));


        OrderItemResponseDto expectedItem = OrderItemResponseDto.builder()
                .id(1L)
                .feedId(1L)
                .offerId("1L")
                .count(1)
                .changes(new HashSet<>(asList(FRAUD_FIXED, COUNT)))
                .build();

        OrderResponseDto expectedOrder = new OrderResponseDto(singletonList(expectedItem));
        OrderDetectorResult expected = OrderDetectorResult.builder()
                .ruleName(detector.getUniqName())
                .answerText("Состав корзины был изменен в связи с категорийными ограничениями")
                .reason("Состав корзины был изменен в связи с категорийными ограничениями")
                .actions(Set.of(ORDER_ITEM_CHANGE))
                .fixedOrder(expectedOrder)
                .build();
        final OrderDetectorResult actual = detector.detectFraud(simpleOrderContainer(order));

        assertThat(actual.getRuleName()).isEqualTo(expected.getRuleName());
        assertThat(actual.getReason()).isEqualTo(expected.getReason());
        assertThat(actual.getActions()).containsAll(expected.getActions());
        assertThat(actual.getFixedOrder().getItems()).containsAll(expected.getFixedOrder().getItems());
    }

    /**
     * Тест очень похож на предыдущий, но важно, что проверка именно для категории. Связано с найденным багом, который
     * поправлен в этом же коммите в правиле на категорийные правила
     */
    @Test
    public void fraudFixedCount_categoryId_maxCountPerUser_noHistory() {
        PgItemLimitRule rule = PgItemLimitRule.builder()
                .categoryId(TEST_CATEGORY_ID)
                .maxCount(1L)
                .ruleType(LimitRuleType.USER)
                .build();
        when(rulesService.getDirectRulesForItems(anyCollection(), any(), any()))
                .thenReturn(List.of(testRule(rule)));

        OrderItemRequestDto item1 = OrderItemRequestProvider.getPreparedBuilder()
                .id(TEST_ITEM_ID + 1)
                .msku(2L)
                .offerId("2L")
                .categoryId(TEST_CATEGORY_ID + 1)
                .count(2)
                .build();

        OrderItemRequestDto item2 = OrderItemRequestProvider.getPreparedBuilder()
                .id(TEST_ITEM_ID)
                .msku(3L)
                .offerId("1L")
                .categoryId(TEST_CATEGORY_ID)
                .count(3)
                .build();

        var order = OrderRequestProvider.getFulfillmentOrderRequest(
                Arrays.asList(item1, item2));

        OrderItemResponseDto expectedItem = OrderItemResponseDto.builder()
                .id(1L)
                .feedId(1L)
                .offerId("1L")
                .count(1)
                .changes(new HashSet<>(asList(FRAUD_FIXED, COUNT)))
                .build();

        OrderResponseDto expectedOrder = new OrderResponseDto(singletonList(expectedItem));
        OrderDetectorResult expected = OrderDetectorResult.builder()
                .ruleName(detector.getUniqName())
                .answerText("Состав корзины был изменен в связи с категорийными ограничениями")
                .reason("Состав корзины был изменен в связи с категорийными ограничениями")
                .actions(Set.of(ORDER_ITEM_CHANGE))
                .fixedOrder(expectedOrder)
                .build();
        final OrderDetectorResult actual = detector.detectFraud(simpleOrderContainer(order));

        assertThat(actual.getRuleName()).isEqualTo(expected.getRuleName());
        assertThat(actual.getReason()).isEqualTo(expected.getReason());
        assertThat(actual.getActions()).containsAll(expected.getActions());
        assertThat(actual.getFixedOrder().getItems()).containsExactlyElementsOf(expected.getFixedOrder().getItems());
    }

    @Test
    public void fraudFixedCount_categoryId_maxCountPerUser_noHistory_sameCategory() {
        PgItemLimitRule rule = PgItemLimitRule.builder()
                .categoryId(TEST_CATEGORY_ID)
                .maxCount(1L)
                .ruleType(LimitRuleType.ORDER)
                .build();
        when(rulesService.getDirectRulesForItems(anyCollection(), any(), any()))
                .thenReturn(List.of(testRule(rule)));

        OrderItemRequestDto item1 = OrderItemRequestProvider.getPreparedBuilder()
                .id(TEST_ITEM_ID)
                .msku(2L)
                .offerId("2L")
                .categoryId(TEST_CATEGORY_ID)
                .count(2)
                .build();

        OrderItemRequestDto item2 = OrderItemRequestProvider.getPreparedBuilder()
                .id(TEST_ITEM_ID + 1)
                .msku(3L)
                .offerId("3L")
                .categoryId(TEST_CATEGORY_ID)
                .count(2)
                .build();

        var order = OrderRequestProvider.getFulfillmentOrderRequest(
                Arrays.asList(item1, item2));

        OrderItemResponseDto expectedItem1 = OrderItemResponseDto.builder()
                .id(1L)
                .feedId(1L)
                .offerId("2L")
                .count(1)
                .changes(new HashSet<>(asList(FRAUD_FIXED, COUNT)))
                .build();
        OrderItemResponseDto expectedItem2 = OrderItemResponseDto.builder()
                .id(2L)
                .feedId(1L)
                .offerId("3L")
                .count(1)
                .changes(new HashSet<>(asList(FRAUD_FIXED, COUNT)))
                .build();
        final OrderDetectorResult actual = detector.detectFraud(simpleOrderContainer(order));

        assertThat(actual.getActions()).contains(ORDER_ITEM_CHANGE);
        assertThat(actual.getFixedOrder().getItems()).contains(expectedItem1, expectedItem2);
    }

    @Test
    public void fraudFixedMissing_msku_maxCountPerUser_withHistory() {
        final long msku = 1L;
        final long itemId = 1L;

        PgItemLimitRule rule = PgItemLimitRule.builder()
                .msku(msku)
                .maxCount(1L)
                .ruleType(LimitRuleType.USER)
                .periodDays(7)
                .build();

        var order = OrderRequestProvider.getFulfillmentOrderRequest(
                Collections.singletonList(OrderItemRequestProvider.getPreparedBuilder()
                        .id(itemId)
                        .msku(msku)
                        .modelId(0L)
                        .count(1)
                        .build())
                );

        OrderItemResponseDto expectedItem = OrderItemResponseDto.builder()
                .id(1L)
                .feedId(1L)
                .offerId(null)
                .count(0)
                .changes(new HashSet<>(asList(FRAUD_FIXED, MISSING)))
                .build();
        OrderResponseDto expectedOrder = new OrderResponseDto(singletonList(expectedItem));
        OrderDetectorResult expected = OrderDetectorResult.builder()
                .ruleName(detector.getUniqName())
                .answerText("Состав корзины был изменен в связи с категорийными ограничениями")
                .reason("Состав корзины был изменен в связи с категорийными ограничениями")
                .actions(Set.of(ORDER_ITEM_CHANGE))
                .fixedOrder(expectedOrder)
                .build();
        Long puid = order.getBuyer().getUid();
        Order historyOrder = Order.newBuilder()
                .setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(puid).build())
                .setCreationDate(Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli())
                .addItems(OrderItem.newBuilder()
                        .setCount(2)
                        .setModelId(0L)
                        .setSku(String.valueOf(msku)))
                .setRgb(RGBType.BLUE)
                .build();

        when(rulesService.getDirectRulesForItems(anyCollection(), any(), any()))
            .thenReturn(List.of(testRule(rule, Instant.now(), puid, List.of(historyOrder))));

        final OrderDetectorResult actual = detector.detectFraud(OrderDataContainer.builder()
                .orderRequest(order)
                .gluedIdsFuture(new FutureValueHolder<>(Set.of(MarketUserId.fromUid(puid))))
                .lastOrdersFuture(new FutureValueHolder<>(List.of(historyOrder)))
                .build());

        assertThat(actual.getRuleName()).isEqualTo(expected.getRuleName());
        assertThat(actual.getReason()).isEqualTo(expected.getReason());
        assertThat(actual.getActions()).containsAll(expected.getActions());
        assertThat(actual.getFixedOrder().getItems()).containsAll(expected.getFixedOrder().getItems());
    }

    @Test
    public void fraudFixed_msku_maxAmountPerOrder() {
        final long msku = 1L;
        final long itemId = 1L;

        PgItemLimitRule rule = PgItemLimitRule.builder()
                .msku(msku)
                .maxPrice(BigDecimal.valueOf(5))
                .ruleType(LimitRuleType.ORDER)
                .build();
        when(rulesService.getDirectRulesForItems(anyCollection(), any(), any()))
                .thenReturn(List.of(testRule(rule)));

        OrderItemRequestDto item = OrderItemRequestProvider.getPreparedBuilder()
                .id(itemId)
                .msku(msku)
                .price(BigDecimal.valueOf(1.5d))
                .count(4)
                .build();
        var order = OrderRequestProvider.getFulfillmentOrderRequest(
                Collections.singletonList(item));

        OrderItemResponseDto expectedItem = OrderItemResponseDto.builder()
                .id(1L)
                .feedId(1L)
                .offerId(null)
                .count(3)
                .changes(new HashSet<>(asList(FRAUD_FIXED, COUNT)))
                .build();

        OrderResponseDto expectedOrder = new OrderResponseDto(singletonList(expectedItem));
        OrderDetectorResult expected = OrderDetectorResult.builder()
                .ruleName(detector.getUniqName())
                .answerText("Состав корзины был изменен в связи с категорийными ограничениями")
                .reason("Состав корзины был изменен в связи с категорийными ограничениями")
                .actions(Set.of(ORDER_ITEM_CHANGE))
                .fixedOrder(expectedOrder)
                .build();
        final OrderDetectorResult actual =
                detector.detectFraud(simpleOrderContainer(order));

        assertThat(actual.getRuleName()).isEqualTo(expected.getRuleName());
        assertThat(actual.getReason()).isEqualTo(expected.getReason());
        assertThat(actual.getActions()).containsAll(expected.getActions());
        assertThat(actual.getFixedOrder().getItems()).containsAll(expected.getFixedOrder().getItems());
    }

    @Test
    public void fraudFixed_msku_maxAmountPerUser_noHistory() {
        final long msku = 1L;
        final long itemId = 1L;

        PgItemLimitRule rule = PgItemLimitRule.builder()
                .msku(msku)
                .maxPrice(BigDecimal.valueOf(5))
                .ruleType(LimitRuleType.USER)
                .build();
        when(rulesService.getDirectRulesForItems(anyCollection(), any(), any()))
                .thenReturn(List.of(testRule(rule)));

        OrderItemRequestDto item = OrderItemRequestProvider.getPreparedBuilder()
                .id(itemId)
                .msku(msku)
                .price(BigDecimal.valueOf(1.5d))
                .count(4)
                .build();
        var order = OrderRequestProvider.getFulfillmentOrderRequest(
                Collections.singletonList(item));

        OrderItemResponseDto expectedItem = OrderItemResponseDto.builder()
                .id(1L)
                .feedId(1L)
                .offerId(null)
                .count(3)
                .changes(new HashSet<>(asList(FRAUD_FIXED, COUNT)))
                .build();

        OrderResponseDto expectedOrder = new OrderResponseDto(singletonList(expectedItem));
        OrderDetectorResult expected = OrderDetectorResult.builder()
                .ruleName(detector.getUniqName())
                .answerText("Состав корзины был изменен в связи с категорийными ограничениями")
                .reason("Состав корзины был изменен в связи с категорийными ограничениями")
                .actions(Set.of(ORDER_ITEM_CHANGE))
                .fixedOrder(expectedOrder)
                .build();
        final OrderDetectorResult actual =
                detector.detectFraud(simpleOrderContainer(order));

        assertThat(actual.getRuleName()).isEqualTo(expected.getRuleName());
        assertThat(actual.getReason()).isEqualTo(expected.getReason());
        assertThat(actual.getActions()).containsAll(expected.getActions());
        assertThat(actual.getFixedOrder().getItems()).containsAll(expected.getFixedOrder().getItems());
    }

    @Test
    public void fraudFixed_msku_maxAmountPerUser_withHistory() {
        final long msku = 1L;
        final long itemId = 1L;

        PgItemLimitRule rule = PgItemLimitRule.builder()
                .msku(msku)
                .maxPrice(BigDecimal.valueOf(6))
                .ruleType(LimitRuleType.USER)
                .periodDays(7)
                .build();

        OrderItemRequestDto item = OrderItemRequestProvider.getPreparedBuilder()
                .id(itemId)
                .msku(msku)
                .modelId(0L)
                .price(BigDecimal.valueOf(1.5d))
                .count(4)
                .build();
        var order = OrderRequestProvider.getFulfillmentOrderRequest(
                Collections.singletonList(item));

        OrderItemResponseDto expectedItem = OrderItemResponseDto.builder()
                .id(1L)
                .feedId(1L)
                .offerId(null)
                .count(3)
                .changes(new HashSet<>(asList(FRAUD_FIXED, COUNT)))
                .build();
        OrderResponseDto expectedOrder = new OrderResponseDto(singletonList(expectedItem));
        OrderDetectorResult expected = OrderDetectorResult.builder()
                .ruleName(detector.getUniqName())
                .answerText("Состав корзины был изменен в связи с категорийными ограничениями")
                .reason("Состав корзины был изменен в связи с категорийными ограничениями")
                .actions(Set.of(ORDER_ITEM_CHANGE))
                .fixedOrder(expectedOrder)
                .build();
        Long puid = order.getBuyer().getUid();
        Order historyOrder = Order.newBuilder()
                .setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(puid).build())
                .setCreationDate(Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli())
                .addItems(OrderItem.newBuilder()
                        .setCount(1)
                        .setModelId(0L)
                        .setPrice(100)
                        .setSku(String.valueOf(msku)))
                .setRgb(RGBType.BLUE)
                .build();

        when(rulesService.getDirectRulesForItems(anyCollection(), any(), any()))
            .thenReturn(List.of(testRule(rule, Instant.now(), puid, List.of(historyOrder))));

        final OrderDetectorResult actual = detector.detectFraud(OrderDataContainer.builder()
                .orderRequest(order)
                .gluedIdsFuture(new FutureValueHolder<>(Set.of(MarketUserId.fromUid(puid))))
                .lastOrdersFuture(new FutureValueHolder<>(List.of(historyOrder)))
                .build());

        assertThat(actual.getRuleName()).isEqualTo(expected.getRuleName());
        assertThat(actual.getReason()).isEqualTo(expected.getReason());
        assertThat(actual.getActions()).containsAll(expected.getActions());
        assertThat(actual.getFixedOrder().getItems()).containsAll(expected.getFixedOrder().getItems());
    }

    @Test
    public void fraudFixedDsbs() {
        final long msku = 1L;
        PgItemLimitRule rule1 = PgItemLimitRule.builder()
                .modelId(2L)
                .maxCount(5L)
                .ruleType(LimitRuleType.USER)
                .periodDays(7)
                .build();
        PgItemLimitRule rule2 = PgItemLimitRule.builder()
                .modelId(2L)
                .maxCount(4L)
                .ruleType(LimitRuleType.USER)
                .periodDays(7)
                .build();

        OrderItemRequestDto item = OrderItemRequestProvider.getPreparedBuilder()
                .id(2L)
                .modelId(2L)
                .offerId("2")
                .price(BigDecimal.valueOf(1.5d))
                .count(4)
                .build();
        var order = OrderRequestProvider.getFulfillmentOrderRequest(
                List.of(item));

        OrderItemResponseDto expectedItem = OrderItemResponseDto.builder()
                .id(2L)
                .feedId(1L)
                .offerId("2")
                .count(3)
                .changes(new HashSet<>(asList(FRAUD_FIXED, COUNT)))
                .build();

        OrderResponseDto expectedOrder = new OrderResponseDto(singletonList(expectedItem));
        OrderDetectorResult expected = OrderDetectorResult.builder()
                .ruleName(detector.getUniqName())
                .answerText("Состав корзины был изменен в связи с категорийными ограничениями")
                .reason("Состав корзины был изменен в связи с категорийными ограничениями")
                .actions(Set.of(ORDER_ITEM_CHANGE))
                .fixedOrder(expectedOrder)
                .build();
        Long puid = order.getBuyer().getUid();
        Order historyOrder = Order.newBuilder()
                .setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(puid).build())
                .setCreationDate(Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli())
                .addItems(OrderItem.newBuilder()
                        .setCount(1)
                        .setPrice(100)
                        .setModelId(2L))
                .setRgb(RGBType.BLUE)
                .build();

        when(rulesService.getDirectRulesForItems(anyCollection(), any(), any()))
            .thenReturn(List.of(testRule(rule1, Instant.now(), puid, List.of(historyOrder)), testRule(rule2, Instant.now(), puid, List.of(historyOrder))));

        final OrderDetectorResult actual = detector.detectFraud(OrderDataContainer.builder()
                .orderRequest(order)
                .gluedIdsFuture(new FutureValueHolder<>(Set.of(MarketUserId.fromUid(puid))))
                .lastOrdersFuture(new FutureValueHolder<>(List.of(historyOrder)))
                .build());

        assertThat(actual.getRuleName()).isEqualTo(expected.getRuleName());
        assertThat(actual.getReason()).isEqualTo(expected.getReason());
        assertThat(actual.getActions()).containsAll(expected.getActions());
        assertThat(actual.getFixedOrder().getItems()).containsAll(expected.getFixedOrder().getItems());
    }

    @Test
    public void fraudFixedMissing_msku_maxCountPerUser_withPreviousOrders() {
        final long msku = 1L;
        final long itemId1 = 1L;
        final long itemId2 = 2L;

        PgItemLimitRule rule = PgItemLimitRule.builder()
            .msku(msku)
            .maxCount(1L)
            .ruleType(LimitRuleType.USER)
            .periodDays(7)
            .build();
        when(rulesService.getDirectRulesForItems(anyCollection(), any(), any()))
            .thenReturn(List.of(testRule(rule)));

        var order = OrderRequestProvider.getPreparedOrderRequestBuilder()
            .carts(List.of(
                CartRequestDto.builder()
                    .fulfilment(true)
                    .items(List.of(
                        OrderItemRequestProvider.getPreparedBuilder()
                            .id(itemId1)
                            .count(1)
                            .modelId(0L)
                            .msku(msku)
                            .build()
                    ))
                    .delivery(OrderDeliveryProvider.getEmptyOrderDeliveryRequest())
                    .build(),
                CartRequestDto.builder()
                    .fulfilment(true)
                    .items(List.of(
                        OrderItemRequestProvider.getPreparedBuilder()
                            .id(itemId2)
                            .msku(msku)
                            .modelId(0L)
                            .count(1)
                            .build()
                    ))
                    .delivery(OrderDeliveryProvider.getEmptyOrderDeliveryRequest())
                    .build()
            ))
            .build();

        OrderItemResponseDto expectedItem1 = OrderItemResponseDto.builder()
            .id(1L)
            .feedId(1L)
            .offerId(null)
            .count(1)
            .changes(Set.of())
            .build();
        OrderItemResponseDto expectedItem2 = OrderItemResponseDto.builder()
            .id(2L)
            .feedId(1L)
            .offerId(null)
            .count(0)
            .changes(new HashSet<>(asList(FRAUD_FIXED, MISSING)))
            .build();
        OrderResponseDto expectedOrder = new OrderResponseDto(List.of(expectedItem1, expectedItem2));
        OrderDetectorResult expected = OrderDetectorResult.builder()
            .ruleName(detector.getUniqName())
            .answerText("Состав корзины был изменен в связи с категорийными ограничениями")
            .reason("Состав корзины был изменен в связи с категорийными ограничениями")
            .actions(Set.of(ORDER_ITEM_CHANGE))
            .fixedOrder(expectedOrder)
            .build();
        final OrderDetectorResult actual = detector.detectFraud(simpleOrderContainer(order));

        assertThat(actual)
            .isEqualToIgnoringGivenFields(expected, "fixedOrder")
            .extracting(x -> x.getFixedOrder().getItems(), as(ITERABLE))
            .containsExactlyElementsOf(expected.getFixedOrder().getItems());
    }

    private OrderDataContainer simpleOrderContainer(MultiCartRequestDto order) {
        return OrderDataContainer.builder()
                .orderRequest(order)
                .gluedIdsFuture(new FutureValueHolder<>(Set.of(MarketUserId.fromUid(order.getBuyer().getUid()))))
                .lastOrdersFuture(new FutureValueHolder<>(List.of()))
                .build();
    }

    private ItemLimitRule testRule(PgItemLimitRule pgItemLimitRule) {
        var limitRuleConverter = new LimitRuleConverter();
        return limitRuleConverter.applyHistory(
            limitRuleConverter.convertToBusinessObject(pgItemLimitRule, Instant.now(), null),
            List.of());
    }

    private ItemLimitRule testRule(PgItemLimitRule pgItemLimitRule, Instant now, long buyerUid, List<Order> historyOrders) {
        var limitRuleConverter = new LimitRuleConverter();
        return limitRuleConverter.applyHistory(
            limitRuleConverter.convertToBusinessObject(pgItemLimitRule, now, buyerUid),
            historyOrders);
    }
}
