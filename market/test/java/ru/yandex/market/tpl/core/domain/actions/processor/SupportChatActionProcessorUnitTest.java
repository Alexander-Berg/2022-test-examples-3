package ru.yandex.market.tpl.core.domain.actions.processor;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.tpl.api.model.specialrequest.SpecialRequestType;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequestPoint;
import ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequestRepository;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequest;
import ru.yandex.market.tpl.core.task.flow.Context;
import ru.yandex.market.tpl.core.task.flow.EmptyPayload;
import ru.yandex.market.tpl.core.task.flow.LogisticRequestLinksHolder;
import ru.yandex.market.tpl.core.task.persistence.LogisticRequestLink;
import ru.yandex.market.tpl.core.task.persistence.LogisticRequestLinkRepository;
import ru.yandex.market.tpl.core.task.persistence.LogisticRequestLinkStatus;
import ru.yandex.market.tpl.core.task.projection.FlowTask;
import ru.yandex.market.tpl.core.task.projection.TaskAction;
import ru.yandex.market.tpl.core.task.projection.TaskFlowType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @author sekulebyakin
 */
@ExtendWith(MockitoExtension.class)
public class SupportChatActionProcessorUnitTest {

    private static final String TEST_TRACKER_URL = "https://st-url.ru";
    private static final String TEST_LMS_URL = "https://lms-url.ru/";

    @InjectMocks
    private SupportChatActionProcessor processor;

    @Mock
    private ConfigurationProviderAdapter configuration;
    @Mock
    private PickupPointRepository pickupPointRepository;
    @Mock
    private LogisticRequestLinkRepository logisticRequestLinkRepository;
    @Mock
    private LogisticRequestRepository logisticRequestRepository;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(processor, "trackerUrl", TEST_TRACKER_URL);
        ReflectionTestUtils.setField(processor, "lmsPickupPointUrl", TEST_LMS_URL);
    }

    @Test
    void unknownFlowMakePayloadTest() {
        // mocks
        var context = mock(Context.class);
        var flowTask = mock(FlowTask.class);
        var taskAction = mock(TaskAction.class);
        when(context.getTask()).thenReturn(flowTask);
        when(flowTask.getFlowType()).thenReturn(TaskFlowType.TEST_FLOW);

        // method call
        var payload = (SupportChatPayload) processor.makePayload(context, taskAction);

        // result
        assertThat(payload.getInitialMessage()).isEqualTo("");
    }

    @Test
    void lockerInventoryMakePayloadTest() {
        // mocks
        var context = mock(Context.class);
        var linkHolder = mock(LogisticRequestLinksHolder.class);
        var flowTask = mock(FlowTask.class);
        var taskAction = mock(TaskAction.class);
        var logisticRequestLink = mock(LogisticRequestLink.class);
        when(context.getTask()).thenReturn(flowTask);
        when(flowTask.getFlowType()).thenReturn(TaskFlowType.LOCKER_INVENTORY);

        var specialRequest = mock(SpecialRequest.class);
        var pointTo = mock(LogisticRequestPoint.class);
        var pickupPoint = mock(PickupPoint.class);
        when(specialRequest.getPointTo()).thenReturn(pointTo);
        when(specialRequest.getSpecialRequestType()).thenReturn(SpecialRequestType.LOCKER_INVENTORY);
        when(pointTo.getPickupPointId()).thenReturn(55L);
        doReturn(pickupPoint).when(pickupPointRepository).findByIdOrThrow(eq(55L));

        when(specialRequest.getRequestSource()).thenReturn("MARKETTPL-8346");
        when(pickupPoint.getCode()).thenReturn("ohoho");
        when(pickupPoint.getAddress()).thenReturn("address 1");
        when(pickupPoint.getLogisticPointId()).thenReturn(1000599L);

        when(logisticRequestLinkRepository.findLinksForTask(any())).thenReturn(List.of(logisticRequestLink));
        when(logisticRequestLink.getStatus()).thenReturn(LogisticRequestLinkStatus.ACTIVE);
        when(logisticRequestRepository.findById(any())).thenReturn(Optional.of(specialRequest));

        // method call
        var payload = (SupportChatPayload) processor.makePayload(context, taskAction);

        // result
        assertThat(payload.getInitialMessage()).isEqualTo(
                "Внешний ID постамата: <a " +
                        "href='" + TEST_LMS_URL + "1000599'>ohoho</a>" +
                        "<br><br>Адрес постамата: <a href='" + TEST_LMS_URL +
                        "1000599'>address 1</a>" +
                        "<br><br>Инвентаризация: <a href='" + TEST_TRACKER_URL
                        + "/MARKETTPL-8346'>MARKETTPL-8346</a>"
        );
    }

    @Test
    void processActionTest() {
        // mocks
        var payload = new EmptyPayload();
        var context = mock(Context.class);

        // method call
        var processedPayload = processor.processAction(context, payload);

        // result
        assertThat(processedPayload).isSameAs(payload);
        verifyNoInteractions(context);
    }

}
