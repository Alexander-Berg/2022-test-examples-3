package ru.yandex.market.wms.common.spring.service.unit;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.entity.ReceiptDetail;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.implementation.AltSkuDao;
import ru.yandex.market.wms.common.spring.dao.implementation.BomSkuDao;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDetailDao;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDetailStatusHistoryDao;
import ru.yandex.market.wms.common.spring.pojo.ReceiptDetailCopyParams;
import ru.yandex.market.wms.common.spring.pojo.ReceiptDetailKey;
import ru.yandex.market.wms.common.spring.service.LotService;
import ru.yandex.market.wms.common.spring.service.ReceiptAssortmentService;
import ru.yandex.market.wms.common.spring.service.ReceiptService;
import ru.yandex.market.wms.common.spring.service.receiptDetails.ReceiptDetailService;
import ru.yandex.market.wms.common.spring.utils.CisParser;
import ru.yandex.market.wms.receiving.service.ReceiptDetailItemService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ReceiptDetailServiceTest extends BaseTest {

    private static final ReceiptDetailCopyParams COPY_PARAMS =
            ReceiptDetailCopyParams.builder()
                    .addWho("TEST1")
                    .newReceiptDetailKey(new ReceiptDetailKey("0000011111", "00123"))
                    .originalReceiptDetail(ReceiptDetail.builder()
                            .serialKey("100")
                            .skuId(new SkuId("465852", "sku1"))
                            .quantityExpected(BigDecimal.TEN)
                            .quantityReceived(BigDecimal.ZERO)
                            .receiptDetailKey(new ReceiptDetailKey("0000011111", "00012"))
                            .build())
                    .build();

    private ReceiptDetailService service;
    private ReceiptService receiptService;
    private ReceiptAssortmentService receiptAssortmentService;
    private ReceiptDetailDao receiptDetailDao;
    private ReceiptDetailItemService receiptDetailItemService;

    @BeforeEach
    public void setup() {
        super.setup();
        receiptService = mock(ReceiptService.class);
        receiptAssortmentService = mock(ReceiptAssortmentService.class);
        receiptDetailDao = mock(ReceiptDetailDao.class);
        receiptDetailItemService = mock(ReceiptDetailItemService.class);
        ReceiptDetailStatusHistoryDao receiptDetailStatusHistoryDao = mock(ReceiptDetailStatusHistoryDao.class);
        AltSkuDao altSkuDao = mock(AltSkuDao.class);
        BomSkuDao bomSkuDao = mock(BomSkuDao.class);
        Clock clock = Clock.fixed(Instant.parse("2020-04-01T12:34:56.789Z"), ZoneOffset.UTC);
        CisParser cisParser = mock(CisParser.class);
        DbConfigService dbConfigService = mock(DbConfigService.class);
        LotService lotService = mock(LotService.class);
        service = new ReceiptDetailService(receiptService, receiptAssortmentService, receiptDetailDao,
                receiptDetailStatusHistoryDao, altSkuDao, bomSkuDao, clock, cisParser, dbConfigService,
                receiptDetailItemService, lotService);
    }

    @AfterEach
    public void resetMocks() {
        reset(receiptService, receiptDetailDao);
    }

    @Test
    public void copyReceiptDetailWhenReceiptIsPresent() {
        String receiptKey = "0000011111";
        String nextReceiptLineNumber = "00123";
        ReceiptDetailKey newReceiptDetailKey = new ReceiptDetailKey(receiptKey, nextReceiptLineNumber);
        when(receiptDetailDao.findByKey(newReceiptDetailKey))
                .thenReturn(Optional.of(ReceiptDetail.builder().build()));
        service.copyReceiptDetail(COPY_PARAMS);
        verify(receiptService).lockReceipt(receiptKey);
        verify(receiptDetailDao).copyReceiptDetail(
                "WMS" + nextReceiptLineNumber, COPY_PARAMS);
        verify(receiptService).validateReceiptNotInFinalStatus(receiptKey);
        verify(receiptDetailDao).findByKey(newReceiptDetailKey);
        verifyNoMoreInteractions(receiptService, receiptDetailDao);
    }
}
