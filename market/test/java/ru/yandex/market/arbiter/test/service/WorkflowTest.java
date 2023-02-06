package ru.yandex.market.arbiter.test.service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.persistence.LockTimeoutException;

import com.google.common.collect.Sets;
import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.arbiter.api.server.dto.ArbiterConversationDto;
import ru.yandex.market.arbiter.api.server.dto.ArbiterConversationSummaryDto;
import ru.yandex.market.arbiter.api.server.dto.AuditDto;
import ru.yandex.market.arbiter.api.server.dto.ConversationSide;
import ru.yandex.market.arbiter.api.server.dto.ConversationStatus;
import ru.yandex.market.arbiter.api.server.dto.CreateConversationRequestDto;
import ru.yandex.market.arbiter.api.server.dto.EventSource;
import ru.yandex.market.arbiter.api.server.dto.EventType;
import ru.yandex.market.arbiter.api.server.dto.MessageDto;
import ru.yandex.market.arbiter.api.server.dto.RefundDto;
import ru.yandex.market.arbiter.api.server.dto.RequestDocumentsDto;
import ru.yandex.market.arbiter.test.BaseUnitTest;
import ru.yandex.market.arbiter.test.TestDataService;
import ru.yandex.market.arbiter.test.util.RandomDataGenerator;
import ru.yandex.market.arbiter.test.util.RandomUtil;
import ru.yandex.market.arbiter.test.util.TestClock;
import ru.yandex.market.arbiter.test.util.TestUtil;
import ru.yandex.market.arbiter.workflow.ArbiterException;
import ru.yandex.market.arbiter.workflow.Workflow;

/**
 * @author moskovkin@yandex-team.ru
 * @since 24.05.2020
 */
@Log4j2
public class WorkflowTest extends BaseUnitTest {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(WorkflowTest.class).build();

    @Autowired
    private TestClock clock;

    @Autowired
    private Workflow workflow;

    @Test
    public void testAudit() {
        TestDataService.TestData testData = testDataService.saveTestData();
        Long arbiterUid = RANDOM.nextLong();

        workflow.arbiterInProgress(arbiterUid, testData.someConversationId());
        workflow.arbiterRequestDocuments(arbiterUid, testData.someConversationId(), new RequestDocumentsDto()
                .recipient(ConversationSide.MERCHANT)
                .text("Document request")
                .waitAnswerUntil(OffsetDateTime.ofInstant(
                        clock.instant().plus(1, ChronoUnit.DAYS), clock.getZone()
                ))
        );
        workflow.serviceMessageAdd(testData.someConversationId(), new MessageDto()
                .text("Document")
                .sender(ConversationSide.MERCHANT)
                .recipient(ConversationSide.ARBITER)
        );
        workflow.arbiterVerdictDecline(arbiterUid, testData.someConversationId());
        workflow.notifyVerdictAndClose();

        ArbiterConversationDto conversation = workflow.getConversation(testData.someConversationId()).orElseThrow();
        Assertions.assertThat(conversation.getAudit())
                .isSortedAccordingTo(Comparator.comparing(AuditDto::getCreationTime));
        Assertions.assertThat(conversation.getAudit()).usingRecursiveFieldByFieldElementComparator()
                .usingElementComparatorIgnoringFields("id", "creationTime")
                .containsSubsequence(
                        new AuditDto()
                                .eventType(EventType.CONVERSATION_STATUS_CHANGE)
                                .eventSource(EventSource.ARBITER)
                                .arbiterUid(arbiterUid)
                                .newConversationStatus(ConversationStatus.IN_PROGRESS),
                        new AuditDto()
                                .eventType(EventType.CONVERSATION_STATUS_CHANGE)
                                .eventSource(EventSource.ARBITER)
                                .arbiterUid(arbiterUid)
                                .newConversationStatus(ConversationStatus.WAITING_DOCUMENTS),
                        new AuditDto()
                                .eventType(EventType.CONVERSATION_STATUS_CHANGE)
                                .eventSource(EventSource.SERVICE)
                                .newConversationStatus(ConversationStatus.IN_PROGRESS),
                        new AuditDto()
                                .eventType(EventType.CONVERSATION_STATUS_CHANGE)
                                .eventSource(EventSource.ARBITER)
                                .arbiterUid(arbiterUid)
                                .newConversationStatus(ConversationStatus.VERDICT),
                        new AuditDto()
                                .eventType(EventType.CONVERSATION_STATUS_CHANGE)
                                .eventSource(EventSource.SYSTEM)
                                .newConversationStatus(ConversationStatus.CLOSED)
                );
    }

