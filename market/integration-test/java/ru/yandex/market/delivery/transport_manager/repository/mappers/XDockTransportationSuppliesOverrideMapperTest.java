package ru.yandex.market.delivery.transport_manager.repository.mappers;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.XDockTransportationSuppliesOverride;
import ru.yandex.market.delivery.transport_manager.domain.entity.XDockTransportationSuppliesOverrideType;

@DatabaseSetup("/repository/transportation/xdoc_transport.xml")
class XDockTransportationSuppliesOverrideMapperTest extends AbstractContextualTest {
    @Autowired
    private XDockTransportationSuppliesOverrideMapper mapper;

    @DatabaseSetup("/repository/xdock_transportation_supplies_override/xdock_transport_supplies_override.xml")
    @Test
    void latest() {
        softly.assertThat(mapper.latest(11L))
            .isEqualTo(new XDockTransportationSuppliesOverride().setId(2L).setTransportationId(11L)
                .setType(XDockTransportationSuppliesOverrideType.PREFER_SELECTED).setSupplyIds(List.of("4", "5", "6"))
                .setCreated(LocalDate.of(2021, 11, 2).atStartOfDay().toInstant(ZoneOffset.UTC)));
    }

    @Test
    void latestMissing() {
        softly.assertThat(mapper.latest(11L)).isNull();
    }

    @DatabaseSetup("/repository/xdock_transportation_supplies_override/xdock_transport_supplies_override.xml")
    @Test
    void list() {
        softly.assertThat(mapper.list(11L)).containsExactly(
            new XDockTransportationSuppliesOverride().setId(2L).setTransportationId(11L)
                .setType(XDockTransportationSuppliesOverrideType.PREFER_SELECTED).setSupplyIds(List.of("4", "5", "6"))
                .setCreated(LocalDate.of(2021, 11, 2).atStartOfDay().toInstant(ZoneOffset.UTC)),
            new XDockTransportationSuppliesOverride().setId(1L).setTransportationId(11L)
                .setType(XDockTransportationSuppliesOverrideType.PREFER_SELECTED).setSupplyIds(List.of("1", "2", "3"))
                .setCreated(LocalDate.of(2021, 11, 2).atStartOfDay().toInstant(ZoneOffset.UTC))
        );
    }

    @Test
    void getById() {

    }

    @DatabaseSetup("/repository/xdock_transportation_supplies_override/empty.xml")
    @ExpectedDatabase(
        value = "/repository/xdock_transportation_supplies_override/after/xdock_transport_supplies_override.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void insert() {
        mapper.insert(new XDockTransportationSuppliesOverride().setTransportationId(11L)
            .setType(XDockTransportationSuppliesOverrideType.AUTO).setSupplyIds(List.of("a", "b", "c")));
    }
}
