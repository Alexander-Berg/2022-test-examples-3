package ru.yandex.market.mbo.cardrender.app.service.yql;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.mbo.cardrender.app.BaseTest;
import ru.yandex.market.mbo.cardrender.app.model.saas.SaasRenderModelHolder;
import ru.yandex.market.mbo.cardrender.app.repository.YtModelRenderRepository;
import ru.yandex.market.mbo.cardrender.app.service.DatacampService;
import ru.yandex.market.mbo.cardrender.app.service.SaasPushService;
import ru.yandex.market.mbo.db.modelstorage.health.ReadStats;
import ru.yandex.market.mbo.db.modelstorage.yt.YtModelStore;
import ru.yandex.market.mbo.export.ExportReportModels;
import ru.yandex.market.mbo.gwt.models.modelstorage.CategoryModelId;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.storage.StorageKeyValueService;

import static org.mockito.Mockito.times;

/**
 * @author apluhin
 * @created 12/17/21
 */
public class StuffReaderTest extends BaseTest {


    private FullStuffReader fullStuffReader;
    private DiffStuffReader diffStuffReader;
    private SaasPushService saasPushService;
    private JdbcTemplate yqlJdbcTemplate;
    private DatacampService datacampService;
    private YtModelStore ytModelStore;
    @Autowired
    private StorageKeyValueService storageKeyValueService;

    @Before
    public void setUp() throws Exception {
        yqlJdbcTemplate = Mockito.mock(JdbcTemplate.class);
        saasPushService = Mockito.mock(SaasPushService.class);
        datacampService = Mockito.mock(DatacampService.class);
        ytModelStore = Mockito.mock(YtModelStore.class);
        fullStuffReader = new FullStuffReader(
                yqlJdbcTemplate,
                saasPushService,
                storageKeyValueService,
                datacampService,
                ytModelStore,
                Mockito.mock(YtModelRenderRepository.class));
        diffStuffReader = new DiffStuffReader(
                yqlJdbcTemplate,
                saasPushService,
                storageKeyValueService,
                datacampService,
                ytModelStore,
                Mockito.mock(YtModelRenderRepository.class));
    }

    @Test
    public void testStuffReader() throws InterruptedException {
        List<SaasRenderModelHolder> models = testModels();
        mockYtRepository(models);
        diffStuffReader.handleBatch(models);

        Mockito.verify(saasPushService, times(1)).sendModelsBytes(Mockito.anyList());
        Mockito.verify(datacampService, times(1)).sendToLbReportModels(Mockito.anyList());
    }

    @Test
    public void testIgnoreLbStuffReader() throws InterruptedException {
        List<SaasRenderModelHolder> models = testModels();
        mockYtRepository(models);
        fullStuffReader.handleBatch(models);

        Mockito.verify(saasPushService, times(1)).sendModelsBytes(Mockito.anyList());
        Mockito.verify(datacampService, times(0)).sendToLbReportModels(Mockito.anyList());
    }

    @Test
    public void testLoadFastSku() throws InterruptedException {
        SaasRenderModelHolder model1 = mockResultSet(1L);
        SaasRenderModelHolder fastSku = new SaasRenderModelHolder(2L, 2L, 2L, 2L,
                ExportReportModels.ExportReportModel.newBuilder().setModifiedTs(1L)
                        .setArticle("duplicate1").setCurrentType("FAST_SKU").build());
        //бывали случае, когда в mbo-models жили две живые FAST_SKU
        SaasRenderModelHolder duplicateFastSku = new SaasRenderModelHolder(2L, 2L, 2L, 2L,
                ExportReportModels.ExportReportModel.newBuilder().setModifiedTs(2L)
                        .setArticle("duplicate2").setCurrentType("FAST_SKU").build());


        List<SaasRenderModelHolder> models = List.of(model1, fastSku, duplicateFastSku);
        mockYtRepository(models);
        diffStuffReader.handleBatch(models);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(saasPushService, times(1)).sendModelsBytes(captor.capture());
        Mockito.verify(datacampService, times(1)).sendToLbReportModels(captor.capture());

        List<List> allValues = captor.getAllValues();
        Assertions.assertThat(allValues.get(0).size()).isEqualTo(2);
        Assertions.assertThat(allValues.get(1).size()).isEqualTo(2);

        List<SaasRenderModelHolder> saasRenderModelHolders = allValues.get(0);
        List<Long> exportTs = saasRenderModelHolders
                .stream().map(it -> it.getExportReportModel().getExportTs()).collect(Collectors.toList());
        Assertions.assertThat(exportTs).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    public void testSkipDeleted() throws InterruptedException {
        SaasRenderModelHolder model = mockResultSet(1L);


        Mockito.when(ytModelStore.getModels(
                Mockito.eq(Set.of(new CategoryModelId(1L, 1L))), Mockito.any()))
                .thenReturn(List.of(ModelStorage.Model.newBuilder()
                        .setDeletedDate(100).setCategoryId(2L).setId(2L).setCurrentType("SKU").build()));

        diffStuffReader.handleBatch(List.of(model));

        Mockito.verify(saasPushService, times(0)).sendModelsBytes(Mockito.anyList());
        Mockito.verify(datacampService, times(0)).sendToLbReportModels(Mockito.anyList());
    }

    @Test(expected = RuntimeException.class)
    public void testFailedByFailPartLbStuffReader() {
        storageKeyValueService.putValue("push_diff_batch_size", 500);
        Mockito.when(datacampService.sendToLbReportModels(Mockito.anyList())).thenThrow(RuntimeException.class);

        List<SaasRenderModelHolder> models = testModels();
        mockYtRepository(models);

        diffStuffReader.handleBatch(models);
    }

    @SneakyThrows
    private SaasRenderModelHolder mockResultSet(Long id) {
        return new SaasRenderModelHolder(
                id,
                id,
                id,
                id,
                ExportReportModels.ExportReportModel.newBuilder().setExportTs(id).setId(id).build()
        );
    }

    public List<SaasRenderModelHolder> testModels() {
        List<SaasRenderModelHolder> models =
                LongStream.range(0, 100).boxed().map(it -> mockResultSet(it)).collect(Collectors.toList());
        return models;
    }

    public void mockYtRepository(List<SaasRenderModelHolder> modelHolders) {
        Mockito.when(ytModelStore.getModels(Mockito.anyCollection(), Mockito.any(ReadStats.class)))
                .thenReturn(
                        modelHolders.stream().map(it ->
                                ModelStorage.Model.newBuilder()
                                        .setCategoryId(it.getCategoryId())
                                        .setId(it.getModelId()).build())
                                .collect(Collectors.toList())
                );
    }

}
