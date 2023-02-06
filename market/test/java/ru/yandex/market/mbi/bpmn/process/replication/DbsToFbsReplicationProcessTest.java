package ru.yandex.market.mbi.bpmn.process.replication;

import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.market.partner.notification.client.PartnerNotificationClient;

import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.bpmn.FunctionalTest;
import ru.yandex.market.mbi.bpmn.process.replication.data.DbsToFbsReplicationForTestingData;
import ru.yandex.market.mbi.bpmn.util.CamundaTestUtil;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тестирует процесс репликации dbs в fbs.
 *
 * @author Ilya Zakharov
 * @see "dbs_to_fbs_replication.bpmn"
 */
public class DbsToFbsReplicationProcessTest extends FunctionalTest {
    @Autowired
    public MbiOpenApiClient client;

    @Autowired
    public MbiApiClient mbiApiClient;

    @Autowired
    public DataCampClient dataCampShopClient;

    @Autowired
    public PartnerNotificationClient partnerNotificationClient;

    @Test
    @DisplayName("Проверяет успешное выполнение репликации")
    public void testSuccessProcess() throws InterruptedException {
        DbsToFbsReplicationForTestingData data = new DbsToFbsReplicationForTestingData(client, mbiApiClient,
                dataCampShopClient, partnerNotificationClient);
        data.mockAll();
        RuntimeService runtimeService = processEngine.getRuntimeService();
        var processInstance = runtimeService.startProcessInstanceByKey(
                "dbs_to_fbs_replication",
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
