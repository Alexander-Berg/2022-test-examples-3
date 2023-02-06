package ru.yandex.market.tsup.service.data_provider.entity.run.enums;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.mj.generated.client.carrier.model.RunStatusDto;

public class TransportationStateTest {
    @ParameterizedTest(name = "Статус {0} поддержан")
    @MethodSource("runStatusValues")
    void mapped(RunStatusDto runStatus) {
        Assertions.assertTrue(
                Arrays.stream(TransportationState.values())
                        .anyMatch(ms -> ms.getRunStatuses().contains(runStatus))
        );
    }

    @ParameterizedTest(name = "Статус {0} входит только в одно значение MovementState")
    @MethodSource("runStatusValues")
    void single(RunStatusDto runStatus) {
        Assertions.assertTrue(
                Arrays.stream(TransportationState.values())
                        .filter(ms -> ms.getRunStatuses().contains(runStatus))
                        .count() <= 1
        );
    }

    static Stream<Arguments> runStatusValues() {
        return Arrays.stream(RunStatusDto.values())
                .map(Arguments::of);
    }
}
