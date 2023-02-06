package ru.yandex.market.logistics.lom.service.validation;

import java.time.LocalTime;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.converter.EnumConverter;
import ru.yandex.market.logistics.lom.converter.lms.DeliveryTypeLmsConverter;
import ru.yandex.market.logistics.lom.converter.lms.PartnerExternalParamLmsConverter;
import ru.yandex.market.logistics.lom.converter.lms.PartnerTypeLmsConverter;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.embedded.DeliveryInterval;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichContext;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichResults;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.DeliveryIntervalValidator;
import ru.yandex.market.logistics.lom.lms.client.LmsLomLightClient;
import ru.yandex.market.logistics.lom.service.partner.PartnerServiceImpl;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Валидация интервала дат доставки")
class DeliveryIntervalValidatorTest extends AbstractTest {
    private static final LocalTime FROM_TIME = LocalTime.of(8, 0);
    private static final LocalTime TO_TIME = LocalTime.of(19, 30);
    private final LmsLomLightClient lmsLomLightClient = mock(LmsLomLightClient.class);
    private final DeliveryIntervalValidator validator = new DeliveryIntervalValidator(
        new PartnerServiceImpl(
            lmsLomLightClient,
            new DeliveryTypeLmsConverter(),
            new PartnerTypeLmsConverter(new EnumConverter()),
            new PartnerExternalParamLmsConverter()
        )
    );
    private final ValidateAndEnrichContext context = new ValidateAndEnrichContext();

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsLomLightClient);
    }

    @ParameterizedTest(name = "[{index}] {0} = null")
    @MethodSource("nullDeliveryIntervalArguments")
    void nullDeliveryInterval(@SuppressWarnings("unused") String fieldName, Order order) {
        softly.assertThat(validator.validateAndEnrich(order, context).isValidationPassed()).isTrue();
    }

    @Nonnull
    private static Stream<Arguments> nullDeliveryIntervalArguments() {
        return Stream.of(
            Arguments.of("deliveryInterval", new Order()),
            Arguments.of("deliveryInterval.deliveryIntervalId", new Order().setDeliveryInterval(new DeliveryInterval()))
        );
    }

    @Test
    @DisplayName("Не найдено расписание по идентификатору")
    void noScheduleDayFoundByDeliveryIntervalId() {
        Order order = new Order().setDeliveryInterval(defaultDeliveryInterval());
        ValidateAndEnrichResults results = validator.validateAndEnrich(order, context);
        softly.assertThat(results.isValidationFailed()).isTrue();
        softly.assertThat(results.getErrorMessage()).isEqualTo("Failed to find delivery interval by id 1");
        verify(lmsLomLightClient).getScheduleDay(1L);
    }

    @ParameterizedTest(name = "[{index}] {0} не совпадает")
    @MethodSource("timeRangeMismatchArguments")
    void timeRangeMismatch(@SuppressWarnings("unused") String fieldName, DeliveryInterval deliveryInterval) {
        mockLmsClient();
        Order order = new Order().setDeliveryInterval(deliveryInterval);
        ValidateAndEnrichResults results = validator.validateAndEnrich(order, context);
        softly.assertThat(results.isValidationFailed()).isTrue();
        softly.assertThat(results.getErrorMessage())
            .isEqualTo("Delivery interval from/to times doesn't match, interval id 1");
        verify(lmsLomLightClient).getScheduleDay(1L);
    }

    @Nonnull
    private static Stream<Arguments> timeRangeMismatchArguments() {
        return Stream.of(
            Arguments.of("deliveryInterval.startTime", defaultDeliveryInterval().setStartTime(LocalTime.of(10, 0))),
            Arguments.of("deliveryInterval.endTime", defaultDeliveryInterval().setEndTime(LocalTime.of(18, 0)))
        );
    }

    @Test
    @DisplayName("Успех валидации")
    void validationSucceeded() {
        mockLmsClient();
        Order order = new Order().setDeliveryInterval(defaultDeliveryInterval());
        ValidateAndEnrichResults results = validator.validateAndEnrich(order, context);
        softly.assertThat(results.isValidationPassed()).isTrue();
        verify(lmsLomLightClient).getScheduleDay(1L);
    }

    private void mockLmsClient() {
        when(lmsLomLightClient.getScheduleDay(1L))
            .thenReturn(Optional.of(new ScheduleDayResponse(1L, 1, FROM_TIME, TO_TIME)));
    }

    @Nonnull
    private static DeliveryInterval defaultDeliveryInterval() {
        return new DeliveryInterval()
            .setDeliveryIntervalId(1L)
            .setStartTime(FROM_TIME)
            .setEndTime(TO_TIME);
    }
}
