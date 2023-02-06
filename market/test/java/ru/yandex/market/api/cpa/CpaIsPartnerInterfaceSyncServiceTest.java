package ru.yandex.market.api.cpa;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.mbi.bpmn.client.MbiBpmnClient;
import ru.yandex.market.mbi.bpmn.client.model.ProcessInstanceRequest;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartInstance;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartResponse;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStatus;
import ru.yandex.market.mbi.bpmn.client.model.ProcessType;

public class CpaIsPartnerInterfaceSyncServiceTest extends FunctionalTest {

    @Autowired
    private MbiBpmnClient mbiBpmnClient;

    @Autowired
    private CpaIsPartnerInterfaceSyncService cpaIsPartnerInterfaceSyncService;

    @Test
    @DbUnitDataSet(before = "CpaIsPartnerInterfaceSyncServiceTest.before.csv")
    void testStartProcess() {
        Mockito.when(mbiBpmnClient.postProcess(Mockito.eq(
                new ProcessInstanceRequest()
                        .processType(ProcessType.CHANGE_ORDER_PROCESSING_METHOD)
                        .params(Map.of(
                                "uid", "10",
                                "partnerId", "100",
                                "isPartnerInterface", "true",
                                "operationId", "1",
                                "partnerInterface", "true"
                        ))
        ))).thenReturn(
                new ProcessStartResponse().records(List.of(
                        (ProcessStartInstance) new ProcessStartInstance()
                                .started(true)
                                .processInstanceId("id")
                                .status(ProcessStatus.ACTIVE)
                ))
        );
        cpaIsPartnerInterfaceSyncService.startBpmnProcess(10, 100, true);
        Mockito.verify(mbiBpmnClient)
                .postProcess(
                        Mockito.eq(
                                new ProcessInstanceRequest()
                                        .processType(ProcessType.CHANGE_ORDER_PROCESSING_METHOD)
                                        .params(Map.of(
                                                "uid", "10",
                                                "partnerId", "100",
                                                "isPartnerInterface", "true",
                                                "operationId", "1",
                                                "partnerInterface", "true"
                                        ))
                        )
                );
    }
}
