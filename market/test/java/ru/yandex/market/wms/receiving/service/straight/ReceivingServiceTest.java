package ru.yandex.market.wms.receiving.service.straight;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.wms.common.model.enums.ReceiptType;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.entity.ExpectedAndReceivedSku;
import ru.yandex.market.wms.common.spring.dao.entity.Loc;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.dao.entity.OrderDetail;
import ru.yandex.market.wms.common.spring.dao.entity.PutawayZone;
import ru.yandex.market.wms.common.spring.dao.entity.Receipt;
import ru.yandex.market.wms.common.spring.dao.entity.ReceiptDetail;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.implementation.LotIdDetailDao;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDao;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDetailDao;
import ru.yandex.market.wms.common.spring.dao.implementation.PalletDao;
import ru.yandex.market.wms.common.spring.dao.implementation.PutawayZoneDAO;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDao;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDetailDao;
import ru.yandex.market.wms.common.spring.enums.PutawayZoneType;
import ru.yandex.market.wms.common.spring.pojo.ReceiptDetailKey;
import ru.yandex.market.wms.receiving.dao.LocDao;
import ru.yandex.market.wms.receiving.service.ScanningOperationLog;
import ru.yandex.market.wms.receiving.service.SkuMeasurementService;

import static org.mockito.Mockito.when;
import static ru.yandex.market.wms.receiving.service.straight.ReceivingService.NEW_BBXD_FLOW;
import static ru.yandex.market.wms.receiving.service.straight.ReceivingService.VALIDATE_BBXD_PLAN_QTY;

@ExtendWith(MockitoExtension.class)
class ReceivingServiceTest extends BaseTest {

    @Mock
    private ReceiptDao receiptDao;
    @Mock
    private PalletDao palletDao;
    @Mock
    private ReceiptDetailDao receiptDetailDao;
    @Mock
    private LotIdDetailDao lotIdDetailDao;
    @Mock
    private OrderDao orderDao;
    @Mock
    private OrderDetailDao orderDetailDao;
    @Mock
    private DbConfigService configService;
    @Mock
    private SkuMeasurementService skuMeasurementService;
    @Mock
    private ScanningOperationLog scanningOperationLog;
    @Mock
    private LocDao locDao;
    @Mock
    private PutawayZoneDAO putawayZoneDAO;

    private ReceivingService receivingService;

    @BeforeEach
    private void before() {
        receivingService = new ReceivingService(
                null, null, null, receiptDetailDao, null,
                null, null, null, null, null,
                null, null, configService, null, receiptDao,
                null, null, null,
                null, null, null, null, lotIdDetailDao,
                null, null, null, null, null,
                palletDao, null, null, null, null,
                null, null, null, null, null,
                null, null, skuMeasurementService, null, null,
                orderDao, orderDetailDao, null, scanningOperationLog, locDao,
                putawayZoneDAO, null, null, null, null,
                null, null);
    }

    @Test
    void validateSameReceiptOnPalletBbxd() {
        final String receiptKey = "123";
        final String wrongReceiptKey = "456";
        final String palletId = "PLT123";
        List<String> boxes = List.of("BOX1", "BOX2");

        when(receiptDao.getReceiptByKey(receiptKey)).thenReturn(
                Receipt.builder()
                        .receiptKey(receiptKey)
                        .type(ReceiptType.XDOCK)
                        .build());
        when(palletDao.getContainers(palletId)).thenReturn(boxes);
        when(receiptDetailDao.getDetailsWithToIds(boxes)).thenReturn(List.of(
                ReceiptDetail.builder()
                        .receiptDetailKey(new ReceiptDetailKey(wrongReceiptKey, "00001"))
                        .build(),
                ReceiptDetail.builder()
                        .receiptDetailKey(new ReceiptDetailKey(wrongReceiptKey, "00002"))
                        .build()
        ));
        assertions.assertThatThrownBy(
                        () -> receivingService.validateSameReceiptAndQuantOnPalletBbxd(palletId, receiptKey,
                                1, null))
                .hasMessage("404 NOT_FOUND \"Different receipt 456 on pallet\"");
    }

