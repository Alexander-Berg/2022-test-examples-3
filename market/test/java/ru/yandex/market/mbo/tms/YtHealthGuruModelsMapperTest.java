package ru.yandex.market.mbo.tms;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.operations.OperationContext;
import ru.yandex.inside.yt.kosher.operations.Statistics;
import ru.yandex.inside.yt.kosher.operations.Yield;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.db.modelstorage.yt.YtModelColumns;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.tms.health.published.guru.GuruCounterSupply;
import ru.yandex.market.mbo.tms.health.published.guru.YtHealthGuruOrSkuOrPartnerSkuModelsMapper;
import ru.yandex.misc.bender.Bender;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

/**
 * @author york
 * @since 17.04.2018
 */
@SuppressWarnings("checkstyle:magicnumber")
public class YtHealthGuruModelsMapperTest {
    private Long idseq;
    private Random random;

    private static final Statistics STATISTICS = new Statistics() {
        @Override
        public void write(YTreeMapNode metricsDict) { }
        @Override
        public void close() throws IOException { }
    };
    private static final OperationContext CONTEXT = new OperationContext();

    @Before
    public void init() {
        idseq = 1L;
        random = new Random(1);
    }

    @Test
    public void testSupplySerialization() {
        GuruCounterSupply supply = new GuruCounterSupply(
            new HashSet(Arrays.asList(10L, 1L)),
            new HashSet(Arrays.asList(2L)),
            randomMap(),
            randomMap(),
            randomMap()
        );
        byte[] serialized = Bender.cons(GuruCounterSupply.class).getSerializer().serializeJson(supply);
        GuruCounterSupply deserialized = Bender.cons(GuruCounterSupply.class).getParser().parseJson(serialized);
        Assert.assertEquals(supply.getBindingParams(), deserialized.getBindingParams());
        Assert.assertEquals(supply.getFakeGroupCategories(), deserialized.getFakeGroupCategories());
        Assert.assertEquals(supply.getGroupCategories(), deserialized.getGroupCategories());
        Assert.assertEquals(supply.getModifiedParams(), deserialized.getModifiedParams());
        Assert.assertEquals(supply.getParams(), deserialized.getParams());
    }

