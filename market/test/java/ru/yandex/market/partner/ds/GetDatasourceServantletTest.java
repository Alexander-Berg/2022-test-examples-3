package ru.yandex.market.partner.ds;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static ru.yandex.market.core.marketmanager.MarketManagerService.MARKET_SUPPORT_MANAGER_UID;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@DbUnitDataSet(before = "GetDatasourceServantletTest.before.csv")
class GetDatasourceServantletTest extends FunctionalTest {

    @DisplayName("Поставщик с дефолтным менеджером")
    @Test
    void testGetSupplier() throws SAXException, IOException, XpathException {
        String response = Objects.requireNonNull(
                FunctionalTestHelper.get(baseUrl + "/getDatasource?id={campaignId}", 10775).getBody());
        XMLAssert.assertXpathsEqual(
                "/node()",
                "<datasource-info>" +
                        "<id>2</id>" +
                        "<domain>test2</domain>" +
                        "<internal-name>test2</internal-name>" +
                        "<manager-id>" + MARKET_SUPPORT_MANAGER_UID + "</manager-id>" +
                        "<placement-types/>" +
                        "</datasource-info>",
                "//data/datasource-info",
                response
        );
        XMLAssert.assertXpathsEqual(
                "/node()",
                "<manager>" +
                        "<email/>" +
                        "<hosted>false</hosted" +
                        "><id>-2</id>" +
                        "<ld-login/>" +
                        "<login>market-sales-manager</login>" +
                        "<manager-type>SUPPORT</manager-type>" +
                        "<name>Служба Яндекс.Маркет</name>" +
                        "<passport-email/>" +
                        "<phone/>" +
                        "</manager>",
                "//data/manager",
                response
        );
    }

    @DisplayName("Поставщик с менеджером")
    @Test
    void testGetSupplierWithManager() throws SAXException, IOException, XpathException {
        String response = Objects.requireNonNull(
                FunctionalTestHelper.get(baseUrl + "/getDatasource?id={campaignId}", 10777).getBody());
        XMLAssert.assertXpathsEqual(
                "/node()",
                "<datasource-info>" +
                        "<id>4</id>" +
                        "<domain>test4</domain>" +
                        "<internal-name>test4</internal-name>" +
                        "<manager-id>1004</manager-id>" +
                        "<placement-types/>" +
                        "</datasource-info>",
                "//data/datasource-info",
                response
        );
        XMLAssert.assertXpathsEqual(
                "/node()",
                "<manager>" +
                        "<email>crm@email.ru</email>" +
                        "<passport-email>manp@y-t.ru</passport-email>" +
                        "<id>1004</id>" +
                        "<login>man4</login>" +
                        "<name>Manager</name>" +
                        "<hosted>false</hosted>" +
                        "<ld-login/>" +
                        "<phone/>" +
                        "<manager-type>YANDEX</manager-type>" +
                        "</manager>",
                "//data/manager",
                response
        );
    }

    @DisplayName("Поставщик с заданным индустриальным менеджером. Не должны светить его")
    @Test
    void testGetSupplierWithIndustrial() throws SAXException, IOException, XpathException {
        String response = Objects.requireNonNull(FunctionalTestHelper
                .get(baseUrl + "/getDatasource?id={campaignId}", 10776).getBody());
        XMLAssert.assertXpathsEqual(
                "/node()",
                "<datasource-info>" +
                        "<id>3</id>" +
                        "<domain>test3</domain>" +
                        "<internal-name>test3</internal-name>" +
                        "<manager-id>-2</manager-id>" +
                        "<placement-types/>" +
                        "</datasource-info>",
                "//data/datasource-info",
                response
        );
    }

    @DisplayName("Магазин без менеджера")
    @Test
    void testGetDatasource() throws SAXException, IOException, XpathException {
        String response = Objects.requireNonNull(FunctionalTestHelper
                .get(baseUrl + "/getDatasource?id={campaignId}", 10774).getBody());
        XMLAssert.assertXpathsEqual(
                "/node()",
                "<datasource-info>" +
                        "<id>1</id>" +
                        "<domain>test1</domain>" +
                        "<internal-name>test1</internal-name>" +
                        "<manager-id>-2</manager-id>" +
                        "<placement-types>" +
                        "<partner-placement-program-type>CPC</partner-placement-program-type>" +
                        "</placement-types>" +
                        "</datasource-info>",
                "//data/datasource-info",
                response
        );
        XMLAssert.assertXpathsEqual(
                "/node()",
                "<manager>" +
                        "<email/>" +
                        "<hosted>false</hosted" +
                        "><id>-2</id>" +
                        "<ld-login/>" +
                        "<login>market-sales-manager</login>" +
                        "<manager-type>SUPPORT</manager-type>" +
                        "<name>Служба Яндекс.Маркет</name>" +
                        "<passport-email/>" +
                        "<phone/>" +
                        "</manager>",
                "//data/manager",
                response
        );
    }

    @Test
    @DbUnitDataSet(before = "GetDatasourceServantletTest.testNameAgency.before.csv")
    void testNameAgency() {
        String body = FunctionalTestHelper
                .get(baseUrl + "/getDatasource?id={campaignId}", 10774).getBody();
        MbiAsserts.assertXmlEquals(
                "<data servant=\"market-payment\" version=\"0\" host=\"i109885202\" actions=\"[getDatasource]\" " +
                        "executing-time=\"[35]\">\n" +
                        "  <datasource-info>\n" +
                        "    <domain>test1</domain>\n" +
                        "    <id>1</id>\n" +
                        "    <internal-name>test1</internal-name>\n" +
                        "    <manager-id>-2</manager-id>\n" +
                        "    <placement-types>\n" +
                        "      <partner-placement-program-type>CPC</partner-placement-program-type>\n" +
                        "    </placement-types>\n" +
                        "  </datasource-info>\n" +
                        "  <manager>\n" +
                        "    <email/>\n" +
                        "    <hosted>false</hosted>\n" +
                        "    <id>10</id>\n" +
                        "    <ld-login/>\n" +
                        "    <login/>\n" +
                        "    <manager-type>AGENCY</manager-type>\n" +
                        "    <name>agencyName</name>\n" +
                        "    <passport-email/>\n" +
                        "    <phone/>\n" +
                        "  </manager>\n" +
                        "</data>",
                body,
                Set.of("executing-time", "servant", "version", "host", "action")
        );
    }

}
