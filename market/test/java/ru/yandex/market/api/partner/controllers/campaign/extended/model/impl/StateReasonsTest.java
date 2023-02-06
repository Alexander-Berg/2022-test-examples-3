package ru.yandex.market.api.partner.controllers.campaign.extended.model.impl;

import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.w3c.dom.Document;

import ru.yandex.market.api.partner.view.JacksonMapperConfig;

import static org.junit.Assert.assertEquals;

/**
 * @author vbudnev
 */
public class StateReasonsTest {
    private final static JacksonMapperConfig fact = new JacksonMapperConfig();

    @Test
    public void test_xml_should_renderValuesAsTagAttributes_when_serialisedViaXmlMapper() throws Exception {

        ObjectMapper objMapper = fact.xmlMapper();
        StateReasonsWrapper sw = new StateReasonsWrapper();

        String actual = objMapper.writeValueAsString(sw);

        Document doc = XMLUnit.buildTestDocument(actual);
        XMLAssert.assertXpathExists("//wrapper/state-reasons/reason[@id='1']", doc);
        XMLAssert.assertXpathExists("//wrapper/state-reasons/reason[@id='5']", doc);
        XMLAssert.assertXpathExists("//wrapper/state-reasons/reason[@id='10']", doc);
        XMLAssert.assertXpathExists("//wrapper/state-reasons/reason[@id='100']", doc);
    }

    @Test
    public void test_json_should_renderValuesAsList_when_serialisedViaJsonMapper() throws JsonProcessingException, ParseException {

        ObjectMapper objMapper = fact.jsonMapper();
        StateReasonsWrapper sw = new StateReasonsWrapper();

        // expected
        // {"stateReasons":[1,5,10,100]}

        String actual = objMapper.writeValueAsString(sw);

        List<Object> reasons = JsonPath.read(actual, "$.stateReasons");

        assertEquals(4, reasons.size());
    }

    @XmlRootElement(name = "wrapper")
    private static class StateReasonsWrapper {
        @XmlElement(name = "stateReasons")
        private StateReasons stateReasons = new StateReasons();

        StateReasonsWrapper() {
            this.stateReasons.setReasons(new HashSet<>(Arrays.asList(1, 5, 10, 100)));
        }
    }

}