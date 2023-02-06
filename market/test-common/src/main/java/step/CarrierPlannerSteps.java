package step;

import client.CarrierPlannerClient;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import toolkit.Retrier;

import ru.yandex.market.tpl.carrier.planner.manual.run.ManualRunDto;

@Slf4j
public class CarrierPlannerSteps {

    private static final CarrierPlannerClient CARRIERPLANNER = new CarrierPlannerClient();

    private static final long AUTOTEST_USER_ID = 18248L;
    private static final long AUTOTEST_TRANSPORT_ID = 225L;

    @Step("Создаём дефолтный рейс в курьерке между Пирожончиками и Томилино")
    public ManualRunDto createRun() {
        log.debug("Creating run with 2 items");
        return Retrier.retry(() -> CARRIERPLANNER.createRun());
    }

    @Step("Подтверждаем рейс")
    public void confirmRun(long runId) {
        log.debug("Confirming run");
        Retrier.retry(() -> CARRIERPLANNER.confirmRun(runId));
    }

    @Step("Назначаем водителя на рейс")
    public void assignUserToRun(long runId, long userId) {
        log.debug("Assigning user to run");
        Retrier.retry(() -> CARRIERPLANNER.assignUser(runId, AUTOTEST_USER_ID));
    }

    @Step("Назначаем машину на рейс")
    public void assignTransportToRun(long runId, long transportId) {
        log.debug("Assigning transport to run");
        Retrier.retry(() -> CARRIERPLANNER.assignTransport(runId, AUTOTEST_TRANSPORT_ID));
    }

}
