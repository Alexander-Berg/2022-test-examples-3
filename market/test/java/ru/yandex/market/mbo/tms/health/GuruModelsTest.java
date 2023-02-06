package ru.yandex.market.mbo.tms.health;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.db.modelstorage.ModelStorageTestUtil;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.mapreduce.util.ExportMapReduceUtil;
import ru.yandex.market.mbo.tms.health.model.on.market.ModelsOnMarketCounter;
import ru.yandex.market.mbo.tms.health.published.guru.GuruCounter;
import ru.yandex.market.mbo.tms.health.published.guru.GuruModelsConsumer;
import ru.yandex.market.mbo.tms.health.published.guru.KpiStatsReport;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author york
 * @since 25.09.2017
 */
@SuppressWarnings({"checkstyle:magicnumber", "checkstyle:linelength"})

public class GuruModelsTest {
    private static final Long PUBLISHED_CATEGORY_ID = 1000L;
    private static final Long PUBLISHED_CATEGORY2_ID = 1002L;
    private static final Long UNPUBLISHED_CATEGORY_ID = 1001L;

    private static final Long PUBLISHED_VENDOR_ID = 100L;
    private static final Long PUBLISHED_VENDOR2_ID = 101L;
    private static final Long UNPUBLISHED_VENDOR_ID = 102L;

    private static final Long PUBLISHED_MODEL_ID = 10L;
    private static final Long UNPUBLISHED_MODEL_ID = 11L;

    private static final Long ASSESSOR_ID = 12345678L;

    @Test
    public void testCounter() {
        GuruCounter guruCounter = new GuruCounter();
        guruCounter.incPublishedGuruModelsCount(1);
        guruCounter.incPublishedGuruModelsCount(1);
        guruCounter.incAliasesCount(3);
        guruCounter.incAliasesCount(5);
        guruCounter.incFilledParamsCount(10);
        guruCounter.incFilledParamsCount(2);
        guruCounter.incPublishedOnMarketGuruModelsCount(1);
        guruCounter.incPublishedOnMarketGuruModelsCount(3);
        guruCounter.incPublishedGuruModificationCount(2);
        guruCounter.incPublishedGuruModificationCount(3);
        guruCounter.incPublishedOnMarketNew(16);
        guruCounter.incPublishedOnMarketNew(1);
        guruCounter.incPublishedOnMarketDel(14);
        guruCounter.incPublishedOnMarketDel(4);
        guruCounter.setPublishedNew(5);
        guruCounter.setPublishedDel(12);
        guruCounter.setPicturesCount(37);

        Assert.assertEquals(2, guruCounter.getPublishedGuruModelsCount());
        Assert.assertEquals(8, guruCounter.getAliasesCount());
        Assert.assertEquals(12, guruCounter.getFilledParamsCount());
        Assert.assertEquals(4, guruCounter.getPublishedOnMarketGuruModelsCount());
        Assert.assertEquals(5, guruCounter.getPublishedGuruModificationCount());
        Assert.assertEquals(17, guruCounter.getPublishedOnMarkeNew());
        Assert.assertEquals(18, guruCounter.getPublishedOnMarketDel());
        Assert.assertEquals(5, guruCounter.getPublishedNew());
        Assert.assertEquals(12, guruCounter.getPublishedDel());
        Assert.assertEquals(37, guruCounter.getPicturesCount());

        guruCounter = new GuruCounter(guruCounter);

        Assert.assertEquals(2, guruCounter.getPublishedGuruModelsCount());
        Assert.assertEquals(8, guruCounter.getAliasesCount());
        Assert.assertEquals(12, guruCounter.getFilledParamsCount());
        Assert.assertEquals(4, guruCounter.getPublishedOnMarketGuruModelsCount());
        Assert.assertEquals(5, guruCounter.getPublishedGuruModificationCount());
        Assert.assertEquals(17, guruCounter.getPublishedOnMarkeNew());
        Assert.assertEquals(18, guruCounter.getPublishedOnMarketDel());
        Assert.assertEquals(5, guruCounter.getPublishedNew());
        Assert.assertEquals(12, guruCounter.getPublishedDel());
        Assert.assertEquals(37, guruCounter.getPicturesCount());

        GuruCounter guruCounterCpy = new GuruCounter();
        guruCounterCpy.incPublishedOnMarketNew(6);
        guruCounterCpy.incPublishedOnMarketDel(4);
        guruCounterCpy.setPublishedNew(6);
        guruCounterCpy.setPublishedDel(8);
        guruCounterCpy.setPicturesCount(8);
        guruCounterCpy.add(guruCounter);

        Assert.assertEquals(2, guruCounterCpy.getPublishedGuruModelsCount());
        Assert.assertEquals(8, guruCounterCpy.getAliasesCount());
        Assert.assertEquals(12, guruCounterCpy.getFilledParamsCount());
        Assert.assertEquals(4, guruCounterCpy.getPublishedOnMarketGuruModelsCount());
        Assert.assertEquals(5, guruCounterCpy.getPublishedGuruModificationCount());
        Assert.assertEquals(23, guruCounterCpy.getPublishedOnMarkeNew());
        Assert.assertEquals(22, guruCounterCpy.getPublishedOnMarketDel());
        Assert.assertEquals(11, guruCounterCpy.getPublishedNew());
        Assert.assertEquals(20, guruCounterCpy.getPublishedDel());
        Assert.assertEquals(45, guruCounterCpy.getPicturesCount());
    }

