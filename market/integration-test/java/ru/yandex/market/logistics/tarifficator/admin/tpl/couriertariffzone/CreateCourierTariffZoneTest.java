package ru.yandex.market.logistics.tarifficator.admin.tpl.couriertariffzone;

import java.lang.reflect.Field;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.admin.dto.tpl.CourierTariffZoneCreateDto;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.tarifficator.util.TestUtils.PARAMETERIZED_TEST_DEFAULT_NAME;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.NOT_BLANK_VALIDATION_INFO;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.createArgumentsForValidation;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.errorMessage;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.fieldValidationFrontError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup(
    value = "/controller/admin/tpl/couriertariffs/zones/db/before/search_prepare.xml",
    connection = "dbUnitQualifiedDatabaseConnection"
)
class CreateCourierTariffZoneTest extends AbstractContextualTest {

    @Test
    @DisplayName("Успешное создание тарифной зоны")
    @ExpectedDatabase(
        value = "/controller/admin/tpl/couriertariffs/zones/db/after/success_creation.xml",
        connection = "dbUnitQualifiedDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createTariffZoneSuccess() throws Exception {
        createCourierTariffZone(validRequest())
            .andExpect(status().isOk())
            .andExpect(content().string(Long.toString(3L)));
    }

    @Test
    @DisplayName("Создание тарифной зоны - зона с таким названием существует")
    @ExpectedDatabase(
        value = "/controller/admin/tpl/couriertariffs/zones/db/before/search_prepare.xml",
        connection = "dbUnitQualifiedDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createTariffZoneNameAlreadyExist() throws Exception {
        createCourierTariffZone(validRequest().setName("Москва"))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Тарифная зона с названием 'Москва' уже существует"));
    }

    @MethodSource("invalidRequestProvider")
    @DisplayName("Валидация параметров")
    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    void validateDto(
        @SuppressWarnings("unused") String testName,
        String fieldPath,
        String fieldError,
        CourierTariffZoneCreateDto courierTariffZoneCreateDto
    ) throws Exception {
        createCourierTariffZone(courierTariffZoneCreateDto)
            .andExpect(status().isBadRequest())
            .andExpect(fieldValidationFrontError(fieldPath, fieldError));
    }

    @Nonnull
    private static Stream<Arguments> invalidRequestProvider() {
        return Stream.of(
            createArgumentsForValidation(
                NOT_BLANK_VALIDATION_INFO,
                CourierTariffZoneCreateDto.class,
                CreateCourierTariffZoneTest::validRequest
            )
        )
            .flatMap(Function.identity());
    }

    @Nonnull
    private static CourierTariffZoneCreateDto validRequest() {
        return validRequest(null, null);
    }

    @Nonnull
    private static CourierTariffZoneCreateDto validRequest(@Nullable Field field, @Nullable Object value) {
        CourierTariffZoneCreateDto dto = new CourierTariffZoneCreateDto()
            .setName("Новосибирск")
            .setDescription("Описание Новосибирской зоны");
        TestUtils.setFieldValue(dto, field, value);
        return dto;
    }

    @Nonnull
    private ResultActions createCourierTariffZone(
        CourierTariffZoneCreateDto courierTariffZoneCreateDto
    ) throws Exception {
        return mockMvc.perform(
            TestUtils.request(HttpMethod.POST, "/admin/tpl-courier-tariffs/zones", courierTariffZoneCreateDto)
        );
    }
}
