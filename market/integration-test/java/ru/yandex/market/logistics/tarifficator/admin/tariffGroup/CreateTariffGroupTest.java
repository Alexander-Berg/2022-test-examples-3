package ru.yandex.market.logistics.tarifficator.admin.tariffGroup;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

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
import ru.yandex.market.logistics.tarifficator.admin.controller.AdminTariffGroupController;
import ru.yandex.market.logistics.tarifficator.admin.dto.TariffGroupCreateDto;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;
import ru.yandex.market.logistics.tarifficator.util.ValidationUtil;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.tarifficator.util.TestUtils.PARAMETERIZED_TEST_DEFAULT_NAME;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.fieldValidationFrontError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Создание группы тарифов через админку")
class CreateTariffGroupTest extends AbstractContextualTest {

    @DisplayName("Создание группы тарифов через админку")
    @Test
    @ExpectedDatabase(
        value = "/controller/admin/tariffGroup/after/create_tariff_group.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void shouldReturnOk() throws Exception {
        performCreateTariffGroup(validRequest())
            .andExpect(status().isOk())
            .andExpect(content().string(Long.toString(1L)));
    }

    @DisplayName("Ошибки валидации")
    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @MethodSource
    void testValidationError(
        @SuppressWarnings("unused") String testName,
        TariffGroupCreateDto createDto,
        String fieldPath,
        String fieldError
    ) throws Exception {
        performCreateTariffGroup(createDto)
            .andExpect(status().isBadRequest())
            .andExpect(fieldValidationFrontError(fieldPath, fieldError));
    }

    @SuppressWarnings("unused")
    @Nonnull
    private static Stream<Arguments> testValidationError() {
        return Stream
            .of(
                Arguments.of(
                    "без описания",
                    validRequest().setDescription(null),
                    "description",
                    ValidationUtil.MUST_NOT_BE_BLANK
                ),
                Arguments.of(
                    "пустое описание",
                    validRequest().setDescription(""),
                    "description",
                    ValidationUtil.MUST_NOT_BE_BLANK
                ),
                Arguments.of(
                    "описание только с пробельными символами",
                    validRequest().setDescription("        "),
                    "description",
                    ValidationUtil.MUST_NOT_BE_BLANK
                )
            );
    }

    @Nonnull
    private ResultActions performCreateTariffGroup(TariffGroupCreateDto createDto) throws Exception {
        return mockMvc.perform(
            TestUtils.request(
                HttpMethod.POST,
                AdminTariffGroupController.PATH_ADMIN_TARIFF_GROUPS,
                createDto
            )
        );
    }

    @Nonnull
    private static TariffGroupCreateDto validRequest() {
        return new TariffGroupCreateDto()
            .setDescription("Описание группы");
    }
}
