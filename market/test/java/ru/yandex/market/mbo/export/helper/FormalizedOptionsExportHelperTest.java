package ru.yandex.market.mbo.export.helper;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRule;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRuleImpl;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRulePredicate;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRuleType;
import ru.yandex.market.mbo.gwt.models.linkedvalues.LinkDirection;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLink;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLinkType;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Parameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author ayratgdl
 * @date 19.11.17
 */
public class FormalizedOptionsExportHelperTest {
    private static final Long PARAMETER_1 = 201L;
    private static final Long PARAMETER_2 = 202L;
    private static final Long PARAMETER_3 = 203L;
    private static final Long OPTION_1_1 = 311L;
    private static final Long OPTION_1_2 = 312L;
    private static final Long OPTION_1_3 = 313L;
    private static final Long OPTION_1_4 = 314L;
    private static final Long OPTION_2_1 = 321L;
    private static final Long OPTION_2_2 = 322L;
    private static final Long OPTION_2_3 = 323L;
    private static final Long OPTION_2_4 = 324L;
    private static final Long OPTION_3_1 = 331L;
    private static final Long OPTION_3_2 = 332L;
    private static final Long OPTION_3_3 = 333L;

    private static final ValueLinkBuilder P_1_DIRECT_P_2_BUILDER =
        new ValueLinkBuilder(PARAMETER_1, PARAMETER_2, LinkDirection.DIRECT);
    private static final ValueLinkBuilder P_1_DIRECT_P_3_BUILDER =
        new ValueLinkBuilder(PARAMETER_1, PARAMETER_3, LinkDirection.DIRECT);
    private static final ValueLinkBuilder P_2_DIRECT_P_3_BUILDER =
        new ValueLinkBuilder(PARAMETER_2, PARAMETER_3, LinkDirection.DIRECT);

    private static final ValueLinkBuilder P_1_BIDIRECTIONAL_P_2_BUILDER =
        new ValueLinkBuilder(PARAMETER_1, PARAMETER_2, LinkDirection.BIDIRECTIONAL);
    private static final ValueLinkBuilder P_1_BIDIRECTIONAL_P_3_BUILDER =
        new ValueLinkBuilder(PARAMETER_1, PARAMETER_3, LinkDirection.BIDIRECTIONAL);
    private static final ValueLinkBuilder P_2_BIDIRECTIONAL_P_3_BUILDER =
        new ValueLinkBuilder(PARAMETER_2, PARAMETER_3, LinkDirection.BIDIRECTIONAL);

    private static final ValueLinkBuilder P_1_REVERSE_P_2_BUILDER =
        new ValueLinkBuilder(PARAMETER_1, PARAMETER_2, LinkDirection.REVERSE);
    private static final ValueLinkBuilder P_1_REVERSE_P_3_BUILDER =
        new ValueLinkBuilder(PARAMETER_1, PARAMETER_3, LinkDirection.REVERSE);

