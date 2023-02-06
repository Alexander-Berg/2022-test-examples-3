package ru.yandex.market.mbo.gwt.models.rules.intersections;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleTester;

import static ru.yandex.market.mbo.gwt.models.rules.ModelRuleTester.testCase;

/**
 * @author gilmulla
 */
public class TestStringIntersections {

    private ModelRuleTester tester;

    @Before
    public void prepareTestCase() {
        tester = testCase()
                .startParameters()
                    .startParameter()
                        .xsl("strIn").type(Param.Type.STRING)
                    .endParameter()
                    .startParameter()
                        .xsl("strOut").type(Param.Type.STRING)
                    .endParameter()
                .endParameters();
    }

    @Test
    public void testSubstringIntersectionOnEmptyParameter() {
        tester
            .startModel()
                .id(1).category(1)
                .param("strIn").setString("abc")
                .param("strOut").setEmpty()
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("First range").group("Test")
                    ._if()
                        .param("strIn").matchesString("abc")
                    .then()
                        .param("strOut").substring("cc")
                .endRule()
                .startRule()
                    .name("Second range").group("Test")
                    ._if()
                        .param("strIn").matchesString("abc")
                    .then()
                        .param("strOut").substring("ddd")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("strOut")
                    .valid()
                    .shouldNull()
                    .stringDomain().substring("cc", "ddd").endDomain()
                .endParam()
            .endResults();
    }
}
