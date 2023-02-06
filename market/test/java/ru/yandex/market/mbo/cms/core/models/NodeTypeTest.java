package ru.yandex.market.mbo.cms.core.models;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.models.property.Property;

public class NodeTypeTest {

    @Test
    public void testMakeTemplateForJsonEmpty() {
        var nt = new NodeType("NODE", new HashMap<>(), new HashMap<>());
        Assert.assertEquals("{}", nt.makeTemplateForJson(false).toString());
    }

    @Test
    public void testMakeTemplateForJsonFields1() {
        var nt = new NodeType("NODE", new HashMap<>(), new HashMap<>());
        nt.addField("f1", new FieldType("f1"));
        Assert.assertEquals("{\"f1\":__f1__}", nt.makeTemplateForJson(false).toString());
    }

    @Test
    public void testMakeTemplateForJsonFields2() {
        var nt = new NodeType("NODE", new HashMap<>(), new HashMap<>());
        nt.addField("f1", new FieldType("f1"));
        nt.addField("f2", new FieldType("f2"));
        Assert.assertEquals("{\"f1\":__f1__,\"f2\":__f2__}", nt.makeTemplateForJson(false).toString());
    }

    @Test
    public void testMakeTemplateForJsonFields2Internal() {
        var nt = new NodeType("NODE", new HashMap<>(), new HashMap<>());
        nt.addField("f1", new FieldType("f1"));
        nt.addField("f2", new FieldType("f2", Map.of(Property.INTERNAL.getName(), List.of("true"))));
        Assert.assertEquals("{\"f1\":__f1__}", nt.makeTemplateForJson(false).toString());
    }

    @Test
    public void testMakeTemplateForJsonFields2Multiple() {
        var nt = new NodeType("NODE", new HashMap<>(), new HashMap<>());
        nt.addField("f1", new FieldType("f1"));
        var f2 = new FieldType("f2");
        f2.setValuesLimitMax(2);
        nt.addField("f2", f2);
        Assert.assertEquals("{\"f1\":__f1__,\"f2\":[__f2__]}", nt.makeTemplateForJson(false).toString());
    }

    @Test
    public void testMakeTemplateForJsonFields2MultipleNoArray() {
        var nt = new NodeType("NODE", new HashMap<>(), new HashMap<>());
        nt.addField("f1", new FieldType("f1"));
        var f2 = new FieldType("f2");
        f2.setValuesLimitMax(2);
        f2.setWrapMultipleIntoArray(false);
        nt.addField("f2", f2);
        Assert.assertEquals("{\"f1\":__f1__,\"f2\":__f2__}", nt.makeTemplateForJson(false).toString());
    }

    @Test
    public void testMakeTemplateForJsonFields2MultipleAsObject() {
        var nt = new NodeType("NODE", new HashMap<>(), new HashMap<>());
        nt.addField("f1", new FieldType("f1"));
        var f2 = new FieldType("f2");
        f2.setValuesLimitMax(2);
        f2.setSerializeMultipleAsObject(true);
        nt.addField("f2", f2);
        Assert.assertEquals("{\"f1\":__f1__,\"f2\":{__f2__}}", nt.makeTemplateForJson(false).toString());
    }

    @Test
    public void testMakeTemplateForJsonFields2FieldAsArray() {
        var nt = new NodeType("NODE", new HashMap<>(), new HashMap<>());
        nt.addField("f1", new FieldType("f1"));
        var f2 = new FieldType("f2");
        f2.setSerializeAsArray(true);
        nt.addField("f2", f2);
        Assert.assertEquals("{\"f1\":__f1__,\"f2\":[__f2__]}", nt.makeTemplateForJson(false).toString());
    }

    @Test
    public void testMakeTemplateForJsonFields2NodeAsArray() {
        var nt = new NodeType("NODE", new HashMap<>(), new HashMap<>());
        nt.setSerializeAsArray(true);
        nt.addField("f1", new FieldType("f1"));
        var f2 = new FieldType("f2");
        f2.setSerializeAsArray(true);
        nt.addField("f2", f2);
        Assert.assertEquals("[__f1__,[__f2__]]", nt.makeTemplateForJson(false).toString());
    }

    @Test
    public void testMakeTemplateForJsonFlowParser() {
        NodeType nodeType = new NodeType();
        nodeType.addField("a", new FieldType("a"));
        FieldType multiValuedField = new FieldType("b");
        multiValuedField.setValuesLimitMax(2);
        nodeType.addField(multiValuedField.getName(), multiValuedField);

        String template = nodeType.makeTemplateForJson(true).toString();
        Assert.assertEquals("{\n  \"a\": __a__,\n  \"b\": [__b__]\n}", template);
    }

    //XML

    @Test
    public void testMakeTemplateForXmlEmpty() {
        var nt = new NodeType("NODE", new HashMap<>(), new HashMap<>());
        Assert.assertEquals("<NODE></NODE>", nt.makeTemplateForXml().toString());
    }

    @Test
    public void testMakeTemplateForXmlFields1() {
        var nt = new NodeType("NODE", new HashMap<>(), new HashMap<>());
        nt.addField("f1", new FieldType("f1"));
        Assert.assertEquals("<NODE><f1>__f1__</f1></NODE>", nt.makeTemplateForXml().toString());
    }

    @Test
    public void testMakeTemplateForXmlFields2() {
        var nt = new NodeType("NODE", new HashMap<>(), new HashMap<>());
        nt.addField("f1", new FieldType("f1"));
        nt.addField("f2", new FieldType("f2"));
        Assert.assertEquals("<NODE><f1>__f1__</f1><f2>__f2__</f2></NODE>", nt.makeTemplateForXml().toString());
    }

    @Test
    public void testMakeTemplateForXmlFields2Internal() {
        var nt = new NodeType("NODE", new HashMap<>(), new HashMap<>());
        nt.addField("f1", new FieldType("f1"));
        nt.addField("f2", new FieldType("f2", Map.of(Property.INTERNAL.getName(), List.of("true"))));
        Assert.assertEquals("<NODE><f1>__f1__</f1></NODE>", nt.makeTemplateForXml().toString());
    }

    @Test
    public void testMakeTemplateForXmlFields2Multiple() {
        var nt = new NodeType("NODE", new HashMap<>(), new HashMap<>());
        nt.addField("f1", new FieldType("f1"));
        var f2 = new FieldType("f2");
        f2.setValuesLimitMax(2);
        nt.addField("f2", f2);
        Assert.assertEquals("<NODE><f1>__f1__</f1><f2>__f2__</f2></NODE>", nt.makeTemplateForXml().toString());
    }
}
