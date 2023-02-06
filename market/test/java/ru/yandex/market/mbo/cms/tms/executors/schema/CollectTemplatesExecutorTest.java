package ru.yandex.market.mbo.cms.tms.executors.schema;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.cms.core.models.CmsSchema;
import ru.yandex.market.mbo.cms.core.models.Constants;
import ru.yandex.market.mbo.cms.core.models.Namespace;
import ru.yandex.market.mbo.cms.core.models.NodeType;
import ru.yandex.market.mbo.cms.core.service.SchemaService;

/*
 * Namespace settings:
 *
 * NAMESPACE N1
 * NODE1 - desktop.xml + phone.json
 * NODE2 - phone.xml
 *
 * NAMESPACE N2
 * NODE1 - desktop.json
 */

public class CollectTemplatesExecutorTest {

    SchemaService schemaService;
    CollectTemplatesExecutor collectTemplatesExecutor;

    @Before
    public void init() {
        schemaService = Mockito.mock(SchemaService.class);
        collectTemplatesExecutor = new CollectTemplatesExecutor(schemaService, null);
        initSchemaService();
    }

    @Test
    public void extractTemplates() {
        List<TemplateView> result = collectTemplatesExecutor.extractTemplates();
        Assert.assertEquals(4, result.size());
        Assert.assertEquals(1, result.stream()
            .filter(o -> o.getNamespace().equals("N1") &&
                o.getDevice().equals(Constants.Device.DESKTOP.getId()) &&
                o.getFormat().equals(Constants.Format.XML.getId()) &&
                o.getContent().equals("{1}")).count());
        Assert.assertEquals(1, result.stream()
            .filter(o -> o.getNamespace().equals("N1") &&
                o.getDevice().equals(Constants.Device.PHONE.getId()) &&
                o.getFormat().equals(Constants.Format.JSON.getId()) &&
                o.getContent().equals("{2}")).count());
        Assert.assertEquals(1, result.stream()
            .filter(o -> o.getNamespace().equals("N1") &&
                o.getDevice().equals(Constants.Device.PHONE.getId()) &&
                o.getFormat().equals(Constants.Format.XML.getId()) &&
                o.getContent().equals("{3}")).count());
        Assert.assertEquals(1, result.stream()
            .filter(o -> o.getNamespace().equals("N2") &&
                o.getDevice().equals(Constants.Device.DESKTOP.getId()) &&
                o.getFormat().equals(Constants.Format.JSON.getId()) &&
                o.getContent().equals("{4}")).count());
    }

    private void initSchemaService() {
        CmsSchema n1 = new CmsSchema();

        Map<String, NodeType> nodeTypes1 = new HashMap<>();
        nodeTypes1.put("NODE1", new NodeType("NODE1"));
        nodeTypes1.get("NODE1").addTemplate(Constants.Device.DESKTOP, Constants.Format.XML, "{1}");
        nodeTypes1.get("NODE1").addTemplate(Constants.Device.PHONE, Constants.Format.JSON, "{2}");
        nodeTypes1.put("NODE2", new NodeType("NODE2"));
        nodeTypes1.get("NODE2").addTemplate(Constants.Device.PHONE, Constants.Format.XML, "{3}");
        n1.setNodeTypes(nodeTypes1);

        CmsSchema n2 = new CmsSchema();
        Map<String, NodeType> nodeTypes2 = new HashMap<>();
        nodeTypes2.put("NODE1", new NodeType("NODE1"));
        nodeTypes2.get("NODE1").addTemplate(Constants.Device.DESKTOP, Constants.Format.JSON, "{4}");

        n2.setNodeTypes(nodeTypes2);

        Mockito.when(schemaService.getNamespaces()).thenReturn(
            Arrays.asList(
                    new Namespace("N1", "master", "s1", "p1", false),
                    new Namespace("N2", "master", "s2", "p2", false)
            ));
        Mockito.when(schemaService.getMasterSchema("N1")).thenReturn(n1);
        Mockito.when(schemaService.getMasterSchema("N2")).thenReturn(n2);
    }
}
