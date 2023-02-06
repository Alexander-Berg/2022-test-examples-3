package ru.yandex.market.antifraud.orders.service.processors;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.detector.ItemAutoLimitDetector;
import ru.yandex.market.antifraud.orders.detector.PgItemLimitRuleDetector;
import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.entity.AntifraudCheckResult;
import ru.yandex.market.antifraud.orders.entity.OrderVerdict;
import ru.yandex.market.antifraud.orders.model.OrderDetectorResult;
import ru.yandex.market.antifraud.orders.storage.entity.rules.ItemAutoLimitDetectorConfiguration;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.CartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.MultiCartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemResponseDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderResponseDto;
import ru.yandex.market.antifraud.orders.web.entity.OrderItemChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author dzvyagin
 */
public class CountMergerTest {

    private static final String AUTO_LIMIT_PREPAY_ANSWER =
            ItemAutoLimitDetectorConfiguration.RestrictionType.PREPAY.getAnswerText(null, null);
    private static final String ABOVE_MULTIPLIED_LIMIT_MESSAGE =
            ItemAutoLimitDetectorConfiguration.RestrictionType.CHANGE_ORDER_ABOVE_LIMIT.getAnswerText(null, null);

    @Test
    public void mergeOrderFixes() {
        OrderResponseDto fixedOrder1 = new OrderResponseDto(List.of(
                OrderItemResponseDto.builder().id(1L).count(12).changes(Set.of(OrderItemChange.COUNT)).build(),
                OrderItemResponseDto.builder().id(2L).count(6).changes(Set.of(OrderItemChange.COUNT)).build()
        ));
        OrderResponseDto fixedOrder2 = new OrderResponseDto(List.of(
                OrderItemResponseDto.builder().id(2L).count(10).changes(Set.of(OrderItemChange.COUNT)).build()
        ));
        List<OrderDetectorResult> detectorResults = List.of(
                OrderDetectorResult.builder()
                        .ruleName(new ItemAutoLimitDetector(null, null, null).getUniqName())
                        .fixedOrder(fixedOrder1)
                        .fixedOrderForPrepay(fixedOrder1)
                        .actions(Set.of(AntifraudAction.ORDER_ITEM_CHANGE))
                        .build(),
                OrderDetectorResult.builder()
                        .ruleName(new ItemAutoLimitDetector(null, null, null).getUniqName())
                        .fixedOrder(fixedOrder2)
                        .fixedOrderForPrepay(fixedOrder2)
                        .actions(Set.of(AntifraudAction.ORDER_ITEM_CHANGE))
                        .build(),
                OrderDetectorResult.builder()
                        .ruleName(new PgItemLimitRuleDetector(null).getUniqName())
                        .fixedOrder(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(22).changes(Set.of(OrderItemChange.COUNT, OrderItemChange.FRAUD_FIXED)).build()
                        )))
                        .actions(Set.of(AntifraudAction.ORDER_ITEM_CHANGE))
                        .build()
        );
        List<OrderItemResponseDto> items = getItems(detectorResults);
        OrderVerdict orderVerdict = buildVerdict(items)
                .withCheckResults(Set.of(new AntifraudCheckResult(AntifraudAction.ORDER_ITEM_CHANGE, "auto", ""),
                        new AntifraudCheckResult(AntifraudAction.ORDER_ITEM_CHANGE, "pg", "")));

        OrderVerdict verdict = new CountMerger().process(buildRequestWithManyItems(), detectorResults, orderVerdict);

        assertNotNull(verdict.getCheckResults());
        Set<AntifraudAction> verdictActions =
                verdict.getCheckResults().stream().map(AntifraudCheckResult::getAntifraudAction).collect(Collectors.toSet());
        assertThat(verdictActions).doesNotContain(AntifraudAction.PREPAID_ONLY);
        assertThat(verdictActions).contains(AntifraudAction.ORDER_ITEM_CHANGE);
        OrderResponseDto merged = verdict.getFixedOrder();
        assertNotNull(merged);
        assertThat(merged.getItems()).containsExactly(
                OrderItemResponseDto.builder().id(1L).count(22).changes(Set.of(OrderItemChange.COUNT, OrderItemChange.FRAUD_FIXED)).build(),
                OrderItemResponseDto.builder().id(2L).count(6).changes(Set.of(OrderItemChange.COUNT)).build()
        );
    }

    @Test
    public void mergeManyFixesWithPgRuleOk() {
        OrderResponseDto fixedOrder = new OrderResponseDto(List.of(
                OrderItemResponseDto.builder().id(1L).count(12).changes(Set.of(OrderItemChange.COUNT)).build(),
                OrderItemResponseDto.builder().id(2L).count(6).changes(Set.of(OrderItemChange.COUNT)).build()
        ));
        List<OrderDetectorResult> detectorResults = List.of(
                OrderDetectorResult.builder()
                        .ruleName(new ItemAutoLimitDetector(null, null, null).getUniqName())
                        .fixedOrder(fixedOrder)
                        .fixedOrderForPrepay(fixedOrder)
                        .actions(Set.of(AntifraudAction.ORDER_ITEM_CHANGE))
                        .build(),
                OrderDetectorResult.builder()
                        .ruleName(new PgItemLimitRuleDetector(null).getUniqName())
                        .fixedOrder(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(13).changes(Set.of()).build()
                        )))
                        .actions(Set.of(AntifraudAction.NO_ACTION))
                        .build()
        );
        List<OrderItemResponseDto> items = getItems(detectorResults);
        OrderVerdict orderVerdict = buildVerdict(items)
                .withCheckResults(Set.of(new AntifraudCheckResult(AntifraudAction.ORDER_ITEM_CHANGE, "", "")));

        OrderVerdict verdict = new CountMerger().process(buildRequestWithManyItems(), detectorResults, orderVerdict);

        assertNotNull(verdict.getCheckResults());
        Set<AntifraudAction> verdictActions =
                verdict.getCheckResults().stream().map(AntifraudCheckResult::getAntifraudAction).collect(Collectors.toSet());
        assertThat(verdictActions).doesNotContain(AntifraudAction.PREPAID_ONLY);
        assertThat(verdictActions).contains(AntifraudAction.ORDER_ITEM_CHANGE);
        OrderResponseDto merged = verdict
                .getFixedOrder();
        assertNotNull(merged);
        assertThat(merged.getItems()).containsExactly(
                OrderItemResponseDto.builder().id(2L).count(6).changes(Set.of(OrderItemChange.COUNT)).build()
        );
    }

    @Test
    public void mergeOneFixWithPgRuleOk() {
        OrderResponseDto fixedOrder = new OrderResponseDto(List.of(
                OrderItemResponseDto.builder().id(1L).count(1).changes(Set.of(OrderItemChange.COUNT)).build()
        ));
        List<OrderDetectorResult> detectorResults = List.of(
                OrderDetectorResult.builder()
                        .ruleName(new ItemAutoLimitDetector(null, null, null).getUniqName())
                        .fixedOrder(fixedOrder)
                        .fixedOrderForPrepay(fixedOrder)
                        .actions(Set.of(AntifraudAction.ORDER_ITEM_CHANGE))
                        .build(),
                OrderDetectorResult.builder()
                        .ruleName(new PgItemLimitRuleDetector(null).getUniqName())
                        .fixedOrder(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(2).changes(Set.of()).build()
                        )))
                        .actions(Set.of(AntifraudAction.NO_ACTION))
                        .build()
        );
        List<OrderItemResponseDto> items = getItems(detectorResults);
        OrderVerdict orderVerdict = buildVerdict(items)
                .withCheckResults(Set.of(new AntifraudCheckResult(AntifraudAction.ORDER_ITEM_CHANGE, "", "")));

        OrderVerdict verdict = new CountMerger().process(buildRequest(), detectorResults, orderVerdict);

        assertNotNull(verdict.getCheckResults());
        Set<AntifraudAction> verdictActions =
                verdict.getCheckResults().stream().map(AntifraudCheckResult::getAntifraudAction).collect(Collectors.toSet());
        assertThat(verdictActions).doesNotContain(AntifraudAction.PREPAID_ONLY);
        assertThat(verdictActions).doesNotContain(AntifraudAction.ORDER_ITEM_CHANGE);
        assertNull(verdict.getFixedOrder());
        assertNotNull(verdict.getCheckResults());
        assertTrue(verdict.getCheckResults().isEmpty());
    }

    @Test
    public void mergeAutoLimitPrepayAndPgRuleFix() {
        List<OrderDetectorResult> detectorResults = List.of(
                OrderDetectorResult.builder()
                        .ruleName(new ItemAutoLimitDetector(null, null, null).getUniqName())
                        .fixedOrder(new OrderResponseDto(List.of()))
                        .fixedOrderForPrepay(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(3).changes(Set.of(OrderItemChange.COUNT)).build()
                        )))
                        .actions(Set.of(AntifraudAction.PREPAID_ONLY))
                        .reason("")
                        .answerText("")
                        .build(),
                OrderDetectorResult.builder()
                        .ruleName(new PgItemLimitRuleDetector(null).getUniqName())
                        .fixedOrder(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(2).changes(Set.of(OrderItemChange.COUNT, OrderItemChange.FRAUD_FIXED)).build()
                        )))
                        .actions(Set.of(AntifraudAction.ORDER_ITEM_CHANGE))
                        .reason("")
                        .answerText("")
                        .build()
        );
        List<OrderItemResponseDto> items = getItems(detectorResults);
        OrderVerdict orderVerdict = buildVerdict(items)
                .withCheckResults(Set.of(
                        new AntifraudCheckResult(AntifraudAction.ORDER_ITEM_CHANGE, "", ""),
                        new AntifraudCheckResult(AntifraudAction.PREPAID_ONLY, AUTO_LIMIT_PREPAY_ANSWER, "")));

        OrderVerdict verdict = new CountMerger().process(buildRequest(), detectorResults, orderVerdict);

        assertNotNull(verdict.getCheckResults());
        Set<AntifraudAction> verdictActions =
                verdict.getCheckResults().stream().map(AntifraudCheckResult::getAntifraudAction).collect(Collectors.toSet());
        assertThat(verdictActions).doesNotContain(AntifraudAction.PREPAID_ONLY);
        assertThat(verdictActions).contains(AntifraudAction.ORDER_ITEM_CHANGE);
        assertNotNull(verdict.getFixedOrder());
        assertThat(verdict.getFixedOrder().getItems()).contains(
                OrderItemResponseDto.builder().id(1L).count(2).changes(Set.of(OrderItemChange.COUNT, OrderItemChange.FRAUD_FIXED)).build()
        );
    }

    @Test
    public void mergeAutoLimitPrepayManyItemsAndPgRuleFix() {
        List<OrderDetectorResult> detectorResults = List.of(
                OrderDetectorResult.builder()
                        .ruleName(new ItemAutoLimitDetector(null, null, null).getUniqName())
                        .fixedOrder(new OrderResponseDto(List.of()))
                        .fixedOrderForPrepay(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(5).changes(Set.of(OrderItemChange.COUNT)).build(),
                                OrderItemResponseDto.builder().id(2L).count(5).changes(Set.of(OrderItemChange.COUNT)).build()
                        )))
                        .actions(Set.of(AntifraudAction.PREPAID_ONLY))
                        .build(),
                OrderDetectorResult.builder()
                        .ruleName(new PgItemLimitRuleDetector(null).getUniqName())
                        .fixedOrder(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(2).changes(Set.of(OrderItemChange.COUNT, OrderItemChange.FRAUD_FIXED)).build()
                        )))
                        .actions(Set.of(AntifraudAction.ORDER_ITEM_CHANGE))
                        .build()
        );
        List<OrderItemResponseDto> items = getItems(detectorResults);
        OrderVerdict orderVerdict = buildVerdict(items)
                .withCheckResults(Set.of(
                        new AntifraudCheckResult(AntifraudAction.ORDER_ITEM_CHANGE, "", ""),
                        new AntifraudCheckResult(AntifraudAction.PREPAID_ONLY, AUTO_LIMIT_PREPAY_ANSWER, "")));

        OrderVerdict verdict = new CountMerger().process(buildRequestWithManyItems(), detectorResults, orderVerdict);

        assertNotNull(verdict.getCheckResults());
        Set<AntifraudAction> verdictActions =
                verdict.getCheckResults().stream().map(AntifraudCheckResult::getAntifraudAction).collect(Collectors.toSet());
        assertThat(verdictActions).contains(AntifraudAction.PREPAID_ONLY);
        assertThat(verdictActions).contains(AntifraudAction.ORDER_ITEM_CHANGE);
        assertNotNull(verdict.getFixedOrder());
        assertThat(verdict.getFixedOrder().getItems()).contains(
                OrderItemResponseDto.builder().id(1L).count(2).changes(Set.of(OrderItemChange.COUNT, OrderItemChange.FRAUD_FIXED)).build()
        );
    }

    @Test
    public void mergeAutoLimitPrepayAndPgRuleOk() {
        List<OrderDetectorResult> detectorResults = List.of(
                OrderDetectorResult.builder()
                        .ruleName(new ItemAutoLimitDetector(null, null, null).getUniqName())
                        .fixedOrder(new OrderResponseDto(List.of()))
                        .fixedOrderForPrepay(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(1).changes(Set.of()).build()
                        )))
                        .actions(Set.of(AntifraudAction.PREPAID_ONLY))
                        .reason("")
                        .answerText("")
                        .build(),
                OrderDetectorResult.builder()
                        .ruleName(new PgItemLimitRuleDetector(null).getUniqName())
                        .fixedOrder(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(2).changes(Set.of()).build()
                        )))
                        .actions(Set.of(AntifraudAction.NO_ACTION))
                        .reason("")
                        .answerText("")
                        .build()
        );
        List<OrderItemResponseDto> items = getItems(detectorResults);
        OrderVerdict orderVerdict = buildVerdict(items)
                .withCheckResults(Set.of(new AntifraudCheckResult(AntifraudAction.PREPAID_ONLY, AUTO_LIMIT_PREPAY_ANSWER, "")));

        OrderVerdict verdict = new CountMerger().process(buildRequest(), detectorResults, orderVerdict);

        assertNotNull(verdict.getCheckResults());
        Set<AntifraudAction> verdictActions =
                verdict.getCheckResults().stream().map(AntifraudCheckResult::getAntifraudAction).collect(Collectors.toSet());
        assertThat(verdictActions).doesNotContain(AntifraudAction.PREPAID_ONLY);
        assertThat(verdictActions).doesNotContain(AntifraudAction.ORDER_ITEM_CHANGE);
        assertNull(verdict.getFixedOrder());
    }

    @Test
    public void mergeAutoLimitPrepayManyItemsAndPgRuleOk() {
        List<OrderDetectorResult> detectorResults = List.of(
                OrderDetectorResult.builder()
                        .ruleName(new ItemAutoLimitDetector(null, null, null).getUniqName())
                        .fixedOrder(new OrderResponseDto(List.of()))
                        .fixedOrderForPrepay(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(1).changes(Set.of(OrderItemChange.COUNT)).build(),
                                OrderItemResponseDto.builder().id(2L).count(5).changes(Set.of(OrderItemChange.COUNT)).build()
                        )))
                        .actions(Set.of(AntifraudAction.PREPAID_ONLY))
                        .build(),
                OrderDetectorResult.builder()
                        .ruleName(new PgItemLimitRuleDetector(null).getUniqName())
                        .fixedOrder(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(2).changes(Set.of()).build()
                        )))
                        .actions(Set.of(AntifraudAction.NO_ACTION))
                        .build()
        );
        List<OrderItemResponseDto> items = getItems(detectorResults);
        OrderVerdict orderVerdict = buildVerdict(items)
                .withCheckResults(Set.of(new AntifraudCheckResult(AntifraudAction.PREPAID_ONLY, AUTO_LIMIT_PREPAY_ANSWER, "")));

        OrderVerdict verdict = new CountMerger().process(buildRequestWithManyItems(), detectorResults, orderVerdict);

        assertNotNull(verdict.getCheckResults());
        Set<AntifraudAction> verdictActions =
                verdict.getCheckResults().stream().map(AntifraudCheckResult::getAntifraudAction).collect(Collectors.toSet());
        assertThat(verdictActions).contains(AntifraudAction.PREPAID_ONLY);
        assertThat(verdictActions).doesNotContain(AntifraudAction.ORDER_ITEM_CHANGE);
        assertNull(verdict.getFixedOrder());
    }

    @Test
    public void mergeAutoLimitPrepayAndNoPgRule() {
        List<OrderDetectorResult> detectorResults = List.of(
                OrderDetectorResult.builder()
                        .ruleName(new ItemAutoLimitDetector(null, null, null).getUniqName())
                        .fixedOrder(new OrderResponseDto(List.of()))
                        .fixedOrderForPrepay(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(2).changes(Set.of()).build()
                        )))
                        .actions(Set.of(AntifraudAction.PREPAID_ONLY))
                        .build()
        );
        List<OrderItemResponseDto> items = getItems(detectorResults);
        OrderVerdict orderVerdict = buildVerdict(items)
                .withCheckResults(Set.of(new AntifraudCheckResult(AntifraudAction.PREPAID_ONLY, AUTO_LIMIT_PREPAY_ANSWER, "")));

        OrderVerdict verdict = new CountMerger().process(buildRequest(), detectorResults, orderVerdict);

        assertNotNull(verdict.getCheckResults());
        Set<AntifraudAction> verdictActions =
                verdict.getCheckResults().stream().map(AntifraudCheckResult::getAntifraudAction).collect(Collectors.toSet());
        assertThat(verdictActions).contains(AntifraudAction.PREPAID_ONLY);
        assertThat(verdictActions).doesNotContain(AntifraudAction.ORDER_ITEM_CHANGE);
        assertNull(verdict.getFixedOrder());
    }

    @Test
    public void noLimits() {
        List<OrderDetectorResult> detectorResults = List.of(
                OrderDetectorResult.builder()
                        .ruleName(new ItemAutoLimitDetector(null, null, null).getUniqName())
                        .actions(Set.of(AntifraudAction.NO_ACTION))
                        .build(),
                OrderDetectorResult.builder()
                        .ruleName(new PgItemLimitRuleDetector(null).getUniqName())
                        .fixedOrder(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(13).changes(Set.of()).build()
                        )))
                        .actions(Set.of(AntifraudAction.NO_ACTION))
                        .build()
        );
        List<OrderItemResponseDto> items = getItems(detectorResults);
        OrderVerdict orderVerdict = buildVerdict(items);

        OrderVerdict verdict = new CountMerger().process(buildRequest(), detectorResults, orderVerdict);

        OrderResponseDto merged = verdict.getFixedOrder();
        assertNull(merged);
        assertNotNull(verdict.getCheckResults());
        Set<AntifraudAction> verdictActions =
                verdict.getCheckResults().stream().map(AntifraudCheckResult::getAntifraudAction).collect(Collectors.toSet());
        assertThat(verdictActions).doesNotContain(AntifraudAction.ORDER_ITEM_CHANGE);
        assertThat(verdictActions).doesNotContain(AntifraudAction.PREPAID_ONLY);
    }

    @Test
    public void onlyPgFixes() {
        List<OrderDetectorResult> detectorResults = List.of(
                OrderDetectorResult.builder()
                        .ruleName(new PgItemLimitRuleDetector(null).getUniqName())
                        .fixedOrder(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(2).changes(Set.of(OrderItemChange.COUNT, OrderItemChange.FRAUD_FIXED)).build()
                        )))
                        .actions(Set.of(AntifraudAction.ORDER_ITEM_CHANGE))
                        .build()
        );
        List<OrderItemResponseDto> items = getItems(detectorResults);
        OrderVerdict orderVerdict = buildVerdict(items)
                .withCheckResults(Set.of(new AntifraudCheckResult(AntifraudAction.ORDER_ITEM_CHANGE, "", "")));

        OrderVerdict verdict = new CountMerger().process(buildRequest(), detectorResults, orderVerdict);

        OrderResponseDto merged = verdict.getFixedOrder();
        assertNotNull(merged);
        assertThat(merged.getItems()).containsExactly(
                OrderItemResponseDto.builder().id(1L).count(2).changes(Set.of(OrderItemChange.COUNT, OrderItemChange.FRAUD_FIXED)).build()
        );
        assertNotNull(verdict.getCheckResults());
        Set<AntifraudAction> verdictActions =
                verdict.getCheckResults().stream().map(AntifraudCheckResult::getAntifraudAction).collect(Collectors.toSet());
        assertThat(verdictActions).contains(AntifraudAction.ORDER_ITEM_CHANGE);
        assertThat(verdictActions).doesNotContain(AntifraudAction.PREPAID_ONLY);
    }

    @Test
    public void onlyPgOk() {
        List<OrderDetectorResult> detectorResults = List.of(
                OrderDetectorResult.builder()
                        .ruleName(new PgItemLimitRuleDetector(null).getUniqName())
                        .fixedOrder(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(2).changes(Set.of()).build()
                        )))
                        .actions(Set.of(AntifraudAction.NO_ACTION))
                        .build()
        );
        List<OrderItemResponseDto> items = getItems(detectorResults);
        OrderVerdict orderVerdict = buildVerdict(items);

        OrderVerdict verdict = new CountMerger().process(buildRequest(), detectorResults, orderVerdict);

        OrderResponseDto merged = verdict.getFixedOrder();
        assertNull(merged);
        assertNotNull(verdict.getCheckResults());
        Set<AntifraudAction> verdictActions =
                verdict.getCheckResults().stream().map(AntifraudCheckResult::getAntifraudAction).collect(Collectors.toSet());
        assertThat(verdictActions).doesNotContain(AntifraudAction.ORDER_ITEM_CHANGE);
        assertThat(verdictActions).doesNotContain(AntifraudAction.PREPAID_ONLY);
    }

    @Test
    public void mergeAutoLimitOkAndPgRuleFix() {
        List<OrderDetectorResult> detectorResults = List.of(
                OrderDetectorResult.builder()
                        .ruleName(new ItemAutoLimitDetector(null, null, null).getUniqName())
                        .actions(Set.of(AntifraudAction.NO_ACTION))
                        .build(),
                OrderDetectorResult.builder()
                        .ruleName(new PgItemLimitRuleDetector(null).getUniqName())
                        .fixedOrder(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(2).changes(Set.of(OrderItemChange.COUNT, OrderItemChange.FRAUD_FIXED)).build()
                        )))
                        .actions(Set.of(AntifraudAction.ORDER_ITEM_CHANGE))
                        .build()
        );
        List<OrderItemResponseDto> items = getItems(detectorResults);
        OrderVerdict orderVerdict = buildVerdict(items)
                .withCheckResults(Set.of(new AntifraudCheckResult(AntifraudAction.ORDER_ITEM_CHANGE, "", "")));

        OrderVerdict verdict = new CountMerger().process(buildRequest(), detectorResults, orderVerdict);

        assertNotNull(verdict.getCheckResults());
        Set<AntifraudAction> verdictActions =
                verdict.getCheckResults().stream().map(AntifraudCheckResult::getAntifraudAction).collect(Collectors.toSet());
        assertThat(verdictActions).doesNotContain(AntifraudAction.PREPAID_ONLY);
        assertThat(verdictActions).contains(AntifraudAction.ORDER_ITEM_CHANGE);
        assertNotNull(verdict.getFixedOrder());
        assertThat(verdict.getFixedOrder().getItems()).contains(
                OrderItemResponseDto.builder().id(1L).count(2).changes(Set.of(OrderItemChange.COUNT, OrderItemChange.FRAUD_FIXED)).build()
        );
    }

    @Test
    public void onlyAutoLimitOk() {
        List<OrderDetectorResult> detectorResults = List.of(
                OrderDetectorResult.builder()
                        .ruleName(new ItemAutoLimitDetector(null, null, null).getUniqName())
                        .actions(Set.of(AntifraudAction.NO_ACTION))
                        .build()
        );
        List<OrderItemResponseDto> items = getItems(detectorResults);
        OrderVerdict orderVerdict = buildVerdict(items);

        OrderVerdict verdict = new CountMerger().process(buildRequest(), detectorResults, orderVerdict);

        OrderResponseDto merged = verdict.getFixedOrder();
        assertNull(merged);
        assertNotNull(verdict.getCheckResults());
        Set<AntifraudAction> verdictActions =
                verdict.getCheckResults().stream().map(AntifraudCheckResult::getAntifraudAction).collect(Collectors.toSet());
        assertThat(verdictActions).doesNotContain(AntifraudAction.ORDER_ITEM_CHANGE);
        assertThat(verdictActions).doesNotContain(AntifraudAction.PREPAID_ONLY);
    }

    @Test
    public void onlyAutoLimitFixes() {
        OrderResponseDto fixedOrder = new OrderResponseDto(List.of(
                OrderItemResponseDto.builder().id(1L).count(1).changes(Set.of(OrderItemChange.COUNT)).build()
        ));
        List<OrderDetectorResult> detectorResults = List.of(
                OrderDetectorResult.builder()
                        .ruleName(new ItemAutoLimitDetector(null, null, null).getUniqName())
                        .fixedOrder(fixedOrder)
                        .fixedOrderForPrepay(fixedOrder)
                        .actions(Set.of(AntifraudAction.ORDER_ITEM_CHANGE))
                        .build()
        );
        List<OrderItemResponseDto> items = getItems(detectorResults);
        OrderVerdict orderVerdict = buildVerdict(items)
                .withCheckResults(Set.of(new AntifraudCheckResult(AntifraudAction.ORDER_ITEM_CHANGE, "", "")));

        OrderVerdict verdict = new CountMerger().process(buildRequest(), detectorResults, orderVerdict);

        OrderResponseDto merged = verdict.getFixedOrder();
        assertNotNull(merged);
        assertThat(merged.getItems()).containsExactly(
                OrderItemResponseDto.builder().id(1L).count(1).changes(Set.of(OrderItemChange.COUNT)).build()
        );
        assertNotNull(verdict.getCheckResults());
        Set<AntifraudAction> verdictActions =
                verdict.getCheckResults().stream().map(AntifraudCheckResult::getAntifraudAction).collect(Collectors.toSet());
        assertThat(verdictActions).contains(AntifraudAction.ORDER_ITEM_CHANGE);
        assertThat(verdictActions).doesNotContain(AntifraudAction.PREPAID_ONLY);
    }

    @Test
    public void onlyAutoBeyondLimitPrepay() {
        List<OrderDetectorResult> detectorResults = List.of(
                OrderDetectorResult.builder()
                        .ruleName(new ItemAutoLimitDetector(null, null, null).getUniqName())
                        .fixedOrder(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(10).changes(Set.of(OrderItemChange.COUNT)).build()
                        )))
                        .fixedOrderForPrepay(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(5).changes(Set.of(OrderItemChange.COUNT)).build()
                        )))
                        .actions(Set.of(AntifraudAction.ORDER_ITEM_CHANGE, AntifraudAction.PREPAID_ONLY))
                        .build()
        );
        List<OrderItemResponseDto> items = getItems(detectorResults);
        OrderVerdict orderVerdict = buildVerdict(items)
                .withCheckResults(Set.of(
                        new AntifraudCheckResult(AntifraudAction.ORDER_ITEM_CHANGE, ABOVE_MULTIPLIED_LIMIT_MESSAGE, ""),
                        new AntifraudCheckResult(AntifraudAction.PREPAID_ONLY, ABOVE_MULTIPLIED_LIMIT_MESSAGE, "")));

        OrderVerdict verdict = new CountMerger().process(buildRequest(), detectorResults, orderVerdict);

        OrderResponseDto merged = verdict.getFixedOrder();
        assertNotNull(merged);
        assertThat(merged.getItems()).containsExactly(
                OrderItemResponseDto.builder().id(1L).count(10).changes(Set.of(OrderItemChange.COUNT)).build()
        );
        assertNotNull(verdict.getCheckResults());
        Set<AntifraudAction> verdictActions =
                verdict.getCheckResults().stream().map(AntifraudCheckResult::getAntifraudAction).collect(Collectors.toSet());
        assertThat(verdictActions).contains(AntifraudAction.ORDER_ITEM_CHANGE);
        assertThat(verdictActions).contains(AntifraudAction.PREPAID_ONLY);
    }

    @Test
    public void mergeAutoBeyondLimitPrepayAndPgRuleFix() {
        List<OrderDetectorResult> detectorResults = List.of(
                OrderDetectorResult.builder()
                        .ruleName(new ItemAutoLimitDetector(null, null, null).getUniqName())
                        .fixedOrder(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(10).changes(Set.of(OrderItemChange.COUNT)).build()
                        )))
                        .fixedOrderForPrepay(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(5).changes(Set.of(OrderItemChange.COUNT)).build()
                        )))
                        .actions(Set.of(AntifraudAction.ORDER_ITEM_CHANGE, AntifraudAction.PREPAID_ONLY))
                        .reason("")
                        .answerText("")
                        .build(),
                OrderDetectorResult.builder()
                        .ruleName(new PgItemLimitRuleDetector(null).getUniqName())
                        .fixedOrder(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(8).changes(Set.of(OrderItemChange.COUNT, OrderItemChange.FRAUD_FIXED)).build()
                        )))
                        .actions(Set.of(AntifraudAction.ORDER_ITEM_CHANGE))
                        .reason("")
                        .answerText("")
                        .build()
        );
        List<OrderItemResponseDto> items = getItems(detectorResults);
        OrderVerdict orderVerdict = buildVerdict(items)
                .withCheckResults(Set.of(
                        new AntifraudCheckResult(AntifraudAction.ORDER_ITEM_CHANGE, ABOVE_MULTIPLIED_LIMIT_MESSAGE, ""),
                        new AntifraudCheckResult(AntifraudAction.PREPAID_ONLY, ABOVE_MULTIPLIED_LIMIT_MESSAGE, ""),
                        new AntifraudCheckResult(AntifraudAction.ORDER_ITEM_CHANGE, "", "")));

        OrderVerdict verdict = new CountMerger().process(buildRequest(), detectorResults, orderVerdict);

        OrderResponseDto merged = verdict.getFixedOrder();
        assertNotNull(merged);
        assertThat(merged.getItems()).containsExactly(
                OrderItemResponseDto.builder().id(1L).count(8).changes(Set.of(OrderItemChange.COUNT, OrderItemChange.FRAUD_FIXED)).build()
        );
        assertNotNull(verdict.getCheckResults());
        Set<AntifraudAction> verdictActions =
                verdict.getCheckResults().stream().map(AntifraudCheckResult::getAntifraudAction).collect(Collectors.toSet());
        assertThat(verdictActions).contains(AntifraudAction.ORDER_ITEM_CHANGE);
        assertThat(verdictActions).doesNotContain(AntifraudAction.PREPAID_ONLY);
    }

    @Test
    public void mergeAutoBeyondLimitPrepayManyItemsAndPgRuleFix() {
        List<OrderDetectorResult> detectorResults = List.of(
                OrderDetectorResult.builder()
                        .ruleName(new ItemAutoLimitDetector(null, null, null).getUniqName())
                        .fixedOrder(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(10).changes(Set.of(OrderItemChange.COUNT)).build()
                        )))
                        .fixedOrderForPrepay(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(5).changes(Set.of(OrderItemChange.COUNT)).build(),
                                OrderItemResponseDto.builder().id(2L).count(5).changes(Set.of(OrderItemChange.COUNT)).build()
                        )))
                        .actions(Set.of(AntifraudAction.ORDER_ITEM_CHANGE, AntifraudAction.PREPAID_ONLY))
                        .reason("")
                        .answerText("")
                        .build(),
                OrderDetectorResult.builder()
                        .ruleName(new PgItemLimitRuleDetector(null).getUniqName())
                        .fixedOrder(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(8).changes(Set.of(OrderItemChange.COUNT, OrderItemChange.FRAUD_FIXED)).build()
                        )))
                        .actions(Set.of(AntifraudAction.ORDER_ITEM_CHANGE))
                        .reason("")
                        .answerText("")
                        .build()
        );
        List<OrderItemResponseDto> items = getItems(detectorResults);
        OrderVerdict orderVerdict = buildVerdict(items)
                .withCheckResults(Set.of(
                        new AntifraudCheckResult(AntifraudAction.ORDER_ITEM_CHANGE, ABOVE_MULTIPLIED_LIMIT_MESSAGE, ""),
                        new AntifraudCheckResult(AntifraudAction.PREPAID_ONLY, ABOVE_MULTIPLIED_LIMIT_MESSAGE, ""),
                        new AntifraudCheckResult(AntifraudAction.ORDER_ITEM_CHANGE, "", "")));

        OrderVerdict verdict = new CountMerger().process(buildRequestWithManyItems(), detectorResults, orderVerdict);

        OrderResponseDto merged = verdict.getFixedOrder();
        assertNotNull(merged);
        assertThat(merged.getItems()).containsExactly(
                OrderItemResponseDto.builder().id(1L).count(8).changes(Set.of(OrderItemChange.COUNT, OrderItemChange.FRAUD_FIXED)).build()
        );
        assertNotNull(verdict.getCheckResults());
        Set<AntifraudAction> verdictActions =
                verdict.getCheckResults().stream().map(AntifraudCheckResult::getAntifraudAction).collect(Collectors.toSet());
        assertThat(verdictActions).contains(AntifraudAction.ORDER_ITEM_CHANGE);
        assertThat(verdictActions).contains(AntifraudAction.PREPAID_ONLY);
    }

    @Test
    public void mergeAutoBeyondLimitPrepayAndPgRuleOk() {
        List<OrderDetectorResult> detectorResults = List.of(
                OrderDetectorResult.builder()
                        .ruleName(new ItemAutoLimitDetector(null, null, null).getUniqName())
                        .fixedOrder(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(10).changes(Set.of(OrderItemChange.COUNT)).build()
                        )))
                        .fixedOrderForPrepay(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(5).changes(Set.of(OrderItemChange.COUNT)).build()
                        )))
                        .actions(Set.of(AntifraudAction.ORDER_ITEM_CHANGE, AntifraudAction.PREPAID_ONLY))
                        .reason("")
                        .answerText("")
                        .build(),
                OrderDetectorResult.builder()
                        .ruleName(new PgItemLimitRuleDetector(null).getUniqName())
                        .fixedOrder(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(15).changes(Set.of()).build()
                        )))
                        .actions(Set.of(AntifraudAction.NO_ACTION))
                        .reason("")
                        .answerText("")
                        .build()
        );
        List<OrderItemResponseDto> items = getItems(detectorResults);
        OrderVerdict orderVerdict = buildVerdict(items)
                .withCheckResults(Set.of(
                        new AntifraudCheckResult(AntifraudAction.ORDER_ITEM_CHANGE, ABOVE_MULTIPLIED_LIMIT_MESSAGE, ""),
                        new AntifraudCheckResult(AntifraudAction.PREPAID_ONLY, ABOVE_MULTIPLIED_LIMIT_MESSAGE, "")));

        OrderVerdict verdict = new CountMerger().process(buildRequest(), detectorResults, orderVerdict);

        OrderResponseDto merged = verdict.getFixedOrder();
        assertNull(merged);
        assertNotNull(verdict.getCheckResults());
        Set<AntifraudAction> verdictActions =
                verdict.getCheckResults().stream().map(AntifraudCheckResult::getAntifraudAction).collect(Collectors.toSet());
        assertThat(verdictActions).doesNotContain(AntifraudAction.ORDER_ITEM_CHANGE);
        assertThat(verdictActions).doesNotContain(AntifraudAction.PREPAID_ONLY);
    }

    @Test
    public void mergeAutoBeyondLimitPrepayManyItemsAndPgRuleOk() {
        List<OrderDetectorResult> detectorResults = List.of(
                OrderDetectorResult.builder()
                        .ruleName(new ItemAutoLimitDetector(null, null, null).getUniqName())
                        .fixedOrder(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(10).changes(Set.of(OrderItemChange.COUNT)).build()
                        )))
                        .fixedOrderForPrepay(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(5).changes(Set.of(OrderItemChange.COUNT)).build(),
                                OrderItemResponseDto.builder().id(2L).count(5).changes(Set.of(OrderItemChange.COUNT)).build()
                        )))
                        .actions(Set.of(AntifraudAction.ORDER_ITEM_CHANGE, AntifraudAction.PREPAID_ONLY))
                        .reason("")
                        .answerText("")
                        .build(),
                OrderDetectorResult.builder()
                        .ruleName(new PgItemLimitRuleDetector(null).getUniqName())
                        .fixedOrder(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).count(15).changes(Set.of()).build()
                        )))
                        .actions(Set.of(AntifraudAction.NO_ACTION))
                        .reason("")
                        .answerText("")
                        .build()
        );
        List<OrderItemResponseDto> items = getItems(detectorResults);
        OrderVerdict orderVerdict = buildVerdict(items)
                .withCheckResults(Set.of(
                        new AntifraudCheckResult(AntifraudAction.ORDER_ITEM_CHANGE, ABOVE_MULTIPLIED_LIMIT_MESSAGE, ""),
                        new AntifraudCheckResult(AntifraudAction.PREPAID_ONLY, ABOVE_MULTIPLIED_LIMIT_MESSAGE, "")));

        OrderVerdict verdict = new CountMerger().process(buildRequestWithManyItems(), detectorResults, orderVerdict);

        OrderResponseDto merged = verdict.getFixedOrder();
        assertNull(merged);
        assertNotNull(verdict.getCheckResults());
        Set<AntifraudAction> verdictActions =
                verdict.getCheckResults().stream().map(AntifraudCheckResult::getAntifraudAction).collect(Collectors.toSet());
        assertThat(verdictActions).doesNotContain(AntifraudAction.ORDER_ITEM_CHANGE);
        assertThat(verdictActions).contains(AntifraudAction.PREPAID_ONLY);
    }

    private List<OrderItemResponseDto> getItems(List<OrderDetectorResult> detectorResults) {
        return detectorResults.stream()
                .map(OrderDetectorResult::getFixedOrder)
                .filter(Objects::nonNull)
                .map(OrderResponseDto::getItems)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private OrderVerdict buildVerdict(List<OrderItemResponseDto> items) {
        return OrderVerdict.builder()
                .checkResults(Set.of())
                .fixedOrder(new OrderResponseDto(items))
                .isDegradation(false)
                .build();
    }

    private MultiCartRequestDto buildRequest() {
        return MultiCartRequestDto.builder()
            .carts(List.of(
                CartRequestDto.builder()
                .items(List.of(OrderItemRequestDto.builder().id(1L).build()))
                .build()))
            .build();
    }

    private MultiCartRequestDto buildRequestWithManyItems() {
        return MultiCartRequestDto.builder()
            .carts(List.of(
                CartRequestDto.builder()
                    .items(List.of(OrderItemRequestDto.builder().id(1L).build(), OrderItemRequestDto.builder().id(2L).build()))
                    .build()))
            .build();
    }
}
