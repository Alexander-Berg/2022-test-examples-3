package ru.yandex.market.core.offer.content;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.offer.content.parameter.CategoryParameterAssignment;
import ru.yandex.market.ir.http.PartnerContentApi;

/**
 * Тесты для {@link PartnerContentConversions}
 */
@ParametersAreNonnullByDefault
class ParameterAssignmentPartnerContentConversionsTest {
    /**
     * Тест для {@link PartnerContentConversions#toParameterAssignment(PartnerContentApi.ParameterValue)}.
     */
    @Test
    void testToParameterAssignmentBooleanTrue() {
        PartnerContentApi.ParameterValue irAssignment =
                PartnerContentApi.ParameterValue.newBuilder()
                        .setBoolValue(true)
                        .setParamId(398734)
                        .setType(PartnerContentApi.ParameterType.BOOL)
                        .build();
        CategoryParameterAssignment assignment = PartnerContentConversions.toParameterAssignment(irAssignment);
        MatcherAssert.assertThat(
                assignment,
                PartnerContentMatchers.isBooleanParameterAssignment(398734, true)
        );
    }

    /**
     * Тест для {@link PartnerContentConversions#toParameterAssignment(PartnerContentApi.ParameterValue)}.
     */
    @Test
    void testToParameterAssignmentBooleanFalse() {
        PartnerContentApi.ParameterValue irAssignment =
                PartnerContentApi.ParameterValue.newBuilder()
                        .setBoolValue(false)
                        .setParamId(398734)
                        .setType(PartnerContentApi.ParameterType.BOOL)
                        .build();
        CategoryParameterAssignment assignment = PartnerContentConversions.toParameterAssignment(irAssignment);
        MatcherAssert.assertThat(
                assignment,
                PartnerContentMatchers.isBooleanParameterAssignment(398734, false)
        );
    }

    /**
     * Тест для {@link PartnerContentConversions#toParameterAssignment(PartnerContentApi.ParameterValue)}.
     */
    @Test
    void testToParameterAssignmentText() {
        PartnerContentApi.ParameterValue irAssignment =
                PartnerContentApi.ParameterValue.newBuilder()
                        .setStringValue("Help me, I'm held hostage here")
                        .setParamId(2385672)
                        .setType(PartnerContentApi.ParameterType.STRING)
                        .build();
        CategoryParameterAssignment assignment = PartnerContentConversions.toParameterAssignment(irAssignment);
        MatcherAssert.assertThat(
                assignment,
                PartnerContentMatchers.isTextParameterAssignment(2385672, "Help me, I'm held hostage here")
        );
    }

    /**
     * Тест для {@link PartnerContentConversions#toParameterAssignment(PartnerContentApi.ParameterValue)}.
     */
    @Test
    void testToParameterAssignmentNumber() {
        PartnerContentApi.ParameterValue irAssignment =
                PartnerContentApi.ParameterValue.newBuilder()
                        .setNumericValue(1024.5)
                        .setParamId(893456793)
                        .setType(PartnerContentApi.ParameterType.NUMERIC)
                        .build();
        CategoryParameterAssignment assignment = PartnerContentConversions.toParameterAssignment(irAssignment);
        MatcherAssert.assertThat(
                assignment,
                PartnerContentMatchers.isNumericParameterAssignment(893456793, 1024.5)
        );
    }

    /**
     * Тест для {@link PartnerContentConversions#toParameterAssignment(PartnerContentApi.ParameterValue)}.
     */
    @Test
    void testToParameterAssignmentEnumeration() {
        PartnerContentApi.ParameterValue irAssignment =
                PartnerContentApi.ParameterValue.newBuilder()
                        .setEnumOptionId(69389)
                        .setParamId(1234579314)
                        .setType(PartnerContentApi.ParameterType.ENUM)
                        .build();
        CategoryParameterAssignment assignment = PartnerContentConversions.toParameterAssignment(irAssignment);
        MatcherAssert.assertThat(
                assignment,
                PartnerContentMatchers.isEnumeraionParameterAssignment(1234579314L, 69389)
        );
    }
}
