package ru.yandex.market.mboc.common.offers.acceptance.rule;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.BIZ_ID_SUPPLIER;

public class CategoryRuleServiceTest extends BaseDbTestClass {
    @Autowired
    private CategoryRuleRepository categoryRuleRepository;

    private CategoryRuleService service;

    @Before
    public void setUp() {
        // Enable by default
        storageKeyValueService.invalidateCache();
        storageKeyValueService.putValue(CategoryRuleConfig.ACCEPTANCE_WITH_CATEGORY_RULES_FEATURE, true);
        service = new CategoryRuleService(storageKeyValueService, categoryRuleRepository);
    }

    /**
     * Задано правило: Бизнес "Рога и Копыта", категория "Мобильные телефоны", вендор Apple
     * Других правил никаких не задано.
     * В этом случае "Рога и копыта" может торговать вообще всем чем угодно, просто продукцией Apple в категории
     * "Мобильные телефоны" кроме него никто не торгует. При этом как он сам, так и другие бизнесы могут продавать
     * любые другие бренды в категории "Мобильные телефоны".
     */
    @Test
    public void singleRule() {
        long biz = BIZ_ID_SUPPLIER;
        long otherBiz = BIZ_ID_SUPPLIER + 1;
        long category = 333333;
        long otherCategory = 333334;
        int vendor = 123;
        int otherVendor = 124;

        initRules(Set.of(
            new CategoryRule(category, biz, vendor)
        ));

        // Business is allowed to sell the vendor in category
        assertThat(service.isAutoAcceptanceAllowed(category, biz, vendor)).hasValue(true);
        // Business is allowed to sell other vendors in category
        assertThat(service.isAutoAcceptanceAllowed(category, biz, otherVendor)).hasValue(true);
        // Other businesses can't sell the vendor in category
        assertThat(service.isAutoAcceptanceAllowed(category, otherBiz, vendor)).hasValue(false);
        // Other businesses can sell other vendor in category
        assertThat(service.isAutoAcceptanceAllowed(category, otherBiz, otherVendor)).hasValue(true);
        // Other businesses can sell without vendor in category
        assertThat(service.isAutoAcceptanceAllowed(category, otherBiz, null)).hasValue(true);
        // Category has no rules
        assertThat(service.isAutoAcceptanceAllowed(otherCategory, biz, vendor)).isEmpty();
    }

    /**
     * Задано правило: Бизнес "Рога и Копыта", категория "Мобильные телефоны", вендор Apple
     * Задано еще одно правило: Бизнес "Ромашка", категория "Мобильные телефоны"
     * Других правил не задано.
     * В этом случае "Рога и копыта" может торговать чем угодно, кроме товаров в категории "Мобильные телефоны",
     * бренд которых НЕ Apple. "Ромашка" может торговать чем угодно, кроме товаров в категории "Мобильные телефоны" с
     * брендом Apple. Кроме них никто другой не может продавать товары в категории "Мобильные телефоны".
     */
    @Test
    public void multipleRules() {
        long biz = BIZ_ID_SUPPLIER;
        long otherBiz = BIZ_ID_SUPPLIER + 1;
        long thirdBiz = BIZ_ID_SUPPLIER + 2;
        long category = 333333;
        int vendor = 123;
        int otherVendor = 124;

        initRules(Set.of(
            new CategoryRule(category, biz, vendor),
            new CategoryRule(category, otherBiz, 0)
        ));

        // Business is allowed to sell the vendor in category
        assertThat(service.isAutoAcceptanceAllowed(category, biz, vendor)).hasValue(true);
        // Business can't sell other vendors in category
        assertThat(service.isAutoAcceptanceAllowed(category, biz, otherVendor)).hasValue(false);
        // Business is not allowed to sell without vendor in category
        assertThat(service.isAutoAcceptanceAllowed(category, biz, null)).hasValue(false);
        // Other businesses can't sell the vendor in category
        assertThat(service.isAutoAcceptanceAllowed(category, otherBiz, vendor)).hasValue(false);
        // Other businesses can sell other vendor in category
        assertThat(service.isAutoAcceptanceAllowed(category, otherBiz, otherVendor)).hasValue(true);
        // Other businesses can sell without vendor in category
        assertThat(service.isAutoAcceptanceAllowed(category, otherBiz, null)).hasValue(true);
        // Third businesses can't sell anything in category
        assertThat(service.isAutoAcceptanceAllowed(category, thirdBiz, vendor)).hasValue(false);
        assertThat(service.isAutoAcceptanceAllowed(category, thirdBiz, otherVendor)).hasValue(false);
        assertThat(service.isAutoAcceptanceAllowed(category, thirdBiz, null)).hasValue(false);
    }

    private void initRules(Set<CategoryRule> rules) {
        categoryRuleRepository.insert(rules);
    }
}
