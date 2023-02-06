/**
 *
 */
package ru.yandex.market.mbo.gwt.models.rules.conditions;

import static ru.yandex.market.mbo.gwt.models.rules.ModelRuleTester.testCase;

import org.junit.Before;

import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleTester;

/**
 * @author gilmulla
 *
 */
public class TestConditionBase {

    protected ModelRuleTester tester;

    @Before
    @SuppressWarnings({"checkstyle:LineLength", "checkstyle:MagicNumber"})
    public void prepareTestCase() {
        tester = testCase()
                .startParameters()
                    .startParameter()
                        .xsl("num1").type(Param.Type.NUMERIC)
                    .endParameter()
                    .startParameter()
                        .xsl("num2").type(Param.Type.NUMERIC)
                    .endParameter()
                    .startParameter()
                        .xsl("num3").type(Param.Type.NUMERIC)
                    .endParameter()
                    .startParameter()
                        .xsl("num4").type(Param.Type.NUMERIC)
                    .endParameter()
                    .startParameter()
                        .xsl("str1").type(Param.Type.STRING)
                    .endParameter()
                    .startParameter()
                        .xsl("str2").type(Param.Type.STRING)
                    .endParameter()
                    .startParameter()
                        .xsl("str3").type(Param.Type.STRING)
                    .endParameter()
                    .startParameter()
                        .xsl("str4").type(Param.Type.STRING)
                    .endParameter()
                    .startParameter()
                        .xsl("bool1").type(Param.Type.BOOLEAN)
                        .option(1, "TRUE")
                        .option(2, "FALSE")
                    .endParameter()
                    .startParameter()
                        .xsl("bool2").type(Param.Type.BOOLEAN)
                        .option(1, "TRUE")
                        .option(2, "FALSE")
                    .endParameter()
                    .startParameter()
                        .xsl("bool3").type(Param.Type.BOOLEAN)
                        .option(1, "TRUE")
                        .option(2, "FALSE")
                    .endParameter()
                    .startParameter()
                        .xsl("bool4").type(Param.Type.BOOLEAN)
                        .option(1, "TRUE")
                        .option(2, "FALSE")
                    .endParameter()
                    .startParameter()
                        .xsl("enum1").type(Param.Type.ENUM)
                        .option(1, "Option1")
                        .option(2, "Option2")
                        .option(3, "Option3")
                    .endParameter()
                    .startParameter()
                        .xsl("enum2").type(Param.Type.ENUM)
                        .option(1, "Option1")
                        .option(2, "Option2")
                    .endParameter()
                    .startParameter()
                        .xsl("enum3").type(Param.Type.ENUM)
                        .option(1, "Option1")
                    .endParameter()
                    .startParameter()
                        .xsl("enum4").type(Param.Type.ENUM)
                    .endParameter()
                .endParameters();
    }
}
