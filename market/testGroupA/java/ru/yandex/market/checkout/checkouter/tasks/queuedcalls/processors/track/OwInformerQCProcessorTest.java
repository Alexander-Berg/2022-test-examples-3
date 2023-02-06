package ru.yandex.market.checkout.checkouter.tasks.queuedcalls.processors.track;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.RuPostIntegrationClient;
import ru.yandex.market.checkout.checkouter.delivery.tracking.CheckpointStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackCheckpoint;
import ru.yandex.market.checkout.checkouter.order.delivery.track.TrackingService;
import ru.yandex.market.checkout.checkouter.returns.AbstractReturnTestBase;
import ru.yandex.market.checkout.checkouter.returns.DeliveryOptionPrice;
import ru.yandex.market.checkout.checkouter.returns.ReturnDelivery;
import ru.yandex.market.checkout.checkouter.service.ow.OwClient;
import ru.yandex.market.checkout.checkouter.service.ow.OwTicketDto;
import ru.yandex.market.checkout.checkouter.storage.returns.ReturnDeliveryDao;
import ru.yandex.market.checkout.checkouter.storage.returns.ReturnDeliveryHistoryDao;
import ru.yandex.market.queuedcalls.QueuedCallProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class OwInformerQCProcessorTest extends AbstractReturnTestBase {

    private static final String TRACK_CODE = "TRACK_CODE";
    private static final Long TRACK_ID = 101L;
    private static final Long DELIVERY_SERVICE_ID = 100L;
    private static final Long RETURN_DELIVERY_ID = 333L;
    private static final String OW_TICKET_ID = "some@ticket";
    @Mock
    private OwClient owClient;
    @Mock
    private ReturnDeliveryDao returnDeliveryDao;
    @Mock
    private ReturnDeliveryHistoryDao returnDeliveryHistoryDao;
    @Mock
    private TrackingService trackingService;
    @Mock
    private RuPostIntegrationClient ruPostIntegrationClient;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private OwInformerQCProcessor owInformerQCProcessor;

    @BeforeEach
    void init() {
        initMocks(this);
        owInformerQCProcessor = new OwInformerQCProcessor(
                owClient, returnDeliveryDao, returnDeliveryHistoryDao, trackingService,
                ruPostIntegrationClient, 10,
                60, 2);
        owInformerQCProcessor.setTransactionTemplate(transactionTemplate);
    }

    @Test
    public void sendCheckpointToOwSuccess() {

        ReturnDelivery returnDelivery = ReturnDelivery.newReturnDelivery(DeliveryType.POST, DELIVERY_SERVICE_ID);
        returnDelivery.setOwTicketId(OW_TICKET_ID);
        returnDelivery.setPrice(new DeliveryOptionPrice(BigDecimal.valueOf(234.56), Currency.RUR));
        Track track = createTrack(true);
        returnDelivery.setTrack(track);

        when(returnDeliveryDao.findReturnDeliveryById(anyLong()))
                .thenReturn(Optional.of(returnDelivery));
        when(trackingService.findTrackById(anyLong())).thenReturn(Optional.of(track));

        owInformerQCProcessor.process(new QueuedCallProcessor.QueuedCallExecution(TRACK_ID,
                null, 3, Instant.now(), TRACK_ID));
        ArgumentCaptor<OwTicketDto> dtoArgumentCaptor = ArgumentCaptor.forClass(OwTicketDto.class);
        verify(owClient).updateTicket(dtoArgumentCaptor.capture(), any());
        OwTicketDto dto = dtoArgumentCaptor.getValue();
        Assertions.assertTrue(dto.getComment().getBody().contains("234.56 RUR"));
        Assertions.assertTrue(dto.getComment().getBody().contains("Получено отправителем"));
    }

    @Test
    public void sendCheckpointToOwSuccessWhenNoPrice() {

        ReturnDelivery returnDelivery = ReturnDelivery.newReturnDelivery(DeliveryType.POST, DELIVERY_SERVICE_ID);
        returnDelivery.setOwTicketId(OW_TICKET_ID);
        Track track = createTrack(true);
        returnDelivery.setTrack(track);

        when(returnDeliveryDao.findReturnDeliveryById(anyLong()))
                .thenReturn(Optional.of(returnDelivery));
        when(trackingService.findTrackById(anyLong())).thenReturn(Optional.of(track));
        when(ruPostIntegrationClient.getPostDeliveryPrice(anyString())).thenThrow(new RuntimeException());

        owInformerQCProcessor.process(new QueuedCallProcessor.QueuedCallExecution(TRACK_ID,
                null, 3, Instant.now(), TRACK_ID));
        ArgumentCaptor<OwTicketDto> dtoArgumentCaptor = ArgumentCaptor.forClass(OwTicketDto.class);
        verify(owClient).updateTicket(dtoArgumentCaptor.capture(), any());
        OwTicketDto dto = dtoArgumentCaptor.getValue();
        Assertions.assertTrue(dto.getComment().getBody().contains("Получено отправителем"));
    }

    @Test
    public void getPriceFromPostIntegration() {
        ReturnDelivery returnDelivery = ReturnDelivery.newReturnDelivery(DeliveryType.POST, DELIVERY_SERVICE_ID);
        returnDelivery.setId(RETURN_DELIVERY_ID);
        returnDelivery.setOwTicketId(OW_TICKET_ID);
        Track track = createTrack(true);
        returnDelivery.setTrack(track);
        returnDelivery.setReturnId(1L);

        when(returnDeliveryDao.findReturnDeliveryById(anyLong()))
                .thenReturn(Optional.of(returnDelivery));
        when(trackingService.findTrackById(anyLong())).thenReturn(Optional.of(track));
        when(ruPostIntegrationClient.getPostDeliveryPrice(anyString())).thenReturn(BigDecimal.TEN);

        owInformerQCProcessor.process(new QueuedCallProcessor.QueuedCallExecution(TRACK_ID,
                null, 3, Instant.now(), TRACK_ID));

        ArgumentCaptor<String> shipmentCaptor = ArgumentCaptor.forClass(String.class);
        verify(ruPostIntegrationClient).getPostDeliveryPrice(shipmentCaptor.capture());
        assertEquals(TRACK_CODE, shipmentCaptor.getValue());

        ArgumentCaptor<BigDecimal> priceArgumentCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        InOrder inOrder = Mockito.inOrder(returnDeliveryDao);
        inOrder.verify(returnDeliveryDao, times(1)).findReturnDeliveryById(anyLong());
        inOrder.verify(returnDeliveryDao, times(1)).setReturnDeliveryPrice(anyLong(),
                priceArgumentCaptor.capture(), anyString());
        var updatedPrice = priceArgumentCaptor.getValue();
        assertNotNull(updatedPrice);
        assertEquals(BigDecimal.TEN, updatedPrice);
    }


    @Nonnull
    private Track createTrack(boolean withCheckpoints) {
        Track track = new Track();
        track.setId(111L);
        track.setTrackerId(100500L);
        track.setOrderId(123L);
        track.setTrackCode(TRACK_CODE);
        track.setDeliveryServiceId(DELIVERY_SERVICE_ID);
        track.setShipmentId(45L);
        track.setReturnDeliveryId(RETURN_DELIVERY_ID);
        track.setDeliveryServiceType(DeliveryServiceType.RETURN_DELIVERY);
        if (withCheckpoints) {
            TrackCheckpoint checkpoint = new TrackCheckpoint();
            checkpoint.setMessage("Получено отправителем");
            checkpoint.setCheckpointStatus(CheckpointStatus.DELIVERED);
            track.setCheckpoints(List.of(checkpoint));
        }
        return track;
    }
}
