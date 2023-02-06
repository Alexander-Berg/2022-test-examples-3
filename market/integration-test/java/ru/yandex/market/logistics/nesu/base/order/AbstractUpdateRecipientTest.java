package ru.yandex.market.logistics.nesu.base.order;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestPayloadDto;
import ru.yandex.market.logistics.lom.model.dto.OrderContactDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.UpdateOrderRecipientRequestDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.lom.model.enums.ContactType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.dto.order.OrderUpdateRecipientRequest;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup({
    "/repository/settings/delivery_type_service_settings.xml",
    "/repository/order/database_order_prepare.xml",
})
public abstract class AbstractUpdateRecipientTest extends AbstractContextualTest {
    protected static final long ORDER_ID = 100L;
    protected static final long SENDER_ID = 1L;
    protected static final long PLATFORM_CLIENT_ID = 3L;

    private static final String FIRST_NAME = "Иван";
    private static final String LAST_NAME = "Иванов";
    private static final String MIDDLE_NAME = "Иванович";
    private static final String EMAIL = "test@test.ru";
    private static final String PHONE = "12345";
    private static final String ADDITIONAL = "67890";

    @Autowired
    protected LomClient lomClient;

    @BeforeEach
    void setup() {
        doReturn(Optional.of(defaultOrder()))
            .when(lomClient).getOrder(ORDER_ID, Set.of());
    }

    @AfterEach
    void checkNoMoreInteractions() {
        verifyNoMoreInteractions(lomClient);
    }

    @Test
    @DisplayName("Неизвестный заказ")
    void unknownOrder() throws Exception {
        updateRecipient(ORDER_ID + 1, defaultUpdateRecipientRequest())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER] with ids [101]"));

