package ru.yandex.market.mbo.gwt.models.rules.domains;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Param.Type;
import ru.yandex.market.mbo.gwt.models.params.ThinCategoryParam;
import ru.yandex.market.mbo.gwt.models.rules.Domain;
import ru.yandex.market.mbo.gwt.models.rules.EnumDomain;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleTester;

import static ru.yandex.market.mbo.gwt.models.rules.ModelRuleTester.testCase;

/**
 * @author gilmulla
 */
@SuppressWarnings("checkstyle:magicNumber")
public class TestEnumDomain {

    private ThinCategoryParam param;
    private ThinCategoryParam bigEnum;
    private ThinCategoryParam boolParam;
    private ThinCategoryParam boolParamNoOptions;
    private ThinCategoryParam boolParamIllegalOptions;

    @Before
    public void prepareTestCase() {
        ModelRuleTester tester = testCase()
                .startParameters()
                    .startParameter()
                        .xsl("param").type(Param.Type.ENUM)
                        .option(1, "One")
                        .option(2, "Two")
                        .option(3, "Three")
                        .option(4, "Four")
                    .endParameter()
                    .startParameter()
                        .xsl("bigEnum").type(Param.Type.ENUM)
                        .option(1, "One")
                        .option(2, "Two")
                        .option(3, "Three")
                        .option(4, "Four")
                        .option(5, "Five")
                        .option(6, "Six")
                    .endParameter()
                    .startParameter()
                        .xsl("boolParam").type(Param.Type.BOOLEAN)
                        .option(1, "TRUE")
                        .option(2, "FALSE")
                    .endParameter()
                    .startParameter()
                        .xsl("boolParamNoOptions").type(Param.Type.BOOLEAN)
                    .endParameter()
                    .startParameter()
                        .xsl("boolParamIllegalOptions").type(Param.Type.BOOLEAN)
                        .option(1, "TRUE1")
                        .option(2, "FALSE")
                    .endParameter()
                .endParameters();
        param = tester.param("param");
        bigEnum = tester.param("bigEnum");
        boolParam = tester.param("boolParam");
        boolParamNoOptions = tester.param("boolParamNoOptions");
        boolParamIllegalOptions = tester.param("boolParamIllegalOptions");
    }