    @Test
    public void testMapperEmitModels() {
        GuruCounterSupply supply = new GuruCounterSupply(
            new HashSet(Arrays.asList(1L)),
            new HashSet(),
            new HashMap<>(),
            new HashMap<>(),
            new HashMap<>());

        ModelStorage.Model.Builder cluster = createModel(1L, CommonModel.Source.CLUSTER);

        ModelStorage.Model.Builder generated = createModel(1L, CommonModel.Source.GENERATED);

        ModelStorage.Model.Builder guruModel = createModel(1L, CommonModel.Source.GURU)
            .setGroupSize(2)
            .setPublished(true);

        ModelStorage.Model.Builder guruModification = createModel(1L, CommonModel.Source.GURU)
            .setParentId(guruModel.getId());

        ModelStorage.Model.Builder generatedGuruModification2 = createModel(1L, CommonModel.Source.GURU)
            .setParentId(guruModel.getId())
            .setPublished(true)
            .setPublishedOnBlueMarket(true);
        source(generatedGuruModification2, CommonModel.Source.GENERATED);

        ModelStorage.Model.Builder simpleGuruModelInGroup = createModel(1L, CommonModel.Source.GURU)
            .setPublishedOnMarket(true);

        ModelStorage.Model.Builder simpleBookModelInGroup = createModel(1L, CommonModel.Source.GURU)
            .setPublished(true)
            .setPublishedOnMarket(true);

        source(simpleBookModelInGroup, CommonModel.Source.BOOK);

        ModelStorage.Model.Builder guruModel2 = createModel(1L, CommonModel.Source.GURU)
            .setGroupSize(1);

        ModelStorage.Model.Builder guruModification2 = createModel(1L, CommonModel.Source.GURU)
            .setParentId(guruModel2.getId())
            .setPublished(true);

        source(guruModification2, CommonModel.Source.VENDOR);

        ModelStorage.Model.Builder guruModel3 = createModel(3L, CommonModel.Source.GURU);
        source(guruModel3, CommonModel.Source.GENERATED);

        List<ModelStorage.ModelOrBuilder> list = Arrays.asList(cluster, generated, guruModel, guruModification,
            generatedGuruModification2, simpleGuruModelInGroup, simpleBookModelInGroup,
            guruModel2, guruModification2, guruModel3);

        Map<Long, YTreeMapNode> result = mapandGetResult(supply, list);

        assertEmited(result, guruModel, guruModification, generatedGuruModification2, simpleGuruModelInGroup,
            guruModel2, guruModification2, guruModel3);

        checkPublishedBetweenEmited(result, list);

        Assert.assertEquals("model", getType(result, guruModel));
        Assert.assertEquals("model", getType(result, simpleGuruModelInGroup));
        Assert.assertEquals("model", getType(result, guruModel2));
        Assert.assertEquals("model", getType(result, guruModel3));

        Assert.assertEquals("modification", getType(result, guruModification));
        Assert.assertEquals("modification", getType(result, generatedGuruModification2));
        Assert.assertEquals("modification", getType(result, guruModification2));

        Assert.assertEquals("GURU", getSourceType(result, guruModel));
        Assert.assertEquals("GURU", getSourceType(result, guruModification));
        Assert.assertEquals("GENERATED", getSourceType(result, generatedGuruModification2));
        Assert.assertEquals("GURU", getSourceType(result, simpleGuruModelInGroup));
        Assert.assertEquals("GURU", getSourceType(result, guruModel2));
        Assert.assertEquals("VENDOR", getSourceType(result, guruModification2));
        Assert.assertEquals("GENERATED", getSourceType(result, guruModel3));

        //now check for internal grouped
        supply = new GuruCounterSupply(
            new HashSet(Arrays.asList(1L)),
            new HashSet(Arrays.asList(1L)),
            new HashMap<>(),
            new HashMap<>(),
            new HashMap<>());

        result = mapandGetResult(supply, list);

        assertEmited(result, guruModification, generatedGuruModification2, simpleGuruModelInGroup,
            guruModification2, guruModel3);

        checkPublishedBetweenEmited(result, list);

        Assert.assertEquals("model", getType(result, simpleGuruModelInGroup));
        Assert.assertEquals("model", getType(result, guruModel3));
        Assert.assertEquals("model", getType(result, guruModification));
        Assert.assertEquals("model", getType(result, generatedGuruModification2));
        Assert.assertEquals("model", getType(result, guruModification2));

        Assert.assertEquals("GURU", getSourceType(result, guruModification));
        Assert.assertEquals("GENERATED", getSourceType(result, generatedGuruModification2));
        Assert.assertEquals("GURU", getSourceType(result, simpleGuruModelInGroup));
        Assert.assertEquals("VENDOR", getSourceType(result, guruModification2));
        Assert.assertEquals("GENERATED", getSourceType(result, guruModel3));

    }

    @Test
    public void testMapperAliasesCount() {
        GuruCounterSupply supply = new GuruCounterSupply(
            new HashSet(Arrays.asList(1L)),
            new HashSet(),
            new HashMap<>(),
            new HashMap<>(),
            new HashMap<>());

        ModelStorage.ModelOrBuilder model = createModel(1L, CommonModel.Source.GURU)
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setXslName(XslNames.ALIASES)
                .addStrValue(ModelStorage.LocalizedString.getDefaultInstance())
                .addStrValue(ModelStorage.LocalizedString.getDefaultInstance()));

        ModelStorage.ModelOrBuilder modification = createModel(1L, CommonModel.Source.GURU)
            .setParentId(model.getId());

