package ru.yandex.market.checkout.pushapi.out.shopApi.xml;

import org.junit.jupiter.api.Test;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.pushapi.client.xml.order.BuyerXmlSerializer;
import ru.yandex.market.checkout.pushapi.providers.ShopOrderProvider;

public class BuyerXmlSerializerTest {

    private BuyerXmlSerializer buyerXmlSerializer = new BuyerXmlSerializer();

    @Test
    public void testSerializeWithPassportUid() throws Exception {
        Buyer buyer = ShopOrderProvider.prepareBuyer();

        XmlTestUtil.assertSerializeResultAndString(
                buyerXmlSerializer, buyer,
                "<buyer id='1234567890' last-name='Tolstoy' first-name='Leo' middle-name='Nikolaevich' " +
                        "personal-email-id='9e92bc743c624f958b8876c7841a653b' " +
                        "personal-full-name-id='a1c595eb35404207aecfa080f90a8986' " +
                        "personal-phone-id='c0dec0dedec0dec0dec0dec0dedec0de' " +
                        "phone='+71234567891' email='a@b.com' uid='359953025' />"
        );
    }

    @Test
    public void testSerializeWithSberId() throws Exception {
        Buyer buyer = ShopOrderProvider.prepareSberIdBuyer();

        XmlTestUtil.assertSerializeResultAndString(
                buyerXmlSerializer, buyer,
                "<buyer id='1234567890' last-name='Tolstoy' first-name='Leo' middle-name='Nikolaevich' " +
                        "personal-email-id='9e92bc743c624f958b8876c7841a653b' " +
                        "personal-full-name-id='a1c595eb35404207aecfa080f90a8986' " +
                        "personal-phone-id='c0dec0dedec0dec0dec0dec0dedec0de' " +
                        "phone='+71234567891' email='a@b.com' uid='2305843009213693951' />"
        );
    }
}
