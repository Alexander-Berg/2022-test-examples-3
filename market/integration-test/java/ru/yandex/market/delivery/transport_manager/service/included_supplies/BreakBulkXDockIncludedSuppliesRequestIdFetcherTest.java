package ru.yandex.market.delivery.transport_manager.service.included_supplies;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.dto.RegisterUnitBarcodeAndType;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitType;

class BreakBulkXDockIncludedSuppliesRequestIdFetcherTest extends AbstractContextualTest {
    @Autowired
    private BreakBulkXDockIncludedSuppliesRequestIdFetcher fetcher;

    @DisplayName("Получить id реквестов по штрихкодам и большому перемещению для Break Bulk XDock")
    @Test
    @DatabaseSetup(
        "/repository/transportation_unit/transportation_with_plan_inbound_register_units_break_bulk_xdock.xml"
    )
    void getRequestIds() {
        softly
            .assertThat(fetcher.getRequestIds(
                    List.of(new RegisterUnitBarcodeAndType("abc", UnitType.PALLET)),
                    new Transportation()
                        .setOutboundUnit(new TransportationUnit().setLogisticPointId(40L))
                        .setInboundUnit(new TransportationUnit().setLogisticPointId(30L))
                )
            )
            .containsExactly(123456L);
    }
}
