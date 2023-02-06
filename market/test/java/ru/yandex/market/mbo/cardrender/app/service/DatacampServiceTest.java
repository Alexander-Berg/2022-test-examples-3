package ru.yandex.market.mbo.cardrender.app.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import Market.DataCamp.API.Operation;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.mbo.cardrender.app.model.datacamp.DatacampLogbrokerEvent;
import ru.yandex.market.mbo.cardrender.app.task.model.ModelCategoryWithTimestamp;
import ru.yandex.market.mbo.common.logbroker.LogbrokerContext;
import ru.yandex.market.mbo.common.logbroker.LogbrokerEvent;
import ru.yandex.market.mbo.common.logbroker.LogbrokerProducerService;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.storage.StorageKeyValueService;

/**
 * @author apluhin
 * @created 1/18/22
 */
public class DatacampServiceTest {

    private StorageKeyValueService storageKeyValueService;
    private DatacampServiceMockLb datacampService;

    @Before
    public void setUp() throws Exception {
        storageKeyValueService = Mockito.mock(StorageKeyValueService.class);
        datacampService = new DatacampServiceMockLb(
                null,
                null,
                storageKeyValueService
        );
    }

    @Test
    public void testSendDeleteModels() {
        List<ModelCategoryWithTimestamp> modelCategoryWithTimestamps = generateDeleteList(101);
        datacampService.sendDeleteModels(modelCategoryWithTimestamps);

        ArgumentCaptor<LogbrokerContext> captor = ArgumentCaptor.forClass(LogbrokerContext.class);
        Mockito.verify(datacampService.currentProducer, Mockito.times(1))
                .uploadEvents(captor.capture());

        List<LogbrokerContext> contexts = captor.getAllValues();
        Assertions.assertThat(contexts.size()).isEqualTo(1);
        LogbrokerContext context = contexts.get(0);
        List<LogbrokerEvent<DatacampLogbrokerEvent>> events = context.getEvents();
        Assertions.assertThat(events.get(0).getEvent().getPayload().getMarketSkus().getMskuList().size()).isEqualTo(100);
        Assertions.assertThat(events.get(1).getEvent().getPayload().getMarketSkus().getMskuList().size()).isEqualTo(1);
        Assertions.assertThat(events.get(1).getEvent().getPayload().getOperationList().get(0).getType())
                .isEqualTo(Operation.OperationMeta.EType.DELETE);
    }

    @Test
    public void testSendUpsertModels() {
        List<ModelStorage.Model> models = generateModelList(20);

        Mockito.when(storageKeyValueService.getCachedInt(Mockito.eq("update_batch_size_lb"), Mockito.any()))
                .thenReturn(10);

        datacampService.sendToLbModelStorageModels(models);

        ArgumentCaptor<LogbrokerContext> captor = ArgumentCaptor.forClass(LogbrokerContext.class);
        Mockito.verify(datacampService.currentProducer, Mockito.times(1))
                .uploadEvents(captor.capture());

        List<LogbrokerContext> contexts = captor.getAllValues();
        Assertions.assertThat(contexts.size()).isEqualTo(1);
        LogbrokerContext context = contexts.get(0);
        List<LogbrokerEvent<DatacampLogbrokerEvent>> events = context.getEvents();
        Assertions.assertThat(events.get(0).getEvent().getPayload().getMarketSkus().getMskuList().size()).isEqualTo(10);
        Assertions.assertThat(events.get(1).getEvent().getPayload().getMarketSkus().getMskuList().size()).isEqualTo(10);
        Assertions.assertThat(events.get(0).getEvent().getPayload().getOperationList().get(0).getType())
                .isEqualTo(Operation.OperationMeta.EType.UPSERT);
    }

    @Test
    public void testUpdateProducer() {
        Mockito.when(datacampService.currentProducer.uploadEvents(Mockito.any())).thenReturn(
                List.of(new LogbrokerEvent<DatacampLogbrokerEvent>().setStatus(LogbrokerEvent.Status.ERROR))
        );
        Mockito.when(storageKeyValueService.getCachedInt(Mockito.eq("update_batch_size_lb"), Mockito.any()))
                .thenReturn(10);
        IntStream.range(0, 11).boxed().forEach(iter -> {
            List<ModelStorage.Model> modelList = generateModelList(1);
            try {
                datacampService.sendToLbModelStorageModels(modelList);
            } catch (Exception e) {
                //ignore
            }
        });

        LogbrokerProducerService currentProducer = datacampService.currentProducer;
        datacampService.validateProducerStatus();
        Mockito.verify(currentProducer, Mockito.times(1)).tearDown();
        Assertions.assertThat(currentProducer).isNotEqualTo(datacampService.currentProducer);
    }

    private List<ModelCategoryWithTimestamp> generateDeleteList(int size) {
        return IntStream.range(0, size)
                .boxed().map(it -> new ModelCategoryWithTimestamp(it, it, it)).collect(Collectors.toList());
    }

    private List<ModelStorage.Model> generateModelList(int size) {
        return IntStream.range(0, size)
                .boxed().map(it -> ModelStorage.Model.newBuilder().setId(it).build()).collect(Collectors.toList());
    }
}
