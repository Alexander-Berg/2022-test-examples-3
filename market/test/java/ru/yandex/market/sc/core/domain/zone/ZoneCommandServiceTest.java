package ru.yandex.market.sc.core.domain.zone;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.process.model.PartnerProcessDto;
import ru.yandex.market.sc.core.domain.process.repository.ProcessRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.zone.model.PartnerZoneDto;
import ru.yandex.market.sc.core.domain.zone.model.PartnerZoneRequestDto;
import ru.yandex.market.sc.core.domain.zone.repository.ZoneMapper;
import ru.yandex.market.sc.core.domain.zone.repository.ZoneRepository;
import ru.yandex.market.sc.core.exception.ScException;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author valter
 */
@EmbeddedDbTest
class ZoneCommandServiceTest {

    @Autowired
    ZoneCommandService zoneCommandService;
    @Autowired
    ZoneRepository zoneRepository;
    @Autowired
    ProcessRepository processRepository;
    @Autowired
    TestFactory testFactory;
    @Autowired
    ZoneMapper zoneMapper;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    void create() {
        var processIds = List.of(testFactory.storedProcess("111").getId());
        var zone = zoneMapper.mapToPartner(zoneCommandService.createZone(sortingCenter, request("1", processIds)));
        assertThat(zone).isEqualToIgnoringGivenFields(zoneDto("1"), "id", "processes");
        assertThat(zone.getProcesses())
                .usingElementComparatorIgnoringFields("id")
                .isEqualTo(List.of(processDto("111")));
    }

    @Test
    void createWithoutProcesses() {
        assertThat(zoneMapper.mapToPartner(zoneCommandService.createZone(sortingCenter, request("1"))))
                .isEqualToIgnoringGivenFields(zoneDto("1"), "id");
    }

    @Test
    void createWithEmptyName() {
        assertThatThrownBy(() -> zoneCommandService.createZone(sortingCenter, request(" ")))
                .isInstanceOf(ScException.class);
    }

    @Test
    void createWithExistingName() {
        testFactory.storedZone(sortingCenter, "1");
        assertThatThrownBy(() -> zoneCommandService.createZone(sortingCenter, request("1")))
                .isInstanceOf(ScException.class);
    }

    @Test
    void createWithDeletedName() {
        testFactory.storedDeletedZone(sortingCenter, "1");
        assertThat(zoneMapper.mapToPartner(zoneCommandService.createZone(sortingCenter, request("1"))))
                .isEqualToIgnoringGivenFields(zoneDto("1"), "id");
    }

    @Test
    void update() {
        var processIds1 = List.of(testFactory.storedProcess("111").getId());
        var zone = zoneCommandService.createZone(sortingCenter, request("1", processIds1));
        var processIds2 = List.of(testFactory.storedProcess("222").getId());
        var updateZone = zoneCommandService.updateZone(sortingCenter, zone.getId(), request("2", processIds2));
        assertThat(zoneMapper.mapToPartner(updateZone))
                .isEqualToIgnoringGivenFields(zoneDto("2"), "id", "processes");
        assertThat(zoneMapper.mapToPartner(updateZone).getProcesses())
                .usingElementComparatorIgnoringFields("id")
                .isEqualTo(List.of(processDto("222")));
        var processIds3 = List.of(processIds1.get(0), processIds2.get(0));
        updateZone = zoneCommandService.updateZone(sortingCenter, zone.getId(), request("2", processIds3));
        assertThat(zoneMapper.mapToPartner(updateZone).getProcesses())
                .usingElementComparatorIgnoringFields("id")
                .containsExactlyInAnyOrder(processDto("111"), processDto("222"));
    }

    @Test
    void updateWithEmptyProcesses() {
        var processIds1 = List.of(testFactory.storedProcess("111").getId());
        var zone = zoneCommandService.createZone(sortingCenter, request("1", processIds1));
        var updateZone = zoneCommandService.updateZone(sortingCenter, zone.getId(), request("2", emptyList()));
        assertThat(zoneMapper.mapToPartner(updateZone))
                .isEqualToIgnoringGivenFields(zoneDto("2"), "id", "processes");
        assertThat(updateZone.getProcesses()).isEmpty();
    }

    @Test
    void updateNotExisting() {
        assertThatThrownBy(() -> zoneCommandService.updateZone(sortingCenter, 1L, request("2")))
                .isInstanceOf(TplEntityNotFoundException.class);
    }

    @Test
    void updateWithEmptyName() {
        var zone = testFactory.storedZone(sortingCenter, "1");
        assertThatThrownBy(() -> zoneCommandService.updateZone(sortingCenter, zone.getId(), request(" ")))
                .isInstanceOf(ScException.class);
    }

