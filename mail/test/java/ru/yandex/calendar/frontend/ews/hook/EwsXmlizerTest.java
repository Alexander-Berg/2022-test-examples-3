package ru.yandex.calendar.frontend.ews.hook;

import com.microsoft.schemas.exchange.services._2006.messages.SendNotificationResultType;
import com.microsoft.schemas.exchange.services._2006.types.SubscriptionStatusType;
import org.junit.Test;
import org.w3c.dom.Element;

import ru.yandex.misc.xml.dom.DomUtils;

/**
 * @author gutman
 */
public class EwsXmlizerTest {

    @Test
    public void unmarshal() throws Exception {
        String xml =
                "<m:SendNotification xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\" xmlns:m=\"http://schemas.microsoft.com/exchange/services/2006/messages\">\n" +
                "  <m:ResponseMessages>\n" +
                "    <m:SendNotificationResponseMessage ResponseClass=\"Success\">\n" +
                "      <m:ResponseCode>NoError</m:ResponseCode>\n" +
                "      <m:Notification>\n" +
                "        <t:SubscriptionId>FwB0ZXN0LW1haWwyLm1zZnQudmlydHVhbBAAAABzUd2ea9nSRZxccnlZiYGL</t:SubscriptionId>\n" +
                "        <t:PreviousWatermark>AQAAAE0NHLpGbA9HjwSmPXXJyw5makMAAAAAAAE=</t:PreviousWatermark>\n" +
                "        <t:MoreEvents>false</t:MoreEvents>\n" +
                "        <t:StatusEvent>\n" +
                "          <t:Watermark>AQAAAE0NHLpGbA9HjwSmPXXJyw5oakMAAAAAAAE=</t:Watermark>\n" +
                "        </t:StatusEvent>\n" +
                "      </m:Notification>\n" +
                "    </m:SendNotificationResponseMessage>\n" +
                "  </m:ResponseMessages>\n" +
                "</m:SendNotification>\n";
        EwsXmlizer.unmarshalResponse(DomUtils.I.readRootElement(xml));
    }

    @Test
    public void marshall() throws Exception {
        SendNotificationResultType result = new SendNotificationResultType();
        result.setSubscriptionStatus(SubscriptionStatusType.OK);
        Element element = EwsXmlizer.marshalResult(result).getDocumentElement();
        EwsXmlizer.unmarshalResult(element);
    }

}
