package ru.yandex.market.mbo.core.templates.generator;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.db.forms.ModelFormService;
import ru.yandex.market.mbo.gwt.models.forms.model.FormType;
import ru.yandex.market.mbo.gwt.models.forms.model.ModelForm;
import ru.yandex.market.mbo.gwt.models.forms.model.ModelFormTab;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.templates.generator.ModelGeneratorType;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.user.AutoUser;
import ru.yandex.market.mbo.utils.MboAssertions;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.BOOLEAN1;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.CATEGORY_ID1;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.ENUM1;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.NUMERIC1;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.NUMERIC_ENUM1;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.STRING1;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.getBooleanParam;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.getEnumParam;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.getNameParam;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.getNumericEnumParam;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.getNumericParam;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.getStringParam;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.getVendorParam;

/**
 * @author danfertev
 * @since 20.06.2019
 */
public class IncorrectPartnerSkuGeneratorTest {
    private static final long UID = 5757L;
    private static final String BLOCK_NAME = "block";
    private static final double MIN_VALUE = 0;
    private static final double MAX_VALUE = 100;

    private ModelFormService modelFormService;
    private ParameterLoaderServiceStub parameterLoader;
    private AutoUser autoUser;
    private ModelGenerator modelGenerator;

    @Before
    public void setUp() {
        modelFormService = mock(ModelFormService.class);
        parameterLoader = new ParameterLoaderServiceStub();
        parameterLoader.addCategoryParam(getVendorParam(CATEGORY_ID1));
        parameterLoader.addCategoryParam(getNameParam(CATEGORY_ID1));
        autoUser = new AutoUser(UID);
        modelGenerator = new IncorrectPartnerSkuGenerator(modelFormService, parameterLoader, autoUser);
    }

    @Test
    public void testGenerateOnlyVendorAndNameParamsIfNoCategoryParams() {
        CommonModel model = modelGenerator.generateModel(CATEGORY_ID1);

        assertNoGeneratedParams(model);
    }

    @Test
    public void testGenerateWithParentModel() {
        CommonModel model = modelGenerator.generateModel(CATEGORY_ID1);

        Assertions.assertThat(model.getRelation(ModelRelation.RelationType.SKU_PARENT_MODEL).get().getModel())
            .isNotNull();
    }

    @Test
    public void testSkipHiddenAndIgnoredParams() {
        CategoryParam hiddenParam = getEnumParam(CATEGORY_ID1, ENUM1);
        hiddenParam.setHidden(true);
        CategoryParam vendorLineParam = getEnumParam(CATEGORY_ID1, XslNames.VENDOR_LINE);
        parameterLoader.addCategoryParam(hiddenParam);
        parameterLoader.addCategoryParam(vendorLineParam);

        CommonModel model = modelGenerator.generateModel(CATEGORY_ID1);

        assertNoGeneratedParams(model);
    }

    @Test
    public void testGenerateStringParam() {
        CategoryParam stringParam = getStringParam(CATEGORY_ID1, STRING1);
        parameterLoader.addCategoryParam(stringParam);
        initModelFrom(CATEGORY_ID1, stringParam);

        CommonModel model = modelGenerator.generateModel(CATEGORY_ID1);

        MboAssertions.assertThat(model).getParameterValues(STRING1).values(IncorrectPartnerSkuGenerator.STRING_VALUE);
    }

    @Test
    public void testGenerateMultifieldStringParam() {
        CategoryParam stringParam = getStringParam(CATEGORY_ID1, STRING1);
        stringParam.setMultifield(true);
        parameterLoader.addCategoryParam(stringParam);
        initModelFrom(CATEGORY_ID1, stringParam);

        CommonModel model = modelGenerator.generateModel(CATEGORY_ID1);

        String[] stringValues = IntStream.range(0, IncorrectPartnerSkuGenerator.MULTI_VALUE_COUNT)
            .mapToObj(i -> IncorrectPartnerSkuGenerator.appendIndex(IncorrectPartnerSkuGenerator.STRING_VALUE, i))
            .toArray(String[]::new);

        MboAssertions.assertThat(model).getParameterValues(STRING1).values(stringValues);
    }

    @Test
    public void testGenerateNumericParam() {
        CategoryParam numericParam = getNumericParam(CATEGORY_ID1, NUMERIC1);
        parameterLoader.addCategoryParam(numericParam);
        initModelFrom(CATEGORY_ID1, numericParam);

        CommonModel model = modelGenerator.generateModel(CATEGORY_ID1);

        MboAssertions.assertThat(model).getParameterValues(NUMERIC1)
            .values(IncorrectPartnerSkuGenerator.NUMERIC_VALUE);
    }

