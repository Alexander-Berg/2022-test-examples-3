package ru.yandex.market.api.opinion;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v2.opinion.ModelOpinionV2;
import ru.yandex.market.api.domain.v2.opinion.OpinionV2;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.PagedResult;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class ModelOpinionsPageV2JsonParserTest extends UnitTestBase {

    private ModelOpinionsPageV2JsonParser parser;

    @Before
    @Override
    public void setUp() throws Exception {
        parser = new ModelOpinionsPageV2JsonParser();
    }

    @Test
    public void testParseModelOpinionsPage() {
        PagedResult<ModelOpinionV2> result = parser.parse(ResourceHelpers.getResource("model-opinions-page.json"));

        PageInfo pageInfo = result.getPageInfo();
        assertEquals(5, pageInfo.getCount());
        assertEquals(1, pageInfo.getNumber());
        assertEquals(22, (int) pageInfo.getTotalElements());
        assertEquals(3, (int) pageInfo.getTotalPages());

        assertEquals(5, result.getElements().size());

        assertEquals(Arrays.asList(59590730L, 59439769L, 59370864L, 59310879L, 59256372L),
                result.getElements().stream().map(OpinionV2::getId).collect(Collectors.toList()));
    }
}
