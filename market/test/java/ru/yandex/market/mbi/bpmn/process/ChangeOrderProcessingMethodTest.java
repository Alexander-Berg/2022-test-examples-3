package ru.yandex.market.mbi.bpmn.process;

import java.util.Map;

import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.mbi.bpmn.FunctionalTest;
import ru.yandex.market.mbi.bpmn.util.CamundaTestUtil;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.market.mbi.open.api.client.model.BooleanParamValueDTO;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.mbi.bpmn.model.enums.ProcessType.CHANGE_ORDER_PROCESSING_METHOD;

public class ChangeOrderProcessingMethodTest extends FunctionalTest {

    @Autowired
    public MbiOpenApiClient client;

    @Test
    @DisplayName("Проверяет успешное выполнение миграции")
    public void testSuccessProcess() throws InterruptedException {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        var processInstance = runtimeService.startProcessInstanceByKey(
            CHANGE_ORDER_PROCESSING_METHOD.getId(),
            TEST_BUSINESS_KEY,
            Map.of("partnerId", 1L, "uid", 1, "isPartnerInterface", true)
        );

        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
            processEngine,
            processInstance.getProcessInstanceId()
        ));

        CamundaTestUtil.checkIncidents(processEngine, processInstance);

        verifyAll();
    }

    private void verifyAll() {
        Mockito.verify(client).setBoolParam(
                1L,
                (BooleanParamValueDTO) new BooleanParamValueDTO()
                        .value(true)
                        .paramType(ParamType.CPA_IS_PARTNER_INTERFACE.name())
                        .partnerId(1L)
        );
        Mockito.verify(client).switchSelfCheck(1L, true, 1L);
        Mockito.verify(client).updateStockSettings(1L, true, 1L);
        Mockito.verify(client).updateStockIntervals(1L, true, 1L);
        Mockito.verify(client).updateShipmentDateCalculationRule(1L, true, 1L);
        Mockito.verify(client).updateOrderProcessing(1L, true, 1L);
        Mockito.verify(client).updateFf4ShopsPartner(1L, 1L);

        Mockito.verifyNoMoreInteractions(client);
    }
}
