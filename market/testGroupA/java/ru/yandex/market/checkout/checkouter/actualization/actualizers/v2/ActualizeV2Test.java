package ru.yandex.market.checkout.checkouter.actualization.actualizers.v2;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.promo.PromoConfigurer;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CommonPaymentResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.MultiCartResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.MultiCartTotalsResponse;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItemAmount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.ANAPLAN_ID;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.SHOP_PROMO_KEY;

public class ActualizeV2Test extends AbstractWebTestBase {

    @Autowired
    private PromoConfigurer promoConfigurer;

    @Test
    public void shouldFailIfEmptyItemCountInStockStorage() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setCheckCartErrors(false);
        Iterator<OrderItem> itemsIterator = parameters.getOrder().getItems().iterator();
        OrderItem actualItem = itemsIterator.next();
        SSItem ssItem = SSItem.of(
                actualItem.getShopSku(), actualItem.getSupplierId(),
                ObjectUtils.firstNonNull(actualItem.getWarehouseId(), 1)
        );
        parameters.setStockStorageResponse(List.of(SSItemAmount.of(ssItem, 0)));

        MultiCart multiCart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(parameters);

        assertEquals(1, multiCart.getCarts().size());
        assertEquals(1, multiCart.getCarts().get(0).getItems().size());
        Set<ItemChange> changes = multiCart.getCarts().get(0).getItems().iterator().next().getChanges();
        assertEquals(1, changes.size());
        assertEquals(ItemChange.MISSING, changes.iterator().next());
    }

    @Test
    public void shouldFailIfEmptyItemCountInRq() throws Throwable {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        parameters.setCheckCartErrors(false);
        parameters.getOrder().getItems().iterator().next().setCount(0);

        MultiCart multiCart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(parameters);

        assertEquals(1, multiCart.getCarts().size());
        assertEquals(1, multiCart.getCarts().get(0).getItems().size());
        Set<ItemChange> changes = multiCart.getCarts().get(0).getItems().iterator().next().getChanges();
        assertEquals(1, changes.size());
        assertEquals(ItemChange.MISSING, changes.iterator().next());
    }

    @Test
    public void shouldDiscountCalculator() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        BigDecimal buyerDiscount = BigDecimal.valueOf(10);
        promoConfigurer.applyDirectDiscount(
                parameters.getOrder().getItems().iterator().next(),
                PROMO_KEY,
                ANAPLAN_ID,
                SHOP_PROMO_KEY,
                buyerDiscount, null, true, true);
        promoConfigurer.applyTo(parameters);
        MultiCart multiCart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(parameters);
        assertEquals(1, multiCart.getCarts().size());
        assertEquals(1, multiCart.getCarts().get(0).getItems().size());
        Set<ItemPromo> promos = multiCart.getCarts().get(0).getItems().iterator().next().getPromos();
        assertEquals(1, promos.size());
        ItemPromo itemPromo = promos.iterator().next();
        assertEquals(PromoType.DIRECT_DISCOUNT, itemPromo.getType());
        assertEquals(buyerDiscount, itemPromo.getBuyerDiscount());
    }

    @Test
    public void shouldTruePrepayForAll() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.addShopMetaData(parameters.getShopId(), ShopSettingsHelper.getDsbsShopPrepayMeta());
        MultiCartResponse multiCartResponse = orderCreateHelper.multiCartActualize(parameters);
        MultiCartTotalsResponse totals = multiCartResponse.getTotals();
        assertNotNull(totals);
        CommonPaymentResponse commonPayment = totals.getCommonPayment();
        assertNotNull(commonPayment);
        assertTrue(commonPayment.getPrepayForAll());
    }

    @Test
    public void shouldFalsePrepayForAll() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.addShopMetaData(parameters.getShopId(), ShopSettingsHelper.getPostpayMeta());
        MultiCartResponse multiCartResponse = orderCreateHelper.multiCartActualize(parameters);
        MultiCartTotalsResponse totals = multiCartResponse.getTotals();
        assertNotNull(totals);
        CommonPaymentResponse commonPayment = totals.getCommonPayment();
        assertNotNull(commonPayment);
        assertFalse(commonPayment.getPrepayForAll());
    }


}
