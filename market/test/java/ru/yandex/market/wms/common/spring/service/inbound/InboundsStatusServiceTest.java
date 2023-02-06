package ru.yandex.market.wms.common.spring.service.inbound;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.model.common.StatusCode;
import ru.yandex.market.logistic.api.model.fulfillment.exception.FulfillmentApiException;
import ru.yandex.market.wms.common.model.enums.ReceiptStatus;
import ru.yandex.market.wms.common.model.enums.TrailerStatus;
import ru.yandex.market.wms.common.spring.dto.inbound.InboundStatus;
import ru.yandex.market.wms.common.spring.dto.inbound.InboundStatusHistory;
import ru.yandex.market.wms.common.spring.model.entity.ReceiptStatusEntity;
import ru.yandex.market.wms.common.spring.model.entity.TrailerStatusEntity;
import ru.yandex.market.wms.common.spring.repository.ReceiptStatusRepository;
import ru.yandex.market.wms.common.spring.repository.TrailerStatusRepository;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
class InboundsStatusServiceTest {

    private final ReceiptStatusToInboundStatusConverter inboundStatusConverter =
            new ReceiptStatusToInboundStatusConverter();
    private final TrailerStatusRepository trailerStatusRepository = mock(TrailerStatusRepository.class);
    private final ReceiptStatusRepository receiptStatusRepository = mock(ReceiptStatusRepository.class);
    private final InboundsStatusService service =
            new InboundsStatusService(trailerStatusRepository, receiptStatusRepository, inboundStatusConverter);

    @Test
    void getInboundsStatus() {
        //given
        List<ResourceId> resourceIdList = resourceIdList(4);
        when(receiptStatusRepository.findLastStatusesByReceiptKeys(any())).thenReturn(receiptStatusEntities(
                ReceiptStatus.NEW, ReceiptStatus.IN_RECEIVING, ReceiptStatus.CLOSED, ReceiptStatus.VERIFIED_CLOSED
        ));
        when(trailerStatusRepository.findClosingStatusesByReceiptKeys(any())).thenReturn(toMap(trailerStatusEntities(
                TrailerStatus.COMPLETED, TrailerStatus.COMPLETED, TrailerStatus.COMPLETED, TrailerStatus.COMPLETED)
        ));

        //when
        List<InboundStatus> inboundsStatus = service.getInboundsStatus(resourceIdList);

        //then
        assertEquals(inboundsStatus.size(), 4);
        assertEquals(inboundsStatus.get(0).getStatus().getStatusCode(), StatusCode.ARRIVED);
        assertEquals(inboundsStatus.get(0).getStatus().getMessage(), "receiptKey_0");
        assertEquals(inboundsStatus.get(0).getInboundId().getYandexId(), "yandex0");
        assertEquals(inboundsStatus.get(0).getInboundId().getPartnerId(), "receiptKey_0");
    }

    @Test
    void getInboundsStatusEmpty() {
        //given
        List<ResourceId> resourceIdList = emptyList();

        //when
        List<InboundStatus> inboundsStatus = service.getInboundsStatus(resourceIdList);

        //then
        assertIterableEquals(inboundsStatus, emptyList());
    }

    @Test
    void getInboundsStatusExceedLength() {
        //given
        List<ResourceId> resourceIdList = resourceIdList(101);

        //when
        Exception exception = assertThrows(FulfillmentApiException.class, () ->
                service.getInboundsStatus(resourceIdList));

        //then
        assertTrue(exception.getMessage().contains("Max size of id's list is 100."));
    }

    @Test
    void getInboundsStatusWithoutTrailerStatus() {
        //given
        List<ResourceId> resourceIdList = resourceIdList(4);
        when(receiptStatusRepository.findLastStatusesByReceiptKeys(any())).thenReturn(receiptStatusEntities(
                ReceiptStatus.NEW, ReceiptStatus.IN_RECEIVING, ReceiptStatus.CLOSED, ReceiptStatus.VERIFIED_CLOSED
        ));
        when(trailerStatusRepository.findClosingStatusesByReceiptKeys(any())).thenReturn(emptyMap());

        //when
        List<InboundStatus> inboundsStatus = service.getInboundsStatus(resourceIdList);

        //then
        assertEquals(inboundsStatus.size(), 4);
        assertEquals(inboundsStatus.get(0).getStatus().getStatusCode(), StatusCode.CREATED);
        assertEquals(inboundsStatus.get(0).getStatus().getMessage(), "receiptKey_0");
        assertEquals(inboundsStatus.get(0).getInboundId().getYandexId(), "yandex0");
        assertEquals(inboundsStatus.get(0).getInboundId().getPartnerId(), "receiptKey_0");
        assertEquals(inboundsStatus.get(3).getStatus().getStatusCode(), StatusCode.ACCEPTED);
    }

