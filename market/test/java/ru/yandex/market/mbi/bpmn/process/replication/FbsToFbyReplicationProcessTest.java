package ru.yandex.market.mbi.bpmn.process.replication;

import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.bpmn.FunctionalTest;
import ru.yandex.market.mbi.bpmn.process.replication.data.FbyReplicationForTestingData;
import ru.yandex.market.mbi.bpmn.util.CamundaTestUtil;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.market.mbi.open.api.exception.MbiOpenApiClientResponseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.mbi.bpmn.model.enums.ProcessType.FBS_TO_FBY_REPLICATION;

public class FbsToFbyReplicationProcessTest extends FunctionalTest {

    @Autowired
    public MbiOpenApiClient client;

    @Autowired
    public MbiApiClient mbiApiClient;

    @Autowired
    public DataCampClient dataCampShopClient;

    @Autowired
    private SaasService saasService;

    @Test
    @DisplayName("Проверяет успешное выполнение репликации")
    public void testSuccessProcess() throws InterruptedException {
        var data = new FbyReplicationForTestingData(client, mbiApiClient, dataCampShopClient, saasService);
        data.mockAll();
        RuntimeService runtimeService = processEngine.getRuntimeService();
        var processInstance = runtimeService.startProcessInstanceByKey(
                FBS_TO_FBY_REPLICATION.getId(),
                TEST_BUSINESS_KEY,
                data.params()
        );

        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine,
                processInstance.getProcessInstanceId()
        ));

        CamundaTestUtil.checkIncidents(processEngine, processInstance);
        data.verifyAll(processInstance.getProcessInstanceId());
    }

    @Test
    @DisplayName("Проверяет неуспешное создание партнера")
    public void testFailFbyCreation() throws InterruptedException {
        var data = new FbyReplicationForTestingData(client, mbiApiClient, dataCampShopClient, saasService);
        data.mockFailReplicateFbsPartner();
        RuntimeService runtimeService = processEngine.getRuntimeService();
        MbiOpenApiClientResponseException ex =
                assertThrows(MbiOpenApiClientResponseException.class, () ->
                        runtimeService.startProcessInstanceByKey(
                                FBS_TO_FBY_REPLICATION.getId(),
                                TEST_BUSINESS_KEY,
                                data.params()
                        )
                );
        assertEquals(400, ex.getHttpErrorCode());
    }
}
