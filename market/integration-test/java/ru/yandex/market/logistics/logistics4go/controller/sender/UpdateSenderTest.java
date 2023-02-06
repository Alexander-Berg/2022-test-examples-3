package ru.yandex.market.logistics.logistics4go.controller.sender;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.logistics4go.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4go.client.api.SendersApi;
import ru.yandex.market.logistics.logistics4go.client.model.ErrorType;
import ru.yandex.market.logistics.logistics4go.client.model.NotFoundError;
import ru.yandex.market.logistics.logistics4go.client.model.PersonName;
import ru.yandex.market.logistics.logistics4go.client.model.Phone;
import ru.yandex.market.logistics.logistics4go.client.model.ResourceType;
import ru.yandex.market.logistics.logistics4go.client.model.SenderContactDto;
import ru.yandex.market.logistics.logistics4go.client.model.UpdateSenderRequest;
import ru.yandex.market.logistics.logistics4go.client.model.ValidationError;
import ru.yandex.market.logistics.logistics4go.client.model.ValidationViolation;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Обновление магазина")
@ParametersAreNonnullByDefault
class UpdateSenderTest extends AbstractIntegrationTest {
    private static final long SENDER_ID = 1;
    private static final long MISSING_SENDER_ID = 2;

    @Test
    @DisplayName("Обновление магазина, все поля заполнены")
    @DatabaseSetup("/controller/sender/update/before/sender.xml")
    @ExpectedDatabase(
        value = "/controller/sender/update/after/all_fields.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void updateAllFields() {
        UpdateSenderRequest request = new UpdateSenderRequest()
            .name("new-sender-name")
            .siteUrl("new-sender-site-url")
            .contact(
                new SenderContactDto()
                    .name(
                        new PersonName()
                            .lastName("new-sender-last-name")
                            .firstName("new-sender-first-name")
                            .middleName("new-sender-middle-name")
                    )
                    .email("new-sender-email")
                    .phone(new Phone().number("new-sender-phone-number").extension("new-sender-additional-number"))
            );

        updateSender(SENDER_ID, request)
            .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    @DisplayName("Обновление магазина, только необходимые поля заполнены")
    @DatabaseSetup("/controller/sender/update/before/sender.xml")
    @ExpectedDatabase(
        value = "/controller/sender/update/after/required_fields.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createRequiredFields() {
        updateSender(SENDER_ID, getSenderWithRequiredFields())
            .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    @DisplayName("Обновление несуществующего магазина")
    void senderNotFound() {
        NotFoundError error = updateSender(MISSING_SENDER_ID, getSenderWithRequiredFields())
            .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
            .as(NotFoundError.class);

        softly.assertThat(error).isEqualTo(
            new NotFoundError()
                .code(ErrorType.RESOURCE_NOT_FOUND)
                .addIdsItem(MISSING_SENDER_ID)
                .resourceType(ResourceType.SENDER)
                .message("Failed to find SENDER with ids [" + MISSING_SENDER_ID + "]")
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Валидация запроса на обновление")
    void requestValidation(
        @SuppressWarnings("unused") String displayName,
        UpdateSenderRequest request,
        List<ValidationViolation> violations
    ) {
        ValidationError response = updateSender(SENDER_ID, request)
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);

        softly.assertThat(response.getErrors())
            .containsExactlyInAnyOrderElementsOf(violations);
    }

    @Nonnull
    static Stream<Arguments> requestValidation() {
        return Stream.of(
            Arguments.of(
                "Контакт, имя и сайт отсутствуют",
                new UpdateSenderRequest(),
                List.of(
                    new ValidationViolation().field("contact").message("must not be null"),
                    new ValidationViolation().field("name").message("must not be null"),
                    new ValidationViolation().field("siteUrl").message("must not be null")
                )
            ),
            Arguments.of(
                "Контакт присутствует, но не заполнен",
                new UpdateSenderRequest()
                    .name("sender-name")
                    .siteUrl("sender-site-url")
                    .contact(new SenderContactDto()),
                List.of(
                    new ValidationViolation().field("contact.name").message("must not be null"),
                    new ValidationViolation().field("contact.email").message("must not be null"),
                    new ValidationViolation().field("contact.phone").message("must not be null")
                )
            )
        );
    }

    @Nonnull
    private SendersApi.UpdateSenderOper updateSender(long senderId, UpdateSenderRequest request) {
        return apiClient.senders().updateSender().senderIdPath(senderId).body(request);
    }

    private UpdateSenderRequest getSenderWithRequiredFields() {
        return new UpdateSenderRequest()
            .name("new-sender-name")
            .siteUrl("new-sender-site-url")
            .contact(
                new SenderContactDto()
                    .name(
                        new PersonName()
                            .lastName("new-sender-last-name")
                            .firstName("new-sender-first-name")
                    )
                    .email("new-sender-email")
                    .phone(new Phone().number("new-sender-phone-number"))
            );
    }
}
