package ru.yandex.market.wms.receiving.service.straight;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.model.enums.ReceiptType;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.entity.ExpectedAndReceivedSku;
import ru.yandex.market.wms.common.spring.dao.entity.Receipt;
import ru.yandex.market.wms.common.spring.dao.entity.Sku;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDetailDao;
import ru.yandex.market.wms.common.spring.service.ReceiptAssortmentService;
import ru.yandex.market.wms.common.spring.service.ReceiptService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

class SurplusServiceTest extends BaseTest {

    private ReceiptDetailDao receiptDetailDao;
    private SurplusService surplusService;
    private ReceiptService receiptService;
    private ReceiptAssortmentService receiptAssortmentService;

    @BeforeEach
    void setUp() {
        receiptDetailDao = mock(ReceiptDetailDao.class);
        receiptService = mock(ReceiptService.class);
        receiptAssortmentService = mock(ReceiptAssortmentService.class);
        surplusService = new SurplusService(receiptDetailDao, receiptService, receiptAssortmentService);
    }

    @AfterEach
    void afterAll() {
        reset(receiptDetailDao);
    }

    @Test
    void getSurplusNoData3p() {
        when(receiptService.getReceiptByKey("123")).thenReturn(Receipt.builder()
                .receiptKey("123")
                .type(ReceiptType.DEFAULT)
                .build());
        when(receiptDetailDao.findSkuBalance("123", SkuId.of("STORER", "SKU")))
                .thenReturn(Optional.empty());
        BigDecimal surplus = surplusService.getSurplus("123", Sku.builder()
                .storerKey("STORER")
                .sku("SKU")
                .build(), BigDecimal.ONE, null);
        assertions.assertThat(surplus.intValue()).isEqualTo(1);
    }

    @Test
    void getSurplusNoData1p() {
        when(receiptService.getReceiptByKey("123")).thenReturn(Receipt.builder()
                .receiptKey("123")
                .type(ReceiptType.DEFAULT)
                .build());
        when(receiptDetailDao.findSkuBalance("123", SkuId.of("465852", "SKU")))
                .thenReturn(Optional.empty());
        BigDecimal surplus = surplusService.getSurplus("123", Sku.builder()
                .storerKey("465852")
                .sku("SKU")
                .build(), BigDecimal.ONE, null);
        assertions.assertThat(surplus.intValue()).isEqualTo(1);
    }

    @Test
    void getSurplus1pExpectedIsZero() {
        when(receiptService.getReceiptByKey("123")).thenReturn(Receipt.builder()
                .receiptKey("123")
                .type(ReceiptType.DEFAULT)
                .build());
        SkuId sku = SkuId.of("465852", "SKU");
        when(receiptDetailDao.findSkuBalance("123", sku))
                .thenReturn(Optional.of(ExpectedAndReceivedSku.builder()
                        .quantityExpected(BigDecimal.ZERO)
                        .quantityReceived(BigDecimal.ONE)
                        .key(ExpectedAndReceivedSku.Key.of(sku))
                        .build()));
        BigDecimal surplus = surplusService.getSurplus("123", Sku.builder()
                .storerKey("465852")
                .sku("SKU")
                .build(), BigDecimal.ONE, null);
        assertions.assertThat(surplus.intValue()).isEqualTo(1);
    }

    @Test
    void getSurplus1p() {
        when(receiptService.getReceiptByKey("123")).thenReturn(Receipt.builder()
                .receiptKey("123")
                .type(ReceiptType.DEFAULT)
                .build());
        SkuId sku = SkuId.of("465852", "SKU");
        when(receiptDetailDao.findSkuBalance("123", sku))
                .thenReturn(Optional.of(ExpectedAndReceivedSku.builder()
                        .quantityExpected(BigDecimal.ONE)
                        .quantityReceived(BigDecimal.ONE)
                        .key(ExpectedAndReceivedSku.Key.of(sku))
                        .build()));
        BigDecimal surplus = surplusService.getSurplus("123", Sku.builder()
                .storerKey("465852")
                .sku("SKU")
                .build(), BigDecimal.ONE, null);
        assertions.assertThat(surplus.intValue()).isEqualTo(1);
    }
}
