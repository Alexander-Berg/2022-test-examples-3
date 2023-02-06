package ru.yandex.market.mboc.tms.executors;

import java.time.Duration;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.mbo.solomon.SolomonPushService;
import ru.yandex.market.mboc.common.offers.model.upload.OffersUploadStat;
import ru.yandex.market.mboc.common.offers.repository.upload.ErpOfferUploadQueueRepository;
import ru.yandex.market.mboc.common.offers.repository.upload.MdmOfferUploadQueueRepository;
import ru.yandex.market.mboc.common.offers.repository.upload.YtOfferUploadQueueRepository;
import ru.yandex.market.mboc.tms.UpdateOfferUploadingMetricsForSolomonExecutor;
import ru.yandex.misc.monica.solomon.sensors.Sensor;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.solomon.SolomonPushService.SENSOR_TAG;

/**
 * @author apluhin
 * @created 10/23/20
 */
public class UpdateOfferUploadingMetricsForSolomonExecutorTest {
    private UpdateOfferUploadingMetricsForSolomonExecutor executor;
    private YtOfferUploadQueueRepository ytOfferUploadQueueRepository;
    private ErpOfferUploadQueueRepository erpOfferUploadQueueRepository;
    private MdmOfferUploadQueueRepository mdmOfferUploadQueueRepository;
    private SolomonPushService solomonPushService;

    @Before
    public void setUp() throws Exception {
        ytOfferUploadQueueRepository = Mockito.mock(YtOfferUploadQueueRepository.class);
        erpOfferUploadQueueRepository = Mockito.mock(ErpOfferUploadQueueRepository.class);
        mdmOfferUploadQueueRepository = Mockito.mock(MdmOfferUploadQueueRepository.class);
        solomonPushService = Mockito.mock(SolomonPushService.class);
        executor = new UpdateOfferUploadingMetricsForSolomonExecutor(
                ytOfferUploadQueueRepository,
                erpOfferUploadQueueRepository,
                mdmOfferUploadQueueRepository,
                solomonPushService
        );
    }

    @Test
    public void testCorrectPushSensors() {
        var offersStat = new OffersUploadStat(1,
                Duration.ofSeconds(10),
                Duration.ofSeconds(5),
                Duration.ofSeconds(8),
                Duration.ofSeconds(9));
        when(ytOfferUploadQueueRepository.calcOfferUploadingStat()).thenReturn(offersStat);
        when(erpOfferUploadQueueRepository.calcOfferUploadingStat()).thenReturn(offersStat);
        when(mdmOfferUploadQueueRepository.calcOfferUploadingStat()).thenReturn(offersStat);
        executor.execute();
        var sensorCaptor = ArgumentCaptor.forClass(Sensor.class);
        verify(solomonPushService, times(15)).push(sensorCaptor.capture());
        var mappedSensors = sensorCaptor.getAllValues().stream()
                .collect(Collectors.toMap(it -> it.labels.get(SENSOR_TAG), it -> it));

        assertTrue(mappedSensors.containsKey("mboc_offer_uploading_yt_queue_size"));
        assertTrue(mappedSensors.containsKey("mboc_offer_uploading_erp_queue_size"));
        assertTrue(mappedSensors.containsKey("mboc_offer_uploading_mdm_queue_size"));
    }

}
