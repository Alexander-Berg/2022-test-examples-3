package ru.yandex.market.arbiter.test.service;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.arbiter.api.server.dto.ArbiterConversationDto;
import ru.yandex.market.arbiter.api.server.dto.ConversationStatus;
import ru.yandex.market.arbiter.api.server.dto.RequestDocumentsDto;
import ru.yandex.market.arbiter.test.BaseUnitTest;
import ru.yandex.market.arbiter.test.TestDataService;
import ru.yandex.market.arbiter.test.util.RandomDataGenerator;
import ru.yandex.market.arbiter.workflow.Workflow;
import ru.yandex.market.arbiter.workflow.impl.ArbiterAllActionsImpl;
import ru.yandex.market.arbiter.workflow.impl.SystemAllActionsImpl;

/**
 * @author moskovkin@yandex-team.ru
 * @since 05.06.2020
 */
public class SystemAllActionsImplTest extends BaseUnitTest {
    private static final EnhancedRandom RANDOM =
            RandomDataGenerator.dataRandom(SystemAllActionsImplTest.class).build();

    @Autowired
    private Workflow workflow;

    @Autowired
    private ArbiterAllActionsImpl arbiterAllActions;

    @Autowired
    private SystemAllActionsImpl systemAllActions;

    @Test
    void testSystemInProgressResetWaitings() {
        TestDataService.TestData testData = testDataService.saveTestData();
        RequestDocumentsDto requestDocuments = RANDOM.nextObject(RequestDocumentsDto.class);
        Long arbiterUid = RANDOM.nextLong();

        arbiterAllActions.arbiterRequestDocuments(
                arbiterUid, testData.someConversation().getId(), requestDocuments
        );

        ArbiterConversationDto conversationBefore = workflow.getConversation(testData.someConversation().getId())
                .orElseThrow();

        systemAllActions.systemInProgress(testData.someConversation().getId());

        ArbiterConversationDto conversationAfter = workflow.getConversation(testData.someConversation().getId())
                .orElseThrow();

        Assertions.assertThat(conversationBefore.getWaitings())
                .isNotEmpty();
        Assertions.assertThat(conversationAfter.getWaitings())
                .isEmpty();
    }

    @Test
    void testSystemInProgressResetVerdict() {
        TestDataService.TestData testData = testDataService.saveTestData();
        Long arbiterUid = RANDOM.nextLong();

        arbiterAllActions.arbiterVerdictAgree(arbiterUid, testData.someConversation().getId());

        ArbiterConversationDto conversationBefore = workflow.getConversation(testData.someConversation().getId())
                .orElseThrow();

        systemAllActions.systemInProgress(testData.someConversation().getId());

        ArbiterConversationDto conversationAfter = workflow.getConversation(testData.someConversation().getId())
                .orElseThrow();

        Assertions.assertThat(conversationBefore.getVerdict())
                .isNotNull();
        Assertions.assertThat(conversationAfter.getVerdict())
                .isNull();
    }

    @Test
    void testSystemInProgressSwitchStatus() {
        TestDataService.TestData testData = testDataService.saveTestData();
        Long arbiterUid = RANDOM.nextLong();

        arbiterAllActions.arbiterVerdictAgree(arbiterUid, testData.someConversation().getId());

        ArbiterConversationDto conversationBefore = workflow.getConversation(testData.someConversation().getId())
                .orElseThrow();

        systemAllActions.systemInProgress(testData.someConversation().getId());

        ArbiterConversationDto conversationAfter = workflow.getConversation(testData.someConversation().getId())
                .orElseThrow();

        Assertions.assertThat(conversationBefore.getStatus())
                .isNotEqualTo(ConversationStatus.IN_PROGRESS);
        Assertions.assertThat(conversationAfter.getWaitings())
                .isNotEqualTo(ConversationStatus.IN_PROGRESS);
    }

    @Test
    public void testSystemVerdictExecute() {
        TestDataService.TestData testData = testDataService.saveTestData();
        Long arbiterUid = RANDOM.nextLong();

        arbiterAllActions.arbiterVerdictDecline(arbiterUid, testData.someConversationId());

        ArbiterConversationDto conversationBefore = workflow.getConversation(testData.someConversationId())
                .orElseThrow();

        systemAllActions.systemVerdictExecuted(testData.someConversationId());

        ArbiterConversationDto conversationAfter = workflow.getConversation(testData.someConversationId())
                .orElseThrow();

        Assertions.assertThat(conversationBefore.getStatus())
                .isNotEqualTo(ConversationStatus.CLOSED);

        Assertions.assertThat(conversationAfter.getStatus())
                .isEqualTo(ConversationStatus.CLOSED);
    }
}
