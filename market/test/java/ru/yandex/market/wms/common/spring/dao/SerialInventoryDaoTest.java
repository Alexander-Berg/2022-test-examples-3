package ru.yandex.market.wms.common.spring.dao;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.SerialInventory;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.implementation.SerialInventoryDao;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

public class SerialInventoryDaoTest extends IntegrationTest {

    @Autowired
    private SerialInventoryDao serialInventoryDao;
    @Autowired
    private Clock clock;

    @Test
    @DatabaseSetup("/db/empty-db.xml")
    @ExpectedDatabase(value = "/db/dao/serial-inventory/after-create.xml", assertionMode = NON_STRICT)
    public void insert() {
        serialInventoryDao
                .insert(Collections.singletonList(createSerialInventory("3455505393")), LocalDateTime.now(clock));
    }

    @Test
    @DatabaseSetup("/db/dao/serial-inventory/before-select.xml")
    @ExpectedDatabase(value = "/db/dao/serial-inventory/before-select.xml", assertionMode = NON_STRICT)
    public void findBySerialNumberWhenItExists() {
        List<SerialInventory> maybeSerialInventory = serialInventoryDao.findAllBySerialNumbers(
                Collections.singletonList("3455505395"));
        assertions.assertThat(maybeSerialInventory).isNotEmpty();
        List<SerialInventory> expectedSerialInventory = Collections.singletonList(createSerialInventory("3455505395"));
        assertions.assertThat(maybeSerialInventory).isEqualTo(expectedSerialInventory);
    }

    @Test
    @DatabaseSetup("/db/dao/serial-inventory/before-select.xml")
    @ExpectedDatabase(value = "/db/dao/serial-inventory/before-select.xml", assertionMode = NON_STRICT)
    public void findBySerialNumberWhenItNotExists() {
        List<SerialInventory> maybeSerialInventory = serialInventoryDao.findAllBySerialNumbers(
                Collections.singletonList("3455505396"));
        assertions.assertThat(maybeSerialInventory).isEmpty();
    }

    @Test
    @DatabaseSetup("/db/dao/serial-inventory-lost/lost-db.xml")
    public void findAllFixLostSerialNumbers() {
        List<String> fixLosts = serialInventoryDao.getAllFixLost().stream()
                .map(SerialInventory::getSerialNumber)
                .collect(Collectors.toList());
        assertions.assertThat(fixLosts).containsExactlyInAnyOrder("SERIAL_2", "SERIAL_5");
    }

    @Test
    @DatabaseSetup("/db/empty-db.xml")
    public void notFoundfindBySerialNumberWhenItExists() {
        List<SerialInventory> fixLosts = serialInventoryDao.getAllFixLost();
        assertions.assertThat(fixLosts).isEmpty();
    }

    @Test
    @DatabaseSetup("/db/dao/serial-inventory/fake-random/find-1.xml")
    public void findRandomFakeByIdLocAndSkuTest() {
        var serials = serialInventoryDao.findRandomFakeByIdLocAndSku(SkuId.of("STORER", "SKU1"), "LOC1", "ID01", 4);
        assertions.assertThat(serials.stream().map(SerialInventory::getSerialNumber).toList())
                .containsExactlyInAnyOrderElementsOf(List.of("001", "002"));
    }

    @Test
    @DatabaseSetup("/db/dao/serial-inventory/fake-random/find-1.xml")
    public void findRandomFakeByIdLocAndSkuEmptyTest() {
        var serials = serialInventoryDao.findRandomFakeByIdLocAndSku(SkuId.of("STORER", "SKU1"), "LOC2", "ID01", 4);
        assertions.assertThat(serials.isEmpty()).isTrue();
    }

    @Test
    @DatabaseSetup("/db/dao/serial-inventory/fake-random/find-1.xml")
    public void countFakeTypesByIdLocAndSkuIdTest() {
        var serialsCount = serialInventoryDao.countFakeTypesByIdLocAndSkuId(SkuId.of("STORER", "SKU1"), "LOC1", "ID01");
        assertions.assertThat(serialsCount).isEqualTo(2);
        var serialsCount2 = serialInventoryDao.countFakeTypesByIdLocAndSkuId(SkuId.of("STORER", "SKU2"), "PACK", "");
        assertions.assertThat(serialsCount2).isEqualTo(2);
        var serialsCount3 = serialInventoryDao.countFakeTypesByIdLocAndSkuId(SkuId.of("STORER", "SKU2"), "PACK1", "");
        assertions.assertThat(serialsCount3).isEqualTo(1);
        var serialsCount4 = serialInventoryDao.countFakeTypesByIdLocAndSkuId(SkuId.of("STORER", "SKU7"), "NONE", "");
        assertions.assertThat(serialsCount4).isEqualTo(0);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 5})
    @DatabaseSetup("/db/dao/serial-inventory/fake-random/find-fair.xml")
    public void findRandomFakeByIdLocAndSkuFairTest(int sample) {

        int serialsCount = 10;
        int steps = (serialsCount * 100) / sample;
        var map = new HashMap<String, Integer>();
        for (int i = 0; i < steps; i++) {
            for (var serial : serialInventoryDao
                    .findRandomFakeByIdLocAndSku(SkuId.of("STORER", "SKU1"), "LOC1", "ID01", sample)) {
                map.compute(serial.getSerialNumber(), (k, v) -> (v == null) ? 1 : v + 1);
            }
        }
        assertions.assertThat(map.keySet().size()).isEqualTo(serialsCount);
    }

    private SerialInventory createSerialInventory(String serialNumber) {
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
                .isFake(false)
                .build();
    }
}
