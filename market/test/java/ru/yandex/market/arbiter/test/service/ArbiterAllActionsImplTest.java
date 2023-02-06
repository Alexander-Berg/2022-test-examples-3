package ru.yandex.market.arbiter.test.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.arbiter.api.server.dto.ArbiterConversationDto;
import ru.yandex.market.arbiter.api.server.dto.ConversationSide;
import ru.yandex.market.arbiter.api.server.dto.ConversationStatus;
import ru.yandex.market.arbiter.api.server.dto.MessageDto;
import ru.yandex.market.arbiter.api.server.dto.RefundDto;
import ru.yandex.market.arbiter.api.server.dto.RequestDocumentsDto;
import ru.yandex.market.arbiter.api.server.dto.VerdictDto;
import ru.yandex.market.arbiter.api.server.dto.VerdictType;
import ru.yandex.market.arbiter.api.server.dto.WaitingDto;
import ru.yandex.market.arbiter.test.BaseUnitTest;
import ru.yandex.market.arbiter.test.TestDataService;
import ru.yandex.market.arbiter.test.util.RandomDataGenerator;
import ru.yandex.market.arbiter.workflow.ArbiterException;
import ru.yandex.market.arbiter.workflow.Workflow;
import ru.yandex.market.arbiter.workflow.impl.ArbiterAllActionsImpl;

/**
 * @author moskovkin@yandex-team.ru
 * @since 05.06.2020
 */
public class ArbiterAllActionsImplTest extends BaseUnitTest {
    private static final EnhancedRandom RANDOM =
            RandomDataGenerator.dataRandom(ArbiterAllActionsImplTest.class).build();

    @Autowired
    private Workflow workflow;

    @Autowired
    private ArbiterAllActionsImpl arbiterAllActions;

    @Test
    public void testArbiterRequestDocumentsAddMessage() {
        TestDataService.TestData testData = testDataService.saveTestData();

        RequestDocumentsDto requestDocuments = RANDOM.nextObject(RequestDocumentsDto.class);
        Long arbiterUid = RANDOM.nextLong();

        arbiterAllActions.arbiterRequestDocuments(arbiterUid, testData.someConversation().getId(), requestDocuments);

        ArbiterConversationDto conversation = workflow.getConversation(testData.someConversation().getId())
                .orElseThrow();

        // Message was added
        MessageDto expectedMessage = new MessageDto()
                .recipient(requestDocuments.getRecipient())
                .text(requestDocuments.getText());

        Assertions.assertThat(conversation.getMessages())
                .usingElementComparatorOnFields("text", "recipient")
                .contains(expectedMessage);
    }

    @Test
    public void testArbiterRequestDocumentsSwitchStatus() {
        TestDataService.TestData testData = testDataService.saveTestData();
        ArbiterConversationDto conversationBefore = testData.someConversation();

        RequestDocumentsDto requestDocuments = RANDOM.nextObject(RequestDocumentsDto.class);
        Long arbiterUid = RANDOM.nextLong();

        arbiterAllActions.arbiterRequestDocuments(
                arbiterUid, testData.someConversation().getId(), requestDocuments
        );

        ArbiterConversationDto conversationAfter = workflow.getConversation(conversationBefore.getId())
                .orElseThrow();
        Assertions.assertThat(conversationBefore.getStatus())
                .isNotEqualTo(ConversationStatus.WAITING_DOCUMENTS);
        Assertions.assertThat(conversationAfter.getStatus())
                .isEqualTo(ConversationStatus.WAITING_DOCUMENTS);
    }

    @Test
    public void testArbiterRequestDocumentsResetVerdict() {
        TestDataService.TestData testData = testDataService.saveTestData();
        RequestDocumentsDto requestDocuments = RANDOM.nextObject(RequestDocumentsDto.class);
        Long arbiterUid = RANDOM.nextLong();

        arbiterAllActions.arbiterVerdictAgree(arbiterUid, testData.someConversation().getId());

        ArbiterConversationDto conversationBefore = workflow.getConversation(testData.someConversation().getId())
                .orElseThrow();

        arbiterAllActions.arbiterRequestDocuments(
                arbiterUid, testData.someConversation().getId(), requestDocuments
        );

        ArbiterConversationDto conversationAfter = workflow.getConversation(testData.someConversation().getId())
                .orElseThrow();

        Assertions.assertThat(conversationBefore.getVerdict())
                .isNotNull();
        Assertions.assertThat(conversationAfter.getVerdict())
                .isNull();
    }