    @Test
    @SuppressWarnings("checkstyle:methodLength")
    public void testConsumer() {
        ModelStorage.Model notGuruModel = ModelStorageTestUtil.generateModel().toBuilder()
            .setCategoryId(PUBLISHED_CATEGORY_ID)
            .setCurrentType(CommonModel.Source.CLUSTER.name()).build();

        //Опубликованая модель источник оператор
        ModelStorage.Model publishedGuruModel = ModelStorageTestUtil.generateModel().toBuilder()
            .setId(PUBLISHED_MODEL_ID)
            .setCurrentType(CommonModel.Source.GURU.name())
            .setSourceType(CommonModel.Source.GURU.name())
            .setCategoryId(PUBLISHED_CATEGORY_ID)
            .addTitles(ModelStorage.LocalizedString.newBuilder()
                .setIsoCode("ru")
                .setValue("SomeName")
                .build())
            .setVendorId(PUBLISHED_VENDOR_ID)
            .clearParentId()
            .setPublished(true)
            .clearRelations()
            .clearParameterValues()
            .addAllParameterValues(
                Arrays.asList(stringParam(1L), stringParam(5L),
                    stringParam(XslNames.ALIASES, 10L, "122", "33"))
            ).build();

        //Опубликованая модель источник вендор
        ModelStorage.Model publishedGuruModelByVendor = ModelStorageTestUtil.generateModel().toBuilder()
            .setCurrentType(CommonModel.Source.GURU.name())
            .setSourceType(CommonModel.Source.VENDOR.name())
            .setCategoryId(PUBLISHED_CATEGORY_ID)
            .addTitles(ModelStorage.LocalizedString.newBuilder()
                .setIsoCode("ru")
                .setValue("SomeName")
                .build())
            .setVendorId(PUBLISHED_VENDOR_ID)
            .clearParentId()
            .setPublished(true)
            .clearRelations()
            .clearParameterValues().build();

        //Опубликованая модель источник автогенерация
        ModelStorage.Model publishedGuruModelByAuto = ModelStorageTestUtil.generateModel().toBuilder()
            .setCurrentType(CommonModel.Source.GURU.name())
            .setSourceType(CommonModel.Source.GENERATED.name())
            .setCategoryId(PUBLISHED_CATEGORY_ID)
            .addTitles(ModelStorage.LocalizedString.newBuilder()
                .setIsoCode("ru")
                .setValue("SomeName")
                .build())
            .setVendorId(PUBLISHED_VENDOR_ID)
            .clearParentId()
            .clearRelations()
            .setPublished(true)
            .clearParameterValues().build();

        //Опубликованая модель источник ЯНГ
        ModelStorage.Model publishedGuruModelByYang = ModelStorageTestUtil.generateModel().toBuilder()
            .setCurrentType(CommonModel.Source.GURU.name())
            .setSourceType(CommonModel.Source.GURU.name())
            .setCategoryId(PUBLISHED_CATEGORY_ID)
            .addTitles(ModelStorage.LocalizedString.newBuilder()
                .setIsoCode("ru")
                .setValue("SomeName")
                .build())
            .setVendorId(PUBLISHED_VENDOR_ID)
            .clearParentId()
            .setPublished(true)
            .clearRelations()
            .setCreatedUserId(ASSESSOR_ID)
            .clearParameterValues().build();

        // не опубликована на маркете, так как не опубликован вендор (распубликован сегодня)
        ModelStorage.Model noVendorModel = ModelStorageTestUtil.generateModel().toBuilder()
            .setCurrentType(CommonModel.Source.GURU.name())
            .setSourceType(CommonModel.Source.GURU.name())
            .setCategoryId(PUBLISHED_CATEGORY_ID)
            .addTitles(ModelStorage.LocalizedString.newBuilder()
                .setIsoCode("ru")
                .setValue("SomeName")
                .build())
            .setVendorId(UNPUBLISHED_VENDOR_ID)
            .clearParentId()
            .setPublished(true)
            .clearRelations()
            .clearParameterValues()
            .build();

        //не опубликованная модель (распубликована сегодня)
        ModelStorage.Model upPublishedGuruModel = ModelStorageTestUtil.generateModel().toBuilder()
            .setId(UNPUBLISHED_MODEL_ID)
            .setCategoryId(PUBLISHED_CATEGORY_ID)
            .addTitles(ModelStorage.LocalizedString.newBuilder()
                .setIsoCode("ru")
                .setValue("SomeName")
                .build())
            .setVendorId(PUBLISHED_VENDOR_ID)
            .setCurrentType(CommonModel.Source.GURU.name())
            .setSourceType(CommonModel.Source.GURU.name())
            .setPublished(false)
            .clearParentId()
            .clearParameterValues()
            .clearRelations()
            .addAllParameterValues(
                Arrays.asList(stringParam(2L), stringParam(5L),
                    stringParam(XslNames.ALIASES, 10L, "33"))
            ).build();

        //Опубликованая модель
        ModelStorage.Model nonPublishedGuruModel = ModelStorageTestUtil.generateModel().toBuilder()
            .setId(PUBLISHED_MODEL_ID)
            .setCurrentType(CommonModel.Source.GURU.name())
            .setSourceType(CommonModel.Source.GURU.name())
            .setCategoryId(PUBLISHED_CATEGORY_ID)
            .addTitles(ModelStorage.LocalizedString.newBuilder()
                .setIsoCode("ru")
                .setValue("SomeName")
                .build())
            .setVendorId(PUBLISHED_VENDOR_ID)
            .clearParentId()
            .clearRelations()
            .setPublished(false)
            .clearParameterValues()
            .build();


        ModelStorage.Model publishedModification = ModelStorageTestUtil.generateModel().toBuilder()
            .setCategoryId(PUBLISHED_CATEGORY_ID)
            .setCurrentType(CommonModel.Source.GURU.name())
            .setSourceType(CommonModel.Source.GURU.name())
            .setPublished(true)
            .setParentId(publishedGuruModel.getId())
            .setVendorId(publishedGuruModel.getVendorId())
            .addTitles(ModelStorage.LocalizedString.newBuilder()
                .setIsoCode("ru")
                .setValue("SomeName")
                .build())
            .clearParameterValues()
            .clearRelations()
            .addAllParameterValues(
                Arrays.asList(stringParam(1L), stringParam(2L), stringParam(3L))
            ).build();

        ModelStorage.Model publishedBook = ModelStorageTestUtil.generateModel().toBuilder()
            .setCategoryId(PUBLISHED_CATEGORY_ID)
            .setCurrentType(CommonModel.Source.GURU.name())
            .setSourceType(CommonModel.Source.BOOK.name())
            .setPublished(true)
            .setParentId(100L)
            .clearParameterValues()
            .clearRelations()
            .addAllParameterValues(
                Arrays.asList(stringParam(1L), stringParam(2L), stringParam(3L))
            ).build();

        GuruModelsConsumer emptyConfigConsumer = new GuruModelsConsumer(new HashMap<>(), PUBLISHED_CATEGORY_ID);
        consume(emptyConfigConsumer, notGuruModel, upPublishedGuruModel, publishedBook);
        emptyConfigConsumer.processModelGroup(publishedGuruModel, Collections.singletonList(publishedModification));

        GuruCounter counter = emptyConfigConsumer.getGuruCounter();
        Assert.assertEquals(1, counter.getPublishedGuruModelsCount());
        Assert.assertEquals(1, counter.getPublishedGuruModificationCount());
        Assert.assertEquals(3, counter.getAliasesCount());
        Assert.assertEquals(0, counter.getFilledParamsCount());

        GuruModelsConsumer modelsConsumer = new GuruModelsConsumer(createConfiguration(), PUBLISHED_CATEGORY_ID);
        modelsConsumer.processModel(notGuruModel);
        modelsConsumer.processModel(noVendorModel);
        modelsConsumer.processModel(upPublishedGuruModel);
        modelsConsumer.processModel(nonPublishedGuruModel);
        modelsConsumer.processModelGroup(publishedGuruModel, Collections.singletonList(publishedModification));
        modelsConsumer.processModel(publishedGuruModelByVendor);
        modelsConsumer.processModel(publishedGuruModelByAuto);
        modelsConsumer.processModel(publishedGuruModelByYang);

        counter = modelsConsumer.getGuruCounter();
        Assert.assertEquals(5, counter.getPublishedGuruModelsCount());
        Assert.assertEquals(1, counter.getPublishedGuruModificationCount());
        Assert.assertEquals(4, counter.getPublishedOnMarketGuruModelsCount());
        Assert.assertEquals(3, counter.getAliasesCount());
        Assert.assertEquals(3, counter.getFilledParamsCount());
        Assert.assertEquals(4, counter.getPublishedOnMarkeNew());

        Assert.assertEquals(1, counter.getVendorSourceModels());
        Assert.assertEquals(1, counter.getAutoSourceModels());
        Assert.assertEquals(1, counter.getYangSourceModels());
        Assert.assertEquals(1, counter.getOperatorSourceModels());
        Assert.assertEquals(counter.getPublishedOnMarketGuruModelsCount(), counter.getOperatorSourceModels() +
            counter.getVendorSourceModels() +
            counter.getAutoSourceModels() +
            counter.getYangSourceModels());
    }