    @Test
    public void testGenerateMultifieldNumericParam() {
        CategoryParam numericParam = getNumericParam(CATEGORY_ID1, NUMERIC1);
        numericParam.setMultifield(true);
        parameterLoader.addCategoryParam(numericParam);
        initModelFrom(CATEGORY_ID1, numericParam);

        CommonModel model = modelGenerator.generateModel(CATEGORY_ID1);

        Double[] numericValues = IntStream.range(0, IncorrectPartnerSkuGenerator.MULTI_VALUE_COUNT)
            .mapToObj(i -> IncorrectPartnerSkuGenerator.NUMERIC_VALUE + i)
            .toArray(Double[]::new);

        MboAssertions.assertThat(model).getParameterValues(NUMERIC1).values(numericValues);
    }

    @Test
    public void testGenerateNumericParamUseMinValue() {
        CategoryParam numericParam = getNumericParam(CATEGORY_ID1, NUMERIC1);
        numericParam.setMinValue(BigDecimal.valueOf(MIN_VALUE));
        parameterLoader.addCategoryParam(numericParam);
        initModelFrom(CATEGORY_ID1, numericParam);

        CommonModel model = modelGenerator.generateModel(CATEGORY_ID1);

        MboAssertions.assertThat(model).getParameterValues(NUMERIC1)
            .values(MIN_VALUE - IncorrectPartnerSkuGenerator.NUMERIC_DIFF);
    }

    @Test
    public void testGenerateNumericParamUseMaxValue() {
        CategoryParam numericParam = getNumericParam(CATEGORY_ID1, NUMERIC1);
        numericParam.setMaxValue(BigDecimal.valueOf(MAX_VALUE));
        parameterLoader.addCategoryParam(numericParam);
        initModelFrom(CATEGORY_ID1, numericParam);

        CommonModel model = modelGenerator.generateModel(CATEGORY_ID1);

        MboAssertions.assertThat(model).getParameterValues(NUMERIC1)
            .values(MAX_VALUE + IncorrectPartnerSkuGenerator.NUMERIC_DIFF);
    }

    @Test
    public void testGenerateMultifieldNumericParamUseMaxValue() {
        CategoryParam numericParam = getNumericParam(CATEGORY_ID1, NUMERIC1);
        numericParam.setMaxValue(BigDecimal.valueOf(MAX_VALUE));
        numericParam.setMultifield(true);
        parameterLoader.addCategoryParam(numericParam);
        initModelFrom(CATEGORY_ID1, numericParam);

        CommonModel model = modelGenerator.generateModel(CATEGORY_ID1);

        Double[] numericValues = IntStream.range(0, IncorrectPartnerSkuGenerator.MULTI_VALUE_COUNT)
            .mapToObj(i -> MAX_VALUE + IncorrectPartnerSkuGenerator.NUMERIC_DIFF + i)
            .toArray(Double[]::new);

        MboAssertions.assertThat(model).getParameterValues(NUMERIC1).values(numericValues);
    }

    @Test
    public void testGenerateEnumParam() {
        CategoryParam enumParam = getEnumParam(CATEGORY_ID1, ENUM1);
        parameterLoader.addCategoryParam(enumParam);
        initModelFrom(CATEGORY_ID1, enumParam);

        CommonModel model = modelGenerator.generateModel(CATEGORY_ID1);

        MboAssertions.assertThat(model).getParameterValuesHypothesis(ENUM1)
            .values(IncorrectPartnerSkuGenerator.ENUM_VALUE);
    }

    @Test
    public void testGenerateMultifieldEnumParam() {
        CategoryParam enumParam = getEnumParam(CATEGORY_ID1, ENUM1);
        enumParam.setMultifield(true);
        parameterLoader.addCategoryParam(enumParam);
        initModelFrom(CATEGORY_ID1, enumParam);

        CommonModel model = modelGenerator.generateModel(CATEGORY_ID1);

        Assertions.assertThat(model.getParameterValueHypothesis(ENUM1).get().getStringValue())
            .extracting(Word::getWord)
            .allMatch(w -> w.contains(IncorrectPartnerSkuGenerator.ENUM_VALUE));
    }

    @Test
    public void testGenerateNumericEnumParam() {
        CategoryParam numericEnumParam = getNumericEnumParam(CATEGORY_ID1, NUMERIC_ENUM1);
        parameterLoader.addCategoryParam(numericEnumParam);
        initModelFrom(CATEGORY_ID1, numericEnumParam);

        CommonModel model = modelGenerator.generateModel(CATEGORY_ID1);

        MboAssertions.assertThat(model).getParameterValuesHypothesis(NUMERIC_ENUM1)
            .values(String.valueOf(IncorrectPartnerSkuGenerator.NUMERIC_VALUE));
    }

