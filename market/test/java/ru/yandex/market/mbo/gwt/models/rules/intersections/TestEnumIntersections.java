package ru.yandex.market.mbo.gwt.models.rules.intersections;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.EnumDomain;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleTester;

import static ru.yandex.market.mbo.gwt.models.rules.ModelRuleTester.testCase;

/**
 * @author gilmulla
 */
public class TestEnumIntersections {

    private static final long OPTION_ID3 = 3L;
    private static final long OPTION_ID4 = 4L;
    private static final int FIFTEEN_THOUSANDS = 15000;
    private static final int NUMERIC2200 = 2200;
    private static final int TEN_THOUSANDS = 10000;
    private static final int TWENTY_THOUSANDS = 20000;

    private ModelRuleTester tester;

    @Before
    public void prepareTestCase() {
        tester = testCase()
                .startParameters()
                    .startParameter()
                        .xsl("fighting").type(Param.Type.NUMERIC)
                    .endParameter()
                    .startParameter()
                        .xsl("combat_level").type(Param.Type.NUMERIC)
                    .endParameter()
                    .startParameter()
                        .xsl("nation").type(Param.Type.ENUM)
                        .option(1, "USSR")
                        .option(2, "Germany")
                        .option(OPTION_ID3, "Japan")
                        .option(OPTION_ID4, "USA")
                    .endParameter()
                .endParameters();
    }

    @Test
    public void testOneAndTwoIntersectionOnEmptyParameter() {
        tester
        .startModel()
            .id(1).category(1)
            .param("fighting").setNumeric(FIFTEEN_THOUSANDS)
            .param("combat_level").setNumeric(NUMERIC2200)
            .param("nation").setEmpty()
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Nation from fighting").group("Test")
                ._if()
                    .param("fighting").insideRange(TEN_THOUSANDS, TWENTY_THOUSANDS)
                .then()
                    .param("nation").matchesEnum(1L, 2L)
            .endRule()
            .startRule()
                .name("Nation from combat level").group("Test")
                ._if()
                    .param("combat_level").isNotEmpty()
                .then()
                    .param("nation").matchesEnum(1L)
            .endRule()
        .endRuleSet()
        .doInference()
        .results()
            .count(1)
            .iterationCount(2)
            .param("nation").valid().modified().optionId(1L).enumDomain().options(1L).endDomain().endParam()
        .endResults();
    }

    @Test
    public void testTwoAndThreeIntersectionOnEmptyParameter() {
        tester
        .startModel()
            .id(1).category(1)
            .param("fighting").setNumeric(FIFTEEN_THOUSANDS)
            .param("combat_level").setNumeric(NUMERIC2200)
            .param("nation").setEmpty()
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Nation from fighting").group("Test")
                ._if()
                    .param("fighting").insideRange(TEN_THOUSANDS, TWENTY_THOUSANDS)
                .then()
                    .param("nation").matchesEnum(1L, 2L, OPTION_ID3)
            .endRule()
            .startRule()
                .name("Nation from combat level").group("Test")
                ._if()
                    .param("combat_level").isNotEmpty()
                .then()
                    .param("nation").matchesEnum(1L, 2L)
            .endRule()
        .endRuleSet()
        .doInference()
        .results()
            .count(1)
            .iterationCount(2)
            .param("nation").valid().shouldNull().enumDomain().options(1L, 2L).endDomain().endParam()
        .endResults();
    }

    @Test
    public void testCleanAndListIntersectionOnEmptyParameter() {
        tester
        .startModel()
            .id(1).category(1)
            .param("fighting").setNumeric(FIFTEEN_THOUSANDS)
            .param("combat_level").setNumeric(NUMERIC2200)
            .param("nation").setEmpty()
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Nation from fighting").group("Test")
                ._if()
                    .param("fighting").insideRange(TEN_THOUSANDS, TWENTY_THOUSANDS)
                .then()
                    .param("nation").matchesEnum(1L, 2L, OPTION_ID3)
            .endRule()
            .startRule()
                .name("Nation from combat level").group("Test")
                ._if()
                    .param("combat_level").isNotEmpty()
                .then()
                    .param("nation").isEmpty()
            .endRule()
        .endRuleSet()
        .doInference()
        .results()
            .count(1)
            .iterationCount(2)
            .param("nation").valid().shouldNull().conflict().enumDomain().empty().endDomain().endParam()
        .endResults();
    }

