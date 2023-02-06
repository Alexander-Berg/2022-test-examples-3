package ru.yandex.calendar.frontend.caldav.proto.webdav.report;

import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.junit.Test;

import ru.yandex.calendar.frontend.caldav.proto.ccdav.AddressbookSearchOperator;
import ru.yandex.calendar.frontend.caldav.proto.webdav.WebdavConstants;
import ru.yandex.calendar.frontend.caldav.proto.webdav.xml.Prop;
import ru.yandex.calendar.logic.ics.iv5j.ical.parameter.IcsCuType;
import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 * @see ReportRequestPrincipalPropertySearch
 */
public class ReportRequestPrincipalPropertySearchParserTest {

    /**
     * @url http://wiki.yandex-team.ru/calendar/re/caldav/calendarserver/iCal/contacts#request
     */
    @Test
    public void iCal() {
        String xml =
                "<x0:principal-property-search\n" +
                "        xmlns:x2=\"urn:ietf:params:xml:ns:caldav\"\n" +
                "        xmlns:x0=\"DAV:\"\n" +
                "        xmlns:x1=\"http://calendarserver.org/ns/\"\n" +
                "        test=\"anyof\"\n" +
                "    >\n" +
                "    <x0:property-search>\n" +
                "        <x0:prop>\n" +
                "            <x0:displayname />\n" +
                "        </x0:prop>\n" +
                "        <x0:match match-type=\"starts-with\">cyr</x0:match>\n" +
                "    </x0:property-search>\n" +
                "    <x0:property-search>\n" +
                "        <x0:prop>\n" +
                "            <x1:email-address-set />\n" +
                "        </x0:prop>\n" +
                "        <x0:match match-type=\"starts-with\">cyr</x0:match>\n" +
                "    </x0:property-search>\n" +
                "    <x0:property-search>\n" +
                "        <x0:prop>\n" +
                "            <x1:first-name />\n" +
                "        </x0:prop>\n" +
                "        <x0:match match-type=\"starts-with\">cyr</x0:match>\n" +
                "    </x0:property-search>\n" +
                "    <x0:property-search>\n" +
                "        <x0:prop>\n" +
                "            <x1:last-name />\n" +
                "        </x0:prop>\n" +
                "        <x0:match match-type=\"starts-with\">cyr</x0:match>\n" +
                "    </x0:property-search>\n" +
                "    <x0:prop>\n" +
                "        <x1:email-address-set />\n" +
                "        <x2:calendar-user-address-set />\n" +
                "        <x2:calendar-user-type />\n" +
                "        <x0:displayname />\n" +
                "        <x1:last-name />\n" +
                "        <x1:first-name />\n" +
                "        <x1:record-type />\n" +
                "        <x0:principal-URL />\n" +
                "    </x0:prop>\n" +
                "</x0:principal-property-search>\n" +
                "";
        ReportRequestPrincipalPropertySearch reportRequest = (ReportRequestPrincipalPropertySearch) ReportRequestParser.parseXmlString(xml);

        Assert.A.equals(AddressbookSearchOperator.ANYOF, reportRequest.getOperator());
        Assert.A.none(reportRequest.getType());

        Assert.A.hasSize(4, reportRequest.getPropertySearches());
        Assert.A.equals("cyr", reportRequest.getPropertySearches().first().getMatch().getText());
        Assert.A.equals(WebdavConstants.DAV_DISPLAYNAME_PROP,
                (DavPropertyName) reportRequest.getPropertySearches().first().getProp().getProperties().single());

        Prop prop = reportRequest.getProp().get();
        Assert.A.hasSize(8, prop.getProperties());
    }

    /**
     * @url http://wiki.yandex-team.ru/calendar/re/caldav/YandexCalendar/iCal/location
     */
    @Test
    public void iCalLocation() {
        String xml =
                "<x0:principal-property-search xmlns:x2=\"urn:ietf:params:xml:ns:caldav\" xmlns:x0=\"DAV:\"\n" +
                "                              xmlns:x1=\"http://calendarserver.org/ns/\" type=\"ROOM\" test=\"anyof\">\n" +
                "  <x0:property-search>\n" +
                "    <x0:prop>\n" +
                "      <x0:displayname/>\n" +
                "    </x0:prop>\n" +
                "    <x0:match match-type=\"contains\">6.с</x0:match>\n" +
                "  </x0:property-search>\n" +
                "  <x0:property-search>\n" +
                "    <x0:prop>\n" +
                "      <x1:email-address-set/>\n" +
                "    </x0:prop>\n" +
                "    <x0:match match-type=\"starts-with\">6.с</x0:match>\n" +
                "  </x0:property-search>\n" +
                "  <x0:prop>\n" +
                "    <x1:email-address-set/>\n" +
                "    <x2:calendar-user-address-set/>\n" +
                "    <x2:calendar-user-type/>\n" +
                "    <x0:displayname/>\n" +
                "    <x1:last-name/>\n" +
                "    <x1:first-name/>\n" +
                "    <x1:record-type/>\n" +
                "    <x0:principal-URL/>\n" +
                "  </x0:prop>\n" +
                "</x0:principal-property-search>";


        ReportRequestPrincipalPropertySearch reportRequest = (ReportRequestPrincipalPropertySearch) ReportRequestParser.parseXmlString(xml);

        Assert.A.equals(IcsCuType.ROOM, reportRequest.getType().get().getType());
    }

    /**
     * @url http://tools.ietf.org/html/rfc3744#section-9.4.2
     */
    @Test
    public void rfc3744() {
        String xml =
                "<D:principal-property-search xmlns:D=\"DAV:\">\n" +
                "  <D:property-search>\n" +
                "    <D:prop>\n" +
                "      <D:displayname/>\n" +
                "    </D:prop>\n" +
                "    <D:match>doE</D:match>\n" +
                "  </D:property-search>\n" +
                "  <D:property-search>\n" +
                "    <D:prop xmlns:B=\"http://www.example.com/ns/\">\n" +
                "      <B:title/>\n" +
                "    </D:prop>\n" +
                "    <D:match>Sales</D:match>\n" +
                "  </D:property-search>\n" +
                "  <D:prop xmlns:B=\"http://www.example.com/ns/\">\n" +
                "    <D:displayname/>\n" +
                "    <B:department/>\n" +
                "    <B:phone/>\n" +
                "    <B:office/>\n" +
                "    <B:salary/>\n" +
                "  </D:prop>\n" +
                "</D:principal-property-search>\n" +
                "";
        ReportRequestPrincipalPropertySearch reportRequest = (ReportRequestPrincipalPropertySearch) ReportRequestParser.parseXmlString(xml);
        Assert.A.equals(AddressbookSearchOperator.ALLOF, reportRequest.getOperator());
        Assert.A.hasSize(2, reportRequest.getPropertySearches());
    }

} //~
