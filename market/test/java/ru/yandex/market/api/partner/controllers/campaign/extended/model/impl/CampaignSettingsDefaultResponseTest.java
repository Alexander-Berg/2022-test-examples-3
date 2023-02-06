package ru.yandex.market.api.partner.controllers.campaign.extended.model.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import ru.yandex.market.api.partner.controllers.campaign.extended.view.ApiCampaignSettingsResponse;
import ru.yandex.market.api.partner.view.JacksonMapperConfig;
import ru.yandex.market.core.contact.InnerRole;

import static org.junit.Assert.assertEquals;


/**
 * @author vbudnev
 */
public class CampaignSettingsDefaultResponseTest {
    private static final JacksonMapperConfig mapperConfig = new JacksonMapperConfig();
    private final static ObjectMapper xmlMapper = mapperConfig.xmlMapper();
    private final static ObjectMapper jsonMapper = mapperConfig.jsonMapper();

    private static CampaignSettingsDefaultResponse s;

    @Before
    public void init() {
        s = new CampaignSettingsDefaultResponse();
        s.setUseOpenStat(true);
        s.setShowInSnippets(false);
        s.setShowInPremium(true);
        s.setShowInContext(false);
        s.setCountryRegion(7);
        s.setOnline(true);
        s.setShopName("abc");
        //init all except roles
    }

    @Test
    public void test_xml_should_renderBasicLayout_when_placedInApiResponse() throws IOException, SAXException, XpathException {
        String actual = xmlMapper.writeValueAsString(new ApiCampaignSettingsResponse(s));
        Document doc = XMLUnit.buildTestDocument(actual);

        XMLAssert.assertXpathExists("//response/settings[@shop-name='abc']", doc);
        XMLAssert.assertXpathExists("//response/settings[@use-open-stat='true']", doc);
        XMLAssert.assertXpathExists("//response/settings[@show-in-snippets='false']", doc);
        XMLAssert.assertXpathExists("//response/settings[@show-in-premium='true']", doc);
        XMLAssert.assertXpathExists("//response/settings[@show-in-context='false']", doc);
        XMLAssert.assertXpathExists("//response/settings[@country-region='7']", doc);
        XMLAssert.assertXpathExists("//response/settings[@is-online='true']", doc);
    }

    @Test
    public void test_xml_should_renderRoles_when_filled() throws IOException, XpathException, SAXException {
        s.setRoles(Arrays.asList(InnerRole.SHOP_ADMIN, InnerRole.SHOP_TECHNICAL));

        String actual = xmlMapper.writeValueAsString(new ApiCampaignSettingsResponse(s));
        Document doc = XMLUnit.buildTestDocument(actual);
        XMLAssert.assertXpathExists("//response/settings/roles/role[.='SHOP_ADMIN']", doc);
        XMLAssert.assertXpathExists("//response/settings/roles/role[.='SHOP_TECHNICAL']", doc);
    }

    @Test
    public void test_xml_shouldNot_renderRoles_when_notSet() throws IOException, XpathException, SAXException {
        String actual = xmlMapper.writeValueAsString(new ApiCampaignSettingsResponse(s));
        Document doc = XMLUnit.buildTestDocument(actual);
        XMLAssert.assertXpathNotExists("//response/campaign/roles", doc);
    }


    @Test
    public void test_json_should_renderBasicLayout_when_placedInApiResponse() throws JsonProcessingException {
        String actual = jsonMapper.writeValueAsString(new ApiCampaignSettingsResponse(s));

        //check only existence
        JsonPath.read(actual, "$.settings.shopName");
        JsonPath.read(actual, "$.settings.useOpenStat");
        JsonPath.read(actual, "$.settings.showInSnippets");
        JsonPath.read(actual, "$.settings.showInPremium");
        JsonPath.read(actual, "$.settings.showInContext");
        JsonPath.read(actual, "$.settings.countryRegion");
        JsonPath.read(actual, "$.settings.isOnline");
    }

    @Test
    public void test_json_should_renderRoles_when_filled() throws IOException, XpathException, SAXException {
        s.setRoles(Arrays.asList(InnerRole.SHOP_ADMIN, InnerRole.SHOP_TECHNICAL));

        String actual = jsonMapper.writeValueAsString(new ApiCampaignSettingsResponse(s));
        JsonPath.read(actual, "$.settings.roles");

        List<Object> roles = JsonPath.read(actual, "$..[?('SHOP_ADMIN' in @['roles'])]");
        assertEquals(1, roles.size());

        roles = JsonPath.read(actual, "$..[?('SHOP_TECHNICAL' in @['roles'])]");
        assertEquals(1, roles.size());

    }

    @Test(expected = PathNotFoundException.class)
    public void test_json_shouldNot_renderRoles_when_notSet() throws IOException, XpathException, SAXException {
        String actual = jsonMapper.writeValueAsString(new ApiCampaignSettingsResponse(s));
        JsonPath.read(actual, "$.settings.roles");
    }
}