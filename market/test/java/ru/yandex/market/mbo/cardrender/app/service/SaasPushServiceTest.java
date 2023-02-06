package ru.yandex.market.mbo.cardrender.app.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.cardrender.app.BaseTest;
import ru.yandex.market.mbo.cardrender.app.model.saas.DeleteModelSaasRow;
import ru.yandex.market.mbo.cardrender.app.model.saas.SaasRenderModelHolder;
import ru.yandex.market.mbo.cardrender.app.repository.DeleteModelLogRepository;
import ru.yandex.market.mbo.cardrender.app.repository.YtModelRenderRepository;
import ru.yandex.market.mbo.export.ExportReportModels;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.saas.SaasDocumentWriter;
import ru.yandex.market.mbo.saas.SaasLogbrokerEvent;
import ru.yandex.market.mbo.saas.SaasPushClient;
import ru.yandex.market.mbo.storage.StorageKeyValueService;

import static org.mockito.Mockito.times;

/**
 * @author apluhin
 * @created 12/8/21
 */
public class SaasPushServiceTest extends BaseTest {

    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    private SaasDocumentWriter<SaasRenderModelHolder> saasDocumentWriter;
    private SaasPushClient saasSlowPushClient;
    private SaasPushClient saasFastPushClient;
    @Autowired
    private DeleteModelLogRepository deleteModelLogRepository;
    private YtModelRenderRepository ytModelRenderRepository;

    private SaasPushService saasPushService;

    @Before
    public void setUp() throws Exception {
        ytModelRenderRepository = Mockito.mock(YtModelRenderRepository.class);
        saasSlowPushClient = Mockito.mock(SaasPushClient.class);
        saasFastPushClient = Mockito.mock(SaasPushClient.class);
        saasPushService = new SaasPushService(
                storageKeyValueService,
                saasDocumentWriter,
                deleteModelLogRepository,
                saasSlowPushClient,
                saasFastPushClient,
                ytModelRenderRepository);
    }

    @Test
    public void testIgnoreSaveByFlag() throws InterruptedException {
        storageKeyValueService.putValue("enabled_saas_push", false);
        storageKeyValueService.invalidateCache();
        saasPushService.sendModels(List.of(buildModel(1L)));
        Mockito.verify(saasSlowPushClient, times(0)).publishEvents(Mockito.any());
    }

    @Test
    public void testSendToSaasPush() throws InterruptedException {
        storageKeyValueService.putValue("enabled_saas_push", Boolean.valueOf("true"));
        storageKeyValueService.invalidateCache();
        saasPushService.sendModels(List.of(buildModel(1L)));
        Mockito.verify(saasFastPushClient, times(1)).publishEvents(Mockito.any());
    }

    @Test
    public void testIgnoreSendToSaasPushCluster() throws InterruptedException {
        storageKeyValueService.putValue("enabled_saas_push", Boolean.valueOf("true"));
        storageKeyValueService.invalidateCache();
        ExportReportModels.ExportReportModel fastSku = ExportReportModels.ExportReportModel.newBuilder()
                .setId(1L).setCurrentType(CommonModel.Source.CLUSTER.name()).setCategoryId(1L).setVendorId(1L).build();
        saasPushService.sendModels(List.of(fastSku));
        Mockito.verify(saasFastPushClient, times(0)).publishEvents(Mockito.any());
    }

    @Test
    public void testIgnoreDeleteAsyncByFlag() {
        storageKeyValueService.putValue("enabled_saas_push", false);
        storageKeyValueService.invalidateCache();
        saasPushService.sendDeleteWithDelay(List.of(1L));
        Assertions.assertThat(deleteModelLogRepository.findAll()).isEmpty();
    }

