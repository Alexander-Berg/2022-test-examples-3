package ru.yandex.market.checkout.pushapi.out.shopApi.xml;

import java.io.IOException;
import java.util.Arrays;

import javax.annotation.Nullable;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeliveryWithRegion;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.pushapi.client.xml.order.BuyerXmlSerializer;
import ru.yandex.market.checkout.pushapi.providers.ShopOrderProvider;
import ru.yandex.market.checkout.pushapi.shop.entity.ExternalCart;
import ru.yandex.market.checkout.pushapi.shop.entity.ExternalCartItem;
import ru.yandex.market.checkout.util.EnhancedRandomHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

//TODO пераработать тест на адекатную проверку MARKETCHECKOUT-14893
public class ExternalCartXmlSerializerTest {

    private DeliveryWithRegion deliveryWithRegion = mock(DeliveryWithRegion.class);
    private ExternalCartItem item1 = new ExternalCartItem() {{
        setOfferId("item1");
        setFeedId(1L);
    }};
    private ExternalCartItem item2 = new ExternalCartItem() {{
        setOfferId("item2");
        setFeedId(1L);
    }};

    private ExternalCartXmlSerializer serializer = new ExternalCartXmlSerializer();

    private final EnhancedRandom enhancedRandomHelper = EnhancedRandomHelper.createEnhancedRandom();


    @BeforeEach
    public void setUp() throws Exception {
        serializer.setDeliveryWithRegionXmlSerializer(
                new DeliveryWithRegionXmlSerializer() {
                    @Override
                    public void serializeXml(DeliveryWithRegion value, PrimitiveXmlWriter writer) throws IOException {
                        assertEquals(deliveryWithRegion, value);
                        writer.addNode("delivery", "delivery");
                    }
                }
        );
        serializer.setExternalCartItemShopXmlSerializer(new ExternalCartItemXmlSerializer(null) {
            @Override
            public void serializeXml(ExternalCartItem value, PrimitiveXmlWriter writer) throws IOException {
                if (value == item1) {
                    writer.addNode("item", "item1");
                } else if (value == item2) {
                    writer.addNode("item", "item2");
                } else {
                    fail();
                }
            }
        });
        serializer.setBuyerXmlSerializer(new BuyerXmlSerializer());
    }

    @RepeatedTest(10)
    public void testSerialize() throws Exception {
        ExternalCart externalCart = prepareCart(null);

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                externalCart,
                "<cart businessId='1' currency='RUR' delivery-currency='USD' fulfilment='true' rgb='BLUE' " +
                        "preorder='true' context='SANDBOX' experiments='experiments'>" +
                        "        <delivery>delivery</delivery>" +
                        "        <items>" +
                        "            <item>item1</item>" +
                        "            <item>item2</item>" +
                        "        </items>" +
                        "    </cart>"
        );
    }

    @RepeatedTest(10)
    public void serializeWithBuyer() throws Exception {
        ExternalCart externalCart = prepareCart(ShopOrderProvider.prepareBuyer());

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                externalCart,
                "<cart businessId='1' currency='RUR' delivery-currency='USD' fulfilment='true' rgb='BLUE' " +
                        "preorder='true' context='SANDBOX' experiments='experiments'>" +
                        "        <delivery>delivery</delivery>" +
                        "        <buyer id='1234567890' last-name='Tolstoy' first-name='Leo' " +
                        "middle-name='Nikolaevich' " +
                        "personal-email-id='9e92bc743c624f958b8876c7841a653b' " +
                        "personal-full-name-id='a1c595eb35404207aecfa080f90a8986' " +
                        "personal-phone-id='c0dec0dedec0dec0dec0dec0dedec0de' " +
                        "phone='+71234567891' email='a@b.com' uid='359953025' />" +
                        "        <items>" +
                        "            <item>item1</item>" +
                        "            <item>item2</item>" +
                        "        </items>" +
                        "    </cart>"
        );
    }

    @RepeatedTest(10)
    public void testSerializeEmpty() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                new ExternalCart(),
                "<cart />"
        );
    }

    private ExternalCart prepareCart(@Nullable Buyer buyer) {
        final ExternalCart externalCart = enhancedRandomHelper.nextObject(ExternalCart.class,
                "deliveryWithRegion.parcels.route", "deliveryWithRegion.shipments.route", "deliveryWithRegion.shipment.route");
        externalCart.setBusinessId(1L);
        externalCart.setFulfilment(true);
        externalCart.setDeliveryWithRegion(deliveryWithRegion);
        externalCart.setItems(Arrays.asList(item1, item2));
        externalCart.setCurrency(Currency.RUR);
        externalCart.setDeliveryCurrency(Currency.USD);
        externalCart.setRgb(Color.BLUE);
        externalCart.setPreorder(true);
        externalCart.setContext(Context.SANDBOX);
        externalCart.setHasCertificate(false);
        externalCart.setBuyer(buyer);
        externalCart.setExperiments("experiments");
        return externalCart;
    }
}
