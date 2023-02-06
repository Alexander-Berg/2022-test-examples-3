package ru.yandex.market.wms.common.spring.service.unit;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.wms.common.model.enums.LotStatus;
import ru.yandex.market.wms.common.model.enums.NSqlConfigKey;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.entity.LotLocId;
import ru.yandex.market.wms.common.spring.dao.entity.Sku;
import ru.yandex.market.wms.common.spring.dao.implementation.InventoryHoldDao;
import ru.yandex.market.wms.common.spring.dao.implementation.LotLocIdDao;
import ru.yandex.market.wms.common.spring.service.LotLocIdService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.wms.common.spring.utils.EntityCreationUtils.createSku;

public class LotLocIdServiceTest extends BaseTest {

    private static final String LOT = "0000012345";
    private static final String LOC = "STAGE";
    private static final String ID = "CART";
    private static final String USER = "TEST";
    private static final Long SERIALKEY = 1L;

    private LotLocIdService lotLocIdService;
    private LotLocIdDao lotLocIdDao;
    private InventoryHoldDao inventoryHoldDao;
    private DbConfigService configService;

    @BeforeEach
    public void setup() {
        super.setup();
        lotLocIdDao = mock(LotLocIdDao.class);
        inventoryHoldDao = mock(InventoryHoldDao.class);
        configService = mock(DbConfigService.class);
        lotLocIdService = new LotLocIdService(lotLocIdDao, inventoryHoldDao, configService);
    }

    @Test
    public void addOrUpdateIfExists() {
        Sku sku = createSku();
        when(lotLocIdDao.findSerialKey(LOT, LOC, ID, sku.getStorerKey(), sku.getSku(), LotStatus.OK))
                .thenReturn(Optional.of(SERIALKEY));
        lotLocIdService.addOrUpdate(LOT, LOC, ID, BigDecimal.ONE, sku.getSkuId(), LotStatus.OK, USER);
        verify(lotLocIdDao).findSerialKey(LOT, LOC, ID, sku.getStorerKey(), sku.getSku(), LotStatus.OK);
        verify(lotLocIdDao).addToQty(SERIALKEY, BigDecimal.ONE, USER);
    }

    @Test
    public void addOrUpdateIfNotExists() {
        Sku sku = createSku();
        when(lotLocIdDao.findSerialKey(LOT, LOC, ID, sku.getStorerKey(), sku.getSku(), LotStatus.HOLD))
                .thenReturn(Optional.empty());
        lotLocIdService.addOrUpdate(LOT, LOC, ID, BigDecimal.ONE, sku.getSkuId(), LotStatus.HOLD, USER);
        verify(lotLocIdDao).findSerialKey(LOT, LOC, ID, sku.getStorerKey(), sku.getSku(), LotStatus.HOLD);
        ArgumentCaptor<List<LotLocId>> captor = ArgumentCaptor.forClass(List.class);
        verify(lotLocIdDao).insert(captor.capture());
        List<LotLocId> captured = captor.getValue();
        assertions.assertThat(captured).hasSize(1);
        LotLocId lotLocId = captured.get(0);
        assertLotLocId(lotLocId, sku, LotStatus.HOLD);
    }

    @Test
    public void addQtyAndCheckStatusIfExists() {
        when(configService.getConfigAsBoolean(NSqlConfigKey.YM_LOTLOCID_INVHOLD_STATUS)).thenReturn(false);
        Sku sku = createSku();
        when(lotLocIdDao.findSerialKey(LOT, LOC, ID, sku.getStorerKey(), sku.getSku(), LotStatus.HOLD))
                .thenReturn(Optional.of(SERIALKEY));
        lotLocIdService.addQtyAndCheckStatus(LOT, LOC, ID, BigDecimal.ONE, sku.getSkuId(), LotStatus.HOLD, USER);
        verify(lotLocIdDao).findSerialKey(LOT, LOC, ID, sku.getStorerKey(), sku.getSku(), LotStatus.HOLD);
        verify(lotLocIdDao).addToQty(SERIALKEY, BigDecimal.ONE, USER);
    }

