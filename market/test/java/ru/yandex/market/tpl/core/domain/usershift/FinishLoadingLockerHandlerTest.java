package ru.yandex.market.tpl.core.domain.usershift;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.task.LockerSubtaskType;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliverySubtaskStatus;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;

class FinishLoadingLockerHandlerTest {

    @Test
    void falidation_Fail() {

        //given
        LockerDeliveryTask ldt = buildLockerDeliveryTask(LockerDeliverySubtaskStatus.NOT_STARTED);
        ScanRequest scanRequest = buildScanRequest();

        //when
        Assertions.assertThrows( TplInvalidParameterException.class,
                () -> FinishLoadingLockerHandler.builder()
                .scanRequest(scanRequest)
                .build()
                .handler(ldt));
    }

    @Test
    void falidation_Success() {

        //given
        LockerDeliveryTask ldt = buildLockerDeliveryTask(LockerDeliverySubtaskStatus.FAILED);
        ScanRequest scanRequest = buildScanRequest();

        //when
        FinishLoadingLockerHandler.builder()
                .scanRequest(scanRequest)
                .build()
                .handler(ldt);
    }

    @NotNull
    private LockerDeliveryTask buildLockerDeliveryTask(LockerDeliverySubtaskStatus failed) {
        LockerDeliveryTask ldt = new LockerDeliveryTask();

        RoutePoint routePoint = new RoutePoint();
        routePoint.setUserShift(new UserShift());
        ldt.setRoutePoint(routePoint);

        LockerSubtask lockerSubtask1 = buildSubTaskDelivery(1L, LockerDeliverySubtaskStatus.NOT_STARTED, ldt);
        LockerSubtask lockerSubtask2 = buildSubTaskDelivery(2L, failed, ldt);
        ldt.setSubtasks(List.of(
                lockerSubtask1,
                lockerSubtask2
        ));
        return ldt;
    }

    private ScanRequest buildScanRequest() {
        return ScanRequest.builder()
                .successfullyScannedOrders(
                        List.of(1L))
                .build();
    }


    private LockerSubtask buildSubTaskDelivery(long l, LockerDeliverySubtaskStatus notStarted, LockerDeliveryTask ldt) {
        LockerSubtask lockerSubtask = new LockerSubtask();
        lockerSubtask.setType(LockerSubtaskType.DELIVERY);
        lockerSubtask.setOrderId(l);
        lockerSubtask.setStatus(notStarted);
        lockerSubtask.setTask(ldt);
        return lockerSubtask;

    }
}
