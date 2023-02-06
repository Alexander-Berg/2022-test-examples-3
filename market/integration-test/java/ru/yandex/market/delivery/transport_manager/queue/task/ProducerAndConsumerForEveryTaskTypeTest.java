package ru.yandex.market.delivery.transport_manager.queue.task;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.base.consumer.BaseQueueConsumer;
import ru.yandex.market.delivery.transport_manager.queue.base.producer.BaseQueueProducer;

class ProducerAndConsumerForEveryTaskTypeTest extends AbstractContextualTest {
    private Map<TaskType, List<BaseQueueProducer<?>>> producers;

    private Map<TaskType, List<BaseQueueConsumer<?>>> consumers;

    @Autowired
    public void setProducers(List<BaseQueueProducer<?>> producers) {
        this.producers = producers.stream()
            .filter(p -> Objects.nonNull(p.getTaskType()))
            .collect(Collectors.groupingBy(BaseQueueProducer::getTaskType));
    }

    @Autowired
    public void setConsumers(List<BaseQueueConsumer<?>> consumers) {
        this.consumers = consumers.stream()
            .filter(c -> Objects.nonNull(c.getTaskType()))
            .collect(Collectors.groupingBy(BaseQueueConsumer::getTaskType));
    }

    @ParameterizedTest
    @MethodSource("taskTypes")
    void producer(TaskType taskType) {
        List<BaseQueueProducer<?>> producersOfType = producers.get(taskType);
        softly.assertThat(producersOfType)
            .withFailMessage("Producer for {} is not found", taskType)
            .isNotNull();
        softly.assertThat(producersOfType)
            .withFailMessage("Multiple producers for {}  found", taskType)
            .hasSize(1);
    }

    @ParameterizedTest
    @MethodSource("taskTypes")
    void consumer(TaskType taskType) {
        List<BaseQueueConsumer<?>> consumersOfType = consumers.get(taskType);
        softly.assertThat(consumersOfType)
            .withFailMessage("Consumer for {} is not found")
            .isNotNull();
        softly.assertThat(consumersOfType)
            .withFailMessage("Multiple consumers for {}  found")
            .hasSize(1);
    }

    static Stream<Arguments> taskTypes() {
        return Stream
            .of(TaskType.values())
            .map(Arguments::of);
    }
}
