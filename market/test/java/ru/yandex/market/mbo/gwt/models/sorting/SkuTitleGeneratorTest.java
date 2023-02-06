package ru.yandex.market.mbo.gwt.models.sorting;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValueHypothesis;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.OptionBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Unit;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.WordUtil;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * @author danfertev
 * @since 09.04.2018
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class SkuTitleGeneratorTest {
    private CategoryParam param1;
    private CategoryParam param2;
    private CategoryParam param3;
    private CategoryParam param4;
    private CategoryParam param5;

    @Before
    public void setUp() throws Exception {
        param1 = CategoryParamBuilder.newBuilder(1L, "param1", Param.Type.NUMERIC)
            .setName("param1")
            .setSkuParameterMode(SkuParameterMode.SKU_NONE)
            .build();

        param2 = CategoryParamBuilder.newBuilder(2L, "param2", Param.Type.STRING)
            .setName("param2")
            .setSkuParameterMode(SkuParameterMode.SKU_NONE)
            .build();

        param3 = CategoryParamBuilder.newBuilder(3L, "param3", Param.Type.ENUM)
            .setName("param3")
            .setSkuParameterMode(SkuParameterMode.SKU_NONE)
            .addOption(OptionBuilder.newBuilder(31L).addName("option31"))
            .addOption(OptionBuilder.newBuilder(32L).addName("option32"))
            .build();

        param4 = CategoryParamBuilder.newBuilder(4L, "param4", Param.Type.BOOLEAN)
            .setName("param4")
            .setSkuParameterMode(SkuParameterMode.SKU_NONE)
            .addOption(OptionBuilder.newBuilder(40L).addName("false"))
            .addOption(OptionBuilder.newBuilder(41L).addName("true"))
            .build();

        param5 = CategoryParamBuilder.newBuilder(5L, "param5", Param.Type.NUMERIC_ENUM)
            .setName("param5")
            .setSkuParameterMode(SkuParameterMode.SKU_NONE)
            .addOption(OptionBuilder.newBuilder(51L).addName("51"))
            .addOption(OptionBuilder.newBuilder(52L).addName("пятьдесят два"))
            .build();
    }

    @Test
    public void noDefiningParams() {
        CommonModel sku = SkuBuilderHelper.getNoParamSkuBuilder()
            .title("title")
            .param(param1).setNumeric(10)
            .param(param2).setString("text")
            .param(param3).setOption(31L)
            .param(param4).setBoolean(true)
            .param(param5).setOption(51L)
            .endModel();

        param1.setSkuParameterMode(SkuParameterMode.SKU_INFORMATIONAL);

        String title = SkuTitleGenerator.createSkuTitle(sku, Arrays.asList(param1, param2, param3, param4, param5));

        Assertions.assertThat(title).isEqualTo(sku.getTitle());
    }

    @Test
    public void singleNumericDefiningParam() {
        CommonModel sku = SkuBuilderHelper.getNoParamSkuBuilder()
            .title("title")
            .param(param1).setNumeric(10)
            .param(param2).setString("text")
            .param(param3).setOption(31L)
            .param(param4).setBoolean(true)
            .param(param5).setOption(51L)
            .endModel();

        param1.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);

        String title = SkuTitleGenerator.createSkuTitle(sku, Arrays.asList(param1, param2, param3, param4, param5));

        Assertions.assertThat(title).isEqualTo(sku.getTitle() + ", 10");
    }

    @Test
    public void singleNumericDefiningParamWithUnit() {
        CommonModel sku = SkuBuilderHelper.getNoParamSkuBuilder()
            .title("title")
            .param(param1).setNumeric(10)
            .param(param2).setString("text")
            .param(param3).setOption(31L)
            .param(param4).setBoolean(true)
            .param(param5).setOption(51L)
            .endModel();

        param1.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);
        param1.setUnit(new Unit("Meter", "m", new BigDecimal(0), 0L, 0L));

        String title = SkuTitleGenerator.createSkuTitle(sku, Arrays.asList(param1, param2, param3, param4, param5));

        Assertions.assertThat(title).isEqualTo(sku.getTitle() + ", 10 m");
    }

    @Test
    public void multipleNumericDefiningParamWithUnit() {
        CommonModel sku = SkuBuilderHelper.getNoParamSkuBuilder()
            .title("title")
            .startParameterValue()
                .paramId(param1.getId()).num(10L)
            .endParameterValue()
            .startParameterValue()
                .paramId(param1.getId()).num(11L)
            .endParameterValue()
            .param(param2).setString("text")
            .param(param3).setOption(31L)
            .param(param4).setBoolean(true)
            .param(param5).setOption(51L)
            .endModel();

        param1.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);
        param1.setUnit(new Unit("Meter", "m", new BigDecimal(0), 0L, 0L));

        String title = SkuTitleGenerator.createSkuTitle(sku, Arrays.asList(param1, param2, param3, param4, param5));

        Assertions.assertThat(title).isEqualTo(sku.getTitle() + ", 10 m, 11 m");
    }

    @Test
    public void singleStringDefiningParam() {
        CommonModel sku = SkuBuilderHelper.getNoParamSkuBuilder()
            .title("title")
            .param(param1).setNumeric(10)
            .param(param2).setString("text1")
            .param(param3).setOption(31L)
            .param(param4).setBoolean(true)
            .param(param5).setOption(51L)
            .endModel();

        param2.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);

        String title = SkuTitleGenerator.createSkuTitle(sku, Arrays.asList(param1, param2, param3, param4, param5));

        Assertions.assertThat(title).isEqualTo(sku.getTitle() + ", ru: text1");
    }

    @Test
    public void multipleStringDefiningParamSameLangId() {
        CommonModel sku = SkuBuilderHelper.getNoParamSkuBuilder()
            .title("title")
            .param(param1).setNumeric(10)
            .param(param2).setString("text1", "text2")
            .param(param3).setOption(31L)
            .param(param4).setBoolean(true)
            .param(param5).setOption(51L)
            .endModel();

        param2.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);

        String title = SkuTitleGenerator.createSkuTitle(sku, Arrays.asList(param1, param2, param3, param4, param5));

        Assertions.assertThat(title).isEqualTo(sku.getTitle() + ", ru: text1, ru: text2");
    }

    @Test
    public void multipleStringDefiningParamDifferentLangId() {
        CommonModel sku = SkuBuilderHelper.getNoParamSkuBuilder()
            .title("title")
            .param(param1).setNumeric(10)
            .param(param2).setWords(new Word(Language.RUSSIAN.getId(), "text1"),
                new Word(Language.ENGLISH.getId(), "text2"))
            .param(param3).setOption(31L)
            .param(param4).setBoolean(true)
            .param(param5).setOption(51L)
            .endModel();

        param2.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);

        String title = SkuTitleGenerator.createSkuTitle(sku, Arrays.asList(param1, param2, param3, param4, param5));

        Assertions.assertThat(title).isEqualTo(sku.getTitle() + ", ru: text1, en: text2");
    }

    @Test
    public void singleEnumDefiningParam() {
        CommonModel sku = SkuBuilderHelper.getNoParamSkuBuilder()
            .title("title")
            .param(param1).setNumeric(10)
            .param(param2).setString("text1")
            .param(param3).setOption(31L)
            .param(param4).setBoolean(true)
            .param(param5).setOption(51L)
            .endModel();

        param3.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);

        String title = SkuTitleGenerator.createSkuTitle(sku, Arrays.asList(param1, param2, param3, param4, param5));

        Assertions.assertThat(title).isEqualTo(sku.getTitle() + ", option31");
    }

    @Test
    public void multipleEnumDefiningParam() {
        CommonModel sku = SkuBuilderHelper.getNoParamSkuBuilder()
            .title("title")
            .param(param1).setNumeric(10)
            .param(param2).setString("text")
            .startParameterValue()
                .paramId(3L).optionId(31L)
            .endParameterValue()
            .startParameterValue()
                .paramId(3L).optionId(32L)
            .endParameterValue()
            .param(param4).setBoolean(true)
            .param(param5).setOption(51L)
            .endModel();

        param3.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);

        String title = SkuTitleGenerator.createSkuTitle(sku, Arrays.asList(param1, param2, param3, param4, param5));

        Assertions.assertThat(title).isEqualTo(sku.getTitle() + ", option31, option32");
    }

    @Test
    public void singleEnumDefiningParamUnknownOption() {
        CommonModel sku = SkuBuilderHelper.getNoParamSkuBuilder()
            .title("title")
            .param(param1).setNumeric(10)
            .param(param2).setString("text1")
            .param(param3).setOption(30L)
            .param(param4).setBoolean(true)
            .param(param5).setOption(51L)
            .endModel();

        param3.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);

        String title = SkuTitleGenerator.createSkuTitle(sku, Arrays.asList(param1, param2, param3, param4, param5));

        Assertions.assertThat(title).isEqualTo(sku.getTitle() + ", неизвестная опция: 30");
    }

    @Test
    public void singleBooleanDefiningParam() {
        CommonModel sku = SkuBuilderHelper.getNoParamSkuBuilder()
            .title("title")
            .param(param1).setNumeric(10)
            .param(param2).setString("text1")
            .param(param3).setOption(30L)
            .param(param4).setBoolean(true)
            .param(param5).setOption(51L)
            .endModel();

        param4.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);

        String title = SkuTitleGenerator.createSkuTitle(sku, Arrays.asList(param1, param2, param3, param4, param5));

        Assertions.assertThat(title).isEqualTo(sku.getTitle() + ", param4: да");
    }

    @Test
    public void multipleDefiningParam() {
        CommonModel sku = SkuBuilderHelper.getNoParamSkuBuilder()
            .title("title")
            .param(param1).setNumeric(10)
            .param(param2).setString("text1")
            .param(param3).setOption(30L)
            .param(param4).setBoolean(true)
            .param(param5).setOption(51L)
            .endModel();

        param1.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);
        param4.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);

        String title = SkuTitleGenerator.createSkuTitle(sku, Arrays.asList(param1, param2, param3, param4, param5));

        Assertions.assertThat(title).isEqualTo(sku.getTitle() + ", 10, param4: да");
    }

    @Test
    public void generatedSkuTitleWithoutHypothesis() {
        CommonModel sku = SkuBuilderHelper.getNoParamSkuBuilder()
            .currentType(CommonModel.Source.GENERATED_SKU)
            .title("title")
            .param(param1).setNumeric(10)
            .param(param2).setString("text1")
            .param(param3).setOption(30L)
            .param(param4).setBoolean(true)
            .param(param5).setOption(51L)
            .endModel();

        param1.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);

        String title = SkuTitleGenerator.createSkuTitle(sku, Arrays.asList(param1, param2, param3, param4, param5));

        Assertions.assertThat(title).isEqualTo(sku.getTitle());
    }

    @Test
    public void generatedSkuTitleWithSingleHypothesis() {
        CommonModel sku = SkuBuilderHelper.getNoParamSkuBuilder()
            .currentType(CommonModel.Source.GENERATED_SKU)
            .title("title")
            .param(param1).setNumeric(10)
            .param(param2).setString("text1")
            .param(param3).setOption(30L)
            .param(param4).setBoolean(true)
            .param(param5).setOption(51L)
            .endModel();

        param1.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);
        param3.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);
        ParameterValueHypothesis hypothesis1 = new ParameterValueHypothesis();
        hypothesis1.setParamId(param1.getId());
        hypothesis1.setXslName(param1.getXslName());
        hypothesis1.setType(param1.getType());
        hypothesis1.addStringValue(WordUtil.defaultWord("h1"));

        ParameterValueHypothesis hypothesis2 = new ParameterValueHypothesis();
        hypothesis2.setParamId(param2.getId());
        hypothesis2.setXslName(param2.getXslName());
        hypothesis2.setType(param2.getType());
        hypothesis2.addStringValue(WordUtil.defaultWord("h2"));
        sku.setParameterValueHypotheses(Arrays.asList(hypothesis1, hypothesis2));

        String title = SkuTitleGenerator.createSkuTitle(sku, Arrays.asList(param1, param2, param3, param4, param5));

        Assertions.assertThat(title).isEqualTo(sku.getTitle() + ", [h1]");
    }

    @Test
    public void generatedSkuTitleWithMultipleHypothesis() {
        CommonModel sku = SkuBuilderHelper.getNoParamSkuBuilder()
            .currentType(CommonModel.Source.GENERATED_SKU)
            .title("title")
            .param(param1).setNumeric(10)
            .param(param2).setString("text1")
            .param(param3).setOption(30L)
            .param(param4).setBoolean(true)
            .param(param5).setOption(51L)
            .endModel();

        param1.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);
        param2.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);
        ParameterValueHypothesis hypothesis1 = new ParameterValueHypothesis();
        hypothesis1.setParamId(param1.getId());
        hypothesis1.setXslName(param1.getXslName());
        hypothesis1.setType(param1.getType());
        hypothesis1.addStringValue(WordUtil.defaultWord("h1"));

        ParameterValueHypothesis hypothesis2 = new ParameterValueHypothesis();
        hypothesis2.setParamId(param2.getId());
        hypothesis2.setXslName(param2.getXslName());
        hypothesis2.setType(param2.getType());
        hypothesis2.addStringValue(WordUtil.defaultWord("h2"));
        sku.setParameterValueHypotheses(Arrays.asList(hypothesis1, hypothesis2));

        String title = SkuTitleGenerator.createSkuTitle(sku, Arrays.asList(param1, param2, param3, param4, param5));

        Assertions.assertThat(title).isEqualTo(sku.getTitle() + ", [h1], [h2]");
    }

    @Test
    public void singleNumericEnumDefiningParam() {
        CommonModel sku = SkuBuilderHelper.getNoParamSkuBuilder()
            .title("title")
            .param(param1).setNumeric(10)
            .param(param2).setString("text")
            .param(param3).setOption(31L)
            .param(param4).setBoolean(true)
            .param(param5).setOption(51L)
            .endModel();

        param5.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);

        String title = SkuTitleGenerator.createSkuTitle(sku, Arrays.asList(param1, param2, param3, param4, param5));

        Assertions.assertThat(title).isEqualTo(sku.getTitle() + ", 51");
    }

    @Test
    public void singleNumericEnumDefiningParamWithUnit() {
        CommonModel sku = SkuBuilderHelper.getNoParamSkuBuilder()
            .title("title")
            .param(param1).setNumeric(10)
            .param(param2).setString("text")
            .param(param3).setOption(31L)
            .param(param4).setBoolean(true)
            .param(param5).setOption(51L)
            .endModel();

        param5.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);
        param5.setUnit(new Unit("Meter", "m", new BigDecimal(0), 0L, 0L));

        String title = SkuTitleGenerator.createSkuTitle(sku, Arrays.asList(param1, param2, param3, param4, param5));

        Assertions.assertThat(title).isEqualTo(sku.getTitle() + ", 51 m");
    }

    @Test
    public void multipleNumericEnumDefiningParamWithUnit() {
        CommonModel sku = SkuBuilderHelper.getNoParamSkuBuilder()
            .title("title")
            .param(param1).setNumeric(10)
            .param(param2).setString("text")
            .param(param3).setOption(31L)
            .param(param4).setBoolean(true)
            .startParameterValue()
                .paramId(param5.getId()).numericOptionId(51L)
            .endParameterValue()
            .startParameterValue()
                .paramId(param5.getId()).numericOptionId(52L)
            .endParameterValue()
            .endModel();

        param5.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);
        param5.setUnit(new Unit("Meter", "m", new BigDecimal(0), 0L, 0L));

        String title = SkuTitleGenerator.createSkuTitle(sku, Arrays.asList(param1, param2, param3, param4, param5));

        Assertions.assertThat(title).isEqualTo(sku.getTitle() + ", 51 m, пятьдесят два m");
    }
}
