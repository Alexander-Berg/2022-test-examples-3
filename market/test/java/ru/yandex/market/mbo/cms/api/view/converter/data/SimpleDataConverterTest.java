package ru.yandex.market.mbo.cms.api.view.converter.data;

import java.util.Collections;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.cms.api.view.ConversionContext;
import ru.yandex.market.mbo.cms.api.view.data.DataNode;
import ru.yandex.market.mbo.cms.api.view.data.builder.ValueBuilder;
import ru.yandex.market.mbo.cms.core.models.CmsSchema;
import ru.yandex.market.mbo.cms.core.models.Node;
import ru.yandex.market.mbo.cms.core.models.NodeBlock;
import ru.yandex.market.mbo.cms.core.models.NodeType;

public class SimpleDataConverterTest {

    public static final long ROOT_NODE_ID = 1111L;
    public static final long FIELD_ID = 2222L;
    private SimpleDataConverter simpleDataConverter;

    @Before
    public void init() {
        DataConversionDispatcher dataConversionDispatcher = Mockito.mock(DataConversionDispatcher.class);
        simpleDataConverter = new SimpleDataConverter(dataConversionDispatcher);
    }

    @Test
    public void doConvertNode() {
        Node node = new Node(new NodeType("NODE", new HashMap<>(), new HashMap<>()), 1, 1);
        NodeBlock nodeBlock = new NodeBlock();
        node.setParametersBlock(nodeBlock);
        node.addParameterValue("qqq", "eee");
        node.setId(ROOT_NODE_ID);
        ConversionContext context = new ConversionContext(new CmsSchema(), node);
        context.getDataCollection().getValues().put(FIELD_ID, ValueBuilder.aValue()
                .withValue("eee")
                .withName("qqq")
                .build());
        long nodeId = simpleDataConverter.doConvertNode(context, ROOT_NODE_ID,
                "node",
                Collections.singletonList(FIELD_ID));

        Assert.assertEquals(ROOT_NODE_ID, nodeId);
        DataNode convertedNode = context.getDataCollection().getNodes().get(nodeId);
        Assert.assertEquals(1, convertedNode.getValues().size());
        Assert.assertEquals(FIELD_ID, (long) convertedNode.getValues().get(0).getId());
        Assert.assertEquals("qqq", convertedNode.getValues().get(0).getName());
    }

    @Test
    public void convertNodeNew() {
        Node node = new Node(new NodeType("NODE", new HashMap<>(), new HashMap<>()), 1, 1);
        node.setId(0);
        ConversionContext context = new ConversionContext(new CmsSchema(), node);
        long nodeId = simpleDataConverter.convertNode(context, node);
        Assert.assertEquals(1L, nodeId);
    }

    @Test
    public void convertNodeExisted() {
        Node node = new Node(new NodeType("NODE", new HashMap<>(), new HashMap<>()), 1, 1);
        node.setId(ROOT_NODE_ID);
        ConversionContext context = new ConversionContext(new CmsSchema(), node);
        long nodeId = simpleDataConverter.convertNode(context, node);
        Assert.assertEquals(ROOT_NODE_ID, nodeId);
    }
}
