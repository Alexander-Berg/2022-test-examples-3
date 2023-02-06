package ru.yandex.market.tpl.carrier.planner.controller.internal.getmovement;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.request.GetMovementRequest;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.market.tpl.carrier.planner.service.delivery.ds.GetMovementDsApiProcessor;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class GetMovementControllerTest extends BasePlannerWebTest {

    private final GetMovementDsApiProcessor getMovementDsApiProcessor;
    private final RunHelper runHelper;
    private final RunGenerator runGenerator;
    private final TestUserHelper testUserHelper;

    private String movementExternalId;

    @BeforeEach
    public void setup() {
        transactionTemplate.execute(tx -> {
            var user = testUserHelper.findOrCreateUser(UID);
            var transport = testUserHelper.findOrCreateTransport();
            Run run = runGenerator.generate();
            var userShift = runHelper.assignUserAndTransport(run, user, transport);
            movementExternalId = run.streamMovements().findAny().get().getExternalId();
            return null;
        });
    }

    @Test
    void getMovementCheckCar() {
        var getMovementResponse = getMovementDsApiProcessor.apiCall(createGetMovementRequest(movementExternalId), null);
        var car = getMovementResponse.getCourier().getCar();
        Assertions.assertEquals(car.getBrand(), "ВАЗ");
        Assertions.assertEquals(car.getModel(), "2114");
        Assertions.assertNull(car.getTrailerNumber());
    }

    private GetMovementRequest createGetMovementRequest(String externalId) {
        return new GetMovementRequest(ResourceId.builder().setYandexId(externalId).build());
    }
}
