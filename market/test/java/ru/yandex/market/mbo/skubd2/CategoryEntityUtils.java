package ru.yandex.market.mbo.skubd2;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.googlecode.protobuf.format.JsonFormat;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import ru.yandex.ir.parser.formalizer.XmlEntity;
import ru.yandex.market.ir.FormalizerContractor;
import ru.yandex.market.ir.FormalizerKnowledge;
import ru.yandex.market.ir.dao.CategoryCreator;
import ru.yandex.market.ir.decision.DependencyRuleFactory;
import ru.yandex.market.ir.decision.RuleFactory;
import ru.yandex.market.ir.parser.TypeFactory;
import ru.yandex.market.ir.parser.view.automat.StateFactory;
import ru.yandex.market.ir.processor.FormalizationStringIndexer;
import ru.yandex.market.ir.processor.PatternEntriesExtractor;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.skubd2.formalize.EmbeddedFormalizer;
import ru.yandex.market.mbo.skubd2.formalize.Formalizer;
import ru.yandex.market.mbo.skubd2.knowledge.SkuRuntimeKnowledge;
import ru.yandex.market.mbo.skubd2.load.FormalizerProtoUtils;
import ru.yandex.market.mbo.skubd2.load.dao.CategoryEntity;
import ru.yandex.market.mbo.skubd2.load.dao.ParameterValue;
import ru.yandex.market.mbo.skubd2.load.dao.SkuEntity;
import ru.yandex.market.mbo.skubd2.load.dao.ValueAliasEntity;
import ru.yandex.market.mbo.skubd2.load.dao.ValueType;
import ru.yandex.market.mbo.skubd2.service.CategorySkutcher;
import ru.yandex.utils.string.DeNullingProcessor;
import ru.yandex.utils.string.indexed.IndexedStringFactory;

/**
 * @author Shamil Ablyazov, <a href="mailto:a-shar@yandex-team.ru"/>.
 */
public class CategoryEntityUtils {
    private CategoryEntityUtils() {
    }

    public static CategorySkutcher buildCategorySkutcher(
        String categoryFileName, String modelFileName, Consumer<MboParameters.Category.Builder> catBuilderConsumer
    ) throws IOException {
        categoryFileName = new File(categoryFileName).getAbsolutePath();
        modelFileName = new File(modelFileName).getAbsolutePath();

        MboParameters.Category categoryEntity = buildExportCategory(categoryFileName, catBuilderConsumer);
        long hid = categoryEntity.getHid();
        Stream<ModelStorage.Model> modelStream = buildModelStream(modelFileName);

        IndexedStringFactory indexedStringFactory = new IndexedStringFactory();
        indexedStringFactory.setStringProcessor(new DeNullingProcessor());
        indexedStringFactory.setSteam(true);
        FormalizationStringIndexer stringIndexer = new FormalizationStringIndexer();
        stringIndexer.setIndexedStringFactory(indexedStringFactory);
        CategoryCreator categoryCreator = new CategoryCreator();
        categoryCreator.setIndexedStringFactory(indexedStringFactory);
        categoryCreator.setStateFactory(new StateFactory());
        categoryCreator.setRuleFactory(new RuleFactory());
        TypeFactory typeFactory = new TypeFactory();
        typeFactory.setIndexedStringFactory(indexedStringFactory);
        categoryCreator.setTypeFactory(typeFactory);
        categoryCreator.setDependencyRuleFactory(new DependencyRuleFactory());

        PatternEntriesExtractor patternEntriesExtractor = new PatternEntriesExtractor();
        patternEntriesExtractor.setIndexedStringFactory(indexedStringFactory);

        FormalizerContractor formalizerContractor = new FormalizerContractor();
        formalizerContractor.setPatternEntriesExtractor(patternEntriesExtractor);

        SkuRuntimeKnowledge runtimeKnowledge = new SkuRuntimeKnowledge(formalizerContractor,
            indexedStringFactory,
            stringIndexer, categoryCreator
        );
        runtimeKnowledge.fullyReloadCategory(hid, categoryEntity, modelStream);
        return runtimeKnowledge.getSkutcherByHid(hid);
    }

