package ru.yandex.market.api.opinion;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.ResourceHelpers;

public class ModelOpinionParserTest extends UnitTestBase {

    @Test
    public void shouldExtractModelIdAsResourceIdForThreeOpinions() {
        OpinionV1Parser.Model parser = new OpinionV1Parser.Model();
        List<OpinionV1> opinions = parser.parse(
            ResourceHelpers.getResource("tree-opinions.xml")
        ).getElements();

        Assert.assertEquals(3, opinions.size());
        OpinionV1 opinion = opinions.get(0);
        Assert.assertEquals(9, opinion.getResourceId());
        opinion = opinions.get(1);
        Assert.assertEquals(8, opinion.getResourceId());
        opinion = opinions.get(2);
        Assert.assertEquals(7, opinion.getResourceId());
    }

    @Test
    public void shouldParseUsageTime() {
        OpinionV1Parser.Model parser = new OpinionV1Parser.Model();
        List<OpinionV1> opinions = parser.parse(
            ResourceHelpers.getResource("tree-opinions.xml")
        ).getElements();

        ModelOpinionV1 opinion = (ModelOpinionV1) opinions.get(0);
        Assert.assertEquals(UsageTime.UNKNOWN, opinion.getUsageTime());
        opinion = (ModelOpinionV1) opinions.get(1);
        Assert.assertEquals(UsageTime.FEW_YEARS, opinion.getUsageTime());
        opinion = (ModelOpinionV1) opinions.get(2);
        Assert.assertEquals(UsageTime.FEW_WEEKS, opinion.getUsageTime());
    }

}
