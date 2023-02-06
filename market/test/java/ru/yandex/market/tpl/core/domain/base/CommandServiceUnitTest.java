package ru.yandex.market.tpl.core.domain.base;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import ru.yandex.market.tpl.core.service.FlushManager;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * @author sekulebyakin
 */
@ExtendWith(MockitoExtension.class)
public abstract class CommandServiceUnitTest {

    @Mock
    protected ApplicationEventPublisher eventPublisher;

    @Spy
    @InjectMocks
    protected TplEventPublisher tplEventPublisher;

    @Mock
    protected EntityManager entityManager;

    @Mock
    protected FlushManager flushManager;

    @Mock
    protected IdempotencyManager idempotencyManager;

    @AfterEach
    void clearMocks() {
        Mockito.clearInvocations(eventPublisher, tplEventPublisher, entityManager, flushManager, idempotencyManager);
    }

    protected List<DomainEvent> getPublishedEvents() {
        var eventsCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(eventsCaptor.capture());
        return eventsCaptor.getAllValues();
    }

}
