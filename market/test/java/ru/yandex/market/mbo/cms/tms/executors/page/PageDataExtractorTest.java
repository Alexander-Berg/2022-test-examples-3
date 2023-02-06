package ru.yandex.market.mbo.cms.tms.executors.page;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.cms.core.models.CmsSchema;
import ru.yandex.market.mbo.cms.core.models.Constants;
import ru.yandex.market.mbo.cms.core.models.FieldType;
import ru.yandex.market.mbo.cms.core.models.Node;
import ru.yandex.market.mbo.cms.core.models.NodeType;
import ru.yandex.market.mbo.cms.core.models.Page;
import ru.yandex.market.mbo.cms.core.service.SchemaService;

import static org.junit.Assert.assertEquals;

public class PageDataExtractorTest {

    private SchemaService schemaService;
    private PageDataExtractor pageDataExtractor;

    @Before
    public void init() {
        schemaService = Mockito.mock(SchemaService.class);
        CmsSchema schema = buildCmsSchema();
        Mockito.when(schemaService.getMasterSchema(Mockito.any())).thenReturn(schema);
        pageDataExtractor = new PageDataExtractor(schemaService);
    }

    @Test
    public void extractNode() {
        CmsSchema schema = buildCmsSchema();
        Node root = schema.makeNode("CONTEXT_ROOT")
                .addWidgetValue("ENTRYPOINTS",
                        schema.makeNode("PAGE_ENTRYPOINT")
                                .addWidgetValue("ROWS",
                                        schema.makeNode("NOT_REACT_ROW")
                                )
                )
                .addWidgetValue("CONTENT",
                        schema.makeNode("PAGE_CONTENT")
                                .addWidgetValue("ROWS",
                                        schema.makeNode("ROW_REACT"))
                                .addWidgetValue("ROWS",
                                        schema.makeNode("NOT_REACT_ROW"))
                );
        Page page = new Page();
        page.setRootNode(root);

        List<NodeView> result = pageDataExtractor.extractNodes(page);
        assertEquals(1L, result.stream().filter(o -> Boolean.TRUE.equals(o.isReact())).count());
        return;
    }

    @SuppressWarnings("checkstyle:methodlength")
    public static CmsSchema buildCmsSchema() {
        CmsSchema schema = new CmsSchema();
        schema.getSchemaIdToRevisionId().put(0L, 0L);
        Map<String, NodeType> nodeTypes = schema.getNodeTypes();
        NodeType nodeType;

        nodeType = new NodeType("CONTEXT_ROOT");
        nodeTypes.put("CONTEXT_ROOT", nodeType);
        nodeType.addProperty("label", Arrays.asList("Рут"));
        nodeType.addField("CONTENT", new FieldType("CONTENT", ImmutableMap.<String, List<String>>builder()
                .put("allowedTypes", Arrays.asList("PAGE_CONTENT"))
                .put("label", Arrays.asList("Контент"))
                .build()));
        nodeType.addField("ENTRYPOINTS", new FieldType("ENTRYPOINTS",
                ImmutableMap.<String, List<String>>builder()
                        .put("allowedTypes", Arrays.asList("PAGE_ENTRYPOINT"))
                        .put("allowedValuesMin", Arrays.asList("0"))
                        .put("allowedValuesMax", Arrays.asList("1"))
                        .put("label", Arrays.asList("Точки входа"))
                        .build()));

        nodeType = new NodeType("PAGE_CONTENT");
        nodeTypes.put("PAGE_CONTENT", nodeType);
        nodeType.addProperty("label", Arrays.asList("Контент"));
        nodeType.addField("ROWS", new FieldType("ROWS", ImmutableMap.<String, List<String>>builder()
                .put("allowedTypes", Arrays.asList("ROW_REACT", "NOT_REACT_ROW"))
                .put("allowedValuesMin", Arrays.asList("1"))
                .put("label", Arrays.asList("Строки"))
                .build()));

        nodeType = new NodeType("PAGE_ENTRYPOINT");
        nodeTypes.put("PAGE_ENTRYPOINT", nodeType);
        nodeType.addProperty("label", Arrays.asList("Энтрипойнт"));
        nodeType.addField("ROWS", new FieldType("ROWS", ImmutableMap.<String, List<String>>builder()
                .put("allowedTypes", Arrays.asList("ROW_REACT", "NOT_REACT_ROW"))
                .put("allowedValuesMin", Arrays.asList("1"))
                .put("label", Arrays.asList("Строки"))
                .build()));

        nodeType = new NodeType("ROW_REACT");
        nodeTypes.put("ROW_REACT", nodeType);
        nodeType.addProperty("label", Arrays.asList("строка реакт"));
        nodeType.addTemplate(Constants.Device.DESKTOP,
                Collections.singletonMap(Constants.Format.JSON, "\"entity\": \"box\""));

        nodeType = new NodeType("NOT_REACT_ROW");
        nodeTypes.put("NOT_REACT_ROW", nodeType);
        nodeType.addProperty("label", Arrays.asList("строка не реакт"));
        nodeType.addTemplate(Constants.Device.DESKTOP,
                Collections.singletonMap(Constants.Format.JSON, "\"entity\": \"row\""));

        return schema;
    }
}
