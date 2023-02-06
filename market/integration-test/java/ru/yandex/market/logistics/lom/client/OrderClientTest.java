package ru.yandex.market.logistics.lom.client;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ResponseCreator;

import ru.yandex.market.logistics.lom.model.dto.CancellationOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.CancellationSegmentRequestDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.change.ConfirmChangeOrderDeliveryOptionRequest;
import ru.yandex.market.logistics.lom.model.dto.change.ConfirmChangeOrderDeliveryOptionResponse;
import ru.yandex.market.logistics.lom.model.dto.change.DenyChangeOrderDeliveryOptionRequest;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderStatus;
import ru.yandex.market.logistics.lom.model.enums.CancellationSegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.tags.OrderTag;
import ru.yandex.market.logistics.lom.model.error.FieldError;
import ru.yandex.market.logistics.lom.model.error.ValidationError;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.lom.model.dto.DtoBuilderFactory.orderRequestDto;
import static ru.yandex.market.logistics.lom.model.dto.DtoBuilderFactory.orderResponseDto;
import static ru.yandex.market.logistics.lom.model.dto.DtoBuilderFactory.orderWithRouteRequestDto;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.queryParam;

class OrderClientTest extends AbstractClientTest {

    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Получить заказ")
    void getOrderById() {
        mock.expect(method(HttpMethod.GET))
            .andExpect(requestTo(startsWith(uri + "/orders/1")))
            .andExpect(queryParam("forceUseMaster", "true"))
            .andRespond(
                withSuccess(
                    extractFileContent("response/order/get_order.json"),
                    MediaType.APPLICATION_JSON
                )
            );

        OrderDto actual = lomClient.getOrder(1L).orElseThrow(IllegalStateException::new);
        softly.assertThat(actual).usingRecursiveComparison().isEqualTo(expectedOrder(false));
    }

    @Test
    @DisplayName("Получить заказ с опциональными частями")
    void getOrderByIdWithOptionalParts() {
        mock.expect(method(HttpMethod.GET))
            .andExpect(requestTo(startsWith(uri + "/orders/1")))
            .andExpect(queryParam(
                "optionalParts",
                "CHANGE_REQUESTS",
                "CANCELLATION_REQUESTS",
                "UPDATE_RECIPIENT_ENABLED",
                "GLOBAL_STATUSES_HISTORY",
                "RETURNS_IDS"
            ))
            .andExpect(queryParam("forceUseMaster", "true"))
            .andRespond(
                withSuccess(
                    extractFileContent("response/order/get_order_with_optional_parts.json"),
                    MediaType.APPLICATION_JSON
                )
            );

        OrderDto actual = lomClient.getOrder(1L, OptionalOrderPart.ALL).orElseThrow(IllegalStateException::new);
        softly.assertThat(actual).usingRecursiveComparison().isEqualTo(expectedOrder(true));
    }

    @Test
    @DisplayName("Получить заказ с опциональными частями из реплики")
    void getOrderByIdWithOptionalPartsFromReplica() {
        mock.expect(method(HttpMethod.GET))
            .andExpect(requestTo(startsWith(uri + "/orders/1")))
            .andExpect(queryParam(
                "optionalParts",
                "CHANGE_REQUESTS",
                "CANCELLATION_REQUESTS",
                "UPDATE_RECIPIENT_ENABLED",
                "GLOBAL_STATUSES_HISTORY",
                "RETURNS_IDS"
            ))
            .andExpect(queryParam("forceUseMaster", "false"))
            .andRespond(
                withSuccess(
                    extractFileContent("response/order/get_order_with_optional_parts.json"),
                    MediaType.APPLICATION_JSON
                )
            );

        OrderDto actual = lomClient.getOrder(1L, OptionalOrderPart.ALL, false)
            .orElseThrow(IllegalStateException::new);
        softly.assertThat(actual).usingRecursiveComparison().isEqualTo(expectedOrder(true));
    }

