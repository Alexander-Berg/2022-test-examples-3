package ru.yandex.market.antifraud.orders.storage.dao;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.antifraud.orders.entity.Msku;
import ru.yandex.market.antifraud.orders.storage.entity.limits.LimitRuleType;
import ru.yandex.market.antifraud.orders.storage.entity.limits.PgItemLimitRule;
import ru.yandex.market.antifraud.orders.storage.entity.stat.ItemParams;
import ru.yandex.market.antifraud.orders.test.annotations.DaoLayerTest;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemRequestDto;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author dzvyagin
 */
@DaoLayerTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ItemLimitRulesDaoTest {


    @Autowired
    private NamedParameterJdbcOperations jdbcTemplate;

    private ItemLimitRulesDao itemLimitRulesDao;

    @Before
    public void init() {
        itemLimitRulesDao = new ItemLimitRulesDao(jdbcTemplate);
    }


    @Test
    public void findRulesForItems() {
        PgItemLimitRule rule1 = PgItemLimitRule.builder()
                .msku(123L)
                .maxCount(2L)
                .ruleType(LimitRuleType.ORDER)
                .build();
        PgItemLimitRule rule2 = PgItemLimitRule.builder()
                .msku(124L)
                .maxCount(2L)
                .ruleType(LimitRuleType.ORDER)
                .build();
        rule1 = itemLimitRulesDao.saveRule(rule1);
        rule2 = itemLimitRulesDao.saveRule(rule2);

        List<PgItemLimitRule> rules = itemLimitRulesDao.findRulesForItems(List.of(
            ItemParams.builder().msku(new Msku(123L)).modelId(1L).categoryId(2).build()
        ), LocalDate.now());
        assertThat(rules).contains(rule1);
        assertThat(rules).doesNotContain(rule2);
    }

    @Test
    public void findRulesForDsbsItems() {
        PgItemLimitRule rule1 = PgItemLimitRule.builder()
                .modelId(159L)
                .maxCount(2L)
                .ruleType(LimitRuleType.ORDER)
                .build();

        rule1 = itemLimitRulesDao.saveRule(rule1);

        List<PgItemLimitRule> rules = itemLimitRulesDao.findRulesForItems(
            Set.of(
                ItemParams.builder().modelId(159L).categoryId(14).build()
            ),
            LocalDate.now()
        );
        assertThat(rules).contains(rule1);
    }

    @Test
    public void findRulesWithinPeriod() {
        PgItemLimitRule rule1 = PgItemLimitRule.builder()
            .msku(125L)
            .maxCount(2L)
            .ruleType(LimitRuleType.ORDER)
            .periodFrom(Date.valueOf("2022-06-01"))
            .periodTo(Date.valueOf("2022-06-02"))
            .build();
        PgItemLimitRule rule2 = PgItemLimitRule.builder()
            .msku(125L)
            .maxCount(2L)
            .ruleType(LimitRuleType.ORDER)
            .periodFrom(Date.valueOf("2022-06-03"))
            .periodTo(Date.valueOf("2022-06-10"))
            .build();
        rule1 = itemLimitRulesDao.saveRule(rule1);
        rule2 = itemLimitRulesDao.saveRule(rule2);

        List<PgItemLimitRule> rules = itemLimitRulesDao.findRulesForItems(List.of(
            ItemParams.builder().msku(new Msku(125L)).modelId(1L).categoryId(2).build()
        ), LocalDate.of(2022,6,4));
        assertThat(rules)
            .contains(rule2)
            .doesNotContain(rule1);
    }

    @Test
    public void insertRules() {
        PgItemLimitRule rule1 = PgItemLimitRule.builder()
                .msku(128L)
                .maxCount(2L)
                .ruleType(LimitRuleType.ORDER)
                .build();
        PgItemLimitRule rule2 = PgItemLimitRule.builder()
                .msku(129L)
                .maxCount(2L)
                .ruleType(LimitRuleType.ORDER)
                .build();
        rule1 = itemLimitRulesDao.saveRule(rule1);
        rule2 = itemLimitRulesDao.saveRule(rule2);
        List<PgItemLimitRule> allRules = itemLimitRulesDao.getAllRules();
        assertThat(allRules).contains(rule1, rule2);
        PgItemLimitRule rule3 = PgItemLimitRule.builder()
                .modelId(18L)
                .maxCount(2L)
                .ruleType(LimitRuleType.ORDER)
                .build();
        PgItemLimitRule rule4 = PgItemLimitRule.builder()
                .categoryId(19)
                .maxCount(2L)
                .ruleType(LimitRuleType.ORDER)
                .build();
        itemLimitRulesDao.insertRules(List.of(rule3, rule4));
        rule3 = rule3.withId(rule2.getId() + 1);
        rule4 = rule4.withId(rule2.getId() + 2);
        List<PgItemLimitRule> allRules2 = itemLimitRulesDao.getAllRules();
        assertThat(allRules2).contains(rule1, rule2);
        assertThat(allRules2).contains(rule3, rule4);
    }
}