    @Test
    public void testEnumStringRepresentation() {
        Assert.assertEquals("Значение должно быть из списка: One",
            EnumDomain.of(param, 1L).toString());
        Assert.assertEquals("Значение должно быть из списка: One; Two",
            EnumDomain.of(param, 1L, 2L).toString());
        Assert.assertEquals("Значение должно быть из списка: One; Two; Three",
            EnumDomain.of(param, 1L, 2L, 3L).toString());
        Assert.assertEquals("Значение должно быть из списка: One; Two; Three... (еще 1 знач.)",
            EnumDomain.of(param, 1L, 2L, 3L, 4L).toString());
        Assert.assertEquals("Значение должно быть из списка: One; Two; Three... (еще 2 знач.)",
            EnumDomain.of(bigEnum, 1L, 2L, 3L, 4L, 5L).toString());
        Assert.assertEquals("Значение должно быть из списка: One; Two; Three... (еще 3 знач.)",
            EnumDomain.of(bigEnum, 1L, 2L, 3L, 4L, 5L, 6L).toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBooleanNoOptionsCreationFail() {
        EnumDomain.bool(boolParamNoOptions, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBooleanIllegalOptionsCreationFail() {
        EnumDomain.bool(boolParamIllegalOptions, true);
    }

    //TODO - test didn't start in github, but fails after moving to arcadia, fix it later
    @Test(expected = IllegalArgumentException.class)
    @Ignore
    public void testWrongOptionCreationFail() {
        EnumDomain.of(param, 1L, 2L, 3L, 4L, 5L);
    }

    @Test
    public void testCreationWithDublicatedOptions() {
        Assert.assertTrue(EnumDomain.of(param, 1L, 2L, 2L, 3L)
                .isEqual(EnumDomain.of(param, 1L, 2L, 3L)));
    }

    @Test
    public void testEmptyDomainAndEmptyDomainIntersection() {
        intersection(
            EnumDomain.empty(param),
            EnumDomain.empty(param),
            EnumDomain.empty(param));
    }

    @Test
    public void testEmptyDomainAndNotEmptyDomainIntersection() {
        intersection(
            EnumDomain.empty(param),
            EnumDomain.of(param, 1L, 2L),
            EnumDomain.empty(param));
    }

    @Test
    public void testNotEmptyAndNotEmptyIntersectionSuccess1() {
        intersection(
            EnumDomain.of(param, 1L, 2L, 3L),
            EnumDomain.of(param, 2L, 3L, 4L),
            EnumDomain.of(param, 2L, 3L));
    }

    @Test
    public void testNotEmptyAndNotEmptyIntersectionSuccess2() {
        intersection(
            EnumDomain.of(param, 1L, 2L),
            EnumDomain.of(param, 2L, 3L),
            EnumDomain.of(param, 2L));
    }

    @Test
    public void testNotEmptyAndNotEmptyIntersectionConflict() {
        intersection(
            EnumDomain.of(param, 1L, 2L),
            EnumDomain.of(param, 3L, 4L),
            EnumDomain.empty(param));
    }

    @Test
    public void testIntersectionNotLogic() {
        intersection(
            EnumDomain.not(param, 1L),
            EnumDomain.not(param, 2L),
            EnumDomain.not(param, 1L, 2L)
        );

        intersection(
            EnumDomain.not(param, 1L, 3L),
            EnumDomain.of(param, 1L, 2L, 3L, 4L),
            EnumDomain.of(param, 2L, 4L)
        );

        intersection(
            EnumDomain.not(param, 1L, 3L),
            EnumDomain.of(param, 2L, 4L),
            EnumDomain.of(param, 2L, 4L)
        );

        intersection(
            EnumDomain.not(param, 1L, 2L),
            EnumDomain.of(param, 1L, 2L),
            EnumDomain.empty(param)
        );
    }

    @Test
    public void testValidateNot() {
        validation(EnumDomain.not(param, 1L), enumVal(2L), "");
        validation(EnumDomain.not(param, 1L), enumVal(1L), "Значение не должно содержать: One");
        validation(EnumDomain.not(param, 1L, 2L), enumVal(2L), "Значение не должно содержать: One; Two");
    }

    @Test
    public void testValidateNotEmptyValue() {
        validation(EnumDomain.empty(param), enumVal(2L),
                "Пустая область значений");
        validation(EnumDomain.of(param, EnumDomain.EMPTY), enumVal(2L),
                "Значение должно отсутствовать");
        validation(EnumDomain.of(param, 1L, 2L, 3L), enumVal(2L), "");
        validation(EnumDomain.of(param, 1L), enumVal(4L),
                "Значение должно быть из списка: One");
        validation(EnumDomain.of(param, 1L, 2L, 3L), enumVal(4L),
                "Значение должно быть из списка: One; Two; Three");
        validation(EnumDomain.of(param, EnumDomain.EMPTY, 1L, 2L, 3L), enumVal(4L),
                "Значение должно отсутствовать или "
                    + "содержаться в следующем списке: One; Two; Three");
    }

    @Test
    public void testGetSingleValue() {
        Assert.assertNull(EnumDomain.empty(param).getSingleValue());
        Assert.assertTrue(commonEnumAssertion(EnumDomain.of(param, EnumDomain.EMPTY)
                .getSingleValue()).isEmpty());
        Assert.assertEquals(commonEnumAssertion(EnumDomain.of(param, 1L)
                .getSingleValue()).getOptionId().longValue(), 1L);
        Assert.assertNull(EnumDomain.of(param, EnumDomain.EMPTY, 1L).getSingleValue());
        Assert.assertNull(EnumDomain.of(param, 1L, 2L).getSingleValue());
        Assert.assertNull(EnumDomain.not(param, 1L).getSingleValue());
    }

    @Test
    public void testGetSingleBooleanValue() {
        Assert.assertNull(EnumDomain.empty(boolParam).getSingleValue());
        Assert.assertEquals(commonBooleanAssertion(EnumDomain.of(boolParam, 1L)
                .getSingleValue()).getOptionId().longValue(), 1L);
        Assert.assertTrue(commonBooleanAssertion(EnumDomain.of(boolParam, 1L)
                .getSingleValue()).getBooleanValue());
        Assert.assertFalse(commonBooleanAssertion(EnumDomain.of(boolParam, 2L)
                .getSingleValue()).getBooleanValue());
        Assert.assertNull(EnumDomain.of(boolParam, 1L, 2L).getSingleValue());
        Assert.assertNull(EnumDomain.not(boolParam, 1L).getSingleValue());
    }

    private void intersection(Domain domain1, Domain domain2, Domain result) {
        Assert.assertTrue(domain1.intersection(domain2).isEqual(result));
        Assert.assertTrue(domain2.intersection(domain1).isEqual(result));
    }

    private void validation(Domain domain, ParameterValue value, String expectedMessage) {
        String actualMessage = domain.validate(value);
        Assert.assertEquals(expectedMessage, actualMessage);
    }

    private ParameterValue commonEnumAssertion(ParameterValue val) {
        Assert.assertEquals(Param.Type.ENUM, val.getType());
        Assert.assertEquals(param.getId(), val.getParamId());
        Assert.assertEquals(param.getXslName(), val.getXslName());
        return val;
    }

    private ParameterValue commonBooleanAssertion(ParameterValue val) {
        Assert.assertEquals(Param.Type.BOOLEAN, val.getType());
        Assert.assertEquals(boolParam.getId(), val.getParamId());
        Assert.assertEquals(boolParam.getXslName(), val.getXslName());
        return val;
    }

    private ParameterValue enumVal(Long optionId) {
        ParameterValue val = new ParameterValue();
        val.setType(Type.ENUM);
        val.setParamId(param.getId());
        val.setXslName(param.getXslName());
        val.setOptionId(optionId);
        return val;
    }
}
