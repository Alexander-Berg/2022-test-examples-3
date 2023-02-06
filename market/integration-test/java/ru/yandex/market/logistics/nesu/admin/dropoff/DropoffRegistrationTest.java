package ru.yandex.market.logistics.nesu.admin.dropoff;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.admin.model.enums.AdminCargoType;
import ru.yandex.market.logistics.nesu.admin.model.response.AdminDropoffRegistrationNewDto;
import ru.yandex.market.logistics.nesu.admin.model.response.AdminScheduleDto;
import ru.yandex.market.logistics.nesu.admin.utils.AdminValidationUtils;
import ru.yandex.market.logistics.nesu.jobs.producer.DropoffRegistrationProducer;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.utils.MatcherUtils;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Регистрация дропоффа")
@ParametersAreNonnullByDefault
class DropoffRegistrationTest extends AbstractContextualTest {
    private static final long DROPOFF_LOGISTIC_POINT_ID = 100L;

    @Autowired
    private DropoffRegistrationProducer dropoffRegistrationProducer;

    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void setup() {
        doNothing().when(dropoffRegistrationProducer).produceTask(anyLong());
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient, dropoffRegistrationProducer);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Валидация полей")
    @MethodSource({"nullFieldsValidation", "scheduleIntervalValidation", "logisticPointScheduleValidation"})
    void fieldsValidation(
        @SuppressWarnings("unused") String displayName,
        UnaryOperator<AdminDropoffRegistrationNewDto> dropoffRegistrationNewDtoUpdater,
        ValidationErrorData errorData,
        boolean verifyGetDropoffLogisticsPoint
    ) throws Exception {
        mockGetDropoffLogisticsPoint();

        register(dropoffRegistrationNewDtoUpdater.apply(dropoffRegistrationNewDto()))
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(errorData));

