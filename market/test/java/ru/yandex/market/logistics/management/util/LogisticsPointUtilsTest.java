package ru.yandex.market.logistics.management.util;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.exception.BadRequestException;

class LogisticsPointUtilsTest extends AbstractTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("testSource")
    @DisplayName("Валидация пар полей deferredCourierAvailable и availableForOnDemand логистической точки")
    void testValidateDeferredCourierAndOnDemandAvailability(
        @SuppressWarnings("unused") String displayName,
        Boolean deferredCourierAvailable,
        Boolean availableForOnDemand,
        boolean isExceptionThrown
    ) {
        AbstractThrowableAssert<?, ? extends Throwable> assertion = softly.assertThatCode(
            () -> LogisticsPointUtils.validateDeferredCourierAndOnDemandAvailability(
                deferredCourierAvailable,
                availableForOnDemand
            )
        ).as("Asserting that the exception is " + (isExceptionThrown ? " thrown and valid" : " not thrown"));

        if (isExceptionThrown) {
            assertion
                .isExactlyInstanceOf(BadRequestException.class)
                .hasMessage("400 BAD_REQUEST \"Point must be available for "
                    + "ondemand if it is available for deferred courier\"");
        } else {
            assertion.doesNotThrowAnyException();
        }
    }

    @SuppressWarnings("unused")
    @Nonnull
    private static Stream<Arguments> testSource() {
        return Stream.of(
            Arguments.of("deferredCourierAvailable == false и availableForOnDemand == false", false, false, false),
            Arguments.of("deferredCourierAvailable == false и availableForOnDemand == true", false, true, false),
            Arguments.of("deferredCourierAvailable == true и availableForOnDemand == false", true, false, true),
            Arguments.of("deferredCourierAvailable == true и availableForOnDemand == true", true, true, false),
            Arguments.of("deferredCourierAvailable == null и availableForOnDemand == null", null, null, false),
            Arguments.of("deferredCourierAvailable == null и availableForOnDemand == true", null, false, false),
            Arguments.of("deferredCourierAvailable == true и availableForOnDemand == null", false, null, false),
            Arguments.of("deferredCourierAvailable == null и availableForOnDemand == true", null, true, false),
            Arguments.of("deferredCourierAvailable == true и availableForOnDemand == null", true, null, true)
        );
    }
}