    @Test
    void getInboundsStatusTrailerStatusUnknown() {
        //given
        List<ResourceId> resourceIdList = resourceIdList(1);
        when(receiptStatusRepository.findLastStatusesByReceiptKeys(any())).thenReturn(receiptStatusEntities(
                ReceiptStatus.IN_RECEIVING
        ));
        when(trailerStatusRepository.findClosingStatusesByReceiptKeys(any()))
                .thenReturn(toMap(trailerStatusEntities(TrailerStatus.DOCKED)));

        //when
        List<InboundStatus> inboundsStatus = service.getInboundsStatus(resourceIdList);

        //then
        assertEquals(inboundsStatus.size(), 1);
        assertEquals(inboundsStatus.get(0).getStatus().getStatusCode(), StatusCode.ACCEPTANCE);
    }

    @Test
    void getInboundsStatusEmptyReceiptList() {
        //given
        List<ResourceId> resourceIdList = resourceIdList(1);
        when(receiptStatusRepository.findLastStatusesByReceiptKeys(any())).thenReturn(emptyList());
        when(trailerStatusRepository.findClosingStatusesByReceiptKeys(any()))
                .thenReturn(toMap(trailerStatusEntities(TrailerStatus.DOCKED)));

        //when
        List<InboundStatus> inboundsStatus = service.getInboundsStatus(resourceIdList);

        //then
        assertEquals(inboundsStatus.size(), 0);
    }

    @Test
    void getInboundHistory() {
        //given
        ResourceId resourceId = new ResourceId("yandexId", "receipt");
        when(receiptStatusRepository.findUniqueStatusesByReceiptKey("receipt")).thenReturn(receiptStatusEntities(
                "receipt", ReceiptStatus.NEW, ReceiptStatus.CANCELLED, ReceiptStatus.CLOSED)
        );
        when(trailerStatusRepository.findLastStatusByReceiptKey(any())).thenReturn(Optional.empty());

        //when
        InboundStatusHistory inboundHistory = service.getInboundHistory(resourceId);

        //then
        assertEquals(inboundHistory.getHistory().size(), 3);
        assertEquals(inboundHistory.getInboundId().getPartnerId(), "receipt");
        assertEquals(inboundHistory.getInboundId().getYandexId(), "yandexId");
        assertEquals(inboundHistory.getHistory().get(0).getMessage(), "receipt");
        assertEquals(inboundHistory.getHistory().get(2).getStatusCode(), StatusCode.CREATED);
        assertEquals(inboundHistory.getHistory().get(1).getStatusCode(), StatusCode.CANCELLED);
        assertEquals(inboundHistory.getHistory().get(0).getStatusCode(), StatusCode.ACCEPTANCE);
    }

    @Test
    void getInboundHistoryDuplicate() {
        //given
        ResourceId resourceId = new ResourceId("yandexId", "receipt");
        when(receiptStatusRepository.findUniqueStatusesByReceiptKey("receipt")).thenReturn(receiptStatusEntities(
                "receipt", ReceiptStatus.NEW, ReceiptStatus.CANCELLED, ReceiptStatus.CLOSED,
                ReceiptStatus.CANCELLED, ReceiptStatus.CLOSED)
        );
        when(trailerStatusRepository.findLastStatusByReceiptKey(any())).thenReturn(Optional.empty());

        //when
        InboundStatusHistory inboundHistory = service.getInboundHistory(resourceId);

        //then
        assertEquals(inboundHistory.getHistory().size(), 3);
        assertEquals(inboundHistory.getInboundId().getPartnerId(), "receipt");
        assertEquals(inboundHistory.getInboundId().getYandexId(), "yandexId");
        assertEquals(inboundHistory.getHistory().get(0).getMessage(), "receipt");
        assertEquals(inboundHistory.getHistory().get(2).getStatusCode(), StatusCode.CREATED);
        assertEquals(inboundHistory.getHistory().get(1).getStatusCode(), StatusCode.CANCELLED);
        assertEquals(inboundHistory.getHistory().get(0).getStatusCode(), StatusCode.ACCEPTANCE);
    }