    @Test
    public void testGenerateMultifieldNumericEnumParam() {
        CategoryParam numericEnumParam = getNumericEnumParam(CATEGORY_ID1, NUMERIC_ENUM1);
        numericEnumParam.setMultifield(true);
        parameterLoader.addCategoryParam(numericEnumParam);
        initModelFrom(CATEGORY_ID1, numericEnumParam);

        CommonModel model = modelGenerator.generateModel(CATEGORY_ID1);

        String[] numericEnumValues = IntStream.range(0, IncorrectPartnerSkuGenerator.MULTI_VALUE_COUNT)
            .mapToObj(i -> IncorrectPartnerSkuGenerator.NUMERIC_VALUE + i)
            .map(String::valueOf)
            .toArray(String[]::new);

        MboAssertions.assertThat(model).getParameterValuesHypothesis(NUMERIC_ENUM1).values(numericEnumValues);
    }

    @Test
    public void testGenerateBooleanParam() {
        CategoryParam booleanParam = getBooleanParam(CATEGORY_ID1, BOOLEAN1);
        parameterLoader.addCategoryParam(booleanParam);
        initModelFrom(CATEGORY_ID1, booleanParam);

        CommonModel model = modelGenerator.generateModel(CATEGORY_ID1);

        MboAssertions.assertThat(model).getParameterValues(BOOLEAN1).values(true);
    }

    @Test
    public void testGenerateMultifieldBooleanParam() {
        CategoryParam booleanParam = getBooleanParam(CATEGORY_ID1, BOOLEAN1);
        booleanParam.setMultifield(true);
        parameterLoader.addCategoryParam(booleanParam);
        initModelFrom(CATEGORY_ID1, booleanParam);

        CommonModel model = modelGenerator.generateModel(CATEGORY_ID1);

        MboAssertions.assertThat(model).getParameterValues(BOOLEAN1).values(true);
    }

    @Test
    public void testUseOnlyParamAndMDMTabs() {
        CategoryParam stringParam = getStringParam(CATEGORY_ID1, STRING1);
        CategoryParam enumParam = getEnumParam(CATEGORY_ID1, ENUM1);
        parameterLoader.addCategoryParam(stringParam);
        parameterLoader.addCategoryParam(enumParam);

        initModelFrom(CATEGORY_ID1, "TAB1", stringParam, enumParam);
        assertNoGeneratedParams(modelGenerator.generateModel(CATEGORY_ID1));

        ModelForm modelForm = new ModelForm();
        ModelFormTab paramTab = new ModelFormTab(XslNames.PARAMETERS_TAB_TITLE);
        modelForm.addTab(paramTab);
        paramTab.addBlock(BLOCK_NAME, Collections.singletonList(stringParam.getXslName()));
        ModelFormTab mdmTab = new ModelFormTab(XslNames.MDM_TAB_TITLE);
        modelForm.addTab(mdmTab);
        mdmTab.addBlock(BLOCK_NAME, Collections.singletonList(enumParam.getXslName()));
        when(modelFormService.getModelForm(eq(CATEGORY_ID1), eq(FormType.MODEL_EDITOR))).thenReturn(modelForm);

        CommonModel model = modelGenerator.generateModel(CATEGORY_ID1);
        MboAssertions.assertThat(model).getParameterValues(STRING1)
            .values(IncorrectPartnerSkuGenerator.STRING_VALUE);
        MboAssertions.assertThat(model).getParameterValuesHypothesis(ENUM1)
            .values(IncorrectPartnerSkuGenerator.ENUM_VALUE);
    }

    private void initModelFrom(long categoryId, String tabName, CategoryParam... params) {
        ModelForm modelForm = new ModelForm();
        ModelFormTab paramTab = new ModelFormTab(tabName);
        List<String> paramNames = Arrays.stream(params).map(CategoryParam::getXslName).collect(Collectors.toList());

        modelForm.addTab(paramTab);
        paramTab.addBlock(BLOCK_NAME, paramNames);

        when(modelFormService.getModelForm(eq(categoryId), eq(FormType.MODEL_EDITOR))).thenReturn(modelForm);
    }

    private void initModelFrom(long categoryId, CategoryParam... params) {
        initModelFrom(categoryId, XslNames.PARAMETERS_TAB_TITLE, params);
    }

    private static void assertNoGeneratedParams(CommonModel model) {
        Assertions.assertThat(model.getParameterValues()).hasSize(2);
        MboAssertions.assertThat(model).getParameterValues(XslNames.NAME)
            .values(ModelGeneratorType.INCORRECT_PARTNER_SKU.getName());
        MboAssertions.assertThat(model).getParameterValues(XslNames.VENDOR)
            .values(KnownIds.NOT_DEFINED_GLOBAL_VENDOR);
    }
}
