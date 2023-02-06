package ru.yandex.calendar.frontend.caldav.proto.carddav.report;

import org.junit.Test;

import ru.yandex.calendar.frontend.caldav.proto.carddav.CarddavConstants;
import ru.yandex.calendar.frontend.caldav.proto.carddav.report.PropFilterPredicate.TextMatch;
import ru.yandex.calendar.frontend.caldav.proto.carddav.report.PropFilterPredicate.TextMatch.MatchType;
import ru.yandex.calendar.frontend.caldav.proto.ccdav.AddressbookSearchOperator;
import ru.yandex.calendar.frontend.caldav.proto.webdav.WebdavConstants;
import ru.yandex.calendar.frontend.caldav.proto.webdav.report.PropertiesRequest;
import ru.yandex.calendar.frontend.caldav.proto.webdav.report.PropertiesRequest.Set;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.xml.dom.DomUtils;

/**
 * @author Stepan Koltsov
 */
public class ReportRequestAddressbookQueryParserTest {

    @Test
    public void test() {
        // copy-paste from Charles sniffing AddressBook
        String xml =
                "<C:addressbook-query xmlns:D=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:carddav\">\n" +
                "  <D:prop><D:getetag/><C:address-data/></D:prop>\n" +
                "  <C:filter test=\"anyof\">\n" +
                "    <C:prop-filter name=\"NICKNAME\" test=\"allof\">\n" +
                "      <C:text-match collation=\"i;unicode-casemap\" match-type=\"contains\">sdf</C:text-match>\n" +
                "    </C:prop-filter>\n" +
                "    <C:prop-filter name=\"FN\" test=\"allof\">\n" +
                "      <C:text-match collation=\"i;unicode-casemap\" match-type=\"contains\">sdf</C:text-match>\n" +
                "    </C:prop-filter>\n" +
                "    <C:prop-filter name=\"EMAIL\" test=\"allof\">\n" +
                "      <C:text-match collation=\"i;unicode-casemap\" match-type=\"contains\">sdf</C:text-match>\n" +
                "    </C:prop-filter>\n" +
                "  </C:filter>\n" +
                "</C:addressbook-query>";
        ReportRequestAddressbookQuery request = ReportRequestAddressbookQueryParser.parse(DomUtils.I.readRootElement(xml));
        Assert.A.equals(AddressbookSearchOperator.ANYOF, request.getCardComponentFilter().getOperator());
        Assert.A.hasSize(3, request.getCardComponentFilter().getPropFilters());

        PropFilter propFilter = request.getCardComponentFilter().getPropFilters().first();
        Assert.A.equals("NICKNAME", propFilter.getName());
        Assert.A.equals(AddressbookSearchOperator.ALLOF, propFilter.getOperator());
        Assert.A.hasSize(1, propFilter.getPredicates());

        PropFilterPredicate.TextMatch textMatch = (TextMatch) propFilter.getPredicates().single();
        Assert.A.equals("i;unicode-casemap", textMatch.getCollation());
        Assert.A.equals(MatchType.CONTAINS, textMatch.getMatchType());
        Assert.A.equals("sdf", textMatch.getText());
        Assert.A.isTrue(textMatch.isPositive());

        PropertiesRequest.Set propertiesRequest = (Set) request.getPropertiesRequest();
        Assert.A.hasSize(2, propertiesRequest.getDavPropertyNameSet().getContent());
        Assert.A.isTrue(propertiesRequest.getDavPropertyNameSet().contains(WebdavConstants.DAV_GETETAG_PROP));
        Assert.A.isTrue(propertiesRequest.getDavPropertyNameSet().contains(CarddavConstants.CARDDAV_ADDRESS_DATA_PROP));
    }

} //~
