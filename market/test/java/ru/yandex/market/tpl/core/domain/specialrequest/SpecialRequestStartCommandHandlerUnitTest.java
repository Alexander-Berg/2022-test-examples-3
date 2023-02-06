package ru.yandex.market.tpl.core.domain.specialrequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequestCommand;
import ru.yandex.market.tpl.core.domain.specialrequest.events.SpecialRequestStatusChangedEvent;
import ru.yandex.market.tpl.core.exception.TplInvalidTransitionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author sekulebyakin
 */
public class SpecialRequestStartCommandHandlerUnitTest extends BaseSpecialRequestCommandHandlerUnitTest {

    private final SpecialRequestStartCommandHandler handler = new SpecialRequestStartCommandHandler();
    private final SpecialRequestCommand.Start command = new SpecialRequestCommand.Start(1L);

    private SpecialRequest specialRequest;

    @BeforeEach
    void setup() {
        specialRequest = createTestSpecialRequest();
    }

    @Test
    void startFromCreatedStatusTest() {
        var events = handler.execute(specialRequest, command);
        assertThat(specialRequest.getStatus()).isEqualTo(SpecialRequestStatus.IN_PROGRESS);
        assertThat(events).hasSize(1);
        var domainEvent = events.iterator().next();
        assertThat(domainEvent).isInstanceOf(SpecialRequestStatusChangedEvent.class);
        var event = (SpecialRequestStatusChangedEvent) domainEvent;
        assertThat(event.getAggregate()).isSameAs(specialRequest);
        assertThat(event.getTransition().getTo()).isEqualTo(SpecialRequestStatus.IN_PROGRESS);

        // idempotency
        assertThat(handler.execute(specialRequest, command)).isEmpty();
    }

    @Test
    void startWithTerminalStatusTest() {
        specialRequest.cancel(LogisticRequestCommand.Cancel.builder().logisticRequestId(1L).build());

        assertThatThrownBy(() -> handler.execute(specialRequest, command))
                .isInstanceOf(TplInvalidTransitionException.class);
    }

}
