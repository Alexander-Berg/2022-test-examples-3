package ru.yandex.market.api.parsers;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.controller.v2.CategoryControllerV2;
import ru.yandex.market.api.controller.v2.PageInfoParameters;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.util.parser2.ParserTestUtils;

public class LimitedCountPageInfoParserTest {

    private CategoryControllerV2.LimitedCountPageInfoParser parser;

    @Before
    public void setUp() {
        parser = new CategoryControllerV2.LimitedCountPageInfoParser();
    }

    @Test
    public void testParseValidPageInfo(){
        HttpServletRequest request = MockRequestBuilder.start()
                .param(PageInfoParameters.PAGE_PARAM_NAME, "2")
                .param(PageInfoParameters.PAGE_SIZE_PARAM_NAME, "20")
                .build();

        ParserTestUtils.assertParsed(new PageInfo(2, 20), parser.get(request));
    }

    @Test
    public void testParseNoPageInfo() {
        HttpServletRequest request = MockRequestBuilder.start().build();

        ParserTestUtils.assertParsed(new PageInfo(1, 100), parser.get(request));
    }

    @Test
    public void testParsePageOnlyPageInfo() {
        HttpServletRequest request = MockRequestBuilder.start()
                .param(PageInfoParameters.PAGE_PARAM_NAME, "2")
                .build();

        ParserTestUtils.assertParsed(new PageInfo(2, 10), parser.get(request));
    }

    @Test
    public void testParseCountOnlyPageInfo() {
        HttpServletRequest request = MockRequestBuilder.start()
                .param(PageInfoParameters.PAGE_SIZE_PARAM_NAME, "30")
                .build();

        ParserTestUtils.assertParsed(new PageInfo(1, 30), parser.get(request));
    }
}
