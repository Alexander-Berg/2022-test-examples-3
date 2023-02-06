package ru.yandex.market.mbo.tms.health;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.ModelStorageTestUtil;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.tms.health.binding.params.BindingParamsCounter;
import ru.yandex.market.mbo.tms.health.binding.params.BindingParamsResultConsumer;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

/**
 * @author york
 * @since 25.05.2017
 */
@SuppressWarnings({"checkstyle:magicnumber", "checkstyle:linelength"})
public class BindingParamsHealthTest {

    private static final String CONFIG_KEY = "binding-params-map";

    private static Random random = new Random();

    @Test
    public void testConfigPropogation() {
        Multimap<Long, Long> paramsMap = ArrayListMultimap.create();
        paramsMap.put(100L, 1L);
        paramsMap.put(100L, 2L);
        paramsMap.put(200L, 1L);
        paramsMap.put(200L, 3L);
        paramsMap.put(200L, 4L);
        paramsMap.put(200L, 5L);
        String serializedConfiguration = serializeConfiguration(paramsMap);
        BindingParamsResultConsumer consumer = new BindingParamsResultConsumer(serializedConfiguration, 100L);
        Assert.assertEquals(2, consumer.getCounter().getBindingParamsCount());
        consumer = new BindingParamsResultConsumer(serializedConfiguration, 200L);
        Assert.assertEquals(4, consumer.getCounter().getBindingParamsCount());
    }

    @Test
    public void testCounters() throws Exception {
        BindingParamsCounter stats = new BindingParamsCounter();
        stats.setBindingParamsCount(1);

        stats.incrPublishedCount(10);
        Assert.assertEquals(10, stats.getPublishedCount());
        stats.incrPublishedCount(5);
        Assert.assertEquals(15, stats.getPublishedCount());

        stats.incrUnpublishedCount(8);
        Assert.assertEquals(8, stats.getUnpublishedCount());
        stats.incrUnpublishedCount(4);
        Assert.assertEquals(12, stats.getUnpublishedCount());

        stats.incrPublishedWithAbsentCount(3);
        Assert.assertEquals(3, stats.getPublishedWithAbsentCount());
        stats.incrPublishedWithAbsentCount(2);
        Assert.assertEquals(5, stats.getPublishedWithAbsentCount());

        stats.incrUnpublishedWithAbsentCount(1);
        Assert.assertEquals(1, stats.getUnpublishedWithAbsentCount());
        stats.incrUnpublishedWithAbsentCount(3);
        Assert.assertEquals(4, stats.getUnpublishedWithAbsentCount());

        Assert.assertEquals(1, stats.getBindingParamsCount());

        Assert.assertEquals(1, stats.getBindingParamsCount());
        Assert.assertEquals(15, stats.getPublishedCount());
        Assert.assertEquals(12, stats.getUnpublishedCount());
        Assert.assertEquals(5, stats.getPublishedWithAbsentCount());
        Assert.assertEquals(4, stats.getUnpublishedWithAbsentCount());
    }

    @Test
    public void testConsumer() throws Exception {
        Multimap<Long, Long> paramsMap = ArrayListMultimap.create();
        paramsMap.put(100L, 1L);
        paramsMap.put(100L, 2L);
        String serializedConfiguration = serializeConfiguration(paramsMap);
        BindingParamsResultConsumer consumer = new BindingParamsResultConsumer(serializedConfiguration, 100L);
        ModelStorage.Model.Builder publishedGuruModel = ModelStorageTestUtil.generateModel().toBuilder()
            .setCurrentType(CommonModel.Source.GURU.name())
            .clearParameterValues()
            .addAllParameterValues(
                Arrays.asList(pVal(10L), pVal(2L), pVal(3L))
            );
        consumer.processModel(publishedGuruModel.build(), true);

        ModelStorage.Model.Builder unpublishedGuruModel = ModelStorageTestUtil.generateModel().toBuilder()
            .setCurrentType(CommonModel.Source.GURU.name())
            .clearParameterValues()
            .addAllParameterValues(
                Arrays.asList(pVal(2L), pVal(3L))
            );
        consumer.processModel(unpublishedGuruModel.build(), false);

        ModelStorage.Model.Builder unpublishedGuruModel2 = ModelStorageTestUtil.generateModel().toBuilder()
            .setCurrentType(CommonModel.Source.GURU.name())
            .clearParameterValues()
            .addAllParameterValues(
                Arrays.asList(pVal(1L), pVal(4L))
            );
        consumer.processModel(unpublishedGuruModel2.build(), false);

        ModelStorage.Model.Builder unpublishedGuruModel3 = ModelStorageTestUtil.generateModel().toBuilder()
            .setCurrentType(CommonModel.Source.GURU.name())
            .clearParameterValues()
            .addAllParameterValues(
                Arrays.asList(pVal(10L), pVal(2L), pVal(1L))
            );
        consumer.processModel(unpublishedGuruModel3.build(), false);

        ModelStorage.Model.Builder publishedClusterModel = ModelStorageTestUtil.generateModel().toBuilder()
            .setCurrentType(CommonModel.Source.CLUSTER.name())
            .clearParameterValues()
            .addAllParameterValues(
                Arrays.asList(pVal(1L), pVal(2L), pVal(3L))
            );
        consumer.processModel(publishedClusterModel.build(), true);

        BindingParamsCounter stats = consumer.getCounter();
        Assert.assertEquals(2, stats.getBindingParamsCount());
        Assert.assertEquals(1, stats.getPublishedCount());
        Assert.assertEquals(1, stats.getPublishedWithAbsentCount());
        Assert.assertEquals(3, stats.getUnpublishedCount());
        Assert.assertEquals(2, stats.getUnpublishedWithAbsentCount());
    }

    private ModelStorage.ParameterValue pVal(Long id) {
        return ModelStorage.ParameterValue.newBuilder()
            .setParamId(id)
            .setXslName("xsl-" + id)
            .setValueType(MboParameters.ValueType.ENUM)
            .setOptionId(random.nextInt())
            .build();
    }

    private String serializeConfiguration(Multimap<Long, Long> paramsMap) {
        Map<String, String> sub = BindingParamsResultConsumer.getConfig(paramsMap);
        return sub.get(CONFIG_KEY);
    }
}