    @Test
    public void testSwitchExpiredConversationsToInProgress() {
        TestDataService.TestData testData = testDataService.saveTestData();
        Long arbiterUid = RANDOM.nextLong();

        OffsetDateTime oneWeek =
                OffsetDateTime.ofInstant(clock.instant().plus(7, ChronoUnit.DAYS), clock.getZone());

        OffsetDateTime twoWeek =
                OffsetDateTime.ofInstant(clock.instant().plus(14, ChronoUnit.DAYS), clock.getZone());

        Set<Long> longWaitingConversationIds = testData.getConversations().stream()
                .map(ArbiterConversationDto::getId)
                .filter(id -> id % 2 == 0)
                .peek(id -> workflow.arbiterRequestDocuments(arbiterUid, id,
                        RANDOM.nextObject(RequestDocumentsDto.class)
                                .waitAnswerUntil(twoWeek)
                                .recipient(ConversationSide.USER)
                ))
                .collect(Collectors.toUnmodifiableSet());
        Set<Long> shortWaitingConversationIds = testData.getConversations().stream()
                .map(ArbiterConversationDto::getId)
                .filter(id -> id % 3 == 0)
                .peek(id -> workflow.arbiterRequestDocuments(arbiterUid, id,
                        RANDOM.nextObject(RequestDocumentsDto.class)
                                .waitAnswerUntil(oneWeek)
                                .recipient(ConversationSide.MERCHANT)
                ))
                .collect(Collectors.toUnmodifiableSet());
        Set<Long> noWaitingConversationIds = testData.getConversations().stream()
                .map(ArbiterConversationDto::getId)
                .filter(id -> (id % 2) != 0 && (id % 3) != 0)
                .collect(Collectors.toUnmodifiableSet());
        Set<Long> shortWaitingNoLongWaitngConversationIds =
                Sets.difference(shortWaitingConversationIds, longWaitingConversationIds);

        clock.setTime(clock.instant().plus(9, ChronoUnit.DAYS));
        workflow.switchExpiredWaitingToInProgress();

        // Just in case
        Assertions.assertThat(longWaitingConversationIds).isNotEmpty();
        Assertions.assertThat(shortWaitingConversationIds).isNotEmpty();
        Assertions.assertThat(noWaitingConversationIds).isNotEmpty();
        Assertions.assertThat(shortWaitingNoLongWaitngConversationIds).isNotEmpty();

        Set<Long> foundInProgress = workflow.arbiterConversationSearch(
                null, null,
                List.of(ConversationStatus.IN_PROGRESS),
                null, null, null, null, null, null, null
        ).getItems().stream().map(ArbiterConversationSummaryDto::getId).collect(Collectors.toUnmodifiableSet());

        Set<Long> foundWaitingDocuments = workflow.arbiterConversationSearch(
                null, null,
                List.of(ConversationStatus.WAITING_DOCUMENTS),
                null, null, null, null, null, null, null
        ).getItems().stream().map(ArbiterConversationSummaryDto::getId).collect(Collectors.toUnmodifiableSet());

        Assertions.assertThat(foundWaitingDocuments)
                .containsExactlyInAnyOrderElementsOf(longWaitingConversationIds);
        Assertions.assertThat(foundInProgress)
                .containsExactlyInAnyOrderElementsOf(
                        Sets.union(noWaitingConversationIds, shortWaitingNoLongWaitngConversationIds)
                );
    }

    @Test
    @SneakyThrows
    public void testParallelAddConversation() {
        TestUtil.ParallelCallResults<Long> results = TestUtil.doParallelCalls(
                10, () -> workflow.addConversation(RANDOM.nextObject(CreateConversationRequestDto.class))
        );

        Assertions.assertThat(results.getErrors()).isEmpty();
    }

    @Test
    public void testAllConversationStatusParallelCallsInOneTime() {
        TestDataService.TestData testData = testDataService.saveTestData();
        List<Callable<Boolean>> allCalls = getAllConversationWorkflowMethodCalls(testData);
        TestUtil.ParallelCallResults<Boolean> results = TestUtil.doParallelCalls(1, allCalls);

        Assertions.assertThat(results.getErrors())
                .isNotEmpty()
                .hasOnlyElementsOfTypes(
                        // Some known exceptions allowed
                        ArbiterException.class,

                        // Even 3 retries may not be enough if we make ton of parallel cals
                        LockTimeoutException.class
                );
        Assertions.assertThat(results.getResults())
                .isNotEmpty();
    }

    @Test
    public void testTwoConversationStatusParallelCallsWorksAsExpected() {
        for (int i = 0; i < 10; i++) {
            doTwoParallelCalls();
        }
    }

