package ru.yandex.direct.grid.processing.util.findandreplace;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsHrefParamsInstruction;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class BannerHrefParamsReplaceRuleTest {

    private static final String BASE_URL = "http://yandex.ru/";
    private static final String DEFAULT_PARAM_KEY = "tyt";
    private static final String DEFAULT_PARAM_VALUE = "slon";
    private static final String DEFAULT_PARAMS = "?" + DEFAULT_PARAM_KEY + "=" + DEFAULT_PARAM_VALUE;

    @Parameterized.Parameter(0)
    public String href;

    @Parameterized.Parameter(1)
    public GdFindAndReplaceAdsHrefParamsInstruction instruction;

    @Parameterized.Parameter(2)
    public String expectedReplaceResult;

    @Parameterized.Parameters(name = "замена параметров в ссылке href={0} ; instruction={1} ; expectedReplaceResult={2}")
    public static Object[][] params() {
        return new Object[][]{
                {null,
                        new GdFindAndReplaceAdsHrefParamsInstruction()
                                .withSearchKey(DEFAULT_PARAM_KEY).withReplaceValue("param1"),
                        null},
                {BASE_URL + DEFAULT_PARAMS,
                        new GdFindAndReplaceAdsHrefParamsInstruction()
                                .withSearchKey(DEFAULT_PARAM_KEY).withReplaceValue("param1"),
                        BASE_URL + "?" + "param1" + "=" + DEFAULT_PARAM_VALUE},
                {BASE_URL + "?" + DEFAULT_PARAM_KEY,
                        new GdFindAndReplaceAdsHrefParamsInstruction()
                                .withSearchKey(DEFAULT_PARAM_KEY).withReplaceValue("param1"),
                        BASE_URL + "?" + "param1"},
                {BASE_URL + DEFAULT_PARAMS,
                        new GdFindAndReplaceAdsHrefParamsInstruction()
                                .withSearchKey(DEFAULT_PARAM_KEY).withReplaceValue("param1"),
                        BASE_URL + "?" + "param1" + "=" + DEFAULT_PARAM_VALUE},
                {BASE_URL + DEFAULT_PARAMS + "&param2=2",
                        new GdFindAndReplaceAdsHrefParamsInstruction()
                                .withSearchKey(DEFAULT_PARAM_KEY).withReplaceValue("param1"),
                        BASE_URL + "?" + "param1" + "=" + DEFAULT_PARAM_VALUE + "&param2=2"},
                {BASE_URL,
                        new GdFindAndReplaceAdsHrefParamsInstruction()
                                .withSearchKey(null).withReplaceValue("?param1"),
                        BASE_URL  + "?param1"},
                {BASE_URL + "#with-anchor",
                        new GdFindAndReplaceAdsHrefParamsInstruction()
                                .withSearchKey(null).withReplaceValue("?param1"),
                        BASE_URL  + "?param1" + "#with-anchor"},
                {BASE_URL + DEFAULT_PARAMS + "#with-anchor",
                        new GdFindAndReplaceAdsHrefParamsInstruction()
                                .withSearchKey(null).withReplaceValue(null),
                        BASE_URL + "#with-anchor"},
                {BASE_URL + DEFAULT_PARAMS,
                        new GdFindAndReplaceAdsHrefParamsInstruction()
                                .withSearchKey(null).withReplaceValue(null),
                        BASE_URL},
                {BASE_URL,
                        new GdFindAndReplaceAdsHrefParamsInstruction()
                                .withSearchKey(null).withReplaceValue(null),
                        BASE_URL}
        };
    }

    @Test
    public void replaceHrefParams() {
        BannerHrefParamsReplaceRule replaceRule = new BannerHrefParamsReplaceRule(instruction);
        assertEquals(expectedReplaceResult, replaceRule.apply(href));
    }
}
