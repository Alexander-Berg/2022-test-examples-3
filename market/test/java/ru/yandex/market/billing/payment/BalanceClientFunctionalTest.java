package ru.yandex.market.billing.payment;

import org.junit.jupiter.api.Test;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.mbi.util.MbiAsserts;

/**
 * @author zoom
 */
class BalanceClientFunctionalTest extends FunctionalTest {
    @Test
    @DbUnitDataSet(after = "BalanceClientFunctionalTest.checkNotifyClient2.after.csv")
    void checkNotifyClient2() {
        var response = FunctionalTestHelper.postForXml(getBaseUrl() + "/xmlrpc", "" +
                "<methodCall>\n" +
                "<methodName>BalanceClient.NotifyClient2</methodName>\n" +
                "<params>\n" +
                "<param>\n" +
                "<value>\n" +
                "<struct>\n" +
                "  <member>\n" +
                "    <name>ClientID</name>\n" +
                "    <value><string>4021792</string></value>\n" +
                "  </member>\n" +
                "  <member>\n" +
                "    <name>Tid</name>\n" +
                "    <value><string>20130712141215818</string></value>\n" +
                "  </member>\n" +
                "  <member>\n" +
                "    <name>OverdraftLimit</name>\n" +
                "    <value><string>230.000000</string></value>\n" +
                "  </member>\n" +
                "  <member>\n" +
                "    <name>OverdraftSpent</name>\n" +
                "    <value><string>60.00</string></value>\n" +
                "  </member>\n" +
                "  <member>\n" +
                "    <name>MinPaymentTerm</name>\n" +
                "    <value><string>2013-07-15</string></value>\n" +
                "  </member>\n" +
                "</struct>\n" +
                "</value>\n" +
                "</param>\n" +
                "</params>\n" +
                "</methodCall>"
        );

        MbiAsserts.assertXmlEquals(
                //language=xml
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<methodResponse>" +
                        "<params><param><value><array><data>" +
                        "<value><i4>0</i4></value>" +
                        "<value>Success</value>" +
                        "</data></array></value></param></params>" +
                        "</methodResponse>",
                response
        );
    }
}
