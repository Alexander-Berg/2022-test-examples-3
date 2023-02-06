package ru.yandex.market.crm.core.triggers;

import java.io.StringReader;

import javax.xml.transform.stream.StreamSource;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class BpmnModelTransformerTest {

    @Test
    public void testTransformDocumentWithSpecalXmlCharacters() throws Exception {
        String result = BpmnModelTransformer.transformModelToXml(new StreamSource(new StringReader(""
                + "<root>"
                + "<![CDATA[ <, >, &, ', \" ]]>"
                + "</root>"
        )));
        assertTrue("CDATA section should not be modified", result.contains("<![CDATA[ <, >, &, ', \" ]]>"));
    }

}
