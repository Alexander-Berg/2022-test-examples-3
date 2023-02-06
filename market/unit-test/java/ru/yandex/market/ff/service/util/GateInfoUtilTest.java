package ru.yandex.market.ff.service.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.model.dto.warehouse.schedule.GateInfo;
import ru.yandex.market.ff.model.dto.warehouse.schedule.WarehouseScheduleWithGatesDTO;
import ru.yandex.market.ff.model.dto.warehouse.schedule.WorkingDay;
import ru.yandex.market.logistics.management.entity.type.GateTypeResponse;

import static com.google.common.collect.Lists.newArrayList;

class GateInfoUtilTest {

    private static final LocalDate DEFAULT_DATE = LocalDate.of(2019, 10, 10);
    private static final List<WorkingDay> WORKING_DAYS =
            newArrayList(new WorkingDay(
                    DEFAULT_DATE, LocalTime.of(0, 0), LocalTime.of(4, 0))
            );
    SoftAssertions assertions;

    GateInfo inboundGate1 = new GateInfo(0L, "0", true, EnumSet.of(GateTypeResponse.INBOUND));
    GateInfo inboundGate2 = new GateInfo(1L, "1", true, EnumSet.of(GateTypeResponse.INBOUND));
    GateInfo outboundGate1 = new GateInfo(2L, "2", true, EnumSet.of(GateTypeResponse.OUTBOUND));
    GateInfo outboundGate2 = new GateInfo(3L, "3", true, EnumSet.of(GateTypeResponse.OUTBOUND));

    WarehouseScheduleWithGatesDTO warehouseScheduleWithGatesDTO =
            new WarehouseScheduleWithGatesDTO(
                    Lists.newArrayList(inboundGate1, inboundGate2, outboundGate1, outboundGate2), WORKING_DAYS);

    @BeforeEach
    public void createAssertions() {
        assertions = new SoftAssertions();
    }

    @Test
    void getInboundGatesTest() {
        List<GateInfo> inboundGatesInfo = GateInfoUtil.getInboundGatesInfo(warehouseScheduleWithGatesDTO);
        assertions.assertThat(inboundGatesInfo).containsOnly(inboundGate1, inboundGate2);
    }

    @Test
    void getOutboundGatesTest() {
        List<GateInfo> inboundGatesInfo = GateInfoUtil.getOutboundGatesInfo(warehouseScheduleWithGatesDTO);
        assertions.assertThat(inboundGatesInfo).containsOnly(outboundGate1, outboundGate2);
    }
}
