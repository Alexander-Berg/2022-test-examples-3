package ru.yandex.market.wms.common.spring.dao;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ru.yandex.market.wms.common.dao.LogisticUnitDAO;
import ru.yandex.market.wms.common.model.dto.LogisticUnitDTO;
import ru.yandex.market.wms.common.model.dto.OutboundRegister;
import ru.yandex.market.wms.common.model.dto.RegisterUnit;
import ru.yandex.market.wms.common.model.enums.LogisticUnitStatus;
import ru.yandex.market.wms.common.model.enums.LogisticUnitType;
import ru.yandex.market.wms.common.model.enums.OutboundRegisterType;
import ru.yandex.market.wms.common.model.enums.RegisterUnitType;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig;
import ru.yandex.market.wms.common.spring.config.LostWriteOffServiceTestConfig;
import ru.yandex.market.wms.common.spring.dao.implementation.OutboundRegisterDao;
import ru.yandex.market.wms.common.spring.exception.DuplicateLogisticUnitException;
import ru.yandex.market.wms.common.spring.utils.uuid.UuidGenerator;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest(classes = {IntegrationTestConfig.class, LostWriteOffServiceTestConfig.class})
public class OutboundRegisterDaoTest extends IntegrationTest {

    @Autowired
    private OutboundRegisterDao dao;

    @Autowired
    private LogisticUnitDAO logisticUnitDAO;

    @Autowired
    private UuidGenerator uuidGenerator;
    @Autowired
    private Clock clock;
    @Autowired
    ObjectMapper objectMapper;

    @Test
    @ExpectedDatabase(value = "/db/dao/logistic-unit/db.xml", assertionMode = NON_STRICT_UNORDERED)
    public void insertRegister() {
        String uuid = uuidGenerator.generate().toString();
        Instant now = clock.instant();
        String user = "TEST";
        String key = "EXTERN_1";
        OutboundRegister doc = OutboundRegister.builder()
                .registerKey(uuid)
                .externRegisterKey(uuid)
                .externOrderKey(key)
                .addDate(now)
                .editDate(now)
                .addWho(user)
                .editWho(user)
                .type(OutboundRegisterType.FACTUAL)
                .build();

        List<LogisticUnitDTO> units = new ArrayList<>();
        units.add(createUnit("STORER_1", "SKU_1", 1, uuid));
        units.add(createUnit("STORER_1", "SKU_2", 2, uuid));
        units.add(createUnit("STORER_2", "SKU_3", 1, uuid));
        dao.insertOutboundRegister(doc);
        logisticUnitDAO.insertLogisticUnits(units, clock.instant(), user);
    }

    @Test
    @DatabaseSetup("/db/dao/logistic-unit/sku-part.xml")
    @DatabaseSetup("/db/dao/logistic-unit/db.xml")
    public void getRegister() {
        Collection<OutboundRegister> outbound = dao.getOutboundRegisters("EXTERN_1");
        assertions.assertThat(outbound).isNotNull();
        assertions.assertThat(outbound.size()).isEqualTo(1);

        List<RegisterUnit> items = outbound.stream().findFirst().get().getRegisterUnits();
        assertions.assertThat(items.size()).isEqualTo(3);

        List<String> skus = items.stream().map(a -> a.getSku()).distinct().collect(Collectors.toList());
        List<String> manskus = items.stream().map(a -> a.getManufacturerSku()).distinct().collect(Collectors.toList());
        List<String> storers = items.stream().map(a -> a.getStorerKey()).distinct().collect(Collectors.toList());
        assertions.assertThat(skus).containsExactlyInAnyOrder("SKU_1", "SKU_2", "SKU_3");
        assertions.assertThat(manskus).containsExactlyInAnyOrder("MSKU_1", "MSKU_2", "MSKU_3");
        assertions.assertThat(storers).containsExactlyInAnyOrder("STORER_1", "STORER_2");
    }