    @Test
    public void testUpdateDeleteEvent() {
        storageKeyValueService.putValue("enabled_saas_push", true);
        storageKeyValueService.invalidateCache();
        DeleteModelSaasRow deleteModelSaasRow = DeleteModelSaasRow.skuDelete(1L, LocalDateTime.now().minusDays(1));
        saasPushService.sendDeleteWithDelayRows(List.of(deleteModelSaasRow));
        List<DeleteModelSaasRow> rows = deleteModelLogRepository.findAll();
        Assertions.assertThat(rows.get(0)).isEqualTo(deleteModelSaasRow);

        DeleteModelSaasRow updatedEvent = DeleteModelSaasRow.fullDelete(1L, LocalDateTime.now().plusDays(1));
        saasPushService.sendDeleteWithDelayRows(List.of(updatedEvent));

        List<DeleteModelSaasRow> newRows = deleteModelLogRepository.findAll();
        Assertions.assertThat(newRows.get(0)).isEqualTo(updatedEvent);
        Assertions.assertThat(newRows.get(0)).isNotEqualTo(deleteModelSaasRow);
    }

    @Test
    public void testSkipAliveModels() throws InterruptedException {
        storageKeyValueService.putValue("enabled_saas_push", true);
        storageKeyValueService.invalidateCache();
        LocalDateTime now = LocalDateTime.now();
        DeleteModelSaasRow sku = DeleteModelSaasRow.skuDelete(1L, now);
        DeleteModelSaasRow model = DeleteModelSaasRow.modelDelete(2L, now);
        DeleteModelSaasRow full = DeleteModelSaasRow.fullDelete(3L, now);

        Mockito.when(ytModelRenderRepository.isContainsModels(Mockito.eq(List.of(2L, 3L))))
                .thenReturn(Map.of(3L, true));
        Mockito.when(ytModelRenderRepository.isContainsSkus(Mockito.eq(List.of(1L, 3L))))
                .thenReturn(Map.of(1L, true));
        saasPushService.sendDelete(
                List.of(sku, model, full)
        );
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(saasFastPushClient, times(1)).publishEvents(captor.capture());

        Assertions.assertThat(captor.getValue().size()).isEqualTo(2);
        List<SaasLogbrokerEvent> value = captor.getValue();
        Assertions.assertThat(value.stream().map(it -> it.getPayload().getDocument().getUrl()).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(
                        "2M", "3S"
                );
    }

    @Test
    public void testDeleteAsyncByFlag() {
        storageKeyValueService.putValue("enabled_saas_push", true);
        storageKeyValueService.invalidateCache();
        saasPushService.sendDeleteWithDelay(List.of(1L));
        Assertions.assertThat(
                deleteModelLogRepository.findAll().stream().map(it -> it.getModelId()).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(1L);
    }

    @Test
    public void testDeleteAsyncByFlagTwice() {
        storageKeyValueService.putValue("enabled_saas_push", true);
        storageKeyValueService.invalidateCache();
        saasPushService.sendDeleteWithDelay(List.of(1L));
        saasPushService.sendDeleteWithDelay(List.of(1L));
        Assertions.assertThat(
                deleteModelLogRepository.findAll().stream().map(it -> it.getModelId()).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(1L);
    }

    @Test(expected = RuntimeException.class)
    public void testIgnoreDeleteByFlag() throws InterruptedException {
        storageKeyValueService.putValue("enabled_saas_push", false);
        storageKeyValueService.invalidateCache();
        saasPushService.sendDelete(List.of(DeleteModelSaasRow.skuDelete(1L, LocalDateTime.now())));
        Mockito.verify(saasSlowPushClient, times(0)).publishEvents(Mockito.any());
    }

    @Test
    public void testDeleteByFlag() throws InterruptedException {
        storageKeyValueService.putValue("enabled_saas_push", true);
        storageKeyValueService.invalidateCache();
        saasPushService.sendDelete(List.of(DeleteModelSaasRow.skuDelete(1L, LocalDateTime.now())));
        Mockito.verify(saasFastPushClient, times(1)).publishEvents(Mockito.any());
    }

    private ExportReportModels.ExportReportModel buildModel(long id) {
        return ExportReportModels.ExportReportModel.newBuilder()
                .setId(id).setCurrentType(CommonModel.Source.SKU.name()).setCategoryId(id).setVendorId(id).build();
    }
}
