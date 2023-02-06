package ru.yandex.market.checkout.checkouter.client;

import java.io.IOException;
import java.math.BigDecimal;

import com.google.common.collect.Iterables;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.ItemPrices;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.report.ItemInfo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.checkout.test.providers.RegionProvider.getManufacturerCountries;

public class CheckouterClientOrderItemSupplierInformationTest extends AbstractWebTestBase {

    private Parameters parameters;

    @BeforeEach
    public void setUp() {
        parameters = BlueParametersProvider.defaultBlueOrderParameters();
        OrderItem onlyItem =
                Iterables.getOnlyElement(Iterables.getOnlyElement(parameters.getBuiltMultiCart().getCarts())
                        .getItems());
        ItemInfo overrideItemInfo = parameters.getReportParameters().overrideItemInfo(onlyItem.getFeedOfferId());
        overrideItemInfo.setSupplierDescription("Описание товара от поставщика");
        overrideItemInfo.setSupplierWorkSchedule("Пн-Пт с 9:00 до 18:00");
        overrideItemInfo.setManufacturerCountries(getManufacturerCountries());
    }

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @DisplayName("/cart должен возвращать информацию о поставщике из репорта")
    @Test
    public void cartReturnsSupplierInformation() throws Exception {
        MultiCart cart = cart();

        assertThat(cart.getCarts(), hasSize(1));
        Order order = Iterables.getOnlyElement(cart.getCarts());
        OrderItem item = Iterables.getOnlyElement(order.getItems());
        assertSupplierInformation(item);
    }

    private MultiCart cart() throws IOException {
        orderCreateHelper.initializeMock(parameters);
        CartParameters cartParameters = CartParameters.builder()
                .withRgb(Color.BLUE)
                .withUid(parameters.getBuyer().getUid())
                .build();
        return client.cart(
                parameters.getBuiltMultiCart(),
                cartParameters
        );
    }

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @DisplayName("/checkout должен возвращать информацию от поставщика")
    @Test
    public void checkoutReturnsSupplierInformation() throws Exception {
        MultiCart cart = cart();
        pushApiConfigurer.mockAccept(cart.getCarts(), true);
        CheckoutParameters checkoutParameters = CheckoutParameters.builder()
                .withUid(parameters.getBuyer().getUid())
                .withRgb(Color.BLUE)
                .build();

        MultiOrder multiOrder = client.checkout(
                orderCreateHelper.mapCartToOrder(cart, parameters),
                checkoutParameters
        );

        assertThat(multiOrder.getOrders(), hasSize(1));
        Order order = Iterables.getOnlyElement(multiOrder.getOrders());
        OrderItem item = Iterables.getOnlyElement(order.getItems());
        assertSupplierInformation(item);

        Order savedOrder = client.getOrder(order.getId(), ClientRole.SYSTEM, 1L);
        OrderItem savedItem = Iterables.getOnlyElement(savedOrder.getItems());
        assertSupplierInformation(savedItem);
    }

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("/checkout должен возвращать поля PartnerMarkup")
    @Test
    public void checkoutReturnsPartnerMarkups() throws Exception {
        MultiCart cart = cart();
        pushApiConfigurer.mockAccept(cart.getCarts(), true);
        CheckoutParameters checkoutParameters = CheckoutParameters.builder()
                .withUid(parameters.getBuyer().getUid())
                .withRgb(Color.BLUE)
                .build();
        MultiOrder multiOrder = client.checkout(
                orderCreateHelper.mapCartToOrder(cart, parameters),
                checkoutParameters
        );

        assertThat(multiOrder.getOrders(), hasSize(1));
        Order order = Iterables.getOnlyElement(multiOrder.getOrders());
        OrderItem item = Iterables.getOnlyElement(order.getItems());
        ItemPrices actualItemPrices = item.getPrices();
        assertThat(actualItemPrices.getPartnerPriceMarkup(), is(notNullValue()));
        assertThat(actualItemPrices.getPartnerPriceMarkup().getUpdateTime(), notNullValue());
        assertThat(actualItemPrices.getPartnerPriceMarkup().getVendorString(), equalTo("GreatSomething"));
        assertThat(actualItemPrices.getPartnerPriceMarkup().getCoefficients(), hasSize(3));
        assertThat(actualItemPrices.getPartnerPrice(), equalTo(new BigDecimal("35983")));
    }


    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("/checkout должен возвращать валюту поставщика")
    @Test
    public void checkoutReturnsSupplierCurrency() throws Exception {
        MultiCart cart = cart();
        pushApiConfigurer.mockAccept(cart.getCarts(), true);
        CheckoutParameters checkoutParameters = CheckoutParameters.builder()
                .withUid(parameters.getBuyer().getUid())
                .withRgb(Color.BLUE)
                .build();
        MultiOrder multiOrder = client.checkout(
                orderCreateHelper.mapCartToOrder(cart, parameters),
                checkoutParameters
        );

        assertThat(multiOrder.getOrders(), hasSize(1));
        Order order = Iterables.getOnlyElement(multiOrder.getOrders());
        OrderItem item = Iterables.getOnlyElement(order.getItems());
        assertThat(item.getSupplierCurrency(), equalTo(Currency.RUR));
        final Order readOrder = client.getOrder(order.getId(), ClientRole.SYSTEM, null);
        assertThat(
                Iterables.getOnlyElement(readOrder.getItems()).getSupplierCurrency(),
                equalTo(Currency.RUR)
        );

    }

    private void assertSupplierInformation(OrderItem item) {
        assertThat(item.getSupplierDescription(), notNullValue());
        assertThat(item.getSupplierDescription(), is("Описание товара от поставщика"));
        assertThat(item.getSupplierWorkSchedule(), notNullValue());
        assertThat(item.getSupplierWorkSchedule(), is("Пн-Пт с 9:00 до 18:00"));
        assertThat(item.getManufacturerCountries(), notNullValue());
        assertThat(item.getManufacturerCountries(), hasSize(2));
        assertThat(item.getManufacturerCountries().get(0).getName().get(), is("Москва"));
        assertThat(item.getManufacturerCountries().get(1).getName().get(), is("Новосибирск"));
    }
}