    @Test
    void validateSameQuantOnPalletBbxd1() {
        final String receiptKey = "123";
        final String palletId = "PLT123";
        List<String> boxes = List.of("BOX1", "BOX2");
        SkuId skuId = SkuId.of("465852", "ROV0000000000000000358");

        when(receiptDao.getReceiptByKey(receiptKey)).thenReturn(
                Receipt.builder()
                        .receiptKey(receiptKey)
                        .type(ReceiptType.XDOCK)
                        .build());
        when(palletDao.getContainers(palletId)).thenReturn(boxes);
        when(receiptDetailDao.getDetailsWithToIds(boxes)).thenReturn(List.of(
                ReceiptDetail.builder()
                        .receiptDetailKey(new ReceiptDetailKey(receiptKey, "00001"))
                        .build(),
                ReceiptDetail.builder()
                        .receiptDetailKey(new ReceiptDetailKey(receiptKey, "00002"))
                        .build()
        ));
        when(lotIdDetailDao.getQtyPerBoxes(boxes, skuId)).thenReturn(Map.of("BOX1", 2, "BOX2", 3));

        assertions.assertThatThrownBy(
                        () -> receivingService.validateSameReceiptAndQuantOnPalletBbxd(palletId, receiptKey,
                                1, skuId))
                .hasMessageContaining("Wrong qty of boxes");
    }

    @Test
    void validateSameQuantOnPalletBbxd2() {
        final String receiptKey = "123";
        final String palletId = "PLT123";
        List<String> boxes = List.of("BOX1", "BOX2");
        SkuId skuId = SkuId.of("465852", "ROV0000000000000000358");

        when(receiptDao.getReceiptByKey(receiptKey)).thenReturn(
                Receipt.builder()
                        .receiptKey(receiptKey)
                        .type(ReceiptType.XDOCK)
                        .build());
        when(palletDao.getContainers(palletId)).thenReturn(boxes);
        when(receiptDetailDao.getDetailsWithToIds(boxes)).thenReturn(List.of(
                ReceiptDetail.builder()
                        .receiptDetailKey(new ReceiptDetailKey(receiptKey, "00001"))
                        .build(),
                ReceiptDetail.builder()
                        .receiptDetailKey(new ReceiptDetailKey(receiptKey, "00002"))
                        .build()
        ));
        when(lotIdDetailDao.getQtyPerBoxes(boxes, skuId)).thenReturn(Map.of("BOX1", 2, "BOX2", 2));

        assertions.assertThatThrownBy(
                        () -> receivingService.validateSameReceiptAndQuantOnPalletBbxd(palletId, receiptKey,
                                1, skuId))
                .hasMessage("400 BAD_REQUEST \"Pallet PLT123 contains quant 2 different from input 1 for this SKU\"");
    }

    @Test
    void validateBbxdPlannedQty1() {
        when(configService.getConfigAsBoolean(VALIDATE_BBXD_PLAN_QTY, true)).thenReturn(true);
        String receiptKey = "123";
        String externReceiptKey = "456";
        Receipt receipt = Receipt.builder()
                .receiptKey(receiptKey)
                .externReceiptKey(externReceiptKey)
                .type(ReceiptType.XDOCK)
                .build();
        SkuId skuId = SkuId.of("465852", "ROV0000000000000000358");
        when(receiptDetailDao.getSkuBalances(receiptKey))
                .thenReturn(List.of(ExpectedAndReceivedSku.builder()
                        .key(ExpectedAndReceivedSku.Key.of(skuId))
                        .quantityExpected(BigDecimal.ONE)
                        .build()));

        assertions.assertThatThrownBy(() -> receivingService.validateBbxdPlannedQty(receipt))
                .hasMessageContaining(
                        ("Для поставки %s (%s) не соответствует потребность для товара %s. " +
                                "Ожидалось в поставке: %s. Ожидалось в заказе: %s").formatted(receiptKey,
                                externReceiptKey, skuId.getSku(), 1, "-"));
    }

