package ru.yandex.market.api.partner.controllers.campaign.extended.model.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import ru.yandex.market.api.partner.controllers.campaign.extended.view.ApiLoginsResponse;
import ru.yandex.market.api.partner.controllers.campaign.extended.view.ApiLoginsV1CompatibilityResponse;
import ru.yandex.market.mbi.jaxb.jackson.ApiObjectMapperFactory;

import static org.junit.Assert.assertEquals;

/**
 * @author vbudnev
 */
public class LoginDefaultResponseTest {

    private final static ApiObjectMapperFactory fact = new ApiObjectMapperFactory();
    private final static ObjectMapper xmlMapper = fact.createXmlMapper();
    private final static ObjectMapper jsonMapper = fact.createJsonMapper();

    private LoginDefaultResponse l11;
    private LoginDefaultResponse l21;
    private LoginDefaultResponse l31;

    @Before
    public void init() {
        l11 = new LoginDefaultResponse("login1");
        l21 = new LoginDefaultResponse("login2");
        l31 = new LoginDefaultResponse("login3");
    }

    @Test
    public void test_xml_should_renderAsTree_when_placedInApiResponse() throws IOException, SAXException, XpathException {
        String actual = xmlMapper.writeValueAsString(new ApiLoginsResponse(Arrays.asList(l11, l21, l31)));

        Document doc = XMLUnit.buildTestDocument(actual);

        XMLAssert.assertXpathExists("//response/logins/login[.='login1']", doc);
        XMLAssert.assertXpathExists("//response/logins/login[.='login1']", doc);
        XMLAssert.assertXpathExists("//response/logins/login[.='login2']", doc);
    }

    @Test
    public void test_xml_should_renderApiV1RedundantName_when_placedInApiCompatibilityResponse() throws IOException, XpathException, SAXException {
        String actual = xmlMapper.writeValueAsString(new ApiLoginsV1CompatibilityResponse(Arrays.asList(l11, l21, l31)));

        Document doc = XMLUnit.buildTestDocument(actual);

        XMLAssert.assertXpathExists("//response/logins/string[.='login1']", doc);
        XMLAssert.assertXpathExists("//response/logins/string[.='login1']", doc);
        XMLAssert.assertXpathExists("//response/logins/string[.='login2']", doc);
    }

    @Test
    public void test_json_should_renderAsMap_when_placedInApiResponse() throws JsonProcessingException {

        //expected
        // {"logins":[
        //     "login1",
        //     "login2",
        //     "login3"
        // ]}

        String actual = jsonMapper.writeValueAsString(new ApiLoginsResponse(Arrays.asList(l11, l21, l31)));

        List<Object> logins = JsonPath.read(actual, "$.logins");
        assertEquals(3, logins.size());
    }

}