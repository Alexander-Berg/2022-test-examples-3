package ru.yandex.calendar.frontend.caldav.proto.webdav.report;

import org.junit.Test;

/**
 * @author Stepan Koltsov
 */
public class ReportRequestPrincipalSearchPropertySetParserTest {

    /**
     * @url http://wiki.yandex-team.ru/calendar/re/caldav/calendarserver/iCal/init/02-report-principals
     */
    @Test
    public void parse() {
        String xml = "<x0:principal-search-property-set xmlns:x0=\"DAV:\"/>";
        ReportRequestPrincipalSearchPropertySet searchPropertySet =
                (ReportRequestPrincipalSearchPropertySet) ReportRequestParser.parseXmlString(xml);
    }

} //~
