package ru.yandex.market.api.opinion;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v2.opinion.OpinionV2;
import ru.yandex.market.api.domain.v2.opinion.ShopOpinionV2;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.PagedResult;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class ShopOpinionsPageV2JsonParserTest extends UnitTestBase {

    ShopOpinionsPageV2JsonParser parser;

    @Before
    @Override
    public void setUp() throws Exception {
        parser = new ShopOpinionsPageV2JsonParser();
    }

    @Test
    public void testParseShopUserOpinions() {
        PagedResult<ShopOpinionV2> result =  parser.parse(
            ResourceHelpers.getResource("shop-opinions-page.json")
        );

        PageInfo pageInfo = result.getPageInfo();
        assertEquals(2180, (int) pageInfo.getTotalElements());
        assertEquals(436,  (int) pageInfo.getTotalPages());
        assertEquals(1, pageInfo.getNumber());
        assertEquals(5, pageInfo.getCount());

        assertEquals(5, result.getElements().size());

        assertEquals(Arrays.asList(58021335L, 58022499L, 58025223L, 58028519L, 58028941L),
                result.getElements().stream().map(OpinionV2::getId).collect(Collectors.toList()));
    }

}