    @Test
    public void testArbiterRequestDocumentsLeaveOnlyLastWaiting() {
        TestDataService.TestData testData = testDataService.saveTestData();
        Long arbiterUid = RANDOM.nextLong();

        List<RequestDocumentsDto> userRequests = RANDOM.objects(RequestDocumentsDto.class, 10)
                .peek(r -> r.setRecipient(ConversationSide.USER))
                .collect(Collectors.toUnmodifiableList());
        RequestDocumentsDto lastUserRequest = userRequests.get(userRequests.size() - 1);

        List<RequestDocumentsDto> merchantRequests = RANDOM.objects(RequestDocumentsDto.class, 11)
                .peek(r -> r.setRecipient(ConversationSide.MERCHANT))
                .collect(Collectors.toUnmodifiableList());
        RequestDocumentsDto lastMerchantRequest = merchantRequests.get(merchantRequests.size() - 1);

        // Send all requests
        Stream.concat(userRequests.stream(), merchantRequests.stream())
                .forEach(r -> arbiterAllActions.arbiterRequestDocuments(
                        arbiterUid, testData.someConversation().getId(), r
                ));

        ArbiterConversationDto conversation = workflow.getConversation(testData.someConversation().getId())
                .orElseThrow();

        // Waitng was added
        Assertions.assertThat(conversation.getWaitings())
                .usingElementComparatorIgnoringFields("creationTime")
                .containsOnly(
                        new WaitingDto()
                                .waitAnswerUntil(lastUserRequest.getWaitAnswerUntil())
                                .sender(lastUserRequest.getRecipient()),
                        new WaitingDto()
                                .waitAnswerUntil(lastMerchantRequest.getWaitAnswerUntil())
                                .sender(lastMerchantRequest.getRecipient())
                );
    }

    @Test
    void testArbiterInProgressResetWaitings() {
        TestDataService.TestData testData = testDataService.saveTestData();
        RequestDocumentsDto requestDocuments = RANDOM.nextObject(RequestDocumentsDto.class);
        Long arbiterUid = RANDOM.nextLong();

        arbiterAllActions.arbiterRequestDocuments(
                arbiterUid, testData.someConversation().getId(), requestDocuments
        );

        ArbiterConversationDto conversationBefore = workflow.getConversation(testData.someConversation().getId())
                .orElseThrow();

        arbiterAllActions.arbiterInProgress(arbiterUid, testData.someConversation().getId());

        ArbiterConversationDto conversationAfter = workflow.getConversation(testData.someConversation().getId())
                .orElseThrow();

        Assertions.assertThat(conversationBefore.getWaitings())
                .isNotEmpty();
        Assertions.assertThat(conversationAfter.getWaitings())
                .isEmpty();
    }

    @Test
    void testArbiterInProgressResetVerdict() {
        TestDataService.TestData testData = testDataService.saveTestData();
        Long arbiterUid = RANDOM.nextLong();

        arbiterAllActions.arbiterVerdictAgree(arbiterUid, testData.someConversation().getId());

        ArbiterConversationDto conversationBefore = workflow.getConversation(testData.someConversation().getId())
                .orElseThrow();

        arbiterAllActions.arbiterInProgress(arbiterUid, testData.someConversation().getId());

        ArbiterConversationDto conversationAfter = workflow.getConversation(testData.someConversation().getId())
                .orElseThrow();

        Assertions.assertThat(conversationBefore.getVerdict())
                .isNotNull();
        Assertions.assertThat(conversationAfter.getVerdict())
                .isNull();
    }

    @Test
    void testArbiterInProgressSwitchStatus() {
        TestDataService.TestData testData = testDataService.saveTestData();
        Long arbiterUid = RANDOM.nextLong();

        arbiterAllActions.arbiterVerdictAgree(arbiterUid, testData.someConversation().getId());

        ArbiterConversationDto conversationBefore = workflow.getConversation(testData.someConversation().getId())
                .orElseThrow();

        arbiterAllActions.arbiterInProgress(arbiterUid, testData.someConversation().getId());

        ArbiterConversationDto conversationAfter = workflow.getConversation(testData.someConversation().getId())
                .orElseThrow();

        Assertions.assertThat(conversationBefore.getStatus())
                .isNotEqualTo(ConversationStatus.IN_PROGRESS);
        Assertions.assertThat(conversationAfter.getWaitings())
                .isNotEqualTo(ConversationStatus.IN_PROGRESS);
    }

