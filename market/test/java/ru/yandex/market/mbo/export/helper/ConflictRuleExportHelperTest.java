package ru.yandex.market.mbo.export.helper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.conflictrule.ConflictRule;
import ru.yandex.market.mbo.conflictrule.ConflictRuleDAOMock;
import ru.yandex.market.mbo.conflictrule.ConflictRuleServiceImpl;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.visual.VisualCategory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author ayratgdl
 * @date 13.11.17
 */
public class ConflictRuleExportHelperTest {
    private static final Long CATEGORY_1 = 101L;
    private static final Long PARAMETER_1 = 201L;
    private static final Long PARAMETER_2 = 202L;
    private static final Long OPTION_1_1 = 311L;
    private static final Long OPTION_1_2 = 312L;
    private static final Long OPTION_2_1 = 321L;
    private static final Long OPTION_2_2 = 322L;

    private ConflictRuleServiceImpl conflictRuleService;

    @Before
    public void setUp() {
        conflictRuleService = new ConflictRuleServiceImpl();

        ConflictRuleDAOMock conflictRuleDAO = new ConflictRuleDAOMock();
        conflictRuleService.setConflictRuleDAO(conflictRuleDAO);
    }

    @Test
    public void getActualConflictRulesFromCategoryWithFullData() {
        VisualCategory visualCategory =
            new VisualCategoryBuilder(CATEGORY_1)
                .addParameter(PARAMETER_1, OPTION_1_1)
                .addParameter(PARAMETER_2, OPTION_2_1)
                .build();

        ConflictRule conflictRule = new ConflictRule()
            .setFirstParamId(PARAMETER_1)
            .setSecondParamId(PARAMETER_2)
            .setCategoryId(CATEGORY_1)
            .addPair(OPTION_1_1, OPTION_2_1);

        conflictRuleService.createConflictRule(conflictRule);

        List<ConflictRule> actualConflictRules =
            ConflictRuleExportHelper.getActualConflictRulesFromCategory(conflictRuleService, visualCategory);

        Assert.assertEquals(Arrays.asList(conflictRule), actualConflictRules);
    }

    @Test
    public void getActualConflictRulesFromCategoryWhenAbsentParameter() {
        VisualCategory visualCategory =
            new VisualCategoryBuilder(CATEGORY_1)
                .addParameter(PARAMETER_1, OPTION_1_1)
                .build();

        ConflictRule conflictRule = new ConflictRule()
            .setFirstParamId(PARAMETER_1)
            .setSecondParamId(PARAMETER_2)
            .setCategoryId(CATEGORY_1)
            .addPair(OPTION_1_1, OPTION_2_1);

        conflictRuleService.createConflictRule(conflictRule);

        List<ConflictRule> actualConflictRules =
            ConflictRuleExportHelper.getActualConflictRulesFromCategory(conflictRuleService, visualCategory);

        Assert.assertEquals(Collections.emptyList(), actualConflictRules);
    }

    @Test
    public void getActualConflictRulesFromCategoryWhenAbsentOption() {
        VisualCategory visualCategory =
            new VisualCategoryBuilder(CATEGORY_1)
                .addParameter(PARAMETER_1, OPTION_1_1)
                .addParameter(PARAMETER_2, OPTION_2_1)
                .build();

        ConflictRule conflictRule = new ConflictRule()
            .setFirstParamId(PARAMETER_1)
            .setSecondParamId(PARAMETER_2)
            .setCategoryId(CATEGORY_1)
            .addPair(OPTION_1_1, OPTION_2_1)
            .addPair(OPTION_1_2, OPTION_2_2);
        conflictRuleService.createConflictRule(conflictRule);

        List<ConflictRule> actualConflictRules =
            ConflictRuleExportHelper.getActualConflictRulesFromCategory(conflictRuleService, visualCategory);

        List<ConflictRule> expectedConflictRules = Arrays.asList(
            conflictRule.copy().removePair(OPTION_1_2, OPTION_2_2)
        );

        Assert.assertEquals(expectedConflictRules, actualConflictRules);
    }

    @Test
    public void getActualConflictRulesFromCategoryWhenNoConflictRules() {
        VisualCategory visualCategory =
            new VisualCategoryBuilder(CATEGORY_1)
                .addParameter(PARAMETER_1, OPTION_1_1)
                .addParameter(PARAMETER_2, OPTION_2_1)
                .build();

        List<ConflictRule> actualConflictRules =
            ConflictRuleExportHelper.getActualConflictRulesFromCategory(conflictRuleService, visualCategory);

        List<ConflictRule> expectedConflictRules = Collections.emptyList();

        Assert.assertEquals(expectedConflictRules, actualConflictRules);
    }

    private static class VisualCategoryBuilder {
        private Long categoryId;
        private List<CategoryParam> params = new ArrayList<>();

        VisualCategoryBuilder(Long categoryId) {
            this.categoryId = categoryId;
        }

        VisualCategoryBuilder addParameter(Long paramId, Long... optionIds) {
            CategoryParam param = new Parameter();
            param.setId(paramId);

            for (Long optionId : optionIds) {
                Option option = new OptionImpl();
                option.setId(optionId);
                param.addOption(option);
            }
            params.add(param);

            return this;
        }

        VisualCategory build() {
            VisualCategory visualCategory = Mockito.mock(VisualCategory.class);
            Mockito.when(visualCategory.getHid()).thenReturn(categoryId);
            Mockito.when(visualCategory.getParameters()).thenReturn(params);
            return visualCategory;
        }
    }
}
