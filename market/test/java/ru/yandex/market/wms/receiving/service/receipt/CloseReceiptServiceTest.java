package ru.yandex.market.wms.receiving.service.receipt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.logistics.cte.client.dto.SupplyItemUpdateBatchRequestDTO;
import ru.yandex.market.logistics.cte.client.dto.SupplyItemUpdateRequestDTO;
import ru.yandex.market.wms.common.model.enums.ReceiptStatus;
import ru.yandex.market.wms.common.model.enums.TrailerStatus;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.dao.entity.Receipt;
import ru.yandex.market.wms.common.spring.dao.entity.ReceivedItemIdentity;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDetailUitDao;
import ru.yandex.market.wms.common.spring.exception.ReceiptInWrongStatusException;
import ru.yandex.market.wms.common.spring.service.ReceiptConsolidationTaskCreateService;
import ru.yandex.market.wms.common.spring.service.identities.ItemIdentityService;
import ru.yandex.market.wms.common.spring.servicebus.ServicebusClient;
import ru.yandex.market.wms.receiving.BaseReceivingTest;
import ru.yandex.market.wms.receiving.core.model.response.CloseReceiptResult;
import ru.yandex.market.wms.receiving.core.model.response.PostStatusesEnum;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.wms.common.model.enums.ReceiptStatus.CLOSED;
import static ru.yandex.market.wms.common.model.enums.ReceiptStatus.IN_RECEIVING;
import static ru.yandex.market.wms.common.model.enums.ReceiptStatus.NEW;
import static ru.yandex.market.wms.common.model.enums.ReceiptStatus.PALLET_ACCEPTANCE;
import static ru.yandex.market.wms.common.model.enums.ReceiptStatus.RECEIVED_COMPLETE;
import static ru.yandex.market.wms.common.model.enums.ReceiptStatus.SCHEDULED;
import static ru.yandex.market.wms.common.model.enums.ReceiptType.DEFAULT;
import static ru.yandex.market.wms.common.model.enums.ReceiptType.INTER_WAREHOUSE_DMG;
import static ru.yandex.market.wms.receiving.service.receipt.CloseReceiptService.SUPPLY_ITEMS_ON_RECEIPT_CLOSE;
import static ru.yandex.market.wms.receiving.utils.ReceivingConstants.STATUS_SOURCE;

@ExtendWith(MockitoExtension.class)
class CloseReceiptServiceTest extends BaseReceivingTest {

    @InjectMocks
    private CloseReceiptService closeReceiptService;

    @Mock
    private ServicebusClient servicebusClient;

    @Mock
    private ReceiptDetailUitDao receiptDetailUitDao;

    @Mock
    private DbConfigService configService;

    @Mock
    private ReceiptStateService receiptStateService;

    @Mock
    private ReceiptConsolidationTaskCreateService receiptConsolidationTaskCreateService;

    @Mock
    private ItemIdentityService itemIdentityService;

    @Test
    void closeReceipt() {
        //given
        String receiptKey = "Rcpt1234";
        mockReceipt(receiptKey, IN_RECEIVING);
        mockReceiptDetail(receiptKey, NEW);

        //when
        closeReceiptService.closeReceipts(List.of(receiptKey), CLOSED);

        //then
        verify(receiptDetailDao, times(0))
                .updateStatusWithHistoryForReceipt(eq(receiptKey), eq(CLOSED), eq(STATUS_SOURCE), any());
        verify(receiptService, times(1))
                .countNotClosedReceiptByTrailerKey(eq(receiptKey), eq("trailer"));
        verify(trailerDao, times(1))
                .updateTrailerOnReceiptClose(eq("trailer"), eq(STATUS_SOURCE), eq(TrailerStatus.COMPLETED));
    }

    @Test
    void closeReceiptInPalletAcceptance_shouldFail() {
        //given
        String receiptKey = "Rcpt1234";
        mockReceipt(receiptKey, PALLET_ACCEPTANCE);

        assertThrows(ReceiptInWrongStatusException.class,
                () -> closeReceiptService.closeReceipts(List.of(receiptKey), CLOSED));
    }

    @Test
    void closeReceiptScheduled_shouldFail() {
        //given
        String receiptKey = "Rcpt1234";
        mockReceipt(receiptKey, SCHEDULED);

        //when
        assertThrows(ReceiptInWrongStatusException.class,
                () -> closeReceiptService.closeReceipts(List.of(receiptKey), CLOSED));
    }

    @Test
    void closeReceiptClosed_shouldFail() {
        //given
        String receiptKey = "Rcpt1234";
        mockReceipt(receiptKey, CLOSED);

        //when
        assertThrows(ReceiptInWrongStatusException.class,
                () -> closeReceiptService.closeReceipts(List.of(receiptKey), CLOSED));
    }

    @Test
    void closeReceiptToNew_shouldFail() {
        //given
        String receiptKey = "Rcpt1234";

        //when
        assertThrows(IllegalArgumentException.class,
                () -> closeReceiptService.closeReceipts(List.of(receiptKey), NEW));
    }

    @Test
    void closeReceipt_validateDetailsStatus() {
        //given
        String receiptKey = "Rcpt1234";
        mockReceipt(receiptKey, IN_RECEIVING);
        mockReceiptDetail(receiptKey, NEW);
        when(receiptDetailDao.getReceiptDetailsReceived(receiptKey)).thenReturn(
                List.of(receiptDetail(receiptKey, 1, RECEIVED_COMPLETE, 0.5f),
                        receiptDetail(receiptKey, 3, NEW, 2)
                )
        );

        //when
        List<CloseReceiptResult> closeReceiptResults = closeReceiptService.closeReceipts(List.of(receiptKey), CLOSED);

        //then
        assertThat("OK result", closeReceiptResults.get(0).getStatus(), equalTo(PostStatusesEnum.OK));
    }

    @Test
    void updateResupplyItems() {
        String receiptKey = "1234";
        Receipt receipt = Receipt.builder()
                .receiptKey(receiptKey)
                .externReceiptKey("4321" + receiptKey)
                .status(IN_RECEIVING)
                .type(INTER_WAREHOUSE_DMG) // For update
                .build();

        when(receiptDao.findReceiptByKey(receiptKey)).thenReturn(Optional.ofNullable(receipt));
        when(configService.getConfigAsBoolean(SUPPLY_ITEMS_ON_RECEIPT_CLOSE, false)).thenReturn(true);
        when(itemIdentityService.getReceivedItemsPerUit(receiptKey)).thenReturn(
                Map.of("uit", List.of(ReceivedItemIdentity.builder().uuid("uuid").uit("uit").build())));
        closeReceiptService.updateResupplyItems(receiptKey);

        ArgumentCaptor<SupplyItemUpdateBatchRequestDTO> requestCaptor =
                ArgumentCaptor.forClass(SupplyItemUpdateBatchRequestDTO.class);

        SupplyItemUpdateBatchRequestDTO expectedRequest = new SupplyItemUpdateBatchRequestDTO(43211234L,
                List.of(new SupplyItemUpdateRequestDTO("uuid", "uit")));
        verify(servicebusClient, times(1)).updateResupplyItems(requestCaptor.capture());
        Assertions.assertEquals(expectedRequest, requestCaptor.getValue());
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
}
