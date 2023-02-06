package ru.yandex.market.mbo.cardrender.app.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.kikimr.persqueue.consumer.StreamConsumer;
import ru.yandex.kikimr.persqueue.consumer.stream.StreamConsumerConfig;
import ru.yandex.market.logbroker.model.LogbrokerInstallation;
import ru.yandex.market.mbo.cardrender.app.repository.YtModelRenderRepository;
import ru.yandex.market.mbo.storage.StorageKeyValueService;

/**
 * @author apluhin
 * @created 6/7/21
 */
public class ModelRenderLBServiceTest {

    private ModelRenderLBService modelRenderLBService;
    private LogbrokerInstallation logbrokerInstallation;
    private StreamConsumerConfig streamConsumerConfig;
    private YtModelRenderRepository modelRenderRepository;
    private StorageKeyValueService storageKeyValue;

    @Before
    public void setUp() throws Exception {
        logbrokerInstallation = Mockito.mock(LogbrokerInstallation.class);
        streamConsumerConfig = Mockito.mock(StreamConsumerConfig.class);
        modelRenderRepository = Mockito.mock(YtModelRenderRepository.class);
        storageKeyValue = Mockito.mock(StorageKeyValueService.class);
        modelRenderLBService = new ModelRenderLBService(
                logbrokerInstallation,
                streamConsumerConfig,
                modelRenderRepository,
                storageKeyValue
        );
    }

    @Test
    public void checkEnableDelivery() {
        modelRenderLBService.enableDelivery();
        Mockito.verify(storageKeyValue, Mockito.times(1))
                .putValue(Mockito.eq("render_model_to_yt_enabled"), Mockito.eq(true));
    }

    @Test
    public void checkDisableDelivery() {
        modelRenderLBService.disableDelivery();
        Mockito.verify(storageKeyValue, Mockito.times(1))
                .putValue(Mockito.eq("render_model_to_yt_enabled"), Mockito.eq(false));
    }

    @Test
    public void checkScheduledTaskEnable() throws InterruptedException {
        Mockito.when(storageKeyValue.getBool(Mockito.eq("render_model_to_yt_enabled"), Mockito.eq(true)))
                .thenReturn(true);
        Mockito.when(logbrokerInstallation.createStreamConsumer(Mockito.any())).thenReturn(Mockito.mock(StreamConsumer.class));
        modelRenderLBService.validateDeliveryStatus();
        Mockito.verify(logbrokerInstallation, Mockito.times(1)).createStreamConsumer(Mockito.any());
        Assert.assertEquals(true, ReflectionTestUtils.getField(modelRenderLBService, "currentEnableDeliveryStatus"));
    }

    @Test
    public void checkScheduledTaskDisable() {
        Mockito.when(storageKeyValue.getBool(Mockito.eq("render_model_to_yt_enabled"), Mockito.eq(true)))
                .thenReturn(false);
        StreamConsumer mock = Mockito.mock(StreamConsumer.class);
        ReflectionTestUtils.setField(modelRenderLBService, "streamConsumer", mock);
        ReflectionTestUtils.setField(modelRenderLBService, "currentEnableDeliveryStatus", true);
        modelRenderLBService.validateDeliveryStatus();
        Mockito.verify(mock, Mockito.times(1)).stopConsume();
        Assert.assertEquals(false, ReflectionTestUtils.getField(modelRenderLBService, "currentEnableDeliveryStatus"));
    }

}
