package ru.yandex.market.wms.packing.dao;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.packing.pojo.Ticket;

import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static com.github.springtestdbunit.annotation.DatabaseOperation.REFRESH;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.wms.packing.dao.TicketTestData.ticket666;
import static ru.yandex.market.wms.packing.dao.TicketTestData.ticket777;
import static ru.yandex.market.wms.packing.dao.TicketTestData.ticket778;
import static ru.yandex.market.wms.packing.dao.TicketTestData.ticket779;


class PackingtaskdaotestRovGetticketsIncludingWithdrawal extends IntegrationTest {

    @Autowired
    private PackingTaskDao dao;

    /**
     * 1 изъятие
     * 1 заказ на станции 1 в двух ячейках
     * 1 заказ на станции 1 в одной ячейке
     * 1 заказ на станции 2 в двух ячейках
     */
    @Test
    @DatabaseSetup("/db/locations_setup_rov.xml")
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/dao/packing_task/get_tickets_multiple/setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/dao/packing_task/get_tickets_multiple/withdrawal_orders.xml", type = REFRESH)
    void getTickets() {
        List<Ticket> tickets = dao.getTickets();
        assertThat(tickets).containsExactlyInAnyOrder(ticket666(), ticket777(), ticket778(), ticket779());
    }
}
