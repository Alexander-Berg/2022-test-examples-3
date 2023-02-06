package ru.yandex.market.sc.core.domain.zone;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.process.model.PartnerProcessDto;
import ru.yandex.market.sc.core.domain.process.repository.Process;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.zone.model.PartnerZoneDto;
import ru.yandex.market.sc.core.domain.zone.repository.ZoneMapper;
import ru.yandex.market.sc.core.exception.ScException;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author valter
 */
@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ZoneQueryServiceTest {

    private final ZoneQueryService zoneQueryService;
    private final ZoneMapper zoneMapper;
    private final TestFactory testFactory;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    void getZonesEmpty() {
        assertThat(zoneQueryService.getZones(sortingCenter)).isEmpty();
    }

    @Test
    void getZones() {
        var zone1 = testFactory.storedZone(sortingCenter, "1");
        var process = testFactory.storedProcess("111");
        var zone2 = testFactory.storedZone(sortingCenter, "2", List.of(process));
        assertThat(zoneQueryService.getZones(sortingCenter))
                .isEqualTo(
                        List.of(
                                zoneMapper.mapToPartner(zone1),
                                zoneMapper.mapToPartner(zone2)
                        )
                );
    }

    @Test
    void getZonesNoDeleted() {
        var zone = testFactory.storedZone(sortingCenter, "1");
        testFactory.storedDeletedZone(sortingCenter, "2");
        assertThat(zoneQueryService.getZones(sortingCenter))
                .isEqualTo(List.of(zoneMapper.mapToPartner(zone)));
    }

    @Test
    void getZoneWithoutProcesses() {
        var zone = testFactory.storedZone(sortingCenter, "1");
        assertThat(zoneMapper.mapToPartner(zoneQueryService.findZone(zone.getId(), sortingCenter)))
                .isEqualTo(zoneDto(zone.getId(), "1", false));
    }

    @Test
    void getZone() {
        var process = testFactory.storedProcess("111");
        var zone = testFactory.storedZone(sortingCenter, "1", List.of(process));
        assertThat(zoneMapper.mapToPartner(zoneQueryService.findZone(zone.getId(), sortingCenter)))
                .isEqualTo(
                        zoneDto(zone.getId(), "1", false, List.of(processDto(process)))
                );
    }

    @Test
    void getNotExisting() {
        assertThatThrownBy(() -> zoneMapper.mapToPartner(zoneQueryService.findZone(1L, sortingCenter)))
                .isInstanceOf(ScException.class);
    }

    @Test
    void getDeleted() {
        var zone = testFactory.storedDeletedZone(sortingCenter, "2");
        assertThat(zoneMapper.mapToPartner(zoneQueryService.findZone(zone.getId(), sortingCenter)))
                .isEqualTo(zoneDto(zone.getId(), "2", true));
    }


    private PartnerZoneDto zoneDto(long id, String name, boolean deleted, List<PartnerProcessDto> processes) {
        return new PartnerZoneDto(id, sortingCenter.getId(), name, deleted, processes);
    }

    private PartnerZoneDto zoneDto(long id, String name, boolean deleted) {
        return new PartnerZoneDto(id, sortingCenter.getId(), name, deleted, emptyList());
    }

    private PartnerProcessDto processDto(Process process) {
        return new PartnerProcessDto(process.getId(), process.getSystemName(), process.getDisplayName());
    }
}