    @Test
    public void testArbiterMessageAddStoreMessage() {
        TestDataService.TestData testData = testDataService.saveTestData();
        MessageDto message = RANDOM.nextObject(MessageDto.class)
                .recipient(ConversationSide.USER)
                .sender(ConversationSide.USER); //Should be ignored
        Long arbiterUid = RANDOM.nextLong();

        ArbiterConversationDto convBefore = workflow.getConversation(testData.someConversationId()).orElseThrow();
        arbiterAllActions.arbiterMessageAdd(arbiterUid, testData.someConversationId(), message);
        ArbiterConversationDto convAfter = workflow.getConversation(testData.someConversationId()).orElseThrow();

        Assertions.assertThat(convBefore.getMessages())
                .usingElementComparatorOnFields("text", "recipient")
                .doesNotContain(message);

        Assertions.assertThat(convAfter.getMessages())
                .usingElementComparatorOnFields("text", "recipient", "sender")
                .contains(new MessageDto()
                        .text(message.getText())
                        .recipient(message.getRecipient())
                        .sender(ConversationSide.ARBITER)
                );

        Assertions.assertThat(convBefore.getStatus())
                .isEqualTo(convAfter.getStatus());
    }

    @Test
    public void testArbiterMessageAddThrowsOnIncorrectRecipient() {
        TestDataService.TestData testData = testDataService.saveTestData();
        MessageDto message = RANDOM.nextObject(MessageDto.class)
                .recipient(ConversationSide.ARBITER);
        Long arbiterUid = RANDOM.nextLong();

        Assertions.assertThatThrownBy(() ->
                arbiterAllActions.arbiterMessageAdd(arbiterUid, testData.someConversationId(), message)
        )
        .isInstanceOf(ArbiterException.class);
    }

    @Test
    public void testArbiterVerdictConfirm() {
        TestDataService.TestData testData = testDataService.saveTestData();

        RefundDto refundDto = RANDOM.nextObject(RefundDto.class);
        Long arbiterUid = RANDOM.nextLong();

        arbiterAllActions.arbiterVerdictRefund(arbiterUid, testData.someConversationId(), refundDto);

        ArbiterConversationDto conversationBefore = workflow.getConversation(testData.someConversationId())
                .orElseThrow();

        arbiterAllActions.arbiterVerdictConfirm(arbiterUid, testData.someConversationId());

        ArbiterConversationDto conversationAfter = workflow.getConversation(testData.someConversationId())
                .orElseThrow();

        Assertions.assertThat(conversationBefore.getStatus())
                .isNotEqualTo(ConversationStatus.VERDICT);

        Assertions.assertThat(conversationAfter.getStatus())
                .isEqualTo(ConversationStatus.VERDICT);
    }

    @Test
    public void testSavedLatestVerdict() {
        TestDataService.TestData testData = testDataService.saveTestData();

        RefundDto refundDto = RANDOM.nextObject(RefundDto.class);
        RequestDocumentsDto requestDocumentsDto = RANDOM.nextObject(RequestDocumentsDto.class);
        Long arbiterUid = RANDOM.nextLong();

        arbiterAllActions.arbiterRequestDocuments(arbiterUid, testData.someConversationId(), requestDocumentsDto);
        arbiterAllActions.arbiterVerdictRefund(arbiterUid, testData.someConversationId(), refundDto);
        arbiterAllActions.arbiterVerdictAgree(arbiterUid, testData.someConversationId());

        ArbiterConversationDto conversationAfter = workflow.getConversation(testData.someConversationId())
                .orElseThrow();

        Assertions.assertThat(conversationAfter.getVerdict())
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .isEqualTo(new VerdictDto()
                        .type(VerdictType.AGREE)
                );
        Assertions.assertThat(conversationAfter.getStatus())
                .isEqualTo(ConversationStatus.VERDICT);
        Assertions.assertThat(conversationAfter.getWaitings())
                .isEmpty();
    }

