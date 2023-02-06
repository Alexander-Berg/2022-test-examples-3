package ru.yandex.market.core.delivery.tariff.checker;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.delivery.tariff.error.PartnerExceptionWrapper;
import ru.yandex.market.core.delivery.tariff.model.CategoryId;
import ru.yandex.market.core.delivery.tariff.model.CategoryRule;
import ru.yandex.market.core.delivery.tariff.model.DeliveryOption;
import ru.yandex.market.core.delivery.tariff.model.DeliveryRule;
import ru.yandex.market.core.delivery.tariff.model.DeliveryRuleId;
import ru.yandex.market.core.delivery.tariff.model.DeliveryTariff;
import ru.yandex.market.core.delivery.tariff.model.OptionGroup;
import ru.yandex.market.core.delivery.tariff.model.PriceRule;
import ru.yandex.market.core.delivery.tariff.model.TariffType;
import ru.yandex.market.core.delivery.tariff.model.WeightRule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.common.util.collections.CollectionFactory.list;
import static ru.yandex.common.util.collections.CollectionFactory.newTreeSet;
import static ru.yandex.common.util.collections.CollectionFactory.set;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class DeliveryTariffCheckerTest {

    @Test
    void checkYml() {
        DeliveryTariffChecker checker = new DeliveryTariffChecker(new DeliveryTariff(
                1L, TariffType.DEFAULT, null, true, null, null, null, null));
        checker.check();
        assertEquals(0, checker.getErrors().size());
    }

    @Test
    void checkDefault() {
        DeliveryTariffChecker checker = new DeliveryTariffChecker(new DeliveryTariff(
                1L, TariffType.UNIFORM, null, false, null, null, null, Collections.singletonList(new OptionGroup(
                set(new DeliveryOption(0, (short) 0, BigDecimal.ZERO, (short) 0, (short) 0, (byte) 12))))));
        checker.check();
        assertEquals(0, checker.getErrors().size());
    }

    @Test
    void checkWeightRules() {
        assertEquals(0, check(null, checker -> checker.checkWeightRules(new WeightRule[]{
                new WeightRule(new DeliveryRuleId(1, 0), null, 1),
                new WeightRule(new DeliveryRuleId(1, 1), 1, 2),
                new WeightRule(new DeliveryRuleId(1, 2), 2, null),
        })).size());

        assertEquals(1, check(null, checker -> checker.checkWeightRules(new WeightRule[]{
                new WeightRule(new DeliveryRuleId(1, 0), null, 1),
                new WeightRule(new DeliveryRuleId(1, 1), 2, null),
        })).size());

        assertEquals(1, check(null, checker -> checker.checkWeightRules(new WeightRule[]{
                new WeightRule(new DeliveryRuleId(1, 0), null, 1),
                new WeightRule(new DeliveryRuleId(1, 1), 1, 2),
        })).size());

        assertEquals(1, check(null, checker -> checker.checkWeightRules(new WeightRule[]{
                new WeightRule(new DeliveryRuleId(1, 0), null, 1),
                new WeightRule(new DeliveryRuleId(1, 1), 1, 2),
        })).size());
    }

    @Test
    void checkCategoryRules() {
        assertEquals(list(), check(null, checker -> checker.checkCategoryRules(new CategoryRule[]{
                new CategoryRule(new DeliveryRuleId(1, 0), newTreeSet(set(new CategoryId("1", 1)))),
                new CategoryRule(new DeliveryRuleId(1, 0), true),
        })));
    }

    @Test
    void checkCategoryRulesWrong() {
        assertEquals(2, check(null, checker -> {
            CategoryRule rule1 = new CategoryRule(new DeliveryRuleId(1, 0), newTreeSet(set(new CategoryId("1", 1))));
            rule1.setOthers(true);
            CategoryRule rule2 = new CategoryRule(new DeliveryRuleId(1, 0), false);
            checker.checkCategoryRules(new CategoryRule[]{rule1, rule2});
        }).size());
    }

    @Test
    void checkRulesOrder() {
        assertEquals(0, check(null, checker -> checker.checkRulesOrder(new DeliveryRule[]{
                new PriceRule(new DeliveryRuleId(1, 0), null, null),
                new PriceRule(new DeliveryRuleId(1, 1), null, null),
                new PriceRule(new DeliveryRuleId(1, 2), null, null),
                new PriceRule(new DeliveryRuleId(1, 3), null, null),
                new PriceRule(new DeliveryRuleId(1, 4), null, null),
        })).size());

        assertEquals(0, check(null, checker -> checker.checkRulesOrder(new DeliveryRule[]{
        })).size());

        assertEquals(1, check(null, checker -> checker.checkRulesOrder(new DeliveryRule[]{
                new PriceRule(new DeliveryRuleId(1, 4), null, null),
        })).size());

        assertEquals(2, check(null, checker -> checker.checkRulesOrder(new DeliveryRule[]{
                new PriceRule(new DeliveryRuleId(1, 4), null, null),
                new PriceRule(new DeliveryRuleId(1, 4), null, null),
                new PriceRule(new DeliveryRuleId(1, 2), null, null),
        })).size());
    }

    // раньше checkOption падал с NPE, если час перескока не был указан
    @Test
    void checkNullOrderBeforeHour() {
        check(null, checker -> checker.checkOption(
                new DeliveryOption(0, (short) 0, BigDecimal.ZERO, (short) 0, (short) 0, null))).size();
    }

    // раньше checkIntervalRules падал с NPE, если левая граница первого правила была равна null
    @Test
    void checkNullFromPrice() {
        PriceRule[] priceRules = new PriceRule[1];
        priceRules[0] = new PriceRule(new DeliveryRuleId(1, 1), null, null);
        assertEquals(0, check(null, checker -> checker.checkIntervalRules(priceRules,
                PriceRule::getPriceFrom, PriceRule::getPriceFrom)).size());

    }

    private List<PartnerExceptionWrapper> check(DeliveryTariff tariff, Consumer<DeliveryTariffChecker> test) {
        DeliveryTariffChecker checker = new DeliveryTariffChecker(tariff);
        test.accept(checker);
        return checker.getErrors();
    }

}