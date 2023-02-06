package ru.yandex.market.mboc.monetization.executor;

import java.util.List;
import java.util.Objects;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.mboc.common.db.jooq.generated.monetization.enums.SessionStatus;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.ConfigValidationError;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.GroupingSession;
import ru.yandex.market.mboc.common.infrastructure.util.UnstableInit;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.monetization.GroupingTestUtils;
import ru.yandex.market.mboc.monetization.config.MonetizationJooqConfig;
import ru.yandex.market.mboc.monetization.repository.ConfigParameterRepository;
import ru.yandex.market.mboc.monetization.repository.ConfigValidationErrorRepository;
import ru.yandex.market.mboc.monetization.repository.GroupingConfigRepository;
import ru.yandex.market.mboc.monetization.repository.GroupingSessionRepository;
import ru.yandex.market.mboc.monetization.repository.filters.GroupingSessionFilter;
import ru.yandex.market.mboc.monetization.service.GroupingConfigService;
import ru.yandex.market.mboc.monetization.service.GroupingConfigServiceImpl;
import ru.yandex.market.mboc.monetization.service.GroupingSessionService;
import ru.yandex.market.mboc.monetization.service.GroupingSessionServiceImpl;
import ru.yandex.market.mboc.monetization.service.grouping.YtModelGroupingService;
import ru.yandex.market.mboc.monetization.validation.ConfigValidationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mboc.monetization.executor.ModelGroupingSessionExecutor.SESSION_COMPLETED;
import static ru.yandex.market.mboc.monetization.executor.ModelGroupingSessionExecutor.STALE_IN_PROGRESS_FAILED;

/**
 * @author danfertev
 * @since 25.10.2019
 */
@ContextConfiguration(classes = {MonetizationJooqConfig.class})
public class ModelGroupingSessionExecutorTest extends BaseDbTestClass {
    private ModelGroupingSessionExecutor executor;

    @Autowired
    private GroupingSessionRepository groupingSessionRepository;
    @Autowired
    private GroupingConfigRepository groupingConfigRepository;
    @Autowired
    private ConfigParameterRepository configParameterRepository;
    @Autowired
    private ConfigValidationErrorRepository configValidationErrorRepository;

    private GroupingSessionService groupingSessionService;
    private GroupingConfigService groupingConfigService;

    private ConfigValidationService configValidationServiceMock;
    private YtModelGroupingService ytModelGroupingServiceMock;

    @Before
    public void setUp() {
        groupingSessionService = new GroupingSessionServiceImpl(groupingSessionRepository);
        groupingConfigService = new GroupingConfigServiceImpl(groupingConfigRepository, configParameterRepository,
            configValidationErrorRepository);

        configValidationServiceMock = mock(ConfigValidationService.class);
        ytModelGroupingServiceMock = mock(YtModelGroupingService.class);
        executor = new ModelGroupingSessionExecutor(
            groupingSessionService,
            groupingConfigService,
            configValidationServiceMock,
            UnstableInit.simple(ytModelGroupingServiceMock)
        );
    }

    @Test
    public void testPreviousSessionInProgress() {
        GroupingSession inProgressSession = groupingSessionService.startNewSession();
        executor.execute();

        List<GroupingSession> killedSessions = groupingSessionService.find(
            new GroupingSessionFilter().setStatuses(SessionStatus.FAILED)
        );
        assertFinishedSession(killedSessions, SessionStatus.FAILED, STALE_IN_PROGRESS_FAILED);
        assertThat(killedSessions)
            .extracting(GroupingSession::getId)
            .containsExactlyInAnyOrder(inProgressSession.getId());

        List<GroupingSession> completedSessions = groupingSessionService.find(
            new GroupingSessionFilter().setStatuses(SessionStatus.COMPLETED)
        );
        assertFinishedSession(completedSessions, SessionStatus.COMPLETED, SESSION_COMPLETED);
    }

    @Test
    public void testNoConfigs() {
        executor.execute();

        List<GroupingSession> sessions = groupingSessionService.find(new GroupingSessionFilter());
        assertFinishedSession(sessions, SessionStatus.COMPLETED, SESSION_COMPLETED);
    }

    @Test
    public void testConfigValidationFailed() {
        groupingConfigService.save(GroupingTestUtils.simpleDisplayConfig());
        when(configValidationServiceMock.validateAndLog(anyCollection()))
            .thenReturn(List.of(new ConfigValidationError()));

        executor.execute();

        List<GroupingSession> sessions = groupingSessionService.find(new GroupingSessionFilter());
        assertFinishedSession(sessions, SessionStatus.FAILED, ModelGroupingSessionExecutor.CONFIG_VALIDATION_FAILED);
    }

    private void assertFinishedSession(List<GroupingSession> sessions, SessionStatus status, String statusMessage) {
        assertThat(sessions).hasSize(1);
        assertThat(sessions)
            .extracting(GroupingSession::getStatus)
            .containsExactlyInAnyOrder(status);
        assertThat(sessions)
            .extracting(GroupingSession::getStatusMessage)
            .containsExactlyInAnyOrder(statusMessage);
        assertThat(sessions)
            .extracting(GroupingSession::getFinishedAt)
            .allMatch(Objects::nonNull);
    }
}