    @Test
    public void testArbiterVerdictRefund() {
        TestDataService.TestData testData = testDataService.saveTestData();

        RefundDto refundDto = RANDOM.nextObject(RefundDto.class);
        RequestDocumentsDto requestDocumentsDto = RANDOM.nextObject(RequestDocumentsDto.class);
        Long arbiterUid = RANDOM.nextLong();

        arbiterAllActions.arbiterRequestDocuments(arbiterUid, testData.someConversationId(), requestDocumentsDto);

        ArbiterConversationDto conversationBefore = workflow.getConversation(testData.someConversationId())
                .orElseThrow();

        arbiterAllActions.arbiterVerdictRefund(arbiterUid, testData.someConversationId(), refundDto);

        ArbiterConversationDto conversationAfter = workflow.getConversation(conversationBefore.getId())
                .orElseThrow();

        Assertions.assertThat(conversationBefore.getVerdict())
                .isNull();
        Assertions.assertThat(conversationBefore.getStatus())
                .isNotEqualTo(ConversationStatus.WAITING_VERDICT_CONFIRMATION);
        Assertions.assertThat(conversationBefore.getWaitings())
                .isNotEmpty();

        Assertions.assertThat(conversationAfter.getVerdict())
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .isEqualTo(new VerdictDto()
                        .type(VerdictType.REFUND)
                        .refund(refundDto)
                );
        Assertions.assertThat(conversationAfter.getStatus())
                .isEqualTo(ConversationStatus.WAITING_VERDICT_CONFIRMATION);
        Assertions.assertThat(conversationAfter.getWaitings())
                .isEmpty();
    }

    @Test
    public void testArbiterVerdictAgree() {
        TestDataService.TestData testData = testDataService.saveTestData();

        RequestDocumentsDto requestDocumentsDto = RANDOM.nextObject(RequestDocumentsDto.class);
        Long arbiterUid = RANDOM.nextLong();

        arbiterAllActions.arbiterRequestDocuments(arbiterUid, testData.someConversationId(), requestDocumentsDto);

        ArbiterConversationDto conversationBefore = workflow.getConversation(testData.someConversationId())
                .orElseThrow();

        arbiterAllActions.arbiterVerdictAgree(arbiterUid, conversationBefore.getId());

        ArbiterConversationDto conversationAfter = workflow.getConversation(conversationBefore.getId())
                .orElseThrow();

        Assertions.assertThat(conversationBefore.getVerdict())
                .isNull();
        Assertions.assertThat(conversationBefore.getStatus())
                .isNotEqualTo(ConversationStatus.VERDICT);
        Assertions.assertThat(conversationBefore.getWaitings())
                .isNotEmpty();

        Assertions.assertThat(conversationAfter.getVerdict())
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .isEqualTo(new VerdictDto()
                        .type(VerdictType.AGREE)
                );
        Assertions.assertThat(conversationAfter.getStatus())
                .isEqualTo(ConversationStatus.VERDICT);
        Assertions.assertThat(conversationAfter.getWaitings())
                .isEmpty();
    }

    @Test
    public void testArbiterVerdictDecline() {
        TestDataService.TestData testData = testDataService.saveTestData();

        RequestDocumentsDto requestDocumentsDto = RANDOM.nextObject(RequestDocumentsDto.class);
        Long arbiterUid = RANDOM.nextLong();

        arbiterAllActions.arbiterRequestDocuments(arbiterUid, testData.someConversationId(), requestDocumentsDto);

        ArbiterConversationDto conversationBefore = workflow.getConversation(testData.someConversationId())
                .orElseThrow();

        arbiterAllActions.arbiterVerdictDecline(arbiterUid, conversationBefore.getId());

        ArbiterConversationDto conversationAfter = workflow.getConversation(conversationBefore.getId())
                .orElseThrow();

        Assertions.assertThat(conversationBefore.getVerdict())
                .isNull();
        Assertions.assertThat(conversationBefore.getStatus())
                .isNotEqualTo(ConversationStatus.VERDICT);
        Assertions.assertThat(conversationBefore.getWaitings())
                .isNotEmpty();

        Assertions.assertThat(conversationAfter.getVerdict())
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .isEqualTo(new VerdictDto()
                        .type(VerdictType.DECLINE)
                );
        Assertions.assertThat(conversationAfter.getStatus())
                .isEqualTo(ConversationStatus.VERDICT);
        Assertions.assertThat(conversationAfter.getWaitings())
                .isEmpty();
    }
}
