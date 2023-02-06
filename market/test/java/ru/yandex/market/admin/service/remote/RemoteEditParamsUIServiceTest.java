package ru.yandex.market.admin.service.remote;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.api.cpa.CpaIsPartnerInterfaceSyncService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.mbi.bpmn.client.MbiBpmnClient;
import ru.yandex.market.mbi.bpmn.client.model.ProcessInstanceRequest;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartInstance;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartResponse;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStatus;
import ru.yandex.market.mbi.bpmn.client.model.ProcessType;
import ru.yandex.market.mbi.environment.EnvironmentService;

@DbUnitDataSet(before = "RemoteEditParamsUIServiceTest.before.csv")
public class RemoteEditParamsUIServiceTest extends FunctionalTest {

    @Autowired
    private RemoteEditParamsUIService remoteEditParamsUIService;

    @Autowired
    private MbiBpmnClient mbiBpmnClient;

    @Autowired
    private EnvironmentService environmentService;

    @Test
    void testSetPiApiParamBpmn() {
        environmentService.setValue(CpaIsPartnerInterfaceSyncService.CHANGE_ORDER_PROCESSING_METHOD_ENABLED, "true");
        Mockito.when(mbiBpmnClient.postProcess(Mockito.eq(
                new ProcessInstanceRequest()
                        .processType(ProcessType.CHANGE_ORDER_PROCESSING_METHOD)
                        .params(Map.of(
                                "uid", "0",
                                "partnerId", "100",
                                "isPartnerInterface", "false",
                                "operationId", "1",
                                "partnerInterface", "false"
                        ))
        ))).thenReturn(
                new ProcessStartResponse().records(List.of(
                        (ProcessStartInstance) new ProcessStartInstance()
                                .started(true)
                                .processInstanceId("id")
                                .status(ProcessStatus.ACTIVE)
                ))
        );

        remoteEditParamsUIService.editParam(100, ParamType.CPA_IS_PARTNER_INTERFACE.getId(), "false");

        Mockito.verify(mbiBpmnClient)
                .postProcess(
                        Mockito.eq(
                                new ProcessInstanceRequest()
                                        .processType(ProcessType.CHANGE_ORDER_PROCESSING_METHOD)
                                        .params(Map.of(
                                                "uid", "0",
                                                "partnerId", "100",
                                                "isPartnerInterface", "false",
                                                "operationId", "1",
                                                "partnerInterface", "false"
                                        ))
                        )
                );
    }
}
