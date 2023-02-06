package ru.yandex.market.wms.packing.dao;

import java.time.Instant;

import ru.yandex.market.wms.common.model.enums.OrderType;
import ru.yandex.market.wms.packing.LocationsRov;
import ru.yandex.market.wms.packing.enums.TicketType;
import ru.yandex.market.wms.packing.pojo.Ticket;

import static ru.yandex.market.wms.common.utils.CollectionUtils.asSet;

public interface TicketTestData {

    static Ticket ticket666() {
        return Ticket.builder()
                .orderKey("ORD0666")
                .orderType(OrderType.OUTBOUND_FIT)
                .externOrderKey("EXT0666")
                .type(TicketType.SORTABLE)
                .editDate(Instant.parse("2020-01-01T06:50:03Z"))
                .sourceLoc("SS1")
                .sortingCells(asSet(LocationsRov.SS1_CELL1, LocationsRov.SS1_CELL2))
                .build();
    }

    static Ticket ticket777() {
        return Ticket.builder()
                .orderKey("ORD0777")
                .orderType(OrderType.STANDARD)
                .externOrderKey("EXT0777")
                .type(TicketType.SORTABLE)
                .editDate(Instant.parse("2020-01-01T06:50:03Z"))
                .sourceLoc("SS1")
                .sortingCells(asSet(LocationsRov.SS1_CELL1, LocationsRov.SS1_CELL2))
                .build();
    }

    static Ticket ticket778() {
        return Ticket.builder()
                .orderKey("ORD0778")
                .orderType(OrderType.STANDARD)
                .externOrderKey("EXT0778")
                .type(TicketType.SORTABLE)
                .editDate(Instant.parse("2020-01-01T06:50:04Z"))
                .sourceLoc("SS1")
                .sortingCells(asSet(LocationsRov.SS1_CELL3))
                .build();
    }

    static Ticket ticket779() {
        return Ticket.builder()
                .orderKey("ORD0779")
                .orderType(OrderType.STANDARD)
                .externOrderKey("EXT0779")
                .type(TicketType.SORTABLE)
                .editDate(Instant.parse("2020-01-01T06:50:05Z"))
                .sourceLoc("SS2")
                .sortingCells(asSet(LocationsRov.SS2_CELL3, LocationsRov.SS2_CELL4))
                .build();
    }
}
