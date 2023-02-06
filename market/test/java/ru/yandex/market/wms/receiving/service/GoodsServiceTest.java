package ru.yandex.market.wms.receiving.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.cte.client.FulfillmentCteClientWebClient;
import ru.yandex.market.logistics.cte.client.dto.SupplyItemDTO;
import ru.yandex.market.logistics.cte.client.dto.SupplyItemRequestDTO;
import ru.yandex.market.logistics.cte.client.enums.RegistryType;
import ru.yandex.market.logistics.cte.client.enums.StockType;
import ru.yandex.market.wms.common.model.enums.InventoryHoldStatus;
import ru.yandex.market.wms.common.model.enums.ReceiptType;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.entity.Receipt;
import ru.yandex.market.wms.common.spring.dao.entity.ReceiptDetail;
import ru.yandex.market.wms.common.spring.dao.entity.Sku;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDao;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDetailDao;
import ru.yandex.market.wms.common.spring.exception.ReceiptDetailsNotFoundException;
import ru.yandex.market.wms.common.spring.pojo.ReceiptDetailKey;
import ru.yandex.market.wms.common.spring.service.SkuService;
import ru.yandex.market.wms.receiving.model.converter.StockTypeConverter;
import ru.yandex.market.wms.receiving.service.returns.GoodsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoodsServiceTest extends BaseTest {

    public static final String RECEIPT_KEY = "123";
    public static final String LINE_NUMBER = "001";
    public static final String SKU = "ROV";
    public static final String STORER_KEY = "100500";
    public static final String EXTERN_RECEIPT_KEY = "12345";
    public static final SkuId SKU_ID = new SkuId(STORER_KEY, SKU);
    public static final String MANUFACTURER_SKU = "sku.sku";
    public static final BigDecimal PRICE = BigDecimal.valueOf(10.5);
    public static final String UUID = "uuid2";
    private static final String CATEGORY_ID = "1";
    private ReceiptDao receiptDao;
    private GoodsService goodsService;
    private FulfillmentCteClientWebClient cteClient;
    private ReceiptDetailDao receiptDetailDao;
    private SkuService skuService;
    private DbConfigService configService;

    @BeforeEach
    public void init() {
        StockTypeConverter stockTypeConverter = new StockTypeConverter();
        receiptDetailDao = mock(ReceiptDetailDao.class);
        cteClient = mock(FulfillmentCteClientWebClient.class);
        receiptDao = mock(ReceiptDao.class);
        skuService = mock(SkuService.class);
        configService = mock(DbConfigService.class);
        goodsService = new GoodsService("100", receiptDetailDao, cteClient, stockTypeConverter, skuService,
                receiptDao, configService);

        when(skuService.getSku(SKU_ID))
                .thenReturn(Sku.builder()
                        .manufacturerSku(MANUFACTURER_SKU)
                        .build());

    }

    @Test
    void getInventoryHoldStatusByUserDefinedAttributes() {

        when(receiptDetailDao
                .getExpectedReceiptDetailsWithSkus(RECEIPT_KEY, Set.of(SKU_ID)))
                .thenReturn(List.of(getReceiptDetail()));

        when(receiptDao.getReceiptByKey(RECEIPT_KEY)).thenReturn(Receipt.builder()
                .receiptKey(RECEIPT_KEY)
                .type(ReceiptType.DEFAULT)
                .build());

        when(cteClient.evaluateResupplyItem(eq(Long.parseLong(EXTERN_RECEIPT_KEY)), eq(UUID),
                any(SupplyItemRequestDTO.class)))
                .thenReturn(getSupplyItem());

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class,
                        () -> goodsService.getInventoryHoldStatusByUserDefinedAttributes(
                                RECEIPT_KEY,
                                SkuId.of(STORER_KEY, SKU),
                                false,
                                "",
                                "",
                                Set.of(),
                                UUID));

        assertEquals("Unknown value of UNDEFINED for InventoryHoldStatus.", exception.getMessage());
        verify(cteClient).evaluateResupplyItem(eq(Long.parseLong(EXTERN_RECEIPT_KEY)), eq(UUID),
                any(SupplyItemRequestDTO.class));
        verify(receiptDetailDao).getExpectedReceiptDetailsWithSkus(RECEIPT_KEY, Set.of(SKU_ID));
    }

    @Test
    void getInventoryHoldStatusOK() {

        when(receiptDetailDao
                .getExpectedReceiptDetailsWithSkus(RECEIPT_KEY, Set.of(SKU_ID)))
                .thenReturn(List.of(getReceiptDetail()));

        when(receiptDao.getReceiptByKey(RECEIPT_KEY)).thenReturn(Receipt.builder()
                .receiptKey(RECEIPT_KEY)
                .type(ReceiptType.DEFAULT)
                .build());

        when(cteClient.evaluateResupplyItem(eq(Long.parseLong(EXTERN_RECEIPT_KEY)), eq(UUID),
                any(SupplyItemRequestDTO.class)))
                .thenReturn(getGoodStockItem());

        InventoryHoldStatus actualHoldStatus = goodsService.getInventoryHoldStatusByUserDefinedAttributes(
                RECEIPT_KEY,
                SkuId.of(STORER_KEY, SKU),
                false,
                "",
                "",
                Set.of(),
                UUID
        );
        assertEquals(InventoryHoldStatus.OK, actualHoldStatus);
    }

    @Test
    void getInventoryHoldStatusWithNullReceiptDetail() {

        ReceiptDetailsNotFoundException exception =
                assertThrows(ReceiptDetailsNotFoundException.class,
                        () -> goodsService.getInventoryHoldStatusByUserDefinedAttributes(
                                RECEIPT_KEY,
                                SkuId.of(STORER_KEY, SKU),
                                false,
                                "",
                                "",
                                Set.of(),
                                UUID));

        assertEquals("404 NOT_FOUND \"Receipt details not found for sku: SkuId(storerKey=100500, sku=ROV)\"",
                exception.getMessage());
    }

    @Test
    void getInventoryHoldStatusRegistryTypeRefund() {

        when(receiptDetailDao
                .getExpectedReceiptDetailsWithSkus(RECEIPT_KEY, Set.of(SKU_ID)))
                .thenReturn(List.of(getReceiptDetail()));

        when(receiptDao.getReceiptByKey(RECEIPT_KEY)).thenReturn(Receipt.builder()
                .receiptKey(RECEIPT_KEY)
                .type(ReceiptType.CUSTOMER_RETURN)
                .build());

        when(cteClient.evaluateResupplyItem(eq(Long.parseLong(EXTERN_RECEIPT_KEY)), eq(UUID),
                any(SupplyItemRequestDTO.class)))
                .thenReturn(getGoodStockItem());

        goodsService.getInventoryHoldStatusByUserDefinedAttributes(
                RECEIPT_KEY,
                SkuId.of(STORER_KEY, SKU),
                false,
                "",
                "",
                Set.of(),
                UUID);

        verify(cteClient).evaluateResupplyItem(eq(Long.parseLong(EXTERN_RECEIPT_KEY)), eq(UUID),
                argThat(argument -> argument.getSupplyDTO().getRegistryType() == RegistryType.REFUND));
    }

    @Test
    void getInventoryHoldStatusRegistryTypeUnpaid() {

        when(receiptDetailDao
                .getExpectedReceiptDetailsWithSkus(RECEIPT_KEY, Set.of(SKU_ID)))
                .thenReturn(List.of(getReceiptDetail()));

        when(receiptDao.getReceiptByKey(RECEIPT_KEY)).thenReturn(Receipt.builder()
                .receiptKey(RECEIPT_KEY)
                .type(ReceiptType.VALID_UNREDEEMED)
                .build());

        when(cteClient.evaluateResupplyItem(eq(Long.parseLong(EXTERN_RECEIPT_KEY)), eq(UUID),
                any(SupplyItemRequestDTO.class)))
                .thenReturn(getGoodStockItem());

        goodsService.getInventoryHoldStatusByUserDefinedAttributes(
                RECEIPT_KEY,
                SkuId.of(STORER_KEY, SKU),
                false,
                "",
                "",
                Set.of(),
                UUID);

        verify(cteClient).evaluateResupplyItem(eq(Long.parseLong(EXTERN_RECEIPT_KEY)), eq(UUID),
                argThat(argument -> argument.getSupplyDTO().getRegistryType() == RegistryType.UNPAID));
    }

    @Test
    void getInventoryHoldStatusRegistryTypeUnknown() {

        when(receiptDetailDao
                .getExpectedReceiptDetailsWithSkus(RECEIPT_KEY, Set.of(SKU_ID)))
                .thenReturn(List.of(getReceiptDetail()));

        when(receiptDao.getReceiptByKey(RECEIPT_KEY)).thenReturn(Receipt.builder()
                .receiptKey(RECEIPT_KEY)
                .type(ReceiptType.UNKNOWN)
                .build());

        when(cteClient.evaluateResupplyItem(eq(Long.parseLong(EXTERN_RECEIPT_KEY)), eq(UUID),
                any(SupplyItemRequestDTO.class)))
                .thenReturn(getGoodStockItem());

        goodsService.getInventoryHoldStatusByUserDefinedAttributes(
                RECEIPT_KEY,
                SkuId.of(STORER_KEY, SKU),
                false,
                "",
                "",
                Set.of(),
                UUID);

        verify(cteClient).evaluateResupplyItem(eq(Long.parseLong(EXTERN_RECEIPT_KEY)), eq(UUID),
                argThat(argument -> argument.getSupplyDTO().getRegistryType() == RegistryType.UNKNOWN));
    }

    private SupplyItemDTO getSupplyItem() {
        return SupplyItemDTO.builder()
                .stockType(StockType.UNDEFINED)
                .build();
    }

    private SupplyItemDTO getGoodStockItem() {
        return SupplyItemDTO.builder()
                .stockType(StockType.OK)
                .build();
    }

    private ReceiptDetail getReceiptDetail() {
        return ReceiptDetail.builder()
                .skuId(SKU_ID)
                .receiptDetailKey(new ReceiptDetailKey(RECEIPT_KEY, LINE_NUMBER))
                .externReceiptKey(EXTERN_RECEIPT_KEY)
                .categoryId(CATEGORY_ID)
                .unitPrice(PRICE)
                .name("")
                .build();
    }
}
