package ru.yandex.market.logistics.logistics4go.controller.sender;

import java.util.List;
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

import ru.yandex.market.logistics.logistics4go.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4go.client.api.SendersApi;
import ru.yandex.market.logistics.logistics4go.client.model.CreateSenderResponse;
import ru.yandex.market.logistics.logistics4go.client.model.PersonName;
import ru.yandex.market.logistics.logistics4go.client.model.Phone;
import ru.yandex.market.logistics.logistics4go.client.model.SenderContactDto;
import ru.yandex.market.logistics.logistics4go.client.model.SenderDto;
import ru.yandex.market.logistics.logistics4go.client.model.ValidationError;
import ru.yandex.market.logistics.logistics4go.client.model.ValidationViolation;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.CreatePartnerDto;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PlatformClientPartnerDto;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Создание магазина")
@ParametersAreNonnullByDefault
class CreateSenderTest extends AbstractIntegrationTest {
    private static final long CREATED_SENDER_ID = 1;
    private static final String SENDER_NAME = "sender-name";
    private static final long PARTNER_ID = 100;
    private static final long MARKET_ID = 2014152L;
    private static final long YANDEX_GO_PLATFORM_CLIENT_ID = 6;

    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void setup() {
        CreatePartnerDto request = CreatePartnerDto.newBuilder()
            .partnerType(PartnerType.YANDEX_GO_SHOP)
            .name(SENDER_NAME)
            .readableName(SENDER_NAME)
            .marketId(MARKET_ID)
            .build();

        PartnerResponse response = PartnerResponse.newBuilder()
            .id(PARTNER_ID)
            .name(SENDER_NAME)
            .readableName(SENDER_NAME)
            .marketId(MARKET_ID)
            .build();

        when(lmsClient.createPartner(request)).thenReturn(response);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Создание магазина, все поля заполнены")
    @ExpectedDatabase(
        value = "/controller/sender/create/after/all_fields.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createAllFields() {
        SenderDto senderDto = new SenderDto()
            .name(SENDER_NAME)
            .externalId("sender-external-id")
            .siteUrl("sender-site-url")
            .contact(
                new SenderContactDto()
                    .name(
                        new PersonName()
                            .lastName("sender-last-name")
                            .firstName("sender-first-name")
                            .middleName("sender-middle-name")
                    )
                    .email("sender-email")
                    .phone(new Phone().number("+79998887766").extension("sender-additional-number"))
            );

        CreateSenderResponse response = createSender(senderDto)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertCreatedId(response);
        verifyLms();
    }

    @Test
    @DisplayName("Создание магазина, только необходимые поля заполнены")
    @ExpectedDatabase(
        value = "/controller/sender/create/after/required_fields.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createRequiredFields() {
        SenderDto senderDto = new SenderDto()
            .name(SENDER_NAME)
            .siteUrl("sender-site-url")
            .contact(
                new SenderContactDto()
                    .name(
                        new PersonName()
                            .lastName("sender-last-name")
                            .firstName("sender-first-name")
                    )
                    .email("sender-email")
                    .phone(new Phone().number("+79998887766"))
            );

        CreateSenderResponse response = createSender(senderDto)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertCreatedId(response);
        verifyLms();
    }

    @Test
    @DisplayName("Создание магазина с существующим externalId")
    @DatabaseSetup("/controller/sender/create/before/sender.xml")
    @ExpectedDatabase(
        value = "/controller/sender/create/after/all_fields.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void existedExternalId() {
        SenderDto senderDto = new SenderDto()
            .name(SENDER_NAME)
            .externalId("sender-external-id")
            .siteUrl("sender-site-url")
            .contact(
                new SenderContactDto()
                    .name(
                        new PersonName()
                            .lastName("sender-last-name")
                            .firstName("sender-first-name")
                            .middleName("sender-middle-name")
                    )
                    .email("sender-email")
                    .phone(new Phone().number("+79998887766").extension("sender-additional-number"))
            );

        CreateSenderResponse response = createSender(senderDto)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertCreatedId(response);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Валидация запроса на создание")
    void requestValidation(
        @SuppressWarnings("unused") String displayName,
        SenderDto request,
        List<ValidationViolation> violations
    ) {
        ValidationError response = createSender(request)
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
                new SenderDto(),
                List.of(
                    new ValidationViolation().field("name").message("must not be null"),
                    new ValidationViolation().field("contact").message("must not be null"),
                    new ValidationViolation().field("siteUrl").message("must not be null")
                )
            ),
            Arguments.of(
                "Контакт присутствует, но не заполнен",
                new SenderDto().name(SENDER_NAME).siteUrl("sender-site-url").contact(new SenderContactDto()),
                List.of(
                    new ValidationViolation().field("contact.name").message("must not be null"),
                    new ValidationViolation().field("contact.email").message("must not be null"),
                    new ValidationViolation().field("contact.phone").message("must not be null")
                )
            ),
            Arguments.of(
                "Невалидный телефон",
                new SenderDto()
                    .name(SENDER_NAME)
                    .siteUrl("sender-site-url")
                    .contact(
                        new SenderContactDto()
                            .name(
                                new PersonName()
                                    .lastName("sender-last-name")
                                    .firstName("sender-first-name")
                                    .middleName("sender-middle-name")
                            )
                            .email("sender-email")
                            .phone(new Phone().number("invalid_phone_number"))
                    ),
                List.of(new ValidationViolation().field("contact.phone.number").message("invalid phone number"))
            )
        );
    }

    @Nonnull
    private SendersApi.CreateSenderOper createSender(SenderDto senderDto) {
        return apiClient.senders().createSender().body(senderDto);
    }

    private void assertCreatedId(@Nullable CreateSenderResponse response) {
        CreateSenderResponse expected = new CreateSenderResponse()
            .id(CREATED_SENDER_ID)
            .partnerId(PARTNER_ID);
        softly.assertThat(response)
            .isNotNull()
            .isEqualTo(expected);
    }

    private void verifyLms() {
        verify(lmsClient).createPartner(any());
        verify(lmsClient).changePartnerStatus(PARTNER_ID, PartnerStatus.ACTIVE);

        PlatformClientPartnerDto createPlatformClientPartner = PlatformClientPartnerDto.newBuilder()
            .partnerId(PARTNER_ID)
            .platformClientId(YANDEX_GO_PLATFORM_CLIENT_ID)
            .status(PartnerStatus.ACTIVE)
            .build();
        verify(lmsClient).addOrUpdatePlatformClientPartner(createPlatformClientPartner);
    }
}