    @Test
    public void computeFormalizedOptionsEmpty() {
        Set<Long> actualFormalizedOptions =
            FormalizedOptionsExportHelper.computeFormalizedOptions(
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        Set<Long> expectedFormalizedOptions = Collections.emptySet();

        Assert.assertEquals(expectedFormalizedOptions, actualFormalizedOptions);
    }

    @Test
    public void computeFormalizedOptionsForDirectLinks() {
        List<CategoryParam> parameters = Arrays.asList(
            buildParameter(PARAMETER_1, OPTION_1_1, OPTION_1_2),
            buildParameter(PARAMETER_2, OPTION_2_1, OPTION_2_2)
        );

        List<ValueLink> valueLinks = Arrays.asList(
            P_1_DIRECT_P_2_BUILDER.build(OPTION_1_1, OPTION_2_1),
            P_1_DIRECT_P_2_BUILDER.build(OPTION_1_2, OPTION_2_3),
            P_1_DIRECT_P_2_BUILDER.build(OPTION_1_3, OPTION_2_2)
        );

        Set<Long> actualFormalizedOptions =
            FormalizedOptionsExportHelper.computeFormalizedOptions(
                valueLinks, Collections.emptyList(), parameters);

        Set<Long> expectedFormalizedOptions = new HashSet<>(Arrays.asList(OPTION_1_1, OPTION_1_2, OPTION_2_1));

        Assert.assertEquals(expectedFormalizedOptions, actualFormalizedOptions);
    }

    @Test
    public void computeFormalizedOptionsForBidirectionalLinks() {
        List<CategoryParam> parameters = Arrays.asList(
            buildParameter(PARAMETER_1, OPTION_1_1, OPTION_1_2),
            buildParameter(PARAMETER_2, OPTION_2_1, OPTION_2_2)
        );

        List<ValueLink> valueLinks = Arrays.asList(
            P_1_BIDIRECTIONAL_P_2_BUILDER.build(OPTION_1_1, OPTION_2_1),
            P_1_BIDIRECTIONAL_P_2_BUILDER.build(OPTION_1_2, OPTION_2_3),
            P_1_BIDIRECTIONAL_P_2_BUILDER.build(OPTION_1_3, OPTION_2_2)
        );

        Set<Long> actualFormalizedOptions =
            FormalizedOptionsExportHelper.computeFormalizedOptions(
                valueLinks, Collections.emptyList(), parameters);

        Set<Long> expectedFormalizedOptions =
            new HashSet<>(Arrays.asList(OPTION_1_1, OPTION_2_1));

        Assert.assertEquals(expectedFormalizedOptions, actualFormalizedOptions);
    }

    @Test
    public void computeFormalizedOptionsForReverseLinks() {
        List<CategoryParam> parameters = Arrays.asList(
            buildParameter(PARAMETER_1, OPTION_1_1, OPTION_1_2),
            buildParameter(PARAMETER_2, OPTION_2_1, OPTION_2_2)
        );

        List<ValueLink> valueLinks = Arrays.asList(
            P_1_REVERSE_P_2_BUILDER.build(OPTION_1_1, OPTION_2_1),
            P_1_REVERSE_P_2_BUILDER.build(OPTION_1_2, OPTION_2_3),
            P_1_REVERSE_P_2_BUILDER.build(OPTION_1_3, OPTION_2_2)
        );

        Set<Long> actualFormalizedOptions =
            FormalizedOptionsExportHelper.computeFormalizedOptions(
                valueLinks, Collections.emptyList(), parameters);

        Set<Long> expectedFormalizedOptions =
            new HashSet<>(Arrays.asList(OPTION_1_1, OPTION_2_1, OPTION_2_2));

        Assert.assertEquals(expectedFormalizedOptions, actualFormalizedOptions);
    }

    /**
     * OPTION_2_1 зависит по direct связи от нескольких значений, некоторые из которых не выгружаются.
     */
    @Test
    public void computeFormalizedOptionsForDirectLinksWithSeveralMasterOptions() {
        List<CategoryParam> parameters = Arrays.asList(
            buildParameter(PARAMETER_1, OPTION_1_1, OPTION_1_2),
            buildParameter(PARAMETER_2, OPTION_2_1, OPTION_2_2)
        );

        List<ValueLink> valueLinks = Arrays.asList(
            P_1_DIRECT_P_2_BUILDER.build(OPTION_1_1, OPTION_2_1),
            P_1_DIRECT_P_2_BUILDER.build(OPTION_1_3, OPTION_2_1)
        );

        Set<Long> actualFormalizedOptions =
            FormalizedOptionsExportHelper.computeFormalizedOptions(
                valueLinks, Collections.emptyList(), parameters);

        Set<Long> expectedFormalizedOptions =
            new HashSet<>(Arrays.asList(OPTION_1_1, OPTION_1_2, OPTION_2_1, OPTION_2_2));

        Assert.assertEquals(expectedFormalizedOptions, actualFormalizedOptions);
    }

    /**
     * OPTION_2_1 зависит по direct связи от нескольких значений, все из которых не выгружаются.
     */
    @Test
    public void computeFormalizedOptionsForDirectLinksWithAllNoActiveMasterOptions() {
        List<CategoryParam> parameters = Arrays.asList(
            buildParameter(PARAMETER_1, OPTION_1_1, OPTION_1_2),
            buildParameter(PARAMETER_2, OPTION_2_1, OPTION_2_2)
        );

        List<ValueLink> valueLinks = Arrays.asList(
            P_1_DIRECT_P_2_BUILDER.build(OPTION_1_3, OPTION_2_1),
            P_1_DIRECT_P_2_BUILDER.build(OPTION_1_4, OPTION_2_1)
        );

        Set<Long> actualFormalizedOptions =
            FormalizedOptionsExportHelper.computeFormalizedOptions(
                valueLinks, Collections.emptyList(), parameters);

        Set<Long> expectedFormalizedOptions =
            new HashSet<>(Arrays.asList(OPTION_1_1, OPTION_1_2, OPTION_2_2));

        Assert.assertEquals(expectedFormalizedOptions, actualFormalizedOptions);
    }

    /**
     * OPTION_3_1 зависит по direct связи по двух параметров (PARAMETER_1 и PARAMETER_2).
     * В случае PARAMETER_1 все значения от которых он зависит не выгружаются.
     * В случае PARAMETER_2 есть значения от которых он зависит и которые выгружаются.
     * В результате OPTION_3_1 не выгружается из-за зависимости от PARAMETER_1
     */
    @Test
    public void computeFormalizedOptionsForDirectLinksWithTwoParameters() {
        List<CategoryParam> parameters = Arrays.asList(
            buildParameter(PARAMETER_1, OPTION_1_1, OPTION_1_2),
            buildParameter(PARAMETER_2, OPTION_2_1, OPTION_2_2),
            buildParameter(PARAMETER_3, OPTION_3_1, OPTION_3_2)
        );

        List<ValueLink> valueLinks = Arrays.asList(
            P_1_DIRECT_P_3_BUILDER.build(OPTION_1_3, OPTION_3_1),
            P_1_DIRECT_P_3_BUILDER.build(OPTION_1_4, OPTION_3_1),
            P_2_DIRECT_P_3_BUILDER.build(OPTION_2_1, OPTION_3_1),
            P_2_DIRECT_P_3_BUILDER.build(OPTION_2_3, OPTION_3_1)
        );

        Set<Long> actualFormalizedOptions =
            FormalizedOptionsExportHelper.computeFormalizedOptions(
                valueLinks, Collections.emptyList(), parameters);

        Set<Long> expectedFormalizedOptions =
            new HashSet<>(Arrays.asList(OPTION_1_1, OPTION_1_2, OPTION_2_1, OPTION_2_2, OPTION_3_1, OPTION_3_2));

        Assert.assertEquals(expectedFormalizedOptions, actualFormalizedOptions);
    }

    /**
     * OPTION_1_1 по bidirect связи зависит от нескольких значений, некоторые из которых не выгружаются.
     * OPTION_2_1 по bidirect связи зависит от нескольких значений, некоторые из которых не выгружаются.
     */
    @Test
    public void computeFormalizedOptionsForBidirectionalLinksWithSeveralMasterOptions() {
        List<CategoryParam> parameters = Arrays.asList(
            buildParameter(PARAMETER_1, OPTION_1_1, OPTION_1_2),
            buildParameter(PARAMETER_2, OPTION_2_1, OPTION_2_2)
        );

        List<ValueLink> valueLinks = Arrays.asList(
            P_1_BIDIRECTIONAL_P_2_BUILDER.build(OPTION_1_1, OPTION_2_1),
            P_1_BIDIRECTIONAL_P_2_BUILDER.build(OPTION_1_1, OPTION_2_3),
            P_1_BIDIRECTIONAL_P_2_BUILDER.build(OPTION_1_2, OPTION_2_2),
            P_1_BIDIRECTIONAL_P_2_BUILDER.build(OPTION_1_3, OPTION_2_2)
        );

        Set<Long> actualFormalizedOptions =
            FormalizedOptionsExportHelper.computeFormalizedOptions(
                valueLinks, Collections.emptyList(), parameters);

        Set<Long> expectedFormalizedOptions =
            new HashSet<>(Arrays.asList(OPTION_1_1, OPTION_1_2, OPTION_2_1, OPTION_2_2));

        Assert.assertEquals(expectedFormalizedOptions, actualFormalizedOptions);
    }

    /**
     * OPTION_3_1 (targer_option_id) зависит по bidirect связи по двух параметров (PARAMETER_1 и PARAMETER_2).
     * В случае PARAMETER_1 все значения от которых он зависит не выгружаются.
     * В случае PARAMETER_2 есть значения от которых он зависит и которые выгружаются.
     * В результате OPTION_3_1 не выгружается из-за зависимости от PARAMETER_1
     */
    @Test
    public void computeFormalizedOptionsForBidirectionalLinksWithTwoSourceParameters() {
        List<CategoryParam> parameters = Arrays.asList(
            buildParameter(PARAMETER_1, OPTION_1_1, OPTION_1_2),
            buildParameter(PARAMETER_2, OPTION_2_1, OPTION_2_2),
            buildParameter(PARAMETER_3, OPTION_3_1, OPTION_3_2)
        );

        List<ValueLink> valueLinks = Arrays.asList(
            P_1_BIDIRECTIONAL_P_3_BUILDER.build(OPTION_1_3, OPTION_3_1),
            P_1_BIDIRECTIONAL_P_3_BUILDER.build(OPTION_1_4, OPTION_3_1),
            P_2_BIDIRECTIONAL_P_3_BUILDER.build(OPTION_2_1, OPTION_3_1),
            P_2_BIDIRECTIONAL_P_3_BUILDER.build(OPTION_2_3, OPTION_3_1)
        );

        Set<Long> actualFormalizedOptions =
            FormalizedOptionsExportHelper.computeFormalizedOptions(
                valueLinks, Collections.emptyList(), parameters);

        Set<Long> expectedFormalizedOptions =
            new HashSet<>(Arrays.asList(OPTION_1_1, OPTION_1_2, OPTION_2_1, OPTION_2_2, OPTION_3_1, OPTION_3_2));

        Assert.assertEquals(expectedFormalizedOptions, actualFormalizedOptions);
    }

    /**
     * OPTION_1_1 (source_option_id) зависит по bidirect связи по двух параметров (PARAMETER_2 и PARAMETER_3).
     * В случае PARAMETER_2 все значения от которых он зависит не выгружаются.
     * В случае PARAMETER_3 есть значения от которых он зависит и которые выгружаются.
     * В результате OPTION_1_1 не выгружается из-за зависимости от PARAMETER_2
     */
    @Test
    public void computeFormalizedOptionsForBidirectionalLinksWithTwoTargetParameters() {
        List<CategoryParam> parameters = Arrays.asList(
            buildParameter(PARAMETER_1, OPTION_1_1, OPTION_1_2),
            buildParameter(PARAMETER_2, OPTION_2_1, OPTION_2_2),
            buildParameter(PARAMETER_3, OPTION_3_1, OPTION_3_2)
        );

        List<ValueLink> valueLinks = Arrays.asList(
            P_1_BIDIRECTIONAL_P_2_BUILDER.build(OPTION_1_1, OPTION_2_3),
            P_1_BIDIRECTIONAL_P_2_BUILDER.build(OPTION_1_1, OPTION_2_4),
            P_1_BIDIRECTIONAL_P_3_BUILDER.build(OPTION_1_1, OPTION_3_1),
            P_1_BIDIRECTIONAL_P_3_BUILDER.build(OPTION_1_1, OPTION_3_3)
        );

        Set<Long> actualFormalizedOptions =
            FormalizedOptionsExportHelper.computeFormalizedOptions(
                valueLinks, Collections.emptyList(), parameters);

        Set<Long> expectedFormalizedOptions =
            new HashSet<>(Arrays.asList(OPTION_1_1, OPTION_1_2, OPTION_2_1, OPTION_2_2, OPTION_3_1, OPTION_3_2));

        Assert.assertEquals(expectedFormalizedOptions, actualFormalizedOptions);
    }

    /**
     * OPTION_1_1 по bidirect связи зависит от нескольких значений, все из которых не выгружаются.
     * OPTION_2_1 по bidirect связи зависит от нескольких значений, все из которых не выгружаются.
     */
    @Test
    public void computeFormalizedOptionsForBidirectionalLinksWithAllNoActiveMasterOptions() {
        List<CategoryParam> parameters = Arrays.asList(
            buildParameter(PARAMETER_1, OPTION_1_1, OPTION_1_2),
            buildParameter(PARAMETER_2, OPTION_2_1, OPTION_2_2)
        );

        List<ValueLink> valueLinks = Arrays.asList(
            P_1_BIDIRECTIONAL_P_2_BUILDER.build(OPTION_1_1, OPTION_2_3),
            P_1_BIDIRECTIONAL_P_2_BUILDER.build(OPTION_1_1, OPTION_2_4),
            P_1_BIDIRECTIONAL_P_2_BUILDER.build(OPTION_1_3, OPTION_2_1),
            P_1_BIDIRECTIONAL_P_2_BUILDER.build(OPTION_1_4, OPTION_2_1)
        );

        Set<Long> actualFormalizedOptions =
            FormalizedOptionsExportHelper.computeFormalizedOptions(
                valueLinks, Collections.emptyList(), parameters);

        Set<Long> expectedFormalizedOptions = new HashSet<>(Arrays.asList(OPTION_1_2, OPTION_2_2));

        Assert.assertEquals(expectedFormalizedOptions, actualFormalizedOptions);
    }

    /**
     * OPTION_1_1 зависит по reverse связи от нескольких значений, некоторые их которых не выгружаются.
     */
    @Test
    public void computeFormalizedOptionsForReverseLinksWithSeveralMasterOptions() {
        List<CategoryParam> parameters = Arrays.asList(
            buildParameter(PARAMETER_1, OPTION_1_1, OPTION_1_2),
            buildParameter(PARAMETER_2, OPTION_2_1, OPTION_2_2)
        );

        List<ValueLink> valueLinks = Arrays.asList(
            P_1_REVERSE_P_2_BUILDER.build(OPTION_1_1, OPTION_2_1),
            P_1_REVERSE_P_2_BUILDER.build(OPTION_1_1, OPTION_2_3)
        );

        Set<Long> actualFormalizedOptions =
            FormalizedOptionsExportHelper.computeFormalizedOptions(
                valueLinks, Collections.emptyList(), parameters);

        Set<Long> expectedFormalizedOptions =
            new HashSet<>(Arrays.asList(OPTION_1_1, OPTION_1_2, OPTION_2_1, OPTION_2_2));

        Assert.assertEquals(expectedFormalizedOptions, actualFormalizedOptions);
    }

    /**
     * OPTION_1_1 зависит по reverse связи от нескольких значений, все из которых не выгружаются.
     */
    @Test
    public void computeFormalizedOptionsForReverseLinksWithAllNoActiveMasterOptions() {
        List<CategoryParam> parameters = Arrays.asList(
            buildParameter(PARAMETER_1, OPTION_1_1, OPTION_1_2),
            buildParameter(PARAMETER_2, OPTION_2_1, OPTION_2_2)
        );

        List<ValueLink> valueLinks = Arrays.asList(
            P_1_REVERSE_P_2_BUILDER.build(OPTION_1_1, OPTION_2_3),
            P_1_REVERSE_P_2_BUILDER.build(OPTION_1_1, OPTION_2_4)
        );

        Set<Long> actualFormalizedOptions =
            FormalizedOptionsExportHelper.computeFormalizedOptions(
                valueLinks, Collections.emptyList(), parameters);

        Set<Long> expectedFormalizedOptions =
            new HashSet<>(Arrays.asList(OPTION_1_2, OPTION_2_1, OPTION_2_2));

        Assert.assertEquals(expectedFormalizedOptions, actualFormalizedOptions);
    }

    /**
     * OPTION_1_1 зависит по reverse связи по двух параметров (PARAMETER_2 и PARAMETER_3).
     * В случае PARAMETER_2 все значения от которых он зависит не выгружаются.
     * В случае PARAMETER_3 есть значения от которых он зависит и которые выгружаются.
     * В результате OPTION_1_1 не выгружается из-за зависимости от PARAMETER_2
     */
    @Test
    public void computeFormalizedOptionsForReverseLinksWithTwoSourceParameters() {
        List<CategoryParam> parameters = Arrays.asList(
            buildParameter(PARAMETER_1, OPTION_1_1, OPTION_1_2),
            buildParameter(PARAMETER_2, OPTION_2_1, OPTION_2_2),
            buildParameter(PARAMETER_3, OPTION_3_1, OPTION_3_2)
        );

        List<ValueLink> valueLinks = Arrays.asList(
            P_1_REVERSE_P_2_BUILDER.build(OPTION_1_1, OPTION_2_3),
            P_1_REVERSE_P_2_BUILDER.build(OPTION_1_1, OPTION_2_4),
            P_1_REVERSE_P_3_BUILDER.build(OPTION_1_1, OPTION_3_1),
            P_1_REVERSE_P_3_BUILDER.build(OPTION_1_1, OPTION_3_3)
        );

        Set<Long> actualFormalizedOptions =
            FormalizedOptionsExportHelper.computeFormalizedOptions(
                valueLinks, Collections.emptyList(), parameters);

        Set<Long> expectedFormalizedOptions =
            new HashSet<>(Arrays.asList(OPTION_1_1, OPTION_1_2, OPTION_2_1, OPTION_2_2, OPTION_3_1, OPTION_3_2));

        Assert.assertEquals(expectedFormalizedOptions, actualFormalizedOptions);
    }

    /**
     * Проверка транзитивного исключения option-ов.
     * OPTION_2_2 исключается так как зависит от OPTION_1_3 который не выгружается
     * OPTION_3_2 исключется так как зависит от OPTION_2_2
     */
    @Test
    public void computeFormalizedOptionsExpectedSkipThroughFewLinks() {
        List<CategoryParam> parameters = Arrays.asList(
            buildParameter(PARAMETER_1, OPTION_1_1, OPTION_1_2),
            buildParameter(PARAMETER_2, OPTION_2_1, OPTION_2_2),
            buildParameter(PARAMETER_3, OPTION_3_1, OPTION_3_2)
        );

        List<ValueLink> valueLinks = Arrays.asList(
            P_1_DIRECT_P_2_BUILDER.build(OPTION_1_1, OPTION_2_1),
            P_1_DIRECT_P_2_BUILDER.build(OPTION_1_3, OPTION_2_2),
            P_2_DIRECT_P_3_BUILDER.build(OPTION_2_1, OPTION_3_1),
            P_2_DIRECT_P_3_BUILDER.build(OPTION_2_2, OPTION_3_2)
        );

        Set<Long> actualFormalizedOptions =
            FormalizedOptionsExportHelper.computeFormalizedOptions(
                valueLinks, Collections.emptyList(), parameters);

        Set<Long> expectedFormalizedOptions =
            new HashSet<>(Arrays.asList(OPTION_1_1, OPTION_1_2, OPTION_2_1, OPTION_3_1));

        Assert.assertEquals(expectedFormalizedOptions, actualFormalizedOptions);
    }

    /**
     * Дана direct связь PARAMETER_1 --> PARAMETER_2.
     * OPTION_2_2 не имеет ни какую связь со значениями из PARAMETER_1.
     * Хотя мы знаем что в этом случае формализатор ни когда не формализует OPTION_2_2, мы это значение
     * не выкидываем
     */
    @Test
    public void computeFormalizedOptionsExpectedLeaveOptionsWithoutDirectLink() {
        List<CategoryParam> parameters = Arrays.asList(
            buildParameter(PARAMETER_1, OPTION_1_1, OPTION_1_2),
            buildParameter(PARAMETER_2, OPTION_2_1, OPTION_2_2)
        );

        List<ValueLink> valueLinks = Arrays.asList(
            P_1_DIRECT_P_2_BUILDER.build(OPTION_1_1, OPTION_2_1)
        );

        Set<Long> actualFormalizedOptions =
            FormalizedOptionsExportHelper.computeFormalizedOptions(
                valueLinks, Collections.emptyList(), parameters);

        Set<Long> expectedFormalizedOptions =
            new HashSet<>(Arrays.asList(OPTION_1_1, OPTION_1_2, OPTION_2_1, OPTION_2_2));

        Assert.assertEquals(expectedFormalizedOptions, actualFormalizedOptions);
    }

    @Test
    public void computeFormalizedOptionsExpectedSkipNoActiveOptions() {
        CategoryParam parameter1 = buildParameter(PARAMETER_1, OPTION_1_1, OPTION_1_2);
        parameter1.getOptions().stream().filter(o -> OPTION_1_1.equals(o.getId())).forEach(o -> o.setActive(false));

        List<ValueLink> valueLinks = Collections.emptyList();

        Set<Long> actualFormalizedOptions =
            FormalizedOptionsExportHelper.computeFormalizedOptions(
                valueLinks, Collections.emptyList(), Arrays.asList(parameter1));

        Set<Long> expectedFormalizedOptions = new HashSet<>(Arrays.asList(OPTION_1_2));

        Assert.assertEquals(expectedFormalizedOptions, actualFormalizedOptions);
    }

    @Test
    public void computeFormalizedOptionsExpectedSkipCategoryAndConditionalValues() {
        CategoryParam parameter1 = buildParameter(PARAMETER_1, OPTION_1_1, OPTION_1_2, OPTION_1_3, OPTION_1_4);

        List<GLRule> rules = new ArrayList<>();
        GLRule categoryValues = new GLRuleImpl();
        categoryValues.setType(GLRuleType.CATEGORY_VALUES);
        GLRulePredicate excludeRevokeValues = new GLRulePredicate();
        excludeRevokeValues.setParamId(parameter1.getId());
        excludeRevokeValues.setCondition(GLRulePredicate.VALUE_UNDEFINED);
        excludeRevokeValues.setExcludeRevokeValueIds(Arrays.asList(OPTION_1_2, OPTION_1_3));
        categoryValues.getThens().add(excludeRevokeValues);
        rules.add(categoryValues);

        GLRule conditionalValues = new GLRuleImpl();
        conditionalValues.setType(GLRuleType.CONDITIONAL_VALUES);
        excludeRevokeValues = new GLRulePredicate();
        excludeRevokeValues.setParamId(parameter1.getId());
        excludeRevokeValues.setCondition(GLRulePredicate.VALUE_UNDEFINED);
        excludeRevokeValues.setExcludeRevokeValueIds(Arrays.asList(OPTION_1_3, OPTION_1_4));
        conditionalValues.getThens().add(excludeRevokeValues);
        rules.add(conditionalValues);

        Set<Long> actualFormalizedOptions =
            FormalizedOptionsExportHelper.computeFormalizedOptions(
                Collections.emptyList(), rules, Arrays.asList(parameter1));

        Set<Long> expectedFormalizedOptions = new HashSet<>(Arrays.asList(OPTION_1_2, OPTION_1_3, OPTION_1_4));

        Assert.assertEquals(expectedFormalizedOptions, actualFormalizedOptions);
    }

    @Test
    public void computeFormalizedOptionsExpectedSkipCategoryAndConditionalAliases() {
        CategoryParam parameter1 = buildParameter(PARAMETER_1, OPTION_1_1, OPTION_1_2, OPTION_1_3, OPTION_1_4);
        CategoryParam parameter2 = buildParameter(PARAMETER_2, OPTION_2_1, OPTION_2_2);

        List<GLRule> rules = new ArrayList<>();
        GLRule categoryValues = new GLRuleImpl();
        categoryValues.setType(GLRuleType.CATEGORY_VALUES);
        GLRulePredicate excludeRevokeValues = new GLRulePredicate();
        excludeRevokeValues.setParamId(parameter1.getId());
        excludeRevokeValues.setCondition(GLRulePredicate.VALUE_UNDEFINED);
        excludeRevokeValues.setExcludeRevokeValueIds(Arrays.asList(OPTION_1_2));
        categoryValues.getThens().add(excludeRevokeValues);
        rules.add(categoryValues);

        GLRule categoryAlias = new GLRuleImpl();
        categoryAlias.setType(GLRuleType.CATEGORY_ALIASES);
        GLRulePredicate alias = new GLRulePredicate();
        alias.setParamId(parameter1.getId());
        alias.setCondition(GLRulePredicate.ENUM_MATCHES);
        alias.setValueId(OPTION_1_3);
        categoryAlias.getIfs().add(alias);
        rules.add(categoryAlias);

        GLRule conditionalAlias = new GLRuleImpl();
        conditionalAlias.setType(GLRuleType.CONDITIONAL_ALIASES);
        alias = new GLRulePredicate();
        alias.setParamId(parameter1.getId());
        alias.setCondition(GLRulePredicate.ENUM_MATCHES);
        alias.setValueId(OPTION_1_4);
        conditionalAlias.getIfs().add(alias);
        alias = new GLRulePredicate();
        alias.setParamId(parameter2.getId());
        alias.setCondition(GLRulePredicate.ENUM_MATCHES);
        alias.setValueId(OPTION_2_2);
        conditionalAlias.getIfs().add(alias);
        rules.add(conditionalAlias);

        Set<Long> actualFormalizedOptions =
            FormalizedOptionsExportHelper.computeFormalizedOptions(
                Collections.emptyList(), rules, Arrays.asList(parameter1, parameter2));

        Set<Long> expectedFormalizedOptions = new HashSet<>(
            Arrays.asList(OPTION_1_2, OPTION_1_3, OPTION_1_4, OPTION_2_1, OPTION_2_2));

        Assert.assertEquals(expectedFormalizedOptions, actualFormalizedOptions);
    }

    @Test
    public void computeFormalizedOptionsOnlyConditionalRules() {
        CategoryParam parameter1 = buildParameter(PARAMETER_1, OPTION_1_1, OPTION_1_2, OPTION_1_3, OPTION_1_4);
        CategoryParam parameter2 = buildParameter(PARAMETER_2, OPTION_2_1, OPTION_2_2);

        List<GLRule> rules = new ArrayList<>();
        GLRule conditionalValues = new GLRuleImpl();
        conditionalValues.setType(GLRuleType.CONDITIONAL_VALUES);
        GLRulePredicate excludeRevokeValues = new GLRulePredicate();
        excludeRevokeValues.setParamId(parameter1.getId());
        excludeRevokeValues.setCondition(GLRulePredicate.VALUE_UNDEFINED);
        excludeRevokeValues.setExcludeRevokeValueIds(Arrays.asList(OPTION_1_3));
        conditionalValues.getThens().add(excludeRevokeValues);
        rules.add(conditionalValues);

        GLRule conditionalAlias = new GLRuleImpl();
        conditionalAlias.setType(GLRuleType.CONDITIONAL_ALIASES);
        GLRulePredicate alias = new GLRulePredicate();
        alias.setParamId(parameter1.getId());
        alias.setCondition(GLRulePredicate.ENUM_MATCHES);
        alias.setValueId(OPTION_1_4);
        conditionalAlias.getIfs().add(alias);
        alias = new GLRulePredicate();
        alias.setParamId(parameter2.getId());
        alias.setCondition(GLRulePredicate.ENUM_MATCHES);
        alias.setValueId(OPTION_2_2);
        conditionalAlias.getIfs().add(alias);
        rules.add(conditionalAlias);

        Set<Long> actualFormalizedOptions =
            FormalizedOptionsExportHelper.computeFormalizedOptions(
                Collections.emptyList(), rules, Arrays.asList(parameter1, parameter2));

        Set<Long> expectedFormalizedOptions = new HashSet<>(
            Arrays.asList(OPTION_1_1, OPTION_1_2, OPTION_1_3, OPTION_1_4, OPTION_2_1, OPTION_2_2));

        Assert.assertEquals(expectedFormalizedOptions, actualFormalizedOptions);
    }

    @Test
    public void computeFormalizedOptionsOtherRuleTypes() {
        CategoryParam parameter1 = buildParameter(PARAMETER_1, OPTION_1_1, OPTION_1_2, OPTION_1_3, OPTION_1_4);

        List<GLRule> rules = new ArrayList<>();
        GLRule categoryValues = new GLRuleImpl();
        categoryValues.setType(GLRuleType.CATEGORY_DEFINITIONS);
        GLRulePredicate excludeRevokeValues = new GLRulePredicate();
        excludeRevokeValues.setParamId(parameter1.getId());
        excludeRevokeValues.setCondition(GLRulePredicate.VALUE_UNDEFINED);
        excludeRevokeValues.setExcludeRevokeValueIds(Arrays.asList(OPTION_1_2, OPTION_1_3));
        categoryValues.getThens().add(excludeRevokeValues);
        rules.add(categoryValues);

        GLRule categoryAlias = new GLRuleImpl();
        categoryAlias.setType(GLRuleType.MANUAL);
        GLRulePredicate alias = new GLRulePredicate();
        alias.setParamId(parameter1.getId());
        alias.setCondition(GLRulePredicate.ENUM_MATCHES);
        alias.setValueId(OPTION_1_4);
        categoryAlias.getIfs().add(alias);
        rules.add(categoryAlias);

        Set<Long> actualFormalizedOptions =
            FormalizedOptionsExportHelper.computeFormalizedOptions(
                Collections.emptyList(), rules, Arrays.asList(parameter1));

        Set<Long> expectedFormalizedOptions = new HashSet<>(
            Arrays.asList(OPTION_1_1, OPTION_1_2, OPTION_1_3, OPTION_1_4));

        Assert.assertEquals(expectedFormalizedOptions, actualFormalizedOptions);
    }

    private CategoryParam buildParameter(Long paramId, Long... optionIds) {
        CategoryParam param = new Parameter();
        param.setId(paramId);

        for (Long optionId : optionIds) {
            Option option = new OptionImpl();
            option.setId(optionId);
            param.addOption(option);
        }

        return param;
    }

    private static class ValueLinkBuilder {
        private Long sourceParamId;
        private Long targetParamId;
        private LinkDirection linkDirection;

        ValueLinkBuilder(Long sourceParamId, Long targetParamId, LinkDirection linkDirection) {
            this.sourceParamId = sourceParamId;
            this.targetParamId = targetParamId;
            this.linkDirection = linkDirection;
        }

        ValueLink build(Long sourceOptionId, Long targetOptionId) {
            return new ValueLink(sourceParamId, sourceOptionId, targetParamId, targetOptionId,
                                 linkDirection, ValueLinkType.GENERAL, 0L);
        }
    }
}
