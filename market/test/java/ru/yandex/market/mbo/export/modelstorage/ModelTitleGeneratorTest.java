package ru.yandex.market.mbo.export.modelstorage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import ru.yandex.market.mbo.core.title.ModelTitleGenerator;
import ru.yandex.market.mbo.core.title.SkuTitleGenerator;
import ru.yandex.market.mbo.gwt.models.gurulight.GLMeasure;
import ru.yandex.market.mbo.gwt.models.gurulight.SizeMeasureDto;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.params.Unit;
import ru.yandex.market.mbo.gwt.models.rules.ParameterBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParametersBuilder;
import ru.yandex.market.mbo.gwt.models.titlemaker.ForTitleParameter;
import ru.yandex.market.mbo.gwt.models.titlemaker.ForTitleSizeMeasure;
import ru.yandex.market.mbo.gwt.models.titlemaker.ModelTitle;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;

/**
 * Created by annaalkh on 04.05.17.
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ModelTitleGeneratorTest {

    private List<ForTitleParameter> params;

    @Before
    public void before() {
        params = ParametersBuilder
            .startParameters()

            .startParameter()
            .id(1).xsl("age").name("Age").type(Param.Type.NUMERIC)
            .skuParameterMode(SkuParameterMode.SKU_DEFINING)
            .startUnit().name("возраст").reportName("лет").scale(0).measureId(0).id(0).endUnit()
            .endParameter()

            .startParameter()
            .id(2).xsl("name").name("Name").type(Param.Type.STRING)
            .endParameter()

            .startParameter()
            .id(3).xsl("vendor").name("Vendor").type(Param.Type.ENUM)
            .option(11, "AAA")
            .option(12, "BBB")
            .endParameter()

            .startParameter()
            .id(4).xsl("is_universal").name("Universal").type(Param.Type.BOOLEAN)
            .skuParameterMode(SkuParameterMode.SKU_DEFINING)
            .endParameter()

            .startParameter()
            .id(5).xsl("voltage").name("Voltage").type(Param.Type.NUMERIC_ENUM)
            .startUnit().name("напряжение").reportName("В").scale(1).measureId(1).id(2).endUnit()
            .skuParameterMode(SkuParameterMode.SKU_DEFINING)
            .option(13, 220)
            .option(14, 380)
            .endParameter()

            .startParameter()
            .id(106).xsl(XslNames.USE_NAME_AS_TITLE).name("Блаблабла").type(Param.Type.BOOLEAN)
            .endParameter()

            .endParameters()
            .stream()
            .map(ForTitleParameter::fromCategoryParam)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    @Test
    public void createSimpleTitle() {
        String template = "{\"delimiter\":\" \",\"values\":[[(1 ),(v1 )],[(1 ),(v3 )],[(t0 ),(t0 )]]}";

        List<ParameterValue> paramValues = generateTestParamValues();

        CommonModel model = generateTestModel();
        model.addParameterValues(paramValues);

        ModelTitleGenerator titleGenerator = new ModelTitleGenerator(template, params, Collections.emptyList());
        String title = titleGenerator.createTitle(model).getTitle();

        Assert.assertEquals("10 AAA Model name", title);
    }

    @Test
    public void createManualTitle() {
        String template = "{\"delimiter\":\" \",\"values\":[[(1 ),(v1 )],[(1 ),(v3 )],[(t0 ),(t0 )]]}";

        List<ParameterValue> paramValues = generateTestParamValues();

        CommonModel model = generateTestModel();
        model.addParameterValues(paramValues);

        ParameterValue manualTitleValue = new ParameterValue();
        manualTitleValue.setParamId(106);
        manualTitleValue.setXslName(XslNames.USE_NAME_AS_TITLE);
        manualTitleValue.setType(Param.Type.BOOLEAN);
        manualTitleValue.setBooleanValue(true);

        model.addParameterValue(manualTitleValue);

        ModelTitleGenerator titleGenerator = new ModelTitleGenerator(template, params, Collections.emptyList());
        String title = titleGenerator.createTitle(model).getTitle();

        Assert.assertEquals("Model name", title);
    }


    @Test
    public void titleGeneratorClearScope() {
        String template = "{\"delimiter\":\" \",\"values\":[[(1 ),(v1 )],[(1 ),(v3 )],[(t0 ),(t0 )]]}";

        List<ParameterValue> paramValues = generateTestParamValues();
        List<ParameterValue> exceptAge = paramValues.stream()
            .filter(
                pv -> !pv.getXslName().equals("age")
            ).collect(Collectors.toList());

        CommonModel model1 = generateTestModel();
        model1.addParameterValues(exceptAge);

        CommonModel model2 = generateTestModel();
        model2.addParameterValues(paramValues);

        CommonModel model3 = generateTestModel();
        model3.addParameterValues(exceptAge);

        ModelTitleGenerator titleGenerator = new ModelTitleGenerator(template, params, Collections.emptyList());

        String title1 = titleGenerator.createTitle(model1).getTitle();
        String title2 = titleGenerator.createTitle(model2).getTitle();
        String title3 = titleGenerator.createTitle(model3).getTitle();

        Assert.assertEquals("missed value is skipped", "AAA Model name", title1);
        Assert.assertEquals("age is present", "10 AAA Model name", title2);
        Assert.assertEquals("generator removed age from scope", "AAA Model name", title3);
    }

    @Test
    public void titleGeneratorClearScopeMultivalues() {
        String template = "{\"delimiter\":\" \",\"values\":[[(t0 ),(t0 )],[(count(s1) > 0),(join(s1) )],[(v3 ),(v3)]]}";

        List<ParameterValue> paramValues = generateTestParamValues();
        ParameterValue ageValue = new ParameterValue();
        ageValue.setParamId(1);
        ageValue.setXslName("age");
        ageValue.setType(Param.Type.NUMERIC);
        ageValue.setNumericValue(BigDecimal.valueOf(14));
        paramValues.add(ageValue);

        List<ParameterValue> exceptAge = paramValues.stream()
            .filter(
                pv -> !pv.getXslName().equals("age")
            ).collect(Collectors.toList());

        CommonModel model1 = generateTestModel();
        model1.addParameterValues(exceptAge);

        CommonModel model2 = generateTestModel();
        model2.addParameterValues(paramValues);

        CommonModel model3 = generateTestModel();
        model3.addParameterValues(exceptAge);

        ModelTitleGenerator titleGenerator = new ModelTitleGenerator(template, params, Collections.emptyList());

        String title1 = titleGenerator.createTitle(model1).getTitle();
        String title2 = titleGenerator.createTitle(model2).getTitle();
        String title3 = titleGenerator.createTitle(model3).getTitle();

        Assert.assertEquals("missed value is skipped", "Model name AAA", title1);
        Assert.assertEquals("age is present", "Model name 10, 14 AAA", title2);
        Assert.assertEquals("generator removed age from scope", "Model name AAA", title3);
    }

    @Test
    public void showOnlyLastValueFromMultivalues() {
        String template = "{\"delimiter\":\" \",\"values\":[[(t0 ),(t0 )], [(1 ),(v1 )]]}";

        CommonModel model = generateTestModel();
        model.addParameterValues(generateTestParamValues());

        ParameterValue ageValue = new ParameterValue();
        ageValue.setParamId(1);
        ageValue.setXslName("age");
        ageValue.setType(Param.Type.NUMERIC);
        ageValue.setNumericValue(BigDecimal.valueOf(14));
        model.addParameterValue(ageValue);

        ModelTitleGenerator titleGenerator = new ModelTitleGenerator(template, params, Collections.emptyList());

        String title1 = titleGenerator.createTitle(model).getTitle();
        Assert.assertEquals("missed value is skipped", "Model name 14", title1);

    }

    @Test
    public void createSimpleTitleWithCondition() {
        String template = "{\"delimiter\":\" \",\"values\":[[(v1 ),(v1 ),null,(true)]," +
            "[(v3 ),(v3 ),null,(true)],[(t0 ),(t0 )]]}";

        List<ParameterValue> paramValues = generateTestParamValues();

        CommonModel model = generateTestModel();
        model.addParameterValues(paramValues);

        ModelTitleGenerator titleGenerator = new ModelTitleGenerator(template, params, Collections.emptyList());
        String title = titleGenerator.createTitle(model).getTitle();

        Assert.assertEquals("10 AAA Model name", title);
    }

    @Test
    public void createTitleWithEnumParam() {
        String template = "{\"delimiter\":\" \",\"values\":[[(v130 ),(v130 ),null,(true)]," +
            "[(v3 ),(v3 ),null,(true)],[(t0 ),(t0 )]]}";

        List<ParameterValue> paramValues = generateTestParamValues();

        CategoryParam enumParam = new Parameter();
        enumParam.setId(130L);
        enumParam.setXslName("type");
        enumParam.addName(WordUtil.defaultWord("Type"));
        enumParam.setType(Param.Type.ENUM);

        Option option1 = new OptionImpl();
        option1.setId(14L);
        option1.setNames(WordUtil.defaultWords("First"));

        Option option2 = new OptionImpl();
        option2.setId(15L);
        option2.setNames(WordUtil.defaultWords("Second"));

        enumParam.addOption(option1);
        enumParam.addOption(option2);

        params.add(ForTitleParameter.fromCategoryParam(enumParam));

        ParameterValue enumValue = new ParameterValue();
        enumValue.setParamId(130);
        enumValue.setXslName("type");
        enumValue.setType(Param.Type.ENUM);
        enumValue.setOptionId(15L);

        paramValues.add(enumValue);

        CommonModel model = generateTestModel();
        model.addParameterValues(paramValues);

        ModelTitleGenerator titleGenerator = new ModelTitleGenerator(template, params, Collections.emptyList());
        String title = titleGenerator.createTitle(model).getTitle();

        Assert.assertEquals("Second AAA Model name", title);
    }

    @Test
    public void createSimpleTitleWithBooleanParam() {
        String template = "{\"delimiter\":\" \",\"values\":[[(1 ),(v4 )],[(1 ),(v3 )],[(t0 ),(t0 )]]}";

        List<ParameterValue> paramValues = generateTestParamValues();

        CommonModel model = generateTestModel();
        model.addParameterValues(paramValues);

        ModelTitleGenerator titleGenerator = new ModelTitleGenerator(template, params, Collections.emptyList());
        String title = titleGenerator.createTitle(model).getTitle();

        Assert.assertEquals("True AAA Model name", title);
    }

    @Test
    public void createTitleWithoutManadatoryParam() {
        String template = "{\"delimiter\":\" \",\"values\":[[(v1 ),(v1 ),null,(true)]," +
            "[(v28 ),(v28 ),null,(true)],[(t0 ),(t0 )]]}";

        List<ParameterValue> paramValues = generateTestParamValues();

        CategoryParam additionalParam = new Parameter();
        additionalParam.setId(28L);
        additionalParam.setXslName("type");
        additionalParam.addName(WordUtil.defaultWord("Type"));
        additionalParam.setType(Param.Type.STRING);

        params.add(ForTitleParameter.fromCategoryParam(additionalParam));

        CommonModel model = generateTestModel();
        model.addParameterValues(paramValues);

        ModelTitleGenerator titleGenerator = new ModelTitleGenerator(template, params, Collections.emptyList());
        ModelTitle title = titleGenerator.createTitle(model);

        Assert.assertNull(title.getTitle());
        Assert.assertTrue(title.hasError());
    }

    @Test
    public void createTitleWithoutNotManadatoryParamWithDefaultValue() {
        String template = "{\"delimiter\":\" \",\"values\":[[(v1 ),(v1 ),null,(true)]," +
            "[(v28 ),(v28 ),(\"[No value]\"),(false)],[(t0 ),(t0 )]]}";

        List<ParameterValue> paramValues = generateTestParamValues();

        CategoryParam additionalParam = new Parameter();
        additionalParam.setId(28L);
        additionalParam.setXslName("type");
        additionalParam.addName(WordUtil.defaultWord("Type"));
        additionalParam.setType(Param.Type.STRING);

        params.add(ForTitleParameter.fromCategoryParam(additionalParam));

        CommonModel model = generateTestModel();
        model.addParameterValues(paramValues);

        ModelTitleGenerator titleGenerator = new ModelTitleGenerator(template, params, Collections.emptyList());
        String title = titleGenerator.createTitle(model).getTitle();

        Assert.assertEquals("10 [No value] Model name", title);
    }

    @Test
    public void createTitleWithoutNotManadatoryParamWithoutDefaultValue() {
        String template = "{\"delimiter\":\" \",\"values\":[[(v1 ),(v1 ),null,(true)],[(1 ),(v28 )],[(t0 ),(t0 )]]}";

        List<ParameterValue> paramValues = generateTestParamValues();

        CategoryParam additionalParam = new Parameter();
        additionalParam.setId(28L);
        additionalParam.setXslName("type");
        additionalParam.addName(WordUtil.defaultWord("Type"));
        additionalParam.setType(Param.Type.STRING);

        params.add(ForTitleParameter.fromCategoryParam(additionalParam));

        CommonModel model = generateTestModel();
        model.addParameterValues(paramValues);

        ModelTitleGenerator titleGenerator = new ModelTitleGenerator(template, params, Collections.emptyList());
        String title = titleGenerator.createTitle(model).getTitle();

        Assert.assertEquals("10 Model name", title);
    }

    @Test
    public void createTitleForModification() {
        String template = "{\"delimiter\":\" \",\"values\":[[(1 ),(v1 )],[(1 ),(v6 )],[(t0 ),(t0 )]]}";

        List<ParameterValue> paramValues = generateTestParamValues();

        CategoryParam additionalParam = new Parameter();
        additionalParam.setId(6L);
        additionalParam.setXslName("extra_param");
        additionalParam.addName(WordUtil.defaultWord("Extra param"));
        additionalParam.setType(Param.Type.STRING);
        params.add(ForTitleParameter.fromCategoryParam(additionalParam));

        ParameterValue parentParameterValue = new ParameterValue();
        parentParameterValue.setParamId(6);
        parentParameterValue.setXslName("extra_param");
        parentParameterValue.setType(Param.Type.STRING);
        parentParameterValue.setStringValue(WordUtil.defaultWords("EXTRA"));

        CommonModel parentModel = generateTestModel();
        parentModel.setId(2);
        parentModel.addParameterValues(Arrays.asList(parentParameterValue));

        CommonModel model = generateTestModel();
        model.addParameterValues(paramValues);
        model.setParentModel(parentModel);

        ModelTitleGenerator titleGenerator = new ModelTitleGenerator(template, params, Collections.emptyList());
        String title = titleGenerator.createTitle(model).getTitle();

        Assert.assertEquals("10 EXTRA Model name", title);
    }

    @Test
    public void createTitleForModificationOverrideModelValue() {
        String template = "{\"delimiter\":\" \",\"values\":[[(1 ),(v1 )],[(1 ),(v6 )],[(t0 ),(t0 )]]}";

        List<ParameterValue> paramValues = generateTestParamValues();

        CategoryParam additionalParam = new Parameter();
        additionalParam.setId(6L);
        additionalParam.setXslName("extra_param");
        additionalParam.addName(WordUtil.defaultWord("Extra param"));
        additionalParam.setType(Param.Type.STRING);
        params.add(ForTitleParameter.fromCategoryParam(additionalParam));

        ParameterValue parentParameterValue = new ParameterValue();
        parentParameterValue.setParamId(6);
        parentParameterValue.setXslName("extra_param");
        parentParameterValue.setType(Param.Type.STRING);
        parentParameterValue.setStringValue(WordUtil.defaultWords("EXTRA"));

        ParameterValue overridenParameterValue = new ParameterValue();
        overridenParameterValue.setParamId(6);
        overridenParameterValue.setXslName("extra_param");
        overridenParameterValue.setType(Param.Type.STRING);
        overridenParameterValue.setStringValue(WordUtil.defaultWords("SUPER EXTRA"));

        CommonModel parentModel = generateTestModel();
        parentModel.setId(2);
        parentModel.addParameterValues(Arrays.asList(parentParameterValue));

        CommonModel model = generateTestModel();
        model.addParameterValues(paramValues);
        model.addParameterValues(Arrays.asList(overridenParameterValue));
        model.setParentModel(parentModel);

        ModelTitleGenerator titleGenerator = new ModelTitleGenerator(template, params, Collections.emptyList());
        String title = titleGenerator.createTitle(model).getTitle();

        Assert.assertEquals("10 SUPER EXTRA Model name", title);
    }

    @Test
    public void createSkuDefaultTitle() {
        String guruTemplate = "{\"delimiter\":\" \",\"values\":[[(1 ),(v3 ),null,(true)],[(1 ),(t0 ),null,(true)]]}";

        List<ParameterValue> paramValues = generateTestParamValues();

        CategoryParam enumParam = new Parameter();
        enumParam.setId(130L);
        enumParam.setXslName("type");
        enumParam.addName(WordUtil.defaultWord("Type"));
        enumParam.setType(Param.Type.ENUM);
        enumParam.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);

        Option option1 = new OptionImpl();
        option1.setId(14L);
        option1.setNames(WordUtil.defaultWords("First"));

        Option option2 = new OptionImpl();
        option2.setId(15L);
        option2.setNames(WordUtil.defaultWords("Second"));

        enumParam.addOption(option1);
        enumParam.addOption(option2);

        params.add(ForTitleParameter.fromCategoryParam(enumParam));

        ParameterValue enumValue = new ParameterValue();
        enumValue.setParamId(130);
        enumValue.setXslName("type");
        enumValue.setType(Param.Type.ENUM);
        enumValue.setOptionId(15L);

        paramValues.add(enumValue);

        CategoryParam anotherBooleanParam = new Parameter();
        anotherBooleanParam.setId(330L);
        anotherBooleanParam.setXslName("anotherBooleanParam");
        anotherBooleanParam.addName(WordUtil.defaultWord("BooleanParam"));
        anotherBooleanParam.setType(Param.Type.BOOLEAN);
        anotherBooleanParam.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);
        params.add(ForTitleParameter.fromCategoryParam(anotherBooleanParam));

        ParameterValue booleanParamValue = new ParameterValue();
        booleanParamValue.setParamId(330L);
        booleanParamValue.setXslName("someBooleanParam");
        booleanParamValue.setType(Param.Type.BOOLEAN);
        booleanParamValue.setBooleanValue(true);
        paramValues.add(booleanParamValue);

        CategoryParam oneMoreBooleanParam = new Parameter();
        oneMoreBooleanParam.setId(370L);
        oneMoreBooleanParam.setXslName("oneMoreBooleanParam");
        oneMoreBooleanParam.addName(WordUtil.defaultWord("OneMoreBooleanParam"));
        oneMoreBooleanParam.setType(Param.Type.BOOLEAN);
        oneMoreBooleanParam.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);
        params.add(ForTitleParameter.fromCategoryParam(oneMoreBooleanParam));

        ParameterValue oneMorebooleanParamValue = new ParameterValue();
        oneMorebooleanParamValue.setParamId(370L);
        oneMorebooleanParamValue.setXslName("oneMoreBooleanParam");
        oneMorebooleanParamValue.setType(Param.Type.BOOLEAN);
        oneMorebooleanParamValue.setBooleanValue(false);
        paramValues.add(oneMorebooleanParamValue);

        CommonModel model = generateTestModel();
        model.setCurrentType(CommonModel.Source.SKU);
        model.addParameterValues(paramValues);

        ModelTitleGenerator titleGenerator =
            new SkuTitleGenerator(null, guruTemplate, params, Collections.emptyList());
        String title = titleGenerator.createTitle(model).getTitle();

        Assert.assertEquals("AAA Model name 10 лет booleanparam universal Second 380 В", title);
    }

    @Test
    public void createSkuDefaultTitleWithSize() {
        String guruTemplate = "{\"delimiter\":\" \",\"values\":[[(1 ),(v3 ),null,(true)],[(1 ),(t0 ),null,(true)]]}";

        List<ParameterValue> paramValues = generateTestParamValues();

        // set size
        params.add(generateTestSizeParam());

        ParameterValue sizeValue = new ParameterValue();
        sizeValue.setParamId(240);
        sizeValue.setXslName("type");
        sizeValue.setType(Param.Type.ENUM);
        sizeValue.setOptionId(243L);

        paramValues.add(sizeValue);

        // set scale
        params.add(generateTestScaleParam());

        ParameterValue scaleValue = new ParameterValue();
        scaleValue.setParamId(250);
        scaleValue.setXslName("size_UNITS");
        scaleValue.setType(Param.Type.ENUM);
        scaleValue.setOptionId(251L);

        paramValues.add(scaleValue);

        CommonModel model = generateTestModel();
        model.setCurrentType(CommonModel.Source.SKU);
        model.addParameterValues(paramValues);

        // generate size measure list to indicate that there are size parameters
        List<ForTitleSizeMeasure> sizeMeasures = generateTestSizeMeasures();

        ModelTitleGenerator titleGenerator =
            new SkuTitleGenerator(null, guruTemplate, params, sizeMeasures);
        String title = titleGenerator.createTitle(model).getTitle();

        Assert.assertEquals("AAA Model name 10 лет universal XXL (INT) 380 В", title);
    }

    @Test
    public void createSkuDefaultTitleWithMissingSkuParamValues() {
        String guruTemplate = "{\"delimiter\":\" \",\"values\":[[(1 ),(v3 ),null,(true)],[(1 ),(t0 ),null,(true)]]}";

        List<ParameterValue> paramValues = generateTestParamValues();
        // remove some of param values
        paramValues.removeIf(pv -> pv.getXslName().equals("voltage") || pv.getXslName().equals("age"));

        // add size param - but not value
        params.add(generateTestSizeParam());

        // add scale param - but not value
        params.add(generateTestScaleParam());

        CommonModel model = generateTestModel();
        model.setCurrentType(CommonModel.Source.SKU);
        model.addParameterValues(paramValues);

        // generate size measure list to indicate that there are size parameters
        List<ForTitleSizeMeasure> sizeMeasures = generateTestSizeMeasures();

        ModelTitleGenerator titleGenerator =
            new SkuTitleGenerator(null, guruTemplate, params, sizeMeasures);
        String title = titleGenerator.createTitle(model).getTitle();

        Assert.assertEquals("AAA Model name universal", title);
    }

    /**
     * Проверяем, правильно ли обрамляется кавычками параметр со специальным флагом. Больше вариантов обкавычевования
     * можно найти в {@link ru.yandex.market.mbo.core.title.QuotesTest}
     */
    @Test
    public void createSkuTitleWithQuotedVendor() {
        String guruTemplate = "{\"delimiter\":\" \",\"values\":[[(1 ),(v3 ),null,(true)],[(1 ),(t0 ),null,(true)]]}";

        List<ParameterValue> paramValues = generateTestParamValues();

        CategoryParam vendorParam = new Parameter();
        vendorParam.setId(130L);
        vendorParam.setXslName(XslNames.VENDOR);
        vendorParam.addName(WordUtil.defaultWord("Производитель"));
        vendorParam.setType(Param.Type.STRING);
        vendorParam.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);
        vendorParam.setQuotedInTitle(true);

        params.add(ForTitleParameter.fromCategoryParam(vendorParam));

        ParameterValue vendorValue = new ParameterValue();
        vendorValue.setParamId(130);
        vendorValue.setXslName(XslNames.VENDOR);
        vendorValue.setType(Param.Type.STRING);

        paramValues.add(vendorValue);
        CommonModel model;
        String title;

        //Английский вендор
        model = getModelWithVendor(paramValues, vendorValue, "Samsung");
        title = generateTitle(guruTemplate, params, model);
        Assert.assertEquals("AAA Model name 10 лет universal Samsung 380 В", title);

        //Русский вендор
        model = getModelWithVendor(paramValues, vendorValue, "Самсунгъ");
        title = generateTitle(guruTemplate, params, model);
        Assert.assertEquals("AAA Model name 10 лет universal «Самсунгъ» 380 В", title);

        //Русский уже с кавычками
        model = getModelWithVendor(paramValues, vendorValue, "\"Самсунгъ\"");
        title = generateTitle(guruTemplate, params, model);
        Assert.assertEquals("AAA Model name 10 лет universal «Самсунгъ» 380 В", title);

        //Русский с английским вперемешку
        model = getModelWithVendor(paramValues, vendorValue, "Корпорация Microsoft");
        title = generateTitle(guruTemplate, params, model);
        Assert.assertEquals("AAA Model name 10 лет universal «Корпорация Microsoft» 380 В", title);

        //Кавычки в начале слова
        model = getModelWithVendor(paramValues, vendorValue, "\"Атлант\" - студия");
        title = generateTitle(guruTemplate, params, model);
        Assert.assertEquals("AAA Model name 10 лет universal «Атлант» - студия 380 В", title);

        //Кавычки в конце слова
        model = getModelWithVendor(paramValues, vendorValue, "Корпорация \"Microsoft\"");
        title = generateTitle(guruTemplate, params, model);
        Assert.assertEquals("AAA Model name 10 лет universal Корпорация «Microsoft» 380 В", title);

        //Кавычки посередине
        model = getModelWithVendor(paramValues, vendorValue, "Игры \"АРТ\" настольные");
        title = generateTitle(guruTemplate, params, model);
        Assert.assertEquals("AAA Model name 10 лет universal Игры «АРТ» настольные 380 В", title);
    }

    @Test
    public void emptyMandatory() {
        String template = "{\"delimiter\":\" \",\"values\":[[(1 ),(v3 ), \"false-result\", true],[(t0 ),(t0 )]]}";

        ModelTitleGenerator titleGenerator = new ModelTitleGenerator(template, params, Collections.emptyList());
        ModelTitle title = titleGenerator.createTitle(generateTestModel());

        Assert.assertEquals("Error: Mandatory field <Vendor> is undefined", title.getError());
    }

    @Test
    public void createTitleWithoutIgnoredEnumParam() {
        String template = "{\"delimiter\":\" \",\"values\":[[(v1 ),(v1 ),null,(true)]," +
            "[(v214 ),(v214 ),null,(true)],[(t0 ),(t0 )]]}";

        List<ParameterValue> paramValues = generateTestParamValues();

        CategoryParam enumParam = new Parameter();
        enumParam.setId(213L);
        enumParam.setXslName("type");
        enumParam.addName(WordUtil.defaultWord("Type"));
        enumParam.setType(Param.Type.ENUM);

        Option option = new OptionImpl();
        option.setId(214L);
        option.setNames(WordUtil.defaultWords("This value should be ignored"));
        option.setIgnoredInTitle(true);
        enumParam.addOption(option);

        params.add(ForTitleParameter.fromCategoryParam(enumParam));

        ParameterValue enumValue = new ParameterValue();
        enumValue.setParamId(213L);
        enumValue.setXslName("type");
        enumValue.setType(Param.Type.ENUM);
        enumValue.setOptionId(214L);
        paramValues.add(enumValue);

        CommonModel model = generateTestModel();
        model.addParameterValues(paramValues);

        ModelTitleGenerator titleGenerator = new ModelTitleGenerator(template, params, Collections.emptyList());
        String title = titleGenerator.createTitle(model).getTitle();

        Assert.assertEquals("10 Model name", title);
    }

    private CommonModel getModelWithVendor(List<ParameterValue> paramValues, ParameterValue vendorValue, String text) {
        vendorValue.setStringValue(WordUtil.defaultWords(text));
        CommonModel model = generateTestModel();
        model.setCurrentType(CommonModel.Source.SKU);
        model.addParameterValues(paramValues);
        return model;
    }

    private String generateTitle(String guruTemplate, List<ForTitleParameter> params, CommonModel model) {
        SkuTitleGenerator titleGenerator = new SkuTitleGenerator(null, guruTemplate, params, Collections.emptyList());
        return titleGenerator.createTitle(model).getTitle();
    }

    private List<ForTitleParameter> generateTestParams() {
        CategoryParam param1 = new Parameter();
        param1.setId(1);
        param1.setXslName("age");
        param1.addName(WordUtil.defaultWord("Age"));
        param1.setType(Param.Type.NUMERIC);
        Unit ageUnit = new Unit("возраст", "лет", BigDecimal.ZERO, 0L, 0L);
        param1.setUnit(ageUnit);
        param1.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);

        CategoryParam param2 = new Parameter();
        param2.setId(2);
        param2.setXslName("name");
        param2.addName(WordUtil.defaultWord("Name"));
        param2.setType(Param.Type.STRING);

        CategoryParam param3 = new Parameter();
        param3.setId(3);
        param3.setXslName("vendor");
        param3.addName(WordUtil.defaultWord("Vendor"));
        param3.setType(Param.Type.ENUM);

        Option vendorOption1 = new OptionImpl();
        vendorOption1.setId(11L);
        vendorOption1.setNames(WordUtil.defaultWords("AAA"));

        Option vendorOption2 = new OptionImpl();
        vendorOption2.setId(12L);
        vendorOption2.setNames(WordUtil.defaultWords("BBB"));

        param3.addOption(vendorOption1);
        param3.addOption(vendorOption2);

        CategoryParam param4 = new Parameter();
        param4.setId(4);
        param4.setXslName("is_universal");
        param4.addName(WordUtil.defaultWord("Universal"));
        param4.setType(Param.Type.BOOLEAN);
        param4.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);

        CategoryParam param5 = new Parameter();
        param5.setId(5);
        param5.setXslName("voltage");
        param5.addName(WordUtil.defaultWord("Voltage"));
        param5.setType(Param.Type.NUMERIC_ENUM);
        Unit numericEnumUnit = new Unit("напряжение", "В", BigDecimal.ONE, 1L, 2L);
        param5.setUnit(numericEnumUnit);
        param5.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);

        Option numericOption1 = new OptionImpl();
        numericOption1.setId(13L);
        numericOption1.setNumericValue(new BigDecimal(220));

        Option numericOption2 = new OptionImpl();
        numericOption2.setId(14L);
        numericOption2.setNumericValue(new BigDecimal(380));

        param5.addOption(numericOption1);
        param5.addOption(numericOption2);

        List<CategoryParam> result = new ArrayList<>();
        result.add(param1);
        result.add(param2);
        result.add(param3);
        result.add(param4);
        result.add(param5);

        return result.stream().map(ForTitleParameter::fromCategoryParam).collect(Collectors.toList());
    }

    private List<ParameterValue> generateTestParamValues() {
        ParameterValue parameterValue1 = new ParameterValue();
        parameterValue1.setParamId(1);
        parameterValue1.setXslName("age");
        parameterValue1.setType(Param.Type.NUMERIC);
        parameterValue1.setNumericValue(BigDecimal.TEN);

        ParameterValue parameterValue2 = new ParameterValue();
        parameterValue2.setParamId(2);
        parameterValue2.setXslName("name");
        parameterValue2.setType(Param.Type.STRING);
        parameterValue2.setStringValue(WordUtil.defaultWords("Model name"));

        ParameterValue parameterValue3 = new ParameterValue();
        parameterValue3.setParamId(3);
        parameterValue3.setXslName("vendor");
        parameterValue3.setType(Param.Type.ENUM);
        parameterValue3.setOptionId(11L);

        ParameterValue parameterValue4 = new ParameterValue();
        parameterValue4.setParamId(4);
        parameterValue4.setXslName("is_universal");
        parameterValue4.setType(Param.Type.BOOLEAN);
        parameterValue4.setBooleanValue(true);

        ParameterValue parameterValue5 = new ParameterValue();
        parameterValue5.setParamId(5);
        parameterValue5.setXslName("voltage");
        parameterValue5.setType(Param.Type.NUMERIC_ENUM);
        parameterValue5.setOptionId(14L);


        List<ParameterValue> result = new ArrayList<>();
        result.add(parameterValue1);
        result.add(parameterValue2);
        result.add(parameterValue3);
        result.add(parameterValue4);
        result.add(parameterValue5);

        return result;
    }

    private ForTitleParameter generateTestSizeParam() {
        CategoryParam sizeParam = ParameterBuilder.builder()
            .id(240)
            .xsl("size")
            .name("Size")
            .type(Param.Type.ENUM)
            .skuParameterMode(SkuParameterMode.SKU_DEFINING)
            .option(241, "M")
            .option(242, "L")
            .option(243, "XXL")
            .option(244, "46")
            .option(245, "48")
            .endParameter();

        return ForTitleParameter.fromCategoryParam(sizeParam);
    }

    private ForTitleParameter generateTestScaleParam() {
        CategoryParam scaleParam = ParameterBuilder.builder()
            .id(250)
            .xsl("size_UNITS")
            .name("Size (размерная сетка)")
            .type(Param.Type.ENUM)
            .option(251, "INT")
            .option(252, "RU")
            .endParameter();

        return ForTitleParameter.fromCategoryParam(scaleParam);
    }

    private List<ForTitleSizeMeasure> generateTestSizeMeasures() {
        GLMeasure sizeMeasure = new GLMeasure();
        sizeMeasure.setId(650L);
        sizeMeasure.setName("Размер одежды");
        sizeMeasure.setValueParamId(240);
        sizeMeasure.setUnitParamId(250);

        SizeMeasureDto measureDto = new SizeMeasureDto(sizeMeasure, new Unit(), "", "");
        return Collections.singletonList(SizeMeasureDto.convertForTitleGenerator(measureDto));
    }

    private CommonModel generateTestModel() {
        CommonModel model = new CommonModel();
        model.setId(1L);
        model.setSource(CommonModel.Source.GURU);
        model.setCurrentType(CommonModel.Source.GURU);

        return model;
    }
}