        if (verifyGetDropoffLogisticsPoint) {
            verifyGetDropoffLogisticsPoint();
        }
    }

    @Nonnull
    private static Stream<Arguments> nullFieldsValidation() {
        return Stream.of(
            Arguments.of(
                "Не указана точки дропоффа",
                (UnaryOperator<AdminDropoffRegistrationNewDto>) dto -> dto.setLogisticPointId(null),
                nullError("logisticPointId"),
                false
            ),
            Arguments.of(
                "Не указан склад сортировочного центра",
                (UnaryOperator<AdminDropoffRegistrationNewDto>) dto -> dto.setSortingCenterWarehouseId(null),
                nullError("sortingCenterWarehouseId"),
                false
            ),
            Arguments.of(
                "Не указана служба доставки",
                (UnaryOperator<AdminDropoffRegistrationNewDto>) dto -> dto.setDeliveryServiceId(null),
                nullError("deliveryServiceId"),
                false
            ),
            Arguments.of(
                "Не указан регион доступности",
                (UnaryOperator<AdminDropoffRegistrationNewDto>) dto -> dto.setAvailabilityLocationId(null),
                nullError("availabilityLocationId"),
                false
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> scheduleIntervalValidation() {
        return Stream.of(
            Arguments.of(
                "Расписание приёма заказов: время начала периода не задано",
                (UnaryOperator<AdminDropoffRegistrationNewDto>) dto -> {
                    dto.getAvailabilitySchedule().setMondayFrom(null);
                    return dto;
                },
                adminScheduleError("availabilitySchedule.mondayFrom", "Не задано время начала"),
                false
            ),
            Arguments.of(
                "Расписание приёма заказов: время начала периода не задано",
                (UnaryOperator<AdminDropoffRegistrationNewDto>) dto -> {
                    dto.getAvailabilitySchedule().setTuesdayTo(null);
                    return dto;
                },
                adminScheduleError("availabilitySchedule.tuesdayTo", "Не задано время окончания"),
                false
            ),
            Arguments.of(
                "Расписание приёма заказов: время начала периода позже времени окончания периода",
                (UnaryOperator<AdminDropoffRegistrationNewDto>) dto -> {
                    dto.getAvailabilitySchedule()
                        .setMondayFrom(LocalTime.of(15, 0))
                        .setMondayTo(LocalTime.of(14, 0));
                    return dto;
                },
                adminScheduleError(
                    "availabilitySchedule.mondayFrom",
                    "Время начала 15:00 должно быть меньше времени окончания 14:00"
                ),
                false
            ),
            Arguments.of(
                "Расписание отгрузки заказов: время начала периода не задано",
                (UnaryOperator<AdminDropoffRegistrationNewDto>) dto -> {
                    dto.getTmMovementSchedule().setTuesdayFrom(null);
                    return dto;
                },
                adminScheduleError("tmMovementSchedule.tuesdayFrom", "Не задано время начала"),
                false
            ),
            Arguments.of(
                "Расписание отгрузки заказов: время начала периода не задано",
                (UnaryOperator<AdminDropoffRegistrationNewDto>) dto -> {
                    dto.getTmMovementSchedule().setMondayTo(null);
                    return dto;
                },
                adminScheduleError("tmMovementSchedule.mondayTo", "Не задано время окончания"),
                false
            ),
            Arguments.of(
                "Расписание отгрузки заказов: время начала периода позже времени окончания периода",
                (UnaryOperator<AdminDropoffRegistrationNewDto>) dto -> {
                    dto.getTmMovementSchedule()
                        .setTuesdayFrom(LocalTime.of(15, 0))
                        .setTuesdayTo(LocalTime.of(14, 0));
                    return dto;
                },
                adminScheduleError(
                    "tmMovementSchedule.tuesdayFrom",
                    "Время начала 15:00 должно быть меньше времени окончания 14:00"
                ),
                false
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> logisticPointScheduleValidation() {
        return Stream.of(
            Arguments.of(
                "Расписание приёма заказов: нерабочий день точки",
                (UnaryOperator<AdminDropoffRegistrationNewDto>) dto -> {
                    dto.getAvailabilitySchedule()
                        .setWednesdayFrom(LocalTime.of(10, 0))
                        .setWednesdayTo(LocalTime.of(18, 0));
                    return dto;
                },
                adminScheduleLogisticPointNotWorkingScheduleError("availabilitySchedule.wednesdayFrom"),
                true
            ),
            Arguments.of(
                "Расписание приёма заказов: не заполнен рабочий день точки",
                (UnaryOperator<AdminDropoffRegistrationNewDto>) dto -> {
                    dto.getAvailabilitySchedule()
                        .setTuesdayFrom(null)
                        .setTuesdayTo(null);
                    return dto;
                },
                adminScheduleLogisticPointWorkingScheduleError("availabilitySchedule.tuesdayFrom"),
                true
            ),
            Arguments.of(
                "Расписание приёма заказов: время начала периода раньше времени начала работы точки",
                (UnaryOperator<AdminDropoffRegistrationNewDto>) dto -> {
                    dto.getAvailabilitySchedule()
                        .setMondayFrom(LocalTime.of(9, 59))
                        .setMondayTo(LocalTime.of(18, 0));
                    return dto;
                },
                adminScheduleError(
                    "availabilitySchedule.mondayFrom",
                    "Время 09:59 раньше времени начала работы логистической точки 10:00"
                ),
                true
            ),
            Arguments.of(
                "Расписание приёма заказов: время окончания периода позже времени окончания работы точки",
                (UnaryOperator<AdminDropoffRegistrationNewDto>) dto -> {
                    dto.getAvailabilitySchedule()
                        .setMondayFrom(LocalTime.of(10, 0))
                        .setMondayTo(LocalTime.of(18, 1));
                    return dto;
                },
                adminScheduleError(
                    "availabilitySchedule.mondayTo",
                    "Время 18:01 позже времени окончания работы логистической точки 18:00"
                ),
                true
            ),
            Arguments.of(
                "Расписание отгрузки заказов: нерабочий день точки",
                (UnaryOperator<AdminDropoffRegistrationNewDto>) dto -> {
                    dto.getTmMovementSchedule()
                        .setWednesdayFrom(LocalTime.of(10, 0))
                        .setWednesdayTo(LocalTime.of(18, 0));
                    return dto;
                },
                adminScheduleLogisticPointNotWorkingScheduleError("tmMovementSchedule.wednesdayFrom"),
                true
            ),
            Arguments.of(
                "Расписание отгрузки заказов: не заполнен рабочий день точки",
                (UnaryOperator<AdminDropoffRegistrationNewDto>) dto -> {
                    dto.getTmMovementSchedule()
                        .setTuesdayFrom(null)
                        .setTuesdayTo(null);
                    return dto;
                },
                adminScheduleLogisticPointWorkingScheduleError("tmMovementSchedule.tuesdayFrom"),
                true
            ),
            Arguments.of(
                "Расписание отгрузки заказов: время начала периода раньше времени начала работы точки",
                (UnaryOperator<AdminDropoffRegistrationNewDto>) dto -> {
                    dto.getTmMovementSchedule()
                        .setTuesdayFrom(LocalTime.of(9, 59))
                        .setTuesdayTo(LocalTime.of(18, 0));
                    return dto;
                },
                adminScheduleError(
                    "tmMovementSchedule.tuesdayFrom",
                    "Время 09:59 раньше времени начала работы логистической точки 10:00"
                ),
                true
            ),
            Arguments.of(
                "Расписание отгрузки заказов: время окончания периода позже времени окончания работы точки",
                (UnaryOperator<AdminDropoffRegistrationNewDto>) dto -> {
                    dto.getTmMovementSchedule()
                        .setTuesdayFrom(LocalTime.of(10, 0))
                        .setTuesdayTo(LocalTime.of(18, 1));
                    return dto;
                },
                adminScheduleError(
                    "tmMovementSchedule.tuesdayTo",
                    "Время 18:01 позже времени окончания работы логистической точки 18:00"
                ),
                true
            )
        );
    }

    @Test
    @DisplayName("Валидация полей: расписание логистической точки null")
    void logisticPointScheduleIsNull() throws Exception {
        mockGetDropoffLogisticsPoint(null);

        register(dropoffRegistrationNewDto())
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(List.of(
                adminScheduleLogisticPointNotWorkingScheduleError("availabilitySchedule.mondayFrom"),
                adminScheduleLogisticPointNotWorkingScheduleError("availabilitySchedule.tuesdayFrom"),
                adminScheduleLogisticPointNotWorkingScheduleError("tmMovementSchedule.mondayFrom"),
                adminScheduleLogisticPointNotWorkingScheduleError("tmMovementSchedule.tuesdayFrom")
            )));

        verifyGetDropoffLogisticsPoint();
    }

    @Test
    @DisplayName("Успех")
    @ExpectedDatabase(
        value = "/repository/dropoff/after/dropoff_registration_state_enqueued.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void registerDropoff() throws Exception {
        mockGetDropoffLogisticsPoint();

        register(
            dropoffRegistrationNewDto()
                .setAvailabilityOrdersPerPartnerLimit(100)
                .setAvailabilityForbiddenCargoTypes(List.of(AdminCargoType.JEWELRY, AdminCargoType.ART))
        )
            .andExpect(status().isOk())
            .andExpect(content().string("1"));
        verifyGetDropoffLogisticsPoint();
        verify(dropoffRegistrationProducer).produceTask(1);
    }

    @Test
    @DisplayName("Успех. Не передан флаг доступности для магазинов")
    @ExpectedDatabase(
        value = "/repository/dropoff/after/dropoff_registration_state_availability_enabled_null.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void registerDropoffAvailabilityEnabledIsNull() throws Exception {
        mockGetDropoffLogisticsPoint();

        register(dropoffRegistrationNewDto().setAvailabilityEnabled(null))
            .andExpect(status().isOk())
            .andExpect(content().string("1"));
        verifyGetDropoffLogisticsPoint();
        verify(dropoffRegistrationProducer).produceTask(1);
    }

    @Test
    @DisplayName("Успех. Не передан флаг возврата на дропофф")
    @ExpectedDatabase(
        value = "/repository/dropoff/after/dropoff_registration_state_return_to_dropoff_enabled_null.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void registerReturnToDropoffEnabledIsNull() throws Exception {
        mockGetDropoffLogisticsPoint();

        register(dropoffRegistrationNewDto().setReturnToDropoffEnabled(null))
            .andExpect(status().isOk())
            .andExpect(content().string("1"));
        verifyGetDropoffLogisticsPoint();
        verify(dropoffRegistrationProducer).produceTask(1);
    }

    @Test
    @DisplayName("Ошибка. Не передано значение продолжительности перемещения")
    void registerMovementDurationIsNull() throws Exception {
        mockGetDropoffLogisticsPoint();

        register(dropoffRegistrationNewDto().setMovementDurationHours(null))
            .andExpect(status().isBadRequest())
            .andExpect(
                MatcherUtils.validationErrorMatcher(
                    AdminValidationUtils.createNullFieldError("movementDurationHours", "adminDropoffRegistrationNewDto")
                )
            );
    }

    @Test
    @DisplayName("Ошибка. Есть активный процесс регистрации дропоффа для точки")
    @DatabaseSetup("/repository/dropoff/dropoff_registration_state_enqueued.xml")
    void registerDropoffEnqueuedProcessExists() throws Exception {
        mockGetDropoffLogisticsPoint();

        register(dropoffRegistrationNewDto())
            .andExpect(status().is5xxServerError())
            .andExpect(errorMessage(
                "could not execute statement; SQL [n/a]; constraint [dropoff_registration_state_pkey]; nested exception"
                    + " is org.hibernate.exception.ConstraintViolationException: could not execute statement"
            ));
        verifyGetDropoffLogisticsPoint();
    }

    @Test
    @DisplayName("Ошибка. Логистическая точка не найдена")
    void registerDropoffLogisticPointNotFound() throws Exception {
        register(dropoffRegistrationNewDto())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [LOGISTICS_POINT] with ids [100]"));
        verifyGetDropoffLogisticsPoint();
    }

    private void mockGetDropoffLogisticsPoint() {
        mockGetDropoffLogisticsPoint(Set.of(LmsFactory.createScheduleDayDto(1), LmsFactory.createScheduleDayDto(2)));
    }

    private void mockGetDropoffLogisticsPoint(@Nullable Set<ScheduleDayResponse> schedule) {
        when(lmsClient.getLogisticsPoint(DROPOFF_LOGISTIC_POINT_ID))
            .thenReturn(Optional.of(
                LmsFactory.createLogisticsPointResponseBuilder(
                    DROPOFF_LOGISTIC_POINT_ID,
                    1000L,
                    "Dropoff point",
                    PointType.PICKUP_POINT
                )
                    .schedule(schedule)
                    .build()
            ));
    }

    @Nonnull
    private ResultActions register(AdminDropoffRegistrationNewDto dropoffRegistrationNewDto) throws Exception {
        return mockMvc.perform(request(
            HttpMethod.POST,
            "/admin/dropoff-registration",
            dropoffRegistrationNewDto
        ));
    }

    @Nonnull
    private AdminDropoffRegistrationNewDto dropoffRegistrationNewDto() {
        return new AdminDropoffRegistrationNewDto()
            .setLogisticPointId(100L)
            .setSortingCenterWarehouseId(200L)
            .setDeliveryServiceId(10L)
            .setAvailabilityLocationId(213)
            .setAvailabilityPartnerLimit(1000)
            .setAvailabilityEnabled(true)
            .setReturnToDropoffEnabled(true)
            .setTmMovementSchedule(
                new AdminScheduleDto()
                    .setMondayFrom(LocalTime.of(13, 0))
                    .setMondayTo(LocalTime.of(18, 0))
                    .setTuesdayFrom(LocalTime.of(14, 0))
                    .setTuesdayTo(LocalTime.of(17, 0))
            )
            .setAvailabilitySchedule(
                new AdminScheduleDto()
                    .setMondayFrom(LocalTime.of(10, 0))
                    .setMondayTo(LocalTime.of(15, 0))
                    .setTuesdayFrom(LocalTime.of(11, 0))
                    .setTuesdayTo(LocalTime.of(16, 0))
            )
            .setMovementDurationHours(3);
    }

    private void verifyGetDropoffLogisticsPoint() {
        verify(lmsClient).getLogisticsPoint(DROPOFF_LOGISTIC_POINT_ID);
    }

    @Nonnull
    private static ValidationErrorData nullError(String field) {
        return AdminValidationUtils.createNullFieldError(field, "adminDropoffRegistrationNewDto");
    }

    @Nonnull
    private static ValidationErrorData adminScheduleLogisticPointNotWorkingScheduleError(String field) {
        return fieldError(
            field,
            "Указано расписание в нерабочий день точки сдачи",
            "adminDropoffRegistrationNewDto",
            "ValidAdminSchedule"
        );
    }

    @Nonnull
    private static ValidationErrorData adminScheduleLogisticPointWorkingScheduleError(String field) {
        return fieldError(
            field,
            "Не указано расписание в рабочий день точки сдачи",
            "adminDropoffRegistrationNewDto",
            "ValidAdminSchedule"
        );
    }

    @Nonnull
    private static ValidationErrorData adminScheduleError(String field, String errorMessage) {
        return fieldError(field, errorMessage, "adminDropoffRegistrationNewDto", "ValidAdminSchedule");
    }
}
