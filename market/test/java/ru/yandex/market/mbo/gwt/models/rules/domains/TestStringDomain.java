/**
 *
 */
package ru.yandex.market.mbo.gwt.models.rules.domains;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Param.Type;
import ru.yandex.market.mbo.gwt.models.params.ThinCategoryParam;
import ru.yandex.market.mbo.gwt.models.rules.Domain;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleTester;
import ru.yandex.market.mbo.gwt.models.rules.StringDomain;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.WordUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.yandex.market.mbo.gwt.models.rules.ModelRuleTester.testCase;

/**
 * @author gilmulla
 *
 */
public class TestStringDomain {

    private ThinCategoryParam param;

    @Before
    public void prepareTestCase() {
        ModelRuleTester tester = testCase()
                .startParameters()
                    .startParameter()
                        .xsl("param").type(Param.Type.STRING)
                    .endParameter()
                .endParameters();
        param = tester.param("param");
    }

    @Test
    public void testAnyDomainAndAnyDomainIntersection() {
        intersection(
                StringDomain.any(param),
                StringDomain.any(param),
                StringDomain.any(param));
    }

    @Test
    public void testEmptyDomainAndEmptyDomainIntersection() {
        intersection(
                StringDomain.empty(param),
                StringDomain.empty(param),
                StringDomain.empty(param));
    }

    @Test
    public void testEmptyDomainAndAnyDomainIntersection() {
        intersection(
                StringDomain.empty(param),
                StringDomain.any(param),
                StringDomain.empty(param));
    }

    @Test
    public void testAnyDomainAndDomainWithoutEmptyValueIntersection() {
        intersection(
                StringDomain.any(param),
                StringDomain.notContainsEmpty(param),
                StringDomain.notContainsEmpty(param));
    }

    @Test
    public void testIntersectionOfTwoDomainsWithoutEmptyValues() {
        intersection(
                StringDomain.notContainsEmpty(param),
                StringDomain.notContainsEmpty(param),
                StringDomain.notContainsEmpty(param));
    }

    @Test
    public void testEmptyValueDomainAndDomainWithoutEmptyValueIntersection() {
        intersection(
                StringDomain.match(param, StringDomain.EMPTY),
                StringDomain.notContainsEmpty(param),
                StringDomain.empty(param));
    }

    @Test
    public void testEmptyAndMatchIntersection() {
        intersection(
                StringDomain.empty(param),
                StringDomain.match(param, "aaa", "bbb"),
                StringDomain.empty(param));
    }

    @Test
    public void testEmptyAndSubstringIntersection() {
        intersection(
                StringDomain.empty(param),
                StringDomain.substring(param, "aaa", "bbb"),
                StringDomain.empty(param));
    }

    @Test
    public void testMatchAndMatchIntersectionSuccess() {
        intersection(
                StringDomain.match(param, "aaa", "bbb"),
                StringDomain.match(param, "aaa", "bbb"),
                StringDomain.match(param, "aaa", "bbb"));
    }

    @Test
    public void testMatchAndMatchIntersectionFail() {
        intersection(
                StringDomain.match(param, "aaa", "bbb"),
                StringDomain.match(param, "aaa", "ccc"),
                StringDomain.empty(param));
    }

    @Test
    public void testMatchAndSubstringIntersectionSuccess1() {
        intersection(
                StringDomain.match(param, "aaa", "bbb"),
                StringDomain.substring(param, "aa", "bb"),
                StringDomain.match(param, "aaa", "bbb"));
    }

    @Test
    public void testMatchAndSubstringIntersectionSuccess2() {
        intersection(
                StringDomain.match(param, "aaa", "bbb"),
                StringDomain.substring(param, "aa", "bb", "aaa"),
                StringDomain.match(param, "aaa", "bbb"));
    }

    @Test
    public void testMatchAndSubstringIntersectionFail1() {
        intersection(
                StringDomain.match(param, "aaa", "bbb"),
                StringDomain.substring(param, "dd"),
                StringDomain.empty(param));
    }

    @Test
    public void testMatchAndSubstringIntersectionFail2() {
        intersection(
                StringDomain.match(param, "aaa", "bbb"),
                StringDomain.substring(param, "aaa", "bbbb"),
                StringDomain.empty(param));
    }

