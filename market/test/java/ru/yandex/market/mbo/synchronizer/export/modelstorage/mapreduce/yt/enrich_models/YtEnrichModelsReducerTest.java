package ru.yandex.market.mbo.synchronizer.export.modelstorage.mapreduce.yt.enrich_models;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.inside.yt.kosher.operations.Statistics;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.core.export.yt.EnrichModelsContextYtExportUtils;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.export.modelstorage.pipe.CategoryInfo;
import ru.yandex.market.mbo.gwt.models.ModelStopWordsModel;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLink;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.constants.Categories;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.constants.Models;
import ru.yandex.market.mbo.yt.TestYtWrapper;
import ru.yandex.market.mbo.yt.operations.TestYtReduceUtils;
import ru.yandex.market.mbo.yt.operations.TestYtSortUtils;
import ru.yandex.market.mbo.yt.utils.StatisticsStub;
import ru.yandex.market.mbo.yt.utils.YieldStub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static ru.yandex.market.mbo.common.model.KnownIds.VENDOR_PARAM_ID;
import static ru.yandex.market.mbo.synchronizer.export.modelstorage.BaseCategoryModelsExtractorTestClass.cleanExportTsFlag;

/**
 * Tests of {@link YtEnrichModelsReducer}.
 *
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class YtEnrichModelsReducerTest {
    private static final Comparator<YTreeMapNode> MODEL_ID_COMPARATOR =
        Comparator.comparingLong(ytNode -> YtEnrichModelsOutput.model(ytNode).getId());

    private static final long EXPORT_TIME = System.currentTimeMillis();

    private static final ModelStorage.Model M1 = ModelStorage.Model.newBuilder()
        .setId(1)
        .setCategoryId(1)
        .setCurrentType(CommonModel.Source.GURU.name())
        .build();

    private static final ModelStorage.Model SKU1_1 = ModelStorage.Model.newBuilder()
        .setId(2)
        .setCategoryId(M1.getCategoryId())
        .setCurrentType(CommonModel.Source.SKU.name())
        .addRelations(ModelStorage.Relation.newBuilder()
            .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
            .setCategoryId(M1.getCategoryId())
            .setId(M1.getId())
            .build())
        .build();

    private static final ModelStorage.Model SKU1_2 = ModelStorage.Model.newBuilder(SKU1_1)
        .setId(3)
        .setCategoryId(M1.getCategoryId())
        .setCurrentType(CommonModel.Source.SKU.name())
        .addRelations(ModelStorage.Relation.newBuilder()
            .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
            .setCategoryId(M1.getCategoryId())
            .setId(M1.getId())
            .build())
        .build();

    private static final ModelStorage.Model MODIF1 = ModelStorage.Model.newBuilder()
        .setId(5)
        .setCategoryId(M1.getCategoryId())
        .setCurrentType(CommonModel.Source.GURU.name())
        .setParentId(M1.getId())
        .build();

    private static final ModelStorage.Model SKU_MODIF_11 = ModelStorage.Model.newBuilder()
        .setId(6)
        .setCategoryId(MODIF1.getCategoryId())
        .setCurrentType(CommonModel.Source.SKU.name())
        .addRelations(ModelStorage.Relation.newBuilder()
            .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
            .setCategoryId(MODIF1.getCategoryId())
            .setId(MODIF1.getId())
            .build())
        .build();

    private static final ModelStorage.Model SKU_MODIF_12 = ModelStorage.Model.newBuilder()
        .setId(7)
        .setCategoryId(MODIF1.getCategoryId())
        .setCurrentType(CommonModel.Source.SKU.name())
        .addRelations(ModelStorage.Relation.newBuilder()
            .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
            .setCategoryId(MODIF1.getCategoryId())
            .setId(MODIF1.getId())
            .build())
        .build();

    private static final ModelStorage.Model MODIF2 = ModelStorage.Model.newBuilder()
        .setId(8)
        .setCategoryId(M1.getCategoryId())
        .setCurrentType(CommonModel.Source.GURU.name())
        .setParentId(M1.getId())
        .build();

    private static final ModelStorage.Model SKU_MODIF_2 = ModelStorage.Model.newBuilder()
        .setId(9)
        .setCategoryId(MODIF2.getCategoryId())
        .setCurrentType(CommonModel.Source.SKU.name())
        .addRelations(ModelStorage.Relation.newBuilder()
            .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
            .setCategoryId(MODIF2.getCategoryId())
            .setId(MODIF2.getId())
            .build())
        .build();

    private static final ModelStorage.Model M2 = ModelStorage.Model.newBuilder()
        .setId(10)
        .setCategoryId(2)
        .setCurrentType(CommonModel.Source.GURU.name())
        .build();

    private static final ModelStorage.Model M3 = ModelStorage.Model.newBuilder()
        .setId(30)
        .setCategoryId(3)
        .setVendorId(3L)
        .setCurrentType(CommonModel.Source.GURU.name())
        .build();

    private static final ModelStorage.Model M_AS_MODEL = ModelStorage.Model.newBuilder()
        .setId(11)
        .setCategoryId(1)
        .setPublished(true)
        .setCurrentType(CommonModel.Source.GURU.name())
        .build();

    private static final ModelStorage.Model M_AS_SKU = ModelStorage.Model.newBuilder()
        .setId(M_AS_MODEL.getId())
        .setCategoryId(1)
        .setPublished(true)
        .setCurrentType(CommonModel.Source.SKU.name())
        .addRelations(ModelStorage.Relation.newBuilder()
            .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
            .setCategoryId(M_AS_MODEL.getCategoryId())
            .setId(M_AS_MODEL.getId())
            .build())
        .build();

    private static final List<String> REDUCE_BY = Arrays.asList(
        YtEnrichModelsInput.CATEGORY_ID.getField(),
        YtEnrichModelsInput.GROUP_MODEL_ID.getField()
    );

    private static final List<String> JOIN_BY = Arrays.asList(
        YtEnrichModelsInput.CATEGORY_ID.getField()
    );

    private static final List<CategoryInfo> CATEGORY_INFOS = Arrays.asList(
        Categories.CATEGORY_INFO_1, Categories.CATEGORY_INFO_2, Categories.CATEGORY_INFO_3
    );

    private YtEnrichModelsReducer reducer;
    private static final List<ModelStopWordsModel> MODEL_STOP_WORDS_MODELS = new ArrayList<>();


    @Before
    public void init() {
        Set<Long> categoryIds = CATEGORY_INFOS.stream().map(CategoryInfo::getCategoryId).collect(Collectors.toSet());
        List<ValueLink> manufacturerValueLinks = new ArrayList<>();
        EnrichReducerContext context =
            new EnrichReducerContext(categoryIds, manufacturerValueLinks, MODEL_STOP_WORDS_MODELS, true);
        reducer = new YtEnrichModelsReducer(Models.UID, EXPORT_TIME, context, false, false);
    }

    @Test
    public void testIsSkuRenderingFlag() {
        YieldStub<YTreeMapNode> yield = run(M_AS_MODEL, M_AS_SKU);

        Assertions.assertThat(yield.getOut())
            .extracting(YtEnrichModelsOutput::isModelForRender)
            .containsExactly(true, false);
    }

    @Test
    public void testSimple() {
        YieldStub<YTreeMapNode> yield = run(
            M1, SKU1_1, SKU1_2,
            MODIF1, SKU_MODIF_11, SKU_MODIF_12,
            MODIF2, SKU_MODIF_2,
            M2
        );

        // Check correct output
        Assertions.assertThat(yield.getOut())
            .extracting(YtEnrichModelsOutput::model)
            .usingElementComparatorOnFields("id")
            .containsExactlyInAnyOrder(M1, SKU1_1, SKU1_2, MODIF1, SKU_MODIF_11, SKU_MODIF_12, MODIF2, SKU_MODIF_2, M2);
        Assertions.assertThat(yield.getOut(0, MODEL_ID_COMPARATOR))
            .extracting(YtEnrichModelsOutput::groupModelId)
            .containsExactly(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 10L);
    }

    @Test
    public void testSetExportTs() {
        YieldStub<YTreeMapNode> yield = run(
            M1, SKU1_1, SKU1_2
        );

        // Check correct output
        Assertions.assertThat(yield.getOut())
            .extracting(it -> YtEnrichModelsOutput.model(it).getExportTs())
            .containsExactlyInAnyOrder(EXPORT_TIME, EXPORT_TIME, EXPORT_TIME);
    }

    @Test
    public void testPartnerModelWithSkuEnrichment() {
        YieldStub<YTreeMapNode> yield = run(
            Models.PARTNER1, Models.PARTNER_SKU1_1, Models.PARTNER_SKU1_2,
            Models.PARTNER2,
            Models.PARTNER3, Models.PARTNER_SKU3_1
        );

        // Check correct output
        Assertions.assertThat(yield.getOut())
            .extracting(model -> cleanExportTsFlag(YtEnrichModelsOutput.model(model)))
            .extracting(Models::removeIsPartnerModificationDate)
            .containsExactlyInAnyOrder(
                Models.PARTNER1_ENRICHED,
                Models.PARTNER_SKU1_1_ENRICHED,
                Models.PARTNER_SKU1_2_ENRICHED,
                Models.PARTNER3_ENRICHED,
                Models.PARTNER_SKU3_1_ENRICHED);

        Assertions.assertThat(yield.getOut())
            .extracting(YtEnrichModelsOutput::isModelForRender)
            .containsExactly(false, true, false, false, true);

        Assertions.assertThat(yield.getOut(0, MODEL_ID_COMPARATOR))
            .extracting(YtEnrichModelsOutput::groupModelId)
            .containsExactly(13L, 13L, 13L, 17L, 17L);
    }

    @Test
    public void testSkipNoExtractedCategories() {
        List<ValueLink> manufacturerValueLinks = new ArrayList<>();
        EnrichReducerContext context = new EnrichReducerContext(
            Collections.singletonList(Categories.CATEGORY_INFO_2.getCategoryId()),
            manufacturerValueLinks, MODEL_STOP_WORDS_MODELS, true
        );

        reducer = new YtEnrichModelsReducer(Models.UID, EXPORT_TIME, context, false, false);

        YieldStub<YTreeMapNode> yield = run(
            M1, SKU1_1, SKU1_2,
            MODIF1, SKU_MODIF_11, SKU_MODIF_12,
            MODIF2, SKU_MODIF_2,
            M2
        );

        // Check correct output
        Assertions.assertThat(yield.getOut())
            .extracting(YtEnrichModelsOutput::model)
            .usingElementComparatorOnFields("id")
            .containsExactlyInAnyOrder(M2);
    }

    @Test
    public void testSettingManufacturerParameter() {
        final long manufacturerId = 99L;

        List<ValueLink> manufacturerValueLinks = new ArrayList<>();
        ValueLink valueLink = new ValueLink();
        valueLink.setSourceParamId(VENDOR_PARAM_ID);
        valueLink.setSourceOptionId(M3.getVendorId());
        valueLink.setTargetOptionId(manufacturerId);
        manufacturerValueLinks.add(valueLink);

        EnrichReducerContext context = new EnrichReducerContext(
            Collections.singletonList(M3.getCategoryId()), manufacturerValueLinks,
            MODEL_STOP_WORDS_MODELS, true);

        reducer = new YtEnrichModelsReducer(Models.UID, EXPORT_TIME, context, false, false);

        YieldStub<YTreeMapNode> yield = run(M3);

        Optional<ModelStorage.ParameterValue> parameter = YtEnrichModelsOutput.model(yield.getOut().get(0))
            .getParameterValuesList()
            .stream()
            .filter(e -> e.getXslName().equals(XslNames.MANUFACTURER))
            .findAny();

        Assertions.assertThat(parameter).isNotEmpty();
        Assertions.assertThat(parameter.get())
            .extracting(ModelStorage.ParameterValue::getOptionId)
            .isEqualTo((int) manufacturerId);
    }

    @Test
    public void testUnpublishedModelIfHasStopWord() {
        final String stopWord = "test";

        List<ValueLink> manufacturerValueLinks = new ArrayList<>();
        List<ModelStopWordsModel> models = Collections.singletonList(
            new ModelStopWordsModel(stopWord, Collections.emptySet(), Collections.emptySet())
        );

        ModelStorage.Model model = M3.toBuilder()
            .setPublished(true)
            .setPublishedOnMarket(true)
            .setPublishedOnMarket(true)
            .addParameterValueHypothesis(ModelStorage.ParameterValueHypothesis.newBuilder()
                .addStrValue(MboParameters.Word.newBuilder().setName(stopWord).build()).build())
            .build();

        EnrichReducerContext context = new EnrichReducerContext(
            Collections.singletonList(model.getCategoryId()), manufacturerValueLinks,
            models, true);


        reducer = new YtEnrichModelsReducer(Models.UID, EXPORT_TIME, context, false, false);

        YieldStub<YTreeMapNode> yield = run(model);

        ModelStorage.Model result = YtEnrichModelsOutput.model(yield.getOut().get(0));

        assertFalse(result.getPublished());
        assertFalse(result.getPublishedOnBlueMarket());
        assertFalse(result.getPublishedOnMarket());
    }

    @Test
    public void testStatisticsAreWritten() {
        StatisticsStub statisticsStub = new StatisticsStub();

        run(statisticsStub,
            M1, SKU1_1, SKU1_2,
            MODIF1, SKU_MODIF_11, SKU_MODIF_12,
            MODIF2, SKU_MODIF_2,
            M2
        );

        Assertions.assertThat(statisticsStub.containsByKeyPrefix("cache_statistics")).isTrue();
        Assertions.assertThat(statisticsStub.containsByKeyPrefix("pipe_statistics")).isTrue();
        Assertions.assertThat(statisticsStub.get("reducer/total_time")).isPositive();

        // check statistics doesn't contain lambda names (MBO-23266)
        Assertions.assertThat(statisticsStub.getByKeyPrefix("").keySet())
            .allMatch(key -> !key.contains("$$Lambda$"));
    }

    private YieldStub<YTreeMapNode> run(ModelStorage.Model... models) {
        return run(new StatisticsStub(), models);
    }

    private YieldStub<YTreeMapNode> run(Statistics statistics, ModelStorage.Model... models) {
        YieldStub<YTreeMapNode> sortYield = TestYtSortUtils.run(
            YtEnrichModelsInput.sortFields(),
            TestYtWrapper.wrapToEntries(Arrays.asList(models))
        );

        Map<Integer, Iterator<YTreeMapNode>> categoryInfos = ImmutableMap.of(
            0,
            CATEGORY_INFOS.stream()
                .map(EnrichModelsContextYtExportUtils::mapCategoryInfo)
                .collect(Collectors.toList()).iterator()
        );

        Map<Integer, Iterator<YTreeMapNode>> entries = ImmutableMap.of(
            1, sortYield.getOut().iterator()
        );

        YieldStub<YTreeMapNode> yield = new YieldStub<>();
        TestYtReduceUtils.run(reducer, REDUCE_BY, JOIN_BY, true, categoryInfos, entries, yield, statistics);
        return yield;
    }
}
