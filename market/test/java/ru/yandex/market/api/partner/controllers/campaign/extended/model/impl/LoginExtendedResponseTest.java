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
import ru.yandex.market.core.contact.InnerRole;
import ru.yandex.market.mbi.jaxb.jackson.ApiObjectMapperFactory;

import static org.junit.Assert.assertEquals;

/**
 * @author vbudnev
 */
public class LoginExtendedResponseTest {

    private final static ApiObjectMapperFactory fact = new ApiObjectMapperFactory();
    private final static ObjectMapper xmlMapper = fact.createXmlMapper();
    private final static ObjectMapper jsonMapper = fact.createJsonMapper();

    private LoginExtendedResponse l11;
    private LoginExtendedResponse l21;
    private LoginExtendedResponse l31;

    @Before
    public void init() {
        l11 = new LoginExtendedResponse("login1");
        l21 = new LoginExtendedResponse("login2");
        l31 = new LoginExtendedResponse("login3");

        //no roles
    }

    //TODO:
    //init all except roles
    @Test
    public void test_xml_should_renderAsTree_when_placedInApiResponse() throws IOException, SAXException, XpathException {


        String actual = xmlMapper.writeValueAsString(new ApiLoginsResponse(Arrays.asList(l11, l21, l31)));

        Document doc = XMLUnit.buildTestDocument(actual);

        XMLAssert.assertXpathExists("//response/logins/login[@login='login1']", doc);
        XMLAssert.assertXpathExists("//response/logins/login[@login='login1']", doc);
        XMLAssert.assertXpathExists("//response/logins/login[@login='login2']", doc);

    }

    @Test
    public void test_xml_shouldNot_renderRoles_when_notSet() throws IOException, SAXException, XpathException {
        String actual = xmlMapper.writeValueAsString(new ApiLoginsResponse(Arrays.asList(l11)));

        Document doc = XMLUnit.buildTestDocument(actual);

        XMLAssert.assertXpathNotExists("//response/logins/login[@login='login1']/roles", doc);
    }

    @Test
    public void test_xml_should_renderRoles_when_filled() throws IOException, XpathException, SAXException {

        l11.setRoles(Arrays.asList(InnerRole.SHOP_TECHNICAL, InnerRole.SHOP_OPERATOR));
        l21.setRoles(Arrays.asList(InnerRole.SHOP_ADMIN));

        String actual = xmlMapper.writeValueAsString(new ApiLoginsResponse(Arrays.asList(l11, l21, l31)));

        Document doc = XMLUnit.buildTestDocument(actual);

        XMLAssert.assertXpathExists("//response/logins/login[@login='login1']/roles/role[.='SHOP_TECHNICAL']", doc);
        XMLAssert.assertXpathExists("//response/logins/login[@login='login1']/roles/role[.='SHOP_OPERATOR']", doc);
        XMLAssert.assertXpathExists("//response/logins/login[@login='login2']/roles/role[.='SHOP_ADMIN']", doc);
    }


    @Test
    public void test_json_should_renderAsMap_when_placedInApiResponse() throws JsonProcessingException {

        //expected
        // {"logins":[
        //     {"login":"login1"},
        //     {"login":"login2"},
        //     {"login":"login3"}
        // ]}


        String actual = jsonMapper.writeValueAsString(new ApiLoginsResponse(Arrays.asList(l11, l21, l31)));

        List<Object> logins = JsonPath.read(actual, "$.logins");
        assertEquals(3, logins.size());
    }

    @Test
    public void test_json_shouldNot_renderRoles_when_notSet() throws JsonProcessingException {
        String actual = jsonMapper.writeValueAsString(new ApiLoginsResponse(Arrays.asList(l11, l21, l31)));

        List<Object> roles = JsonPath.read(actual, "$.logins[?(@.login=='login1')].roles.*");
        assertEquals(0, roles.size());

        roles = JsonPath.read(actual, "$.logins[?(@.login=='login2')].roles.*");
        assertEquals(0, roles.size());

        roles = JsonPath.read(actual, "$.logins[?(@.login=='login3')].roles.*");
        assertEquals(0, roles.size());
    }

    @Test
    public void test_json_should_renderRoles_when_filled() throws IOException, XpathException, SAXException {

        l11.setRoles(Arrays.asList(InnerRole.SHOP_TECHNICAL, InnerRole.SHOP_OPERATOR));
        l21.setRoles(Arrays.asList(InnerRole.SHOP_ADMIN));

        String actual = jsonMapper.writeValueAsString(new ApiLoginsResponse(Arrays.asList(l11, l21, l31)));

        List<Object> roles = JsonPath.read(actual, "$.logins[?(@.login=='login1')].roles.*");
        assertEquals(2, roles.size());

        roles = JsonPath.read(actual, "$.logins[?(@.login=='login2')].roles.*");
        assertEquals(1, roles.size());

        roles = JsonPath.read(actual, "$.logins[?(@.login=='login3')].roles.*");
        assertEquals(0, roles.size());

        roles = JsonPath.read(actual, "$..[?('SHOP_ADMIN' in @['roles'])]");
        assertEquals(1, roles.size());
    }

}