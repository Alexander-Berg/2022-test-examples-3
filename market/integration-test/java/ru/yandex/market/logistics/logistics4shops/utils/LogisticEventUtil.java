package ru.yandex.market.logistics.logistics4shops.utils;

import java.util.function.Function;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.logistics.logistics4shops.event.model.LogisticEvent;
import ru.yandex.market.logistics.logistics4shops.repository.LogisticEventRepository;

@Component
@RequiredArgsConstructor
public class LogisticEventUtil {

    @InjectSoftAssertions
    protected SoftAssertions softly;

    private final LogisticEventRepository repository;

    @Transactional
    @SneakyThrows
    public LogisticEvent getEventPayload(long id) {
        return LogisticEvent.parseFrom(repository.getById(id).getEventBytes());
    }

    @Transactional
    public void assertEventPayload(
        long id,
        Function<LogisticEvent, Object> payloadExtractor,
        Object expectedPayload,
        SoftAssertions softly
    ) {
        LogisticEvent eventPayload = getEventPayload(id);
        ProtobufAssertionsUtils.prepareProtobufAssertion(softly.assertThat(payloadExtractor.apply(eventPayload)))
            .isEqualTo(expectedPayload);
    }

}
