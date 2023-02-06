package ru.yandex.market.antifraud.orders.storage.entity.limits;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.joda.time.LocalDate;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import ru.yandex.market.antifraud.orders.entity.AntifraudItemLimitRule;
import ru.yandex.market.antifraud.orders.entity.ItemLimitRule;
import ru.yandex.market.antifraud.orders.entity.ItemLimitRuleWithHistory;
import ru.yandex.market.antifraud.orders.entity.Msku;
import ru.yandex.market.antifraud.orders.storage.entity.stat.ItemParams;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.Order;
import ru.yandex.market.crm.platform.models.OrderItem;

import static org.assertj.core.api.Assertions.anyOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author dzvyagin
 */
@RunWith(Theories.class)
public class LimitRuleConverterTest {

    @Test
    public void convertToVanillaRule() {
        PgItemLimitRule rule = PgItemLimitRule.builder()
            .categoryId(123)
            .maxCount(12L)
            .maxPrice(BigDecimal.valueOf(999L))
            .periodDays(7)
            .ruleType(LimitRuleType.GLUE)
            .tag("test-test")
            .build();
        AntifraudItemLimitRule vanillaRule = new LimitRuleConverter().convertToVanillaRule(rule);
        assertThat(vanillaRule.getCategoryId()).isEqualTo(123);
        assertThat(vanillaRule.getMaxAmountPerUser()).isEqualTo(999L);
        assertThat(vanillaRule.getMaxCountPerUser()).isEqualTo(12L);
        assertThat(vanillaRule.getHistoryPeriod()).isEqualTo(7);
        assertThat(vanillaRule.getTag()).isEqualTo("test-test");
    }

    @Test
    public void convertToPgRule() {
        AntifraudItemLimitRule vanillaRule = AntifraudItemLimitRule.builder()
            .msku(123L)
            .maxCountPerUser(3L)
            .maxCountPerOrder(1L)
            .tag("test-test-test")
            .historyPeriod(8)
            .build();
        List<PgItemLimitRule> pgRules = new LimitRuleConverter().convertToPgRule(vanillaRule);
        assertThat(pgRules).hasSize(2);
        PgItemLimitRule rule1 = PgItemLimitRule.builder()
            .msku(123L)
            .maxCount(1L)
            .ruleType(LimitRuleType.ORDER)
            .periodDays(8)
            .tag("test-test-test")
            .build();
        PgItemLimitRule rule2 = PgItemLimitRule.builder()
            .msku(123L)
            .maxCount(3L)
            .ruleType(LimitRuleType.GLUE)
            .periodDays(8)
            .tag("test-test-test")
            .build();
        assertThat(pgRules).contains(rule1, rule2);
    }

    @DataPoint("item")
    public static ItemParams itemParams = ItemParams.builder()
        .categoryId(123)
        .modelId(13345L)
        .msku(new Msku(145L))
        .build();

    @DataPoints
    public static Instant[] times = new Instant[]{
        Instant.parse("2022-06-20T00:00:00Z"),
        Instant.parse("2022-06-25T00:00:00Z"),
        Instant.parse("2022-06-30T00:00:00Z"),
    };

    @DataPoints("uid")
    public static Long[] uids = new Long[]{998765L, 98766L};

    @DataPoints
    public static PgItemLimitRule[] rules = new PgItemLimitRule[]{
        PgItemLimitRule.builder()
            .categoryId(123)
            .maxCount(12L)
            .ruleType(LimitRuleType.ORDER)
            .build(),
        PgItemLimitRule.builder()
            .modelId(13345L)
            .maxCount(14L)
            .ruleType(LimitRuleType.USER)
            .periodDays(7)
            .build(),
        PgItemLimitRule.builder()
            .msku(145L)
            .maxPrice(BigDecimal.valueOf(1000))
            .ruleType(LimitRuleType.GLUE)
            .periodFrom(Date.valueOf("2022-06-15"))
            .build(),
        PgItemLimitRule.builder()
            .categoryId(124)
            .maxPrice(BigDecimal.valueOf(1200))
            .ruleType(LimitRuleType.GLUE)
            .periodDays(7)
            .periodFrom(Date.valueOf("2022-06-15"))
            .build(),
    };

