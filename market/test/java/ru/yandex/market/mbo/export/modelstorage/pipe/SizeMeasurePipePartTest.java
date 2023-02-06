package ru.yandex.market.mbo.export.modelstorage.pipe;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.Range;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.common.util.collections.MultiMap;
import ru.yandex.market.mbo.core.modelstorage.util.ModelProtoConverter;
import ru.yandex.market.mbo.export.helper.SizeMeasureExportHelper;
import ru.yandex.market.mbo.export.modelstorage.pipe.sizemeasure.SizeMeasureConversionPipePart;
import ru.yandex.market.mbo.export.modelstorage.pipe.sizemeasure.SizeMeasureScalePipePart;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.gurulight.GLMeasure;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRule;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRuleImpl;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRulePredicate;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRuleType;
import ru.yandex.market.mbo.gwt.models.gurulight.SizeMeasureDto;
import ru.yandex.market.mbo.gwt.models.gurulight.SizeMeasureValueOption;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getDefaultSkuBuilder;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getGuruBuilder;
import static ru.yandex.market.mbo.utils.MboAssertions.assertThat;

/**
 * @author danfertev
 * @since 31.03.2018
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class SizeMeasurePipePartTest {
    private static final AtomicLong IDS = new AtomicLong(0);
    private static final long USER_ID = 100500L;
    private CategoryParam heightScale = new Parameter();
    private CategoryParam heightSize = new Parameter();
    private CategoryParam heightNumeric = new Parameter();
    private CategoryParam heightNumericMin = new Parameter();
    private CategoryParam heightNumericMax = new Parameter();

    List<CategoryParam> params = Arrays.asList(
        heightScale, heightSize, heightNumeric, heightNumericMin, heightNumericMax);

    private Option heightScaleRu = new OptionImpl();
    private Option heightScaleEu = new OptionImpl();

    private Option heightRuSize30 = new OptionImpl();
    private Option heightRuSize34 = new OptionImpl();
    private Option heightEuSizeL = new OptionImpl();
    private Option heightEuSizeXL = new OptionImpl();

    private GLMeasure heightMeasure = new GLMeasure();
    private SizeMeasureDto height = new SizeMeasureDto(heightMeasure, null, null, null);
    List<SizeMeasureDto> measures = Collections.singletonList(height);

    @Before
    public void setUp() throws Exception {
        heightScale.setId(IDS.incrementAndGet());
        heightScale.setXslName("height_UNITS");
        heightScale.addName(WordUtil.defaultWord("Рост(размерная сетка)"));
        heightScale.setType(Param.Type.ENUM);

        heightScaleRu.setId(IDS.incrementAndGet());
        heightScaleRu.setParamId(heightScale.getId());
        heightScaleRu.setNames(WordUtil.defaultWords("RU"));

        heightScaleEu.setId(IDS.incrementAndGet());
        heightScaleEu.setParamId(heightScale.getId());
        heightScaleEu.setNames(WordUtil.defaultWords("EU"));

        heightScale.addOption(heightScaleRu);
        heightScale.addOption(heightScaleEu);

        heightSize.setId(IDS.incrementAndGet());
        heightSize.setXslName("height");
        heightSize.addName(WordUtil.defaultWord("Рост"));
        heightSize.setType(Param.Type.ENUM);

        heightRuSize30.setId(IDS.incrementAndGet());
        heightRuSize30.setParamId(heightSize.getId());
        heightRuSize30.setNames(WordUtil.defaultWords("30"));

        heightRuSize34.setId(IDS.incrementAndGet());
        heightRuSize34.setParamId(heightSize.getId());
        heightRuSize34.setNames(WordUtil.defaultWords("34"));

        heightEuSizeL.setId(IDS.incrementAndGet());
        heightEuSizeL.setParamId(heightSize.getId());
        heightEuSizeL.setNames(WordUtil.defaultWords("L"));

        heightEuSizeXL.setId(IDS.incrementAndGet());
        heightEuSizeXL.setParamId(heightSize.getId());
        heightEuSizeXL.setNames(WordUtil.defaultWords("XL"));

        heightSize.addOption(heightRuSize30);
        heightSize.addOption(heightRuSize34);
        heightSize.addOption(heightEuSizeL);
        heightSize.addOption(heightEuSizeXL);

        heightNumeric.setId(IDS.incrementAndGet());
        heightNumeric.setXslName("height_SPOILED");
        heightNumeric.addName(WordUtil.defaultWord("Рост(числовое значение)"));
        heightNumeric.setType(Param.Type.NUMERIC);

        heightNumericMin.setId(IDS.incrementAndGet());
        heightNumericMin.setXslName("height_MIN");
        heightNumericMin.addName(WordUtil.defaultWord("Рост(числовое значение) (min)"));
        heightNumericMin.setType(Param.Type.NUMERIC);

        heightNumericMax.setId(IDS.incrementAndGet());
        heightNumericMax.setXslName("height_MAX");
        heightNumericMax.addName(WordUtil.defaultWord("Рост(числовое значение) (max)"));
        heightNumericMax.setType(Param.Type.NUMERIC);

        heightMeasure.setId(IDS.incrementAndGet());
        heightMeasure.setName("Рост");
        heightMeasure.setUnitParamId(heightScale.getId());
        heightMeasure.setValueParamId(heightSize.getId());
        heightMeasure.setNumericValueParamId(heightNumeric.getId());
        heightMeasure.setMaxNumericValueParamId(heightNumericMax.getId());
        heightMeasure.setMinNumericValueParamId(heightNumericMin.getId());
    }

    @Test
    public void noSizeParamValue() throws IOException {
        CommonModel sku = getDefaultSkuBuilder()
            .id(IDS.incrementAndGet()).endModel();

        ModelPipeContext context = createContextWithSku(sku);
        createPipe().acceptModelsGroup(context);

        Assertions.assertThat(getSkus(context)).contains(sku);
    }

    @Test
    public void unableToFindScaleForSize() throws IOException {
        CommonModel sku = createSkuWithSize(new OptionImpl(333L));

        ModelPipeContext context = createContextWithSku(sku);
        createPipe().acceptModelsGroup(context);

        Assertions.assertThat(getSkus(context)).contains(sku);
    }

    @Test
    public void unableToFindSizeValueOption() throws IOException {
        CommonModel sku = createSkuWithSize(heightRuSize30);

        ModelPipeContext context = createContextWithSku(sku);
        createPipe().acceptModelsGroup(context);

        Assertions.assertThat(getSkus(context)).contains(sku);
    }

    @Test
    public void scaleAdded() throws IOException {
        CommonModel before = createSkuWithSize(heightRuSize30);

        ModelPipeContext context = createContextWithSku(before);
        createPipe(createScaleWithValues(heightScaleRu, createValueOption(heightRuSize30))).acceptModelsGroup(context);

        CommonModel after = getSkus(context).get(0);

        assertThat(after, heightScale).values(heightScaleRu);
        assertModificationSource(after, heightScale, heightScaleRu, ModificationSource.OPERATOR_FILLED);
    }

    @Test
    public void scaleAddedWithSizeModificationSource() throws IOException {
        CommonModel before = CommonModelBuilder.newBuilder()
            .id(IDS.incrementAndGet()).currentType(CommonModel.Source.SKU)
            .param(heightSize).setOption(heightRuSize30.getId()).modificationSource(ModificationSource.AUTO)
            .getModel();

        ModelPipeContext context = createContextWithSku(before);
        createPipe(createScaleWithValues(heightScaleRu, createValueOption(heightRuSize30))).acceptModelsGroup(context);

        CommonModel after = getSkus(context).get(0);

        assertThat(after, heightScale).values(heightScaleRu);
        assertModificationSource(after, heightScale, heightScaleRu, ModificationSource.AUTO);
    }

    @Test
    public void overlappedRangesButNoReverseRules() throws IOException {
        CommonModel before = createSkuWithSize(heightRuSize30);

        ModelPipeContext context = createContextWithSku(before);
        SizeMeasureValueOption ruSize30 = createValueOption(heightRuSize30, Range.between(10L, 20L), false);
        SizeMeasureValueOption euSizeL = createValueOption(heightEuSizeL, Range.between(15L, 25L), false);

        MultiMap<Long, SizeMeasureValueOption> scaleWithValues = createScaleWithValues(heightScaleRu, ruSize30);
        appendScaleWithValues(scaleWithValues, heightScaleEu, euSizeL);
        createPipe(scaleWithValues).acceptModelsGroup(context);

        CommonModel after = getSkus(context).get(0);

        assertThat(after, heightScale).values(heightScaleRu);
        assertThat(after, heightSize).values(heightRuSize30);
    }

    @Test
    public void notOverlappedRangesAndReverseRuleExists() throws IOException {
        CommonModel before = createSkuWithSize(heightRuSize30);

        ModelPipeContext context = createContextWithSku(before);
        SizeMeasureValueOption ruSize30 = createValueOption(heightRuSize30, Range.between(10L, 20L), false);
        SizeMeasureValueOption euSizeL = createValueOption(heightEuSizeL, Range.between(100L, 105L), true);

        MultiMap<Long, SizeMeasureValueOption> scaleWithValues = createScaleWithValues(heightScaleRu, ruSize30);
        appendScaleWithValues(scaleWithValues, heightScaleEu, euSizeL);
        createPipe(scaleWithValues).acceptModelsGroup(context);

        CommonModel after = getSkus(context).get(0);

        assertThat(after, heightScale).values(heightScaleRu);
        assertThat(after, heightSize).values(heightRuSize30);
    }

    @Test
    public void overlappedRangesAndReverseRuleExists() throws IOException {
        CommonModel before = createSkuWithSize(heightRuSize30);

        ModelPipeContext context = createContextWithSku(before);
        SizeMeasureValueOption ruSize30 = createValueOption(heightRuSize30, Range.between(10L, 20L), false);
        SizeMeasureValueOption euSizeL = createValueOption(heightEuSizeL, Range.between(15L, 25L), true);

        MultiMap<Long, SizeMeasureValueOption> scaleWithValues = createScaleWithValues(heightScaleRu, ruSize30);
        appendScaleWithValues(scaleWithValues, heightScaleEu, euSizeL);
        createPipe(scaleWithValues).acceptModelsGroup(context);

        CommonModel after = getSkus(context).get(0);

        assertThat(after, heightScale).values(heightScaleRu);
        assertThat(after, heightSize).valuesInAnyOrder(heightRuSize30, heightEuSizeL);
        assertModificationSource(after, heightSize, heightEuSizeL, ModificationSource.DEPENDENCY_RULE);
        assertRuleId(after, heightSize, euSizeL);

    }

    @Test
    public void multipleOverlappedRangesAndReverseRuleExists() throws IOException {
        CommonModel before = createSkuWithSize(heightRuSize30);

        ModelPipeContext context = createContextWithSku(before);
        SizeMeasureValueOption ruSize30 = createValueOption(heightRuSize30, Range.between(10L, 30L), true);
        SizeMeasureValueOption euSizeL = createValueOption(heightEuSizeL, Range.between(10L, 19L), true);
        SizeMeasureValueOption euSizeXL = createValueOption(heightEuSizeXL, Range.between(20L, 29L), true);

        MultiMap<Long, SizeMeasureValueOption> scaleWithValues = createScaleWithValues(heightScaleRu, ruSize30);
        appendScaleWithValues(scaleWithValues, heightScaleEu, euSizeL);
        appendScaleWithValues(scaleWithValues, heightScaleEu, euSizeXL);
        createPipe(scaleWithValues).acceptModelsGroup(context);

        CommonModel after = getSkus(context).get(0);

        assertThat(after, heightScale).values(heightScaleRu);
        assertThat(after, heightSize).valuesInAnyOrder(heightRuSize30, heightEuSizeL, heightEuSizeXL);
    }

    @Test
    public void overlappedRangesInTheSameScale() throws IOException {
        CommonModel before = createSkuWithSize(heightRuSize30);

        ModelPipeContext context = createContextWithSku(before);
        SizeMeasureValueOption ruSize30 = createValueOption(heightRuSize30, Range.between(10L, 20L), true);
        SizeMeasureValueOption ruSize34 = createValueOption(heightRuSize34, Range.between(15L, 25L), true);

        MultiMap<Long, SizeMeasureValueOption> scaleWithValues = createScaleWithValues(heightScaleRu, ruSize30);
        appendScaleWithValues(scaleWithValues, heightScaleRu, ruSize34);
        createPipe(scaleWithValues).acceptModelsGroup(context);

        CommonModel after = getSkus(context).get(0);

        assertThat(after, heightScale).values(heightScaleRu);
        assertThat(after, heightSize).valuesInAnyOrder(heightRuSize30, heightRuSize34);
    }

    @Test
    public void multipleSkuOverlappedRangesAndReverseRuleExists() throws IOException {
        CommonModel before1 = createSkuWithSize(heightRuSize30);
        CommonModel before2 = createSkuWithSize(heightEuSizeL);

        ModelPipeContext context = createContextWithSku(before1, before2);
        SizeMeasureValueOption ruSize30 = createValueOption(heightRuSize30, Range.between(10L, 20L), true);
        SizeMeasureValueOption euSizeL = createValueOption(heightEuSizeL, Range.between(15L, 25L), true);

        MultiMap<Long, SizeMeasureValueOption> scaleWithValues = createScaleWithValues(heightScaleRu, ruSize30);
        appendScaleWithValues(scaleWithValues, heightScaleEu, euSizeL);
        createPipe(scaleWithValues).acceptModelsGroup(context);

        List<CommonModel> skusAfter = getSkus(context);
        CommonModel after1 = skusAfter.get(0);

        assertThat(after1, heightScale).values(heightScaleRu);
        assertThat(after1, heightSize).valuesInAnyOrder(heightRuSize30, heightEuSizeL);
        assertRuleId(after1, heightSize, euSizeL);

        CommonModel after2 = skusAfter.get(1);

        assertThat(after2, heightScale).values(heightScaleEu);
        assertThat(after2, heightSize).valuesInAnyOrder(heightEuSizeL, heightRuSize30);
        assertRuleId(after2, heightSize, ruSize30);
    }

    @Test
    public void guruModelNotChanged() throws IOException {
        CommonModel guru = createGuruWithSize(heightRuSize30);

        CommonModel modification1 = createGuruWithSize(heightRuSize34);
        CommonModel modification2 = createGuruWithSize(heightEuSizeL);

        ModelPipeContext context = createContext(guru, Arrays.asList(modification1, modification2),
            Collections.emptyList());
        createPipe().acceptModelsGroup(context);

        Assertions.assertThat(getGuru(context)).isEqualTo(guru);
        Assertions.assertThat(getModifications(context)).containsExactlyInAnyOrder(modification1, modification2);
    }

    @Test
    public void guruIsSkuModelChanged() throws IOException {
        CommonModel guruBefore = createIsSkuModelWithSize(true, heightRuSize30);

        CommonModel modification1Before = createIsSkuModelWithSize(false, heightRuSize34);
        CommonModel modification2Before = createIsSkuModelWithSize(true, heightEuSizeL);
        CommonModel modification3Before = createIsSkuModel(true);

        ModelPipeContext context = createContext(
            guruBefore,
            Arrays.asList(modification1Before, modification2Before, modification3Before),
            Collections.emptyList());
        SizeMeasureValueOption ruSize30 = createValueOption(heightRuSize30, Range.between(10L, 20L), true);
        SizeMeasureValueOption euSizeL = createValueOption(heightEuSizeL, Range.between(15L, 25L), true);
        MultiMap<Long, SizeMeasureValueOption> scaleWithValues = createScaleWithValues(heightScaleRu, ruSize30);
        appendScaleWithValues(scaleWithValues, heightScaleEu, euSizeL);
        createPipe(scaleWithValues).acceptModelsGroup(context);

        CommonModel guruAfter = getGuru(context);
        assertThat(guruAfter, heightScale).values(heightScaleRu);
        assertThat(guruAfter, heightSize).valuesInAnyOrder(heightRuSize30, heightEuSizeL);
        assertRuleId(guruAfter, heightSize, euSizeL);

        List<CommonModel> modificationsAfter = getModifications(context);

        CommonModel modification1After = modificationsAfter.get(0);
        Assertions.assertThat(modification1After).isEqualTo(modification1Before);

        CommonModel modification2After = modificationsAfter.get(1);
        assertThat(modification2After, heightScale).values(heightScaleEu);
        assertThat(modification2After, heightSize).valuesInAnyOrder(heightEuSizeL, heightRuSize30);
        assertRuleId(modification2After, heightSize, ruSize30);

        CommonModel modification3After = modificationsAfter.get(2);
        Assertions.assertThat(modification3After).isEqualTo(modification3Before);
    }

    @Test
    public void whenMinMaxExistsThenDoesntAddToModel() throws IOException {
        CommonModel before1 = createSkuWithSize(heightRuSize30);
        addNumValueToModel(before1, heightNumericMax, 25L);
        addNumValueToModel(before1, heightNumericMin, 5L);

        ModelPipeContext context = createContextWithSku(before1);
        SizeMeasureValueOption ruSize30 = createValueOption(heightRuSize30, Range.between(10L, 20L), true);

        MultiMap<Long, SizeMeasureValueOption> scaleWithValues = createScaleWithValues(heightScaleRu, ruSize30);
        createPipe(scaleWithValues).acceptModelsGroup(context);

        List<CommonModel> skusAfter = getSkus(context);
        CommonModel after1 = skusAfter.get(0);

        assertThat(after1, heightNumericMax).values(BigDecimal.valueOf(25L));
        assertThat(after1, heightNumericMin).values(BigDecimal.valueOf(5L));
    }

    @Test
    public void whenOnlyMinExistsThenDoesntAddToModel() throws IOException {
        CommonModel before1 = createSkuWithSize(heightRuSize30);
        addNumValueToModel(before1, heightNumericMin, 5L);

        ModelPipeContext context = createContextWithSku(before1);
        SizeMeasureValueOption ruSize30 = createValueOption(heightRuSize30, Range.between(10L, 20L), true);

        MultiMap<Long, SizeMeasureValueOption> scaleWithValues = createScaleWithValues(heightScaleRu, ruSize30);
        createPipe(scaleWithValues).acceptModelsGroup(context);

        List<CommonModel> skusAfter = getSkus(context);
        CommonModel after1 = skusAfter.get(0);

        assertThat(after1, heightNumericMin).values(BigDecimal.valueOf(5L));
        assertThat(after1, heightNumericMax).notExists();
    }

    @Test
    public void whenSizeIsHypothesisThenDoesntAddToModel() throws IOException {
        CommonModel before1 = createModelWithSizeHypothesis(CommonModel.Source.SKU, "123");

        ModelPipeContext context = createContextWithSku(before1);
        SizeMeasureValueOption ruSize30 = createValueOption(heightRuSize30, Range.between(10L, 20L), true);

        MultiMap<Long, SizeMeasureValueOption> scaleWithValues = createScaleWithValues(heightScaleRu, ruSize30);
        createPipe(scaleWithValues).acceptModelsGroup(context);

        List<CommonModel> skusAfter = getSkus(context);
        CommonModel after1 = skusAfter.get(0);

        assertThat(after1, heightNumericMin).notExists();
        assertThat(after1, heightNumericMax).notExists();
    }

    @Test
    public void whenMinMaxNotExistsThenAddToModel() throws IOException {
        CommonModel before1 = createSkuWithSize(heightRuSize30);

        ModelPipeContext context = createContextWithSku(before1);
        SizeMeasureValueOption ruSize30 = createValueOption(heightRuSize30, Range.between(10L, 20L), true);

        MultiMap<Long, SizeMeasureValueOption> scaleWithValues = createScaleWithValues(heightScaleRu, ruSize30);
        createPipe(scaleWithValues).acceptModelsGroup(context);

        List<CommonModel> skusAfter = getSkus(context);
        CommonModel after1 = skusAfter.get(0);

        assertThat(after1, heightNumericMax).values(BigDecimal.valueOf(20L));
        assertThat(after1, heightNumericMin).values(BigDecimal.valueOf(10L));
    }

    @Test
    public void whenMinMaxNotExistsAnsSeveralRangesThenAddToModelMinMaxFromRanges() throws IOException {
        CommonModel before1 = createSkuWithSize(heightRuSize30);

        ModelPipeContext context = createContextWithSku(before1);
        SizeMeasureValueOption ruSize30 = createValueOptionSeveralRanges(
            heightRuSize30,
            ImmutableList.of(Range.between(10L, 20L), Range.between(40L, 50L), Range.between(3L, 7L)),
            true);

        MultiMap<Long, SizeMeasureValueOption> scaleWithValues = createScaleWithValues(heightScaleRu, ruSize30);
        createPipe(scaleWithValues).acceptModelsGroup(context);

        List<CommonModel> skusAfter = getSkus(context);
        CommonModel after1 = skusAfter.get(0);

        assertThat(after1, heightNumericMax).values(BigDecimal.valueOf(7L));
        assertThat(after1, heightNumericMin).values(BigDecimal.valueOf(3L));
    }

    private CommonModel createModelWithSize(CommonModel.Source type, Option size) {
        return getGuruBuilder()
            .id(IDS.incrementAndGet()).currentType(type)
            .param(heightSize).setOption(size.getId()).modificationSource(ModificationSource.OPERATOR_FILLED)
            .getModel();
    }

    private CommonModel createModelWithSizeHypothesis(CommonModel.Source type, String size) {
        return getGuruBuilder()
            .id(IDS.incrementAndGet()).currentType(type)
            .parameterValueHypothesis(heightSize.getId(), heightSize.getXslName(), heightSize.getType(), size)
            .getModel();
    }

    private CommonModel createGuruWithSize(Option size) {
        return createModelWithSize(CommonModel.Source.GURU, size);
    }

    private CommonModel createSkuWithSize(Option size) {
        return createModelWithSize(CommonModel.Source.SKU, size);
    }

    private CommonModelBuilder getIsSkuModelBuilder(boolean isSku) {
        return getGuruBuilder()
            .id(IDS.incrementAndGet())
            .param(XslNames.IS_SKU).setBoolean(isSku);
    }

    private CommonModel createIsSkuModel(boolean isSku) {
        return getIsSkuModelBuilder(isSku).getModel();
    }

    private CommonModel createIsSkuModelWithSize(boolean isSku, Option size) {
        return getIsSkuModelBuilder(isSku)
            .param(heightSize).setOption(size.getId()).modificationSource(ModificationSource.OPERATOR_FILLED)
            .getModel();
    }

    private void addNumValueToModel(CommonModel model, CategoryParam param, long value) {
        model.addParameterValue(new ParameterValue(param, BigDecimal.valueOf(value)));
    }

    private SizeMeasureValueOption createValueOption(Option option) {
        return new SizeMeasureValueOption(option);
    }

    private SizeMeasureValueOption createValueOption(Option option, Range<Long> range,
                                                     boolean createReverseRule) {
        return new SizeMeasureValueOption(
            option,
            Collections.singletonList(createSizeToNumericRule(option, range)),
            createReverseRule ? Collections.singletonList(createNumericToSizeRule(option, range))
                : Collections.emptyList()
        );
    }

    private SizeMeasureValueOption createValueOptionSeveralRanges(Option option, List<Range<Long>> ranges,
                                                     boolean createReverseRule) {
        return new SizeMeasureValueOption(
            option,
            ranges.stream().map(r -> createSizeToNumericRule(option, r)).collect(Collectors.toList()),
            createReverseRule ?
                ranges.stream().map(r -> createNumericToSizeRule(option, r)).collect(Collectors.toList()) :
                Collections.emptyList()
        );
    }

    private GLRulePredicate createSizeMatchPredicate(Option option) {
        GLRulePredicate predicate = new GLRulePredicate();
        predicate.setCondition(GLRulePredicate.ENUM_MATCHES);
        predicate.setParamId(heightSize.getId());
        predicate.setValueId(option.getId());
        return predicate;
    }

    private GLRulePredicate createNumericRangePredicate(Range<Long> range) {
        GLRulePredicate predicate = new GLRulePredicate();
        predicate.setCondition(GLRulePredicate.NUMBER_RANGE);
        predicate.setParamId(heightNumeric.getId());
        predicate.setMinValue(new BigDecimal(range.getMinimum()));
        predicate.setMaxValue(new BigDecimal(range.getMaximum()));
        return predicate;
    }

    private GLRule createSizeToNumericRule(Option option, Range<Long> range) {
        GLRule rule = new GLRuleImpl();
        rule.setType(GLRuleType.CONVERSION);
        rule.getIfs().add(createSizeMatchPredicate(option));
        rule.getThens().add(createNumericRangePredicate(range));
        return rule;
    }

    private GLRule createNumericToSizeRule(Option option, Range<Long> range) {
        GLRule rule = new GLRuleImpl();
        rule.setType(GLRuleType.CONVERSION);
        rule.getIfs().add(createNumericRangePredicate(range));
        rule.getThens().add(createSizeMatchPredicate(option));
        return rule;
    }

    private ModelPipeContext createContext(CommonModel guru, List<CommonModel> modificatons, List<CommonModel> skus) {
        return new ModelPipeContext(
            ModelProtoConverter.convert(guru),
            modificatons.stream().map(ModelProtoConverter::convert).collect(Collectors.toList()),
            skus.stream().map(ModelProtoConverter::convert).collect(Collectors.toList()));
    }

    private ModelPipeContext createContextWithSku(CommonModel... skus) {
        CommonModel guru = CommonModelBuilder.newBuilder()
            .id(IDS.incrementAndGet()).currentType(CommonModel.Source.GURU)
            .getModel();

        return new ModelPipeContext(
            ModelProtoConverter.convert(guru),
            Collections.emptyList(),
            Arrays.stream(skus).map(ModelProtoConverter::convert).collect(Collectors.toList()));
    }

    private CommonModel getGuru(ModelPipeContext context) {
        return ModelProtoConverter.convert(context.getModel().build());
    }

    private List<CommonModel> getModifications(ModelPipeContext context) {
        return ModelProtoConverter.reverseConvertAll(context.getModifications().stream()
            .map(ModelStorage.Model.Builder::build)
            .collect(Collectors.toList()));
    }

    private List<CommonModel> getSkus(ModelPipeContext context) {
        return ModelProtoConverter.reverseConvertAll(context.getSkusForOutput());
    }

    private Pipe createPipe() {
        return createPipe(new MultiMap<>());
    }

    private Pipe createPipe(MultiMap<Long, SizeMeasureValueOption> scaleWithValues) {
        return Pipe.start()
            .then(SizeMeasureScalePipePart.createForTestPurposes(
                params,
                measures,
                SizeMeasureExportHelper.create(scaleWithValues.getBackingMap(), Collections.emptyList(),
                    Collections.emptyMap()),
                USER_ID))
            .then(SizeMeasureConversionPipePart.createForTestPurposes(
                params,
                measures,
                SizeMeasureExportHelper.create(scaleWithValues.getBackingMap(), Collections.emptyList(),
                    Collections.emptyMap()),
                USER_ID))
            .build();
    }

    private MultiMap<Long, SizeMeasureValueOption> createScaleWithValues(Option scale,
                                                                         SizeMeasureValueOption... sizes) {
        MultiMap<Long, SizeMeasureValueOption> scaleWithValues = new MultiMap<>();
        scaleWithValues.put(scale.getId(), Lists.newArrayList(sizes));
        return scaleWithValues;
    }

    private void appendScaleWithValues(MultiMap<Long, SizeMeasureValueOption> scaleWithValues,
                                       Option scale, SizeMeasureValueOption... sizes) {
        for (SizeMeasureValueOption size : sizes) {
            scaleWithValues.append(scale.getId(), size);
        }
    }

    private void assertModificationSource(CommonModel after, CategoryParam param,
                                          Option option, ModificationSource source) {
        Optional<ParameterValue> value = after.getParameterValues(param.getId()).stream()
            .filter(v -> v.getOptionId() == option.getId())
            .findFirst();

        Assertions.assertThat(value).isPresent();
        Assertions.assertThat(value.get().getModificationSource()).isEqualTo(source);
    }

    private void assertRuleId(CommonModel after, CategoryParam param, SizeMeasureValueOption valueOption) {
        Optional<ParameterValue> value = after.getParameterValues(param.getId()).stream()
            .filter(v -> v.getOptionId() == valueOption.getOption().getId())
            .findFirst();

        Assertions.assertThat(value).isPresent();
        Assertions.assertThat(value.get().getRuleModificationId())
            .isEqualTo(valueOption.getConvertToSizeValueRules().get(0).getId());
    }
}
