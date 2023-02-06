package ru.yandex.market.delivery.transport_manager.service.xdoc;

import java.util.List;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

class XDocSupplyItemsCountServiceTest extends AbstractContextualTest {
    @Autowired
    private XDocSupplyItemsCountService xDocSupplyItemsCountService;

    @DatabaseSetup({
        "/repository/transportation/xdoc_transport.xml",
        "/repository/tag/xdoc_transport_plan.xml",
        "/repository/tag/axapta_movement_order.xml",
        "/repository/transportation/xdoc_to_ff_transportations.xml",
        "/repository/transportation/xdoc_to_dc_transportations.xml",
    })
    @Test
    void getItemsCountBySuppliesToDcWithoutCheckingAxaptaMovement() {
        softly.assertThat(
            xDocSupplyItemsCountService.getItemsCountBySuppliesToDc(List.of("2100", "2300"))
        ).isEqualTo(Map.of(
            "2100", 3
        ));
    }


    @DatabaseSetup({
        "/repository/transportation/xdoc_transport.xml",
        "/repository/tag/xdoc_transport_plan.xml",
        "/repository/tag/axapta_movement_order.xml",
        "/repository/transportation/xdoc_to_ff_transportations.xml",
        "/repository/transportation/xdoc_to_dc_transportations.xml",
    })
    @Test
    void getItemsCountBySuppliesToDcWithoutCheckingAxaptaMovementByTmu() {
        softly.assertThat(
            xDocSupplyItemsCountService.getItemsCountBySuppliesToDc(List.of("TMU21", "TMU23"))
        ).isEqualTo(Map.of(
            "2100", 3
        ));
    }



    @DatabaseSetup({
        "/repository/transportation/xdoc_transport.xml",
        "/repository/tag/xdoc_transport_plan.xml",
        "/repository/tag/axapta_movement_order.xml",
        "/repository/transportation/xdoc_to_ff_transportations.xml",
        "/repository/transportation/xdoc_to_dc_transportations_tmu.xml",
    })
    @Test
    void getItemsCountBySuppliesToDcWithoutCheckingAxaptaMovementTmuNoRequest() {
        softly.assertThat(
            xDocSupplyItemsCountService.getItemsCountBySuppliesToDc(List.of("TMU21", "TMU23"))
        ).isEqualTo(Map.of(
            "TMU21", 3
        ));
    }

    @DatabaseSetup({
        "/repository/transportation/xdoc_transport.xml",
        "/repository/tag/xdoc_transport_plan.xml",
        "/repository/transportation/xdoc_to_ff_transportations_break_bulk_xdock.xml"
    })
    @Test
    void getItemsCountBySuppliesToDcWithoutCheckingAxaptaMovementBreakBulkXdock() {
        softly.assertThat(
            xDocSupplyItemsCountService.getItemsCountBySuppliesToDcbreakBulkXdock(List.of("2", "4"))
        ).isEqualTo(Map.of(
            "2", 3
        ));
    }

    @DatabaseSetup({
        "/repository/transportation/xdoc_transport.xml",
        "/repository/tag/xdoc_transport_plan.xml",
        "/repository/transportation/xdoc_to_ff_transportations_break_bulk_xdock.xml"
    })
    @Test
    void getItemsCountBySuppliesToDcWithoutCheckingAxaptaMovementBreakBulkXdockTmu() {
        softly.assertThat(
            xDocSupplyItemsCountService.getItemsCountBySuppliesToDcbreakBulkXdock(List.of("TMU2", "TMU4"))
        ).isEqualTo(Map.of(
            "2", 3
        ));
    }
}
