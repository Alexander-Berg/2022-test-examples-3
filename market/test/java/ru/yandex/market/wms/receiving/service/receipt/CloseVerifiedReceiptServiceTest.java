package ru.yandex.market.wms.receiving.service.receipt;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.wms.common.model.enums.AnomalyLotStatus;
import ru.yandex.market.wms.common.model.enums.LocationType;
import ru.yandex.market.wms.common.model.enums.ReceiptStatus;
import ru.yandex.market.wms.common.spring.dao.entity.Receipt;
import ru.yandex.market.wms.common.spring.exception.BadRequestException;
import ru.yandex.market.wms.common.spring.service.ReceiptConsolidationTaskCreateService;
import ru.yandex.market.wms.common.spring.service.SkuService;
import ru.yandex.market.wms.common.spring.service.receiptDetails.ReceiptDetailService;
import ru.yandex.market.wms.receiving.BaseReceivingTest;
import ru.yandex.market.wms.receiving.core.model.response.CloseReceiptResult;
import ru.yandex.market.wms.receiving.core.model.response.PostStatusesEnum;
import ru.yandex.market.wms.receiving.dao.entity.AnomalyLotLocType;
import ru.yandex.market.wms.receiving.service.returns.BomSkuService;
import ru.yandex.market.wms.receiving.service.straight.AnomalyLotService;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.wms.common.model.enums.AnomalyLotStatus.CONSOLIDATED;
import static ru.yandex.market.wms.common.model.enums.AnomalyLotStatus.RE_RECEIVING;
import static ru.yandex.market.wms.common.model.enums.LocationType.REJECT_BUF;
import static ru.yandex.market.wms.common.model.enums.LocationType.REJECT_STORE;
import static ru.yandex.market.wms.common.model.enums.ReceiptStatus.CLOSED;
import static ru.yandex.market.wms.common.model.enums.ReceiptStatus.CLOSED_WITH_DISCREPANCIES;
import static ru.yandex.market.wms.common.model.enums.ReceiptStatus.IN_RECEIVING;
import static ru.yandex.market.wms.common.model.enums.ReceiptStatus.IN_TRANSIT;
import static ru.yandex.market.wms.common.model.enums.ReceiptStatus.NEW;
import static ru.yandex.market.wms.common.model.enums.ReceiptStatus.RECEIVED_COMPLETE;
import static ru.yandex.market.wms.common.model.enums.ReceiptStatus.SCHEDULED;
import static ru.yandex.market.wms.common.model.enums.ReceiptStatus.VERIFIED_CLOSED;
import static ru.yandex.market.wms.common.model.enums.ReceiptType.DEFAULT;

@ExtendWith(MockitoExtension.class)
public class CloseVerifiedReceiptServiceTest extends BaseReceivingTest {

    @Mock
    private SkuService skuService;

    @Mock
    private BomSkuService bomSkuService;

    @Mock
    private AnomalyLotService anomalyLotService;

    @Mock
    private ReceiptDetailService receiptDetailService;

    @Mock
    private ReceiptConsolidationTaskCreateService receiptConsolidationTaskCreateService;

    @Mock
    private ReceiptStateService receiptStateService;

    @InjectMocks
    private CloseReceiptService closeReceiptService;

    @Test
    public void closeVerificationReceiptWithVerification() {
        //given
        String receiptKey = "Rcpt1234";
        mockReceipt(receiptKey, CLOSED);
        mockReceiptDetail(receiptKey, CLOSED);
        when(appProperties.getAnomalyStorageZones()).thenReturn(Set.of());

        //when
        closeReceiptService.closeReceipts(List.of(receiptKey), VERIFIED_CLOSED);

        //then
        verify(anomalyNotificationService, times(1)).sendAnomalyNotification(any(Receipt.class));
    }

    @Test
    public void closeVerificationReceiptWithNotApplicableStatus() {
        //given
        String receiptKey = "Rcpt1234";
        mockReceipt(receiptKey, NEW);

        //when
        assertThrows(BadRequestException.class,
                () -> closeReceiptService.closeReceipts(List.of(receiptKey), VERIFIED_CLOSED));

        //then
        verify(anomalyNotificationService, times(0)).sendAnomalyNotification(any(Receipt.class));
    }

    @Test
    public void closeVerificationReceiptWithNotCompletedConsolidationTasks() {
        //given
        String receiptKey = "Rcpt1234";
        mockReceipt(receiptKey, CLOSED);
        mockReceiptDetail(receiptKey, CLOSED);
        when(taskDetailDao.getQuantityAnomalyConsolidationTasks(receiptKey)).thenReturn(1);

        //when
        List<CloseReceiptResult> closeReceiptResult =
                closeReceiptService.closeReceipts(List.of(receiptKey), VERIFIED_CLOSED);

        //then
        assertThat("Close result message", closeReceiptResult.get(0).getMessage(),
                equalTo("В поставке " + receiptKey + " остались незавершенные задания"));
        assertThat("Close result status", closeReceiptResult.get(0).getStatus(),
                equalTo(PostStatusesEnum.NOK));
        assertThat("Close result receiptKey", closeReceiptResult.get(0).getReceiptKey(),
                equalTo(receiptKey));
        verify(anomalyNotificationService, times(0)).sendAnomalyNotification(any(Receipt.class));
    }

