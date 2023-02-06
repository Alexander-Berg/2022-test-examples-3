package ru.yandex.market.core.program.partner.calculator;

import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.program.partner.model.NeedTestingState;
import ru.yandex.market.core.program.partner.model.ProgramArgs;
import ru.yandex.market.core.program.partner.model.ProgramStatus;
import ru.yandex.market.core.program.partner.model.ProgramSubStatus;
import ru.yandex.market.core.program.partner.model.ProgramType;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.program.partner.model.Substatus;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяем расчет статусов для DELIVERY в
 * {@link ru.yandex.market.core.program.partner.calculator.DeliveryProgramCalculator}.
 */
@DbUnitDataSet(before = "DeliveryProgramCalculatorTest.before.csv")
class DeliveryProgramCalculatorTest extends FunctionalTest {

    @Autowired
    private ProgramCalculator deliveryProgramCalculator;

    private static Stream<Arguments> calculateTestData() {
        return Stream.of(
                Arguments.of(50L, Status.FAILED, null),
                Arguments.of(51L, Status.FAILED, Substatus.CLOSED),
                Arguments.of(52L, Status.FAILED, Substatus.FILL_APPLICATION),
                Arguments.of(53L, Status.TESTING_FAILED, null),
                Arguments.of(54L, Status.FULL, null),
                Arguments.of(55L, Status.TESTING_HOLD, Substatus.NEED_INFO),
                Arguments.of(56L, Status.TESTING, null)
        );
    }


    @ParameterizedTest
    @MethodSource("calculateTestData")
    void calculateTest(long datasourceId, Status expectedStatus, Substatus expectedSubstatus) {
        ProgramStatus calculatedStatus = deliveryProgramCalculator.calculate(
                0L, datasourceId, ProgramArgs.builder().build()
        );
        ProgramStatus expected = getExpected(expectedSubstatus, expectedStatus);
        assertThat(calculatedStatus).isEqualTo(expected);
    }

    private static ProgramStatus getExpected(@Nullable Substatus substatus, Status status) {
        ProgramStatus.Builder builder = ProgramStatus.builder()
                .status(status)
                .program(ProgramType.DELIVERY)
                .needTestingState(NeedTestingState.NOT_REQUIRED)
                .enabled(true);

        if (substatus != null) {
            builder.addSubStatus(ProgramSubStatus.builder().code(substatus).build());
        }
        return builder.build();
    }

}
