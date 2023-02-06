package ru.yandex.market.api.util;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import ru.yandex.market.api.common.GuruFilterParamsParser;
import ru.yandex.market.api.common.Result;
import ru.yandex.market.api.error.ValidationError;
import ru.yandex.market.api.error.ValidationErrors;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.integration.UnitTestBase;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by yntv on 28.09.16.
 */
public class GuruFilterParamsParserTest extends BaseTest {

    private static String SAMPLE_FILTER_PARAM_NAME = "42";
    private static String SEARCH_BY_PHRASE = "-8";

    @Test
    public void notFilterParam_WaitSkipIt() {
        test(
            "notFilterParam",
            new String[]{"someValue"},
            map -> Assert.assertTrue(map.isEmpty()),
            errors -> Assert.assertTrue(errors.isEmpty())
        );
    }

    @Test
    public void filterParamWithInvalidValue_WaitError() {
        test(
            SAMPLE_FILTER_PARAM_NAME,
            new String[]{"trashValue"},
            map -> Assert.assertNotNull(map),
            errors -> Assert.assertFalse(errors.isEmpty())
        );
    }

    @Test
    public void singleIntegerNumber() {
        forFilterParam(new String[]{"42"}, parsed -> equals(parsed, "42"));
    }

    @Test
    public void singleFractionalNumber() {
        forFilterParam(new String[]{"42.42"}, parsed -> equals(parsed, "42.42"));
    }

    @Test
    public void intervalWithCommaFrom() {
        forFilterParam(new String[]{",42"}, parsed -> equals(parsed, "~42"));
    }

    @Test
    public void intervalWithCommaTo() {
        forFilterParam(new String[]{"42,"}, parsed -> equals(parsed, "42~"));
    }

    @Test
    public void fullIntervalWithComma() {
        forFilterParam(new String[]{"42,43"}, parsed -> equals(parsed, "42~43"));
    }

    @Test
    public void useLastParameterValue() {
        forFilterParam(new String[]{"42", "43"}, parsed -> equals(parsed, "43"));
    }

    @Test
    public void trueValues() {
        String[] inputs = new String[]{"1", "T", "TRUE", "Y", "YES"};
        for (String input : inputs) {
            forFilterParam(new String[]{input}, parsed -> equals(parsed, "1"));
        }
    }

    @Test
    public void falseValues() {
        String[] inputs = new String[]{"0", "F", "FALSE", "N", "NO"};
        for (String input : inputs) {
            forFilterParam(new String[]{input}, parsed -> equals(parsed, "0"));
        }
    }

    @Test
    public void searchByNotEmptyPhrase() {
        String paramName = SEARCH_BY_PHRASE;
        forSingleParam(paramName, new String[]{"queryText"}, parsed -> equals(parsed, "queryText"));
    }

    @Test
    public void searchByEmptyPhrase() {
        test(
            SEARCH_BY_PHRASE,
            new String[]{""},
            parsed -> Assert.assertTrue(parsed.isEmpty()),
            errors -> Assert.assertTrue(errors.isEmpty())
        );
    }

    @Test
    public void intervalWithTildeFrom() {
        forFilterParam(new String[]{"~42"}, parsed -> equals(parsed, "~42"));
    }

    @Test
    public void intervalWithTildeTo() {
        forFilterParam(new String[]{"42~"}, parsed -> equals(parsed, "42~"));
    }

    @Test
    public void fullIntervalWithTilde() {
        forFilterParam(new String[]{"42~43"}, parsed -> equals(parsed, "42~43"));
    }

    @Test
    public void useLowValuesFromLastInterval() {
        forFilterParam(new String[]{"42~", "44~"}, parsed -> equals(parsed, "44~"));
    }

    @Test
    public void useHiValuesFromLastInterval() {
        forFilterParam(new String[]{"~42", "~44"}, parsed -> equals(parsed, "~44"));
    }

    @Test
    public void useLowAndValuesFromLastInterval() {
        forFilterParam(new String[]{"42~43", "44~45"}, parsed -> equals(parsed, "44~45"));
    }

    private void equals(String expected, String actual) {
        Assert.assertTrue(String.format("Expected = %s, actual = %s", expected, actual),
                expected.compareTo(actual) == 0);
    }

    private void forFilterParam(String[] values, Consumer<String> assertParsedValueFunction) {
        forSingleParam(SAMPLE_FILTER_PARAM_NAME, values, parsedValue -> assertParsedValueFunction.accept(parsedValue));
    }

    private void forSingleParam(String paramName, String[] values, Consumer<String> assertFunction) {
        test(paramName, values, map -> assertFunction.accept(map.get(paramName)), errors -> {
        });
    }

    private void test(String paramName,
                      String[] paramValues,
                      Consumer<Map<String, String>> assertFunction,
                      Consumer<ValidationErrors> assertErrors) {
        ValidationErrors errors = new ValidationErrors();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameters(new HashMap<String, Object>(){{
            put(paramName, paramValues);
        }});

        Result<Map<String, String>, ValidationError> result = new GuruFilterParamsParser().get(request);
        if (!result.isOk()) {
            errors.add(result.getError());
        }

        assertFunction.accept(result.getValue());
        assertErrors.accept(errors);
    }
}
