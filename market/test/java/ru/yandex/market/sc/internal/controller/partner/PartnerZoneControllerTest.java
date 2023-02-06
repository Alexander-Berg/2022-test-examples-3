package ru.yandex.market.sc.internal.controller.partner;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.domain.process.repository.Process;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.zone.ZoneType;
import ru.yandex.market.sc.core.domain.zone.model.ZoneStatisticDto;
import ru.yandex.market.sc.core.domain.zone.repository.Zone;
import ru.yandex.market.sc.core.domain.zone.repository.ZoneMapper;
import ru.yandex.market.sc.core.domain.zone.repository.ZoneRepository;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.controller.dto.PartnerZonesDtoWrapper;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.tpl.common.util.JacksonUtil;

import static java.util.Collections.emptyList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.domain.process.ProcessQueryService.CHECK_IN_PROCESS_SYSTEM_NAME;
import static ru.yandex.market.sc.core.domain.process.ProcessQueryService.COMMON_PROCESS_SYSTEM_NAME;

/**
 * @author valter
 */
@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PartnerZoneControllerTest {

    private final MockMvc mockMvc;
    private final TestFactory testFactory;
    private final ZoneRepository zoneRepository;
    private final ZoneMapper zoneMapper;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    void getZones() throws Exception {
        var zone1 = testFactory.storedZone(sortingCenter, "1");
        var process = testFactory.storedProcess("system", "display");
        var zone2 = testFactory.storedZone(sortingCenter, "2", List.of(process));
        mockMvc.perform(
                        get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/zones")
                )
                .andExpect(status().isOk())
                .andExpect(content().json(JacksonUtil.toString(
                        new PartnerZonesDtoWrapper(List.of(zoneMapper.mapToPartner(zone1),
                                zoneMapper.mapToPartner(zone2)))
                ), true));
    }

    @Nested
    @DisplayName("Статистика по пользователям в зонах")
    class ZoneStatistics {

        Process process;

        @BeforeEach
        void init() {
            process = testFactory.storedCheckInAndLeaveOperation();
        }

        @Test
        @SneakyThrows
        @DisplayName("Количество зачекиненных людей в зоне (без рабочих станций)")
        void userCheckInZoneWithoutWorkstation() {
            var zone = testFactory.storedZone(sortingCenter, "1", process);
            var workstation = testFactory.storedWorkstation(sortingCenter, "2", zone.getId(), process);

            testFactory.checkInZone(zone, testFactory.storedUser(sortingCenter, 123L));
            testFactory.checkInZone(zone, testFactory.storedUser(sortingCenter, 127L));
            testFactory.checkInZone(workstation, testFactory.storedUser(sortingCenter, 125L));
            testFactory.checkInZone(zone, testFactory.storedUser(139L));
            testFactory.leaveZone(zone, testFactory.findUserByUid(139L));

            mockMvc.perform(get("/internal/partners/{sc-partner-id}/zones", sortingCenter.getPartnerId()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.zones[?(@.name == '1')].statistic.totalUsersOnlyZone").value(2));
        }

        @Test
        @SneakyThrows
        @DisplayName("Количество активных людей в зоне + рабочих станциях")
        void userActiveZoneWithWorkstation() {
            var zone = testFactory.storedZone(sortingCenter, "1", process);
            var workstation1 = testFactory.storedWorkstation(sortingCenter, "2", zone.getId(), process);
            var workstation2 = testFactory.storedWorkstation(sortingCenter, "3", zone.getId(), process);

            testFactory.checkInZone(zone, testFactory.storedUser(sortingCenter, 123L));
            testFactory.checkInZone(zone, testFactory.storedUser(sortingCenter, 127L));
            testFactory.checkInZone(workstation1, testFactory.storedUser(sortingCenter, 125L));
            testFactory.checkInZone(workstation1, testFactory.storedUser(sortingCenter, 129L));
            testFactory.checkInZone(zone, testFactory.storedUser(139L));
            testFactory.leaveZone(zone, testFactory.findUserByUid(139L));
            testFactory.checkInZone(workstation1, testFactory.storedUser(140L));
            testFactory.leaveZone(workstation1, testFactory.findUserByUid(140L));
            testFactory.checkInZone(workstation2, testFactory.storedUser(sortingCenter, 135L));
            testFactory.checkInZone(workstation2, testFactory.storedUser(sortingCenter, 149L));

            mockMvc.perform(get("/internal/partners/{sc-partner-id}/zones", sortingCenter.getPartnerId()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.zones[?(@.name == '1')].statistic.activeUsers").value(6));
        }

        @Test
        @SneakyThrows
        @DisplayName("Количество людей в зоне + рабочих станциях")
        void totalUserZoneAndWorkstation() {
            var zone = testFactory.storedZone(sortingCenter, "1", process);
            var workstation = testFactory.storedWorkstation(sortingCenter, "2", zone.getId(), process);

            testFactory.checkInZone(zone, testFactory.storedUser(sortingCenter, 123L));
            testFactory.checkInZone(zone, testFactory.storedUser(sortingCenter, 127L));
            testFactory.checkInZone(workstation, testFactory.storedUser(sortingCenter, 125L));
            testFactory.checkInZone(workstation, testFactory.storedUser(sortingCenter, 129L));
            testFactory.checkInZone(zone, testFactory.storedUser(139L));
            testFactory.leaveZone(zone, testFactory.findUserByUid(139L));

            mockMvc.perform(get("/internal/partners/{sc-partner-id}/zones", sortingCenter.getPartnerId()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.zones[?(@.name == '1')].statistic.totalUsersCheckIn").value(4));
        }

        @Test
        @SneakyThrows
        @DisplayName("Количество рабочих станций в зоне")
        void totalWorkstationInZone() {
            var zone = testFactory.storedZone(sortingCenter, "1", process);
            testFactory.storedWorkstation(sortingCenter, "2", zone.getId(), process);
            testFactory.storedWorkstation(sortingCenter, "3", zone.getId(), process);
            testFactory.storedWorkstation(sortingCenter, "4", zone.getId(), process);

            mockMvc.perform(get("/internal/partners/{sc-partner-id}/zones", sortingCenter.getPartnerId()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.zones[?(@.name == '1')].statistic.totalWorkstationsInZone").value(3));
        }
    }

    @Test
    void getZone() throws Exception {
        var process1 = testFactory.storedProcess("system1", "display1");
        var process2 = testFactory.storedProcess("system2", "display2");
        var zone = testFactory.storedZone(sortingCenter, "1", List.of(process1, process2));

        mockMvc.perform(
                        get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/zones/" + zone.getId())
                )
                .andExpect(status().isOk())
                .andExpect(content().json(zoneResponseBody(zone), true));
    }

    @Test
    void getZoneFilteringCheckin() throws Exception {
        var process1 = testFactory.storedProcess("system1", "display1");
        var process2 = testFactory.storedProcess(COMMON_PROCESS_SYSTEM_NAME, "display2");
        var process3 = testFactory.storedProcess(CHECK_IN_PROCESS_SYSTEM_NAME, "display3");
        var zone = testFactory.storedZone(sortingCenter, "1", List.of(process1, process2, process3));

        mockMvc.perform(
                        get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/zones/" + zone.getId())
                )
                .andExpect(status().isOk())
                .andExpect(content().json(zoneResponseBody(zone.getId(), "1", false, true,
                        processesArrayNode(List.of(process1))), false));
    }

    @Test
    void createZone() throws Exception {
        var process = testFactory.storedProcess("system", "display");
        var resultActions = mockMvc.perform(
                        MockMvcRequestBuilders.post("/internal/partners/" + sortingCenter.getPartnerId() + "/zones")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(zoneRequest("1", List.of(process.getId())))

                )
                .andExpect(status().isOk());
        var createdZone = zoneRepository.findAll()
                .stream()
                .filter(it -> it.getName().equals("1"))
                .findFirst()
                .orElseThrow();
        resultActions.andExpect(content().json(zoneResponseBody(createdZone.getId(), "1", false, true,
                processesArrayNode(List.of(process))), false));
    }

    @Test
    void createZoneWithEmptyProcesses() throws Exception {
        var resultActions = mockMvc.perform(
                        MockMvcRequestBuilders.post("/internal/partners/" + sortingCenter.getPartnerId() + "/zones")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(zoneRequest("1", emptyList()))

                )
                .andExpect(status().isOk());
        var createdZone = zoneRepository.findAll()
                .stream()
                .filter(it -> it.getName().equals("1"))
                .findFirst()
                .orElseThrow();
        resultActions.andExpect(content().json(zoneResponseBody(createdZone.getId(), "1", false, true,
                processesArrayNode(emptyList())), false));
    }

    @Test
    void updateZone() throws Exception {
        var zone = testFactory.storedZone(sortingCenter, "1");
        var process = testFactory.storedProcess("system", "display");
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/internal/partners/" + sortingCenter.getPartnerId()
                                        + "/zones/" + zone.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(zoneRequest("2", List.of(process.getId())))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(zoneResponseBody(zone.getId(), "2", false, true,
                        processesArrayNode(List.of(process))), false));
    }

    @Test
    void deleteZone() throws Exception {
        var zone = testFactory.storedZone(sortingCenter, "1");
        mockMvc.perform(
                        MockMvcRequestBuilders.delete("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/zones/" + zone.getId())

                )
                .andExpect(status().isOk())
                .andExpect(content().json(zoneResponseBody(zone.getId(), "1", true, true,
                        processesArrayNode(zone.getProcesses())), false));
    }


    private String zoneRequest(String name, List<Long> processIds) {
        var processIdsJson = processIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));

        return "{" +
                "\"name\":\"" + name + "\"," +
                "\"processIds\":" + processIdsJson +
                "}";
    }

    private String zonesResponseBody(List<Zone> zones) {
        return zones.stream()
                .map(zone -> zoneResponseBody(zone.getId(), zone.getName(), zone.isDeleted(), false,
                        processesArrayNode(zone.getProcesses())))
                .collect(Collectors.joining(",", "{\"zones\":[", "]}"));
    }

    static String processesArrayNode(List<Process> processes) {
        return processes.stream()
                .map(PartnerZoneControllerTest::processJsonNode)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String processJsonNode(Process process) {
        return "{" +
                "\"id\":" + process.getId() + "," +
                "\"systemName\":\"" + process.getSystemName() + "\"," +
                "\"displayName\":\"" + process.getDisplayName() + "\"" +
                "}";
    }

    private String zoneResponseBody(Zone zone) {
        return zoneResponseBody(zone.getId(), zone.getName(), zone.isDeleted(), true,
                processesArrayNode(zone.getProcesses()));
    }

    private String zoneResponseBody(long id, String name, boolean deleted, boolean wrapper, String processJson) {
        return (wrapper ? "{\"zone\":" : "") +
                "{" +
                "\"id\":" + id + "," +
                "\"sortingCenterId\":" + sortingCenter.getId() + "," +
                "\"name\":\"" + name + "\"," +
                "\"deleted\":" + deleted + "," +
                "\"processes\":" + processJson + "," +
                "\"type\":" + ZoneType.DEFAULT.name() + "," +
                "\"statistic\":" + JacksonUtil.toString(new ZoneStatisticDto(0, 0, 0, 0)) +
                "}" +
                (wrapper ? "}" : "");
    }
}
