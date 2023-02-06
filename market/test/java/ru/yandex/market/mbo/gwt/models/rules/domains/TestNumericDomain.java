/**
 *
 */
package ru.yandex.market.mbo.gwt.models.rules.domains;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Param.Type;
import ru.yandex.market.mbo.gwt.models.params.ThinCategoryParam;
import ru.yandex.market.mbo.gwt.models.rules.Domain;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleTester;
import ru.yandex.market.mbo.gwt.models.rules.NumericDomain;

import java.math.BigDecimal;

import static ru.yandex.market.mbo.gwt.models.rules.ModelRuleTester.testCase;

/**
 * @author gilmulla
 *
 */
@SuppressWarnings("magicnumber")
public class TestNumericDomain {
    private ThinCategoryParam param;

    @Before
    public void prepareTestCase() {
        ModelRuleTester tester = testCase()
                .startParameters()
                    .startParameter()
                        .xsl("param").type(Param.Type.NUMERIC)
                    .endParameter()
                .endParameters();
        param = tester.param("param");
    }

    @Test
    public void testAnyDomainAndAnyDomainIntersection() {
        intersection(
                NumericDomain.any(param),
                NumericDomain.any(param),
                NumericDomain.any(param));
    }

    @Test
    public void testAnyDomainAndEmptyDomainIntersection() {
        intersection(
                NumericDomain.any(param),
                NumericDomain.empty(param),
                NumericDomain.empty(param));
    }

    @Test
    public void testAnyDomainAndDomainWithoutEmptyValueIntersection() {
        intersection(
                NumericDomain.any(param),
                NumericDomain.notContainsEmpty(param),
                NumericDomain.notContainsEmpty(param));
    }

    @Test
    public void testEmptyValueDomainAndDomainWithoutEmptyValueIntersection() {
        intersection(
                NumericDomain.singleEmptyValue(param),
                NumericDomain.notContainsEmpty(param),
                NumericDomain.empty(param));
    }

    @Test
    public void testEmptyDomainAndEmptyDomainIntersection() {
        intersection(
                NumericDomain.empty(param),
                NumericDomain.empty(param),
                NumericDomain.empty(param));
    }

    @Test
    public void testEmptyAndSingleEmptyIntersection() {
        intersection(
                NumericDomain.empty(param),
                NumericDomain.singleEmptyValue(param),
                NumericDomain.empty(param));
    }

    @Test
    public void testEmptyAndRangeIntersection() {
        intersection(
                NumericDomain.empty(param),
                NumericDomain.range(param, 80, 120),
                NumericDomain.empty(param));
    }

    @Test
    public void testSingleEmptyAndRangeIntersection() {
        intersection(
                NumericDomain.singleEmptyValue(param),
                NumericDomain.range(param, 80, 120),
                NumericDomain.empty(param));
    }

    @Test
    public void testSingleEmptyAndEmptyPlusRangeIntersection() {
        intersection(
                NumericDomain.singleEmptyValue(param),
                NumericDomain.rangeAndEmpty(param, 80, 120),
                NumericDomain.singleEmptyValue(param));
    }

    @Test
    public void testRangeAndRangeIntersection1() {
        intersection(
            NumericDomain.range(param, 10, 100),
            NumericDomain.range(param, 80, 120),
            NumericDomain.range(param, 80, 100));
    }

    @Test
    public void testRangeAndRangeIntersection2() {
        intersection(
            NumericDomain.range(param, 10, 100),
            NumericDomain.range(param, 100, 120),
            NumericDomain.single(param, 100));
    }

    @Test
    public void testRangeAndRangeIntersection3() {
        intersection(
            NumericDomain.range(param, 10, 100),
            NumericDomain.range(param, 110, 120),
            NumericDomain.empty(param));
    }

    @Test
    public void testRangeAndEmptyPlusRangeIntersection1() {
        intersection(
            NumericDomain.range(param, 10, 100),
            NumericDomain.rangeAndEmpty(param, 80, 120),
            NumericDomain.range(param, 80, 100));
    }

    @Test
    public void testRangeAndEmptyPlusRangeIntersection2() {
        intersection(
            NumericDomain.range(param, 10, 100),
            NumericDomain.rangeAndEmpty(param, 100, 120),
            NumericDomain.single(param, 100));
    }

    @Test
    public void testRangeAndEmptyPlusRangeIntersection3() {
        intersection(
            NumericDomain.range(param, 10, 100),
            NumericDomain.rangeAndEmpty(param, 110, 120),
            NumericDomain.empty(param));
    }

    @Test
    public void testEmptyPlusRangeAndEmptyPlusRangeIntersection1() {
        intersection(
            NumericDomain.rangeAndEmpty(param, 10, 100),
            NumericDomain.rangeAndEmpty(param, 80, 120),
            NumericDomain.rangeAndEmpty(param, 80, 100));
    }

