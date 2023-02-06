package ru.yandex.market.api.partner.controllers.campaign.extended.model.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
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

import ru.yandex.market.api.partner.controllers.campaign.extended.view.ApiCampaignResponse;
import ru.yandex.market.api.partner.controllers.campaign.extended.view.ApiCampaignV1CompatibilityResponse;
import ru.yandex.market.api.partner.controllers.campaign.model.cpa.StateCpa;
import ru.yandex.market.api.partner.controllers.campaign.model.cpa.StateReasonCpa;
import ru.yandex.market.api.partner.view.JacksonMapperConfig;
import ru.yandex.market.core.contact.InnerRole;

import static org.junit.Assert.assertEquals;

/**
 * @author vbudnev
 */
public class CampaignDefaultResponseTest {
    private final static JacksonMapperConfig fact = new JacksonMapperConfig();
    private final static ObjectMapper xmlMapper = fact.xmlMapper();
    private final static ObjectMapper jsonMapper = fact.jsonMapper();

    private static CampaignDefaultResponse response;

    @Before
    public void init() {
        response = new CampaignDefaultResponse(12345);
        response.setDomain("test.domain.com");
        response.setState(10);
    }

    @Test
    public void test_xml_should_renderBasicLayout_when_placedInApiResponse() throws IOException, XpathException, SAXException {
        String actual = xmlMapper.writeValueAsString(new ApiCampaignResponse(response));
        Document doc = XMLUnit.buildTestDocument(actual);

        XMLAssert.assertXpathExists("//response/campaign[@id='12345']", doc);
        XMLAssert.assertXpathExists("//response/campaign[@domain='test.domain.com']", doc);
        XMLAssert.assertXpathExists("//response/campaign[@state='10']", doc);
    }

    @Test
    public void test_xml_shouldNOT_renderStatesCpa_when_notSet() throws IOException, XpathException, SAXException {
        String actual = xmlMapper.writeValueAsString(new ApiCampaignResponse(response));

        Document doc = XMLUnit.buildTestDocument(actual);

        XMLAssert.assertXpathNotExists("//response/campaign/state-reasons-cpa", doc);
        XMLAssert.assertXpathNotExists("//response/campaign[@state-cpa]", doc);
    }

    @Test
    public void test_xml_should_renderStatesCpa_when_filled() throws IOException, XpathException, SAXException {
        response.setStateCpa(StateCpa.SANDBOX);
        response.setStateReasonsCpa(Arrays.asList(StateReasonCpa.CPA_PARTNER, StateReasonCpa.CPA_NEED_TESTING));

        String actual = xmlMapper.writeValueAsString(new ApiCampaignResponse(response));
        Document doc = XMLUnit.buildTestDocument(actual);

        XMLAssert.assertXpathExists("//response/campaign[@state-cpa='SANDBOX']", doc);
        XMLAssert.assertXpathExists("//response/campaign/state-reasons-cpa", doc);
    }

    @Test
    public void test_xml_shouldNOT_renderStates_when_notSet() throws IOException, XpathException, SAXException {
        String actual = xmlMapper.writeValueAsString(new ApiCampaignResponse(response));
        Document doc = XMLUnit.buildTestDocument(actual);

        XMLAssert.assertXpathNotExists("//response/campaign/state-reasons", doc);
    }

    @Test
    public void test_xml_should_renderStates_when_filled() throws IOException, SAXException, XpathException {
        response.setStateReasons(new HashSet<>(Arrays.asList(1, 10)));

        String actual = xmlMapper.writeValueAsString(new ApiCampaignResponse(response));
        Document doc = XMLUnit.buildTestDocument(actual);

        XMLAssert.assertXpathExists("//response/campaign/state-reasons", doc);
    }

    @Test
    public void test_xml_shouldNOT_renderRoles_when_notSet() throws IOException, XpathException, SAXException {
        String actual = xmlMapper.writeValueAsString(new ApiCampaignResponse(response));
        Document doc = XMLUnit.buildTestDocument(actual);

        XMLAssert.assertXpathNotExists("//response/campaign/roles", doc);
    }

    @Test
    public void test_xml_should_renderRoles_when_filled() throws IOException, XpathException, SAXException {
        response.setRoles(Arrays.asList(InnerRole.SHOP_ADMIN, InnerRole.SHOP_TECHNICAL));

        String actual = xmlMapper.writeValueAsString(new ApiCampaignResponse(response));
        Document doc = XMLUnit.buildTestDocument(actual);

        XMLAssert.assertXpathExists("//response/campaign/roles/role[.='SHOP_ADMIN']", doc);
        XMLAssert.assertXpathExists("//response/campaign/roles/role[.='SHOP_TECHNICAL']", doc);
    }

