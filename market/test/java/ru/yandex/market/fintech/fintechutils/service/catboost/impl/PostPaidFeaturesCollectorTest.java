package ru.yandex.market.fintech.fintechutils.service.catboost.impl;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.fintech.fintechutils.AbstractFunctionalTest;
import ru.yandex.market.fintech.fintechutils.helpers.MultiCartProvider;
import ru.yandex.market.fintech.fintechutils.helpers.OrderDeliveryProvider;
import ru.yandex.market.fintech.fintechutils.helpers.OrderItemProvider;
import ru.yandex.market.fintech.fintechutils.helpers.OrderProvider;
import ru.yandex.market.fintech.fintechutils.service.catboost.PostPaidFeaturesCollector;
import ru.yandex.market.fintech.fintechutils.service.category.cache.HidCategoryCache;
import ru.yandex.mj.generated.server.model.MultiCartDto;
import ru.yandex.mj.generated.server.model.OrderDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DbUnitDataSet(before = "hid_category_mappings.before.csv")
class PostPaidFeaturesCollectorTest extends AbstractFunctionalTest {

    @Autowired
    private PostPaidFeaturesCollector postPaidFeaturesCollector;
    @Autowired
    private HidCategoryCache hidCategoryCache;

    @BeforeEach
    void init() {
        hidCategoryCache.refreshCache();
    }

    @Test
    @DisplayName("Проверка расчета фичей по параметрам корзины")
    void calculateOrderFeatures() {
        var multiCartDto = MultiCartProvider.multiCart().build();
        OrderFeatures orderFeatures = postPaidFeaturesCollector.collectFrom(multiCartDto).iterator().next();
        assertEquals(1, orderFeatures.getItemCount(), "item_count должен быть равен количеству позиций заказа");
        assertEquals(1, orderFeatures.getSupplierGmvShare(), "supplier_gmv_share равен 1, если в заказе " +
                "присутствует только 1 поставщик");
        assertEquals("Компьютерная техника > Компьютеры", orderFeatures.getCategoryNameLevel2(),
                "category_name_level_2 должен включать category_name_level_1");
        BigDecimal buyerItemsTotal = multiCartDto.getMultiCart().stream()
                .map(OrderDto::getItems)
                .flatMap(Collection::stream)
                .map(item -> new BigDecimal(item.getBuyerPrice()).multiply(BigDecimal.valueOf(item.getCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, BigDecimal.valueOf(orderFeatures.getGmv()).compareTo(buyerItemsTotal), "gmv заказа должно " +
                "быть равно сумме buyer_price * count всех айтемов заказа");
        assertEquals(1, orderFeatures.getUserOrderCountPrepaid());
        assertEquals(1, orderFeatures.getUserOrderCountPostpaid());
    }

    @Test
    @DisplayName("Фичи category_name_level_1 и category_name_level_2 должны вычисляться раздельно")
    void calculateOrderFeaturesCategoryNameLevel() {

        MultiCartDto multiCartDto = MultiCartProvider.builder()
                .withMultiCart(List.of(
                        OrderProvider.builder()
                                .withOrderId(1L)
                                .withDelivery(OrderDeliveryProvider.delivery().build())
                                .withGmv("3000")
                                .withItems(List.of(
                                        OrderItemProvider.builder()
                                                .withCategoryId(91014) // детские товары,кресло
                                                .withSupplierId(112233L)
                                                .withBuyerPrice("5000")
                                                .withCount(2)
                                                .build(),
                                        OrderItemProvider.builder()
                                                .withCategoryId(91015) // детские товары,коляска
                                                .withSupplierId(112233L)
                                                .withBuyerPrice("15100")
                                                .withCount(1)
                                                .build(),
                                        OrderItemProvider.builder()
                                                .withCategoryId(91016) // спорт,мотоцикл
                                                .withSupplierId(112233L)
                                                .withBuyerPrice("25000")
                                                .withCount(1)
                                                .build()
                                ))
                                .build()
                ))
                .withPuid(99L)
                .build();
        OrderFeatures orderFeatures = postPaidFeaturesCollector.collectFrom(multiCartDto).iterator().next();
        assertEquals("детские товары", orderFeatures.getCategoryNameLevel1());
        assertEquals("спорт > мотоцикл", orderFeatures.getCategoryNameLevel2());
    }

}
