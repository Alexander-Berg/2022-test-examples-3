package ru.yandex.market.delivery.mdbapp.components.failover;

import java.time.ZonedDateTime;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.mdbapp.AbstractMediumContextualTest;
import ru.yandex.market.delivery.mdbapp.components.queue.lom.order.event.LomEventFailoverQueueConsumer;
import ru.yandex.market.delivery.mdbapp.configuration.FeatureProperties;
import ru.yandex.market.delivery.mdbapp.configuration.queue.LomOrderEventFailoverConfiguration;
import ru.yandex.market.delivery.mdbapp.integration.service.LomOrderEventService;
import ru.yandex.market.logistics.lom.model.dto.EventDto;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Тест консьюмера фейловера лома")
class LomEventFailoverQueueConsumerTest extends AbstractMediumContextualTest {

    private LomOrderEventService lomOrderEventService;

    @Autowired
    private FeatureProperties featureProperties;

    private LomEventFailoverQueueConsumer consumer;

    @BeforeEach
    void setUp() {
        lomOrderEventService = Mockito.mock(LomOrderEventService.class);
        consumer = new LomEventFailoverQueueConsumer(lomOrderEventService, featureProperties);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lomOrderEventService);
        featureProperties.setFailoverConsumerEnabled(false);
    }

    @Test
    @DisplayName("Консьюмер включен")
    public void consumerEnabled() {
        featureProperties.setFailoverConsumerEnabled(true);
        LomOrderEventFailoverConfiguration.EventDtoWithError dto = getEventWithErrorDto();
        consumer.processTask(getQueueTask(dto));
        verify(lomOrderEventService).processEvent(Mockito.eq(dto.getEventDto()));
    }

    @Test
    @DisplayName("Консьюмер выключен")
    public void consumerDisabled() {
        featureProperties.setFailoverConsumerEnabled(false);
        LomOrderEventFailoverConfiguration.EventDtoWithError dto = getEventWithErrorDto();
        consumer.processTask(getQueueTask(dto));
    }

    @Nonnull
    private LomOrderEventFailoverConfiguration.EventDtoWithError getEventWithErrorDto() {
        return new LomOrderEventFailoverConfiguration.EventDtoWithError("error", new EventDto().setId(111));
    }

    @Nonnull
    private Task<LomOrderEventFailoverConfiguration.EventDtoWithError> getQueueTask(
        LomOrderEventFailoverConfiguration.EventDtoWithError dto
    ) {
        return new Task<>(new QueueShardId("1"), dto, 0, ZonedDateTime.now(), null, null);
    }
}
