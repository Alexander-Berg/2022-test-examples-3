package ru.yandex.market.mbi.bpmn.process.replication;

import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.bpmn.FunctionalTest;
import ru.yandex.market.mbi.bpmn.client.PartnerStatusServiceClient;
import ru.yandex.market.mbi.bpmn.process.replication.data.DbsReplicationForTestingData;
import ru.yandex.market.mbi.bpmn.util.CamundaTestUtil;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.mbi.bpmn.model.enums.ProcessType.DBS_TO_DBS_REPLICATION;

/**
 * Тестирует процесс репликации dbs в dbs.
 *
 * @author Vadim Lyalin
 * @see "dbs_to_dbs_replication.bpmn"
 */
public class DbsToDbsReplicationProcessTest extends FunctionalTest {
    @Autowired
    public MbiOpenApiClient client;

    @Autowired
    public MbiApiClient mbiApiClient;

    @Autowired
    public DataCampClient dataCampShopClient;

    @Autowired
    public PartnerStatusServiceClient partnerStatusServiceClient;

    @Test
    @DisplayName("Проверяет успешное выполнение репликации")
    public void testSuccessProcess() throws InterruptedException {
        DbsReplicationForTestingData data = new DbsReplicationForTestingData(client, mbiApiClient,
                dataCampShopClient, partnerStatusServiceClient);
        data.mockAll();
        RuntimeService runtimeService = processEngine.getRuntimeService();
        var processInstance = runtimeService.startProcessInstanceByKey(
                DBS_TO_DBS_REPLICATION.getId(),
                TEST_BUSINESS_KEY,
                data.params()
        );

        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine,
                processInstance.getProcessInstanceId()
        ));

        CamundaTestUtil.checkIncidents(processEngine, processInstance);
        data.verifyAll();
    }
}
