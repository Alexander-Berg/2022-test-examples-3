package ru.yandex.market.checkout.checkouter.checkout;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Set;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.Tag;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestBase;
import ru.yandex.market.checkout.checkouter.shop.MarketplaceFeature;
import ru.yandex.market.checkout.helpers.BlueCrossborderOrderHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.DeliveryResponseProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.OrderPropertyType.ALLOW_SPASIBO;

/**
 * @author : poluektov
 * date: 29.01.2019.
 */
public class CheckoutSpasiboOrderTest extends AbstractPaymentTestBase {

    @Autowired
    private BlueCrossborderOrderHelper blueCrossborderOrderHelper;

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @Story(Stories.CHECKOUT)
    @DisplayName("Создание заказа с возможностью оплаты сбер-спасибо")
    @Test
    public void createFFSpasiboOrder() {
        Parameters parameters = prepareOrderParams();

        MultiCart cart = orderCreateHelper.cart(parameters);
        assertThat(cart.getCarts(), hasSize(1));
        assertThat(cart.getCarts().get(0).getValidFeatures(), hasItem(MarketplaceFeature.SPASIBO_PAY));

        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getValidFeatures(), hasItem(MarketplaceFeature.SPASIBO_PAY));
    }

    @Test
    @DisplayName("Проверяем как будет появляться фича спасибо на мультичекауте")
    public void multiCheckoutWithSpasibo() {

        Parameters parameters = prepareOrderParams();
        parameters.addOrder(prepareOrderParams());


        parameters.getBuiltMultiCart().setPromoCode("SOME_PROMO");
        parameters.setMockLoyalty(true);
        //делаем по одному из заказов мультикорзины цену на все айтемы - 1, тем самым отключая фичу. на другом она
        //должна остаться
        parameters.getLoyaltyParameters().clearDiscounts();
        parameters.getBuiltMultiCart().getCarts().get(0).getItems().forEach(
                item -> parameters.getLoyaltyParameters()
                        .addLoyaltyDiscount(item, new LoyaltyDiscount(
                                item.getBuyerPrice().subtract(BigDecimal.ONE),
                                PromoType.MARKET_COUPON))
        );

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        assertThat(multiOrder, notNullValue());
        Order order1 =
                multiOrder.getOrders().stream().filter(o -> o.getLabel().equals(parameters.getBuiltMultiCart()
                        .getCarts().get(0).getLabel())).findFirst().get();
        Order order2 =
                multiOrder.getOrders().stream().filter(o -> o.getLabel().equals(parameters.getBuiltMultiCart()
                        .getCarts().get(1).getLabel())).findFirst().get();
        assertThat(order1.getBuyerItemsTotal(), equalTo(BigDecimal.ONE));
        assertThat(order1.getValidFeatures(), not(hasItem(MarketplaceFeature.SPASIBO_PAY)));
        assertThat(order2.getValidFeatures(), hasItem(MarketplaceFeature.SPASIBO_PAY));
    }

    @Disabled
    @Tag(Tags.CROSSBORDER)
    @Test
    public void checkoutCrossborder() throws IOException {
        DeliveryResponse deliveryResponse = DeliveryResponseProvider.buildPostDeliveryResponse();
        Parameters parameters = blueCrossborderOrderHelper.setupParameters(deliveryResponse, new Parameters());
        MultiCart cart = blueCrossborderOrderHelper.doCartBlueWithoutFulfilment(parameters);
        cart.getCarts().forEach(order ->
                assertThat(order.getValidFeatures(), not(hasItem(MarketplaceFeature.SPASIBO_PAY))));
        MultiOrder multiOrder = blueCrossborderOrderHelper.checkoutCrossborder(parameters);
        multiOrder.getOrders().forEach(order ->
                assertThat(order.getValidFeatures(), not(hasItem(MarketplaceFeature.SPASIBO_PAY))));
    }

    @Test
    public void saveSpasiboProperty() {
        Parameters parameters = prepareOrderParams();

        MultiCart cart = orderCreateHelper.cart(parameters);
        assertThat(cart.getCarts(), hasSize(1));
        assertThat(cart.getCarts().get(0).getValidFeatures(), hasItem(MarketplaceFeature.SPASIBO_PAY));

        parameters.getOrder().addProperty(ALLOW_SPASIBO.create(null, true));
        Order order = orderCreateHelper.createOrder(parameters);
        assertTrue(order.getProperties().stream().anyMatch(p -> p.getName().equals("allowSpasibo")));
        order = orderService.getOrder(order.getId());
        assertTrue(order.getProperties().stream().anyMatch(p -> p.getName().equals("allowSpasibo")));
    }

    @Test
    public void shouldDisableSpasiboForExcludedVendors() {
        long excludedVendor = 206928;
        checkouterProperties.setSpasiboExcludedVendors(Set.of(excludedVendor));
        Parameters parameters = prepareOrderParams();
        parameters.addOrder(prepareOrderParams());

        //Делаем по одному из заказов мультизаказа айтем от вендора из исключений, тем самым отключая фичу.
        //Сейчас это должно отключать фичу для всего мультизаказа.
        parameters.getBuiltMultiCart().getCarts().get(0).getItems().iterator().next().setVendorId(excludedVendor);
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);

        Order order1 =
                multiOrder.getOrders().stream().filter(o -> o.getLabel().equals(parameters.getBuiltMultiCart()
                        .getCarts().get(0).getLabel())).findFirst().get();
        Order order2 =
                multiOrder.getOrders().stream().filter(o -> o.getLabel().equals(parameters.getBuiltMultiCart()
                        .getCarts().get(1).getLabel())).findFirst().get();

        assertThat(order1.getValidFeatures(), not(hasItem(MarketplaceFeature.SPASIBO_PAY)));
        assertThat(order2.getValidFeatures(), not(hasItem(MarketplaceFeature.SPASIBO_PAY)));
    }

    @Test
    public void shouldDisableSpasiboForResellers() {
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        long reseller = parameters.getOrder().getItems().iterator().next().getSupplierId();
        checkouterProperties.setResellers(Set.of(reseller));

        MultiCart cart = orderCreateHelper.cart(parameters);
        assertThat(cart.getCarts(), hasSize(1));
        assertThat(cart.getCarts().get(0).getValidFeatures(), not(hasItem(MarketplaceFeature.SPASIBO_PAY)));

        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getValidFeatures(), not(hasItem(MarketplaceFeature.SPASIBO_PAY)));
    }

    @Test
    public void shouldDisableSpasiboViaToggle() {
        checkouterProperties.setDisableSpasibo(true);
        MultiCart cart = orderCreateHelper.cart(WhiteParametersProvider.simpleWhiteParameters());
        assertThat(cart.getCarts(), hasSize(1));
        assertThat(cart.getCarts().get(0).getValidFeatures(), not(hasItem(MarketplaceFeature.SPASIBO_PAY)));

        Order order = orderCreateHelper.createOrder(WhiteParametersProvider.simpleWhiteParameters());
        assertThat(order.getValidFeatures(), not(hasItem(MarketplaceFeature.SPASIBO_PAY)));
    }

    private Parameters prepareOrderParams() {
        return BlueParametersProvider.defaultBlueOrderParameters();
    }
}
