package ru.yandex.market.mbo.gwt.models.rules.complex;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleTester;

import static ru.yandex.market.mbo.gwt.models.rules.ModelRuleTester.testCase;

/**
 * @author gilmulla
 */
@SuppressWarnings("checkstyle:magicNumber")
public class SpecialTests {
    private ModelRuleTester tester;

    @Before
    public void prepareTestCase() {
        tester = testCase()
                .startParameters()
                    .startParameter()
                        .xsl("a").type(Param.Type.NUMERIC)
                    .endParameter()
                    .startParameter()
                        .xsl("b").type(Param.Type.NUMERIC)
                    .endParameter()
                    .startParameter()
                        .xsl("c").type(Param.Type.NUMERIC)
                    .endParameter()
                .endParameters();
    }

    @Test
    public void testCleanAndRangeOnEmptyParameter() {
        tester
        .startModel()
            .id(1).category(1)
            .param("a").setNumeric(1)
            .param("b").setNumeric(0)
            .param("c").setNumeric(1)
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Range").group("Test")
                .priority(10)
                ._if()
                    .param("a").matchesNumeric(5)
                .then()
                    .param("b").matchesNumeric(2)
            .endRule()
            .startRule()
                .name("Range").group("Test")
                .priority(5)
                ._if()
                    .param("a").matchesNumeric(1)
                .then()
                    .param("b").matchesNumeric(1)
            .endRule()
            .startRule()
                .name("Range").group("Test")
                .priority(10)
                ._if()
                    .param("c").matchesNumeric(1)
                .then()
                    .param("a").matchesNumeric(5)
            .endRule()
        .endRuleSet()
        .doInference()
        .results()
            .count(2)
            .iterationCount(3)
            .param("a").valid().modified().numeric(5).numericDomain().single(5).endDomain().endParam()
            .param("b").valid().modified().numeric(2).numericDomain().single(2).endDomain().endParam()
        .endResults();
    }
}
