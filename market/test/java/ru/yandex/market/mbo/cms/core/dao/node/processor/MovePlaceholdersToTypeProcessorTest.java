package ru.yandex.market.mbo.cms.core.dao.node.processor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.cms.core.models.CmsSchema;
import ru.yandex.market.mbo.cms.core.models.Node;
import ru.yandex.market.mbo.cms.core.models.NodeBlock;
import ru.yandex.market.mbo.cms.core.service.SchemaService;
import ru.yandex.market.mbo.cms.core.utils.OldTemplatesUtil;

import static org.junit.Assert.assertEquals;

public class MovePlaceholdersToTypeProcessorTest {

    private final Set<String> processingWidgetNames = new HashSet<>(Collections.singletonList("WA"));
    private final Set<String> processingPlaceholderNames = new HashSet<>(Arrays.asList("P2", "P3", "P4"));

    private static final String WA_TEMPLATE = "__P1__ __P7(WC)__";
    private static final String WC_TEMPLATE = "__P2__ __P3(WD)__ __P4__ __P5__";
    private static final String WD_TEMPLATE = "__P6__";

    private static final String WA = "WA";
    private static final String WC = "WC";
    private static final String WD = "WD";

    private static final String PLACEHOLDER_1 = "P1";
    private static final String PLACEHOLDER_2 = "P2";
    private static final String PLACEHOLDER_3 = "P3";
    private static final String PLACEHOLDER_4 = "P4";
    private static final String PLACEHOLDER_6 = "P6";
    private static final String PLACEHOLDER_7 = "P7";

    private static final String PLACEHOLDER_1_VALUE = "P1v";
    private static final String PLACEHOLDER_2_VALUE = "P2v";
    private static final String PLACEHOLDER_4_VALUE = "P4v";
    private static final String PLACEHOLDER_6_VALUE = "P6v";

    private static final String PLACEHOLDER_1_ANOTHER_VALUE = "P1vv";
    private static final String PLACEHOLDER_2_ANOTHER_VALUE = "P2vv";
    private static final String PLACEHOLDER_4_ANOTHER_VALUE = "P4vv";
    private static final String PLACEHOLDER_6_ANOTHER_VALUE = "P6vv";

    private static final String NAMESPACE = "abc";

    private MovePlaceholdersToTypeProcessor processor = null;

    @Before
    public void setUp() {
        SchemaService schemaService = Mockito.mock(SchemaService.class);
        CmsSchema schema = Mockito.mock(CmsSchema.class);
        Mockito.when(schema.makeNode(WC)).then(invocation -> makeNode(WC, WC_TEMPLATE));
        Mockito.when(schemaService.getMasterSchema(NAMESPACE)).thenReturn(schema);

        processor = new MovePlaceholdersToTypeProcessor(schemaService, processingWidgetNames,
                processingPlaceholderNames, PLACEHOLDER_7, WC);
    }

    @Test
    public void testTransformation() {
        Node widgetInStorage = makeNode();

        processor.process(widgetInStorage, NAMESPACE);

        List<Node> widgetsAfter = widgetInStorage.getWidgetsFirstBlock(PLACEHOLDER_7);
        assertEquals(1, widgetsAfter.size());
        assertCorrectTransformation(widgetsAfter);

        //no values on original widget
        assertEquals(PLACEHOLDER_1_VALUE, widgetInStorage.getFirstParameterValue(PLACEHOLDER_1));
        assertEquals(null, widgetInStorage.getFirstParameterValue(PLACEHOLDER_2));
        assertEquals(0, widgetInStorage.getWidgetsFirstBlock(PLACEHOLDER_3).size());
        assertEquals(null, widgetInStorage.getFirstParameterValue(PLACEHOLDER_4));
    }

