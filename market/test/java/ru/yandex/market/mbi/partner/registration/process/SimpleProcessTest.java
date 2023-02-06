package ru.yandex.market.mbi.partner.registration.process;

import java.util.Map;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import ru.yandex.market.common.retrofit.CommonRetrofitHttpExecutionException;
import ru.yandex.market.mbi.partner.registration.AbstractFunctionalTest;
import ru.yandex.market.mbi.partner.registration.util.CamundaTestUtil;
import ru.yandex.mj.generated.client.mbi_partner_registration.model.ProcessSearchRequest;
import ru.yandex.mj.generated.client.mbi_partner_registration.model.ProcessStartRequest;
import ru.yandex.mj.generated.client.mbi_partner_registration.model.ProcessType;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleProcessTest extends AbstractFunctionalTest {

    @Test
    void testSimpleProcess() throws Exception {
        var processInstance = partnerRegistrationProcessApiClient.startPartnerProcess(
                new ProcessStartRequest()
                        .processType(ProcessType.SIMPLE_PROCESS)
                        .businessKey("KEY")
                        .params(Map.of("name", "qq"))
        ).schedule().join().getResult();
        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine,
                processInstance.getProcessInstanceId()
        ));

        var process = partnerRegistrationProcessApiClient.getProcess(
                processInstance.getProcessInstanceId()
        ).schedule().join().getResult();

        Assertions.assertEquals(1.0, process.getParams().get("one"));

        CamundaTestUtil.checkIncidents(processEngine, processInstance.getProcessInstanceId());
    }

    @Test
    void testSearchSimpleProcess() throws Exception {
        var processInstance = partnerRegistrationProcessApiClient.startPartnerProcess(
                new ProcessStartRequest()
                        .processType(ProcessType.SIMPLE_PROCESS)
                        .businessKey("KEY")
                        .params(Map.of("name", "qq"))
        ).schedule().join().getResult();
        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine,
                processInstance.getProcessInstanceId()
        ));

        var processes = partnerRegistrationProcessApiClient.searchProcess(
                new ProcessSearchRequest().processInstanceId(processInstance.getProcessInstanceId())
        ).schedule().join().getResult();

        Assertions.assertEquals(1, processes.size());
        CamundaTestUtil.checkIncidents(processEngine, processInstance.getProcessInstanceId());
    }

    @Test
    void testProcessNotFound() {
        Exception e = Assertions.assertThrows(
                CompletionException.class,
                () -> partnerRegistrationProcessApiClient.getProcess("no way").schedule().join()
        );
        CommonRetrofitHttpExecutionException cause = (CommonRetrofitHttpExecutionException) e.getCause();
        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), cause.getHttpCode());
    }
}
