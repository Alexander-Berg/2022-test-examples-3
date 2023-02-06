package ru.yandex.market.tpl.core.task.persistance;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequest;
import ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequestRepository;
import ru.yandex.market.tpl.core.task.flow.LogisticRequestLinksHolder;
import ru.yandex.market.tpl.core.task.persistence.LogisticRequestLink;
import ru.yandex.market.tpl.core.task.persistence.LogisticRequestLinkStatus;
import ru.yandex.market.tpl.core.task.persistence.LogisticRequestLinkStatusChangeReason;
import ru.yandex.market.tpl.core.task.service.LogisticRequestLinkService;
import ru.yandex.market.tpl.core.test.ClockUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @author sekulebyakin
 */
@ExtendWith(MockitoExtension.class)
public class LogisticRequestLinksHolderUnitTest {

    @Mock
    private LogisticRequestLinkService logisticRequestLinkService;
    @Mock
    private LogisticRequestRepository logisticRequestRepository;

    @Mock
    private Clock clock;

    private final long taskId = 54321L;
    private final Set<Long> logisticRequestIds = new HashSet<>();
    private final List<LogisticRequestLink> linkMocks = new ArrayList<>();
    private final List<LogisticRequest> logisticRequestMocks = new ArrayList<>();

    private LogisticRequestLinksHolder linksHolder;

    @BeforeEach
    void setup() {
        // test data
        ClockUtil.initFixed(clock);
        linksHolder = new LogisticRequestLinksHolder(taskId, logisticRequestLinkService,
                logisticRequestRepository, clock, 1L);

        linkMocks.clear();
        logisticRequestMocks.clear();
        logisticRequestIds.clear();
    }

    @Test
    void getLogisticRequestsTest() {
        mockLink(11L, LogisticRequestLinkStatus.ACTIVE);
        mockLink(22L, LogisticRequestLinkStatus.ACTIVE);

        var ids = linksHolder.getLinkedLogisticRequestIds();
        assertThat(ids).hasSize(2);
        assertThat(ids).containsAll(logisticRequestIds);

        var logisticRequests = linksHolder.getLinkedLogisticRequests();
        assertThat(logisticRequests).hasSize(logisticRequestIds.size());
        assertThatContainAllIds(logisticRequests, Set.of(11L, 22L));
    }

    @Test
    void getLogisticRequestsWithLinkStatusFilterTest() {
        mockLink(11L, LogisticRequestLinkStatus.ACTIVE);
        mockLink(22L, LogisticRequestLinkStatus.ACTIVE);
        mockLink(33L, LogisticRequestLinkStatus.CANCELLED);
        mockLink(44L, LogisticRequestLinkStatus.CANCELLED);

        var cancelledLogisticRequests = linksHolder.getLinkedLogisticRequests(
                Set.of(LogisticRequestLinkStatus.CANCELLED));

        assertThat(cancelledLogisticRequests).hasSize(2);
        assertThatContainAllIds(cancelledLogisticRequests, Set.of(33L, 44L));
    }

    @Test
    void getActiveLogisticRequestsTest() {
        mockLink(11L, LogisticRequestLinkStatus.ACTIVE);
        mockLink(22L, LogisticRequestLinkStatus.ACTIVE);
        mockLink(33L, LogisticRequestLinkStatus.CANCELLED);
        mockLink(44L, LogisticRequestLinkStatus.CANCELLED);

        var activeLogisticRequests = linksHolder.getActiveLinkedLogisticRequests();
        assertThat(activeLogisticRequests).hasSize(2);
        assertThatContainAllIds(activeLogisticRequests, Set.of(11L, 22L));
    }

    @Test
    void getLogisticRequestsWithSingleFetchTest() {
        mockLink(11L, LogisticRequestLinkStatus.ACTIVE);
        mockLink(22L, LogisticRequestLinkStatus.ACTIVE);

        // multiple calls ids
        linksHolder.getLinkedLogisticRequestIds();
        linksHolder.getLinkedLogisticRequestIds();
        linksHolder.getLinkedLogisticRequestIds();

        // no unnecessary repository calls
        verify(logisticRequestLinkService, times(1)).findLinksForTask(eq(taskId));
        verifyNoInteractions(logisticRequestRepository);

        // multiple calls entities
        linksHolder.getLinkedLogisticRequests();
        linksHolder.getLinkedLogisticRequests();
        linksHolder.getLinkedLogisticRequests();

        // no unnecessary repository calls
        verify(logisticRequestLinkService, times(1)).findLinksForTask(eq(taskId));
        verify(logisticRequestRepository, times(1)).findAllById(eq(logisticRequestIds));
    }

    @Test
    void updateStatusTest() {
        var link1 = mockLink(11L, LogisticRequestLinkStatus.ACTIVE);
        var link2 = mockLink(22L, LogisticRequestLinkStatus.ACTIVE);
        var link3 = mockLink(33L, LogisticRequestLinkStatus.ACTIVE);
        var link4 = mockLink(44L, LogisticRequestLinkStatus.ACTIVE);

        linksHolder.updateLinkStatus(List.of(11L, 22L), LogisticRequestLinkStatus.NOT_ACTIVE);
        linksHolder.updateLinkStatus(List.of(33L), LogisticRequestLinkStatus.CANCELLED,
                LogisticRequestLinkStatusChangeReason.LOGISTIC_REQUEST_CANCELLED);

        var now = Instant.now(clock);
        verify(link1).updateStatus(eq(LogisticRequestLinkStatus.NOT_ACTIVE), isNull(), eq(now), eq(1L));
        verify(link2).updateStatus(eq(LogisticRequestLinkStatus.NOT_ACTIVE), isNull(), eq(now), eq(1L));
        verify(link3).updateStatus(
                eq(LogisticRequestLinkStatus.CANCELLED),
                eq(LogisticRequestLinkStatusChangeReason.LOGISTIC_REQUEST_CANCELLED),
                eq(now),
                eq(1L)
        );
        verify(link4, never()).updateStatus(any(), any(), any(), anyLong());
    }

    private void assertThatContainAllIds(Collection<LogisticRequest> logisticRequests, Set<Long> requiredIds) {
        var ids = logisticRequests.stream()
                .map(LogisticRequest::getId)
                .collect(Collectors.toList());
        assertThat(ids).hasSize(requiredIds.size());
        assertThat(ids).containsAll(requiredIds);
    }

    private LogisticRequestLink mockLink(long logisticRequestId, LogisticRequestLinkStatus status) {
        logisticRequestIds.add(logisticRequestId);

        var link = mock(LogisticRequestLink.class);
        when(link.getLogisticRequestId()).thenReturn(logisticRequestId);
        lenient().doReturn(status).when(link).getStatus();
        linkMocks.add(link);

        var lr = mock(LogisticRequest.class);
        lenient().doReturn(logisticRequestId).when(lr).getId();
        logisticRequestMocks.add(lr);

        lenient().doReturn(linkMocks).when(logisticRequestLinkService).findLinksForTask(eq(taskId));
        lenient().doReturn(logisticRequestMocks).when(logisticRequestRepository)
                .findAllById(eq(logisticRequestIds));

        return link;
    }

}
