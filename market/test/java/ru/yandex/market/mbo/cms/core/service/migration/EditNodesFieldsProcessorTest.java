package ru.yandex.market.mbo.cms.core.service.migration;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

public class EditNodesFieldsProcessorTest {

    @Test(expected = IllegalArgumentException.class)
    public void invalidFieldLocation1() {
        EditNodesFieldsProcessor.parseTypeAndField("[]/");
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidFieldLocation2() {
        EditNodesFieldsProcessor.parseTypeAndField("[a]/b/[c]");
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidFieldLocation3() {
        EditNodesFieldsProcessor.parseTypeAndField("[abc]/");
    }

    @Test
    public void parseTypeAndField() {
        Pair<String, String> typeAndField = EditNodesFieldsProcessor.parseTypeAndField("[abc]/def");
        Assert.assertEquals("abc", typeAndField.getLeft());
        Assert.assertEquals("def", typeAndField.getRight());
    }

    @Test
    public void parseFlowTypeAndField() {
        Pair<String, String> typeAndField = EditNodesFieldsProcessor.parseTypeAndField("[a/b/c]/def");
        Assert.assertEquals("a/b/c", typeAndField.getLeft());
        Assert.assertEquals("def", typeAndField.getRight());
    }

}