    @Test
    void validateBbxdPlannedQty2() {
        when(configService.getConfigAsBoolean(VALIDATE_BBXD_PLAN_QTY, true)).thenReturn(true);
        String receiptKey = "123";
        String externReceiptKey = "456";
        Receipt receipt = Receipt.builder()
                .receiptKey(receiptKey)
                .externReceiptKey(externReceiptKey)
                .type(ReceiptType.XDOCK)
                .build();
        SkuId skuId = SkuId.of("465852", "ROV0000000000000000358");
        when(orderDao.findBbxdOrderWithReceipt(externReceiptKey))
                .thenReturn(List.of(
                        Order.builder().orderKey("123").build(),
                        Order.builder().orderKey("456").build()));
        when(orderDetailDao.findOrderDetailsByOrderKeys(List.of("123", "456")))
                .thenReturn(List.of(
                        OrderDetail.builder().sku(skuId.getSku()).storerKey(skuId.getStorerKey())
                                .openQty(BigDecimal.ONE).build(),
                        OrderDetail.builder().sku(skuId.getSku()).storerKey(skuId.getStorerKey())
                                .openQty(BigDecimal.ONE).build()
                ));
        when(receiptDetailDao.getSkuBalances(receiptKey))
                .thenReturn(List.of(ExpectedAndReceivedSku.builder()
                        .key(ExpectedAndReceivedSku.Key.of(skuId))
                        .quantityExpected(BigDecimal.ONE)
                        .build()));

        assertions.assertThatThrownBy(() -> receivingService.validateBbxdPlannedQty(receipt))
                .hasMessageContaining(
                        ("Для поставки %s (%s) не соответствует потребность для товара %s. " +
                                "Ожидалось в поставке: %s. Ожидалось в заказе: %s").formatted(receiptKey,
                                externReceiptKey, skuId.getSku(), 1, 2));
    }

    @Test
    void sortingAllowedOldFlow() {
        String receiptKey = "123";
        String locationId = "LOC123";

        when(configService.getConfigAsBoolean(NEW_BBXD_FLOW, false)).thenReturn(false);
        boolean result = receivingService.sortingAllowed(receiptKey, locationId);
        assertions.assertThat(result).isFalse();
    }

    @Test
    void sortingAllowedNotBbxdReceipt() {
        String receiptKey = "123";
        String locationId = "LOC123";

        when(configService.getConfigAsBoolean(NEW_BBXD_FLOW, false)).thenReturn(true);
        when(receiptDao.getReceiptByKey(receiptKey)).thenReturn(
                Receipt.builder()
                        .receiptKey(receiptKey)
                        .type(ReceiptType.DEFAULT)
                        .build());

        boolean result = receivingService.sortingAllowed(receiptKey, locationId);
        assertions.assertThat(result).isFalse();
    }

    @Test
    void sortingAllowedNotSortedZone() {
        String receiptKey = "123";
        String locationId = "LOC123";
        String zone = "ZONE";

        when(configService.getConfigAsBoolean(NEW_BBXD_FLOW, false)).thenReturn(true);
        when(receiptDao.getReceiptByKey(receiptKey)).thenReturn(
                Receipt.builder()
                        .receiptKey(receiptKey)
                        .type(ReceiptType.XDOCK)
                        .build());
        when(locDao.getLocation(locationId)).thenReturn(Optional.of(
                Loc.builder()
                        .loc(locationId)
                        .putawayzone(zone)
                        .build()));
        when(putawayZoneDAO.find(zone)).thenReturn(
                PutawayZone.builder()
                        .putawayZone(zone)
                        .type(PutawayZoneType.BBXD_RECEIVING)
                        .build());
        boolean result = receivingService.sortingAllowed(receiptKey, locationId);
        assertions.assertThat(result).isFalse();
    }

    @Test
    void sortingAllowedOk() {
        String receiptKey = "123";
        String locationId = "LOC123";
        String zone = "ZONE";

        when(configService.getConfigAsBoolean(NEW_BBXD_FLOW, false)).thenReturn(true);
        when(receiptDao.getReceiptByKey(receiptKey)).thenReturn(
                Receipt.builder()
                        .receiptKey(receiptKey)
                        .type(ReceiptType.XDOCK)
                        .build());
        when(locDao.getLocation(locationId)).thenReturn(Optional.of(
                Loc.builder()
                        .loc(locationId)
                        .putawayzone(zone)
                        .build()));
        when(putawayZoneDAO.find(zone)).thenReturn(
                PutawayZone.builder()
                        .putawayZone(zone)
                        .type(PutawayZoneType.BBXD_SORTER)
                        .build());
        boolean result = receivingService.sortingAllowed(receiptKey, locationId);
        assertions.assertThat(result).isTrue();
    }
}
