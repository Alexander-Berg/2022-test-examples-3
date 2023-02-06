package ru.yandex.market.ff.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RegistryUnitType;
import ru.yandex.market.ff.client.enums.UnitCountType;
import ru.yandex.market.ff.model.entity.registry.RegistryUnitEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RegistryUnitJdbcRepositoryTest extends IntegrationTest {

    @Autowired
    private RegistryUnitJdbcRepository registryUnitJdbcRepository;


    @Test
    @DatabaseSetup("classpath:service/pdf-report/requests.xml")
    void findByRegistryIdTypeAndArticlesNotNull() {
        List<String> articles =
                List.of("00100.microwave3", "00100.microwave5", "00100.televizzzor2", "00100.televizzzor2.2021");
        Map<String, List<RegistryUnitEntity>> registryUnitEntities =
                registryUnitJdbcRepository
                        .findByRegistryIdTypeAndArticles(List.of(1L), RegistryUnitType.ITEM, articles);

        assertNotNull(registryUnitEntities);
        assertNotNull(registryUnitEntities.get("00100.microwave3"));
        assertNotNull(registryUnitEntities.get("00100.microwave3").get(0).getIdentifiers());
        assertNotNull(registryUnitEntities.get("00100.microwave3").get(0).getUnitCountsInfo());

        assertEquals(3, registryUnitEntities.size());
        assertEquals(6, registryUnitEntities.get("00100.microwave3").size());
        assertEquals(3, registryUnitEntities.get("00100.microwave5").size());
        assertEquals(2, registryUnitEntities.get("00100.televizzzor2").size());
    }

    @Test
    @DatabaseSetup("classpath:service/pdf-report/requests.xml")
    void findByRegistryIdTypeAndCountTypeNotNull() {
        List<RegistryUnitEntity> registryUnitEntities =
                registryUnitJdbcRepository.findByRegistryIdTypeAndInCountType(List.of(1L), RegistryUnitType.ITEM,
                        Set.of(UnitCountType.UNDEFINED), null);

        assertNotNull(registryUnitEntities);
        assertNotNull(registryUnitEntities.get(0).getIdentifiers());
        assertNotNull(registryUnitEntities.get(0).getUnitCountsInfo());
        assertEquals(3, registryUnitEntities.size());
    }

    @Test
    @DatabaseSetup("classpath:service/pdf-report/requests.xml")
    void findByRegistryIdTypeAndCountTypePageableWorks() {
        List<RegistryUnitEntity> registryUnitEntities =
                registryUnitJdbcRepository.findByRegistryIdTypeAndInCountType(List.of(1L), RegistryUnitType.ITEM,
                        Set.of(UnitCountType.UNDEFINED), new PageRequest(2, 1));

        assertNotNull(registryUnitEntities);
        assertNotNull(registryUnitEntities.get(0).getIdentifiers());
        assertNotNull(registryUnitEntities.get(0).getUnitCountsInfo());
        assertEquals(1, registryUnitEntities.size());
        assertEquals(25, registryUnitEntities.get(0).getId());
    }


}
