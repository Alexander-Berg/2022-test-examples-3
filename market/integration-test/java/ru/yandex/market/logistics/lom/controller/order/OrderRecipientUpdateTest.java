package ru.yandex.market.logistics.lom.controller.order;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
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
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.model.dto.OrderContactDto;
import ru.yandex.market.logistics.lom.model.dto.UpdateOrderRecipientRequestDto;
import ru.yandex.market.logistics.lom.model.enums.ContactType;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static ru.yandex.market.logistics.lom.utils.TestUtils.INCORRECT_EMAIL_ERROR_MESSAGE;
import static ru.yandex.market.logistics.lom.utils.TestUtils.NOT_NULL_ERROR_MESSAGE;
import static ru.yandex.market.logistics.lom.utils.TestUtils.fieldValidationErrorMatcher;
import static ru.yandex.market.logistics.lom.utils.TestUtils.objectValidationErrorMatcher;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Изменение данных получателя")
@ParametersAreNonnullByDefault
@DatabaseSetup("/controller/order/recipient/before/setup.xml")
class OrderRecipientUpdateTest extends AbstractContextualTest {
    private static final ChangeOrderRequestPayload PAYLOAD = PayloadFactory.createChangeOrderRequestPayload(
        1,
        "1",
        1
    );

    @Test
    @DisplayName("Успешный запрос на обновление по идентификатору заказа")
    @ExpectedDatabase(
        value = "/controller/order/recipient/after/change_request_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateRecipientSuccessByOrderId() throws Exception {
        performRequest(validRequestBuilder().barcode(null).build())
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/update_recipient/update_recipient_by_order_id_response.json",
                "created",
                "updated"
            ));
        queueTaskChecker.assertQueueTaskCreated(QueueType.CHANGE_ORDER_REQUEST, PAYLOAD);
    }

    @Test
    @DisplayName("Успешный запрос на обновление по штрихкоду")
    @ExpectedDatabase(
        value = "/controller/order/recipient/after/change_request_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateRecipientSuccessByBarcode() throws Exception {
        performRequest(validRequestBuilder().orderId(null).build())
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/update_recipient/update_recipient_by_barcode_response.json",
                "created",
                "updated"
            ));
        queueTaskChecker.assertQueueTaskCreated(QueueType.CHANGE_ORDER_REQUEST, PAYLOAD);
    }

    @Test
    @DisplayName("Повторный запрос")
    @DatabaseSetup(
        value = "/controller/order/recipient/after/change_request_created.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/order/recipient/after/change_request_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateRecipientDuplicate() throws Exception {
        performRequest(validRequestBuilder().build())
            .andExpect(status().isConflict())
            .andExpect(errorMessage(
                "Active Change Request with type = RECIPIENT is already exists for order 1001"
            ));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("validateRequestArguments")
    @DisplayName("Невалидное тело запроса")
    void testRequestBodyInvalid(
        String displayName,
        UpdateOrderRecipientRequestDto.UpdateOrderRecipientRequestDtoBuilder requestBuilder,
        ResultMatcher resultMatcher
    ) throws Exception {
        performRequest(requestBuilder.build())
            .andExpect(status().isBadRequest())
            .andExpect(resultMatcher);
        assertOrderHistoryNeverChanged(1001L);
    }

    private static Stream<Arguments> validateRequestArguments() {
        return Stream.of(
            Arguments.of(
                "orderId и barcode заказа не указаны",
                validRequestBuilder().barcode(null).orderId(null),
                objectValidationErrorMatcher(
                    "updateOrderRecipientRequestDto",
                    "at least one field must not be null: [orderId, barcode]"
                )
            ),
            Arguments.of(
                "email получателя некорректный",
                validRequestBuilder().email("incorrect email"),
                fieldValidationErrorMatcher("email", INCORRECT_EMAIL_ERROR_MESSAGE)
            ),
            Arguments.of(
                "contact получателя не указан",
                validRequestBuilder().contact(null),
                fieldValidationErrorMatcher("contact", NOT_NULL_ERROR_MESSAGE)
            ),
            Arguments.of(
                "firstName получателя не указан",
                validRequestBuilder().contact(validOrderContactBuilder()
                    .firstName(null)
                    .personalFullnameId(null)
                    .build()),
                fieldValidationErrorMatcher(
                    "contact",
                    "firstName and lastName or personalFullnameId fields must be not null"
                )
            ),
            Arguments.of(
                "lastName получателя не указан",
                validRequestBuilder().contact(validOrderContactBuilder()
                    .lastName(null)
                    .personalFullnameId(null)
                    .build()),
                fieldValidationErrorMatcher(
                    "contact",
                    "firstName and lastName or personalFullnameId fields must be not null"
                )
            ),
            Arguments.of(
                "phone получателя не указан",
                validRequestBuilder().contact(validOrderContactBuilder().phone(null).personalPhoneId(null).build()),
                fieldValidationErrorMatcher("contact", "phone or personalPhoneId fields must be not null")
            ),
            Arguments.of(
                "contactType получателя не указан",
                validRequestBuilder().contact(validOrderContactBuilder().contactType(null).build()),
                fieldValidationErrorMatcher("contact.contactType", NOT_NULL_ERROR_MESSAGE)
            )
        );
    }

    @Nonnull
    private static UpdateOrderRecipientRequestDto.UpdateOrderRecipientRequestDtoBuilder validRequestBuilder() {
        return UpdateOrderRecipientRequestDto.builder()
            .checkouterRequestId(123L)
            .orderId(1L)
            .barcode("1001")
            .email("test@test.ru")
            .contact(validOrderContactBuilder().build());
    }

    @Nonnull
    private static OrderContactDto.OrderContactDtoBuilder validOrderContactBuilder() {
        return OrderContactDto.builder()
            .phone("12345678")
            .extension("87654321")
            .firstName("Ivan")
            .lastName("Ivanov")
            .middleName("Ivanovich")
            .contactType(ContactType.RECIPIENT)
            .comment("comment")
            .personalFullnameId("personal-fullname-id")
            .personalPhoneId("personal-phone-id");
    }

    @Nonnull
    private ResultActions performRequest(UpdateOrderRecipientRequestDto request) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/orders/updateRecipient", request));
    }
}