    @Test
    @Disabled(value = "Тест некорректный, правильно покрыта ситуация в тесте контроллера")
    void getInboundHistoryTrailer() {
        //given
        ResourceId resourceId = new ResourceId("yandexId", "receipt");
        when(receiptStatusRepository.findUniqueStatusesByReceiptKey("receipt")).thenReturn(receiptStatusEntities(
                "receipt", ReceiptStatus.NEW, ReceiptStatus.CANCELLED, ReceiptStatus.CLOSED,
                ReceiptStatus.CANCELLED, ReceiptStatus.CLOSED)
        );
        when(trailerStatusRepository.findLastStatusByReceiptKey(any())).thenReturn(Optional.of(trailerStatusEntities(
                TrailerStatus.COMPLETED
        ).get(0)));

        //when
        InboundStatusHistory inboundHistory = service.getInboundHistory(resourceId);

        //then
        assertEquals(inboundHistory.getHistory().size(), 4);
        assertEquals(inboundHistory.getInboundId().getPartnerId(), "receipt");
        assertEquals(inboundHistory.getInboundId().getYandexId(), "yandexId");
        assertEquals(inboundHistory.getHistory().get(0).getMessage(), "receiptKey_0");
        assertEquals(inboundHistory.getHistory().get(3).getStatusCode(), StatusCode.CREATED);
        assertEquals(inboundHistory.getHistory().get(2).getStatusCode(), StatusCode.CANCELLED);
        assertEquals(inboundHistory.getHistory().get(1).getStatusCode(), StatusCode.ACCEPTANCE);
        assertEquals(inboundHistory.getHistory().get(0).getStatusCode(), StatusCode.ARRIVED);
    }

    @Test
    void getInboundHistoryTrailerNotCompleted() {
        //given
        ResourceId resourceId = new ResourceId("yandexId", "receipt");
        when(receiptStatusRepository.findUniqueStatusesByReceiptKey("receipt")).thenReturn(receiptStatusEntities(
                "receipt", ReceiptStatus.NEW, ReceiptStatus.CANCELLED, ReceiptStatus.CLOSED,
                ReceiptStatus.CANCELLED, ReceiptStatus.CLOSED)
        );
        when(trailerStatusRepository.findLastStatusByReceiptKey(any())).thenReturn(Optional.of(trailerStatusEntities(
                TrailerStatus.DOCKED
        ).get(0)));

        //when
        InboundStatusHistory inboundHistory = service.getInboundHistory(resourceId);

        //then
        assertEquals(3, inboundHistory.getHistory().size());
        assertEquals("receipt", inboundHistory.getInboundId().getPartnerId());
        assertEquals("yandexId", inboundHistory.getInboundId().getYandexId());
        assertEquals("receipt", inboundHistory.getHistory().get(0).getMessage());
        assertEquals(StatusCode.CREATED, inboundHistory.getHistory().get(2).getStatusCode());
        assertEquals(StatusCode.CANCELLED, inboundHistory.getHistory().get(1).getStatusCode());
        assertEquals(StatusCode.ACCEPTANCE, inboundHistory.getHistory().get(0).getStatusCode());
    }


    private List<ResourceId> resourceIdList(int quantity) {
        List<ResourceId> list = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            list.add(
                    ResourceId.builder()
                            .setPartnerId("receiptKey_" + i)
                            .setYandexId("yandex" + i)
                            .build()
            );
        }
        return list;
    }

    private List<ReceiptStatusEntity> receiptStatusEntities(ReceiptStatus... statuses) {
        List<ReceiptStatusEntity> list = new ArrayList<>();
        int i = 0;
        for (ReceiptStatus status : statuses) {
            ReceiptStatusEntity entity = new ReceiptStatusEntity();
            entity.setAddDate(LocalDateTime.now());
            entity.setReceiptKey("receiptKey_" + i);
            entity.setStatus(status);
            list.add(entity);
            i++;
        }
        return list;
    }

    private List<ReceiptStatusEntity> receiptStatusEntities(String receipt, ReceiptStatus... statuses) {
        List<ReceiptStatusEntity> list = new ArrayList<>();
        for (ReceiptStatus status : statuses) {
            ReceiptStatusEntity entity = new ReceiptStatusEntity();
            entity.setAddDate(LocalDateTime.now());
            entity.setReceiptKey(receipt);
            entity.setStatus(status);
            list.add(entity);
        }
        return list;
    }

    private List<TrailerStatusEntity> trailerStatusEntities(TrailerStatus... statuses) {
        List<TrailerStatusEntity> list = new ArrayList<>();
        int i = 0;
        for (TrailerStatus status : statuses) {
            TrailerStatusEntity trailerStatus = new TrailerStatusEntity();
            trailerStatus.setStatus(status);
            trailerStatus.setAddDate(LocalDateTime.now());
            trailerStatus.setReceiptKey("receiptKey_" + i);
            trailerStatus.setTrailerKey("trailertKey" + status.getCode());
            list.add(trailerStatus);
            i++;
        }
        return list;
    }

    private Map<String, TrailerStatusEntity> toMap(List<TrailerStatusEntity> list) {
        return list.stream()
                .collect(Collectors
                        .toMap(TrailerStatusEntity::getReceiptKey, trailerStatus -> trailerStatus));
    }
}