    @DataPoints
    public static Order[] historyOrders = new Order[]{
        Order.newBuilder()
            .setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(998765L))
            .setCreationDate(Instant.parse("2022-06-12T00:00:00Z").toEpochMilli())
            .build(),
        Order.newBuilder()
            .setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(998766L))
            .setCreationDate(Instant.parse("2022-06-14T00:00:00Z").toEpochMilli())
            .build(),
        Order.newBuilder()
            .setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(998765L))
            .setCreationDate(Instant.parse("2022-06-16T00:00:00Z").toEpochMilli())
            .build(),
        Order.newBuilder()
            .setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(998766L))
            .setCreationDate(Instant.parse("2022-06-18T00:00:00Z").toEpochMilli())
            .build(),
    };

    @Theory
    public void convertToBusinessObject_appliesTo(PgItemLimitRule pgRule,
                                                  Instant currentTime,
                                                  @FromDataPoints("uid") Long buyerUid,
                                                  ItemParams itemParams) {
        assumeThat(pgRule.toItemParams().matches(itemParams))
            .isTrue();
        var rule = new LimitRuleConverter()
            .convertToBusinessObject(pgRule, currentTime, buyerUid);
        assertThat(rule.appliesTo(itemParams))
            .isTrue();
    }

    @Theory
    public void convertToBusinessObject_notAppliesTo(PgItemLimitRule pgRule,
                                                     Instant currentTime,
                                                     @FromDataPoints("uid") Long buyerUid,
                                                     ItemParams itemParams) {
        assumeThat(pgRule.toItemParams().matches(itemParams))
            .isFalse();
        var rule = new LimitRuleConverter()
            .convertToBusinessObject(pgRule, currentTime, buyerUid);
        assertThat(rule.appliesTo(itemParams))
            .isFalse();
    }

    @Theory
    public void convertToBusinessObject_noMaxCount(PgItemLimitRule pgRule,
                                                   Instant currentTime,
                                                   @FromDataPoints("uid") Long buyerUid) {
        assumeThat(pgRule.getMaxCount())
            .isNull();
        var rule = new LimitRuleConverter()
            .convertToBusinessObject(pgRule, currentTime, buyerUid);
        assertThat(rule.getMaxCount())
            .isEqualTo(Integer.MAX_VALUE);
    }

    @Theory
    public void convertToBusinessObject_noMaxAmount(PgItemLimitRule pgRule,
                                                    Instant currentTime,
                                                    @FromDataPoints("uid") Long buyerUid) {
        assumeThat(pgRule.getMaxPrice())
            .isNull();
        var rule = new LimitRuleConverter()
            .convertToBusinessObject(pgRule, currentTime, buyerUid);
        assertThat(rule.getMaxAmount())
            .isEqualTo(BigDecimal.valueOf(Long.MAX_VALUE));
    }

    @Theory
    public void convertToBusinessObject_maxCount(PgItemLimitRule pgRule,
                                                 Instant currentTime,
                                                 @FromDataPoints("uid") Long buyerUid) {
        assumeThat(pgRule.getMaxCount())
            .isNotNull();
        var rule = new LimitRuleConverter()
            .convertToBusinessObject(pgRule, currentTime, buyerUid);
        assertThat(rule.getMaxCount())
            .isEqualTo(pgRule.getMaxCount().intValue());
    }

    @Theory
    public void convertToBusinessObject_maxAmount(PgItemLimitRule pgRule,
                                                  Instant currentTime,
                                                  @FromDataPoints("uid") Long buyerUid) {
        assumeThat(pgRule.getMaxPrice())
            .isNotNull();
        var rule = new LimitRuleConverter()
            .convertToBusinessObject(pgRule, currentTime, buyerUid);
        assertThat(rule.getMaxAmount())
            .isEqualTo(pgRule.getMaxPrice());
    }

    @Theory
    public void convertToBusinessObject_forOrder(PgItemLimitRule pgRule,
                                                 Instant currentTime,
                                                 @FromDataPoints("uid") Long buyerUid,
                                                 Order historyOrder) {
        assumeThat(pgRule.getRuleType())
            .isEqualTo(LimitRuleType.ORDER);
        var rule = new LimitRuleConverter()
            .convertToBusinessObject(pgRule, currentTime, buyerUid);
        assertThat(rule.considers(historyOrder))
            .isFalse();
    }

    @Theory
    public void convertToBusinessObject_anotherUser(PgItemLimitRule pgRule,
                                                    Instant currentTime,
                                                    @FromDataPoints("uid") Long buyerUid,
                                                    Order historyOrder) {
        assumeThat(pgRule.getRuleType())
            .isEqualTo(LimitRuleType.USER);
        assumeThat(historyOrder.getKeyUid().getIntValue())
            .isNotEqualTo(buyerUid);
        var rule = new LimitRuleConverter()
            .convertToBusinessObject(pgRule, currentTime, buyerUid);
        assertThat(rule.considers(historyOrder))
            .isFalse();
    }

    @Theory
    public void convertToBusinessObject_checkPeriodDays(PgItemLimitRule pgRule,
                                                        Instant currentTime,
                                                        @FromDataPoints("uid") Long buyerUid,
                                                        Order historyOrder) {
        assumeThat(pgRule.getPeriodDays())
            .isNotNull();
        assumeThat(Instant.ofEpochMilli(historyOrder.getCreationDate()))
            .isBefore(currentTime.minus(pgRule.getPeriodDays(), ChronoUnit.DAYS));
        var rule = new LimitRuleConverter()
            .convertToBusinessObject(pgRule, currentTime, buyerUid);
        assertThat(rule.considers(historyOrder))
            .isFalse();
    }

    @Theory
    public void convertToBusinessObject_checkPeriodFrom(PgItemLimitRule pgRule,
                                                        Instant currentTime,
                                                        @FromDataPoints("uid") Long buyerUid,
                                                        Order historyOrder) {
        assumeThat(pgRule.getPeriodFrom())
            .isNotNull();
        assumeThat(Instant.ofEpochMilli(historyOrder.getCreationDate()).atZone(ZoneId.systemDefault()).toLocalDate())
            .isBefore(pgRule.getPeriodFrom().toLocalDate());
        var rule = new LimitRuleConverter()
            .convertToBusinessObject(pgRule, currentTime, buyerUid);
        assertThat(rule.considers(historyOrder))
            .isFalse();
    }

    @Theory
    public void convertToBusinessObject_checksPassed(PgItemLimitRule pgRule,
                                                     Instant currentTime,
                                                     @FromDataPoints("uid") Long buyerUid,
                                                     Order historyOrder) {
        assumeThat(pgRule.getRuleType())
            .matches(x -> x == LimitRuleType.GLUE ||
                (x == LimitRuleType.USER && historyOrder.getKeyUid().getIntValue() == buyerUid));
        assumeThat(pgRule.getPeriodFrom())
            .matches(x -> x == null ||
                x.before(java.util.Date.from(Instant.ofEpochMilli(historyOrder.getCreationDate()))));
        assumeThat(pgRule.getPeriodDays())
            .matches(x -> x == null ||
                Instant.ofEpochMilli(historyOrder.getCreationDate()).plus(x, ChronoUnit.DAYS).isAfter(currentTime));
        var rule = new LimitRuleConverter()
            .convertToBusinessObject(pgRule, currentTime, buyerUid);
        assertThat(rule.considers(historyOrder))
            .isTrue();
    }

    @Test
    public void applyHistory() {
        var ruleWithHistory = new ItemLimitRuleWithHistory() {
            @Override
            public boolean appliesTo(ItemParams requestItem) {
                return requestItem.getModelId() == 1L;
            }

            @Override
            public boolean considers(Order order) {
                return order.getId() == 1;
            }

            @Override
            public int getMaxCount() {
                return 15;
            }

            @Override
            public BigDecimal getMaxAmount() {
                return BigDecimal.valueOf(1000);
            }
        };
        var historyOrders = List.of(
            Order.newBuilder()
                .setId(1)
                .addItems(
                    OrderItem.newBuilder()
                        .setModelId(1)
                        .setCount(1)
                        .setPrice(100_00))
                .addItems(
                    OrderItem.newBuilder()
                        .setModelId(2)
                        .setCount(2)
                        .setPrice(100_00))
                .build(),
            Order.newBuilder()
                .setId(2)
                .addItems(
                    OrderItem.newBuilder()
                        .setModelId(1)
                        .setCount(4)
                        .setPrice(100_00))
                .addItems(
                    OrderItem.newBuilder()
                        .setModelId(2)
                        .setCount(8)
                        .setPrice(100_00))
                .build()
        );
        var rule = new LimitRuleConverter().applyHistory(ruleWithHistory, historyOrders);
        assertThat(rule.getMaxCount())
            .isEqualTo(14);
        assertThat(rule.getMaxAmount())
            .isCloseTo(BigDecimal.valueOf(900), withinPercentage(1e-4));
    }
}