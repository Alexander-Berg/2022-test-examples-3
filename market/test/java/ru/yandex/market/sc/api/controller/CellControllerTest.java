package ru.yandex.market.sc.api.controller;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.api.test.ScApiControllerTest;
import ru.yandex.market.sc.api.util.ScApiControllerCaller;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.model.UserRole;
import ru.yandex.market.sc.core.exception.ScErrorCode;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@ScApiControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CellControllerTest {

    private static final long UID = 124L;
    private final MockMvc mockMvc;
    private final TestFactory testFactory;
    private final ScApiControllerCaller caller;

    @MockBean
    Clock clock;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.storedUser(sortingCenter, UID, UserRole.SENIOR_STOCKMAN);
        testFactory.setupMockClock(clock);
    }

    @Test
    void getCellWithOrdersInfo() throws Exception {
        testFactory.createForToday(order(sortingCenter).externalId("o1").build()).cancel();
        testFactory.createForToday(order(sortingCenter).externalId("o2").build()).cancel().accept();
        var place3 = testFactory.createForToday(
                order(sortingCenter).externalId("o3").build()
        ).cancel().accept().sort().getPlace();

        testFactory.createForToday(
                order(sortingCenter).externalId("o4").places("o4p1", "o4p2").build()
        ).cancel().acceptPlaces("o4p1");
        testFactory.createForToday(
                order(sortingCenter).externalId("o5").places("o5p1", "o5p2").build()
        ).cancel().acceptPlaces("o5p1").sortPlaces("o5p1");
        testFactory.createForToday(
                order(sortingCenter).externalId("o6").places("o6p1", "o6p2").build()
        ).cancel().acceptPlaces("o6p1", "o6p2").sortPlaces("o6p1", "o6p2");

        var cell =  place3.getCell();
        var route = testFactory.findPossibleOutcomingWarehouseRouteByPlaceId(place3.getId()).orElseThrow();
        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/cells/" + Objects.requireNonNull(cell).getId())
                        .header("Authorization", "OAuth uid-" + UID)
        )
                .andExpect(status().isOk())
                .andExpect(content().json("{" +
                        "\"id\":" + cell.getId() +
                        ",\"status\":\"NOT_ACTIVE\"" +
                        ",\"type\":\"RETURN\"" +
                        ",\"number\":\"RETURN NEW " + cell.getId() + "\"" +
                        ",\"subType\":\"DEFAULT\"" +
                        ",\"ordersInCell\":3" +
                        ",\"placeCount\":4" +
                        ",\"acceptedButNotSortedPlaceCount\":2" +
                        ",\"ordersAssignedToCell\":5" +
                        ",\"cellPrepared\":true" +
                        ",\"routeId\":" + testFactory.getRouteIdForSortableFlow(route) +
                        ",\"showFilledStatus\":false" +
                        ",\"filledStatus\":false" +
                        ",\"cargoType\":\"NONE\"" +
                        "}", true));
    }

    @Test
    void getCellWithPartialMultiplaceOrder() throws Exception {
        testFactory.createForToday(order(sortingCenter).externalId("o1").places("p1", "p2").build())
                .acceptPlaces("p1").sortPlaces("p1");
        var place2 = testFactory.createForToday(order(sortingCenter).externalId("o2").build())
                .accept().sort().prepare().getPlace();

        var cell = place2.getCell();
        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/cells/" + Objects.requireNonNull(cell).getId())
                        .header("Authorization", "OAuth uid-" + UID)
        )
                .andExpect(status().isOk())
                .andExpect(content().json("{" +
                        "\"cellPrepared\":false" +
                        "}", false));
    }

    @Test
    void getCellWithOldOrder() throws Exception {
        var place = testFactory.createForToday(order(sortingCenter).externalId("o1").build())
                .accept()
                .sort()
                .prepare()
                .getPlace();
        var cell = place.getCell();
        testFactory.setupMockClock(clock, clock.instant().plus(1, ChronoUnit.DAYS));

        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/cells/" + Objects.requireNonNull(cell).getId())
                        .header("Authorization", "OAuth uid-" + UID)
        )
                .andExpect(status().isOk())
                .andExpect(content().json("{" +
                        "\"cellPrepared\":false" +
                        "}", false));
    }

    @Test
    void getCellsForRouteV2() throws Exception {
        var orderWithMultiplaceIncomplete = testFactory.createForToday(
                order(sortingCenter).externalId("o1").places("p1", "p2").build())
                .acceptPlaces("p1")
                .sortPlaces("p1")
                .get();

        var simpleOrder = testFactory.createForToday(
                order(sortingCenter).externalId("o2").build())
                .accept()
                .sort()
                .prepare()
                .get();

        var route = testFactory.findOutgoingCourierRoute(orderWithMultiplaceIncomplete).orElseThrow();

        Cell cell = testFactory.orderPlace(simpleOrder).getCell();
        assertThat(cell).isNotNull();

        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/cells/" + cell.getId() + "/forRoute")
                        .param("routeId", String.valueOf(testFactory.getRouteIdForSortableFlow(route)))
                        .header("Authorization", "OAuth uid-" + UID))
                .andDo(rh -> System.out.println("content: " + rh.getResponse().getContentAsString()))
                .andExpect(status().isOk())
                .andExpect(content().json(String.format(
                        "{\"orders\":{\"%s\": \"DO_NOT_SHIP\",\"%s\": \"OK\"}}",
                        orderWithMultiplaceIncomplete.getExternalId(),
                        simpleOrder.getExternalId()),
                        false));
    }

    @Test
    void markFilledStatus() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.USE_ZONE_FOR_BUFFER_RETURN_CELLS, true);
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ONE_CELL_PER_ZONE_FOR_BUFFER_RETURN_CELLS, true);
        testFactory.storedUser(sortingCenter, 123L, UserRole.SENIOR_STOCKMAN);
        Cell cell = testFactory.storedActiveCell(sortingCenter, CellType.RETURN, CellSubType.BUFFER_RETURNS, "c-6");
        caller.markFilledStatus(cell.getId(), true)
                .andExpect(status().isOk());

        boolean actualFullness = testFactory.getCellFullness(cell.getId());
        assertThat(actualFullness).isTrue();
    }

    @Test
    void markUnfilledStatus() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.USE_ZONE_FOR_BUFFER_RETURN_CELLS, true);
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ONE_CELL_PER_ZONE_FOR_BUFFER_RETURN_CELLS, true);
        testFactory.storedUser(sortingCenter, 123L, UserRole.SENIOR_STOCKMAN);
        Cell cell = testFactory.storedActiveCell(sortingCenter, CellType.RETURN, CellSubType.BUFFER_RETURNS, "c-6");
        caller.markFilledStatus(cell.getId(), true).andExpect(status().isOk());
        caller.markFilledStatus(cell.getId(), false).andExpect(status().isOk());

        boolean actualFullness = testFactory.getCellFullness(cell.getId());
        assertThat(actualFullness).isFalse();
    }

    @Test
    void markUnfilledStatusIncorrectRole() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.USE_ZONE_FOR_BUFFER_RETURN_CELLS, true);
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ONE_CELL_PER_ZONE_FOR_BUFFER_RETURN_CELLS, true);
        testFactory.storedUser(sortingCenter, 123L, UserRole.STOCKMAN);
        Cell cell = testFactory.storedActiveCell(sortingCenter, CellType.RETURN, CellSubType.BUFFER_RETURNS, "c-6");
        caller.markFilledStatus(cell.getId(), true).andExpect(status().isOk());
        caller.markFilledStatus(cell.getId(), false)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is(ScErrorCode.INCORRECT_ROLE.name())))
                .andExpect(jsonPath("$.message", is(ScErrorCode.INCORRECT_ROLE.getMessage())));
    }
}
