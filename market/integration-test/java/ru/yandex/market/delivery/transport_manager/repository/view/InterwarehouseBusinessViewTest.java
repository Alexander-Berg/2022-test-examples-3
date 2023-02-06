package ru.yandex.market.delivery.transport_manager.repository.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.Data;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

public class InterwarehouseBusinessViewTest extends AbstractContextualTest {
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    @DatabaseSetup("/repository/view/transportation_task_view.xml")
    void test() {
        List<ActiveInterwarehouse> transportations = jdbcTemplate.query(
            "SELECT * FROM active_interwarehouse_transportations",
            new BeanPropertyRowMapper<>(ActiveInterwarehouse.class)
        );

        ActiveInterwarehouse outboundSent =
            new ActiveInterwarehouse()
                .setTaskId(1L)
                .setTransportationId(5L)
                .setOutboundWmsId("0000064025")
                .setInboundWmsId("0000064026")
                .setOutboundLogisticsPoint("Софьино")
                .setInboundLogisticsPoint("Томилино")
                .setStatus("OUTBOUND_SENT")
                .setCreated(LocalDateTime.parse("2021-07-22T12:30"))
                .setPlanOutboundDate(LocalDate.of(2021, 7, 30))
                .setPlanInboundDate(LocalDate.of(2021, 7, 31));

        ActiveInterwarehouse inboundSent =
            new ActiveInterwarehouse()
                .setTaskId(1L)
                .setTransportationId(2L)
                .setOutboundWmsId("0000064023")
                .setInboundWmsId("0000064024")
                .setOutboundLogisticsPoint("Софьино")
                .setInboundLogisticsPoint("Томилино")
                .setStatus("INBOUND_SENT")
                .setCreated(LocalDateTime.parse("2021-07-23T12:00"))
                .setPlanOutboundDate(LocalDate.of(2021, 7, 30))
                .setPlanInboundDate(LocalDate.of(2021, 7, 31))
                .setFactOutboundTime(LocalDateTime.parse("2021-07-28T17:30:00.00"));

        assertContainsExactlyInAnyOrder(transportations, outboundSent, inboundSent);
    }

    @Data
    @Accessors(chain = true)
    private static class ActiveInterwarehouse {
        private Long taskId;
        private Long transportationId;
        private String outboundWmsId;
        private String inboundWmsId;
        private String outboundLogisticsPoint;
        private String inboundLogisticsPoint;
        private String status;
        private LocalDateTime created;
        private LocalDate planOutboundDate;
        private LocalDate planInboundDate;
        private LocalDateTime factOutboundTime;
    }
}