    @Test
    public void testFakeGrouped() {
        ModelStorage.Model publishedGuruModel = ModelStorageTestUtil.generateModel().toBuilder()
            .setId(PUBLISHED_MODEL_ID)
            .setCurrentType(CommonModel.Source.GURU.name())
            .setSourceType(CommonModel.Source.GURU.name())
            .setCategoryId(PUBLISHED_CATEGORY_ID)
            .addTitles(ModelStorage.LocalizedString.newBuilder()
                .setIsoCode("ru")
                .setValue("SomeName")
                .build())
            .setVendorId(PUBLISHED_VENDOR_ID)
            .clearParentId()
            .setPublished(true)
            .build();

        ModelStorage.Model publishedModification = ModelStorageTestUtil.generateModel().toBuilder()
            .setId(PUBLISHED_MODEL_ID + 1)
            .setCurrentType(CommonModel.Source.GURU.name())
            .setSourceType(CommonModel.Source.GURU.name())
            .setCategoryId(PUBLISHED_CATEGORY_ID)
            .addTitles(ModelStorage.LocalizedString.newBuilder()
                .setIsoCode("ru")
                .setValue("SomeName")
                .build())
            .setVendorId(PUBLISHED_VENDOR_ID)
            .setParentId(publishedGuruModel.getId())
            .setPublished(true)
            .build();

        ModelStorage.Model publishedModification2 = ModelStorageTestUtil.generateModel().toBuilder()
            .setId(PUBLISHED_MODEL_ID + 2)
            .setCurrentType(CommonModel.Source.GURU.name())
            .setSourceType(CommonModel.Source.GURU.name())
            .setCategoryId(PUBLISHED_CATEGORY_ID)
            .addTitles(ModelStorage.LocalizedString.newBuilder()
                .setIsoCode("ru")
                .setValue("SomeName")
                .build())
            .setVendorId(PUBLISHED_VENDOR_ID)
            .setParentId(publishedGuruModel.getId())
            .setPublished(true)
            .build();

        Map<String, String>  configuration = new HashMap<>();

        Map<Long, Boolean> showModel = new HashMap<>();
        showModel.put(PUBLISHED_CATEGORY_ID, true);
        configuration.put(KpiStatsReport.SHOW_MODEL_KEY, ExportMapReduceUtil.serializeMap(showModel));

        String serializedCat = ExportMapReduceUtil.serializeCollection(Arrays.asList(PUBLISHED_CATEGORY_ID));
        configuration.put(KpiStatsReport.GROUP_KEY, serializedCat);

        Multimap<Long, Long> mm = ArrayListMultimap.create();
        mm.put(PUBLISHED_CATEGORY_ID, PUBLISHED_VENDOR_ID);
        configuration.put(KpiStatsReport.SHOW_VENDOR_KEY, ExportMapReduceUtil.serializeMultimap(mm));

        GuruModelsConsumer consumer = new GuruModelsConsumer(configuration, PUBLISHED_CATEGORY_ID);
        consumer.processModelGroup(publishedGuruModel,
            Arrays.asList(publishedModification, publishedModification2));
        GuruCounter counter = consumer.getGuruCounter();
        Assert.assertEquals(1, counter.getPublishedGuruModelsCount());
        Assert.assertEquals(1, counter.getPublishedOnMarketGuruModelsCount());
        Assert.assertEquals(2, counter.getPublishedGuruModificationCount());

        configuration.put(KpiStatsReport.FAKE_GROUP_KEY, serializedCat);

        consumer = new GuruModelsConsumer(configuration, PUBLISHED_CATEGORY_ID);
        consumer.processModelGroup(publishedGuruModel,
            Arrays.asList(publishedModification, publishedModification2));
        counter = consumer.getGuruCounter();
        Assert.assertEquals(2, counter.getPublishedGuruModelsCount());
        Assert.assertEquals(2, counter.getPublishedOnMarketGuruModelsCount());
        Assert.assertEquals(0, counter.getPublishedGuruModificationCount());
    }

