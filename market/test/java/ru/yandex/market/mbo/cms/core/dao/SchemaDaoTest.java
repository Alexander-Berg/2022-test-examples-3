package ru.yandex.market.mbo.cms.core.dao;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.models.Constants;
import ru.yandex.market.mbo.cms.core.models.KeyTemplate;
import ru.yandex.market.mbo.cms.core.models.KeyTemplates;
import ru.yandex.market.mbo.cms.core.models.NodeType;

public class SchemaDaoTest {

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void processTemplate() {
        NodeType nodeType = new NodeType("T");
        String templateSrc = "__P(T)?__ __PP(TT){1}__ __P(T)?__ __PPP?__";
        String template = "__P__ __PP__ __P__ __PPP__";
        SchemaDao.processTemplate(Constants.Device.DESKTOP, Constants.Format.JSON, templateSrc, nodeType);

        Assert.assertEquals(1, nodeType.getTemplates().size());
        Assert.assertEquals(1, nodeType.getTemplates().get(Constants.Device.DESKTOP).size());
        Assert.assertEquals(template, nodeType.getTemplates().get(Constants.Device.DESKTOP).get(Constants.Format.JSON));

        Assert.assertEquals(3, nodeType.getFields().size());
        Iterator<String> i = nodeType.getFieldsNames().iterator();

        Assert.assertEquals("P", i.next());
        Assert.assertEquals("PP", i.next());
        Assert.assertEquals("PPP", i.next());
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void processNodeJsonSchema() {
        NodeType nodeType = new NodeType("T");
        String templateSrc = "__P(T)?__ __PP(TT){1}__ __P(T)?__ __PPP?__";
        String template = "__P__ __PP__ __P__ __PPP__";
        SchemaDao.processTemplate(Constants.Device.DESKTOP, Constants.Format.JSON, templateSrc, nodeType);

        Assert.assertEquals(1, nodeType.getTemplates().size());
        Assert.assertEquals(1, nodeType.getTemplates().get(Constants.Device.DESKTOP).size());
        Assert.assertEquals(template, nodeType.getTemplates().get(Constants.Device.DESKTOP).get(Constants.Format.JSON));

        Assert.assertEquals(3, nodeType.getFields().size());
        Iterator<String> i = nodeType.getFieldsNames().iterator();

        Assert.assertEquals("P", i.next());
        Assert.assertEquals("PP", i.next());
        Assert.assertEquals("PPP", i.next());
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void enrichTemplates() {
        List<String> kt1 = Arrays.asList("a", "z");
        KeyTemplate t1 = new KeyTemplate(kt1, false, false);
        List<KeyTemplate> templates = Arrays.asList(t1);
        List<KeyTemplate> result = SchemaDao.enrichTemplates(templates);

        Set<List<String>> allTemplates = result.stream().map(kt -> kt.getTemplate()).collect(Collectors.toSet());
        Assert.assertEquals(3, allTemplates.size());
        Assert.assertTrue(allTemplates.contains(Arrays.asList("a", KeyTemplates.SPLIT_NAME_PARAM_NAME, "z")));
        Assert.assertTrue(allTemplates.contains(Arrays.asList("a", KeyTemplates.REARR_FACTORS, "z")));
        Assert.assertTrue(allTemplates.contains(kt1));
    }
}
