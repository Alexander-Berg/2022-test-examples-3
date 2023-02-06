package ru.yandex.market.sre.services.tms.eventdetector.dao.repository;

import java.time.Duration;

import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.mockito.Mockito.mock;

public class IndicatorRawEventsRepositoryTest {

    @Test
    public void deleteOlderThen() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        IndicatorRawEventsRepository repository = new IndicatorRawEventsRepository(mongoTemplate);
        repository.deleteOlderThen(Duration.ofDays(30));
    }
}
