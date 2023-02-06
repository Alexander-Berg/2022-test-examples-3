package ru.yandex.market.ff.health.solomon.jobs;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.health.solomon.SolomonPushClient;
import ru.yandex.market.ff.repository.replica.ShopRequestReplicaRepository;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;
import ru.yandex.market.ff.service.DateTimeService;
import ru.yandex.market.ff.service.registry.RegistryService;
import ru.yandex.market.ff.util.FileContentUtils;

public class SolomonMonitoringIncorrectRegistriesCountSchedulerTest extends IntegrationTest {

    private SolomonMonitoringIncorrectRegistriesCountScheduler executor;

    @Autowired
    private SolomonPushClient solomonPushClient;

    @Autowired
    private DateTimeService dateTimeService;

    @Autowired
    private ShopRequestReplicaRepository shopRequestReplicaRepository;

    @Autowired
    private RegistryService registryService;

    @Autowired
    private ConcreteEnvironmentParamService environmentParamService;

    @BeforeEach
    public void init() {
        executor = new SolomonMonitoringIncorrectRegistriesCountScheduler(
                solomonPushClient, dateTimeService, shopRequestReplicaRepository, registryService,
            environmentParamService);
    }

    @Test
    @DatabaseSetup({
            "classpath:scheduler/incorrect-registry-count/request.xml",
            "classpath:scheduler/incorrect-registry-count/before.xml"
    })
    public void dbQueueSolomonMonitoringCreatedCorrect() throws IOException, JSONException {
        String expectedMonitoringToSolomon = FileContentUtils.getFileContent(
                "scheduler/incorrect-registry-count/monitoring-to-solomon.json");
        executor.calculateAndSend();
        ArgumentCaptor<String> actualMonitoringToSolomonCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(solomonPushClient).push(actualMonitoringToSolomonCaptor.capture());
        String actualMonitoringToSolomon = actualMonitoringToSolomonCaptor.getValue();
        JSONAssert.assertEquals(expectedMonitoringToSolomon,
        actualMonitoringToSolomon, JSONCompareMode.NON_EXTENSIBLE);
    }

}
