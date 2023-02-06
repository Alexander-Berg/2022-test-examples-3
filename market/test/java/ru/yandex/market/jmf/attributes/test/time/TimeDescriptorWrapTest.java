package ru.yandex.market.jmf.attributes.test.time;

import java.time.LocalTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.crm.util.Randoms;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class TimeDescriptorWrapTest extends AbstractTimeDescriptorTest {

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void wrap(@SuppressWarnings("unused") String testName, Object rawValue, LocalTime expected) {
        LocalTime result = descriptor.wrap(attribute, type, rawValue);
        Assertions.assertEquals(expected, result);
    }

    private static Stream<Arguments> data() {
        var randomValue = Randoms.time();

        return Stream.of(
                arguments("null", null, null),
                arguments("random", randomValue, randomValue),
                arguments("hh_mm", "07:13:00", LocalTime.of(7, 13)),
                arguments("hh_mm_ss", "11:13:17", LocalTime.of(11, 13, 17))
        );
    }
}
