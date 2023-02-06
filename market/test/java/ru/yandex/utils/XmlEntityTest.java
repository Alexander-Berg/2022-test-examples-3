package ru.yandex.utils;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.io.IOException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import ru.yandex.ir.formalize.FormalizerClient;
import ru.yandex.ir.parser.formalizer.XmlEntity;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import ru.yandex.ir.util.TestUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.ir.formalize.FormalizerClient.REPORT;
import static ru.yandex.ir.formalize.FormalizerClient.UC;

public class XmlEntityTest {
    private static final String RESOURCES_PATH = TestUtil.getSrcTestResourcesPath();
    private static final int BLACK_HORSE_VALUE_ID_CONSTANT = 1;
    private static final int BLACK_MAGIC_VALUE_ID_CONSTANT = 2;
    private static final int BLACK_DIAMOND_VALUE_ID_CONSTANT = 3;
    private static final int BLACK_HOUSE_VALUE_ID_CONSTANT = 4;

    private static final int BLACK_COLOR_VALUE_ID_CONSTANT = 1;
    private static final int WHITE_COLOR_VALUE_ID_CONSTANT = 2;

    @SuppressWarnings("unchecked")
    @Test
    @Ignore
    public void test() throws ParserConfigurationException, IOException, SAXException {
        File file = new File(RESOURCES_PATH + "/category_test.xml");
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
        XmlEntity.Formalizer formalizer = XmlEntity.parseDoc(doc);

        for (XmlEntity.ConflictRules conflictRules : formalizer.conflictRules) {
            assertRule(conflictRules.rules, 0, 1, 2, BLACK_COLOR_VALUE_ID_CONSTANT, true, true, true);
            assertRule(conflictRules.rules, 1, 3, 4, WHITE_COLOR_VALUE_ID_CONSTANT, false, true, true);
            assertRule(conflictRules.rules, 2, XmlEntity.DUMMY_PARAM_ID, 4, WHITE_COLOR_VALUE_ID_CONSTANT, false,
                    true, true);
            assertRule(conflictRules.rules, 3, 3, XmlEntity.DUMMY_PARAM_ID, WHITE_COLOR_VALUE_ID_CONSTANT, false,
                    true, true);
            assertRule(conflictRules.rules, 4, 3, 4, WHITE_COLOR_VALUE_ID_CONSTANT, false, true, true);
            assertRule(conflictRules.rules, 5, 3, 4, WHITE_COLOR_VALUE_ID_CONSTANT, false, false, true);
            assertRule(conflictRules.rules, 6, 3, 4, WHITE_COLOR_VALUE_ID_CONSTANT, false, true, false);
        }
    }

    private void assertRule(List<XmlEntity.ConflictRule> rules, int idx, int masterParamId, int slaveParamId,
                            int colorValueId, boolean intersection, boolean checkExcluded, boolean checkIncluded) {
        XmlEntity.ConflictRule rule = rules.get(idx);

        assertEquals(masterParamId, rule.getMasterParamId());
        assertEquals(slaveParamId, rule.getSlaveParamId());

        List<XmlEntity.ConflictValue> excludedValues = rule.getExcludedValues();
        List<XmlEntity.ConflictValue> includedValues = rule.getIncludedValues();

        if (checkExcluded) {
            assertValues(colorValueId, excludedValues, 10 * colorValueId + BLACK_HORSE_VALUE_ID_CONSTANT,
                    10 * colorValueId + BLACK_MAGIC_VALUE_ID_CONSTANT);
        } else {
            assertEquals(0, excludedValues.size());
        }

        if (checkIncluded) {
            assertValues(colorValueId, includedValues, 10 * colorValueId + BLACK_DIAMOND_VALUE_ID_CONSTANT,
                    10 * colorValueId + BLACK_HOUSE_VALUE_ID_CONSTANT);
        } else {
            assertEquals(0, includedValues.size());
        }

        assertEquals(intersection, rule.isIntersection());
    }

    private void assertValues(int colorValueId, List<XmlEntity.ConflictValue> excludedValues, int firstValueId,
                              int secondValueId) {
        XmlEntity.ConflictValue value = excludedValues.get(0);
        assertEquals(value.getMasterValueId(), firstValueId);
        assertEquals(value.getSlaveValueId(), colorValueId);

        value = excludedValues.get(1);
        assertEquals(value.getMasterValueId(), secondValueId);
        assertEquals(value.getSlaveValueId(), colorValueId);
    }

    @Test
    public void confidentTest() throws ParserConfigurationException, IOException, SAXException {
        File file = new File(RESOURCES_PATH + "/confident_test.xml");
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
        XmlEntity.Formalizer formalizer = XmlEntity.parseDoc(doc);

        assertNotNull(formalizer);

        XmlEntity.Type confidentType = formalizer.types
                .stream()
                .filter(type -> type.id.equals(10732699))
                .findFirst().get();
        assertEquals(0b1110, confidentType.confidentIn);

        XmlEntity.Type nonConfidentType = formalizer.types
                .stream()
                .filter(type -> type.id.equals(10732698))
                .findFirst().get();
        assertEquals(0, nonConfidentType.confidentIn);
    }

    @Test
    public void clientTest() throws ParserConfigurationException, IOException, SAXException {
        File file = new File(RESOURCES_PATH + "/client_test.xml");
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
        XmlEntity.Formalizer formalizer = XmlEntity.parseDoc(doc);

        assertNotNull(formalizer);

        assertEquals(1, formalizer.categories.size());

        List<FormalizerClient> clients = formalizer.categories.get(0).clients;
        assertEquals(2, clients.size());
        assertTrue(clients.containsAll(Arrays.asList(REPORT, UC)));
    }
}
