package ru.yandex.market.tpl.common.util.datetime;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author ungomma
 */
class TemporalIntervalModuleTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new TemporalIntervalModule());

    @ParameterizedTest
    @MethodSource("temporalIntervalClasses")
    void shouldSerializeInObjectAndBack(TemporalInterval temporalInterval) throws Exception {
        Map<String, TemporalInterval> wrapped = Collections.singletonMap("value", temporalInterval);
        String ser = objectMapper.writeValueAsString(wrapped);

        Map<String, TemporalInterval> deser = objectMapper.readValue(ser,
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, temporalInterval.getClass()));

        assertThat(deser.get("value")).isEqualTo(temporalInterval);
    }

    @ParameterizedTest
    @MethodSource("temporalIntervalClasses")
    void shouldSerializeAsValue(TemporalInterval temporalInterval) throws Exception {

        String serializedValue = StringUtils.strip(
                objectMapper.writeValueAsString(temporalInterval), "\""
        );
        assertThat(serializedValue).isEqualTo(temporalInterval.toISOString());
    }

    static Stream<TemporalInterval> temporalIntervalClasses() throws Exception {
        return StreamEx.of(
                LocalTimeInterval.valueOf("16:20:11-19:00:00"),
                // using fixed start time to ensure stable parametrized test name
                new Interval(Instant.parse("2019-12-12T07:14:13Z"), Duration.ofHours(3))
        );
    }

}