    @Test
    public void testMatchAndAnyIntersection() {
        intersection(
                StringDomain.match(param, "aaa", "bbb"),
                StringDomain.any(param),
                StringDomain.match(param, "aaa", "bbb"));
    }

    @Test
    public void testMismatchAnyIntersection() {
        intersection(
            StringDomain.mismatch(param, "aaa", "bbb"),
            StringDomain.any(param),
            StringDomain.mismatch(param, "aaa", "bbb"));
    }

    @Test
    public void testMismatchIntersection() {
        intersection(
            StringDomain.mismatch(param, "aaa", "bbb"),
            StringDomain.mismatch(param, "aaa", "ccc"),
            StringDomain.mismatch(param, "aaa", "bbb", "ccc"));
    }

    @Test
    public void testMismatchAndMatchIntersection() {
        intersection(
            StringDomain.match(param, "aaa"),
            StringDomain.mismatch(param, "ccc"),
            StringDomain.match(param, "aaa"));

        intersection(
            StringDomain.match(param, "aaa"),
            StringDomain.mismatch(param, "aaa"),
            StringDomain.empty(param));
    }

    @Test
    public void testSubstringAndSubstringIntersection1() {
        intersection(
                StringDomain.substring(param, "aaa", "bbb"),
                StringDomain.substring(param, "ccc", "ddd"),
                StringDomain.substring(param, "aaa", "bbb", "ccc", "ddd"));
    }

    @Test
    public void testSubstringAndSubstringIntersection2() {
        intersection(
                StringDomain.substring(param, "aaa", "bbb"),
                StringDomain.substring(param, "aaa", "ccc"),
                StringDomain.substring(param, "aaa", "bbb", "ccc"));
    }

    @Test
    public void testValidateNotEmptyValue() {
        validation(StringDomain.empty(param), val("aaa", "bbb"),
                "Пустая область значений");
        validation(StringDomain.match(param, StringDomain.EMPTY),
                val("aaa", "bbb"), "Значение должно отсутствовать");
        validation(StringDomain.match(param, "aaa", "bbb"), val("aaa", "bbb"), "");
        validation(StringDomain.match(param, "aaa", "bbb"), val("aaa"),
                "Значение должно быть эквивалентно: aaa; bbb");
        validation(StringDomain.substring(param, "aaa", "bbb"), val("aaab", "bbbb"), "");
        validation(StringDomain.substring(param, "aaa", "bbb"), val("aadd", "bbb"),
                "Значение должно содержать подстроки: aaa; bbb");
    }

    @Test
    public void testGetSingleValue() {
        Assert.assertNull(StringDomain.empty(param).getSingleValue());
        assertStringValues(commonAssertion(StringDomain.match(param, StringDomain.EMPTY)
                .getSingleValue()).getStringValue());
        assertStringValues(commonAssertion(StringDomain.match(param, "aaa", "bbb")
                .getSingleValue()).getStringValue(), "aaa", "bbb");
        Assert.assertNull(StringDomain.substring(param, "aaa", "bbb")
                .getSingleValue());
    }

    private void intersection(Domain domain1, Domain domain2, Domain result) {
        Assert.assertTrue(domain1.intersection(domain2).isEqual(result));
        Assert.assertTrue(domain2.intersection(domain1).isEqual(result));
    }

    private void validation(Domain domain, ParameterValue value, String expectedMessage) {
        String actualMessage = domain.validate(value);
        Assert.assertEquals(expectedMessage, actualMessage);
    }

    private ParameterValue val(String... values) {
        ParameterValue val = new ParameterValue();
        val.setParamId(param.getId());
        val.setType(Type.STRING);
        val.setXslName("param");
        if (values.length > 0) {
            List<Word> words = new ArrayList<>();
            for (String str : values) {
                words.add(new Word(1, str));
            }
            val.setStringValue(words);
        }
        return val;
    }

    private ParameterValue commonAssertion(ParameterValue val) {
        Assert.assertEquals(val.getType(), Param.Type.STRING);
        Assert.assertEquals(val.getParamId(), param.getId());
        Assert.assertEquals(val.getXslName(), param.getXslName());
        return val;
    }

    private void assertStringValues(List<Word> words, String... expected) {
        List<String> actual = WordUtil.getDefaultWords(words);
        Assert.assertTrue(
                CollectionUtils.isEqualCollection(actual, Arrays.asList(expected)));
    }
}
