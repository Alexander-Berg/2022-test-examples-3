package ru.yandex.market.logistics.logistics4go.controller.order;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.logistics4go.client.model.ApiError;
import ru.yandex.market.logistics.logistics4go.client.model.ChangeOrderRequestDto;
import ru.yandex.market.logistics.logistics4go.client.model.ChangeRequestStatus;
import ru.yandex.market.logistics.logistics4go.client.model.ChangeRequestType;
import ru.yandex.market.logistics.logistics4go.client.model.ErrorType;
import ru.yandex.market.logistics.logistics4go.client.model.UpdateRecipientRequest;
import ru.yandex.market.logistics.logistics4go.client.model.ValidationError;
import ru.yandex.market.logistics.logistics4go.client.model.ValidationViolation;
import ru.yandex.market.logistics.logistics4go.utils.LomFactory;
import ru.yandex.market.logistics.logistics4go.utils.OrderFactory;
import ru.yandex.market.logistics.lom.model.dto.UpdateOrderRecipientRequestDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.validatedWith;
import static ru.yandex.market.logistics.logistics4go.utils.OrderFactory.modifier;
import static ru.yandex.market.logistics.logistics4go.utils.PersonalDataFactory.createRecipientStoreRequest;
import static ru.yandex.market.logistics.logistics4go.utils.PersonalDataFactory.createRecipientStoreResponse;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Обновление информации о получателе заказа")
class UpdateRecipientTest extends AbstractOrderTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Валидация запроса")
    void validation(
        String caseName,
        UnaryOperator<UpdateRecipientRequest> requestModifier,
        ValidationViolation expectedViolation
    ) {
        UpdateRecipientRequest request = createRequest();
        ValidationError actualValidationError = apiClient.orders().updateRecipient()
            .orderIdPath(1L)
            .body(requestModifier.apply(request))
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);
        softly.assertThat(actualValidationError.getCode()).isEqualTo(ErrorType.VALIDATION_ERROR);
        softly.assertThat(actualValidationError.getErrors())
            .containsExactly(expectedViolation);
    }

    @Test
    @DisplayName("Успешное обновление получателя")
    @SneakyThrows
    void updateSuccess() {
        UpdateOrderRecipientRequestDto lomRequest = objectMapper.readValue(
            extractFileContent("controller/order/update_recipient/use_only_unencrypted_data.json"),
            UpdateOrderRecipientRequestDto.class
        );
        updateRecipient(lomRequest);
    }

    @Test
    @DisplayName("Успешное обновление получателя с зашифрованными персональными данными")
    @DatabaseSetup("/controller/order/create/before/use_encrypted_personal_data.xml")
    @SneakyThrows
    void updateSuccessEncryptedPersonalData() {
        doReturn(createRecipientStoreResponse(false))
            .when(personalDataStoreApi).v1MultiTypesStorePost(createRecipientStoreRequest(false));
        UpdateOrderRecipientRequestDto lomRequest = objectMapper.readValue(
            extractFileContent("controller/order/update_recipient/use_encrypted_data.json"),
            UpdateOrderRecipientRequestDto.class
        );
        updateRecipient(lomRequest);
        verify(personalDataStoreApi).v1MultiTypesStorePost(createRecipientStoreRequest(false));
    }

    @Test
    @DisplayName("Успешное обновление получателя только с зашифрованными персональными данными")
    @DatabaseSetup("/controller/order/create/before/use_only_encrypted_personal_data.xml")
    @SneakyThrows
    void updateSuccessOnlyEncryptedPersonalData() {
        doReturn(createRecipientStoreResponse(false))
            .when(personalDataStoreApi).v1MultiTypesStorePost(createRecipientStoreRequest(false));
        UpdateOrderRecipientRequestDto lomRequest = objectMapper.readValue(
            extractFileContent("controller/order/update_recipient/use_only_encrypted_data.json"),
            UpdateOrderRecipientRequestDto.class
        );
        updateRecipient(lomRequest);
        verify(personalDataStoreApi).v1MultiTypesStorePost(createRecipientStoreRequest(false));
    }

    @Test
    @DisplayName("Обновление получателя недоступно, есть активная заявка")
    void updateRecipientUnavailable() {
        mockSearchLomOrder(1L, LomFactory.orderUpdateRecipientUnavailable());

        ApiError error = apiClient.orders().updateRecipient()
            .orderIdPath(1L)
            .body(createRequest())
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ApiError.class);

        softly.assertThat(error).isEqualTo(
            new ApiError()
                .code(ErrorType.OPERATION_NOT_ALLOWED)
                .message("Order has active 'RECIPIENT' change request")
        );

        verifySearchLomOrder(1L);
    }

    @Nonnull
    private static Stream<Arguments> validation() {
        return StreamEx.of(violations())
            .mapToEntry(Pair::getRight)
            .mapValues(v -> "%s %s".formatted(v.getField(), v.getMessage()))
            .mapKeyValue((pair, caseName) -> Arguments.of(caseName, pair.getLeft(), pair.getRight()));
    }

    @Nonnull
    private static Stream<Pair<UnaryOperator<UpdateRecipientRequest>, ValidationViolation>> violations() {
        return Stream.of(
            Pair.of(
                r -> r.recipient(null),
                new ValidationViolation().field("recipient").message("must not be null")
            ),
            Pair.of(
                modifier(UpdateRecipientRequest::getRecipient, r -> r.name(null), UpdateRecipientRequest.class),
                new ValidationViolation().field("recipient.name").message("must not be null")
            ),
            Pair.of(
                modifier(r -> r.getRecipient().getName(), r -> r.lastName(null), UpdateRecipientRequest.class),
                new ValidationViolation().field("recipient.name.lastName").message("must not be null")
            ),
            Pair.of(
                modifier(r -> r.getRecipient().getName(), r -> r.lastName(""), UpdateRecipientRequest.class),
                new ValidationViolation().field("recipient.name.lastName").message("size must be between 1 and 100")
            ),
            Pair.of(
                modifier(
                    r -> r.getRecipient().getName(),
                    r -> r.lastName("a".repeat(101)),
                    UpdateRecipientRequest.class
                ),
                new ValidationViolation().field("recipient.name.lastName").message("size must be between 1 and 100")
            ),
            Pair.of(
                modifier(r -> r.getRecipient().getName(), r -> r.firstName(null), UpdateRecipientRequest.class),
                new ValidationViolation().field("recipient.name.firstName").message("must not be null")
            ),
            Pair.of(
                modifier(r -> r.getRecipient().getName(), r -> r.firstName(""), UpdateRecipientRequest.class),
                new ValidationViolation().field("recipient.name.firstName").message("size must be between 1 and 100")
            ),
            Pair.of(
                modifier(
                    r -> r.getRecipient().getName(),
                    r -> r.firstName("a".repeat(101)),
                    UpdateRecipientRequest.class
                ),
                new ValidationViolation().field("recipient.name.firstName").message("size must be between 1 and 100")
            ),
            Pair.of(
                modifier(r -> r.getRecipient().getName(), r -> r.middleName(""), UpdateRecipientRequest.class),
                new ValidationViolation().field("recipient.name.middleName").message("size must be between 1 and 100")
            ),
            Pair.of(
                modifier(
                    r -> r.getRecipient().getName(),
                    r -> r.middleName("a".repeat(101)),
                    UpdateRecipientRequest.class
                ),
                new ValidationViolation().field("recipient.name.middleName").message("size must be between 1 and 100")
            ),
            Pair.of(
                modifier(r -> r.getRecipient().getPhone(), r -> r.number(null), UpdateRecipientRequest.class),
                new ValidationViolation().field("recipient.phone.number").message("must not be null")
            )
        );
    }

    private void updateRecipient(UpdateOrderRecipientRequestDto lomRequest) {
        mockSearchLomOrder(1L, LomFactory.order(false));

        doReturn(
            ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto.builder()
                .id(1L)
                .requestType(ChangeOrderRequestType.RECIPIENT)
                .status(ChangeOrderRequestStatus.PROCESSING)
                .build()
        )
            .when(lomClient).updateOrderRecipient(lomRequest);

        ChangeOrderRequestDto response = apiClient.orders().updateRecipient()
            .orderIdPath(1L)
            .body(createRequest())
            .execute(validatedWith(shouldBeCode(SC_OK)))
            .as(ru.yandex.market.logistics.logistics4go.client.model.ChangeOrderRequestDto.class);

        softly.assertThat(response).isEqualTo(
            new ChangeOrderRequestDto()
                .id(1L)
                .requestType(ChangeRequestType.RECIPIENT)
                .status(ChangeRequestStatus.CREATED)
        );

        verifySearchLomOrder(1L);
        verify(lomClient).updateOrderRecipient(lomRequest);
    }

    @Nonnull
    private UpdateRecipientRequest createRequest() {
        return new UpdateRecipientRequest()
            .recipient(OrderFactory.recipient(false));
    }
}
