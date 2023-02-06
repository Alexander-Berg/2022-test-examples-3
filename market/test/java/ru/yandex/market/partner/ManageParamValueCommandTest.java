package ru.yandex.market.partner;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.api.cpa.CpaIsPartnerInterfaceSyncService;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.bpmn.client.MbiBpmnClient;
import ru.yandex.market.mbi.bpmn.client.model.ProcessInstanceRequest;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartInstance;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartResponse;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStatus;
import ru.yandex.market.mbi.bpmn.client.model.ProcessType;
import ru.yandex.market.mbi.environment.EnvironmentService;

@DbUnitDataSet(before = "ManageParamValueCommandTest.before.csv")
public class ManageParamValueCommandTest extends FunctionalTest {

    @Autowired
    private ManageParamValueCommand manageParamValueCommand;

    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private MbiBpmnClient mbiBpmnClient;

    @Test
    @DbUnitDataSet()
    void testSetApiBpmn() {
        environmentService.setValue(CpaIsPartnerInterfaceSyncService.CHANGE_ORDER_PROCESSING_METHOD_ENABLED, "true");
        Mockito.when(mbiBpmnClient.postProcess(Mockito.eq(
                new ProcessInstanceRequest()
                        .processType(ProcessType.CHANGE_ORDER_PROCESSING_METHOD)
                        .params(Map.of(
                                "uid", "11",
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

        CommandInvocation ci = new CommandInvocation(
                "manage-param-value",
                new String[]{"set", "100", "CPA_IS_PARTNER_INTERFACE", "false"},
                Collections.emptyMap()
        );
        manageParamValueCommand.executeCommand(ci, Mockito.mock(Terminal.class));

        Mockito.verify(mbiBpmnClient)
                .postProcess(
                        Mockito.eq(
                                new ProcessInstanceRequest()
                                        .processType(ProcessType.CHANGE_ORDER_PROCESSING_METHOD)
                                        .params(Map.of(
                                                "uid", "11",
                                                "partnerId", "100",
                                                "isPartnerInterface", "false",
                                                "operationId", "1",
                                                "partnerInterface", "false"
                                        ))
                        )
                );
    }

    @Test
    @DbUnitDataSet()
    void testDeleteApiBpmn() {
        environmentService.setValue(CpaIsPartnerInterfaceSyncService.CHANGE_ORDER_PROCESSING_METHOD_ENABLED, "true");
        Mockito.when(mbiBpmnClient.postProcess(Mockito.eq(
                new ProcessInstanceRequest()
                        .processType(ProcessType.CHANGE_ORDER_PROCESSING_METHOD)
                        .params(Map.of(
                                "uid", "11",
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

        CommandInvocation ci = new CommandInvocation(
                "manage-param-value",
                new String[]{"delete", "100", "CPA_IS_PARTNER_INTERFACE"},
                Collections.emptyMap()
        );
        manageParamValueCommand.executeCommand(ci, Mockito.mock(Terminal.class));

        Mockito.verify(mbiBpmnClient)
                .postProcess(
                        Mockito.eq(
                                new ProcessInstanceRequest()
                                        .processType(ProcessType.CHANGE_ORDER_PROCESSING_METHOD)
                                        .params(Map.of(
                                                "uid", "11",
                                                "partnerId", "100",
                                                "isPartnerInterface", "false",
                                                "operationId", "1",
                                                "partnerInterface", "false"
                                        ))
                        )
                );
    }
}