    @Test
    void updateWithSameName() {
        var zone = testFactory.storedZone(sortingCenter, "1");
        assertThat(zoneMapper.mapToPartner(zoneCommandService.updateZone(sortingCenter, zone.getId(), request("1"))))
                .isEqualTo(zoneDto(zone.getId(), "1", false));
    }

    @Test
    void updateWithExistingName() {
        var zone1 = testFactory.storedZone(sortingCenter, "1");
        testFactory.storedZone(sortingCenter, "2");
        assertThatThrownBy(() -> zoneCommandService.updateZone(sortingCenter, zone1.getId(), request("2")))
                .isInstanceOf(ScException.class);
    }

    @Test
    void updateWithDeletedName() {
        var zone1 = testFactory.storedZone(sortingCenter, "1");
        testFactory.storedDeletedZone(sortingCenter, "2");
        assertThat(zoneMapper.mapToPartner(zoneCommandService.updateZone(sortingCenter, zone1.getId(), request("2"))))
                .isEqualToIgnoringGivenFields(zoneDto("2"), "id");
    }

    @Test
    void updateDeleted() {
        var zone = testFactory.storedDeletedZone(sortingCenter, "1");
        assertThatThrownBy(() -> zoneCommandService.updateZone(sortingCenter, zone.getId(), request("2")))
                .isInstanceOf(ScException.class);
    }

    @Test
    void delete() {
        var zone = testFactory.storedZone(sortingCenter, "1");
        assertThat(zoneMapper.mapToPartner(zoneCommandService.deleteZone(sortingCenter, zone.getId())))
                .isEqualTo(zoneDto(zone.getId(), "1", true));
    }

    @Test
    void deleteNotExisting() {
        assertThatThrownBy(() -> zoneCommandService.deleteZone(sortingCenter, 1L))
                .isInstanceOf(TplEntityNotFoundException.class);
    }

    @Test
    void deleteWithExistingCells() {
        var zone = testFactory.storedZone(sortingCenter, "1");
        testFactory.storedCell(sortingCenter, zone);
        assertThat(zoneMapper.mapToPartner(zoneCommandService.deleteZone(sortingCenter, zone.getId())))
                .isEqualTo(zoneDto(zone.getId(), "1", true));

        Set<Long> usedZoneIds = zoneRepository.findAllUsedZones(zone.getSortingCenter().getId());
        assertThat(usedZoneIds.contains(zone.getId())).isFalse();
    }

    @Test
    void deleteWithExistingProcesses() {
        var processIds = List.of(testFactory.storedProcess("111").getId());
        var zone = zoneCommandService.createZone(sortingCenter, request("1", processIds));
        var deletedZone = zoneCommandService.deleteZone(sortingCenter, zone.getId());
        assertThat(zoneMapper.mapToPartner(deletedZone))
                .isEqualToIgnoringGivenFields(zoneDto(zone.getId(), "1", true), "processes");
        assertThat(zoneMapper.mapToPartner(deletedZone).getProcesses())
                .usingElementComparatorIgnoringFields("id")
                .isEqualTo(List.of(processDto("111")));
    }

    @Test
    void deleteWithDeletedCells() {
        var zone = testFactory.storedZone(sortingCenter, "1");
        testFactory.storedDeletedCell(sortingCenter, zone);
        assertThat(zoneMapper.mapToPartner(zoneCommandService.deleteZone(sortingCenter, zone.getId())))
                .isEqualTo(zoneDto(zone.getId(), "1", true));
    }

    @Test
    void deleteDeleted() {
        var zone = testFactory.storedDeletedZone(sortingCenter, "1");
        assertThat(zoneMapper.mapToPartner(zoneCommandService.deleteZone(sortingCenter, zone.getId())))
                .isEqualTo(zoneDto(zone.getId(), "1", true));
    }

    private PartnerZoneRequestDto request(String name) {
        return new PartnerZoneRequestDto(name, emptyList());
    }

    private PartnerZoneRequestDto request(String name, List<Long> processIds) {
        return new PartnerZoneRequestDto(name, processIds);
    }

    private PartnerZoneDto zoneDto(String name) {
        return zoneDto(1L, name, false);
    }

    private PartnerZoneDto zoneDto(long id, String name, boolean deleted) {
        return zoneDto(id, name, deleted, emptyList());
    }

    private PartnerZoneDto zoneDto(long id, String name, boolean deleted, List<PartnerProcessDto> processes) {
        return new PartnerZoneDto(id, sortingCenter.getId(), name, deleted, processes);
    }

    private PartnerProcessDto processDto(String name) {
        return new PartnerProcessDto(1L, name, name);
    }
}
