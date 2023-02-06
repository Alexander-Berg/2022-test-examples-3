package ru.yandex.market.wms.common.spring.service.unit;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import ru.yandex.market.wms.common.model.enums.InventoryHoldStatus;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.SerialInventory;
import ru.yandex.market.wms.common.spring.dao.entity.Sku;
import ru.yandex.market.wms.common.spring.dao.implementation.SerialInventoryDao;
import ru.yandex.market.wms.common.spring.enums.LostType;
import ru.yandex.market.wms.common.spring.pojo.ItrnCreateParams;
import ru.yandex.market.wms.common.spring.pojo.LotCreationResult;
import ru.yandex.market.wms.common.spring.pojo.LotInputData;
import ru.yandex.market.wms.common.spring.pojo.MovingItem;
import ru.yandex.market.wms.common.spring.pojo.SkuDimensions;
import ru.yandex.market.wms.common.spring.service.SerialInventoryLostService;
import ru.yandex.market.wms.common.spring.service.SerialInventoryService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SerialInventoryServiceTest extends IntegrationTest {

    @Autowired
    SerialInventoryService serialInventoryService;
    @Autowired
    SerialInventoryLostService serialInventoryLostService;
    @Autowired
    SerialInventoryDao serialInventoryDao;

    @Test
    @DatabaseSetup(value = "/db/dao/serial-inventory-lost/lost-db.xml")
    @ExpectedDatabase(value = "/db/dao/serial-inventory-lost/lost-db-after-operlost-writeoff.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void writeOffOperLostTest() {
        List<SerialInventory> serials = serialInventoryLostService.getLost(LostType.OPER);
        serialInventoryService.writeOffWithHold(serials, createItrnParams(), "Test");

        List<SerialInventory> byLostLocation = serialInventoryDao.getByLoc("LOST");
        assertions.assertThat(byLostLocation.size()).isEqualTo(0);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/serial-inventory-lost/lost-db.xml")
    @ExpectedDatabase(value = "/db/dao/serial-inventory-lost/lost-db-after-fixlost-writeoff.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void writeOffFixLostTest() {
        List<SerialInventory> serials = serialInventoryLostService.getLost(LostType.FIX);
        serialInventoryService.writeOffWithHold(serials, createItrnParams(), "Test");

        List<SerialInventory> byLostLocation = serialInventoryDao.getByLoc("LOST");
        List<String> serialNumbers =
                byLostLocation.stream().map(SerialInventory::getSerialNumber).collect(Collectors.toList());
        assertions.assertThat(serialNumbers.size()).isEqualTo(3);
        assertions.assertThat(serialNumbers).doesNotContain("SERIAL_2", "SERIAL_5");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/serial-inventory-lost/move-from-id-and-loc/long-sourcekey-truncate/before.xml")
    @ExpectedDatabase(value = "/db/dao/serial-inventory-lost/move-from-id-and-loc/long-sourcekey-truncate/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testLongItrnSourceKeyTruncate() {
        ItrnCreateParams itrnCreateParams = ItrnCreateParams.builder()
                .generateSystemId()
                .transactionType("MV")
                .sourceKey("46e51f5a-012d-4000-ac74-580428671582")
                .sourceType("1")
                .status("OK")
                .uom("EA")
                .uomCalc(0)
                .build();
        serialInventoryService.moveFromIdAndLocToIdAndLoc("ID4", "FX-AA2", "ID4", "FX-AA1",
                null, itrnCreateParams, "TEST 123");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/serial-inventory-lost/move-from-id-and-loc/short-sourcekey-truncate/before.xml")
    @ExpectedDatabase(value = "/db/dao/serial-inventory-lost/move-from-id-and-loc/short-sourcekey-truncate/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testShortItrnSourceKeyTruncate() {
        ItrnCreateParams itrnCreateParams = ItrnCreateParams.builder()
                .generateSystemId()
                .transactionType("MV")
                .sourceKey("123ab")
                .sourceType("1")
                .status("OK")
                .uom("EA")
                .uomCalc(0)
                .build();
        serialInventoryService.moveFromIdAndLocToIdAndLoc("ID4", "FX-AA2", "ID4", "FX-AA1",
                null, itrnCreateParams, "TEST 123");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/serial-inventory-lost/move-from-id-and-loc/loc-loseid-true/before.xml")
    @ExpectedDatabase(value = "/db/dao/serial-inventory-lost/move-from-id-and-loc/loc-loseid-true/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void dropIdWhenLocLoseIdIsTrue_emptyToId() {
        ItrnCreateParams itrnCreateParams = ItrnCreateParams.builder()
                .generateSystemId()
                .transactionType("MV")
                .sourceKey("ITRN_SOURCE_KEY")
                .sourceType("1")
                .status("OK")
                .uom("EA")
                .uomCalc(0)
                .build();
        serialInventoryService.moveFromIdAndLocToIdAndLoc(
                "ID4",
                "FX-AA2",
                "",
                "FX-AA1",
                itrnCreateParams,
                "TEST 123",
                true);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/serial-inventory-lost/move-from-id-and-loc/remove-redundant-balances/before.xml")
    @ExpectedDatabase(value = "/db/dao/serial-inventory-lost/move-from-id-and-loc/remove-redundant-balances/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testMoveFromIdAndLocToIdAndLocRemoveRedundantBalances() {
        ItrnCreateParams itrnCreateParams = createItrnParams();
        serialInventoryService.moveFromIdAndLocToIdAndLoc("ID4", "FX-AA2", "ID4", "FX-AA1",
                null, itrnCreateParams, "TEST123");
    }

    /*
     * Попытка переместить балансы,
     * когда в таблице LOTxLOCxID уже есть соответствующие ненулевые записи.
     * */
    @Test
    @DatabaseSetup(value = "/db/dao/serial-inventory-lost/move-from-id-and-loc/no-redundant-balances/before.xml")
    @ExpectedDatabase(value = "/db/dao/serial-inventory-lost/move-from-id-and-loc/no-redundant-balances/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testMoveFromIdAndLocToIdAndLocWithNoRedundantBalances() {
        ItrnCreateParams itrnCreateParams = createItrnParams();
        assertThrows(DuplicateKeyException.class, () -> serialInventoryService.moveFromIdAndLocToIdAndLoc("ID4",
                "FX-AA2", "ID4", "FX-AA1", null, itrnCreateParams, "TEST123"));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/serial-inventory-lost/move-from-id-and-loc/with-hold-statuses/before.xml")
    @ExpectedDatabase(value = "/db/dao/serial-inventory-lost/move-from-id-and-loc/with-hold-statuses/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testMoveFromIdAndLocToIdAndLocWithHoldStatuses() {
        ItrnCreateParams itrnCreateParams = createItrnParams();
        serialInventoryService.moveFromIdAndLocToIdAndLoc("ID3", "FX-AA1", "", "FX-AA2",
                null, itrnCreateParams, "TEST123");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/serial-inventory-lost/move-from-id-and-loc/without-inv-holds/before.xml")
    @ExpectedDatabase(value = "/db/dao/serial-inventory-lost/move-from-id-and-loc/without-inv-holds/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testMoveFromIdAndLocToIdAndLocWithFlag() {
        ItrnCreateParams itrnCreateParams = createItrnParams();
        serialInventoryService.moveFromIdAndLocToIdAndLoc("ID3", "FX-AA1", "", "FX-AA2",
                null, itrnCreateParams, "TEST123");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/serial-inventory-lost/move-from-id-and-loc/with-held-lot/before.xml")
    @ExpectedDatabase(value = "/db/dao/serial-inventory-lost/move-from-id-and-loc/with-held-lot/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testMoveFromIdAndLocToIdAndLocWithFlagAndHeldLot() {
        ItrnCreateParams itrnCreateParams = createItrnParams();
        serialInventoryService.moveFromIdAndLocToIdAndLoc("ID3", "FX-AA1", "", "FX-AA2",
                null, itrnCreateParams, "TEST123");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/serial-inventory-lost/move-from-id-and-loc/with-held-loc/before.xml")
    @ExpectedDatabase(value = "/db/dao/serial-inventory-lost/move-from-id-and-loc/with-held-loc/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testMoveFromIdAndLocToIdAndLocWithFlagAndHeldLoc() {
        ItrnCreateParams itrnCreateParams = createItrnParams();
        serialInventoryService.moveFromIdAndLocToIdAndLoc("ID3", "FX-AA1", "", "FX-AA2",
                null, itrnCreateParams, "TEST123");
    }

    @Test
    @DatabaseSetup(value = "/db/service/serialInventory/move-to-id-and-loc/update-status/all-ok/before.xml")
    @ExpectedDatabase(value = "/db/service/serialInventory/move-to-id-and-loc/update-status/all-ok/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void moveToLocAndIdWithStatusUpdate() {
        List<MovingItem> items = List.of(
                createMoveToLocAndIdItem("0001", "LOT1", "A-01", ""),
                createMoveToLocAndIdItem("0002", "LOT2", "A-01", "ID1"),
                createMoveToLocAndIdItem("0003", "LOT2", "A-01", "ID1"),
                createMoveToLocAndIdItem("0004", "LOT2", "A-02", "ID2")
        );
        ItrnCreateParams itrnCreateParams = createItrnParams();
        serialInventoryService.moveToLocAndIdWithLotLocIdStatusUpdate(items, "B-01", "ID3", itrnCreateParams,
                "TEST123");
    }

    @Test
    @DatabaseSetup(value = "/db/service/serialInventory/move-to-id-and-loc/update-status/hold-lot/before.xml")
    @ExpectedDatabase(value = "/db/service/serialInventory/move-to-id-and-loc/update-status/hold-lot/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void moveToLocAndIdWithStatusUpdateAndHoldLot() {
        List<MovingItem> items = List.of(
                createMoveToLocAndIdItem("0001", "LOT1", "A-01", ""),
                createMoveToLocAndIdItem("0002", "LOT2", "A-01", "ID1"),
                createMoveToLocAndIdItem("0003", "LOT2", "A-01", "ID1"),
                createMoveToLocAndIdItem("0004", "LOT2", "A-02", "ID2")
        );
        ItrnCreateParams itrnCreateParams = createItrnParams();
        serialInventoryService.moveToLocAndIdWithLotLocIdStatusUpdate(items, "B-01", "ID3", itrnCreateParams,
                "TEST123");
    }

    @Test
    @DatabaseSetup(value = "/db/service/serialInventory/move-to-id-and-loc/update-status/to-hold-loc/before.xml")
    @ExpectedDatabase(value = "/db/service/serialInventory/move-to-id-and-loc/update-status/to-hold-loc/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void moveToLocAndIdWithStatusUpdateToHoldLoc() {
        List<MovingItem> items = List.of(
                createMoveToLocAndIdItem("0001", "LOT1", "A-01", ""),
                createMoveToLocAndIdItem("0002", "LOT2", "A-01", "ID1"),
                createMoveToLocAndIdItem("0003", "LOT2", "A-01", "ID1"),
                createMoveToLocAndIdItem("0004", "LOT2", "A-02", "ID2")
        );
        ItrnCreateParams itrnCreateParams = createItrnParams();
        serialInventoryService.moveToLocAndIdWithLotLocIdStatusUpdate(items, "B-01", "ID3", itrnCreateParams,
                "TEST123");
    }

    @Test
    @DatabaseSetup(value = "/db/service/serialInventory/move-to-id-and-loc/update-status/from-hold-loc/before.xml")
    @ExpectedDatabase(value = "/db/service/serialInventory/move-to-id-and-loc/update-status/from-hold-loc/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void moveToLocAndIdWithStatusUpdateFromHoldLoc() {
        List<MovingItem> items = List.of(
                createMoveToLocAndIdItem("0001", "LOT1", "A-01", ""),
                createMoveToLocAndIdItem("0002", "LOT2", "A-01", "ID1"),
                createMoveToLocAndIdItem("0003", "LOT2", "A-01", "ID1"),
                createMoveToLocAndIdItem("0004", "LOT2", "A-02", "ID2")
        );
        ItrnCreateParams itrnCreateParams = createItrnParams();
        serialInventoryService.moveToLocAndIdWithLotLocIdStatusUpdate(items, "B-01", "ID3", itrnCreateParams,
                "TEST123");
    }

    /**
     * Проверка корректности работы в случае, если в lost перемещаются товары из разных партий
     */
    @Test
    @DatabaseSetup("/db/dao/serial-inventory/move-to-lost/before.xml")
    @ExpectedDatabase(value = "/db/dao/serial-inventory/move-to-lost/after.xml", assertionMode = NON_STRICT_UNORDERED)
    public void moveToLostFromDifferentLots() {
        List<MovingItem> items = new ArrayList<>();
        items.add(createTestMovingItem("0000012345", "ROV0000000000000000359",
                createSerialInventory("997010000801", false)));
        items.add(createTestMovingItem("0000012345", "ROV0000000000000000359",
                createSerialInventory("997010000802", false)));
        items.add(createTestMovingItem("0000012346", "ROV0000000000000000360",
                createSerialInventory("997020000801", false)));
        items.add(createTestMovingItem("0000012347", "ROV0000000000000000361",
                createSerialInventory("7030000801", false)));

        serialInventoryService.moveItemsToLost(items, createItrnParams(), "TEST123");
    }

    /**
     * Проверка корректности работы в случае, если в lost перемещаются безуитные товары
     */
    @Test
    @DatabaseSetup("/db/dao/serial-inventory/move-to-lost/fakes/before.xml")
    @ExpectedDatabase(value = "/db/dao/serial-inventory/move-to-lost/fakes/after.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void moveToLostFakeAndTrue() {
        List<MovingItem> items = new ArrayList<>();
        items.add(createTestMovingItem("0000012345", "ROV0000000000000000359",
                createSerialInventory("997010000801", false)));
        items.add(createTestMovingItem("0000012345", "ROV0000000000000000359",
                createSerialInventory("997010000802", true)));
        serialInventoryService.moveItemsToLost(items, createItrnParams(), "TEST123");
    }

    /**
     * Тест на корректность поиска нескольких партий по УИТам
     */
    @Test
    @DatabaseSetup("/db/dao/serial-inventory/find-lots-by-serial/before.xml")
    public void findLotsBySerialNumbersTest() {
        List<LotInputData> lotsInputData = new ArrayList<>();
        lotsInputData.add(createLotInput("SKU01", "1", "001"));
        lotsInputData.add(createLotInput("SKU02", "2", "002"));
        Map<LotInputData, LotCreationResult> result =
                serialInventoryService.findOrCreateLots(lotsInputData, "TEST123");
        assertEquals("1", result.get(lotsInputData.get(0)).getLot());
        assertEquals("0000000701", result.get(lotsInputData.get(1)).getLot());
    }

    /**
     * Тест на успешное перемещение УИТа с проверкой блокировок и проверкой перемещенных УИТов
     */
    @Test
    @DatabaseSetup("/db/service/serialInventory/move-to-loc-and-id-check-on-hold/all-ok/before.xml")
    @ExpectedDatabase(value = "/db/service/serialInventory/move-to-loc-and-id-check-on-hold/all-ok/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void moveInventoriesToLocAndIdWithCheckOnHoldAndCheckSerialMovedAllOk() {
        List<SerialInventory> inventories = serialInventoryDao.getByLoc("A-01");

        ItrnCreateParams itrnCreateParams = createItrnParams();
        serialInventoryService.moveInventoriesToLocAndIdWithCheckOnHold(inventories, "B-01", "ID3", itrnCreateParams,
                "TEST123", true);
    }

    /**
     * Тест на успешное перемещение УИТа с проверкой блокировок и без проверки перемещенных УИТов
     */
    @Test
    @DatabaseSetup("/db/service/serialInventory/move-to-loc-and-id-check-on-hold/all-ok/before.xml")
    @ExpectedDatabase(value = "/db/service/serialInventory/move-to-loc-and-id-check-on-hold/all-ok/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void moveInventoriesToLocAndIdWithCheckOnHoldWithoutCheckSerialMoved() {
        List<SerialInventory> inventories = serialInventoryDao.getByLoc("A-01");

        ItrnCreateParams itrnCreateParams = createItrnParams();
        serialInventoryService.moveInventoriesToLocAndIdWithCheckOnHold(inventories, "B-01", "ID3", itrnCreateParams,
                "TEST123", false);
    }

    /**
     * Тест на перемещение УИТа с проверкой блокировок и проверкой перемещенных УИТов, когда УИТы были ранее перемещены
     */
    @Test
    @DatabaseSetup("/db/service/serialInventory/move-to-loc-and-id-check-on-hold/serials-not-moved/before.xml")
    @ExpectedDatabase(
            value = "/db/service/serialInventory/move-to-loc-and-id-check-on-hold/serials-not-moved/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void moveInventoriesToLocAndIdWithCheckOnHoldAndCheckSerialMovedWhenSerialsNotMoved() {
        List<SerialInventory> inventories = serialInventoryDao.getByLoc("B-01");

        ItrnCreateParams itrnCreateParams = createItrnParams();
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                serialInventoryService.moveInventoriesToLocAndIdWithCheckOnHold(
                        inventories,
                        "B-01",
                        "ID3",
                        itrnCreateParams,
                        "TEST123",
                        true)
        );
        assertTrue(exception.getMessage()
                .contains("Какие-то из товаров уже перемещены"));
    }


    /**
     * Тест на перемещение УИТа с проверкой блокировок и без проверки перемещенных УИТов,
     * когда УИТы уже были ранее перемещены
     */
    @Test
    @DatabaseSetup("/db/service/serialInventory/move-to-loc-and-id-check-on-hold/serials-not-moved/before.xml")
    @ExpectedDatabase(
            value = "/db/service/serialInventory/move-to-loc-and-id-check-on-hold/serials-not-moved/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void moveInventoriesToLocAndIdWithCheckOnHoldWithoutCheckSerialMovedWhenSerialsNotMoved() {
        List<SerialInventory> inventories = serialInventoryDao.getByLoc("B-01");

        ItrnCreateParams itrnCreateParams = createItrnParams();
        serialInventoryService.moveInventoriesToLocAndIdWithCheckOnHold(inventories, "B-01", "ID3", itrnCreateParams,
                "TEST123", false);
    }

    /**
     * Тест перемещение УИТа c ячейки с блокировкой
     */
    @Test
    @DatabaseSetup("/db/service/serialInventory/move-to-loc-and-id-check-on-hold/from-hold-loc/before.xml")
    public void moveInventoriesToLocAndIdWithCheckOnHoldFromHoldLoc() {
        List<SerialInventory> inventories = serialInventoryDao.getByLoc("A-01");
        ItrnCreateParams itrnCreateParams = createItrnParams();

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                serialInventoryService.moveInventoriesToLocAndIdWithCheckOnHold(inventories,
                        "B-01",
                        "ID3",
                        itrnCreateParams,
                        "TEST123",
                        true)
        );
        assertTrue(exception.getMessage()
                .contains("Перемещение УИТ-ов c блокировоками не поддержено, локации [A-01], партии []"));
    }

    /**
     * Тест перемещение УИТа с партии с блокировкой
     */
    @Test
    @DatabaseSetup("/db/service/serialInventory/move-to-loc-and-id-check-on-hold/from-hold-lot/before.xml")
    public void moveInventoriesToLocAndIdWithCheckOnHoldFromHoldLot() {
        List<SerialInventory> inventories = serialInventoryDao.getByLoc("A-01");
        ItrnCreateParams itrnCreateParams = createItrnParams();

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                serialInventoryService.moveInventoriesToLocAndIdWithCheckOnHold(inventories,
                        "B-01",
                        "ID3",
                        itrnCreateParams,
                        "TEST123",
                        true)
        );
        assertTrue(exception.getMessage()
                .contains("Перемещение УИТ-ов c блокировоками не поддержено, локации [], партии [LOT2]"));
    }

    private MovingItem createTestMovingItem(String lot, String sku, SerialInventory serialInventory) {
        return MovingItem.builder()
                .fromId("PLT123")
                .fromLoc("STAGE01")
                .quantity(BigDecimal.ONE)
                .quantityPicked(BigDecimal.ZERO)
                .storerKey("465852")
                .lot(lot)
                .sku(sku)
                .serialInventory(serialInventory)
                .build();
    }

    private MovingItem createMoveToLocAndIdItem(String serialNumber, String lot, String fromLoc, String fromId) {
        return MovingItem.builder()
                .sku("SKU_1")
                .storerKey("STORER_1")
                .lot(lot)
                .fromId(fromId)
                .fromLoc(fromLoc)
                .serialInventory(createSerialInventory(serialNumber, false))
                .quantity(BigDecimal.ONE)
                .quantityPicked(BigDecimal.ZERO)
                .build();
    }

    private LotInputData createLotInput(String sku, String storerKey, String serialNumber) {
        SkuDimensions skuDimensions = SkuDimensions.builder()
                .cube(BigDecimal.ZERO)
                .grossWeight(BigDecimal.ZERO)
                .netWeight(BigDecimal.ZERO)
                .tareWeight(BigDecimal.ZERO)
                .build();
        return LotInputData.builder()
                .serialNumber(serialNumber)
                .sku(Sku.builder()
                        .sku(sku)
                        .storerKey(storerKey)
                        .dimensions(skuDimensions)
                        .build())
                .inventoryHoldStatuses(Collections.singleton(InventoryHoldStatus.OK))
                .surplus(false)
                .qty(BigDecimal.ONE)
                .creationDateTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0).toInstant(ZoneOffset.UTC))
                .build();
    }

    private ItrnCreateParams createItrnParams() {
        return ItrnCreateParams.builder()
                .generateSystemId()
                .transactionType("MV")
                .sourceKey("TestKey")
                .sourceType("1")
                .status("OK")
                .uom("EA")
                .uomCalc(0)
                .build();
    }

    private SerialInventory createSerialInventory(String serialNumber, Boolean isFake) {
        return SerialInventory.builder()
                .serialNumber(serialNumber)
                .storerKey("465852")
                .sku("ROV0000000000000001456")
                .lot("0000012345")
                .loc("STAGE")
                .id("")
                .quantity(new BigDecimal("1.00000"))
                .addWho("TEST")
                .editWho("TEST")
                .isFake(isFake)
                .build();
    }

}