        ModelStorage.ModelOrBuilder model2 = createModel(1L, CommonModel.Source.GURU)
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setXslName(XslNames.ALIASES)
                .addStrValue(ModelStorage.LocalizedString.getDefaultInstance()));


        Map<Long, YTreeMapNode> result = mapandGetResult(supply, Arrays.asList(model, modification, model2));
        Assert.assertEquals(2, result.get(model.getId())
            .getInt(YtHealthGuruOrSkuOrPartnerSkuModelsMapper.ALIASES_COUNT));
        Assert.assertEquals(0, result.get(modification.getId())
            .getInt(YtHealthGuruOrSkuOrPartnerSkuModelsMapper.ALIASES_COUNT));
        Assert.assertEquals(1, result.get(model2.getId())
            .getInt(YtHealthGuruOrSkuOrPartnerSkuModelsMapper.ALIASES_COUNT));
    }

    @Test
    public void testMapperBindingParamsCount() {
        GuruCounterSupply supply = new GuruCounterSupply(
            new HashSet(Arrays.asList(1L)),
            new HashSet(),
            Multimaps.asMap(ImmutableSetMultimap.<Long, Long>builder()
                .putAll(1L, 100L)
                .putAll(3L, 101L, 103L)
                .build()),
            new HashMap<>(),
            new HashMap<>());

        ModelStorage.ModelOrBuilder m1 = createModel(1L, CommonModel.Source.GURU);
        ModelStorage.ModelOrBuilder m2 = createModel(1L, CommonModel.Source.GURU, 103L);
        ModelStorage.ModelOrBuilder m3 = createModel(1L, CommonModel.Source.GURU, 100L);
        ModelStorage.ModelOrBuilder m4 = createModel(2L, CommonModel.Source.GURU, 101L, 100L, 103L, 105L);
        ModelStorage.ModelOrBuilder m5 = createModel(3L, CommonModel.Source.GURU, 101L, 100L);
        ModelStorage.ModelOrBuilder m6 = createModel(3L, CommonModel.Source.GURU, 101L, 100L, 103L);

        Map<Long, YTreeMapNode> result = mapandGetResult(supply, Arrays.asList(m1, m2, m3, m4, m5, m6));

        Function<ModelStorage.ModelOrBuilder, Boolean> isNoBinding = m ->
            result.get(m.getId()).getBool(YtHealthGuruOrSkuOrPartnerSkuModelsMapper.NO_BINDING);

        Assert.assertEquals(true, isNoBinding.apply(m1));
        Assert.assertEquals(true, isNoBinding.apply(m2));
        Assert.assertEquals(false, isNoBinding.apply(m3));
        Assert.assertEquals(false, isNoBinding.apply(m4));
        Assert.assertEquals(true, isNoBinding.apply(m5));
        Assert.assertEquals(false, isNoBinding.apply(m6));
    }

    @Test
    public void testMapperParamsCount() {
        GuruCounterSupply supply = new GuruCounterSupply(
            new HashSet(Arrays.asList(1L)),
            new HashSet(),
            new HashMap<>(),
            Multimaps.asMap(ImmutableSetMultimap.<Long, Long>builder()
                .putAll(1L, 100L)
                .putAll(2L, 102L, 103L)
                .build()),
            Multimaps.asMap(ImmutableSetMultimap.<Long, Long>builder()
                .putAll(1L, 101L)
                .build()));

        ModelStorage.ModelOrBuilder model1 = createModel(1L, CommonModel.Source.GURU)
            .setGroupSize(2);
        ModelStorage.ModelOrBuilder modification1 = createModel(1L, CommonModel.Source.GURU, 100L, 101L)
            .setParentId(model1.getId());
        ModelStorage.ModelOrBuilder modification2 = createModel(1L, CommonModel.Source.GURU, 100L)
            .setParentId(model1.getId());

        ModelStorage.ModelOrBuilder model2 = createModel(1L, CommonModel.Source.GURU, 100L, 101L, 102L);

        ModelStorage.ModelOrBuilder model3 = createModel(2L, CommonModel.Source.GURU, 100L, 102L, 102L,
            102L, 103L);
        ModelStorage.ModelOrBuilder model4 = createModel(3L, CommonModel.Source.GURU, 100L, 102L, 102L,
            102L, 103L);

        Map<Long, YTreeMapNode> result = mapandGetResult(supply, Arrays.asList(model1, modification1, modification2,
            model2, model3, model4));

        Function<ModelStorage.ModelOrBuilder, Integer> getParamsCount = m ->
            result.get(m.getId()).getInt(YtHealthGuruOrSkuOrPartnerSkuModelsMapper.PARAMS_COUNT);

        Assert.assertEquals(0, (int) getParamsCount.apply(model1));
        Assert.assertEquals(1, (int) getParamsCount.apply(modification1));
        Assert.assertEquals(0, (int) getParamsCount.apply(modification2));
        Assert.assertEquals(1, (int) getParamsCount.apply(model2));
        Assert.assertEquals(2, (int) getParamsCount.apply(model3));
        Assert.assertEquals(0, (int) getParamsCount.apply(model4));
    }

    private String getType(Map<Long, YTreeMapNode> result, ModelStorage.ModelOrBuilder model) {
        return result.get(model.getId()).getString(YtHealthGuruOrSkuOrPartnerSkuModelsMapper.TYPE);
    }

    private String getSourceType(Map<Long, YTreeMapNode> result, ModelStorage.ModelOrBuilder model) {
        return result.get(model.getId()).getString(YtHealthGuruOrSkuOrPartnerSkuModelsMapper.SOURCE_TYPE);
    }

    private void checkPublishedBetweenEmited(Map<Long, YTreeMapNode> result, List<ModelStorage.ModelOrBuilder> models) {
        for (ModelStorage.ModelOrBuilder model : models) {
            if (!result.containsKey(model.getId())) {
                continue;
            }
            boolean published = result.get(model.getId()).getBool(YtHealthGuruOrSkuOrPartnerSkuModelsMapper.PUBLISHED);
            boolean publishedOnMarket = result.get(model.getId())
                .getBool(YtHealthGuruOrSkuOrPartnerSkuModelsMapper.PUBLISHED_ON_MARKET);
            boolean publishedOnBlueMarket = result.get(model.getId())
                .getBool(YtHealthGuruOrSkuOrPartnerSkuModelsMapper.PUBLISHED_ON_BLUE_MARKET);

            Assert.assertEquals(published, model.getPublished());
            Assert.assertEquals(publishedOnMarket, model.getPublishedOnMarket());
            Assert.assertEquals(publishedOnBlueMarket, model.getPublishedOnBlueMarket());
        }
    }

    private void assertEmited(Map<Long, YTreeMapNode> result, ModelStorage.ModelOrBuilder... models) {
        Set<Long> ids = new HashSet<>();
        for (ModelStorage.ModelOrBuilder model : models) {
            ids.add(model.getId());
            Assert.assertTrue(model.getId() + " is not emited", result.containsKey(model.getId()));
        }
        Set<Long> emited = new HashSet<>(result.keySet());
        emited.removeAll(ids);
        Assert.assertEquals(emited + " erroneously emited", 0, emited.size());
    }


    private Map<Long, YTreeMapNode> mapandGetResult(GuruCounterSupply supply, List<ModelStorage.ModelOrBuilder> list) {
        Map<Long, YTreeMapNode> output = new HashMap<>();
        YtHealthGuruOrSkuOrPartnerSkuModelsMapper mapper = new YtHealthGuruOrSkuOrPartnerSkuModelsMapper(supply);
        Yield<YTreeMapNode> yield = new Yield<YTreeMapNode>() {
            @Override
            public void yield(int index, YTreeMapNode value) {
                output.put(value.getLong(YtHealthGuruOrSkuOrPartnerSkuModelsMapper.MODEL_ID), value);
            }
            @Override
            public void close() throws IOException { }
        };
        consume(mapper, yield, list);
        return output;
    }

    private void consume(YtHealthGuruOrSkuOrPartnerSkuModelsMapper mapper, Yield<YTreeMapNode> yield,
                         List<ModelStorage.ModelOrBuilder> list) {
        list.stream()
            .map(this::toNode)
            .forEach(node -> mapper.map(node, yield, STATISTICS, CONTEXT));
    }

    private ModelStorage.Model.Builder createModel(Long hid, CommonModel.Source currentType, Long... paramIds) {
        ModelStorage.Model.Builder result = ModelStorage.Model.newBuilder()
            .setId(idseq++)
            .setCategoryId(hid)
            .setCurrentType(currentType.name())
            .setSourceType(currentType.name());

        Arrays.stream(paramIds).map(this::numeric).forEach(result::addParameterValues);
        return result;
    }

    private ModelStorage.Model.Builder source(ModelStorage.Model.Builder builder, CommonModel.Source type) {
        if (type == CommonModel.Source.GENERATED) {
            builder.addRelations(ModelStorage.Relation.newBuilder()
                .setId(random.nextLong())
                .setType(ModelStorage.RelationType.SYNC_SOURCE)
                .setCategoryId(builder.getCategoryId())
            );
        } else {
            builder.setSourceType(type.name());
        }
        return builder;
    }

    private ModelStorage.ParameterValue numeric(Long paramId) {
        return  ModelStorage.ParameterValue.newBuilder()
            .setParamId(paramId)
            .setNumericValue("" + random.nextLong())
            .setValueType(MboParameters.ValueType.NUMERIC)
            .setXslName("xsl-" + paramId)
            .build();
    }

    private YTreeMapNode toNode(ModelStorage.ModelOrBuilder modelOrBuilder) {
        ModelStorage.Model model = modelOrBuilder instanceof ModelStorage.Model.Builder ?
            ((ModelStorage.Model.Builder) modelOrBuilder).build() : (ModelStorage.Model) modelOrBuilder;
        return YTree.mapBuilder()
            .key(YtModelColumns.DATA).value(model.toByteArray()).buildMap();
    }

    private Map<Long, Set<Long>> randomMap() {
        SetMultimap<Long, Long> result = HashMultimap.create();
        for (int i = 0; i < random.nextInt(4); i++) {
            Long key = random.nextLong();
            for (int j = 0; j < random.nextInt(4); j++) {
                result.put(key, random.nextLong());
            }
        }
        return Multimaps.asMap(result);
    }
}
