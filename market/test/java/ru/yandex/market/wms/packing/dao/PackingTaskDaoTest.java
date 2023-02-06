package ru.yandex.market.wms.packing.dao;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.wms.common.model.enums.OrderStatus;
import ru.yandex.market.wms.common.model.enums.OrderType;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.packing.LocationsRov;
import ru.yandex.market.wms.packing.LocationsSof;
import ru.yandex.market.wms.packing.enums.TicketType;
import ru.yandex.market.wms.packing.pojo.SortingCell;
import ru.yandex.market.wms.packing.pojo.Ticket;

import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.wms.packing.dao.TicketTestData.ticket777;
import static ru.yandex.market.wms.packing.dao.TicketTestData.ticket778;
import static ru.yandex.market.wms.packing.dao.TicketTestData.ticket779;


/**
 * https://www.testcontainers.org/modules/databases/mssqlserver/
 */
@TestPropertySource(properties = {"warehouse-timezone = Europe/Moscow"})
class PackingTaskDaoTest extends IntegrationTest {

    @Autowired
    private PackingTaskDao dao;

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/dao/packing_task/get_tickets_single/setup.xml", type = INSERT)
    void getTicketsSingleInMoscow() {
        List<Ticket> tickets = dao.getTickets();
        assertThat(tickets).hasSize(1);
        assertThat(tickets.get(0)).isEqualToComparingFieldByField(
                Ticket.builder()
                        .orderKey("ORD0777")
                        .orderType(OrderType.STANDARD)
                        .externOrderKey("EXT0777")
                        .type(TicketType.SORTABLE)
                        .editDate(Instant.parse("2020-01-01T06:50:03Z"))
                        .sourceLoc("SS1")
                        .sortingCells(Set.of(LocationsRov.SS1_CELL1, LocationsRov.SS1_CELL2))
                        .maxOrderStatus(OrderStatus.PICKED_COMPLETE)
                        .shippingDeadline(LocalDateTime.parse("2020-01-06T23:00:00"))
                        .build()
        );
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/dao/packing_task/get_tickets_single/setup.xml", type = INSERT)
    @DatabaseSetup(value =
            "/db/dao/packing_task/get_tickets_single/already-assigned-to-another-user-same-host-setup.xml",
            type = INSERT)
    void getTicketsSingleAlreadyAssignedToAnotherUserSameHost() {
        List<Ticket> tickets = dao.getTickets();

        assertThat(tickets).isEmpty();
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/dao/packing_task/get_tickets_single/setup.xml", type = INSERT)
    @DatabaseSetup(value =
            "/db/dao/packing_task/get_tickets_single/already-assigned-to-same-user-another-host-setup.xml",
            type = INSERT)
    void getTicketsSingleAlreadyAssignedToSameUserAnotherHost() {
        List<Ticket> tickets = dao.getTickets();

        assertThat(tickets).isEmpty();
    }

    /*
    1 заказ на станции 1 в двух ячейках
    1 заказ на станции 1 в одной ячейке
    1 заказ на станции 2 в двух ячейках
    */
    @Test
    @DatabaseSetup("/db/locations_setup_rov.xml")
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/dao/packing_task/get_tickets_multiple/setup.xml", type = INSERT)
    void getTicketsMultipleRov() {
        List<Ticket> tickets = dao.getTickets();
        assertThat(tickets).containsExactlyInAnyOrder(ticket777(), ticket778(), ticket779());
    }


    /*
    1 заказ на станции 1 в двух ячейках
    1 заказ на станции 1 в одной ячейке
    1 заказ на станции 2 в двух ячейках
    */
    @Test
    @DatabaseSetup(value = "/db/locations_setup_sof.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/dao/packing_task/get_tickets_multiple/setup.xml", type = INSERT)
    void getTicketsMultipleSof() {
        List<Ticket> tickets = dao.getTickets();

        Ticket ticket777 = Ticket.builder()
                .orderKey("ORD0777")
                .orderType(OrderType.STANDARD)
                .externOrderKey("EXT0777")
                .type(TicketType.SORTABLE)
                .editDate(Instant.parse("2020-01-01T06:50:03Z"))
                .sourceLoc("SS1")
                .sortingCells(Set.of(LocationsSof.SS1_CELL1, LocationsSof.SS1_CELL2))
                .build();

        Ticket ticket778 = Ticket.builder()
                .orderKey("ORD0778")
                .orderType(OrderType.STANDARD)
                .externOrderKey("EXT0778")
                .type(TicketType.SORTABLE)
                .editDate(Instant.parse("2020-01-01T06:50:04Z"))
                .sourceLoc("SS1")
                .sortingCells(Set.of(LocationsSof.SS1_CELL3))
                .build();
        Ticket ticket779 = Ticket.builder()
                .orderKey("ORD0779")
                .orderType(OrderType.STANDARD)
                .externOrderKey("EXT0779")
                .type(TicketType.SORTABLE)
                .editDate(Instant.parse("2020-01-01T06:50:05Z"))
                .sourceLoc("SS2")
                .sortingCells(Set.of(LocationsSof.SS2_CELL3, LocationsSof.SS2_CELL4))
                .build();

        assertThat(tickets).containsExactlyInAnyOrder(ticket777, ticket778, ticket779);
    }

    /**
     * Сортировка идет сначала по shippingDeadline, потом по типу, потом по editDate
     */
    @Test
    @DatabaseSetup("/db/locations_setup_rov.xml")
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/dao/packing_task/get_tickets_multiple_sorted/setup.xml", type = INSERT)
    void getTicketsMultipleSorted() {
        List<Ticket> tickets = dao.getTickets().stream().sorted(Ticket.COMPARATOR).collect(Collectors.toList());

        List<Ticket> expected = List.of(
                Ticket.builder()
                        .orderKey("ORD0781")
                        .orderType(OrderType.STANDARD)
                        .externOrderKey("EXT0781")
                        .type(TicketType.SORTABLE)
                        .shippingDeadline(LocalDateTime.parse("2020-01-05T23:00:00"))
                        .editDate(Instant.parse("2020-01-01T06:51:00Z"))
                        .sourceLoc("SS1")
                        .sortingCells(Set.of(LocationsRov.SS1_CELL5))
                        .maxOrderStatus(OrderStatus.PICKED_COMPLETE)
                        .build(),

                Ticket.builder()
                        .orderKey(null)
                        .orderType(OrderType.STANDARD)
                        .type(TicketType.SINGLE)
                        .shippingDeadline(LocalDateTime.parse("2020-01-06T05:30:00"))
                        .editDate(Instant.parse("2020-01-01T06:52:00Z"))
                        .sourceLoc(LocationsRov.NONSORT_TABLE1_CONS_SINGLES)
                        .sortingCells(Set.of(SortingCell.builder()
                                .loc(LocationsRov.NONSORT_TABLE1_CONS_SINGLES)
                                .id("PLT124")
                                .build()))
                        .maxOrderStatus(OrderStatus.PICKED_COMPLETE)
                        .build(),

                Ticket.builder()
                        .orderKey(null)
                        .orderType(OrderType.STANDARD)
                        .type(TicketType.OVERSIZE)
                        .shippingDeadline(LocalDateTime.parse("2020-01-06T05:30:00"))
                        .editDate(Instant.parse("2020-01-01T06:51:00Z"))
                        .sourceLoc(LocationsRov.NONSORT_TABLE1_CONS_OVERSIZE)
                        .sortingCells(Set.of(SortingCell.builder()
                                .loc(LocationsRov.NONSORT_TABLE1_CONS_OVERSIZE)
                                .id("PLT122")
                                .build()))
                        .maxOrderStatus(OrderStatus.IN_PACKING)
                        .build(),

                Ticket.builder()
                        .orderKey("ORD0782")
                        .orderType(OrderType.STANDARD)
                        .externOrderKey("EXT0782")
                        .type(TicketType.SORTABLE)
                        .shippingDeadline(LocalDateTime.parse("2020-01-06T05:30:00"))
                        .editDate(Instant.parse("2020-01-01T06:51:00Z"))
                        .sourceLoc("SS1")
                        .sortingCells(Set.of(LocationsRov.SS1_CELL6))
                        .maxOrderStatus(OrderStatus.PICKED_COMPLETE)
                        .build(),

                Ticket.builder()
                        .orderKey("ORD0780")
                        .orderType(OrderType.STANDARD)
                        .externOrderKey("EXT0780")
                        .type(TicketType.SORTABLE)
                        .shippingDeadline(LocalDateTime.parse("2020-01-06T23:00:00"))
                        .editDate(Instant.parse("2020-01-01T06:50:04Z"))
                        .sourceLoc("SS1")
                        .sortingCells(Set.of(LocationsRov.SS1_CELL4))
                        .maxOrderStatus(OrderStatus.PICKED_COMPLETE)
                        .build(),

                Ticket.builder()
                        .orderKey("ORD0779")
                        .orderType(OrderType.STANDARD)
                        .externOrderKey("EXT0779")
                        .type(TicketType.SORTABLE)
                        .shippingDeadline(LocalDateTime.parse("2020-01-06T23:00:00"))
                        .editDate(Instant.parse("2020-01-01T06:50:05Z"))
                        .sourceLoc("SS1")
                        .sortingCells(Set.of(LocationsRov.SS1_CELL3))
                        .maxOrderStatus(OrderStatus.PICKED_COMPLETE)
                        .build(),

                Ticket.builder()
                        .orderKey(null)
                        .orderType(OrderType.STANDARD)
                        .type(TicketType.SINGLE)
                        .shippingDeadline(LocalDateTime.parse("2020-01-07T05:30:00"))
                        .editDate(Instant.parse("2020-01-01T06:50:10Z"))
                        .sourceLoc(LocationsRov.NONSORT_TABLE1_CONS_SINGLES)
                        .sortingCells(Set.of(SortingCell.builder()
                                .loc(LocationsRov.NONSORT_TABLE1_CONS_SINGLES)
                                .id("PLT123")
                                .build()))
                        .maxOrderStatus(OrderStatus.PICKED_COMPLETE)
                        .build(),

                Ticket.builder()
                        .orderKey("ORD0777")
                        .orderType(OrderType.STANDARD)
                        .externOrderKey("EXT0777")
                        .type(TicketType.SORTABLE)
                        .shippingDeadline(LocalDateTime.parse("2020-01-07T05:30:00"))
                        .editDate(Instant.parse("2020-01-01T06:50:04Z"))
                        .sourceLoc("SS1")
                        .sortingCells(Set.of(LocationsRov.SS1_CELL1))
                        .maxOrderStatus(OrderStatus.PICKED_COMPLETE)
                        .build(),

                Ticket.builder()
                        .orderKey("ORD0778")
                        .orderType(OrderType.STANDARD)
                        .externOrderKey("EXT0778")
                        .type(TicketType.SORTABLE)
                        .shippingDeadline(LocalDateTime.parse("2020-01-07T05:30:00"))
                        .editDate(Instant.parse("2020-01-01T06:50:05Z"))
                        .sourceLoc("SS1")
                        .sortingCells(Set.of(LocationsRov.SS1_CELL2))
                        .maxOrderStatus(OrderStatus.PICKED_COMPLETE)
                        .build()
        );

        assertThat(tickets).hasSize(expected.size());

        for (int i = 0; i < tickets.size(); i++) {
            assertThat(tickets.get(i)).isEqualToComparingFieldByField(expected.get(i));
        }
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/dao/packing_task/get_tickets_invalid_order_type/setup.xml", type = INSERT)
    void getTicketsWhenInvalidOrderType() {
        List<Ticket> tickets = dao.getTickets();
        assertThat(tickets).hasSize(1);
        assertThat(tickets.get(0).getOrderKey()).isEqualTo("ORD-GOOD");
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/dao/packing_task/get_tickets_invalid_picked_qty/setup.xml", type = INSERT)
    void getTicketsWhenInvalidPickedQty() {
        List<Ticket> tickets = dao.getTickets();
        assertThat(tickets).hasSize(1);
        assertThat(tickets.get(0).getOrderKey()).isEqualTo("ORD-GOOD");
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/dao/packing_task/get_tickets_carrier_code_problems/setup.xml", type = INSERT)
    void getTicketsWithCarrierCodeProblems() {
        List<Ticket> tickets = dao.getTickets().stream().sorted(Ticket.COMPARATOR).collect(Collectors.toList());

        Ticket ticketWithCarrierDeadlinesNotConfigured = Ticket.builder()
                .orderKey("ORD-BAD2")
                .orderType(OrderType.OUTBOUND_FIT)
                .externOrderKey("EXT-BAD2")
                .type(TicketType.SORTABLE)
                .shippingDeadline(LocalDateTime.parse("2020-01-04T18:00:00"))
                .editDate(Instant.parse("2020-01-05T10:00:00Z"))
                .sourceLoc("SS1")
                .sortingCells(Set.of(LocationsRov.SS1_CELL3))
                .maxOrderStatus(OrderStatus.PICKED_COMPLETE)
                .build();

        Ticket ticketWithCarrierCodeNull = Ticket.builder()
                .orderKey("ORD-BAD1")
                .orderType(OrderType.OUTBOUND_FIT)
                .externOrderKey("EXT-BAD1")
                .type(TicketType.SORTABLE)
                .shippingDeadline(LocalDateTime.parse("2020-01-05T18:00:00"))
                .editDate(Instant.parse("2020-01-05T10:00:00Z"))
                .sourceLoc("SS1")
                .sortingCells(Set.of(LocationsRov.SS1_CELL1))
                .maxOrderStatus(OrderStatus.PICKED_COMPLETE)
                .build();

        Ticket ticketGood = Ticket.builder()
                .orderKey("ORD-GOOD")
                .orderType(OrderType.STANDARD)
                .externOrderKey("EXT-GOOD")
                .type(TicketType.SORTABLE)
                .shippingDeadline(LocalDateTime.parse("2020-01-06T23:00:00"))
                .editDate(Instant.parse("2020-01-05T10:00:00Z"))
                .sourceLoc("SS1")
                .sortingCells(Set.of(LocationsRov.SS1_CELL2))
                .maxOrderStatus(OrderStatus.PICKED_COMPLETE)
                .build();

        Ticket ticketWithCarrierShippingDeadlineNull = Ticket.builder()
                .orderKey("ORD-BAD3")
                .orderType(OrderType.OUTBOUND_FIT)
                .externOrderKey("EXT-BAD3")
                .type(TicketType.SORTABLE)
                .shippingDeadline(LocalDateTime.parse("2020-01-07T01:00:00"))
                .editDate(Instant.parse("2020-01-05T10:00:00Z"))
                .sourceLoc("SS1")
                .sortingCells(Set.of(LocationsRov.SS1_CELL4))
                .maxOrderStatus(OrderStatus.PICKED_COMPLETE)
                .build();

        List<Ticket> expected = Arrays.asList(
                ticketWithCarrierDeadlinesNotConfigured,
                ticketWithCarrierCodeNull,
                ticketGood,
                ticketWithCarrierShippingDeadlineNull
        );

        assertThat(tickets).hasSize(expected.size());

        for (int i = 0; i < tickets.size(); i++) {
            assertThat(tickets.get(i)).isEqualToComparingFieldByField(expected.get(i));
        }
    }
}
