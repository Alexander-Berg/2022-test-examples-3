package ru.yandex.market.tpl.core.domain.specialrequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequestCommand;
import ru.yandex.market.tpl.core.domain.specialrequest.events.SpecialRequestFinishedEvent;
import ru.yandex.market.tpl.core.domain.specialrequest.events.SpecialRequestStatusChangedEvent;
import ru.yandex.market.tpl.core.exception.TplInvalidTransitionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author sekulebyakin
 */
public class SpecialRequestFinishCommandHandlerUnitTest extends BaseSpecialRequestCommandHandlerUnitTest {

    private final SpecialRequestFinishCommandHandler handler = new SpecialRequestFinishCommandHandler();
    private final SpecialRequestCommand.Finish command = new SpecialRequestCommand.Finish(1L);

    private SpecialRequest specialRequest;

    @BeforeEach
    void setup() {
        specialRequest = createTestSpecialRequest();
        specialRequest.start();
    }

    @Test
    void finishTest() {
        var events = handler.execute(specialRequest, command);
        assertThat(specialRequest.getStatus()).isEqualTo(SpecialRequestStatus.FINISHED);
        assertThat(events).hasSize(2);
        var iterator = events.iterator();
        var domainEvent1 = iterator.next();
        var domainEvent2 = iterator.next();

        assertThat(domainEvent1).isInstanceOf(SpecialRequestStatusChangedEvent.class);
        assertThat(domainEvent2).isInstanceOf(SpecialRequestFinishedEvent.class);
        var statusChangedEvent = (SpecialRequestStatusChangedEvent) domainEvent1;
        var finishedEvent = (SpecialRequestFinishedEvent) domainEvent2;

        assertThat(statusChangedEvent.getAggregate()).isSameAs(specialRequest);
        assertThat(statusChangedEvent.getTransition().getTo()).isEqualTo(SpecialRequestStatus.FINISHED);
        assertThat(finishedEvent.getAggregate()).isSameAs(specialRequest);

        // idempotency
        assertThat(handler.execute(specialRequest, command)).isEmpty();
    }

    @Test
    void finishWithTerminalStatusTest() {
        specialRequest.cancel(LogisticRequestCommand.Cancel.builder().logisticRequestId(1L).build());

        assertThatThrownBy(() -> handler.execute(specialRequest, command))
                .isInstanceOf(TplInvalidTransitionException.class);

    }

}