    @Test
    public void testCleanAndListIntersectionOnEmptyParameterFixConflict1() {
        tester
        .startModel()
            .id(1).category(1)
            .param("fighting").setNumeric(FIFTEEN_THOUSANDS)
            .param("combat_level").setNumeric(NUMERIC2200)
            .param("nation").setEmpty()
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Nation from fighting").group("Test")
                .priority(1)
                ._if()
                    .param("fighting").insideRange(TEN_THOUSANDS, TWENTY_THOUSANDS)
                .then()
                    .param("nation").matchesEnum(1L, 2L, OPTION_ID3)
            .endRule()
            .startRule()
                .name("Nation from combat level").group("Test")
                .priority(2)
                ._if()
                    .param("combat_level").isNotEmpty()
                .then()
                    .param("nation").isEmpty()
            .endRule()
        .endRuleSet()
        .doInference()
        .results()
            .count(1)
            .iterationCount(2)
            .param("nation").valid().maxFailedPriority(1).isEmpty()
                .enumDomain().options(EnumDomain.EMPTY).endDomain()
            .endParam()
        .endResults();
    }

    @Test
    public void testCleanAndListIntersectionOnEmptyParameterFixConflict2() {
        tester
        .startModel()
            .id(1).category(1)
            .param("fighting").setNumeric(FIFTEEN_THOUSANDS)
            .param("combat_level").setNumeric(NUMERIC2200)
            .param("nation").setEmpty()
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Nation from fighting").group("Test")
                .priority(2)
                ._if()
                    .param("fighting").insideRange(TEN_THOUSANDS, TWENTY_THOUSANDS)
                .then()
                    .param("nation").matchesEnum(1L, 2L, OPTION_ID3)
            .endRule()
            .startRule()
                .name("Nation from combat level").group("Test")
                .priority(1)
                ._if()
                    .param("combat_level").isNotEmpty()
                .then()
                    .param("nation").isEmpty()
            .endRule()
        .endRuleSet()
        .doInference()
        .results()
            .count(1)
            .iterationCount(2)
            .param("nation").valid().maxFailedPriority(1).shouldNull()
                .enumDomain().options(1L, 2L, OPTION_ID3).endDomain()
            .endParam()
        .endResults();
    }

    @Test
    public void testTwoAndThreeIntersectionOnValidParameter() {
        tester
        .startModel()
            .id(1).category(1)
            .param("fighting").setNumeric(FIFTEEN_THOUSANDS)
            .param("combat_level").setNumeric(NUMERIC2200)
            .param("nation").setOption(1)
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Nation from fighting").group("Test")
                ._if()
                    .param("fighting").insideRange(TEN_THOUSANDS, TWENTY_THOUSANDS)
                .then()
                    .param("nation").matchesEnum(1L, 2L, OPTION_ID3)
            .endRule()
            .startRule()
                .name("Nation from combat level").group("Test")
                ._if()
                    .param("combat_level").isNotEmpty()
                .then()
                    .param("nation").matchesEnum(1L, 2L)
            .endRule()
        .endRuleSet()
        .doInference()
        .results()
            .count(1)
            .iterationCount(2)
            .param("nation").valid().shouldNull().enumDomain().options(1L, 2L).endDomain().endParam()
        .endResults();
    }

    @Test
    public void testTwoAndThreeIntersectionOnInvalidParameter() {
        tester
        .startModel()
            .id(1).category(1)
            .param("fighting").setNumeric(FIFTEEN_THOUSANDS)
            .param("combat_level").setNumeric(NUMERIC2200)
            .param("nation").setOption(OPTION_ID3)
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Nation from fighting").group("Test")
                ._if()
                    .param("fighting").insideRange(TEN_THOUSANDS, TWENTY_THOUSANDS)
                .then()
                    .param("nation").matchesEnum(1L, 2L, OPTION_ID3)
            .endRule()
            .startRule()
                .name("Nation from combat level").group("Test")
                ._if()
                    .param("combat_level").isNotEmpty()
                .then()
                    .param("nation").matchesEnum(1L, 2L)
            .endRule()
        .endRuleSet()
        .doInference()
        .results()
            .count(1)
            .iterationCount(2)
            .param("nation").invalid().shouldNull().enumDomain().options(1L, 2L).endDomain().endParam()
        .endResults();
    }

