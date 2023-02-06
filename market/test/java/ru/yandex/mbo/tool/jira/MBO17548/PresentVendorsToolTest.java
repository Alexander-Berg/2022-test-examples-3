package ru.yandex.mbo.tool.jira.MBO17548;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.mbo.tool.jira.MBO17548.PresentVendorsTool.BeginNameModifier;
import ru.yandex.mbo.tool.jira.MBO17548.PresentVendorsTool.CompositeNameModifier;
import ru.yandex.mbo.tool.jira.MBO17548.PresentVendorsTool.EndNameModifier;
import ru.yandex.mbo.tool.jira.MBO17548.PresentVendorsTool.QuotesNameModifier;

/**
 * @author ayratgdl
 * @since 06.11.18
 */
public class PresentVendorsToolTest {
    @Test
    public void test1() {
        CompositeNameModifier modifier = new CompositeNameModifier(
            new BeginNameModifier("ООО "),
            new EndNameModifier(" RU"),
            new QuotesNameModifier()
        );

        Assert.assertEquals("Гротекс", modifier.modify("ООО \"Гротекс\" RU"));
    }

    @Test
    public void quitesTest() {
        QuotesNameModifier modifier = new QuotesNameModifier();
        Assert.assertEquals("aaa", modifier.modify("\"aaa\""));
    }

    @Test
    public void zaoAndRuTest() {
        CompositeNameModifier modifier = new CompositeNameModifier(
            new BeginNameModifier("ЗАО "),
            new EndNameModifier(" RU")
        );
        Assert.assertEquals("Березовский фармацевтич", modifier.modify("ЗАО Березовский фармацевтич RU"));
    }
}
