package ru.yandex.market.sc.internal.controller.partner;

import java.util.List;

import javax.sql.DataSource;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.sc.core.domain.process.repository.Process;
import ru.yandex.market.sc.core.domain.scan.ScanService;
import ru.yandex.market.sc.core.domain.scan.model.AcceptOrderRequestDto;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.zone.ZoneType;
import ru.yandex.market.sc.core.domain.zone.model.PartnerZoneRequestDto;
import ru.yandex.market.sc.core.domain.zone.repository.Zone;
import ru.yandex.market.sc.core.domain.zone.repository.ZoneRepository;
import ru.yandex.market.sc.core.exception.ScErrorCode;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.tpl.common.util.JacksonUtil;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PartnerWorkstationControllerTest {

    private final ZoneRepository zoneRepository;
    private final TestFactory testFactory;
    private final MockMvc mockMvc;
    private final ScanService scanService;

    SortingCenter sortingCenter;
    Process process;
    Zone zone;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        zone = testFactory.storedZone(sortingCenter, "zone-ws");
        process = testFactory.storedProcess("p1", "p1");
    }

    @Test
    @SneakyThrows
    @DisplayName("Создание рабочей станции")
    void createWorkstation() {
        PartnerZoneRequestDto workstationRequestDto = new PartnerZoneRequestDto(
                "ws-1", List.of(process.getId()), zone.getId());

        var result = mockMvc.perform(
                        post("/internal/partners/" + sortingCenter.getPartnerId() +
                                "/workstations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JacksonUtil.toString(workstationRequestDto))
                )
                .andDo(print())
                .andExpect(status().isOk());

        Zone workstation = StreamEx.of(zoneRepository.findAll())
                .findFirst(z -> z.getName().equals(workstationRequestDto.getName()))
                .orElseThrow();
        result.andExpect(jsonPath("$.workstation.id").value(workstation.getId()))
                .andExpect(jsonPath("$.workstation.parentId").value(workstation.getParentId()))
                .andExpect(jsonPath("$.workstation.type").value(ZoneType.WORKSTATION.name()))
                .andExpect(jsonPath("$.workstation.sortingCenterId").value(sortingCenter.getId()))
                .andExpect(jsonPath("$.workstation.deleted").value(Boolean.FALSE.toString()))
                .andExpect(jsonPath("$.workstation.name").value("ws-1"))
                .andExpect(jsonPath("$.workstation.processes").exists())
                .andExpect(jsonPath("$.workstation.statistic.activeUsers").value(0))
                .andExpect(jsonPath("$.workstation.statistic.totalUsersCheckIn").value(0));
    }

    @Test
    @SneakyThrows
    @DisplayName("Валидация вложенности рабочих станций при создании")
    void validateNestedWorkstationZoneByCreate() {
        var ws1 = testFactory.storedWorkstation(sortingCenter, "ws-1", zone.getId(), process);

        PartnerZoneRequestDto workstationRequestDto = new PartnerZoneRequestDto("ws-2", List.of(process.getId()),
                ws1.getId());

        mockMvc.perform(post("/internal/partners/" + sortingCenter.getPartnerId() + "/workstations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(workstationRequestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(ScErrorCode.WORKSTATION_CANT_BE_NESTED_WORKSTATION.name()));
    }

    @Test
    @SneakyThrows
    @DisplayName("Обновление рабочей станции")
    void updateWorkstation() {
        var ws1 = testFactory.storedWorkstation(sortingCenter, "ws-1", zone.getId(), process);
        var sortZone = testFactory.storedZone(sortingCenter, "sort-zone");
        PartnerZoneRequestDto workstationRequestDto = new PartnerZoneRequestDto("ws-2", List.of(process.getId()),
                sortZone.getId());

        mockMvc.perform(put("/internal/partners/" + sortingCenter.getPartnerId() + "/workstations/" + ws1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(workstationRequestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workstation.id").value(ws1.getId()))
                .andExpect(jsonPath("$.workstation.name").value("ws-2"))
                .andExpect(jsonPath("$.workstation.type").value(ZoneType.WORKSTATION.name()))
                .andExpect(jsonPath("$.workstation.parentId").value(sortZone.getId()))
                .andExpect(jsonPath("$.workstation.statistic.activeUsers").value(0))
                .andExpect(jsonPath("$.workstation.statistic.totalUsersCheckIn").value(0));
    }

    @Test
    @SneakyThrows
    @DisplayName("Валидация вложенности рабочих станций при обновлении")
    void validateNestedWorkstationZoneByUpdate() {
        var ws1 = testFactory.storedWorkstation(sortingCenter, "ws-1", zone.getId(), process);

        PartnerZoneRequestDto workstationRequestDto = new PartnerZoneRequestDto("ws-2", List.of(process.getId()),
                ws1.getId());

        mockMvc.perform(put("/internal/partners/" + sortingCenter.getPartnerId() + "/workstations/" + ws1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(workstationRequestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(ScErrorCode.WORKSTATION_CANT_BE_NESTED_WORKSTATION.name()));
    }

    @Test
    @SneakyThrows
    @DisplayName("Удаление рабочей станции")
    void deleteWorkstation() {
        var ws1 = testFactory.storedWorkstation(sortingCenter, "ws-1", zone.getId(), process);

        mockMvc.perform(delete("/internal/partners/" + sortingCenter.getPartnerId() + "/workstations/" + ws1.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workstation.id").value(ws1.getId()))
                .andExpect(jsonPath("$.workstation.type").value(ZoneType.WORKSTATION.name()))
                .andExpect(jsonPath("$.workstation.statistic.activeUsers").value(0))
                .andExpect(jsonPath("$.workstation.statistic.totalUsersCheckIn").value(0));
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение рабочих станций зоны")
    void getWorkstations() {
        var ws1 = testFactory.storedWorkstation(sortingCenter, "ws1", zone.getId(), process);
        var ws2 = testFactory.storedWorkstation(sortingCenter, "ws2", zone.getId(), process);
        var ws3 = testFactory.storedWorkstation(sortingCenter, "ws3", zone.getId(), process);

        mockMvc.perform(get("/internal/partners/" + sortingCenter.getPartnerId() + "/workstations")
                        .param("zoneId", zone.getId().toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workstations").value(hasSize(3)))
                .andExpect(jsonPath("$.workstations[*].id")
                        .value(containsInAnyOrder(
                                ws1.getId().intValue(),
                                ws2.getId().intValue(),
                                ws3.getId().intValue())
                        )
                );
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение не удаленных рабочих станций")
    void getWorkstationsWithoutDeleted() {
        var zn = testFactory.storedZone(sortingCenter, "zone-parent", process);
        var ws1 = testFactory.storedDeletedWorkstation(sortingCenter, "ws1", zn.getId());
        var ws2 = testFactory.storedWorkstation(sortingCenter, "ws2", zn.getId(), process);
        var ws3 = testFactory.storedWorkstation(sortingCenter, "ws3", zn.getId(), process);

        mockMvc.perform(get("/internal/partners/" + sortingCenter.getPartnerId() + "/workstations")
                        .param("zoneId", zn.getId().toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workstations").value(hasSize(2)))
                .andExpect(jsonPath("$.workstations[*].id")
                        .value(containsInAnyOrder(
                                ws2.getId().intValue(),
                                ws3.getId().intValue())
                        )
                );
    }

    private final DataSource dataSource;

    @Nested
    @DisplayName("Статистика по пользователям в рабочих станциях")
    class WorkstationStatistics {

        Process process;

        @BeforeEach
        void init() {
            process = testFactory.storedCheckInAndLeaveOperation();
        }

        @Test
        @SneakyThrows
        @DisplayName("[OperationLog] Количество активных людей в рабочих станциях")
        void userActive() {
            var zone = testFactory.storedZone(sortingCenter, "1", List.of(process));
            var ws1 = testFactory.storedWorkstation(sortingCenter, "1.1", zone.getId(), process);
            var ws2 = testFactory.storedWorkstation(sortingCenter, "1.2", zone.getId(), process);

            testFactory.checkInZone(ws1, testFactory.storedUser(sortingCenter, 125L));
            testFactory.checkInZone(ws2, testFactory.storedUser(sortingCenter, 129L));
            testFactory.checkInZone(ws2, testFactory.storedUser(sortingCenter, 132L));
            testFactory.checkInZone(ws2, testFactory.storedUser(sortingCenter, 135L));

            mockMvc.perform(get("/internal/partners/{sc-partner-id}/workstations", sortingCenter.getPartnerId())
                            .param("zoneId", zone.getId().toString()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.workstations[?(@.name == '1.1')].statistic.activeUsers").value(1))
                    .andExpect(jsonPath("$.workstations[?(@.name == '1.2')].statistic.activeUsers").value(3));
        }

        @Test
        @SneakyThrows
        @DisplayName("[OperationLog] Количество людей в рабочих станциях")
        void totalUser() {
            var zone = testFactory.storedZone(sortingCenter, "2", List.of(process));
            var ws1 = testFactory.storedWorkstation(sortingCenter, "2.1", zone.getId(), process);
            var ws2 = testFactory.storedWorkstation(sortingCenter, "2.2", zone.getId(), process);

            testFactory.checkInZone(ws1, testFactory.storedUser(sortingCenter, 125L));
            testFactory.checkInZone(ws2, testFactory.storedUser(sortingCenter, 139L));
            testFactory.checkInZone(ws2, testFactory.storedUser(sortingCenter, 142L));

            mockMvc.perform(get("/internal/partners/{sc-partner-id}/workstations", sortingCenter.getPartnerId())
                            .param("zoneId", zone.getId().toString()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.workstations[?(@.name == '2.1')].statistic.totalUsersCheckIn").value(1))
                    .andExpect(jsonPath("$.workstations[?(@.name == '2.2')].statistic.totalUsersCheckIn").value(2));
        }

        @Test
        @SneakyThrows
        @DisplayName("[OrderScanLog] Количество людей в рабочих станциях")
        void totalUsersFromOrderScanLog() {
            var zone = testFactory.storedZone(sortingCenter, "3", List.of(process));
            var ws1 = testFactory.storedWorkstation(sortingCenter, "3.1", zone.getId(), process);
            var ws2 = testFactory.storedWorkstation(sortingCenter, "3.2", zone.getId(), process);

            testFactory.checkInZone(ws1, testFactory.storedUser(sortingCenter, 125L));
            testFactory.createForToday(order(sortingCenter, "o1").places("o1-1", "o1-2").build()).get();
            scanService.acceptOrder(new AcceptOrderRequestDto("o1", "o1-1"),
                    new ScContext(testFactory.findUserByUid(125L), ws1));
            scanService.acceptOrder(new AcceptOrderRequestDto("o1", "o1-2"),
                    new ScContext(testFactory.findUserByUid(125L), ws1));

            testFactory.checkInZone(ws1, testFactory.storedUser(sortingCenter, 126L));
            testFactory.leaveZone(ws1, testFactory.findUserByUid(126L));
            testFactory.checkInZone(ws2, testFactory.storedUser(sortingCenter, 139L));
            testFactory.checkInZone(ws2, testFactory.findUserByUid(126L));

            mockMvc.perform(get("/internal/partners/{sc-partner-id}/workstations", sortingCenter.getPartnerId())
                            .param("zoneId", zone.getId().toString()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.workstations[?(@.name == '3.1')].statistic.totalUsersCheckIn").value(1))
                    .andExpect(jsonPath("$.workstations[?(@.name == '3.2')].statistic.totalUsersCheckIn").value(2));
        }
    }
}
