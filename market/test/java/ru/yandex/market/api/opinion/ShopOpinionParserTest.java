package ru.yandex.market.api.opinion;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.List;

public class ShopOpinionParserTest extends UnitTestBase {

    @Test
    public void shouldParseShopIdAsResourceId() {

        List<OpinionV1> opinions = new OpinionV1Parser.Shop().parse(
            ResourceHelpers.getResource("shop-opinions.xml")
        ).getElements();

        Assert.assertEquals(2, opinions.size());
        OpinionV1 opinion = opinions.get(0);
        Assert.assertEquals(5071, opinion.getResourceId());
        opinion = opinions.get(1);
        Assert.assertEquals(91515, opinion.getResourceId());
        Assert.assertEquals(1130000001562985L, opinion.getAuthorUid());
    }

}
