package ru.yandex.calendar.frontend.caldav.proto.webdav.report;

import java.io.StringReader;

import org.junit.Test;
import org.w3c.dom.Element;

import ru.yandex.calendar.frontend.caldav.proto.caldav.CaldavConstants;
import ru.yandex.calendar.frontend.caldav.proto.webdav.WebdavConstants;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.xml.dom.DomUtils;

/**
 * @author Stepan Koltsov
 */
public class ReportRequestExpandPropertyParserTest {

    @Test
    public void parse() {
        String text =
                "<x0:expand-property xmlns:x0=\"DAV:\">\n" +
                "  <x0:property name=\"calendar-proxy-write-for\" namespace=\"http://calendarserver.org/ns/\">\n" +
                "    <x0:property name=\"displayname\" />\n" +
                "    <x0:property name=\"principal-URL\" />\n" +
                "    <x0:property name=\"calendar-user-address-set\" namespace=\"urn:ietf:params:xml:ns:caldav\" />\n" +
                "  </x0:property>\n" +
                "  <x0:property name=\"calendar-proxy-read-for\" namespace=\"http://calendarserver.org/ns/\">\n" +
                "    <x0:property name=\"displayname\" />\n" +
                "    <x0:property name=\"principal-URL\" />\n" +
                "    <x0:property name=\"calendar-user-address-set\" namespace=\"urn:ietf:params:xml:ns:caldav\" />\n" +
                "  </x0:property>\n" +
                "</x0:expand-property>";
        Element xml = DomUtils.read(new StringReader(text)).getDocumentElement();
        ReportRequestExpandProperty request = ReportRequestExpandPropertyParser.parse(xml);
        Assert.assertHasSize(2, request.getProperties());

        {
            ReportRequestExpandPropertyProperty p = request.getProperties().get(0);
            Assert.A.equals("calendar-proxy-write-for", p.getName());
            Assert.A.equals(CaldavConstants.CALENDARSERVER_NS.getURI(), p.getNamespace());

            Assert.assertHasSize(3, p.getChildren());
            {
                ReportRequestExpandPropertyProperty p1 = p.getChildren().get(0);
                Assert.A.equals("displayname", p1.getName());
                Assert.A.equals(WebdavConstants.DAV_NS.getURI(), p1.getNamespace());
            }
            {
                ReportRequestExpandPropertyProperty p1 = p.getChildren().get(2);
                Assert.A.equals("calendar-user-address-set", p1.getName());
                Assert.A.equals(CaldavConstants.CALDAV_NS.getURI(), p1.getNamespace());
            }
        }

        {
            ReportRequestExpandPropertyProperty p = request.getProperties().get(1);
            Assert.A.equals("calendar-proxy-read-for", p.getName());
            Assert.A.equals(CaldavConstants.CALENDARSERVER_NS.getURI(), p.getNamespace());
        }

    }

} //~