    @Test
    public void test_xml_should_renderApiV1RedundantTag_when_placedInApiCompatibilityResponse() throws IOException, XpathException, SAXException {
        response.setStateCpa(StateCpa.SANDBOX);
        response.setStateReasonsCpa(Arrays.asList(StateReasonCpa.CPA_PARTNER, StateReasonCpa.CPA_NEED_TESTING));
        response.setStateReasons(new HashSet<>(Arrays.asList(1, 10)));

        String actual = xmlMapper.writeValueAsString(new ApiCampaignV1CompatibilityResponse(response));
        Document doc = XMLUnit.buildTestDocument(actual);

        XMLAssert.assertXpathExists("//response/campaign/campaign[@id='12345']", doc);
        XMLAssert.assertXpathExists("//response/campaign/campaign[@domain='test.domain.com']", doc);
        XMLAssert.assertXpathExists("//response/campaign/campaign[@state='10']", doc);
        XMLAssert.assertXpathExists("//response/campaign/campaign[@state-cpa='SANDBOX']", doc);
        XMLAssert.assertXpathExists("//campaign/campaign/state-reasons-cpa", doc);
        XMLAssert.assertXpathExists("//campaign/campaign/state-reasons", doc);
    }


    @Test
    public void test_json_should_renderBasicLayout_when_placedInApiResponse() throws JsonProcessingException {

        // expected: {
        //              "campaign": {
        //                "id":12345,
        //                "domain":"test.domain.com",
        //                "state":10
        //              }
        //          }

        String actual = jsonMapper.writeValueAsString(new ApiCampaignResponse(response));

        List<Object> campaigns = JsonPath.read(actual, "$..campaign");
        assertEquals(1, campaigns.size());

        Integer actualId = JsonPath.read(actual, "$.campaign.id");
        assertEquals(Integer.valueOf(12345), actualId);

        Integer actualdState = JsonPath.read(actual, "$.campaign.state");
        assertEquals(Integer.valueOf(10), actualdState);

        String actualDomain = JsonPath.read(actual, "$.campaign.domain");
        assertEquals("test.domain.com", actualDomain);
    }

    @Test(expected = PathNotFoundException.class)
    public void test_json_shouldNot_renderRoles_when_rolesNotSet() throws JsonProcessingException {
        String actual = jsonMapper.writeValueAsString(new ApiCampaignResponse(response));
        JsonPath.read(actual, "$.campaign.roles");
    }

    @Test
    public void test_json_should_renderRoles_when_filled() throws JsonProcessingException {
        response.setRoles(Arrays.asList(InnerRole.SHOP_ADMIN, InnerRole.SHOP_TECHNICAL));

        String actual = jsonMapper.writeValueAsString(new ApiCampaignResponse(response));

        List<Object> roles = JsonPath.read(actual, "$.campaign.roles");
        assertEquals(2, roles.size());
    }

    @Test(expected = PathNotFoundException.class)
    public void test_json_shouldNOT_renderStateReasons_when_notSet() throws JsonProcessingException {
        // expected: stateReasons does not appear in resulting json, cause its not initialised
        String actual = jsonMapper.writeValueAsString(new ApiCampaignResponse(response));

        JsonPath.read(actual, "$.campaign.stateReasons");
    }

    @Test
    public void test_json_should_renderStateReasons_when_filled() throws JsonProcessingException {
        response.setStateReasons(new HashSet<>(Arrays.asList(1, 10)));

        String actual = jsonMapper.writeValueAsString(new ApiCampaignResponse(response));

        List<Object> reasons = JsonPath.read(actual, "$.campaign.stateReasons");
        assertEquals(2, reasons.size());
    }

    @Test(expected = PathNotFoundException.class)
    public void test_json_shouldNOT_renderStateReasonsCpa_when_notSet() throws JsonProcessingException {
        // expected: stateReasonsCpa does not appear in resulting json, cause its not initialised
        String actual = jsonMapper.writeValueAsString(new ApiCampaignResponse(response));

        //todo :fix cause both will throw
        JsonPath.read(actual, "$.campaign.stateReasonsCpa");
        JsonPath.read(actual, "$.campaign.cpa");
    }

    @Test
    public void test_json_should_renderStateReasonsCpa_when_filled() throws JsonProcessingException {
        response.setStateCpa(StateCpa.SANDBOX);
        response.setStateReasonsCpa(Arrays.asList(StateReasonCpa.CPA_PARTNER, StateReasonCpa.CPA_NEED_TESTING));

        String actual = jsonMapper.writeValueAsString(new ApiCampaignResponse(response));

        List<Object> reasons = JsonPath.read(actual, "$.campaign.stateReasonsCpa");
        assertEquals(2, reasons.size());

        String actualState = JsonPath.read(actual, "$.campaign.stateCpa");
        assertEquals("SANDBOX", actualState);
    }

}