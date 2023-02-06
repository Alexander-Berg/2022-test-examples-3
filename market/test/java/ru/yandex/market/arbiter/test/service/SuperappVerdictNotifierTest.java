package ru.yandex.market.arbiter.test.service;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.arbiter.api.consumer.client.ArbiterConsumerApi;
import ru.yandex.market.arbiter.api.consumer.client.dto.ArbiterVerdictDto;
import ru.yandex.market.arbiter.api.server.dto.ArbiterConversationDto;
import ru.yandex.market.arbiter.api.server.dto.ConversationStatus;
import ru.yandex.market.arbiter.api.server.dto.RefundDto;
import ru.yandex.market.arbiter.api.server.dto.VerdictDto;
import ru.yandex.market.arbiter.api.server.dto.VerdictType;
import ru.yandex.market.arbiter.service.VerdictNotifierArbiterConsumerImpl;
import ru.yandex.market.arbiter.test.BaseUnitTest;
import ru.yandex.market.arbiter.test.TestDataService;
import ru.yandex.market.arbiter.test.TestMapper;
import ru.yandex.market.arbiter.test.util.RandomDataGenerator;
import ru.yandex.market.arbiter.workflow.Workflow;

/**
 * @author moskovkin@yandex-team.ru
 * @since 09.06.2020
 */
public class SuperappVerdictNotifierTest extends BaseUnitTest {
    private static final EnhancedRandom RANDOM =
            RandomDataGenerator.dataRandom(SuperappVerdictNotifierTest.class).build();

    @Autowired
    private VerdictNotifierArbiterConsumerImpl superappVerdictNotifier;

    @Autowired
    private ArbiterConsumerApi mockSuperappArbiterConsumerApi;

    @Autowired
    private Workflow workflow;

    @Autowired
    private TestMapper testMapper;

    @Test
    public void testNotifyVerdictConsumerSendVerdictToApi() {
        TestDataService.TestData testData = testDataService.saveTestData();
        Long arbiterUid = RANDOM.nextLong();
        RefundDto refundDto = RANDOM.nextObject(RefundDto.class);

        workflow.arbiterVerdictRefund(arbiterUid, testData.someConversationId(), refundDto);
        workflow.arbiterVerdictConfirm(arbiterUid, testData.someConversationId());
        superappVerdictNotifier.notifyVerdictConsumer(testData.someConversationId());

        Mockito.verify(mockSuperappArbiterConsumerApi)
                .conversationVerdictPostWithHttpInfo(testData.someConversationId(), new ArbiterVerdictDto()
                        .type(ru.yandex.market.arbiter.api.consumer.client.dto.VerdictType.REFUND)
                        .refund(testMapper.mapToClientRefundDto(refundDto))
                );
    }
}
