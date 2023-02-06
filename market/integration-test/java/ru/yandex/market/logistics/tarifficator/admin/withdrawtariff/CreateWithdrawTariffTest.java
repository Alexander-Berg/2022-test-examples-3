package ru.yandex.market.logistics.tarifficator.admin.withdrawtariff;

import java.lang.reflect.Field;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

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
import ru.yandex.market.logistics.tarifficator.admin.dto.WithdrawTariffCreateDto;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.tarifficator.util.TestUtils.PARAMETERIZED_TEST_DEFAULT_NAME;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.NOT_BLANK_VALIDATION_INFO;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.NULL_VALIDATION_INFO;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.createArgumentsForValidation;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.fieldValidationFrontError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("Создание заборного тарифа через админку")
class CreateWithdrawTariffTest extends AbstractContextualTest {

    @Test
    @DisplayName("Успешное создание тарифа")
    @ExpectedDatabase(
        value = "/controller/admin/withdrawtariffs/db/after/tariff_create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createWithdrawTariffSuccess() throws Exception {
        createWithdrawTariff(validRequest(null, null))
            .andExpect(status().isOk())
            .andExpect(content().string(Long.toString(1L)));
    }

    @DisplayName("Валидация запроса на создание заборного тарифа")
    @MethodSource("invalidRequestProvider")
    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    void validateDto(
        @SuppressWarnings("unused") String testName,
        String fieldPath,
        String fieldError,
        WithdrawTariffCreateDto tariffCreateDto
    ) throws Exception {
        createWithdrawTariff(tariffCreateDto)
            .andExpect(fieldValidationFrontError(fieldPath, fieldError));
    }

    @Nonnull
    private static Stream<Arguments> invalidRequestProvider() {
        return Stream.of(
            createArgumentsForValidation(
                NULL_VALIDATION_INFO,
                WithdrawTariffCreateDto.class,
                CreateWithdrawTariffTest::validRequest
            ),
            createArgumentsForValidation(
                NOT_BLANK_VALIDATION_INFO,
                WithdrawTariffCreateDto.class,
                CreateWithdrawTariffTest::validRequest
            )
        )
            .flatMap(Function.identity());
    }

    @Nonnull
    private static WithdrawTariffCreateDto validRequest(@Nullable Field field, @Nullable Object value) {
        WithdrawTariffCreateDto dto = new WithdrawTariffCreateDto()
            .setLocationId(197L)
            .setName("Тариф Барнаул")
            .setDescription("Только для Барнаула")
            .setEnabled(true);
        TestUtils.setFieldValue(dto, field, value);
        return dto;
    }

    @Nonnull
    private ResultActions createWithdrawTariff(WithdrawTariffCreateDto tariffCreateDto) throws Exception {
        return mockMvc.perform(TestUtils.request(HttpMethod.POST, "/admin/withdraw-tariffs", tariffCreateDto));
    }
}