        verify(lomClient).getOrder(ORDER_ID + 1, Set.of());
    }

    @Test
    @DisplayName("Успешное обновление данных получателя")
    void success() throws Exception {
        doReturn(createExpectedResponse(Set.of(createPayload())))
            .when(lomClient).updateOrderRecipient(defaultUpdateOrderRecipientRequestDto());

        updateRecipient(ORDER_ID, defaultUpdateRecipientRequest())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/update-recipient/success.json"));

        verify(lomClient).getOrder(ORDER_ID, Set.of());
        verify(lomClient).updateOrderRecipient(defaultUpdateOrderRecipientRequestDto());
    }

    @Test
    @DisplayName("Неправильный идентификатор клиента платформы")
    void wrongPlatformClientId() throws Exception {
        doReturn(Optional.of(defaultOrder().setPlatformClientId(PLATFORM_CLIENT_ID + 1)))
            .when(lomClient).getOrder(ORDER_ID, Set.of());

        updateRecipient(ORDER_ID, defaultUpdateRecipientRequest())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER] with ids [100]"));

        verify(lomClient).getOrder(ORDER_ID, Set.of());
    }

    @Test
    @DisplayName("Неизвестный сендер")
    void unknownSender() throws Exception {
        doReturn(Optional.of(defaultOrder().setSenderId(-1L)))
            .when(lomClient).getOrder(ORDER_ID, Set.of());

        updateRecipient(ORDER_ID, defaultUpdateRecipientRequest())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SENDER] with ids [-1]"));

        verify(lomClient).getOrder(ORDER_ID, Set.of());
    }

    @Test
    @DisplayName("Отсутствует payload")
    void payloadNotExist() throws Exception {
        doReturn(createExpectedResponse(Set.of()))
            .when(lomClient).updateOrderRecipient(defaultUpdateOrderRecipientRequestDto());

        updateRecipient(ORDER_ID, defaultUpdateRecipientRequest())
            .andExpect(status().isInternalServerError())
            .andExpect(errorMessage("Cannot find payload for changeOrderRequest: id=1"));

        verify(lomClient).getOrder(ORDER_ID, Set.of());
        verify(lomClient).updateOrderRecipient(defaultUpdateOrderRecipientRequestDto());
    }

    @Test
    @DisplayName("Payload некорректного вида")
    void payloadIsIncorrect() throws Exception {
        doReturn(createExpectedResponse(Set.of(createIncorrectPayload())))
            .when(lomClient).updateOrderRecipient(defaultUpdateOrderRecipientRequestDto());

        updateRecipient(ORDER_ID, defaultUpdateRecipientRequest())
            .andExpect(status().isInternalServerError())
            .andExpect(errorMessage("Exception while parsing payload for changeOrderRequest: id=1"));

        verify(lomClient).getOrder(ORDER_ID, Set.of());
        verify(lomClient).updateOrderRecipient(defaultUpdateOrderRecipientRequestDto());
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("updateRequestFailArguments")
    @DisplayName("Проверка наличия валидации тела запроса")
    void checkFieldsValidation(String caseName, OrderUpdateRecipientRequest updateRequest, ValidationErrorData error)
        throws Exception {
        updateRecipient(ORDER_ID, updateRequest)
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(validationErrorMatcher(error));
    }

    @Nonnull
    private static Stream<Arguments> updateRequestFailArguments() {
        return Stream.of(
            Arguments.of(
                "Пустое имя",
                updateRecipientRequestBuilder().firstName("").build(),
                fieldError(
                    "firstName",
                    "must not be blank",
                    "orderUpdateRecipientRequest",
                    "NotBlank"
                )
            ),
            Arguments.of(
                "Пустая фамилия",
                updateRecipientRequestBuilder().lastName("").build(),
                fieldError(
                    "lastName",
                    "must not be blank",
                    "orderUpdateRecipientRequest",
                    "NotBlank"
                )
            ),
            Arguments.of(
                "Пустой номер телефона",
                updateRecipientRequestBuilder().phone(null).build(),
                fieldError(
                    "phone",
                    "must not be blank",
                    "orderUpdateRecipientRequest",
                    "NotBlank"
                )
            ),
            Arguments.of(
                "Невалидный email",
                updateRecipientRequestBuilder().email("invalid email").build(),
                fieldError(
                    "email",
                    "must be a well-formed email address",
                    "orderUpdateRecipientRequest",
                    "Email",
                    Map.of("regexp", ".*")
                )
            )
        );
    }

    @Nonnull
    private OrderDto defaultOrder() {
        return new OrderDto()
            .setId(ORDER_ID)
            .setSenderId(SENDER_ID)
            .setPlatformClientId(PLATFORM_CLIENT_ID);
    }

    @Nonnull
    protected static OrderUpdateRecipientRequest.OrderUpdateRecipientRequestBuilder updateRecipientRequestBuilder() {
        return OrderUpdateRecipientRequest.builder()
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .middleName(MIDDLE_NAME)
            .email(EMAIL)
            .phone(PHONE)
            .additional(ADDITIONAL);
    }

    @Nonnull
    protected OrderUpdateRecipientRequest defaultUpdateRecipientRequest() {
        return updateRecipientRequestBuilder().build();
    }

    @Nonnull
    private UpdateOrderRecipientRequestDto defaultUpdateOrderRecipientRequestDto() {
        return UpdateOrderRecipientRequestDto.builder()
            .email(defaultUpdateRecipientRequest().getEmail())
            .contact(defaultOrderContactDto())
            .build();
    }

    @Nonnull
    private OrderContactDto defaultOrderContactDto() {
        OrderUpdateRecipientRequest updateRequest = defaultUpdateRecipientRequest();
        return OrderContactDto.builder()
            .firstName(updateRequest.getFirstName())
            .lastName(updateRequest.getLastName())
            .middleName(updateRequest.getMiddleName())
            .phone(updateRequest.getPhone())
            .extension(updateRequest.getAdditional())
            .contactType(ContactType.RECIPIENT)
            .build();
    }

    @Nonnull
    private ChangeOrderRequestDto createExpectedResponse(Set<ChangeOrderRequestPayloadDto> payloads) {
        return ChangeOrderRequestDto.builder()
            .id(1L)
            .requestType(ChangeOrderRequestType.RECIPIENT)
            .status(ChangeOrderRequestStatus.PROCESSING)
            .payloads(payloads)
            .build();
    }

    @Nonnull
    private ChangeOrderRequestPayloadDto createPayload() {
        return ChangeOrderRequestPayloadDto.builder()
            .status(ChangeOrderRequestStatus.INFO_RECEIVED)
            .payload(objectMapper.convertValue(defaultUpdateOrderRecipientRequestDto(), JsonNode.class))
            .build();
    }

    @Nonnull
    private ChangeOrderRequestPayloadDto createIncorrectPayload() throws IOException {
        return ChangeOrderRequestPayloadDto.builder()
            .status(ChangeOrderRequestStatus.INFO_RECEIVED)
            .payload(objectMapper.readTree("{\"barcode\": {}}"))
            .build();
    }

    @Nonnull
    protected abstract ResultActions updateRecipient(long orderId, OrderUpdateRecipientRequest updateRequest)
        throws Exception;
}
