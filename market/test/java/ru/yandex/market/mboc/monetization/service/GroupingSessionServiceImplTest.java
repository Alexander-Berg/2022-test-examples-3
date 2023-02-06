package ru.yandex.market.mboc.monetization.service;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.mboc.common.db.jooq.generated.monetization.enums.SessionStatus;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.GroupingSession;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.monetization.config.MonetizationJooqConfig;
import ru.yandex.market.mboc.monetization.repository.GroupingSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author danfertev
 * @since 31.10.2019
 */
@ContextConfiguration(classes = {MonetizationJooqConfig.class})
public class GroupingSessionServiceImplTest extends BaseDbTestClass {
    @Autowired
    private GroupingSessionRepository repository;

    private GroupingSessionService service;

    @Before
    public void setUp() {
        service = new GroupingSessionServiceImpl(repository);
    }

    @Test
    public void testStartSession() {
        long sessionId = service.startNewSession().getId();

        GroupingSession session = service.getById(sessionId);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
    }

    @Test
    public void testCompleteSession() {
        long sessionId = service.startNewSession().getId();
        service.finishSession(sessionId, SessionStatus.COMPLETED, "COMPLETED");

        GroupingSession session = service.getById(sessionId);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(session.getStatusMessage()).isEqualTo("COMPLETED");
        assertThat(session.getFinishedAt()).isNotNull();
    }

    @Test
    public void testFailSession() {
        long sessionId = service.startNewSession().getId();
        service.finishSession(sessionId, SessionStatus.FAILED, "FAILED");

        GroupingSession session = service.getById(sessionId);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.FAILED);
        assertThat(session.getStatusMessage()).isEqualTo("FAILED");
        assertThat(session.getFinishedAt()).isNotNull();
    }
}
