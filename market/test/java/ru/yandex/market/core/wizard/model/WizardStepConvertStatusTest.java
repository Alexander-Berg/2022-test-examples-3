package ru.yandex.market.core.wizard.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.step.CrossborderLegalStepStatusCalculator;

/**
 * Тесты для {@link CrossborderLegalStepStatusCalculator#convertStatus(PartnerApplicationStatus)}.
 *
 * @author ogonek 20.11.2018
 */
public class WizardStepConvertStatusTest {

    private static final Set<PartnerApplicationStatus> INCONVERTIBLE_STATUSES = ImmutableSet.of(
            PartnerApplicationStatus.NEW_PROGRAMS_VERIFICATION_REQUIRED,
            PartnerApplicationStatus.NEW_PROGRAMS_VERIFICATION_FAILED
    );

    public static Stream<Arguments> source() {
        return Stream.of(
                Arguments.of(PartnerApplicationStatus.NEW, Status.EMPTY),
                Arguments.of(PartnerApplicationStatus.CLOSED, Status.EMPTY),
                Arguments.of(PartnerApplicationStatus.CANCELLED, Status.EMPTY),
                Arguments.of(PartnerApplicationStatus.INTERNAL_CLOSED, Status.EMPTY),
                Arguments.of(PartnerApplicationStatus.NEED_INFO, Status.EMPTY),
                Arguments.of(PartnerApplicationStatus.IN_PROGRESS, Status.ENABLING),
                Arguments.of(PartnerApplicationStatus.INIT, Status.ENABLING),
                Arguments.of(PartnerApplicationStatus.COMPLETED, Status.FILLED),
                Arguments.of(PartnerApplicationStatus.FROZEN, Status.FILLED),
                Arguments.of(PartnerApplicationStatus.DECLINED, Status.FAILED)
        );
    }

    /**
     * Проверяет, что если человек добавил новый PartnerApplicationStatus, то он прописал для него конвертер в
     * {@link CrossborderLegalStepStatusCalculator#convertStatus(PartnerApplicationStatus)}.
     * Если конвертер осознанно не нужен - новый статус следует добавить в INCOVERTABLE_STATUSES.
     */
    @Test
    public void convertNewStatusTest() {
        Set<PartnerApplicationStatus> convertibleStatuses = new HashSet<>(Arrays.asList(PartnerApplicationStatus.values()));
        convertibleStatuses.removeAll(INCONVERTIBLE_STATUSES);

        for (PartnerApplicationStatus status : convertibleStatuses) {
            CrossborderLegalStepStatusCalculator.convertStatus(status);
        }

        for (PartnerApplicationStatus status : INCONVERTIBLE_STATUSES) {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> CrossborderLegalStepStatusCalculator.convertStatus(status)
            );
        }
    }

    /**
     * Тест проверяет, что {@link CrossborderLegalStepStatusCalculator#convertStatus(PartnerApplicationStatus)}
     * конвертирует статусы корректно.
     */
    @ParameterizedTest
    @MethodSource("source")
    public void convertAllStatusTest(PartnerApplicationStatus rawStatus, Status expected) {
        Assertions.assertEquals(expected, CrossborderLegalStepStatusCalculator.convertStatus(rawStatus));
    }

}