    @Test
    @DisplayName("Получить несуществующий заказ")
    void getNonexistentOrder() {
        mock.expect(method(HttpMethod.GET))
            .andExpect(requestTo(startsWith(uri + "/orders/1")))
            .andExpect(queryParam("forceUseMaster", "true"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andRespond(
                withStatus(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response/order/order_not_found.json"))
            );

        Optional<OrderDto> order = lomClient.getOrder(1L);
        softly.assertThat(order).isEmpty();
    }

    @Test
    @DisplayName("Получить несуществующий заказ из реплики")
    void getNonexistentOrderFromReplica() {
        mock.expect(method(HttpMethod.GET))
            .andExpect(requestTo(startsWith(uri + "/orders/1")))
            .andExpect(queryParam("forceUseMaster", "false"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andRespond(
                withStatus(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response/order/order_not_found.json"))
            );

        Optional<OrderDto> order = lomClient.getOrder(1L, false);
        softly.assertThat(order).isEmpty();
    }

    @Test
    @DisplayName("Создать черновик заказа")
    void createOrderDraft() {
        prepareMockRequest(
            HttpStatus.OK,
            HttpMethod.POST,
            "/orders",
            "request/order/create_order.json",
            "response/order/create_order.json",
            Map.of("autoCommit", "false")
        );
        OrderDto actual = lomClient.createOrder(orderRequestDto(), false);
        OrderDto expected = orderResponseDto().setId(1L);
        softly.assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DisplayName("Создать черновик заказа через OpenApi")
    void createOrderDraftOpenApi() {
        prepareMockRequest(
            HttpStatus.OK,
            HttpMethod.POST,
            "/orders",
            "request/order/create_order_with_source_open_api.json",
            "response/order/create_order_with_source_open_api.json",
            Map.of("autoCommit", "false")
        );
        OrderDto actual = lomClient.createOrder(
            (WaybillOrderRequestDto) orderRequestDto().setTags(Set.of(OrderTag.CREATED_VIA_DAAS_OPEN_API)),
            false
        );
        OrderDto expected = orderResponseDto().setId(1L).setOrderTags(List.of(OrderTag.CREATED_VIA_DAAS_OPEN_API));
        softly.assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DisplayName("Создать черновик заказа через ЛК")
    void createOrderDraftBackOffice() {
        prepareMockRequest(
            HttpStatus.OK,
            HttpMethod.POST,
            "/orders",
            "request/order/create_order_with_source_back_office.json",
            "response/order/create_order_with_source_back_office.json",
            Map.of("autoCommit", "false")
        );
        OrderDto actual = lomClient.createOrder(
            (WaybillOrderRequestDto) orderRequestDto().setTags(Set.of(OrderTag.CREATED_VIA_DAAS_BACK_OFFICE)),
            false
        );
        OrderDto expected = orderResponseDto().setId(1L).setOrderTags(List.of(OrderTag.CREATED_VIA_DAAS_BACK_OFFICE));
        softly.assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DisplayName("Создать и закоммитить заказ")
    void createAndCommitOrder() {
        prepareMockRequest(
            HttpStatus.OK,
            HttpMethod.POST,
            "/orders",
            "request/order/create_order.json",
            "response/order/create_order.json",
            Map.of("autoCommit", "true")
        );
        OrderDto actual = lomClient.createOrder(orderRequestDto(), true);
        OrderDto expected = orderResponseDto().setId(1L);
        softly.assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DisplayName("Создать черновик заказа с маршрутом")
    void createOrderWithRoute() {
        prepareMockRequest(
            HttpStatus.OK,
            HttpMethod.POST,
            "/orders",
            "request/order/create_order.json",
            "response/order/create_order.json",
            Map.of("autoCommit", "false")
        );
        OrderDto actual = lomClient.createOrder(orderWithRouteRequestDto(), false);
        OrderDto expected = orderResponseDto().setId(1L);
        softly.assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DisplayName("Создать и закоммитить заказ с маршрутом")
    void createAndCommitOrderWithRoute() {
        prepareMockRequest(
            HttpStatus.OK,
            HttpMethod.POST,
            "/orders",
            "request/order/create_order.json",
            "response/order/create_order.json",
            Map.of("autoCommit", "true")
        );
        OrderDto actual = lomClient.createOrder(orderWithRouteRequestDto(), true);
        OrderDto expected = orderResponseDto().setId(1L);
        softly.assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DisplayName("Создать и закоммитить заказ с маршрутом - заказ существует")
    void createExistingAndCommitOrderWithRoute() {
        prepareMockRequest(
            HttpStatus.ALREADY_REPORTED,
            HttpMethod.POST,
            "/orders",
            "request/order/create_order.json",
            "response/order/create_order.json",
            Map.of("autoCommit", "true")
        );
        OrderDto actual = lomClient.createOrder(orderWithRouteRequestDto(), true);
        OrderDto expected = orderResponseDto().setId(1L);
        softly.assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DisplayName("Обновить черновик заказа")
    void updateOrderDraft() {
        prepareMockRequest(
            HttpMethod.PUT,
            "/orders/1",
            "request/order/update_order.json",
            "response/order/update_order.json"
        );
        WaybillOrderRequestDto orderDto = orderRequestDto();
        orderDto.setComment("test-comment-updated");
        OrderDto actual = lomClient.updateOrderDraft(1L, orderDto);
        OrderDto expected = orderResponseDto().setId(1L).setComment("test-comment-updated");
        softly.assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DisplayName("Закоммитить невалидный заказ")
    void commitOrder() {
        ResponseCreator responseCreator = withBadRequest()
            .contentType(MediaType.APPLICATION_JSON)
            .body(extractFileContent("response/commit/bad_request.json"));

        mock.expect(method(HttpMethod.POST))
            .andExpect(requestTo(uri + "/orders/1/commit"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andRespond(responseCreator);

        ValidationError validationError = lomClient.commitOrder(1L).orElseThrow(IllegalStateException::new);
        softly.assertThat(validationError.getFieldErrors()).containsExactly(
            FieldError.builder().propertyPath("deliveryType").message("must not be null").build(),
            FieldError.builder().propertyPath("externalId").message("must not be blank").build(),
            FieldError.builder().propertyPath("orderContacts").message("must not be empty").build()
        );
    }

    @Test
    @DisplayName("Закоммитить заказ")
    void commitOrderBadRequest() {
        mock.expect(method(HttpMethod.POST))
            .andExpect(requestTo(uri + "/orders/1/commit"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andRespond(withSuccess());

        Optional<ValidationError> error = lomClient.commitOrder(1L);
        softly.assertThat(error).isEmpty();
    }

    @Test
    @DisplayName("Закоммитить несуществующий заказ")
    void commitOrderNotFound() {
        ResponseCreator responseCreator = withStatus(HttpStatus.NOT_FOUND)
            .contentType(MediaType.APPLICATION_JSON);

        mock.expect(method(HttpMethod.POST))
            .andExpect(requestTo(uri + "/orders/1/commit"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andRespond(responseCreator);

        Throwable thrown = catchThrowable(() -> lomClient.commitOrder(1L));
        softly.assertThat(thrown)
            .isInstanceOf(HttpTemplateException.class)
            .hasFieldOrPropertyWithValue("statusCode", 404);
    }

    @Test
    @DisplayName("Отвязать заказ от отгрузки")
    void untieOrderFromShipment() {
        mock.expect(method(HttpMethod.POST))
            .andExpect(requestTo(uri + "/orders/1/untie-from-shipment"))
            .andRespond(withStatus(HttpStatus.OK));

        lomClient.untieOrderFromShipment(1L);
    }

    @Test
    void confirmChangeDeliveryOptionRequest() {
        mock.expect(method(HttpMethod.POST))
            .andExpect(requestTo(uri + "/orders/changeRequests/10/process"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonRequestContent("request/order/change/confirm.json"))
            .andRespond(
                withSuccess()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response/order/change/confirm.json"))
            );

        ConfirmChangeOrderDeliveryOptionResponse expected = new ConfirmChangeOrderDeliveryOptionResponse()
            .setOrder(orderResponseDto().setId(1L));
        ConfirmChangeOrderDeliveryOptionResponse actual = lomClient.processChangeOrderRequest(
            10L,
            new ConfirmChangeOrderDeliveryOptionRequest().setOrder(orderRequestDto())
        );
        softly.assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void denyChangeDeliveryOptionRequest() {
        mock.expect(method(HttpMethod.POST))
            .andExpect(requestTo(uri + "/orders/changeRequests/10/process"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonRequestContent("request/order/change/deny.json"))
            .andRespond(
                withSuccess()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response/order/change/deny.json"))
            );

        softly.assertThatCode(
                () -> lomClient.processChangeOrderRequest(
                    10,
                    new DenyChangeOrderDeliveryOptionRequest().setMessage("No delivery options found")
                )
            )
            .doesNotThrowAnyException();
    }

    @Nonnull
    private OrderDto expectedOrder(boolean withOptionalParts) {
        OrderDto orderDto = new OrderDto()
            .setId(1L)
            .setStatus(OrderStatus.DRAFT)
            .setSenderId(1L)
            .setPlatformClientId(3L);

        if (withOptionalParts) {
            orderDto.setCancellationOrderRequests(List.of(
                CancellationOrderRequestDto.builder()
                    .id(1L)
                    .status(CancellationOrderStatus.SUCCESS)
                    .cancellationErrorMessage("cancellation-message")
                    .cancellationSegmentRequests(Set.of(
                        CancellationSegmentRequestDto.builder()
                            .partnerId(11L)
                            .status(CancellationSegmentStatus.SUCCESS_BY_API)
                            .required(false)
                            .sufficient(true)
                            .build()
                    ))
                    .build()
            ));
        }

        return orderDto;
    }
}
