package ru.yandex.market.delivery.mdbapp.integration.enricher.fulfillment.common;

import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.mdbapp.integration.enricher.EnrichmentFailException;
import ru.yandex.market.delivery.mdbapp.integration.payload.EnrichedFulfillmentOrder;

public class AddressEnricherTest {

    private AddressImpl buyerAddress;
    private AddressImpl shopAddress;
    private Delivery delivery;

    private EnrichedFulfillmentOrder efo;
    private AddressEnricher addressEnricher;
    private SoftAssertions softAssertions;

    @Before
    public void setUp() {
        delivery = new Delivery();
        buyerAddress = new AddressImpl();
        buyerAddress.setNotes("BuyerAddress");
        shopAddress = new AddressImpl();
        shopAddress.setNotes("ShopAddress");
        Order order = new Order();
        order.setDelivery(delivery);
        efo = new EnrichedFulfillmentOrder(order);
        addressEnricher = new AddressEnricher();
        softAssertions = new SoftAssertions();
    }

    @After
    public void tearDown() {
        softAssertions.assertAll();
    }

    @Test
    public void whenBuyerAddressExistsShouldReturnIt() {
        delivery.setBuyerAddress(buyerAddress);
        addressEnricher.enrich(efo);
        softAssertions.assertThat(buyerAddress)
            .as("Should return %s", buyerAddress.getNotes())
            .isEqualTo(efo.getAddress());
    }

    @Test
    public void whenBuyerAndShopAddressExistsShouldReturnBuyer() {
        delivery.setBuyerAddress(buyerAddress);
        addressEnricher.enrich(efo);
        softAssertions.assertThat(buyerAddress)
            .as("Should return %s", buyerAddress.getNotes())
            .isEqualTo(efo.getAddress());
    }

    @Test
    public void whenBuyerAddressNotExistShouldReturnShopAddress() {
        delivery.setShopAddress(shopAddress);
        addressEnricher.enrich(efo);
        softAssertions.assertThat(shopAddress)
            .as("Should return %s", shopAddress.getNotes())
            .isEqualTo(efo.getAddress());
    }

    @Test
    public void whenNeitherBuyerNorShopAddressesExistShouldThrowError() {
        softAssertions.assertThatThrownBy(() -> addressEnricher.enrich(efo))
            .isInstanceOf(EnrichmentFailException.class);
    }
}