    @Test
    public void testConsumerChanges() {
        ModelStorage.Model publishedGuruModel = ModelStorageTestUtil.generateModel().toBuilder()
            .setId(PUBLISHED_MODEL_ID)
            .addTitles(ModelStorage.LocalizedString.newBuilder()
                .setIsoCode("ru")
                .setValue("SomeName")
                .build())
            .setCurrentType(CommonModel.Source.GURU.name())
            .setSourceType(CommonModel.Source.GURU.name())
            .setCategoryId(UNPUBLISHED_CATEGORY_ID)
            .setVendorId(UNPUBLISHED_VENDOR_ID)
            .clearParentId()
            .setPublished(true)
            .build();

        ModelsOnMarketCounter cnt = new ModelsOnMarketCounter();
        cnt.addUnpublishedCategories(Collections.singletonList(UNPUBLISHED_CATEGORY_ID));
        cnt.addPublishedModels(Collections.singletonList(PUBLISHED_MODEL_ID));
        cnt.addUnpublishedVendor(UNPUBLISHED_CATEGORY_ID, UNPUBLISHED_VENDOR_ID);

        GuruModelsConsumer consumer = new GuruModelsConsumer(
            createConfiguration(cnt),
            UNPUBLISHED_CATEGORY_ID);

        consume(consumer, publishedGuruModel);
        GuruCounter counter = consumer.getGuruCounter();
        // распубликовали вендора и категорию но опубликовали модель => в del не учтется
        Assert.assertEquals(0, counter.getPublishedOnMarketDel());
        Assert.assertEquals(0, counter.getPublishedOnMarketGuruModelsCount());
        Assert.assertEquals(1, counter.getPublishedGuruModelsCount());

        cnt = new ModelsOnMarketCounter();
        cnt.addUnpublishedCategories(Collections.singletonList(UNPUBLISHED_CATEGORY_ID));
        cnt.addUnpublishedVendor(UNPUBLISHED_CATEGORY_ID, UNPUBLISHED_VENDOR_ID);

        consumer = new GuruModelsConsumer(
            createConfiguration(cnt),
            UNPUBLISHED_CATEGORY_ID);

        consume(consumer, publishedGuruModel);
        //учтется в del т.к. модель была и раньше опубликована
        counter = consumer.getGuruCounter();
        Assert.assertEquals(1, counter.getPublishedOnMarketDel());
        Assert.assertEquals(0, counter.getPublishedOnMarketGuruModelsCount());
        Assert.assertEquals(1, counter.getPublishedGuruModelsCount());


    }


