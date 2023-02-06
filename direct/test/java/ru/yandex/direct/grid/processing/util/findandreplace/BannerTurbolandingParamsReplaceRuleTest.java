package ru.yandex.direct.grid.processing.util.findandreplace;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsHrefParamsInstruction;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class BannerTurbolandingParamsReplaceRuleTest {

    private static final String DEFAULT_PARAM_KEY = "tyt";
    private static final String DEFAULT_PARAM_VALUE = "slon";
    private static final String DEFAULT_PARAMS = DEFAULT_PARAM_KEY + "=" + DEFAULT_PARAM_VALUE;

    @Parameterized.Parameter(0)
    public String turbolandingParams;

    @Parameterized.Parameter(1)
    public GdFindAndReplaceAdsHrefParamsInstruction instruction;

    @Parameterized.Parameter(2)
    public String expectedReplaceResult;

    @Parameterized.Parameters(name = "замена параметров в ссылке turbolandingParams={0} ; instruction={1} ; expectedReplaceResult={2}")
    public static Object[][] params() {
        return new Object[][]{
                {null,
                        new GdFindAndReplaceAdsHrefParamsInstruction()
                                .withSearchKey(DEFAULT_PARAM_KEY).withReplaceValue("param1"),
                        null},
                {DEFAULT_PARAMS,
                        new GdFindAndReplaceAdsHrefParamsInstruction()
                                .withSearchKey(DEFAULT_PARAM_KEY).withReplaceValue("param1"),
                        "param1" + "=" + DEFAULT_PARAM_VALUE},
                {DEFAULT_PARAMS,
                        new GdFindAndReplaceAdsHrefParamsInstruction()
                                .withSearchKey(DEFAULT_PARAM_VALUE).withReplaceValue("param1"),
                        DEFAULT_PARAM_KEY + "=" + "param1"},
                {DEFAULT_PARAMS,
                        new GdFindAndReplaceAdsHrefParamsInstruction()
                                .withSearchKey(DEFAULT_PARAMS).withReplaceValue("param1"),
                        "param1"},
                {DEFAULT_PARAMS,
                        new GdFindAndReplaceAdsHrefParamsInstruction()
                                .withSearchKey(DEFAULT_PARAM_KEY + "=").withReplaceValue("param1"),
                        "param1" + DEFAULT_PARAM_VALUE}
        };
    }

    @Test
    public void replaceHrefParams() {
        var replaceRule = new BannerTurbolandingParamsReplaceRule(instruction);
        assertEquals(expectedReplaceResult, replaceRule.apply(turbolandingParams));
    }
}
