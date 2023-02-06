package ru.yandex.market.arbiter.test.service;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.arbiter.api.server.dto.ArbiterConversationDto;
import ru.yandex.market.arbiter.api.server.dto.ConversationSide;
import ru.yandex.market.arbiter.api.server.dto.ConversationStatus;
import ru.yandex.market.arbiter.api.server.dto.MessageDto;
import ru.yandex.market.arbiter.api.server.dto.RequestDocumentsDto;
import ru.yandex.market.arbiter.api.server.dto.WaitingDto;
import ru.yandex.market.arbiter.test.BaseUnitTest;
import ru.yandex.market.arbiter.test.TestDataService;
import ru.yandex.market.arbiter.test.util.RandomDataGenerator;
import ru.yandex.market.arbiter.workflow.Workflow;
import ru.yandex.market.arbiter.workflow.impl.ArbiterAllActionsImpl;
import ru.yandex.market.arbiter.workflow.impl.ServiceWaitingActionsImpl;

/**
 * @author moskovkin@yandex-team.ru
 * @since 05.06.2020
 */
public class ServiceWaitingActionsImplTest extends BaseUnitTest {
    private static final EnhancedRandom RANDOM =
            RandomDataGenerator.dataRandom(ServiceWaitingActionsImplTest.class).build();

    @Autowired
    private Workflow workflow;

    @Autowired
    private ServiceWaitingActionsImpl serviceWaitingActions;

    @Autowired
    private ArbiterAllActionsImpl arbiterAllActions;


    @Test
    public void testServiceMessageAdd() {
        TestDataService.TestData testData = testDataService.saveTestData();
        MessageDto userMessage = RANDOM.nextObject(MessageDto.class)
                .recipient(ConversationSide.USER) //Should be ignored
                .sender(ConversationSide.USER);
        MessageDto merchantMessage = RANDOM.nextObject(MessageDto.class)
                .recipient(ConversationSide.MERCHANT) //Should be ignored
                .sender(ConversationSide.MERCHANT);
        RequestDocumentsDto userRequest = RANDOM.nextObject(RequestDocumentsDto.class)
                .recipient(ConversationSide.USER);
        WaitingDto userWaiting = new WaitingDto()
                .waitAnswerUntil(userRequest.getWaitAnswerUntil())
                .sender(userRequest.getRecipient());
        RequestDocumentsDto merchantRequest = RANDOM.nextObject(RequestDocumentsDto.class)
                .recipient(ConversationSide.MERCHANT);
        WaitingDto merchantWaiting = new WaitingDto()
                .waitAnswerUntil(merchantRequest.getWaitAnswerUntil())
                .sender(merchantRequest.getRecipient());
        Long arbiterUid = RANDOM.nextLong();

        arbiterAllActions.arbiterRequestDocuments(
                arbiterUid, testData.someConversation().getId(), userRequest
        );
        arbiterAllActions.arbiterRequestDocuments(
                arbiterUid, testData.someConversation().getId(), merchantRequest
        );

        ArbiterConversationDto conv1 = workflow.getConversation(testData.someConversationId()).orElseThrow();
        Assertions.assertThat(conv1.getStatus())
                .isEqualTo(ConversationStatus.WAITING_DOCUMENTS);
        Assertions.assertThat(conv1.getWaitings())
                .usingElementComparatorIgnoringFields("creationTime")
                .containsOnly(
                        userWaiting,
                        merchantWaiting
                );

        // Clear merchant waiting
        serviceWaitingActions.serviceMessageAdd(testData.someConversationId(), merchantMessage);
        ArbiterConversationDto conv2 = workflow.getConversation(testData.someConversationId()).orElseThrow();
        Assertions.assertThat(conv2.getStatus())
                .isEqualTo(ConversationStatus.WAITING_DOCUMENTS);
        Assertions.assertThat(conv2.getWaitings())
                .usingElementComparatorIgnoringFields("creationTime")
                .containsOnly(
                        userWaiting
                );

        // Do not touch user waiting
        serviceWaitingActions.serviceMessageAdd(testData.someConversationId(), merchantMessage);
        ArbiterConversationDto conv3 = workflow.getConversation(testData.someConversationId()).orElseThrow();
        Assertions.assertThat(conv3.getStatus())
                .isEqualTo(ConversationStatus.WAITING_DOCUMENTS);
        Assertions.assertThat(conv3.getWaitings())
                .usingElementComparatorIgnoringFields("creationTime")
                .containsOnly(
                        userWaiting
                );

        // clear user waiting
        serviceWaitingActions.serviceMessageAdd(testData.someConversationId(), userMessage);
        ArbiterConversationDto conv4 = workflow.getConversation(testData.someConversationId()).orElseThrow();
        Assertions.assertThat(conv4.getStatus())
                .isEqualTo(ConversationStatus.IN_PROGRESS);
        Assertions.assertThat(conv4.getWaitings())
                .isEmpty();
    }
}