    @Test
    public void testConsumerUnPublishedCategories() {
        //Не опубликованая категория, поэтому не опубликованно на маркете
        ModelStorage.Model publishedGuruModel = ModelStorageTestUtil.generateModel().toBuilder()
                .setId(PUBLISHED_MODEL_ID + 10)
                .addTitles(ModelStorage.LocalizedString.newBuilder()
                    .setIsoCode("ru")
                    .setValue("SomeName")
                    .build())
                .setCurrentType(CommonModel.Source.GURU.name())
                .setSourceType(CommonModel.Source.GURU.name())
                .setCategoryId(UNPUBLISHED_CATEGORY_ID)
                .setVendorId(PUBLISHED_VENDOR2_ID)
                .clearParentId()
                .setPublished(true)
                .build();

        //не подсчитается в Del т.к. модель не только распубликовали но и вендор у нее распубликован
        ModelStorage.Model unpublishedGuruModel2 = ModelStorageTestUtil.generateModel().toBuilder()
            .setId(UNPUBLISHED_MODEL_ID)
            .addTitles(ModelStorage.LocalizedString.newBuilder()
                .setIsoCode("ru")
                .setValue("SomeName")
                .build())
            .setCurrentType(CommonModel.Source.GURU.name())
            .setSourceType(CommonModel.Source.GURU.name())
            .setCategoryId(UNPUBLISHED_CATEGORY_ID)
            .setVendorId(UNPUBLISHED_VENDOR_ID)
            .clearParentId()
            .setPublished(false)
            .build();

        GuruModelsConsumer unpublishedCategoryConsumer = new GuruModelsConsumer(createConfiguration(),
            UNPUBLISHED_CATEGORY_ID);
        consume(unpublishedCategoryConsumer, publishedGuruModel, unpublishedGuruModel2);
        GuruCounter counter = unpublishedCategoryConsumer.getGuruCounter();
        Assert.assertEquals(1, counter.getPublishedGuruModelsCount());
        Assert.assertEquals(0, counter.getPublishedOnMarketGuruModelsCount());
        Assert.assertEquals(1, counter.getPublishedOnMarketDel());
    }

