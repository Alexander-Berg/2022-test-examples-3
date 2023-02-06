package ru.yandex.market.mbi.bpmn.process.polling;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerIndexedWithBusinessDTO;
import ru.yandex.market.mbi.bpmn.FunctionalTest;
import ru.yandex.market.mbi.bpmn.model.enums.ProcessType;
import ru.yandex.market.mbi.bpmn.task.polling.PollingTaskType;
import ru.yandex.market.mbi.bpmn.util.CamundaTestUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willAnswer;

public class IndexedFromNewBusinessTaskTest extends FunctionalTest {
    private static final long PARTNER_ID = 777;
    private static final long BUSINESS_ID = 100;
    private static final long OPERATION_ID = 1L;


    @Autowired
    private MbiApiClient mbiApiClient;

    @Test
    @DisplayName("Запрашиваем проверку индексации. Первый раз ждем, второй получаем результат")
    void migrationFailedUpdateSucceed() throws InterruptedException {
        //given
        willAnswer(invocation -> PartnerIndexedWithBusinessDTO.no(PARTNER_ID, BUSINESS_ID))
                .willAnswer(invocation -> PartnerIndexedWithBusinessDTO.yes(PARTNER_ID, BUSINESS_ID))
                .given(mbiApiClient)
                .isPartnerIndexedWithBusiness(eq(PARTNER_ID), eq(BUSINESS_ID));
        //when
        Map<String, Object> params = Map.of(
                "params", Map.of("entityId", PARTNER_ID, "businessId", BUSINESS_ID),
                "pollingTaskType", PollingTaskType.INDEXED_FROM_NEW_BUSINESS,
                "timerDuration", Duration.of(5, ChronoUnit.MINUTES).toString(),
                "operationId", OPERATION_ID
        );
        ProcessInstance processInstance =
                CamundaTestUtil.invoke(processEngine, ProcessType.POLLING_TASK.getId(), params);
        assertNotNull(processInstance);

        //then
        //Проверили, что все выполнение завершилось
        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine,
                processInstance.getProcessInstanceId()
        ));
    }

}
