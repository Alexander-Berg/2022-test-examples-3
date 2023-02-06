package ru.yandex.calendar.frontend.ews.hook;

import java.io.ByteArrayInputStream;

import javax.xml.namespace.QName;

import org.junit.Test;
import org.w3c.dom.Element;

import ru.yandex.misc.test.Assert;
import ru.yandex.misc.xml.dom.DomUtils;

/**
 * @author gutman
 */
public class SoapXmlizerTest {

    @Test
    public void serialize() throws Exception {
        String xml = "<a/>";
        Element element = SoapXmlizer.serializeSoapResponse(DomUtils.I.read(xml));
        Assert.A.isTrue(DomUtils.nameIs(element, SoapXmlizer.ENVELOPE_QNAME));
    }

    @Test
    public void parse() throws Exception {
        String xml =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "  <soap:Header>\n" +
                "    <RequestServerVersion xmlns=\"http://schemas.microsoft.com/exchange/services/2006/messages\" Version=\"Exchange2007_SP1\"/>\n" +
                "  </soap:Header>\n" +
                "  <soap:Body>\n" +
                "    <m:SendNotification xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\" xmlns:m=\"http://schemas.microsoft.com/exchange/services/2006/messages\">\n" +
                "      <m:ResponseMessages>\n" +
                "        <m:SendNotificationResponseMessage ResponseClass=\"Success\">\n" +
                "          <m:ResponseCode>NoError</m:ResponseCode>\n" +
                "          <m:Notification>\n" +
                "            <t:SubscriptionId>FwB0ZXN0LW1haWwyLm1zZnQudmlydHVhbBAAAABzUd2ea9nSRZxccnlZiYGL</t:SubscriptionId>\n" +
                "            <t:PreviousWatermark>AQAAAE0NHLpGbA9HjwSmPXXJyw5makMAAAAAAAE=</t:PreviousWatermark>\n" +
                "            <t:MoreEvents>false</t:MoreEvents>\n" +
                "            <t:StatusEvent>\n" +
                "              <t:Watermark>AQAAAE0NHLpGbA9HjwSmPXXJyw5oakMAAAAAAAE=</t:Watermark>\n" +
                "            </t:StatusEvent>\n" +
                "          </m:Notification>\n" +
                "        </m:SendNotificationResponseMessage>\n" +
                "      </m:ResponseMessages>\n" +
                "    </m:SendNotification>\n" +
                "  </soap:Body>\n" +
                "</soap:Envelope>";
        Element element = SoapXmlizer.parseSoapRequest(new ByteArrayInputStream(xml.getBytes()));
        Assert.A.isTrue(DomUtils.nameIs(element, new QName("http://schemas.microsoft.com/exchange/services/2006/messages", "SendNotification")));
    }

}
