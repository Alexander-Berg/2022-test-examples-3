package ru.yandex.market.tpl.core.service.monitoring;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.external.delivery.sc.dto.ScRoutingResult;
import ru.yandex.market.tpl.core.test.TplMonitoringTestFactory;

import static org.assertj.core.api.Assertions.assertThat;

class TplRoutingLogDiffTest {

    public static final String EXPECTED_TPL_DIFF_COURIER_UID = "TPL_COURIER_UID";
    public static final String SAME_COURIER_ID = "SAME_COURIER_ID";
    public static final String NOT_REALLY_MATTER_TPL_ORDER_ID = "EXTERNAL_ORDER_ID";
    public static final String NOT_REALLY_MATTER_SC_ORDER_ID = "SC_EXTERNAL_ORDER_ID";

    @Test
    void getCouriersDiffSet_when_HasDiffers() {
        //given
        List<OrderRepository.OrderUser> tplOrders = Collections.singletonList(
                TplMonitoringTestFactory.createTplOrderUserItem(NOT_REALLY_MATTER_TPL_ORDER_ID, EXPECTED_TPL_DIFF_COURIER_UID)
        );

        List<ScRoutingResult.OrderCourier> scOrders = Collections.singletonList(
                TplMonitoringTestFactory.createScOrderCourierItem(NOT_REALLY_MATTER_SC_ORDER_ID, "SC_COURIER_ID")
        );


        //when
        Set<String> couriersDiffSet = TplRoutingLogDiff.of(tplOrders, scOrders).getCouriersDiffSet();

        //then
        assertThat(couriersDiffSet).hasSize(1);
        assertThat(couriersDiffSet.toArray()[0]).isEqualTo(EXPECTED_TPL_DIFF_COURIER_UID);
    }

    @Test
    void getCouriersDiffSet_when_HasMixedDiffers() {
        //given
        List<OrderRepository.OrderUser> tplOrders = Arrays.asList(
                TplMonitoringTestFactory.createTplOrderUserItem(NOT_REALLY_MATTER_TPL_ORDER_ID, EXPECTED_TPL_DIFF_COURIER_UID),
                TplMonitoringTestFactory.createTplOrderUserItem(NOT_REALLY_MATTER_TPL_ORDER_ID, SAME_COURIER_ID)
        );

        List<ScRoutingResult.OrderCourier> scOrders = Arrays.asList(
                TplMonitoringTestFactory.createScOrderCourierItem(NOT_REALLY_MATTER_SC_ORDER_ID, "SC_COURIER_ID"),
                TplMonitoringTestFactory.createScOrderCourierItem(NOT_REALLY_MATTER_SC_ORDER_ID, SAME_COURIER_ID)
        );


        //when
        Set<String> couriersDiffSet = TplRoutingLogDiff.of(tplOrders, scOrders).getCouriersDiffSet();

        //then
        assertThat(couriersDiffSet).hasSize(1);
        assertThat(couriersDiffSet.toArray()[0]).isEqualTo(EXPECTED_TPL_DIFF_COURIER_UID);
    }


    @Test
    void getCouriersDiffSet_hasNotDiffs() {
        //given
        List<OrderRepository.OrderUser> tplOrders = Collections.singletonList(
                TplMonitoringTestFactory.createTplOrderUserItem(NOT_REALLY_MATTER_TPL_ORDER_ID, SAME_COURIER_ID)
        );

        List<ScRoutingResult.OrderCourier> scOrders = Collections.singletonList(
                TplMonitoringTestFactory.createScOrderCourierItem(NOT_REALLY_MATTER_SC_ORDER_ID, SAME_COURIER_ID)
        );


        //when
        Set<String> couriersDiffSet = TplRoutingLogDiff.of(tplOrders, scOrders).getCouriersDiffSet();

        //then
        assertThat(couriersDiffSet).hasSize(0);
    }
}
