package ru.yandex.market.sc.api.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.api.test.ScApiControllerTest;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.print.model.DestinationType;
import ru.yandex.market.sc.core.domain.print.repository.PrintTask;
import ru.yandex.market.sc.core.domain.print.repository.PrintTaskRepository;
import ru.yandex.market.sc.core.domain.print.repository.PrintTaskStatus;
import ru.yandex.market.sc.core.domain.print.repository.PrintTemplateType;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@ScApiControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PrintControllerTest {

    private static final long UID = 123L;

    private final MockMvc mockMvc;
    private final PrintTaskRepository printTaskRepository;
    private final TestFactory testFactory;
    private final Clock clock;

    SortingCenter sortingCenter;
    User user;
    PrintTask printTask;
    SortableLot lot;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.storedUser(sortingCenter, UID);
        var cell = testFactory.storedCell(sortingCenter, "O", CellType.COURIER);
        lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        printTask = new PrintTask(
                1L, 333, "mockPrintServer", "printer1", DestinationType.PLAIN, PrintTaskStatus.CREATED,
                Instant.now(clock), null, user.getId(), 1, PrintTemplateType.LOT,
                List.of(lot.getBarcode())
        );
    }

    @Test
    @SneakyThrows
    void listTasks() {
        printTask = printTaskRepository.save(printTask);
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/printService/tasks/list")
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(printTask.getId().intValue())));
    }

    @Test
    @SneakyThrows
    void createTask() {
        var createTaskResult = mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/printService/tasks")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(String.format("""
                                        {
                                            "destination": "printer0",
                                            "destinationType": "PLAIN",
                                            "templateType": "QR",
                                            "fields": ["%s"],
                                            "copies": 1
                                        }
                                        """, lot.getBarcode())
                                )
                )
                .andExpect(status().isOk());
        var printTasks = printTaskRepository.findAll();
        assertThat(printTasks).hasSize(1);
        createTaskResult.andExpect(jsonPath("$.id", is(printTasks.get(0).getId().intValue())));
    }

    @Test
    @SneakyThrows
    void showTask() {
        printTask = printTaskRepository.save(printTask);
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/printService/tasks/" + printTask.getId())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(printTask.getId().intValue())))
                .andExpect(jsonPath("$.jobId", is(333)));
    }

    @Test
    @SneakyThrows
    void retryTask() {
        printTask.setStatus(PrintTaskStatus.FAILED);
        printTask = printTaskRepository.save(printTask);
        var createTaskResult = mockMvc.perform(
                MockMvcRequestBuilders.put("/api/printService/tasks/" + printTask.getId() + "/retry")
                        .header("Authorization", "OAuth uid-" + UID)
        ).andExpect(status().isOk());
        createTaskResult.andExpect(jsonPath("$.id", is(printTask.getId().intValue())));
    }


    @Test
    @SneakyThrows
    public void createZPLTask() {
        var sortingCenter = testFactory.storedSortingCenter();
        var user = testFactory.storedUser(sortingCenter, 1L);

        var courier = testFactory.courier();

        var order1 = testFactory.createForToday(order(sortingCenter, "o1")
                .deliveryDate(LocalDate.now(clock))
                .shipmentDate(LocalDate.now(clock))
                .build()).updateCourier(courier).accept().sort().get();
        var order2 = testFactory.createForToday(order(sortingCenter, "o2")
                .deliveryDate(LocalDate.now(clock))
                .shipmentDate(LocalDate.now(clock))
                .build()).updateCourier(courier).accept().sort().get();
        var order3 = testFactory.create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build()).updateCourier(courier)
                .acceptPlaces(List.of("p1", "p2", "p3"))
                .sortPlaces(List.of("p1", "p2", "p3"))
                .get();

        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, testFactory.orderPlace(order1).getCell());

        testFactory.sortPlaceToLot(testFactory.orderPlace(order1), lot, user);
        testFactory.sortPlaceToLot(testFactory.orderPlace(order2), lot, user);
        testFactory.sortPlaceToLot(testFactory.orderPlace(order3, "p1"), lot, user);
        testFactory.sortPlaceToLot(testFactory.orderPlace(order3, "p2"), lot, user);
        testFactory.sortPlaceToLot(testFactory.orderPlace(order3, "p3"), lot, user);

        testFactory.prepareToShipLot(lot);

        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/printService/tasks")
                        .header("Authorization", "OAuth uid-" + UID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                    "destination": "printer0",
                                    "destinationType": "ZPL",
                                    "templateType": "QR",
                                    "fields": ["%s"],
                                    "copies": 1
                                }
                                """, lot.getBarcode())
                        )
        ).andExpect(status().is2xxSuccessful());
    }

}
