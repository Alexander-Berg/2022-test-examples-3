package ru.yandex.calendar.frontend.caldav.proto.caldav.report;

import java.io.StringReader;
import java.util.Collection;

import lombok.val;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.junit.Test;

import ru.yandex.calendar.frontend.caldav.proto.caldav.CaldavConstants;
import ru.yandex.calendar.frontend.caldav.proto.webdav.DavHref;
import ru.yandex.calendar.frontend.caldav.proto.webdav.WebdavConstants;
import ru.yandex.calendar.frontend.caldav.proto.webdav.report.ReportRequestParser;
import ru.yandex.misc.time.TimeUtils;
import ru.yandex.misc.xml.dom.DomUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class ReportRequestCalendarParserTest {
    @Test
    public void queryIphoneWithOnlyVevent() {
        // https://jira.yandex-team.ru/browse/CAL-2053
        val text =
                "<x0:calendar-query xmlns:x0=\"urn:ietf:params:xml:ns:caldav\" xmlns:x1=\"DAV:\">\n" +
                "    <x1:prop>\n" +
                "        <x1:getetag/>\n" +
                "        <x1:resourcetype/>\n" +
                "    </x1:prop>\n" +
                "    <x0:filter>\n" +
                "        <x0:comp-filter name=\"VCALENDAR\">\n" +
                "            <x0:comp-filter name=\"VEVENT\">\n" +
                "                <x0:time-range start=\"20100409T200000Z\"/>\n" +
                "            </x0:comp-filter>\n" +
                "        </x0:comp-filter>\n" +
                "    </x0:filter>\n" +
                "</x0:calendar-query>";
        val xml = DomUtils.read(new StringReader(text)).getDocumentElement();
        val request = (ReportRequestCalendarQuery) ReportRequestParser.parse(xml);

        val propertySet = (Collection<DavPropertyName>) request.getPropertiesRequest().getDavPropertyNameSetO().get().getContent();
        assertThat(propertySet).containsExactly(WebdavConstants.DAV_GETETAG_PROP, WebdavConstants.DAV_RESOURCETYPE_PROP);

        assertThat(request.getFilter().includesVevents()).isTrue();
        assertThat(request.getFilter().includesVtodos()).isFalse();
        val timeRange = request.getFilter().getVeventConditions().getTimeRangeCondition().getTimeRange();
        assertThat(timeRange).isEqualTo(TimeRange.start(TimeUtils.instant.parse("20100409T200000Z")));
    }

    @Test
    public void queryIphoneWithVeventAndVtodo() {
        val text =
                "<x0:calendar-query xmlns:x0=\"urn:ietf:params:xml:ns:caldav\" xmlns:x1=\"DAV:\">\n" +
                "    <x1:prop>\n" +
                "        <x1:getetag/>\n" +
                "        <x1:resourcetype/>\n" +
                "    </x1:prop>\n" +
                "    <x0:filter>\n" +
                "        <x0:comp-filter name=\"VCALENDAR\">\n" +
                "            <x0:comp-filter name=\"VEVENT\">\n" +
                "                <x0:time-range start=\"20110515T200000Z\"/>\n" +
                "            </x0:comp-filter>\n" +
                "            <x0:comp-filter name=\"VTODO\"/>\n" +
                "        </x0:comp-filter>\n" +
                "    </x0:filter>\n" +
                "</x0:calendar-query>";
        val xml = DomUtils.read(new StringReader(text)).getDocumentElement();
        val filter = ((ReportRequestCalendarQuery) ReportRequestParser.parse(xml)).getFilter();

        assertThat(filter.includesVevents()).isTrue();
        assertThat(filter.includesVtodos()).isTrue();
    }


    @Test
    public void queryIphoneWithOnlyVtodo() {
        val text =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<x0:calendar-query xmlns:x0=\"urn:ietf:params:xml:ns:caldav\" xmlns:x1=\"DAV:\">\n" +
                "    <x1:prop>\n" +
                "        <x1:getetag/>\n" +
                "        <x1:resourcetype/>\n" +
                "    </x1:prop>\n" +
                "    <x0:filter>\n" +
                "        <x0:comp-filter name=\"VCALENDAR\">\n" +
                "            <x0:comp-filter name=\"VTODO\"/>\n" +
                "        </x0:comp-filter>\n" +
                "    </x0:filter>\n" +
                "</x0:calendar-query>";
        val xml = DomUtils.read(new StringReader(text)).getDocumentElement();
        val filter = ((ReportRequestCalendarQuery) ReportRequestParser.parse(xml)).getFilter();

        assertThat(filter.includesVevents()).isFalse();
        assertThat(filter.includesVtodos()).isTrue();
    }

    @Test
    public void queryWithPropTextMatch() {
        val text =
                "<C:calendar-query xmlns:C=\"urn:ietf:params:xml:ns:caldav\">\n" +
                "    <D:prop xmlns:D=\"DAV:\">\n" +
                "        <D:getetag/>\n" +
                "        <C:calendar-data/>\n" +
                "    </D:prop>\n" +
                "    <C:filter>\n" +
                "        <C:comp-filter name=\"VCALENDAR\">\n" +
                "            <C:comp-filter name=\"VEVENT\">\n" +
                "                <C:prop-filter name=\"UID\">\n" +
                "                    <C:text-match collation=\"i;octet\"\n" +
                "                    >DC6C50A017428C5216A2F1CD@example.com</C:text-match>\n" +
                "                </C:prop-filter>\n" +
                "            </C:comp-filter>\n" +
                "        </C:comp-filter>\n" +
                "    </C:filter>\n" +
                "</C:calendar-query>\n";

        val xml = DomUtils.read(new StringReader(text)).getDocumentElement();
        val request = (ReportRequestCalendarQuery) ReportRequestParser.parse(xml);

        assertThat(request.getFilter().includesVevents()).isTrue();
        val propertyName = request.getFilter().getVeventConditions().getPropertyConditions().single().getPropertyName();
        assertThat(propertyName).isEqualTo("UID");
    }

    @Test
    public void specMultiget() {
        // http://tools.ietf.org/html/rfc4791#section-7.9
        val text =
                "<C:calendar-multiget xmlns:D=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:caldav\">\n" +
                "    <D:prop>\n" +
                "        <D:getetag/>\n" +
                "        <C:calendar-data/>\n" +
                "    </D:prop>\n" +
                "    <D:href>/bernard/work/abcd1.ics</D:href>\n" +
                "    <D:href>/bernard/work/mtg1.ics</D:href>\n" +
                "</C:calendar-multiget>";
        val xml = DomUtils.read(new StringReader(text)).getDocumentElement();
        val request = (ReportRequestCalendarMultiget) ReportRequestParser.parse(xml);

        val propertySet = (Collection<DavPropertyName>) request.getPropertiesRequest().getDavPropertyNameSetO().get().getContent();
        assertThat(propertySet).containsExactly(WebdavConstants.DAV_GETETAG_PROP, CaldavConstants.CALDAV_CALENDAR_DATA_PROP);

        val hrefs = request.getHrefs().map(DavHref.getDecodedF());
        assertThat(hrefs).containsExactly("/bernard/work/abcd1.ics", "/bernard/work/mtg1.ics");
    }

    @Test
    public void syncCollectionICal5() {
        val text =
                "<A:sync-collection xmlns:A=\"DAV:\">\n" +
                "  <A:sync-token>1323020838000</A:sync-token>\n" +
                "  <A:prop>\n" +
                "    <A:getcontenttype/>\n" +
                "    <A:getetag/>\n" +
                "  </A:prop>\n" +
                "</A:sync-collection>";
        val xml = DomUtils.read(new StringReader(text)).getDocumentElement();
        val request = (ReportRequestSyncCollection) ReportRequestParser.parse(xml);
        assertThat(request.getSyncToken().getValue()).isEqualTo("1323020838000");
        assertThat(request.getLimit()).isEmpty();
    }

    @Test
    public void syncCollectionWithLimit() {
        val text =
                "<A:sync-collection xmlns:A=\"DAV:\">\n" +
                        "  <A:sync-token>1323020838000</A:sync-token>\n" +
                        "  <A:limit>\n" +
                        "    <A:nresults>100</A:nresults>\n" +
                        "  </A:limit>\n" +
                        "  <A:prop>\n" +
                        "    <A:getcontenttype/>\n" +
                        "    <A:getetag/>\n" +
                        "  </A:prop>\n" +
                        "</A:sync-collection>";
        val xml = DomUtils.read(new StringReader(text)).getDocumentElement();
        val request = (ReportRequestSyncCollection) ReportRequestParser.parse(xml);
        assertThat(request.getSyncToken().getValue()).isEqualTo("1323020838000");
        assertThat(request.getLimit()).hasValue(100);
    }
}
