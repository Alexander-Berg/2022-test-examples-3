package ru.yandex.market.checkout.pushapi.out.shopApi.xml;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.pushapi.client.xml.order.AddressXmlSerializer;
import ru.yandex.market.checkout.pushapi.in.xml.DeliveryResponseXmlSerializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class DeliveryResponseXmlSerializerTest {

    private Address address = mock(Address.class);

    private DeliveryResponseXmlSerializer serializer = new DeliveryResponseXmlSerializer();

    @BeforeEach
    public void setUp() throws Exception {
        serializer.setAddressXmlSerializer(
                new AddressXmlSerializer() {
                    @Override
                    public void serializeXml(Address value, PrimitiveXmlWriter writer) throws IOException {
                        assertEquals(address, value);
                        writer.addNode("address", "address");
                    }
                }
        );
        serializer.setCheckoutDateFormat(new CheckoutDateFormat());
    }

    @Test
    public void shouldSerializePaymentMethods() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                new DeliveryResponse() {{
                    setPaymentAllow(true);
                    setPaymentOptions(new HashSet<>(Arrays.asList(PaymentMethod.SHOP_PREPAID)));
                }},
                "<delivery payment-allow=\"true\"><payment-methods><payment-method>SHOP_PREPAID</payment-method" +
                        "></payment-methods></delivery>"
        );
    }

}
