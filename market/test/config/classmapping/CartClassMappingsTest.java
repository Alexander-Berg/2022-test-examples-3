package config.classmapping;

import java.io.IOException;
import java.util.Collections;

import javax.annotation.Nullable;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.http.MediaType;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.xmlunit.matchers.CompareMatcher;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.TaxSystem;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.client.entity.CartItem;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.EnhancedRandomHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

//MARKETCHECKOUT-4009 run all randomized tests several times to prevent floating failures
public class CartClassMappingsTest extends BaseClassMappingsRandomizedTest {

    private static final EnhancedRandom enhancedRandom = EnhancedRandomHelper.createEnhancedRandom();

    @RepeatedTest(10)
    public void cartXmlSerializerTest() throws IOException {
        final Cart cart = prepareCart(null);

        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        httpMessageConverter.write(cart, MediaType.APPLICATION_XML, outputMessage);

        assertThat(
                outputMessage.getBodyAsString(),
                CompareMatcher.isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<cart currency=\"RUR\" tax-system=\"OSN\" rgb='BLUE' experiments='experiments'>" +
                        "<delivery region-id=\"123\" vat=\"VAT_10\"/>" +
                        "<items>" +
                        "<item feed-id=\"1\" offer-id=\"1\" digital=\"false\" count=\"1\" vat=\"VAT_10\" " +
                        "subsidy='0'/>" +
                        "</items>" +
                        "</cart>")
        );
    }

    @RepeatedTest(10)
    public void cartXmlSerializerWithBuyer() throws IOException {
        final Cart cart = prepareCart(BuyerProvider.getSberIdBuyer());

        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        httpMessageConverter.write(cart, MediaType.APPLICATION_XML, outputMessage);

        assertThat(
                outputMessage.getBodyAsString(),
                CompareMatcher.isIdenticalTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<cart currency=\"RUR\" tax-system=\"OSN\" rgb='BLUE' experiments='experiments'>" +
                        "<delivery region-id=\"123\" vat=\"VAT_10\"/>" +
                        "<buyer first-name=\"Leo\" last-name=\"Tolstoy\" phone=\"+71234567891\" email=\"a@b.com\" " +
                        "personal-email-id='9e92bc743c624f958b8876c7841a653b' " +
                        "personal-full-name-id='a1c595eb35404207aecfa080f90a8986' " +
                        "personal-phone-id='c0dec0dedec0dec0dec0dec0dedec0de' " +
                        "uid=\"2305843009213693951\"/>" +
                        "<items>" +
                        "<item feed-id=\"1\" offer-id=\"1\" digital=\"false\" count=\"1\" vat=\"VAT_10\" " +
                        "subsidy='0'/>" +
                        "</items>" +
                        "</cart>")
        );

    }

    @RepeatedTest(10)
    public void testParseEmpty() throws Exception {
        final Cart actual = (Cart) httpMessageConverter.read(Cart.class, new MockHttpInputMessage("<cart />".getBytes()));

        assertNotNull(actual);
    }

    private Cart prepareCart(@Nullable Buyer buyer) {
        final Cart cart = enhancedRandom.nextObject(Cart.class, "isFulfilment", "isCrossborder", "preorder", "hasCertificate",
                "delivery.parcels.route", "delivery.shipments.route", "delivery.shipment.route", "delivery.deliveryOptionParameters.route");
        cart.setDelivery(new Delivery(123L));
        cart.setCurrency(Currency.RUR);
        cart.setDeliveryCurrency(null);
        cart.setTaxSystem(TaxSystem.OSN);
        cart.setRgb(Color.BLUE);
        cart.getDelivery().setVat(VatType.VAT_10);
        cart.setItems(
                Collections.singletonList(
                        new CartItem() {{
                            setFeedId(1L);
                            setOfferId("1");
                            setCount(1);
                        }})
        );
        cart.getItems().iterator().next().setVat(VatType.VAT_10);
        cart.setBuyer(buyer);
        cart.setExperiments("experiments");
        return cart;
    }
}