    @Test
    public void closeVerificationReceiptWithNotConsolidatedAnomalyTare() {
        //given
        String receiptKey = "Rcpt1234";
        mockReceipt(receiptKey, CLOSED_WITH_DISCREPANCIES);
        mockReceiptDetail(receiptKey, CLOSED_WITH_DISCREPANCIES);
        when(anomalyLotLocTypeDao.getAnomalyContainersLocType(receiptKey)).thenReturn(
                List.of(anomalyLotLocType(1, CONSOLIDATED), anomalyLotLocType(2, RE_RECEIVING))
        );

        //when
        List<CloseReceiptResult> closeReceiptResult =
                closeReceiptService.closeReceipts(List.of(receiptKey), VERIFIED_CLOSED);

        //then
        assertThat("Message of exception", closeReceiptResult.get(0).getMessage(),
                equalTo("В поставке " + receiptKey +
                        " остались тары с аномалиями, не прошедшие консолидацию"));
        verify(anomalyNotificationService, times(0)).sendAnomalyNotification(any(Receipt.class));
    }

    @Test
    public void closeVerificationReceiptWithReceiptDetailsStatus_complete() {
        //given
        String receiptKey = "Rcpt1234";
        mockReceipt(receiptKey, CLOSED_WITH_DISCREPANCIES);
        mockReceiptDetail(receiptKey, NEW);
        when(anomalyLotLocTypeDao.getAnomalyContainersLocType(receiptKey)).thenReturn(
                List.of(anomalyLotLocType(1, CONSOLIDATED), anomalyLotLocType(2, CONSOLIDATED))
        );
        when(receiptDetailDao.getReceiptDetailsReceived(receiptKey)).thenReturn(
                List.of(receiptDetail(receiptKey, 1, RECEIVED_COMPLETE), receiptDetail(receiptKey, 2, IN_RECEIVING))
        );

        //when
        List<CloseReceiptResult> closeReceiptResult =
                closeReceiptService.closeReceipts(List.of(receiptKey), VERIFIED_CLOSED);

        //then
        assertThat("Message of exception", closeReceiptResult.get(0).getMessage(),
                equalTo("Нельзя закрыть приемку, недопустимые статусы деталей поставки: "
                        + receiptKey + " 00002 "
                        + IN_RECEIVING.getTitle()));
        verify(anomalyNotificationService, times(0)).sendAnomalyNotification(any(Receipt.class));
    }

    @Test
    public void closeVerificationReceiptWithReceiptDetailsStatus_new() {
        //given
        String receiptKey = "Rcpt1234";
        mockReceipt(receiptKey, CLOSED);
        mockReceiptDetail(receiptKey, CLOSED);
        when(anomalyLotLocTypeDao.getAnomalyContainersLocType(receiptKey)).thenReturn(
                List.of(anomalyLotLocType(1, CONSOLIDATED), anomalyLotLocType(2, CONSOLIDATED))
        );
        when(receiptDetailDao.getReceiptDetailsReceived(receiptKey)).thenReturn(
                List.of(receiptDetail(receiptKey, 1, RECEIVED_COMPLETE), receiptDetail(receiptKey, 2, IN_TRANSIT))
        );

        //when
        List<CloseReceiptResult> closeReceiptResult =
                closeReceiptService.closeReceipts(List.of(receiptKey), VERIFIED_CLOSED);

        //then
        assertThat("Message of exception", closeReceiptResult.get(0).getMessage(),
                equalTo("Нельзя закрыть приемку, недопустимые статусы деталей поставки: "
                        + receiptKey + " 00002 "
                        + IN_TRANSIT.getTitle()));
        verify(anomalyNotificationService, times(0)).sendAnomalyNotification(any(Receipt.class));
    }

    @Test
    public void closeVerificationReceiptWithReceiptDetailsStatus_cancelled() {
        //given
        String receiptKey = "Rcpt1234";
        mockReceipt(receiptKey, CLOSED);
        mockReceiptDetail(receiptKey, CLOSED);
        when(anomalyLotLocTypeDao.getAnomalyContainersLocType(receiptKey)).thenReturn(
                List.of(anomalyLotLocType(1, CONSOLIDATED), anomalyLotLocType(2, CONSOLIDATED))
        );
        when(receiptDetailDao.getReceiptDetailsReceived(receiptKey)).thenReturn(
                List.of(receiptDetail(receiptKey, 2, SCHEDULED))
        );

        //when
        List<CloseReceiptResult> closeReceiptResult =
                closeReceiptService.closeReceipts(List.of(receiptKey), VERIFIED_CLOSED);

        //then
        assertThat("Message of exception", closeReceiptResult.get(0).getMessage(),
                equalTo("Нельзя закрыть приемку, недопустимые статусы деталей поставки: "
                        + receiptKey + " 00002 "
                        + SCHEDULED.getTitle()));
        verify(anomalyNotificationService, times(0)).sendAnomalyNotification(any(Receipt.class));
    }