    @Test
    @DatabaseSetup("/db/dao/logistic-unit/logistic-unit-sku-empty.xml")
    public void getOutboundRegisters_logisticUnitSkuFieldEmpty() {
        Collection<OutboundRegister> outbound = dao.getOutboundRegisters("EXTERN_1");
        assertions.assertThat(outbound).isNotNull();
        assertions.assertThat(outbound.size()).isEqualTo(1);

        List<RegisterUnit> items = outbound.stream().findFirst().get().getRegisterUnits();
        assertions.assertThat(items.size()).isEqualTo(1);

        RegisterUnit registerUnit = items.stream().findAny().get();
        assertions.assertThat(registerUnit.getSku()).isNull();
        assertions.assertThat(registerUnit.getStorerKey()).isEmpty();
    }

    @Test
    @DatabaseSetup("/db/dao/logistic-unit/logistic-unit-single-cargo.xml")
    public void getOutboundRegisters_singleSku_singleCargo() {
        Collection<OutboundRegister> outbound = dao.getOutboundRegisters("EXTERN_1");
        assertions.assertThat(outbound).isNotNull();
        assertions.assertThat(outbound.size()).isEqualTo(1);

        List<RegisterUnit> items = outbound.stream().findFirst().get().getRegisterUnits();
        assertions.assertThat(items.size()).isEqualTo(1);

        RegisterUnit registerUnit = items.stream().findAny().get();
        assertions.assertThat(registerUnit.getSku()).isEqualTo("SKU_1");
        assertions.assertThat(registerUnit.getStorerKey()).isEqualTo("STORER_1");
        List<Integer> cargoTypes = registerUnit.getCargoTypes();
        assertions.assertThat(cargoTypes).hasOnlyOneElementSatisfying(c -> assertions.assertThat(c).isEqualTo(100));
    }

    @Test
    @DatabaseSetup("/db/dao/logistic-unit/logistic-unit-multiple-cargo.xml")
    public void getOutboundRegisters_singleSku_multipleCargo() {
        Collection<OutboundRegister> outbound = dao.getOutboundRegisters("EXTERN_1");
        assertions.assertThat(outbound).isNotNull();
        assertions.assertThat(outbound.size()).isEqualTo(1);

        List<RegisterUnit> items = outbound.stream().findFirst().get().getRegisterUnits();
        assertions.assertThat(items.size()).isEqualTo(1);

        RegisterUnit registerUnit = items.stream().findAny().get();
        assertions.assertThat(registerUnit.getSku()).isEqualTo("SKU_1");
        assertions.assertThat(registerUnit.getStorerKey()).isEqualTo("STORER_1");
        assertions.assertThat(registerUnit.getAmount()).isEqualTo(1);
        List<Integer> cargoTypes = registerUnit.getCargoTypes();
        assertions.assertThat(cargoTypes).anySatisfy(c -> assertions.assertThat(c).isEqualTo(100));
        assertions.assertThat(cargoTypes).anySatisfy(c -> assertions.assertThat(c).isEqualTo(120));
    }

    @Test
    @DatabaseSetup("/db/dao/logistic-unit/logistic-unit-multiple-sku-multiple-cargo.xml")
    public void getOutboundRegisters_multipleSkus_multipleCargo() {
        Collection<OutboundRegister> outbound = dao.getOutboundRegisters("EXTERN_1");
        assertions.assertThat(outbound).isNotNull();
        assertions.assertThat(outbound.size()).isEqualTo(1);

        List<RegisterUnit> items = outbound.stream().findFirst().get().getRegisterUnits();
        assertions.assertThat(items.size()).isEqualTo(2);

        RegisterUnit registerUnit = items.stream().filter(unit -> unit.getSku().equals("SKU_1")).findAny().get();
        RegisterUnit registerUnit1 = items.stream().filter(unit -> unit.getSku().equals("SKU_2")).findAny().get();
        assertions.assertThat(registerUnit.getSku()).isEqualTo("SKU_1");
        assertions.assertThat(registerUnit.getStorerKey()).isEqualTo("STORER_1");
        assertions.assertThat(registerUnit1.getSku()).isEqualTo("SKU_2");
        assertions.assertThat(registerUnit1.getStorerKey()).isEqualTo("STORER_2");
        List<Integer> cargoTypes = registerUnit.getCargoTypes();
        List<Integer> cargoTypes1 = registerUnit1.getCargoTypes();
        assertions.assertThat(cargoTypes).anySatisfy(c -> assertions.assertThat(c).isEqualTo(100));
        assertions.assertThat(cargoTypes).anySatisfy(c -> assertions.assertThat(c).isEqualTo(120));
        assertions.assertThat(cargoTypes1).anySatisfy(c -> assertions.assertThat(c).isEqualTo(140));
        assertions.assertThat(cargoTypes1).anySatisfy(c -> assertions.assertThat(c).isEqualTo(160));
    }