    private Map<String, String>  createConfiguration() {
        return createConfiguration(getModelsOnMarketCounter());
    }

    private Map<String, String> createConfiguration(ModelsOnMarketCounter onMarket) {
        Multimap<Long, Long> params = ArrayListMultimap.create();
        params.put(PUBLISHED_CATEGORY_ID, 1L);
        params.put(PUBLISHED_CATEGORY_ID, 2L);
        params.put(PUBLISHED_CATEGORY_ID, 3L);

        Multimap<Long, Long> modifParams = ArrayListMultimap.create();
        modifParams.put(PUBLISHED_CATEGORY_ID, 2L);

        Map<Long, Boolean> showModel = new HashMap<>();
        showModel.put(PUBLISHED_CATEGORY_ID, true);

        Multimap<Long, Long> publishedLocalVendor = ArrayListMultimap.create();
        publishedLocalVendor.put(PUBLISHED_CATEGORY_ID, PUBLISHED_VENDOR_ID);
        publishedLocalVendor.put(UNPUBLISHED_CATEGORY_ID, PUBLISHED_VENDOR2_ID);

        Map<String, String> configuration = new HashMap<>();
        configuration.put(KpiStatsReport.GURU_PARAMS_KEY, ExportMapReduceUtil.serializeMultimap(params));
        configuration.put(KpiStatsReport.GURU_MODIFIED_PARAMS_KEY, ExportMapReduceUtil.serializeMultimap(modifParams));

        configuration.put(KpiStatsReport.SHOW_MODEL_KEY, ExportMapReduceUtil.serializeMap(showModel));
        configuration.put(KpiStatsReport.SHOW_VENDOR_KEY, ExportMapReduceUtil.serializeMultimap(publishedLocalVendor));

        configuration.put(KpiStatsReport.PUBLISHED_CAT_KEY,
            ExportMapReduceUtil.serializeCollection(onMarket.getPublishedCategories()));
        configuration.put(KpiStatsReport.UNPUBLISHED_CAT_KEY,
            ExportMapReduceUtil.serializeCollection(onMarket.getUnpublishedCategories()));

        configuration.put(KpiStatsReport.PUBLISHED_VENDORS_KEY,
            ExportMapReduceUtil.serializeMapWithSetValue(onMarket.getPublishedVendors()));
        configuration.put(KpiStatsReport.UNPUBLISHED_VENDORS_KEY,
            ExportMapReduceUtil.serializeMapWithSetValue(onMarket.getUnpublishedVendors()));

        configuration.put(KpiStatsReport.PUBLISHED_MODELS_KEY,
            ExportMapReduceUtil.serializeCollection(onMarket.getPublishedModels()));
        configuration.put(KpiStatsReport.UNPUBLISHED_MODELS_KEY,
            ExportMapReduceUtil.serializeCollection(onMarket.getUnpublishedModels()));
        configuration.put(KpiStatsReport.ASSESSOR_IDS_KEY,
            ExportMapReduceUtil.serializeCollection(Arrays.asList(ASSESSOR_ID)));

        configuration.put(KpiStatsReport.GROUP_KEY,
            ExportMapReduceUtil.serializeCollection(Arrays.asList(PUBLISHED_CATEGORY_ID)));
        return configuration;
    }

