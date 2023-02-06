package ru.yandex.market.checkout.pushapi.client.xml;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.common.xml.AbstractXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeliveryWithRegion;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.pushapi.client.xml.order.AddressXmlSerializer;

import java.io.IOException;
import java.math.BigDecimal;

import static org.mockito.Mockito.mock;

public class DeliveryWithRegionXmlSerializerTest {
    
    private AddressXmlSerializer addressXmlSerializer = mock(AddressXmlSerializer.class);
    private DeliveryWithRegionXmlSerializer serializer = new DeliveryWithRegionXmlSerializer();
    private Address address = mock(Address.class);

    @Before
    public void setUp() throws Exception {
        serializer.setCheckoutDateFormat(new CheckoutDateFormat());
        XmlTestUtil.initMockSerializer(
            addressXmlSerializer, address,
            new XmlTestUtil.MockSerializer() {
                @Override
                public void serialize(AbstractXmlSerializer.PrimitiveXmlWriter writer) throws IOException {
                    writer.addNode("address", "address");
                }
            }
        );
        
        serializer.setAddressXmlSerializer(addressXmlSerializer);
    }

    @Test
    public void testSerializeAcceptRequest() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
            serializer,
            new DeliveryWithRegion() {{
                setId("12345");
                setType(DeliveryType.DELIVERY);
                setPrice(new BigDecimal(1234l));
                setDeliveryDates(new DeliveryDates(XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")));
                setServiceName("pochta");
//                setRegion(new Region(123, "Москва", new Region(234, "Россия", new Region(345, "Земля"))));
                setAddress(address);
                setOutletId(2345l);
            }},
            "    <delivery id='12345'" +
                "          type='DELIVERY'" +
                "          price='1234'" +
                "          service-name='pochta'>" +
                "        <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                "        <region id='123' name='Москва'>" +
                "            <parent id='234' name='Россия'>" +
                "                <parent id='345' name='Земля' />" +
                "            </parent>" +
                "        </region>" +
                "        <address>address</address>" +
                "        <outlet id='2345' />" +
                "    </delivery>"
        );

    }

    @Test
    public void testSerializeCartRequestWithAddress() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
            serializer,
            new DeliveryWithRegion() {{
//                setRegion(new Region(123, "Москва", new Region(234, "Россия", new Region(345, "Земля"))));
                setAddress(address);
            }},
            "    <delivery>" +
                "        <region id='123' name='Москва'>" +
                "            <parent id='234' name='Россия'>" +
                "                <parent id='345' name='Земля' />" +
                "            </parent>" +
                "        </region>" +
                "        <address>address</address>" +
                "    </delivery>"
        );
    }

    @Test
    public void testSerializeCartRequestWithoutAddress() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
            serializer,
            new DeliveryWithRegion() {{
//                setRegion(new Region(123, "Москва", new Region(234, "Россия", new Region(345, "Земля"))));
            }},
            "    <delivery>" +
                "        <region id='123' name='Москва'>" +
                "            <parent id='234' name='Россия'>" +
                "                <parent id='345' name='Земля' />" +
                "            </parent>" +
                "        </region>" +
                "    </delivery>"
        );

    }

    @Test
    public void testSerializeEmpty() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
            serializer,
            new DeliveryWithRegion(),
            "<delivery />"
        );

    }
}