    private LogisticUnitDTO createUnit(String storer, String sku, int amount, String registerKey) {
        return LogisticUnitDTO.builder()
                .sku(sku)
                .storerKey(storer)
                .receiptKey("RKEY_1")
                .status(LogisticUnitStatus.NEW)
                .count(amount)
                .unitKey(sku)
                .type(LogisticUnitType.ITEM)
                .registerKey(registerKey)
                .externalOrderKey("EXTERN_1")
                .build();
    }


    @Test
    @DatabaseSetup("/db/dao/outbound-register/get-registers/db.xml")
    @ExpectedDatabase(value = "/db/dao/outbound-register/get-registers/db.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getOutboundRegistersTest() {
        List<OutboundRegister> expected = new ArrayList<>();
        List<RegisterUnit> units = new ArrayList<>();
        units.add(makeRegisterUnit(null, "000123", "unit1", "unit2",
                null, RegisterUnitType.BOX, 1));
        units.add(makeRegisterUnit(null, null, "unit2", null,
                null, RegisterUnitType.PALLET, 2));
        expected.add(makeRegister("registerKey1", "externKey1", "outbound-1",
                units, null, null));
        List<RegisterUnit> units2 = new ArrayList<>();
        units2.add(makeRegisterUnit(null, null, "unit3", null,
                null, RegisterUnitType.PALLET, 3));
        expected.add(makeRegister("registerKey2", "externKey2", "outbound-1",
                units2, null, null));
        List<OutboundRegister> registers = new ArrayList<>(dao.getOutboundRegisters("outbound-1"));
        Assertions.assertIterableEquals(expected, registers);
    }

    @Test
    @ExpectedDatabase(value = "/db/dao/outbound-register/insert-register/after-insert.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void insertRegisterTest() {
        List<RegisterUnit> units = new ArrayList<>();
        units.add(makeRegisterUnit("key1", "000123", "unit1", "unit2",
                "NEW", RegisterUnitType.BOX, null));
        units.add(makeRegisterUnit("key1", null, "unit2", null,
                "NEW", RegisterUnitType.PALLET, null));
        OutboundRegister register = makeRegister("key1", "key2", "orderKey1",
                units, "test", "test1");
        dao.insertRegisterWithUnits(register);
    }

    @Test
    @DatabaseSetup("/db/dao/logistic-unit/logistic-unit-duplicated-before.xml")
    @ExpectedDatabase(value = "/db/dao/logistic-unit/logistic-unit-duplicated-before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void insertLogisticUnitsDuplicated() {
        String uuid = uuidGenerator.generate().toString();
        String user = "TEST";

        List<LogisticUnitDTO> units = new ArrayList<>();
        units.add(createUnit("STORER_1", "SKU_1", 1, uuid));
        units.add(createUnit("STORER_1", "SKU_2", 2, uuid));
        units.add(createUnit("STORER_2", "SKU_3", 1, uuid));
        DuplicateLogisticUnitException duplicateLogisticUnitException =
                assertThrows(DuplicateLogisticUnitException.class, () -> logisticUnitDAO.insertLogisticUnits(units,
                        clock.instant(), user));
        String actualMessage = duplicateLogisticUnitException.getMessage();
        Assertions.assertNotNull(actualMessage);
        Assertions.assertTrue(actualMessage.contains(
                "(order: EXTERN_1, box: SKU_1),(order: EXTERN_1, box: SKU_2),(order: EXTERN_1, box: SKU_3)"));
    }

