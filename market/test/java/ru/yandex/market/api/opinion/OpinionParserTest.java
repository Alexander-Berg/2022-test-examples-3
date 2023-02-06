package ru.yandex.market.api.opinion;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.PagedResult;
import ru.yandex.market.api.util.ResourceHelpers;

public class OpinionParserTest extends UnitTestBase {

    @Test
    public void shouldParsePagingInfo() {
        PagedResult<OpinionV1> list = new OpinionV1Parser.Shop().parse(
            ResourceHelpers.getResource("test-pager-parsing.xml")
        );
        PageInfo pageInfo = list.getPageInfo();
        Assert.assertEquals("total", 114, (int) pageInfo.getTotal());
        Assert.assertEquals("page_no", 2, pageInfo.getPage());
    }

}
