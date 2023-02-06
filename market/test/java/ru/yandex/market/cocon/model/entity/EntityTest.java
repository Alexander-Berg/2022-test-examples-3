package ru.yandex.market.cocon.model.entity;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.cocon.FunctionalTest;
import ru.yandex.market.cocon.model.CabinetType;
import ru.yandex.market.cocon.model.RuleType;
import ru.yandex.market.cocon.repository.BackendRepository;
import ru.yandex.market.cocon.repository.CabinetBackendRepository;
import ru.yandex.market.cocon.repository.CabinetDomainRepository;
import ru.yandex.market.cocon.repository.CabinetRepository;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DbUnitDataSet(before = "EntityTest.before.csv")
class EntityTest extends FunctionalTest {

    @Autowired
    private CabinetRepository cabinetRepository;

    @Autowired
    private BackendRepository backendRepository;

    @Autowired
    private CabinetDomainRepository cabinetDomainRepository;

    @Autowired
    private CabinetBackendRepository cabinetBackendRepository;

    @Test
    @DisplayName("Получение кабинета по ИД")
    void findCabinetById() {
        long id = 1;
        CabinetEntity expected = new CabinetEntity();
        expected.setId(id);
        expected.setType(CabinetType.SUPPLIER);
        Optional<CabinetEntity> optionalCabinetEntity = cabinetRepository.findById(id);
        assertTrue(optionalCabinetEntity.isPresent());
        assertEquals(expected, optionalCabinetEntity.get());
        assertEquals(expected.getType(), optionalCabinetEntity.get().getType());
    }

    @Test
    @DisplayName("Получение кабинета по типу")
    void findCabinetByType() {
        long id = 2;
        CabinetEntity expected = new CabinetEntity();
        expected.setId(id);
        expected.setType(CabinetType.SHOP);
        Optional<CabinetEntity> optionalCabinetEntity = cabinetRepository.findByType(CabinetType.SHOP);
        assertTrue(optionalCabinetEntity.isPresent());
        assertEquals(expected, optionalCabinetEntity.get());
        assertEquals(expected.getType(), optionalCabinetEntity.get().getType());

    }

    @Test
    @DisplayName("Получение бекенда по ИД")
    void findBackendById() {
        long id = 1;
        BackendEntity expected = new BackendEntity();
        expected.setId(id);
        expected.setClientId("123");
        expected.setDatesourcesKey("datasourcesKey");
        expected.setPath("path");
        Optional<BackendEntity> optionalBackendEntity = backendRepository.findById(id);
        assertTrue(optionalBackendEntity.isPresent());
        assertEquals(expected, optionalBackendEntity.get());
    }

    @Test
    @DisplayName("Получение связи между кабинетом и доменом по типу правила")
    void getCabinetDomain() {
        String expected = "domain1";
        CabinetEntity cabinetEntity = cabinetRepository.getOne(1L);
        Optional<CabinetDomainEntity> cabinetDomainEntity = cabinetDomainRepository.
                findByCabinetAndRuleType(cabinetEntity, RuleType.STATES);
        assertTrue(cabinetDomainEntity.isPresent());
        assertEquals(expected, cabinetDomainEntity.get().getDomain());
        assertEquals(RuleType.STATES, cabinetDomainEntity.get().getRuleType());

    }


    @Test
    @DisplayName("Получение связи между кабинетом и бэкендом по домену и чекеру")
    void getCabinetBackendByCabinetAndDomainAndChecker() {
        BackendEntity expected = new BackendEntity();
        expected.setId(1);
        expected.setClientId("123");
        expected.setDatesourcesKey("datasourcesKey");
        expected.setPath("path");
        CabinetEntity cabinetEntity = cabinetRepository.getOne(1L);
        Optional<CabinetBackendEntity> cabinetBackendEntity =
                cabinetBackendRepository.findCabinetBackendEntityByCabinetAndDomainAndChecker(cabinetEntity,
                        "domain1", "checker1");
        assertTrue(cabinetBackendEntity.isPresent());
        assertEquals(expected, cabinetBackendEntity.get().getBackend());
    }
}