    @Test
    public void testEmptyPlusRangeAndEmptyPlusRangeIntersection2() {
        intersection(
            NumericDomain.rangeAndEmpty(param, 10, 100),
            NumericDomain.rangeAndEmpty(param, 100, 120),
            NumericDomain.singleAndEmpty(param, 100));
    }

    @Test
    public void testEmptyPlusRangeAndEmptyPlusRangeIntersection3() {
        intersection(
            NumericDomain.rangeAndEmpty(param, 10, 100),
            NumericDomain.rangeAndEmpty(param, 110, 120),
            NumericDomain.singleEmptyValue(param));
    }

    @Test
    public void testUnboundedRangesIntersection1() {
        intersection(
            NumericDomain.greaterOrEqual(param, 10),
            NumericDomain.greaterOrEqual(param, 20),
            NumericDomain.greaterOrEqual(param, 20));
    }

    @Test
    public void testUnboundedRangesIntersection2() {
        intersection(
            NumericDomain.greaterOrEqual(param, 20),
            NumericDomain.lessOrEqual(param, 10),
            NumericDomain.empty(param));
    }

    @Test
    public void testUnboundedRangesIntersection3() {
        intersection(
            NumericDomain.greaterOrEqual(param, 10),
            NumericDomain.lessOrEqual(param, 10),
            NumericDomain.single(param, 10));
    }

    @Test
    public void testUnboundedRangesIntersection4() {
        intersection(
            NumericDomain.greaterOrEqual(param, 15),
            NumericDomain.range(param, 10, 20),
            NumericDomain.range(param, 15, 20));
    }

    @Test
    public void testUnboundedRangesIntersection5() {
        intersection(
            NumericDomain.greaterOrEqual(param, 30),
            NumericDomain.range(param, 10, 20),
            NumericDomain.empty(param));
    }

    @Test
    public void testUnboundedRangesIntersection6() {
        intersection(
            NumericDomain.lessOrEqual(param, 15),
            NumericDomain.range(param, 10, 20),
            NumericDomain.range(param, 10, 15));
    }

    @Test
    public void testUnboundedRangesIntersection7() {
        intersection(
            NumericDomain.lessOrEqual(param, 5),
            NumericDomain.range(param, 10, 20),
            NumericDomain.empty(param));
    }

    @Test
    public void testValidateNotEmptyValue() {
        validation(NumericDomain.empty(param), val(10L),
                "Пустая область значений");
        validation(NumericDomain.singleEmptyValue(param), val(10L),
                "Значение должно отсутствовать");
        validation(NumericDomain.single(param, 10), val(10L), "");
        validation(NumericDomain.range(param, 10, 100), val(10L), "");
        validation(NumericDomain.range(param, 10, 100), val(20L), "");
        validation(NumericDomain.greaterOrEqual(param, 10), val(10L), "");
        validation(NumericDomain.lessOrEqual(param, 100), val(100L), "");
    }

    @Test
    public void testGetSingleValue() {
        Assert.assertNull(NumericDomain.empty(param).getSingleValue());
        Assert.assertNull(NumericDomain.any(param).getSingleValue());
        Assert.assertTrue(commonAssertion(NumericDomain.singleEmptyValue(param)
                .getSingleValue()).isEmpty());
        Assert.assertEquals(commonAssertion(NumericDomain.single(param, 10)
                .getSingleValue()).getNumericValue().intValue(), 10);
        Assert.assertNull(NumericDomain.range(param, 10, 100).getSingleValue());
        Assert.assertNull(NumericDomain.rangeAndEmpty(param, 10, 100).getSingleValue());
        Assert.assertNull(NumericDomain.greaterOrEqual(param, 10).getSingleValue());
        Assert.assertNull(NumericDomain.lessOrEqual(param, 10).getSingleValue());
    }

    private void intersection(Domain domain1, Domain domain2, Domain result) {
        Assert.assertTrue(domain1.intersection(domain2).isEqual(result));
        Assert.assertTrue(domain2.intersection(domain1).isEqual(result));
    }

    private void validation(Domain domain, ParameterValue value, String expectedMessage) {
        String actualMessage = domain.validate(value);
        Assert.assertEquals(expectedMessage, actualMessage);
    }

    private ParameterValue commonAssertion(ParameterValue val) {
        Assert.assertEquals(val.getType(), Param.Type.NUMERIC);
        Assert.assertEquals(val.getParamId(), param.getId());
        Assert.assertEquals(val.getXslName(), param.getXslName());
        return val;
    }

    private ParameterValue val(Long value) {
        ParameterValue val = new ParameterValue();
        val.setParamId(param.getId());
        val.setType(Type.NUMERIC);
        val.setXslName("param");
        if (value != null) {
            val.setNumericValue(new BigDecimal(value));
        }
        return val;
    }
}
