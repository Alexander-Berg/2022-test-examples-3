package ru.yandex.market.mbo.cms.core.models;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.models.property.Property;
import ru.yandex.market.mbo.cms.core.utils.CmsDateUtil;

public class TemplatesValuesProcessorTest {

    private Node makeNode() {
        return makeNode(null, Collections.emptyMap());
    }

    private Node makeNode(String fieldName, Map<String, List<String>> fieldProperties) {
        NodeType nodeType = new NodeType();
        if (fieldName != null) {
            FieldType fieldType = new FieldType(fieldName, fieldProperties);
            nodeType.addField(fieldName, fieldType);
        }
        Node result = new Node(nodeType, 0, 0);
        NodeBlock nodeBlock = new NodeBlock();
        result.setParametersBlock(nodeBlock);
        return result;
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void process() {
        Page page = new Page("page_name", 0, 0, new DocumentDescription(), makeNode());

        long pageId = 123;
        long revisionId = 789;
        String namespace = "namespace";
        page.setName("SuperPage");
        page.setId(pageId);
        page.setRevisionId(revisionId);
        page.getDocumentDescription().setNamespace(namespace);

        String fieldName = "f1";
        Node node = makeNode(fieldName, Collections.emptyMap());
        Node node2 = makeNode(fieldName, Map.of(Property.CONTAINS_SPECIAL_VALUES.getName(), List.of("false")));
        LinkedHashSet<String> exportParamsHistory = new LinkedHashSet<>();
        Assert.assertEquals(null,
                TemplatesValuesProcessor.process(null, exportParamsHistory, page, node,
                        node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));
        Assert.assertEquals("", TemplatesValuesProcessor.process("", exportParamsHistory, page, node,
                node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));
        Assert.assertEquals("Some value", TemplatesValuesProcessor.process("Some value",
                exportParamsHistory,
                page,
                node,
                node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));
        Assert.assertEquals(null,
                TemplatesValuesProcessor.process("{{Some value}}", exportParamsHistory, page, node,
                        node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));
        Assert.assertEquals(" ",
                TemplatesValuesProcessor.process("{{Some value}} ", exportParamsHistory, page, node,
                        node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));
        Assert.assertEquals(" ",
                TemplatesValuesProcessor.process(" {{Some value}}", exportParamsHistory, page, node,
                        node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));
        Assert.assertEquals("SuperPage",
                TemplatesValuesProcessor.process("{{PAGE_NAME}}", exportParamsHistory, page, node,
                        node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));
        Assert.assertEquals(String.valueOf(pageId),
                TemplatesValuesProcessor.process("{{docId}}", exportParamsHistory, page, node,
                        node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));
        Assert.assertEquals(namespace,
                TemplatesValuesProcessor.process("{{namespace}}", exportParamsHistory, page, node,
                        node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));
        Assert.assertEquals(String.valueOf(revisionId),
                TemplatesValuesProcessor.process("{{revisionId}}", exportParamsHistory, page, node,
                        node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));

        Assert.assertEquals("SuperPage" + revisionId,
                TemplatesValuesProcessor.process("{{PAGE_NAME}}{{revisionId}}", exportParamsHistory, page, node,
                        node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));

        Assert.assertEquals(" " + pageId + " " + pageId + " " + revisionId + " ",
                TemplatesValuesProcessor.process(" {{docId}} {{docId}} {{revisionId}} ", exportParamsHistory, page,
                        node,
                        node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));

    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testNoProcessing() {
        Page page = new Page("page_name", 0, 0, new DocumentDescription(), makeNode());

        long pageId = 123;
        long revisionId = 789;
        page.setName("SuperPage");
        page.setId(pageId);
        page.setRevisionId(revisionId);

        String fieldName = "f1";
        Node node = makeNode(fieldName, Map.of(Property.CONTAINS_SPECIAL_VALUES.getName(), List.of("false")));
        LinkedHashSet<String> exportParamsHistory = new LinkedHashSet<>();
        Assert.assertEquals(null,
            TemplatesValuesProcessor.process(null, exportParamsHistory, page, node,
                node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));
        Assert.assertEquals("", TemplatesValuesProcessor.process("", exportParamsHistory, page, node,
            node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));
        Assert.assertEquals("Some value", TemplatesValuesProcessor.process("Some value",
            exportParamsHistory,
            page,
            node,
            node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));
        Assert.assertEquals("{{Some value}}",
            TemplatesValuesProcessor.process("{{Some value}}", exportParamsHistory, page, node,
                node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));
        Assert.assertEquals("{{Some value}} ",
            TemplatesValuesProcessor.process("{{Some value}} ", exportParamsHistory, page, node,
                node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));
        Assert.assertEquals(" {{Some value}}",
            TemplatesValuesProcessor.process(" {{Some value}}", exportParamsHistory, page, node,
                node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));
        Assert.assertEquals("{{PAGE_NAME}}",
            TemplatesValuesProcessor.process("{{PAGE_NAME}}", exportParamsHistory, page, node,
                node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));
        Assert.assertEquals("{{docId}}",
            TemplatesValuesProcessor.process("{{docId}}", exportParamsHistory, page, node,
                node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));
        Assert.assertEquals("{{revisionId}}",
            TemplatesValuesProcessor.process("{{revisionId}}", exportParamsHistory, page, node,
                node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));

        Assert.assertEquals("{{PAGE_NAME}}{{revisionId}}",
            TemplatesValuesProcessor.process("{{PAGE_NAME}}{{revisionId}}", exportParamsHistory, page, node,
                node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));

        Assert.assertEquals(" {{docId}} {{docId}} {{revisionId}} ",
            TemplatesValuesProcessor.process(" {{docId}} {{docId}} {{revisionId}} ", exportParamsHistory, page,
                node,
                node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));

    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void processHtml() {

        Page page = new Page(null, 0, 0, new DocumentDescription(), makeNode());

        long pageId = 123;
        long revisionId = 789;
        page.setId(pageId);
        page.setRevisionId(revisionId);

        long blockId = 456;
        String fieldName = "f1";
        Map<String, List<String>> fieldProperties = new HashMap<>();
        fieldProperties.put(Constants.TYPE_PARAM_NAME, Arrays.asList(Constants.RICH_TEXT));
        Node node = makeNode(fieldName, fieldProperties);
        LinkedHashSet<String> exportParamsHistory = new LinkedHashSet<>();

        String sourceValue1 = "page_name";
        String template1 = "{{PAGE_NAME}}";
        String targetValue1 = "<p>page_name</p>";
        page.setName(sourceValue1);

        Assert.assertEquals(targetValue1,
                TemplatesValuesProcessor.process(template1, exportParamsHistory, page, node,
                        node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));

        String sourceValue2 = "page_name";
        String template2 = "<p>{{PAGE_NAME}}</p>";
        String targetValue2 = "<p>page_name</p>";
        page.setName(sourceValue2);

        Assert.assertEquals(targetValue2,
                TemplatesValuesProcessor.process(template2, exportParamsHistory, page, node,
                        node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));

        //this case is for completeness only
        String sourceValue3 = "<p>page_name</p>";
        String template3 = "<p>{{PAGE_NAME}}</p>";
        String targetValue3 = "<p>&lt;p&gt;page_name&lt;&#x2F;p&gt;</p>";
        page.setName(sourceValue3);

        Assert.assertEquals(targetValue3,
                TemplatesValuesProcessor.process(template3, exportParamsHistory, page, node,
                        node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));


        String sourceValue5 = "строка1\nстрока2";
        String template5 = "{{PAGE_NAME}}";
        String targetValue5 = "<p>строка1</p>\n<p>строка2</p>";
        page.setName(sourceValue5);

        Assert.assertEquals(targetValue5,
                TemplatesValuesProcessor.process(template5, exportParamsHistory, page, node,
                        node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));

        String sourceValue6 = "строка1\nстрока2";
        String template6 = "<p>{{PAGE_NAME}}</p>";
        String targetValue6 = "<p>строка1</p>\n<p>строка2</p>";
        page.setName(sourceValue6);

        Assert.assertEquals(targetValue6,
                TemplatesValuesProcessor.process(template6, exportParamsHistory, page, node,
                        node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));

        //this case is for completeness only
        String sourceValue7 = "<p>строка1\nстрока2</p>";
        String template7 = "<p>{{PAGE_NAME}}</p>";
        String targetValue7 = "<p>&lt;p&gt;строка1</p>\n<p>строка2&lt;&#x2F;p&gt;</p>";
        page.setName(sourceValue7);

        Assert.assertEquals(targetValue7,
                TemplatesValuesProcessor.process(template7, exportParamsHistory, page, node,
                        node.getParametersBlock(), fieldName, CmsDateUtil::date, CmsDateUtil::dateToIso8601));

    }
}
