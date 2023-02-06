package ru.yandex.market.wms.packing.dao;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.wms.common.model.enums.OrderStatus;
import ru.yandex.market.wms.common.model.enums.OrderType;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.packing.LocationsRov;
import ru.yandex.market.wms.packing.enums.TicketType;
import ru.yandex.market.wms.packing.pojo.Ticket;

import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {"warehouse-timezone = Asia/Yekaterinburg"})
public class PackingTaskDaoTestEkb extends IntegrationTest {

    @Autowired
    private PackingTaskDao dao;

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/dao/packing_task/get_tickets_single/setup.xml", type = INSERT)
    void getTicketsSingleInEkb() {
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
                        .shippingDeadline(LocalDateTime.parse("2020-01-07T01:00:00"))
                        .build()
        );
    }
}