    public static Formalizer buildEmbeddedFormalizer(String categoryFileName, String modelFileName) throws IOException {
        IndexedStringFactory indexedStringFactory = new IndexedStringFactory();
        indexedStringFactory.setStringProcessor(new DeNullingProcessor());
        indexedStringFactory.setSteam(true);

        PatternEntriesExtractor patternEntriesExtractor = new PatternEntriesExtractor();
        patternEntriesExtractor.setIndexedStringFactory(indexedStringFactory);

        FormalizerContractor formalizerContractor = new FormalizerContractor();
        formalizerContractor.setPatternEntriesExtractor(patternEntriesExtractor);
        MboParameters.Category exportCategory = buildExportCategory(categoryFileName, null);
        Stream<ModelStorage.Model> modelStream = buildModelStream(modelFileName);

        CategoryEntity categoryEntity = CategoryEntity.fromProto(exportCategory, modelStream);
        Long2ObjectMap<LongSet> paramsFilter = new Long2ObjectOpenHashMap<>();

        for (SkuEntity skuEntity : categoryEntity.getSkuEntities()) {
            for (ParameterValue parameterValue : skuEntity.getParameterValues()) {
                if (parameterValue.getType() == ValueType.ENUM
                    || parameterValue.getType() == ValueType.NUMERIC_ENUM
                    || parameterValue.getType() == ValueType.BOOLEAN
                    ) {
                    LongSet optionIds = paramsFilter.computeIfAbsent(
                        parameterValue.getParameterId(), k -> new LongOpenHashSet()
                    );
                    optionIds.add(parameterValue.getOptionId());
                }
            }
            for (ValueAliasEntity valueAliasEntity : skuEntity.getValueAliasEntities()) {
                LongSet optionIds = paramsFilter.computeIfAbsent(
                    valueAliasEntity.getParamId(), k -> new LongOpenHashSet()
                );
                optionIds.add(valueAliasEntity.getAliasId());
            }
        }

        XmlEntity.Formalizer formalizerEntity = FormalizerProtoUtils.createFormalizerEntityForSkutcher(
            exportCategory, paramsFilter);
        CategoryCreator categoryCreator = new CategoryCreator();
        categoryCreator.setIndexedStringFactory(indexedStringFactory);
        categoryCreator.setStateFactory(new StateFactory());
        categoryCreator.setRuleFactory(new RuleFactory());
        TypeFactory typeFactory = new TypeFactory();
        typeFactory.setIndexedStringFactory(indexedStringFactory);
        categoryCreator.setTypeFactory(typeFactory);
        categoryCreator.setDependencyRuleFactory(new DependencyRuleFactory());

        FormalizerKnowledge formalizerKnowledge = categoryCreator
                .readCategoriesFromEntity(0L, formalizerEntity)
                .getDefaultKnowledgeView();
        FormalizationStringIndexer stringIndexer = new FormalizationStringIndexer();
        stringIndexer.setIndexedStringFactory(indexedStringFactory);

        return new EmbeddedFormalizer(formalizerKnowledge, stringIndexer, formalizerContractor);

    }

    public static MboParameters.Category buildExportCategory(
        String categoryFileName,
        Consumer<MboParameters.Category.Builder> catBuilderConsumer
    ) throws IOException {
        categoryFileName = new File(categoryFileName).getAbsolutePath();
        MboParameters.Category.Builder catBuilder = MboParameters.Category.newBuilder();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(categoryFileName))) {
            JsonFormat.merge(reader, catBuilder);
        }
        if (catBuilderConsumer != null) {
            catBuilderConsumer.accept(catBuilder);
        }
        return catBuilder.build();
    }

    public static Stream<ModelStorage.Model> buildModelStream(
        String modelFileName
    ) throws IOException {
        List<ModelStorage.Model> modelList = new ArrayList<>();

        if (modelFileName != null) {
            modelFileName = new File(modelFileName).getAbsolutePath();
            try (BufferedReader reader = Files.newBufferedReader(Paths.get(modelFileName))) {
                JsonParser jsonParser = new JsonParser();
                JsonElement element = jsonParser.parse(reader);
                ModelStorage.Model.Builder builder = ModelStorage.Model.newBuilder();
                for (JsonElement jsonElement : element.getAsJsonArray()) {
                    String str = jsonElement.toString();
                    builder.clear();
                    JsonFormat.merge(str, builder);
                    ModelStorage.Model model = builder.build();
                    modelList.add(model);
                }
            }
        }
        return modelList.stream();
    }
}
