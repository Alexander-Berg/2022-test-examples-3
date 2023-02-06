package ru.yandex.market.mbo.cardrender.app.service;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.mbo.export.ExportReportModels;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.taskqueue.TaskRecord;

/**
 * @author apluhin
 * @created 3/9/22
 */
public class SendModelServiceTest {

    private SendModelService sendModelService;

    private SaasPushService saasPushService;
    private DatacampService datacampService;
    private ContentStorageRemoteService contentStorageRemoteService;

    @Before
    public void setUp() throws Exception {
        saasPushService = Mockito.mock(SaasPushService.class);
        datacampService = Mockito.mock(DatacampService.class);
        contentStorageRemoteService = Mockito.mock(ContentStorageRemoteService.class);
        sendModelService = new SendModelService(
                saasPushService,
                datacampService,
                contentStorageRemoteService
        );
    }

    @Test
    public void testRenderModel() throws InterruptedException {
        TaskRecord taskRecord = new TaskRecord();
        taskRecord.setId(1L);
        sendModelService.sendModels(taskRecord, models());

        var captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(saasPushService, Mockito.times(1)).sendModels(captor.capture());
        Mockito.verify(datacampService, Mockito.times(1)).sendToLbReportModels(captor.capture());
        Mockito.verify(contentStorageRemoteService, Mockito.times(1)).enrichTemplate(Mockito.anyList());

        var saas = (List<ExportReportModels.ExportReportModel>) captor.getAllValues().get(0);
        var datacamp = (List<ExportReportModels.ExportReportModel>) captor.getAllValues().get(0);
        Assertions.assertThat(saas).isEqualTo(datacamp);
        Assertions.assertThat(saas.get(0).getId()).isEqualTo(models().get(0).getId());
    }

    @Test
    public void testRenderFastSku() throws InterruptedException {
        TaskRecord taskRecord = new TaskRecord();
        taskRecord.setId(1L);
        sendModelService.sendModels(taskRecord, models(), true);

        var captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(saasPushService, Mockito.times(1)).sendModels(captor.capture());
        Mockito.verify(datacampService, Mockito.times(1)).sendToLbReportModels(captor.capture());
        Mockito.verify(contentStorageRemoteService, Mockito.times(0)).enrichTemplate(Mockito.anyList());
    }

    private List<ModelStorage.Model> models() {
        return List.of(
                ModelStorage.Model.newBuilder().setId(1L).build()
        );
    }
}
