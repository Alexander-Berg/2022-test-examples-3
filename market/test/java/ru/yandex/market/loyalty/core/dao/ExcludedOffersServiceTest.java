package ru.yandex.market.loyalty.core.dao;

import java.util.Collections;
import java.util.stream.Stream;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.bundle.BundledOrderItemRequest;
import ru.yandex.market.loyalty.api.model.report.InternalSpec;
import ru.yandex.market.loyalty.api.model.report.Specs;
import ru.yandex.market.loyalty.core.model.order.Item;
import ru.yandex.market.loyalty.core.rule.ItemFilteringInfo;
import ru.yandex.market.loyalty.core.rule.ItemsFilter;
import ru.yandex.market.loyalty.core.service.discount.OrderItemsConverter;
import ru.yandex.market.loyalty.core.service.exclusions.ExcludedOffersService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.BuildCustomizer;
import ru.yandex.market.loyalty.core.utils.OrderRequestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.PHARMA_BUD_CATEGORY_ID;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.PHARMA_VITAMINS_AND_MINERALS_CATEGORY_ID;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.STICK_CATEGORY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.categoryId;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.msku;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.specs;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
public class ExcludedOffersServiceTest extends MarketLoyaltyCoreMockedDbTestBase {

    @Autowired
    private ExcludedOffersService excludedOffersService;
    @Autowired
    private ItemsFilter itemsFilter;

    @Test
    public void shouldExcludeStickHid() {
        Item item = item(categoryId(STICK_CATEGORY));
        assertEquals(0, itemsFilter.excludeItems(Stream.of(item)).count());
    }

    @Test
    public void isExcludedNullCategory() {
        var itemRequest = OrderRequestUtils.orderItemBuilder(msku("111")).setCategoryId(null).build();
        var butNotItemRequest = OrderRequestUtils.orderItemBuilder(msku("100630994854")).setCategoryId(null).build();
        assertTrue(excludedOffersService.exclusionTest(
                new ItemFilteringInfo(item(itemRequest), false)).isNegative());
        assertFalse(excludedOffersService.exclusionTest(
                new ItemFilteringInfo(item(butNotItemRequest), false)).isNegative());
    }

    @Test
    public void shouldPermitSpecialStick() {
        Item item = item(categoryId(STICK_CATEGORY), msku("100630994854"));
        assertEquals(1, itemsFilter.excludeItems(Stream.of(item)).count());
    }

    @Test
    public void shouldFindPharma() {
        assertTrue(excludedOffersService.isPharma(item(categoryId(PHARMA_BUD_CATEGORY_ID))));
    }

    @Test
    public void excludedPsychotropic() {
        assertTrue(excludedOffersService.exclusionTest(
                new ItemFilteringInfo(
                        item(categoryId(PHARMA_VITAMINS_AND_MINERALS_CATEGORY_ID),
                                specs(new Specs(Collections.singleton(new InternalSpec("spec", "psychotropic"))))),
                        false))
                .isNegative()
        );
    }

    @Test
    public void applyPharmaBaa() {
        assertFalse(excludedOffersService.exclusionTest(
                new ItemFilteringInfo(
                    item(categoryId(PHARMA_VITAMINS_AND_MINERALS_CATEGORY_ID),
                            specs(new Specs(Collections.singleton(new InternalSpec("spec", "baa"))))),
                        false))
                .isNegative()
        );
    }

    @Test
    public void shouldFindPharmaAndExcludedSpecs() {
        assertTrue(excludedOffersService.isPharma(item(specs(new Specs(Collections.singleton(new InternalSpec("spec",
                "psychotropic")))))));
    }

    @SafeVarargs
    private static Item item(
            BuildCustomizer<BundledOrderItemRequest, OrderRequestUtils.OrderItemBuilder>... customizers
    ) {
        BundledOrderItemRequest itemRequest = OrderRequestUtils.orderItemBuilder(customizers).build();
        return item(itemRequest);
    }

    private static Item item(BundledOrderItemRequest orderItemRequest) {
        return OrderItemsConverter.constructItemsFromRequest(Collections.singletonList(orderItemRequest)).get(0);
    }
}