    @Test
    public void testVerdictNotifyCloseAllVerdictConversations() {
        TestDataService.TestData testData = testDataService.saveTestData();
        Long arbiterUid = RANDOM.nextLong();

        Set<Long> verdictConversationIds = testData.getConversations().stream()
                .map(ArbiterConversationDto::getId)
                .filter(id -> id % 2 == 0)
                .peek(id -> workflow.arbiterVerdictDecline(arbiterUid, id))
                .collect(Collectors.toUnmodifiableSet());

        Set<Long> foundVerdict = workflow.arbiterConversationSearch(
                null, null,
                List.of(ConversationStatus.VERDICT),
                null, null, null, null, null, null, null
        ).getItems().stream().map(ArbiterConversationSummaryDto::getId).collect(Collectors.toUnmodifiableSet());
        Assertions.assertThat(foundVerdict)
                .containsExactlyInAnyOrderElementsOf(verdictConversationIds);

        Set<Long> noVerdictConversationIds = testData.getConversations().stream()
                .map(ArbiterConversationDto::getId)
                .filter(id -> !verdictConversationIds.contains(id))
                .collect(Collectors.toUnmodifiableSet());
        Assertions.assertThat(noVerdictConversationIds).isNotEmpty();

        workflow.notifyVerdictAndClose();

        Set<Long> foundClosed = workflow.arbiterConversationSearch(
                null, null,
                List.of(ConversationStatus.CLOSED),
                null, null, null, null, null, null, null
        ).getItems().stream().map(ArbiterConversationSummaryDto::getId).collect(Collectors.toUnmodifiableSet());

        Assertions.assertThat(foundClosed)
                .containsExactlyInAnyOrderElementsOf(verdictConversationIds);
    }

    private void doTwoParallelCalls() {
        TestDataService.TestData testData = testDataService.saveTestData();
        List<Callable<Boolean>> allCalls = getAllConversationWorkflowMethodCalls(testData);

        List<Callable<Boolean>> someCalls = List.of(
                RandomUtil.randomItem(RANDOM, allCalls),
                RandomUtil.randomItem(RANDOM, allCalls)
        );

        TestUtil.ParallelCallResults<Boolean> results = TestUtil.doParallelCalls(1, someCalls);
        System.out.println("Parallel success count = " + results.getResults().size());

        System.out.println("Parallel calls errors: ");
        for (Throwable error : results.getErrors()) {
            error.printStackTrace();
        }

        Assertions.assertThat(results.getErrors())
                .hasOnlyElementsOfTypes(ArbiterException.class);

        Assertions.assertThat(results.getErrors().stream().map(Throwable::getMessage))
                .allMatch(s -> s.contains("forbidden"));

        testDataService.cleanDatabase();
    }

    private List<Callable<Boolean>> getAllConversationWorkflowMethodCalls(TestDataService.TestData testData) {
        Long arbiterUid = RANDOM.nextLong();
        RefundDto refundDto = RANDOM.nextObject(RefundDto.class);
        RequestDocumentsDto request = RANDOM.nextObject(RequestDocumentsDto.class);
        MessageDto arbiterMessage = RANDOM.nextObject(MessageDto.class)
                .recipient(ConversationSide.USER)
                .sender(ConversationSide.ARBITER);
        MessageDto userMessage = RANDOM.nextObject(MessageDto.class)
                .recipient(ConversationSide.ARBITER)
                .sender(ConversationSide.USER);

        return List.of(
                // VerdictWorkflow
                () -> {
                    workflow.arbiterVerdictDecline(arbiterUid, testData.someConversationId());
                    return true;
                },
                () -> {
                    workflow.arbiterVerdictAgree(arbiterUid, testData.someConversationId());
                    return true;
                },
                () -> {
                    workflow.arbiterVerdictRefund(arbiterUid, testData.someConversationId(), refundDto);
                    return true;
                },
                () -> {
                    workflow.serviceVerdictAgree(testData.someConversationId());
                    return true;
                },

                // ArbiterActionsWorkflow
                () -> {
                    workflow.arbiterRequestDocuments(arbiterUid, testData.someConversationId(), request);
                    return true;
                },
                () -> {
                    workflow.arbiterInProgress(arbiterUid, testData.someConversationId());
                    return true;
                },
                () -> {
                    workflow.systemInProgress(testData.someConversationId());
                    return true;
                },
                () -> {
                    workflow.systemVerdictExecuted(testData.someConversationId());
                    return true;
                },

                // ConfirmWorkflow
                () -> {
                    workflow.arbiterVerdictConfirm(arbiterUid, testData.someConversationId());
                    return true;
                },
                () -> {
                    workflow.serviceVerdictExecuted(testData.someConversationId());
                    return true;
                },

                // ConfirmWorkflow
                () -> {
                    workflow.arbiterMessageAdd(arbiterUid, testData.someConversationId(), arbiterMessage);
                    return true;
                },
                () -> {
                    workflow.serviceMessageAdd(testData.someConversationId(), userMessage);
                    return true;
                }
        );
    }
}
