package ru.yandex.market.deliveryintegrationtests.delivery.tests.carrierplanner;

import io.qameta.allure.Epic;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.market.tpl.carrier.planner.manual.run.ManualRunDto;
import ru.yandex.market.tsup.domain.entity.rating.StatusColorCode;
import ru.yandex.market.tsup.service.data_provider.entity.run.enums.RunStatus;

@Slf4j
@DisplayName("Carrier Planner Test")
@Epic("Carrier Planner")

public class CreateRunTest extends AbstractCarrierPlannerTest {

    private static final long AUTOTEST_USER_ID = 18248L;
    private static final long AUTOTEST_TRANSPORT_ID = 225L;

    @Test
    @DisplayName("Carrier Planner: создание рейса и отображение в ЦУПе")
    void createRunTest() {
        ManualRunDto newRun = CARRIER_PLANNER_STEPS.createRun();
        TSUP_STEPS.verifyRunInList(newRun.getId());
        TSUP_STEPS.verifyRunStatusColorCode(newRun.getId(), StatusColorCode.GRAY);
        TSUP_STEPS.verifyRunMovementState(newRun.getId(), RunStatus.NEW);
        CARRIER_PLANNER_STEPS.confirmRun(newRun.getId());
        TSUP_STEPS.verifyRunMovementState(newRun.getId(), RunStatus.CONFIRMED);
        TSUP_STEPS.verifyRunStatusColorCode(newRun.getId(), StatusColorCode.BROWN);
        CARRIER_PLANNER_STEPS.assignUserToRun(newRun.getId(), AUTOTEST_USER_ID);
        TSUP_STEPS.verifyRunMovingCourier(newRun.getId(), AUTOTEST_USER_ID, "Никаких", null);
        CARRIER_PLANNER_STEPS.assignTransportToRun(newRun.getId(), AUTOTEST_TRANSPORT_ID);
        TSUP_STEPS.verifyRunMovingCourier(newRun.getId(), AUTOTEST_USER_ID, "Никаких", "Х969УЕ");
        TSUP_STEPS.verifyRunStatusColorCode(newRun.getId(), StatusColorCode.GRAY);
    }
}
