package ru.yandex.calendar.frontend.caldav.proto.carddav.report;

import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.junit.Test;

import ru.yandex.calendar.frontend.caldav.proto.carddav.CarddavConstants;
import ru.yandex.calendar.frontend.caldav.proto.webdav.WebdavConstants;
import ru.yandex.calendar.frontend.caldav.proto.webdav.report.ReportRequestParser;
import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 */
public class ReportRequestAddressbookMultigetParserTest {

    @Test
    public void test() {
        String xml =
            "<C:addressbook-multiget xmlns:D=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:carddav\">" +
            "    <D:prop>" +
            "        <D:getetag />" +
            "        <C:address-data />" +
            "    </D:prop>" +
            "    <D:href>/addressbook/nga@yandex-team.ru/addressbook/508F3BB1-66E9-4015-8CCA-A3CD9752D81F-ABSPlugin</D:href>" +
            "    <D:href>/addressbook/nga@yandex-team.ru/addressbook/E01FB76F-5A41-4645-9697-41846B3FD505-ABSPlugin</D:href>" +
            "    <D:href>/addressbook/nga@yandex-team.ru/addressbook/313FDF66-0307-4DE3-B1E9-6E24AA564965-ABSPlugin</D:href>" +
            "</C:addressbook-multiget>";
        ReportRequestAddressbookMultiget req = (ReportRequestAddressbookMultiget) ReportRequestParser.parseXmlString(xml);
        Assert.A.hasSize(3, req.getHrefs());
        Assert.A.equals("/addressbook/nga@yandex-team.ru/addressbook/E01FB76F-5A41-4645-9697-41846B3FD505-ABSPlugin",
                req.getHrefs().get(1).getDecoded());
        DavPropertyNameSet set = req.getPropertiesRequest().getDavPropertyNameSetO().get();
        Assert.A.hasSize(2, set.getContent());
        Assert.A.isTrue(set.getContent().contains(WebdavConstants.DAV_GETETAG_PROP));
        Assert.A.isTrue(set.getContent().contains(CarddavConstants.CARDDAV_ADDRESS_DATA_PROP));
    }

} //~
