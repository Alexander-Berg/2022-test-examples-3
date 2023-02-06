package ru.yandex.market.logistics.nesu.admin;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

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

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.admin.model.response.sender.SenderDto;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

@DatabaseSetup("/repository/sender/before/sender.xml")
class SenderUpdateTest extends AbstractContextualTest {
    @Test
    @DisplayName("Обновить имя и адрес сайта")
    @ExpectedDatabase(
        value = "/repository/sender/after/name_and_site_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void updateName() throws Exception {
        SenderDto request = SenderDto.builder()
            .senderName("тест")
            .senderSiteUrl("test.com")
            .contactEmail("1@dostavkin.com")
            .build();

        update(40001, request)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/sender-update/name_and_site_update.json"));
    }

    @Test
    @DisplayName("Обновить email")
    @ExpectedDatabase(
        value = "/repository/sender/after/emails_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateEmail() throws Exception {
        SenderDto request = SenderDto.builder()
            .senderName("Пикник")
            .senderSiteUrl("pick-nick.ru")
            .contactEmail("test1@test.com, test2@test.com, test3@test.com")
            .build();

        update(40001, request)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/sender-update/email_update.json"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("updateFailArguments")
    @DisplayName("Невалидные данные")
    void updateFail(String caseName, SenderDto request, ValidationErrorData error) throws Exception {
        update(40001, request)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(error));
    }

    @Nonnull
    private static Stream<Arguments> updateFailArguments() {
        return Stream.of(
            Arguments.of(
                "Пустое имя",
                SenderDto.builder()
                    .senderName(" ")
                    .contactEmail("test@test.ru")
                    .build(),
                fieldError(
                    "senderName",
                    "Обязательно для заполнения",
                    "senderUpdateDto",
                    "NotBlank"
                )
            ),
            Arguments.of(
                "Пустой email",
                SenderDto.builder()
                    .senderName("test")
                    .contactEmail(" ")
                    .build(),
                fieldError(
                    "contactEmail",
                    "Обязательно для заполнения",
                    "senderUpdateDto",
                    "NotNull"
                )
            ),
            Arguments.of(
                "Невалидный email",
                SenderDto.builder()
                    .senderName("test")
                    .contactEmail("wrong")
                    .build(),
                fieldError(
                    "contactEmail",
                    "must be a well-formed email address",
                    "senderUpdateDto",
                    "ValidEmails"
                )
            )
        );
    }

    @Test
    @DisplayName("Сендер не найден")
    void senderNotFound() throws Exception {
        SenderDto request = SenderDto.builder()
            .senderName("тест")
            .senderSiteUrl("test.com")
            .contactEmail("1@dostavkin.com")
            .build();

        update(10, request)
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/admin/sender-get/not_found.json"));
    }

    @Nonnull
    private ResultActions update(long senderId, SenderDto request) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/admin/senders/" + senderId, request));
    }
}