    @Test
    public void testOneAndTwoIntersectionOnValidParameter() {
        tester
        .startModel()
            .id(1).category(1)
            .param("fighting").setNumeric(FIFTEEN_THOUSANDS)
            .param("combat_level").setNumeric(NUMERIC2200)
            .param("nation").setOption(1)
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Nation from fighting").group("Test")
                ._if()
                    .param("fighting").insideRange(TEN_THOUSANDS, TWENTY_THOUSANDS)
                .then()
                    .param("nation").matchesEnum(1L, 2L)
            .endRule()
            .startRule()
                .name("Nation from combat level").group("Test")
                ._if()
                    .param("combat_level").isNotEmpty()
                .then()
                    .param("nation").matchesEnum(1L)
            .endRule()
        .endRuleSet()
        .doInference()
        .results()
            .count(1)
            .iterationCount(2)
            .param("nation").valid().optionId(1L).enumDomain().options(1L).endDomain().endParam()
        .endResults();
    }

    @Test
    public void testOneAndTwoIntersectionOnInvalidParameter() {
        tester
        .startModel()
            .id(1).category(1)
            .param("fighting").setNumeric(FIFTEEN_THOUSANDS)
            .param("combat_level").setNumeric(NUMERIC2200)
            .param("nation").setOption(OPTION_ID3)
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Nation from fighting").group("Test")
                ._if()
                    .param("fighting").insideRange(TEN_THOUSANDS, TWENTY_THOUSANDS)
                .then()
                    .param("nation").matchesEnum(1L, 2L)
            .endRule()
            .startRule()
                .name("Nation from combat level").group("Test")
                ._if()
                    .param("combat_level").isNotEmpty()
                .then()
                    .param("nation").matchesEnum(1L)
            .endRule()
        .endRuleSet()
        .doInference()
        .results()
            .count(1)
            .iterationCount(2)
            .param("nation").valid().modified().optionId(1L).enumDomain().options(1L).endDomain().endParam()
        .endResults();
    }

    @Test
    public void testSubstringIntersections1() {
        tester
        .startModel()
            .id(1).category(1)
            .param("fighting").setNumeric(FIFTEEN_THOUSANDS)
            .param("combat_level").setNumeric(NUMERIC2200)
            .param("nation").setOption(OPTION_ID3)
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Nation from fighting").group("Test")
                ._if()
                    .param("fighting").insideRange(TEN_THOUSANDS, TWENTY_THOUSANDS)
                .then()
                    .param("nation").substring("apa")
            .endRule()
            .startRule()
                .name("Nation from combat level").group("Test")
                ._if()
                    .param("combat_level").isNotEmpty()
                .then()
                    .param("nation").matchesEnum(OPTION_ID3, OPTION_ID4)
            .endRule()
        .endRuleSet()
        .doInference()
        .results()
            .count(1)
            .iterationCount(2)
            .param("nation").valid().optionId(OPTION_ID3)
                .enumDomain().options(OPTION_ID3).endDomain()
            .endParam()
        .endResults();
    }

    @Test
    public void testSubstringIntersections2() {
        tester
        .startModel()
            .id(1).category(1)
            .param("fighting").setNumeric(FIFTEEN_THOUSANDS)
            .param("combat_level").setNumeric(NUMERIC2200)
            .param("nation").setOption(2)
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Nation from fighting").group("Test")
                ._if()
                    .param("fighting").insideRange(TEN_THOUSANDS, TWENTY_THOUSANDS)
                .then()
                    .param("nation").substring("an")
            .endRule()
            .startRule()
                .name("Nation from combat level").group("Test")
                ._if()
                    .param("combat_level").isNotEmpty()
                .then()
                    .param("nation").substring("Germ")
            .endRule()
        .endRuleSet()
        .doInference()
        .results()
            .count(1)
            .iterationCount(2)
            .param("nation").valid().notModified().optionId(2L).enumDomain().options(2L).endDomain().endParam()
        .endResults();
    }
}
