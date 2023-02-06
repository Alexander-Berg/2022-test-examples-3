package ru.yandex.market.logistic.gateway.service.executor;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.config.properties.FlowMapProperties;
import ru.yandex.market.logistic.gateway.config.properties.RetryProperties;
import ru.yandex.market.logistic.gateway.service.flow.FlowService;
import ru.yandex.market.logistic.gateway.service.flow.TaskProcessService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.logistic.api.model.common.PartnerType.DROPSHIP;
import static ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow.FF_CREATE_ORDER;

public class FlowServiceTest extends AbstractIntegrationTest {

    private static final long TASK_ID = 1;
    private static final String ERROR_MESSAGE = "Task error";

    @Autowired
    private FlowService flowService;

    @MockBean
    private TaskProcessService taskProcessService;

    @Autowired
    private FlowMapProperties flowMapProperties;

    @Before
    public void setup() {
        RetryProperties props = flowMapProperties.getRetryPropertiesByTag()
            .get("EXPRESS")
            .get(DROPSHIP)
            .get(FF_CREATE_ORDER);

        props.setIntervals(List.of(1, 2, 3));
        props.setLimit(3);
    }

    @Test
    @DatabaseSetup("classpath:repository/state/partner_id_not_exist.xml")
    public void retryIfPartnerIdNotExist() {
        flowService.retryFlow(TASK_ID, new RuntimeException(ERROR_MESSAGE));
        verify(taskProcessService).retryTask(eq(1L), eq(60), anyString());
    }

    @Test
    @DatabaseSetup("classpath:repository/state/partner_id_not_found.xml")
    public void retryIfPartnerNotFound() {
        flowService.retryFlow(TASK_ID, new RuntimeException(ERROR_MESSAGE));
        verify(taskProcessService).retryTask(eq(1L), eq(120), anyString());
    }

    @Test
    @DatabaseSetup("classpath:repository/state/dropship_create_order_flow.xml")
    public void retryDrophipCreateOrderFlow() {
        flowService.retryFlow(TASK_ID, new RuntimeException(ERROR_MESSAGE));
        verify(taskProcessService).retryTask(eq(1L), eq(60), anyString());
    }

    @Test
    @DatabaseSetup("classpath:repository/state/fulfillment_create_order_flow.xml")
    public void retryNotDropshipCreateFlow() {
        flowService.retryFlow(TASK_ID, new RuntimeException(ERROR_MESSAGE));
        verify(taskProcessService).retryTask(eq(1L), eq(120), anyString());
    }

    @Test
    @DatabaseSetup("classpath:repository/state/dropship_other_flow.xml")
    public void retryDropshipOtherFlow() {
        flowService.retryFlow(TASK_ID, new RuntimeException(ERROR_MESSAGE));
        verify(taskProcessService).retryTask(eq(1L), eq(120), anyString());
    }

    @Test
    @DatabaseSetup("classpath:repository/state/dropship_express_create_order_flow.xml")
    public void retryDrophipExpressCreateOrderFlow() {
        flowService.retryFlow(TASK_ID, new RuntimeException(ERROR_MESSAGE));
        verify(taskProcessService).retryTask(eq(1L), eq(1), anyString());
    }

    @Test
    @DatabaseSetup("classpath:repository/state/dropship_some_tags_create_order_flow.xml")
    public void retryDrophipExpressWithSomeTagsCreateOrderFlow() {
        flowService.retryFlow(TASK_ID, new RuntimeException(ERROR_MESSAGE));
        verify(taskProcessService).retryTask(eq(1L), eq(1), anyString());
    }
}
