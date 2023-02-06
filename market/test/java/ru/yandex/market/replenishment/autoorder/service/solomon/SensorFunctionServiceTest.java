package ru.yandex.market.replenishment.autoorder.service.solomon;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.solomon.pusher.SolomonPusher;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
public class SensorFunctionServiceTest extends FunctionalTest {

    @Autowired
    private SensorFunctionService sensorFunctionService;

    @Autowired
    private SolomonPusher solomonPusher;

    @Test
    @DbUnitDataSet(before = "SensorFunctionTask.before.csv")
    public void test() {
        sensorFunctionService.refreshSensors();
        Mockito.verify(solomonPusher, Mockito.atLeastOnce()).push(Mockito.any());
    }
}