    @Test
    public void createTransportOrdersForAnomalyContainers() {
        //given
        String receiptKey = "Rcpt1234";
        mockReceipt(receiptKey, CLOSED);
        mockReceiptDetail(receiptKey, CLOSED);
        when(anomalyLotLocTypeDao.getAnomalyContainersLocType(receiptKey)).thenReturn(
                List.of(anomalyLotLocType(1, CONSOLIDATED, REJECT_BUF),
                        anomalyLotLocType(2, CONSOLIDATED, REJECT_BUF),
                        anomalyLotLocType(3, CONSOLIDATED, REJECT_STORE),
                        anomalyLotLocType(4, CONSOLIDATED, REJECT_BUF),
                        anomalyLotLocType(5, CONSOLIDATED, LocationType.IN_TRANSIT)
                )
        );
        when(receiptDetailDao.getReceiptDetailsReceived(receiptKey)).thenReturn(
                List.of(receiptDetail(receiptKey, 1, RECEIVED_COMPLETE), receiptDetail(receiptKey, 2,
                                RECEIVED_COMPLETE),
                        receiptDetail(receiptKey, 3, NEW), receiptDetail(receiptKey, 4, NEW))
        );
        when(appProperties.getAnomalyStorageZones()).thenReturn(Set.of("zone1", "zone2"));

        //when
        closeReceiptService.closeReceipts(List.of(receiptKey), VERIFIED_CLOSED);

        //then
        verify(transportationClient, times(3)).createTransportOrder(any());
        verify(anomalyNotificationService, times(1)).sendAnomalyNotification(any(Receipt.class));
    }

    @Test
    public void closeWithVerification() {
        //given
        String receiptKey = "Rcpt1234";
        mockReceipt(receiptKey, CLOSED);
        mockReceiptDetail(receiptKey, CLOSED);
        when(anomalyLotLocTypeDao.getAnomalyContainersLocType(receiptKey)).thenReturn(
                List.of(anomalyLotLocType(1, CONSOLIDATED, REJECT_BUF),
                        anomalyLotLocType(2, CONSOLIDATED, REJECT_BUF),
                        anomalyLotLocType(3, CONSOLIDATED, REJECT_STORE),
                        anomalyLotLocType(4, CONSOLIDATED, REJECT_BUF),
                        anomalyLotLocType(5, CONSOLIDATED, LocationType.IN_TRANSIT)
                )
        );
        when(appProperties.getAnomalyStorageZones()).thenReturn(Set.of("zone1", "zone2"));

        //when
        closeReceiptService.closeReceipts(List.of(receiptKey), VERIFIED_CLOSED);

        //then
        verify(receiptDetailDao, times(0))
                .updateStatusWithHistoryForReceipt(eq(receiptKey), eq(VERIFIED_CLOSED), any(), any());
    }

    protected void mockReceipt(String receiptKey, ReceiptStatus receiptStatus) {
        Receipt receipt = receipt(receiptKey, receiptStatus);
        when(receiptService.getReceiptByKey(receiptKey)).thenReturn(receipt);
    }

    protected Receipt receipt(String receiptKey, ReceiptStatus receiptStatus) {
        return Receipt.builder()
                .receiptKey(receiptKey)
                .externReceiptKey("ext" + receiptKey)
                .trailerKey("trailer")
                .receiptDate(Instant.now())
                .trailerNumber("trailerNumber")
                .storer("storer")
                .status(receiptStatus)
                .type(DEFAULT)
                .supplier("supplier")
                .build();
    }

    private void mockReceiptDetail(String receiptKey, ReceiptStatus receiptStatus) {
        when(receiptDetailDao.getReceiptDetailsItems(receiptKey)).thenReturn(
                List.of(receiptDetail(receiptKey, 3, receiptStatus, 2))
        );
    }

    protected AnomalyLotLocType anomalyLotLocType(int i, AnomalyLotStatus status) {
        return anomalyLotLocType(i, status, LocationType.ANO_CONSOLIDATION);
    }

    protected AnomalyLotLocType anomalyLotLocType(int i, AnomalyLotStatus status, LocationType locationType) {
        AnomalyLotLocType anomalyLotLocType = new AnomalyLotLocType();
        anomalyLotLocType.setContainerId("container" + i);
        anomalyLotLocType.setLoc("loc" + i);
        anomalyLotLocType.setLocType(locationType.getCode());
        anomalyLotLocType.setStatus(status.name());
        return anomalyLotLocType;
    }

}