    @Test
    public void addQtyAndCheckStatusIdExistsWithInventoryHoldStatus() {
        when(configService.getConfigAsBoolean(NSqlConfigKey.YM_LOTLOCID_INVHOLD_STATUS))
                .thenReturn(true);
        when(inventoryHoldDao.getLotsWithHold(List.of(LOT)))
                .thenReturn(List.of(LOT));

        Sku sku = createSku();
        when(lotLocIdDao.findLotLocId(LOT, LOC, ID, sku.getStorerKey(), sku.getSku()))
                .thenReturn(Optional.of(createLotLocId(sku)));
        lotLocIdService.addQtyAndCheckStatus(LOT, LOC, ID, BigDecimal.ONE, sku.getSkuId(), LotStatus.OK, USER);
        verify(lotLocIdDao).findLotLocId(LOT, LOC, ID, sku.getStorerKey(), sku.getSku());
        verify(lotLocIdDao).addQtyAndUpdateStatus(SERIALKEY, BigDecimal.ONE, LotStatus.HOLD, USER);
    }

    @Test
    public void addQtyAndCheckStatusNotExists() {
        when(configService.getConfigAsBoolean(NSqlConfigKey.YM_LOTLOCID_INVHOLD_STATUS)).thenReturn(false);
        Sku sku = createSku();
        when(lotLocIdDao.findSerialKey(LOT, LOC, ID, sku.getStorerKey(), sku.getSku(), LotStatus.OK))
                .thenReturn(Optional.empty());
        lotLocIdService.addQtyAndCheckStatus(LOT, LOC, ID, BigDecimal.ONE, sku.getSkuId(), LotStatus.OK, USER);
        verify(lotLocIdDao).findSerialKey(LOT, LOC, ID, sku.getStorerKey(), sku.getSku(), LotStatus.OK);

        ArgumentCaptor<List<LotLocId>> captor = ArgumentCaptor.forClass(List.class);
        verify(lotLocIdDao).insert(captor.capture());
        List<LotLocId> captured = captor.getValue();
        assertions.assertThat(captured).hasSize(1);
        LotLocId lotLocId = captured.get(0);
        assertLotLocId(lotLocId, sku, LotStatus.OK);
    }

    @Test
    public void addQtyAndCheckStatusNotExistsWithInventoryHoldStatus() {
        when(configService.getConfigAsBoolean(NSqlConfigKey.YM_LOTLOCID_INVHOLD_STATUS)).thenReturn(true);
        when(inventoryHoldDao.getLocsWithHold(List.of(LOC)))
                .thenReturn(List.of(LOC));

        Sku sku = createSku();
        when(lotLocIdDao.findLotLocId(LOT, LOC, ID, sku.getStorerKey(), sku.getSku()))
                .thenReturn(Optional.empty());
        lotLocIdService.addQtyAndCheckStatus(LOT, LOC, ID, BigDecimal.ONE, sku.getSkuId(), LotStatus.OK, USER);
        verify(lotLocIdDao).findLotLocId(LOT, LOC, ID, sku.getStorerKey(), sku.getSku());

        ArgumentCaptor<List<LotLocId>> captor = ArgumentCaptor.forClass(List.class);
        verify(lotLocIdDao).insert(captor.capture());
        List<LotLocId> captured = captor.getValue();
        assertions.assertThat(captured).hasSize(1);
        LotLocId lotLocId = captured.get(0);
        assertLotLocId(lotLocId, sku, LotStatus.HOLD);
    }

    private LotLocId createLotLocId(Sku sku) {
        return LotLocId.builder()
                .serialKey(SERIALKEY.toString())
                .lot(LOT)
                .loc(LOC)
                .id(ID)
                .storerKey(sku.getStorerKey())
                .sku(sku.getSku())
                .lotStatus(LotStatus.OK)
                .build();
    }

    private void assertLotLocId(LotLocId lotLocId, Sku sku, LotStatus lotStatus) {
        assertions.assertThat(lotLocId.getLot()).isEqualTo(LOT);
        assertions.assertThat(lotLocId.getLoc()).isEqualTo(LOC);
        assertions.assertThat(lotLocId.getId()).isEqualTo(ID);
        assertions.assertThat(lotLocId.getStorerKey()).isEqualTo(sku.getStorerKey());
        assertions.assertThat(lotLocId.getSku()).isEqualTo(sku.getSku());
        assertions.assertThat(lotLocId.getLotStatus()).isEqualTo(lotStatus);
        assertions.assertThat(lotLocId.getQty()).isOne();
        assertions.assertThat(lotLocId.getQtyPicked()).isZero();
        assertions.assertThat(lotLocId.getAddWho()).isEqualTo(USER);
        assertions.assertThat(lotLocId.getEditWho()).isEqualTo(USER);
    }
}
