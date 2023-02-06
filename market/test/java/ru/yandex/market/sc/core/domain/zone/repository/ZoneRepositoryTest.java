package ru.yandex.market.sc.core.domain.zone.repository;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.process.repository.Process;
import ru.yandex.market.sc.core.domain.process.repository.ProcessRepository;
import ru.yandex.market.sc.core.domain.zone.ZoneType;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author valter
 */
@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ZoneRepositoryTest {

    private final ZoneRepository zoneRepository;
    private final ProcessRepository processRepository;
    private final TestFactory testFactory;

    @Test
    void save() {
        var process = processRepository.save(new Process().setDisplayName("display").setSystemName("system"));
        var zone = zoneRepository.save(new Zone(testFactory.storedSortingCenter(), "z1", ZoneType.DEFAULT,
                List.of(process), null));
        zone = zoneRepository.findZoneById(zone.getId()).orElseThrow();
        assertThat(zone.getType()).isEqualTo(ZoneType.DEFAULT);
        assertThat(zone).isEqualToIgnoringGivenFields(zone, "id", "createdAt", "updatedAt");
    }

}
