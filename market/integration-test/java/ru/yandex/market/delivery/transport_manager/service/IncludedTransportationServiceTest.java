package ru.yandex.market.delivery.transport_manager.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterType;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.service.included_supplies.AssemblageType;

class IncludedTransportationServiceTest extends AbstractContextualTest {
    @Autowired
    private IncludedTransportationService includedTransportationService;

    @DatabaseSetup({
        "/repository/transportation_unit/transportation_with_plan_inbound_register_units.xml",
        "/repository/transportation/xdoc_transport.xml"
    })
    @Test
    void getIncludedRequestIds() {
        Transportation t = new Transportation()
            .setOutboundUnit(new TransportationUnit().setId(12L).setLogisticPointId(2L));
        softly
            .assertThat(
                includedTransportationService.findIncludedRequestIds(t, RegisterType.PLAN, AssemblageType.XDOCK)
            )
            .containsExactly(123456L);
    }

    @DatabaseSetup({
        "/repository/transportation_unit/transportation_with_plan_inbound_register_units.xml",
        "/repository/transportation/xdoc_transport.xml",
        "/repository/tag/xdoc_transport_plan.xml",
    })
    @ExpectedDatabase(
        value = "/repository/tag/after/xdoc_transport_plan.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void updateTagsPlan() {
        Transportation t = new Transportation()
            .setId(11L)
            .setTransportationType(TransportationType.XDOC_TRANSPORT)
            .setOutboundUnit(new TransportationUnit().setId(12L).setLogisticPointId(2L));
        includedTransportationService.updateTags(t, RegisterType.PLAN, AssemblageType.XDOCK);
    }

    @DatabaseSetup({
        "/repository/transportation_unit/transportation_with_plan_inbound_register_units.xml",
        "/repository/transportation/xdoc_transport_with_plan_and_fact_inbound.xml",
        "/repository/tag/xdoc_transport_fact.xml",
    })
    @ExpectedDatabase(
        value = "/repository/tag/after/xdoc_transport_fact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void updateTagsFact() {
        Transportation t = new Transportation()
            .setId(11L)
            .setTransportationType(TransportationType.XDOC_TRANSPORT)
            .setOutboundUnit(new TransportationUnit().setId(12L).setLogisticPointId(2L));
        includedTransportationService.updateTags(t, RegisterType.FACT, AssemblageType.XDOCK);
    }

    @Test
    void updateTagsWrongTransportationType() {
        Transportation t = new Transportation()
            .setId(11L)
            .setTransportationType(TransportationType.XDOC_PARTNER_SUPPLY_TO_FF)
            .setOutboundUnit(new TransportationUnit().setId(12L).setLogisticPointId(2L));
        softly.assertThatThrownBy(() -> includedTransportationService.updateTags(
                t,
                RegisterType.FACT,
                AssemblageType.XDOCK
            ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Transportation 11 type is XDOC_PARTNER_SUPPLY_TO_FF should be XDOC_TRANSPORT");
    }

    @Test
    void updateTagsWrongRegisterType() {
        Transportation t = new Transportation()
            .setId(11L)
            .setTransportationType(TransportationType.XDOC_TRANSPORT)
            .setOutboundUnit(new TransportationUnit().setId(12L).setLogisticPointId(2L));
        softly.assertThatThrownBy(() -> includedTransportationService.updateTags(
                t,
                RegisterType.DENIED,
                AssemblageType.XDOCK
            ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported register type DENIED");
    }

    @DatabaseSetup({
        "/repository/transportation_unit/transportation_with_plan_inbound_register_units.xml",
        "/repository/transportation/xdoc_transport.xml",
        "/repository/tag/xdoc_transport_plan.xml",
    })
    @ExpectedDatabase(
        value = "/repository/tag/after/xdoc_transport_plan2.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void updateTagsAnotherType() {
        Transportation t = new Transportation()
            .setId(11L)
            .setTransportationType(TransportationType.XDOC_TRANSPORT)
            .setOutboundUnit(new TransportationUnit().setId(12L));
        includedTransportationService.updateTags(t, RegisterType.FACT, AssemblageType.XDOCK);
    }

    @DatabaseSetup({
        "/repository/transportation/xdoc_transport.xml",
        "/repository/tag/xdoc_transport_plan.xml",
    })
    @ExpectedDatabase(
        value = "/repository/tag/after/empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void updateZeroTags() {
        Transportation t = new Transportation()
            .setId(11L)
            .setTransportationType(TransportationType.XDOC_TRANSPORT)
            .setOutboundUnit(new TransportationUnit().setId(12L));
        includedTransportationService.updateTags(t, RegisterType.PLAN, AssemblageType.XDOCK);
    }

    @DatabaseSetup({
        "/repository/transportation/xdoc_transport.xml",
        "/repository/tag/xdoc_transport_plan.xml",
    })
    @ExpectedDatabase(
        value = "/repository/tag/after/empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void removeIncludedSupplyTags() {
        includedTransportationService.removeIncludedSupplyTags(11L);
    }

    @DatabaseSetup({
        "/repository/transportation/xdoc_transport.xml",
        "/repository/transportation/xdoc_to_ff_transportations_break_bulk_xdock.xml",
        "/repository/tag/xdoc_transport_fact.xml"
    })
    @Test
    void getIncludedSupplyTransportations() {
        softly.assertThat(includedTransportationService.getIncludedSupplyTransportations(
                new Transportation().setId(11L)
            ))
            .extracting(Transportation::getId)
            .containsExactlyInAnyOrder(1L);
    }
}
