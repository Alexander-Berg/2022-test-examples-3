package ru.yandex.market.api.partner.controllers.campaign.extended.model.impl;


import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import ru.yandex.market.api.partner.controllers.campaign.model.cpa.StateReasonCpa;
import ru.yandex.market.api.partner.view.JacksonMapperConfig;

import static org.junit.Assert.assertEquals;

/**
 * @author vbudnev
 */
public class StateReasonsCpaTest {

    private static final JacksonMapperConfig mapperConfig = new JacksonMapperConfig();

    @Test
    public void test_xml_should_renderStatesAsTagValues_when_serialisedViaXmlMapper() throws IOException, SAXException, XpathException {

        ObjectMapper objMapper = mapperConfig.xmlMapper();
        StateReasonsCpaWrapper sw = new StateReasonsCpaWrapper();

        String actual = objMapper.writeValueAsString(sw);

        Document doc = XMLUnit.buildTestDocument(actual);

        XMLAssert.assertXpathExists("//wrapper/state-reasons-cpa/state-reason-cpa[.='CPA_PARTNER']", doc);
        XMLAssert.assertXpathExists("//wrapper/state-reasons-cpa/state-reason-cpa[.='CPA_NEED_TESTING']", doc);
    }

    @Test
    public void test_json_should_renderStatesAsList_when_serialisedViaJsonMapper() throws JsonProcessingException {

        ObjectMapper objMapper = mapperConfig.jsonMapper();
        StateReasonsCpaWrapper sw = new StateReasonsCpaWrapper();

        // expected
        // {"stateReasonsCpa":["CPA_PARTNER","CPA_NEED_TESTING"]}

        String actual = objMapper.writeValueAsString(sw);

        List<Object> reasons = JsonPath.read(actual, "$.stateReasonsCpa");

        assertEquals(2, reasons.size());
    }

    @XmlRootElement(name = "wrapper")
    private static class StateReasonsCpaWrapper {
        @XmlElement(name = "stateReasonsCpa")
        private StateReasonsCpa stateReasonsCpa = new StateReasonsCpa();

        StateReasonsCpaWrapper() {
            this.stateReasonsCpa.setReasonsCpa(Arrays.asList(StateReasonCpa.CPA_PARTNER, StateReasonCpa.CPA_NEED_TESTING));
        }
    }

}