package ru.yandex.market.wms.common.spring.dao;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.LotStatus;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.LotLocId;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.implementation.LotLocIdDao;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static ru.yandex.market.wms.common.spring.utils.EntityCreationUtils.SCALED_ONE;

public class LotLocIdDaoTest extends IntegrationTest {

    @Autowired
    private LotLocIdDao lotLocIdDao;

    @Test
    @DatabaseSetup("/db/dao/lot-loc-id/before.xml")
    @ExpectedDatabase(value = "/db/dao/lot-loc-id/before.xml", assertionMode = NON_STRICT)
    public void findBySkuIdList() {
        List<SkuId> rov0000000000000001456 = List.of(new SkuId("465852", "ROV0000000000000001456"));
        List<LotLocId> bySkuIdList = lotLocIdDao.findBySkuIdList(rov0000000000000001456);
        assertions.assertThat(bySkuIdList).contains(
                LotLocId.builder()
                        .lot("0000012345")
                        .loc("STAGE")
                        .id("")
                        .storerKey("465852")
                        .sku("ROV0000000000000001456")
                        .lotStatus(LotStatus.OK)
                        .qty(SCALED_ONE)
                        .qtyPicked(SCALED_ONE)
                        .qtyAllocated(SCALED_ONE)
                        .addWho("test")
                        .editWho("TEST1")
                        .serialKey("1")
                        .build(),
                LotLocId.builder()
                        .lot("0000012344")
                        .loc("STAGE")
                        .id("ID")
                        .storerKey("465852")
                        .sku("ROV0000000000000001456")
                        .lotStatus(LotStatus.OK)
                        .qty(SCALED_ONE)
                        .qtyPicked(SCALED_ONE)
                        .qtyAllocated(SCALED_ONE)
                        .addWho("test")
                        .editWho("TEST1")
                        .serialKey("2")
                        .build()
        );
    }

    @Test
    @DatabaseSetup("/db/dao/lot-loc-id/before.xml")
    @ExpectedDatabase(value = "/db/dao/lot-loc-id/before.xml", assertionMode = NON_STRICT)
    public void findSerialKeyIfExists() {
        Optional<Long> maybeSerialKey = lotLocIdDao.findSerialKey("0000012345", "STAGE", "",
                "465852", "ROV0000000000000001456", LotStatus.OK);
        assertions.assertThat(maybeSerialKey).isPresent();
        assertions.assertThat(maybeSerialKey.get()).isEqualTo(1);
    }

    @Test
    @DatabaseSetup("/db/dao/lot-loc-id/before.xml")
    @ExpectedDatabase(value = "/db/dao/lot-loc-id/before.xml", assertionMode = NON_STRICT)
    public void findSerialKeyIfNotExists() {
        Optional<Long> maybeSerialKey = lotLocIdDao.findSerialKey("0000012344", "STAGE", "",
                "465852", "ROV0000000000000001456", LotStatus.HOLD);
        assertions.assertThat(maybeSerialKey).isNotPresent();
    }

    @Test
    @DatabaseSetup("/db/dao/lot-loc-id/before-save.xml")
    @ExpectedDatabase(value = "/db/dao/lot-loc-id/after-create.xml", assertionMode = NON_STRICT)
    public void saveWithEmptyId() {
        LotLocId lotLocId = LotLocId.builder()
                .lot("0000012345")
                .loc("STAGE")
                .id("")
                .storerKey("465852")
                .sku("ROV0000000000000001456")
                .lotStatus(LotStatus.HOLD)
                .qty(BigDecimal.ONE)
                .qtyPicked(BigDecimal.ZERO)
                .addWho("TEST")
                .editWho("TEST")
                .build();
        lotLocIdDao.insert(Collections.singletonList(lotLocId));
    }

    @Test
    @DatabaseSetup("/db/dao/lot-loc-id/before.xml")
    @ExpectedDatabase(value = "/db/dao/lot-loc-id/after-add.xml", assertionMode = NON_STRICT)
    public void addOne() {
        lotLocIdDao.addQtyAndUpdateStatus(1, BigDecimal.ONE, LotStatus.OK, "TEST");
    }

    @Test
    @DatabaseSetup("/db/dao/lot-loc-id/before-subtract.xml")
    @ExpectedDatabase(value = "/db/dao/lot-loc-id/after-subtract.xml", assertionMode = NON_STRICT)
    public void subtractOne() {
        lotLocIdDao.subtractQty(SkuId.PL, "STAGE", "12345", BigDecimal.ONE, "TEST");
    }

    @Test
    @DatabaseSetup("/db/dao/lot-loc-id/before-subtract.xml")
    @ExpectedDatabase(value = "/db/dao/lot-loc-id/after-subtract.xml", assertionMode = NON_STRICT)
    public void subtractOneQtyLessThanZero() {
        lotLocIdDao.subtractQty(SkuId.PL, "STAGE", "12345", BigDecimal.TEN, "TEST");
    }

    @Test
    @DatabaseSetup("/db/dao/lot-loc-id/before-lot-on-pl-storage.xml")
    @ExpectedDatabase(value = "/db/dao/lot-loc-id/before-lot-on-pl-storage.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getLotsOnPalletStorageBySku() {
        SkuId skuId = SkuId.of("465852", "ROV0000000000000000359");
        List<Pair<String, Instant>> lotsOnPalletStorage = lotLocIdDao.getLotsOnPalletStorageBySku(skuId, 5);
        assertions.assertThat(lotsOnPalletStorage.size()).isEqualTo(1);
        assertions.assertThat(lotsOnPalletStorage.get(0).getKey()).isEqualTo("0000000700");
    }

    @Test
    @DatabaseSetup("/db/dao/lot-loc-id/before-with-alllocated-items.xml")
    void checkAllocatedLot_1_qty_1_allocated_check_1_serial() {
        List<String> result = lotLocIdDao.getLotsWithAllocatedAndPickedQtyBySerialNumbers(List.of("SERIAL_1"));

        assertions.assertThat(result.size()).isEqualTo(1);
        assertions.assertThat(result.get(0)).isEqualTo("0000012344");
    }

    @Test
    @DatabaseSetup("/db/dao/lot-loc-id/before-with-alllocated-items.xml")
    void checkAllocatedLot_2_qty_1_allocated_check_2_serial() {
        List<String> result =
                lotLocIdDao.getLotsWithAllocatedAndPickedQtyBySerialNumbers(List.of("SERIAL_2", "SERIAL_3"));

        assertions.assertThat(result.size()).isEqualTo(1);
        assertions.assertThat(result.get(0)).isEqualTo("0000012344");
    }

    @Test
    @DatabaseSetup("/db/dao/lot-loc-id/before-with-alllocated-items.xml")
    void checkAllocatedLot_2_qty_1_allocated_check_1_serial() {
        List<String> result = lotLocIdDao.getLotsWithAllocatedAndPickedQtyBySerialNumbers(List.of("SERIAL_2"));

        assertions.assertThat(result.size()).isEqualTo(0);
    }

    @Test
    @DatabaseSetup("/db/dao/lot-loc-id/before-add-hold.xml")
    @ExpectedDatabase(value = "/db/dao/lot-loc-id/after-add-hold.xml", assertionMode = NON_STRICT)
    void placeHoldStatusOnLot() {
        lotLocIdDao.placeHoldStatusOnLot("0001922728", "TEST");
    }
}