    private ModelsOnMarketCounter getModelsOnMarketCounter() {
        ModelsOnMarketCounter counter = new ModelsOnMarketCounter();
        counter.addPublishedCategories(Arrays.asList(PUBLISHED_CATEGORY_ID, PUBLISHED_CATEGORY2_ID));
        counter.getUnpublishedCategories().add(UNPUBLISHED_CATEGORY_ID);

        counter.addPublishedVendor(PUBLISHED_CATEGORY_ID, PUBLISHED_VENDOR_ID);
        counter.addUnpublishedVendor(PUBLISHED_CATEGORY_ID, UNPUBLISHED_VENDOR_ID);

        counter.getPublishedModels().add(PUBLISHED_MODEL_ID);
        counter.getUnpublishedModels().add(UNPUBLISHED_MODEL_ID);

        return counter;
    }

    private void consume(GuruModelsConsumer consumer, ModelStorage.Model... prototypes) {
        for (ModelStorage.Model prototype : prototypes) {
            consumer.processModel(prototype);
        }
    }

    private ModelStorage.ParameterValue stringParam(Long id) {
        return stringParam(id, "id" + id);
    }

    private ModelStorage.ParameterValue stringParam(Long id, String... values) {
        return stringParam("xsl-" + id, id, values);
    }

    private ModelStorage.ParameterValue stringParam(String xsl, Long id, String... values) {
        return ModelStorage.ParameterValue.newBuilder()
            .setParamId(id)
            .setXslName(xsl)
            .setValueType(MboParameters.ValueType.STRING)
            .addAllStrValue(Arrays.stream(values).map(this::convert).collect(Collectors.toList()))
            .build();
    }

    private ModelStorage.LocalizedString convert(String val) {
        return ModelStorage.LocalizedString.newBuilder()
            .setIsoCode(Language.RUSSIAN.getIsoCode())
            .setValue(val)
            .build();
    }
}