    @Test
    public void testTransformationSecondTime() {
        Node widgetInStorage = makeNode();

        processor.process(widgetInStorage, NAMESPACE);
        processor.process(widgetInStorage, NAMESPACE);

        widgetInStorage.setParameterValue(PLACEHOLDER_1, PLACEHOLDER_1_ANOTHER_VALUE)
                .setParameterValue(PLACEHOLDER_2, PLACEHOLDER_2_ANOTHER_VALUE)
                .setWidgetValue(PLACEHOLDER_3, makeNode(WD, WD_TEMPLATE)
                        .setParameterValue(PLACEHOLDER_6, PLACEHOLDER_6_ANOTHER_VALUE))
                .setParameterValue(PLACEHOLDER_4, PLACEHOLDER_4_ANOTHER_VALUE);

        processor.process(widgetInStorage, NAMESPACE);
        List<Node> widgetsAfter2 = widgetInStorage.getWidgetsFirstBlock(PLACEHOLDER_7);

        assertEquals(1, widgetsAfter2.size());
        assertCorrectTransformation(widgetsAfter2);
    }

    private void assertCorrectTransformation(List<Node> widgetsAfter) {
        assertEquals(1, widgetsAfter.size());
        Node replacementWidget = widgetsAfter.get(0);
        assertEquals(WC, replacementWidget.getNodeType().getName());

        assertEquals(null, replacementWidget.getFirstParameterValue(PLACEHOLDER_1));
        assertEquals(PLACEHOLDER_2_VALUE, replacementWidget.getFirstParameterValue(PLACEHOLDER_2));
        assertEquals(PLACEHOLDER_4_VALUE, replacementWidget.getFirstParameterValue(PLACEHOLDER_4));

        assertEquals(1, replacementWidget.getWidgetsFirstBlock(PLACEHOLDER_3).size());
        assertEquals(PLACEHOLDER_6_VALUE, replacementWidget.getWidgetsFirstBlock(PLACEHOLDER_3)
                .get(0).getFirstParameterValue(PLACEHOLDER_6));
    }

    private Node makeNode() {
        Node widgetInStorage = makeNode(WA, WA_TEMPLATE)
                .setParameterValue(PLACEHOLDER_1, PLACEHOLDER_1_VALUE)
                .setParameterValue(PLACEHOLDER_2, PLACEHOLDER_2_VALUE)
                .setWidgetValue(PLACEHOLDER_3, makeNode(WD, WD_TEMPLATE)
                        .setParameterValue(PLACEHOLDER_6, PLACEHOLDER_6_VALUE))
                .setParameterValue(PLACEHOLDER_4, PLACEHOLDER_4_VALUE);

        List<Node> widgetsBefore = widgetInStorage.getWidgetsFirstBlock(PLACEHOLDER_7);
        assertEquals(0, widgetsBefore.size());

        assertEquals(PLACEHOLDER_1_VALUE, widgetInStorage.getFirstParameterValue(PLACEHOLDER_1));
        assertEquals(PLACEHOLDER_2_VALUE, widgetInStorage.getFirstParameterValue(PLACEHOLDER_2));
        assertEquals(1, widgetInStorage.getWidgetsFirstBlock(PLACEHOLDER_3).size());
        assertEquals(PLACEHOLDER_6_VALUE, widgetInStorage.getWidgetsFirstBlock(PLACEHOLDER_3).get(0)
                .getFirstParameterValue(PLACEHOLDER_6));
        assertEquals(PLACEHOLDER_4_VALUE, widgetInStorage.getFirstParameterValue(PLACEHOLDER_4));
        assertEquals(0, widgetInStorage.getWidgetsFirstBlock(PLACEHOLDER_7).size());

        return widgetInStorage;
    }

    private Node makeNode(String name, String templateAsStr) {
        Node result = new Node(OldTemplatesUtil.makeBadNodeType(name, templateAsStr, null), 0, 0);

        NodeBlock block = new NodeBlock();
        block.updatePlaceholders(result.getPlaceholders());
        result.setParametersBlock(block);

        return result;
    }
}
