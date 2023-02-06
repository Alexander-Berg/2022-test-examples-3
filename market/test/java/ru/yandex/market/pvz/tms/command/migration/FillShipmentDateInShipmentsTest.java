package ru.yandex.market.pvz.tms.command.migration;

import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.pvz.core.domain.dispatch.model.DispatchType;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRequestData;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.tms.command.migration.CreateOrderAdditionalInfoCommand.COMMAND_NAME;

@TransactionlessEmbeddedDbTest
@Import({
        FillShipmentDateInShipments.class,
})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class FillShipmentDateInShipmentsTest {

    private final FillShipmentDateInShipments command;
    private final TestOrderFactory orderFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TestableClock clock;

    @MockBean
    private Terminal terminal;

    @MockBean
    private PrintWriter printWriter;

    @Disabled
    @Test
    void setShipmentDate() {
        clock.setFixed(Instant.now().truncatedTo(ChronoUnit.SECONDS), ZoneId.of("UTC+3"));
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var pickupPointAuthInfo = new PickupPointRequestData(
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), 1L,
                pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod()
        );

        var order = orderFactory.createReadyForReturnOrder(pickupPoint);
        var shipment = orderFactory.createShipmentDispatch(
                pickupPointAuthInfo, order.getExternalId(), DispatchType.EXPIRED
        );
        assertThat(shipment.getShipmentDate()).isNotNull();

        var order2 = orderFactory.createReadyForReturnOrder(pickupPoint);
        var shipment2 = orderFactory.createShipmentDispatch(
                pickupPointAuthInfo, order2.getExternalId(), DispatchType.EXPIRED
        );
        assertThat(shipment2.getShipmentDate()).isNotNull();

        String sql = "UPDATE shipment SET shipment_date = null WHERE id = :id";
        SqlParameterSource namedParameters = new MapSqlParameterSource("id", shipment.getId());
        jdbcTemplate.update(sql, namedParameters);
        namedParameters = new MapSqlParameterSource("id", shipment2.getId());
        jdbcTemplate.update(sql, namedParameters);

        sql = "SELECT shipment_date FROM shipment WHERE id IN (:ids)";
        List<LocalDate> dates = jdbcTemplate.queryForList(
                sql, Map.of("ids", List.of(shipment.getId(), shipment2.getId())), LocalDate.class
        );
        assertThat(dates.size()).isEqualTo(2);
        assertThat(dates.get(0)).isNull();
        assertThat(dates.get(1)).isNull();

        when(terminal.getWriter()).thenReturn(printWriter);
        command.executeCommand(new CommandInvocation(COMMAND_NAME, new String[]{}, Collections.emptyMap()), terminal);

        dates = jdbcTemplate.queryForList(
                sql, Map.of("ids", List.of(shipment.getId(), shipment2.getId())), LocalDate.class
        );
        assertThat(dates.get(0)).isEqualTo(LocalDate.ofInstant(clock.instant(), clock.getZone()));
        assertThat(dates.get(1)).isEqualTo(LocalDate.ofInstant(clock.instant(), clock.getZone()));
    }

}
