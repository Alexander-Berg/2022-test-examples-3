package ru.yandex.market.tsup.repository.mappers;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.common.data_provider.primitive.SimpleIdFilter;
import ru.yandex.market.tpl.common.data_provider.provider.DataProvider;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.domain.entity.DataProviderLog;

class DataProviderLogMapperTest extends AbstractContextualTest {
    @Autowired
    private DataProviderLogMapper mapper;

    @Autowired
    private ObjectMapper objectMapper;


    @BeforeEach
    void setUp() {
        jsonNode(3_000_000_000L);
    }

    private JsonNode jsonNode(long l) {
        return objectMapper.convertValue(new SimpleIdFilter(l), JsonNode.class);
    }

    @ExpectedDatabase(
        value = "/repository/data_provider_log/after/insert.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void insert() {
        mapper.insert(new DataProviderLog(
            null,
            "provider",
            jsonNode(3_000_000_000L),
            null,
            true,
            false,
            "trace",
            Duration.ofMillis(10)
        ));
    }

    @DatabaseSetup("/repository/data_provider_log/data_provider_log_multiple.xml")
    @Test
    void list() {
        Assertions
            .assertThat(
                mapper.list(
                    Provider1.class,
                    ZonedDateTime.of(2021, 10, 1, 12, 2, 0, 0, ZoneId.systemDefault()),
                    ZonedDateTime.of(2021, 10, 1, 12, 4, 0, 0, ZoneId.systemDefault())
                )
            )
            .isEqualTo(List.of(
                new DataProviderLog(
                    4L,
                    Provider1.class.getName(),
                    jsonNode(3_000_000_000L),
                    ZonedDateTime.of(2021, 10, 1, 12, 3, 0, 0, ZoneId.systemDefault()),
                    true,
                    false,
                    "trace",
                    Duration.ofMillis(13)
                )
            ));
    }

    @DatabaseSetup("/repository/data_provider_log/data_provider_log_multiple.xml")
    @Test
    void listUnlimitedToTime() {
        Assertions
            .assertThat(
                mapper.list(
                    Provider1.class,
                    ZonedDateTime.of(2021, 10, 1, 12, 2, 0, 0, ZoneId.systemDefault()),
                    null
                )
            )
            .isEqualTo(List.of(
                new DataProviderLog(
                    4L,
                    Provider1.class.getName(),
                    jsonNode(3_000_000_000L),
                    ZonedDateTime.of(2021, 10, 1, 12, 3, 0, 0, ZoneId.systemDefault()),
                    true,
                    false,
                    "trace",
                    Duration.ofMillis(13)
                ),
                new DataProviderLog(
                    6L,
                    Provider1.class.getName(),
                    jsonNode(3_000_000_001L),
                    ZonedDateTime.of(2021, 10, 1, 12, 5, 0, 0, ZoneId.systemDefault()),
                    true,
                    false,
                    "trace",
                    Duration.ofMillis(15)
                )
            ));
    }

    @DatabaseSetup("/repository/data_provider_log/data_provider_log_multiple.xml")
    @Test
    void distinctFilters() {
        Assertions
            .assertThat(
                mapper.distinctFilters(
                    Provider1.class,
                    ZonedDateTime.of(2021, 10, 1, 12, 0, 0, 0, ZoneId.systemDefault()),
                    ZonedDateTime.of(2021, 10, 1, 12, 10, 0, 0, ZoneId.systemDefault()),
                    200
                )
            )
            .isEqualTo(List.of(
                jsonNode(3_000_000_001L),
                jsonNode(3_000_000_000L),
                jsonNode(2_999_999_999L)
            ));
    }

    @DatabaseSetup("/repository/data_provider_log/data_provider_log_multiple.xml")
    @Test
    void distinctFiltersLimit() {
        Assertions
            .assertThat(
                mapper.distinctFilters(
                    Provider1.class,
                    ZonedDateTime.of(2021, 10, 1, 12, 0, 0, 0, ZoneId.systemDefault()),
                    ZonedDateTime.of(2021, 10, 1, 12, 4, 0, 0, ZoneId.systemDefault()),
                    2
                )
            )
            .isEqualTo(List.of(
                jsonNode(3_000_000_000L),
                jsonNode(2_999_999_999L)
            ));
    }

    @DatabaseSetup("/repository/data_provider_log/data_provider_log_multiple.xml")
    @Test
    void distinctFiltersUnlimitedToTime() {
        Assertions
            .assertThat(
                mapper.distinctFilters(
                    Provider1.class,
                    ZonedDateTime.of(2021, 10, 1, 12, 01, 0, 0, ZoneId.systemDefault()),
                    ZonedDateTime.of(2021, 10, 1, 12, 10, 0, 0, ZoneId.systemDefault()),
                    1_000
                )
            )
            .isEqualTo(List.of(
                jsonNode(3_000_000_001L),
                jsonNode(3_000_000_000L)
            ));
    }

    abstract static class Provider1 implements DataProvider {

    }
}
