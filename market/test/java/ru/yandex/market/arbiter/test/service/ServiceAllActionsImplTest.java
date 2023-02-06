package ru.yandex.market.arbiter.test.service;

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
import ru.yandex.market.arbiter.test.BaseUnitTest;
import ru.yandex.market.arbiter.test.TestDataService;
import ru.yandex.market.arbiter.test.util.RandomDataGenerator;
import ru.yandex.market.arbiter.workflow.ArbiterException;
import ru.yandex.market.arbiter.workflow.Workflow;
import ru.yandex.market.arbiter.workflow.impl.ArbiterAllActionsImpl;
import ru.yandex.market.arbiter.workflow.impl.ServiceAllActionsImpl;

/**
 * @author moskovkin@yandex-team.ru
 * @since 05.06.2020
 */
public class ServiceAllActionsImplTest extends BaseUnitTest {
    private static final EnhancedRandom RANDOM =
            RandomDataGenerator.dataRandom(ServiceAllActionsImplTest.class).build();

    @Autowired
    private Workflow workflow;

    @Autowired
    private ServiceAllActionsImpl serviceAllActions;

    @Autowired
    private ArbiterAllActionsImpl arbiterAllActions;

    @Test
    public void testServiceVerdictExecute() {
        TestDataService.TestData testData = testDataService.saveTestData();
        Long arbiterUid = RANDOM.nextLong();

        arbiterAllActions.arbiterVerdictDecline(arbiterUid, testData.someConversationId());

        ArbiterConversationDto conversationBefore = workflow.getConversation(testData.someConversationId())
                .orElseThrow();

        serviceAllActions.serviceVerdictExecuted(testData.someConversationId());

        ArbiterConversationDto conversationAfter = workflow.getConversation(testData.someConversationId())
                .orElseThrow();

        Assertions.assertThat(conversationBefore.getStatus())
                .isNotEqualTo(ConversationStatus.CLOSED);

        Assertions.assertThat(conversationAfter.getStatus())
                .isEqualTo(ConversationStatus.CLOSED);
    }

    @Test
    public void testServiceMessageAddForDefaultImpl() {
        TestDataService.TestData testData = testDataService.saveTestData();
        MessageDto message = RANDOM.nextObject(MessageDto.class)
                .recipient(ConversationSide.USER) //Should be ignored
                .sender(ConversationSide.USER);

        ArbiterConversationDto convBefore = workflow.getConversation(testData.someConversationId()).orElseThrow();
        serviceAllActions.serviceMessageAdd(testData.someConversationId(), message);
        ArbiterConversationDto convAfter = workflow.getConversation(testData.someConversationId()).orElseThrow();

        Assertions.assertThat(convBefore.getMessages())
                .usingElementComparatorOnFields("text", "sender")
                .doesNotContain(message);

        Assertions.assertThat(convAfter.getMessages())
                .usingElementComparatorOnFields("text", "recipient", "sender")
                .contains(new MessageDto()
                        .text(message.getText())
                        .recipient(ConversationSide.ARBITER)
                        .sender(message.getSender())
                );

        Assertions.assertThat(convBefore.getStatus())
                .isEqualTo(convAfter.getStatus());
    }

    @Test
    public void testServiceMessageAddThrowsForDefaultImpl() {
        TestDataService.TestData testData = testDataService.saveTestData();
        MessageDto message = RANDOM.nextObject(MessageDto.class).sender(ConversationSide.ARBITER);

        Assertions.assertThatThrownBy(() ->
                serviceAllActions.serviceMessageAdd(testData.someConversationId(), message)
        )
        .isInstanceOf(ArbiterException.class);
    }

    @Test
    public void testServiceMessageAddThrowsForIncorrectSender() {
        TestDataService.TestData testData = testDataService.saveTestData();
        MessageDto message = RANDOM.nextObject(MessageDto.class).sender(ConversationSide.ARBITER);

        Assertions.assertThatThrownBy(() ->
                serviceAllActions.serviceMessageAdd(testData.someConversationId(), message)
        )
        .isInstanceOf(ArbiterException.class);
    }

    @Test
    public void testServiceVerdictAgree() {
        TestDataService.TestData testData = testDataService.saveTestData();

        RequestDocumentsDto requestDocumentsDto = RANDOM.nextObject(RequestDocumentsDto.class);
        Long arbiterUid = RANDOM.nextLong();

        arbiterAllActions.arbiterRequestDocuments(arbiterUid, testData.someConversationId(), requestDocumentsDto);

        ArbiterConversationDto conversationBefore = workflow.getConversation(testData.someConversationId())
                .orElseThrow();

        serviceAllActions.serviceVerdictAgree(conversationBefore.getId());

        ArbiterConversationDto conversationAfter = workflow.getConversation(conversationBefore.getId())
                .orElseThrow();

        Assertions.assertThat(conversationBefore.getVerdict())
                .isNull();
        Assertions.assertThat(conversationBefore.getStatus())
                .isNotEqualTo(ConversationStatus.CLOSED);
        Assertions.assertThat(conversationBefore.getWaitings())
                .isNotEmpty();

        Assertions.assertThat(conversationAfter.getVerdict())
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .isEqualTo(new VerdictDto()
                        .type(VerdictType.AGREE)
                );
        Assertions.assertThat(conversationAfter.getStatus())
                .isEqualTo(ConversationStatus.CLOSED);
        Assertions.assertThat(conversationAfter.getWaitings())
                .isEmpty();
    }

    @Test
    public void testServiceVerdictRefund() {
        TestDataService.TestData testData = testDataService.saveTestData();

        RefundDto refundDto = RANDOM.nextObject(RefundDto.class);
        RequestDocumentsDto requestDocumentsDto = RANDOM.nextObject(RequestDocumentsDto.class);
        Long arbiterUid = RANDOM.nextLong();

        arbiterAllActions.arbiterRequestDocuments(arbiterUid, testData.someConversationId(), requestDocumentsDto);

        ArbiterConversationDto conversationBefore = workflow.getConversation(testData.someConversationId())
                .orElseThrow();

        serviceAllActions.serviceVerdictRefund(testData.someConversationId(), refundDto);

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
                .isEqualTo(ConversationStatus.CLOSED);
        Assertions.assertThat(conversationAfter.getWaitings())
                .isEmpty();
    }
}
