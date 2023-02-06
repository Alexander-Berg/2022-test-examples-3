package ru.yandex.market.tpl.core.domain.specialrequest.handlers;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequest;
import ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequestCommand;
import ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequestCommandService;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequest;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestCommand;
import ru.yandex.market.tpl.core.task.flow.Context;
import ru.yandex.market.tpl.core.task.flow.EmptyPayload;
import ru.yandex.market.tpl.core.task.flow.LogisticRequestLinksHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author sekulebyakin
 */
@ExtendWith(MockitoExtension.class)
public class FinishSpecialRequestsHandlerUnitTest {

    @InjectMocks
    private FinishSpecialRequestsHandler handler;

    @Mock
    private LogisticRequestCommandService commandService;

    private final Set<Long> specialRequestIds = Set.of(51L, 52L);

    @Test
    void doAfterActionTest() {
        var context = mockContext();

        handler.doAfterAction(context, new EmptyPayload());

        var commandCaptor = ArgumentCaptor.forClass(LogisticRequestCommand.class);
        verify(commandService, times(2)).execute(commandCaptor.capture());

        var commands = commandCaptor.getAllValues();
        assertThat(commands).hasSize(2);
        var specialRequestIds = commands.stream()
                .peek(c -> assertThat(c).isInstanceOf(SpecialRequestCommand.Finish.class))
                .map(LogisticRequestCommand::getLogisticRequestId)
                .collect(Collectors.toSet());
        assertThat(specialRequestIds).containsAll(specialRequestIds);
    }

    private Context mockContext() {
        var logisticRequests = specialRequestIds.stream()
                .map(id -> {
                    var specialRequest = mock(SpecialRequest.class);
                    when(specialRequest.getId()).thenReturn(id);
                    return (LogisticRequest) specialRequest;
                }).collect(Collectors.toList());
        var context = mock(Context.class);
        var linkHolder = mock(LogisticRequestLinksHolder.class);
        when(linkHolder.getActiveLinkedLogisticRequests()).thenReturn(logisticRequests);
        when(context.getLogisticRequestLinksHolder()).thenReturn(linkHolder);
        return context;
    }
}