    @Test
    @DatabaseSetup("/db/dao/logistic-unit/logistic-unit-duplicated-cancelled-before.xml")
    @ExpectedDatabase(value = "/db/dao/logistic-unit/logistic-unit-duplicated-cancelled-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void insertLogisticUnitsDuplicated_butCancelledReceipt() {
        String uuid = uuidGenerator.generate().toString();
        String user = "TEST";

        List<LogisticUnitDTO> units = new ArrayList<>();
        units.add(createUnit("STORER_1", "SKU_1", 1, uuid));
        units.add(createUnit("STORER_1", "SKU_2", 2, uuid));
        units.add(createUnit("STORER_2", "SKU_3", 1, uuid));
        logisticUnitDAO.insertLogisticUnits(units, clock.instant(), user);
    }

    @Test
    @DatabaseSetup("/db/dao/logistic-unit/logistic-unit-duplicated-closed-before.xml")
    @ExpectedDatabase(value = "/db/dao/logistic-unit/logistic-unit-duplicated-closed-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void insertLogisticUnitsDuplicated_butClosedReceipt() {
        String uuid = uuidGenerator.generate().toString();
        String user = "TEST";

        List<LogisticUnitDTO> units = new ArrayList<>();
        units.add(createUnit("STORER_1", "SKU_1", 1, uuid));
        units.add(createUnit("STORER_1", "SKU_2", 2, uuid));
        units.add(createUnit("STORER_2", "SKU_3", 1, uuid));
        logisticUnitDAO.insertLogisticUnits(units, clock.instant(), user);
    }

    @Test
    @DatabaseSetup("/db/dao/logistic-unit/logistic-unit-duplicated-closed-before.xml")
    @ExpectedDatabase(value = "/db/dao/logistic-unit/logistic-unit-duplicated-closed-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void insertLogisticUnitsDuplicated_butVerifiedClosedReceipt() {
        String uuid = uuidGenerator.generate().toString();
        String user = "TEST";

        List<LogisticUnitDTO> units = new ArrayList<>();
        units.add(createUnit("STORER_1", "SKU_1", 1, uuid));
        units.add(createUnit("STORER_1", "SKU_2", 2, uuid));
        units.add(createUnit("STORER_2", "SKU_3", 1, uuid));
        logisticUnitDAO.insertLogisticUnits(units, clock.instant(), user);
    }



    private static RegisterUnit makeRegisterUnit(
            String registerKey, String externKey, String unitKey, String parentUnitKey, String status,
            RegisterUnitType type, Integer serialKey) {
        return RegisterUnit.builder()
                .registerKey(registerKey)
                .externOrderKey(externKey)
                .unitKey(unitKey)
                .parentUnitKey(parentUnitKey)
                .amount(1)
                .status(status)
                .type(type)
                .storerKey("")
                .serialKey(serialKey)
                .build();
    }

    private static OutboundRegister makeRegister(
            String registerKey,
            String externRegisterKey,
            String externOrderKey,
            List<RegisterUnit> registerUnits,
            String addWho,
            String editWho) {
        return OutboundRegister.builder()
                .type(OutboundRegisterType.FACTUAL)
                .registerKey(registerKey)
                .externRegisterKey(externRegisterKey)
                .externOrderKey(externOrderKey)
                .addWho(addWho)
                .editWho(editWho)
                .addDate(Instant.parse("2020-12-12T14:00:00.000Z"))
                .registerUnits(registerUnits)
                .build();
    }
}
