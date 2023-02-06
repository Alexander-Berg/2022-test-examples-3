package ru.yandex.market.api.parsers;

import com.google.common.base.Charsets;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.common.url.UrlControllerHelper;
import ru.yandex.market.api.domain.v2.redirect.parameters.SuggestUrlUserParams;
import ru.yandex.market.api.domain.v2.redirect.parameters.UrlUserParams;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.functional.Functionals;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.function.Function;

/**
 * @author dimkarp93
 */
public class UrlUserParamsParserTest extends UnitTestBase {

    /**
     * Тестируем, что если в url нет text и suggest_text отдаем обычный UrlUserParams
     */
    @Test
    public void noTextAndSuggestTextInUrl_waitTestUrlUserParamsParse() {
        String url = encode("https://market.yandex.ru/product/14206636?show-uid=865576161616220388916001&nid=54726");

        HttpServletRequest request = MockRequestBuilder.start().param("url", url).build();
        UrlUserParams params = new UrlControllerHelper.UrlUserParamsParser().get(request);

        assertUrlUserParams(new UrlUserParams(url), params);
    }

    /**
     * Тестируем, что если в url есть text отдаем обычный SuggestUrlUserParams
     */
    @Test
    public void urlContainsTextParameter_waitTestSuggestUrlUserParamsParse() {
        String text = "%D0%BA%D0%BE%D0%B6%D0%B0%D0%BD%D1%8B%D0%B5+%D0%BA%D1%80%D0%BE%D1%81%D1%81%D0%BE%D0%B2%D0%BA%D0%B8";
        String url = encode("https://m.market.yandex.ru/search?cvredirect=2&text=" + text);

        HttpServletRequest request = MockRequestBuilder.start()
            .param("url", url)
            .param("suggest_text", text)
            .build();

        UrlUserParams params = new UrlControllerHelper.UrlUserParamsParser().get(request);

        assertSuggestUrlUserParams(new SuggestUrlUserParams(url, text), params);
    }


    /**
     * Тестируем, что если в url есть text отдаем обычный SuggestUrlUserParams
     */
    @Test
    public void urlContainsTextParameter_waitUrlUserParamsParseWhenText() {
        String text = "Apple%20iPhone%207%20128Gb";
        String url = encode("https://m.market.yandex.ru/product/14206682?hid=91491&suggest_text="
            + text  + "&suggest=1&suggest_type=model");

        HttpServletRequest request = MockRequestBuilder.start()
            .param("url", url)
            .param("text", text)
            .build();

        UrlUserParams params = new UrlControllerHelper.UrlUserParamsParser().get(request);

        assertSuggestUrlUserParams(new SuggestUrlUserParams(url, text), params);
    }

    private String encode(String str) {
        Function<String, String> encode = s -> {
            try {
                return URLEncoder.encode(s, Charsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        };

        //TODO замочить двойное применение после мерджинга с изменениями из 2017.4
        return Functionals.compose(encode, encode).apply(str);
    }


    private void assertUrlUserParams(UrlUserParams expected, UrlUserParams actual) {
        Assert.assertEquals(expected.getClass(), UrlUserParams.class);
        Assert.assertEquals(expected.getUrl(), actual.getUrl());
    }

    private void assertSuggestUrlUserParams(SuggestUrlUserParams expected, UrlUserParams actualParams) {
        Assert.assertTrue(actualParams instanceof SuggestUrlUserParams);
        SuggestUrlUserParams actual = (SuggestUrlUserParams) actualParams;
        Assert.assertEquals(expected.getUrl(), actual.getUrl());
        Assert.assertEquals(expected.getText(), actual.getText());
    }

}
