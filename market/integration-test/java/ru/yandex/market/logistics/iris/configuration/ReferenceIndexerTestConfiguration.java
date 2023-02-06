package ru.yandex.market.logistics.iris.configuration;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.test.context.TestConfiguration;

import ru.yandex.market.logistics.iris.core.index.ReferenceIndexerTestFactory;
import ru.yandex.market.logistics.iris.core.index.field.PredefinedFieldProvider;
import ru.yandex.market.logistics.iris.domain.converter.FixedUtcTimestampProvider;
import ru.yandex.market.logistics.iris.util.UtcTimestampProvider;

import static java.time.ZoneOffset.UTC;

@TestConfiguration
public class ReferenceIndexerTestConfiguration extends ReferenceIndexerConfiguration {

    @Override
    public PredefinedFieldProvider predefinedFieldProvider() {
        return ReferenceIndexerTestFactory.getProvider();
    }

    @Override
    public UtcTimestampProvider utcTimestampProvider() {
        LocalDateTime localDateTime = LocalDateTime.parse("2016-01-23T12:34:56", DateTimeFormatter.ISO_DATE_TIME);
        ZonedDateTime updatedDateTime = ZonedDateTime.of(localDateTime, UTC);
        return new FixedUtcTimestampProvider(updatedDateTime);
    }
}
