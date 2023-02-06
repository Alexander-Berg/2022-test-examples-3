package ru.yandex.market.logistics.logistics4go.controller.order;

import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

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
import ru.yandex.market.logistics.logistics4go.client.model.ItemInstanceDto;
import ru.yandex.market.logistics.logistics4go.client.model.ItemWithInstancesDto;
import ru.yandex.market.logistics.logistics4go.client.model.UpdateItemInstancesRequest;
import ru.yandex.market.logistics.logistics4go.client.model.ValidationError;
import ru.yandex.market.logistics.logistics4go.client.model.ValidationViolation;
import ru.yandex.market.logistics.logistics4go.utils.LomFactory;
import ru.yandex.market.logistics.logistics4go.utils.OrderFactory;
import ru.yandex.market.logistics.lom.model.dto.ItemInstancesDto;
import ru.yandex.market.logistics.lom.model.dto.UpdateItemInstancesRequestDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.validatedWith;
import static ru.yandex.market.logistics.logistics4go.utils.OrderFactory.modifier;

@DisplayName("Обновление маркировок заказа")
class UpdateItemsInstancesTest extends AbstractOrderTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Валидация запроса")
    void validation(
        String caseName,
        UnaryOperator<UpdateItemInstancesRequest> requestModifier,
        ValidationViolation expectedViolation
    ) {
        UpdateItemInstancesRequest request = createRequest();
        ValidationError actualValidationError = apiClient.orders().updateItemsInstances()
            .orderIdPath(1L)
            .body(requestModifier.apply(request))
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);
        softly.assertThat(actualValidationError.getCode()).isEqualTo(ErrorType.VALIDATION_ERROR);
        softly.assertThat(actualValidationError.getErrors())
            .containsExactly(expectedViolation);
    }

    @Test
    @DisplayName("Успешное создание заявки на обновление")
    void updateSuccess() {
        mockSearchLomOrder(1L, LomFactory.orderUpdateItemsInstancesAvailable());
        mockLomChangeResponse(lomUpdateRequest());

        ChangeOrderRequestDto response = apiClient.orders().updateItemsInstances()
            .orderIdPath(1L)
            .body(createRequest())
            .execute(validatedWith(shouldBeCode(SC_OK)))
            .as(ChangeOrderRequestDto.class);

        softly.assertThat(response).isEqualTo(
            new ChangeOrderRequestDto()
                .id(1L)
                .requestType(ChangeRequestType.UPDATE_ITEMS_INSTANCES)
                .status(ChangeRequestStatus.CREATED)
        );

        verifySearchLomOrder(1L);
        verifyLomChangeResponse(lomUpdateRequest());
    }

    @Test
    @DisplayName("Существует активная заявка на изменение в ломе")
    void orderHasActiveChangeRequest() {
        mockSearchLomOrder(
            1L,
            LomFactory.order(false)
                .setChangeOrderRequests(List.of(
                    ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto.builder()
                        .requestType(ChangeOrderRequestType.UPDATE_ITEMS_INSTANCES)
                        .status(ChangeOrderRequestStatus.PROCESSING)
                        .build()
                ))
        );

        ApiError error = apiClient.orders().updateItemsInstances()
            .orderIdPath(1L)
            .body(createRequest())
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ApiError.class);

        softly.assertThat(error).isEqualTo(
            new ApiError()
                .code(ErrorType.OPERATION_NOT_ALLOWED)
                .message("Order has active 'UPDATE_ITEMS_INSTANCES' change request")
        );

        verifySearchLomOrder(1L);
    }

    @Test
    @DisplayName("Невозможно обновление после 110 чп")
    void orderHasInStatus() {
        mockSearchLomOrder(1L, LomFactory.order(false));

        ApiError error = apiClient.orders().updateItemsInstances()
            .orderIdPath(1L)
            .body(createRequest())
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ApiError.class);

        softly.assertThat(error).isEqualTo(
            new ApiError()
                .code(ErrorType.OPERATION_NOT_ALLOWED)
                .message("Order has 'IN' status on any segment")
        );

        verifySearchLomOrder(1L);
    }

    @Test
    @DisplayName("Партнер СД не может обновлять маркировки")
    void deliveryPartnerCantUpdateInstances() {
        mockSearchLomOrder(1L, LomFactory.order(false).setWaybill(LomFactory.waybillSegmentsWithoutStatusHistory()));

        ApiError error = apiClient.orders().updateItemsInstances()
            .orderIdPath(1L)
            .body(createRequest())
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ApiError.class);

        softly.assertThat(error).isEqualTo(
            new ApiError()
                .code(ErrorType.OPERATION_NOT_ALLOWED)
                .message("Delivery partner can't update item instances")
        );

        verifySearchLomOrder(1L);
    }

    @Test
    @DisplayName("Товар из запроса не найден в заказе")
    void itemNotFoundInOrder() {
        mockSearchLomOrder(1L, LomFactory.orderUpdateItemsInstancesAvailable());

        ValidationError error = apiClient.orders().updateItemsInstances()
            .orderIdPath(1L)
            .body(
                new UpdateItemInstancesRequest()
                    .items(List.of(itemWithInstances(), new ItemWithInstancesDto().externalId("unknown-ext")))
            )
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);

        softly.assertThat(error.getCode()).isEqualTo(ErrorType.VALIDATION_ERROR);
        softly.assertThat(error.getErrors())
            .containsExactly(new ValidationViolation().field("items[1].externalId").message("not found in LOM order"));

        verifySearchLomOrder(1L);
    }

    @Test
    @DisplayName("Товар из заказа не найден в запросе")
    void itemNotFoundInRequest() {
        mockSearchLomOrder(
            1L,
            LomFactory.orderUpdateItemsInstancesAvailable().setItems(List.of(
                LomFactory.item(false),
                LomFactory.item(false).toBuilder().article("unknown-ext").build()
            ))
        );

        ValidationError error = apiClient.orders().updateItemsInstances()
            .orderIdPath(1L)
            .body(createRequest())
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);

        softly.assertThat(error.getCode()).isEqualTo(ErrorType.VALIDATION_ERROR);
        softly.assertThat(error.getErrors())
            .containsExactly(
                new ValidationViolation()
                    .field("items")
                    .message("item with externalId 'unknown-ext' exists in LOM, but not found in request")
            );

        verifySearchLomOrder(1L);
    }

    @Test
    @DisplayName("У товара больше маркировок, чем его количество в заказе")
    void instancesMoreThanCount() {
        mockSearchLomOrder(1L, LomFactory.orderUpdateItemsInstancesAvailable());

        ValidationError error = apiClient.orders().updateItemsInstances()
            .orderIdPath(1L)
            .body(createRequest().items(List.of(
                itemWithInstances()
                    .instances(List.of(
                        new ItemInstanceDto().cis(OrderFactory.CIS_FULL),
                        new ItemInstanceDto().cis(OrderFactory.CIS_FULL))
                    )
            )))
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);

        softly.assertThat(error.getCode()).isEqualTo(ErrorType.VALIDATION_ERROR);
        softly.assertThat(error.getErrors())
            .containsExactly(
                new ValidationViolation()
                    .field("items[0]")
                    .message(
                        "instances size must not be greater than item count in order, "
                            + "instances size = 2, item count = 1"
                    )
            );

        verifySearchLomOrder(1L);
    }

    @Test
    @DisplayName("Успешное удаление маркировок")
    void removeInstancesSuccess() {
        mockSearchLomOrder(1L, LomFactory.orderUpdateItemsInstancesAvailable());
        mockLomChangeResponse(lomUpdateRequestRemoveInstances());

        ChangeOrderRequestDto response = apiClient.orders().updateItemsInstances()
            .orderIdPath(1L)
            .body(createRequest().items(List.of(itemWithInstances().instances(null))))
            .execute(validatedWith(shouldBeCode(SC_OK)))
            .as(ChangeOrderRequestDto.class);

        softly.assertThat(response).isEqualTo(
            new ChangeOrderRequestDto()
                .id(1L)
                .requestType(ChangeRequestType.UPDATE_ITEMS_INSTANCES)
                .status(ChangeRequestStatus.CREATED)
        );

        verifySearchLomOrder(1L);
        verifyLomChangeResponse(lomUpdateRequestRemoveInstances());
    }

    @Nonnull
    private static Stream<Arguments> validation() {
        return StreamEx.of(violations())
            .mapToEntry(Pair::getRight)
            .mapValues(v -> "%s %s".formatted(v.getField(), v.getMessage()))
            .mapKeyValue((pair, caseName) -> Arguments.of(caseName, pair.getLeft(), pair.getRight()));
    }

    @Nonnull
    private static Stream<Pair<UnaryOperator<UpdateItemInstancesRequest>, ValidationViolation>> violations() {
        return Stream.of(
            Pair.of(
                r -> r.items(null),
                new ValidationViolation().field("items").message("must not be null")
            ),
            Pair.of(
                r -> r.items(List.of()),
                new ValidationViolation().field("items").message("size must be between 1 and 100")
            ),
            Pair.of(
                r -> r.items(IntStream.range(0, 101).mapToObj(i -> itemWithInstances()).collect(Collectors.toList())),
                new ValidationViolation().field("items").message("size must be between 1 and 100")
            ),
            Pair.of(
                modifier(r -> r.getItems().get(0), r -> r.externalId(null), UpdateItemInstancesRequest.class),
                new ValidationViolation().field("items[0].externalId").message("must not be null")
            ),
            Pair.of(
                modifier(r -> r.getItems().get(0), r -> r.externalId(""), UpdateItemInstancesRequest.class),
                new ValidationViolation().field("items[0].externalId").message("size must be between 1 and 50")
            ),
            Pair.of(
                modifier(r -> r.getItems().get(0), r -> r.externalId("a".repeat(51)), UpdateItemInstancesRequest.class),
                new ValidationViolation().field("items[0].externalId").message("size must be between 1 and 50")
            ),
            Pair.of(
                modifier(r -> r.getItems().get(0), r -> r.instances(List.of()), UpdateItemInstancesRequest.class),
                new ValidationViolation().field("items[0].instances").message("size must be between 1 and 100")
            ),
            Pair.of(
                modifier(
                    r -> r.getItems().get(0),
                    r -> r.instances(
                        IntStream.range(0, 101)
                            .mapToObj(i -> new ItemInstanceDto().cis(OrderFactory.CIS_FULL))
                            .collect(Collectors.toList())
                    ),
                    UpdateItemInstancesRequest.class
                ),
                new ValidationViolation().field("items[0].instances").message("size must be between 1 and 100")
            ),
            Pair.of(
                modifier(
                    r -> r.getItems().get(0).getInstances().get(0),
                    r -> r.cis("invalid cis"),
                    UpdateItemInstancesRequest.class
                ),
                new ValidationViolation()
                    .field("items[0].instances[0].cis")
                    .message("item with external id 'item[0].externalId' has invalid cis: 'invalid cis'")
            ),
            Pair.of(
                r -> r.items(List.of(itemWithInstances(), itemWithInstances())),
                new ValidationViolation().field("items").message("externalId 'item[0].externalId' is not unique")
            )
        );
    }

    private void mockLomChangeResponse(UpdateItemInstancesRequestDto request) {
        doReturn(
            ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto.builder()
                .id(1L)
                .requestType(ChangeOrderRequestType.UPDATE_ITEMS_INSTANCES)
                .status(ChangeOrderRequestStatus.PROCESSING)
                .build()
        )
            .when(lomClient).updateOrderItemInstances(request);
    }

    private void verifyLomChangeResponse(UpdateItemInstancesRequestDto request) {
        verify(lomClient).updateOrderItemInstances(request);
    }

    @Nonnull
    private UpdateItemInstancesRequest createRequest() {
        return new UpdateItemInstancesRequest()
            .items(List.of(itemWithInstances()));
    }

    @Nonnull
    UpdateItemInstancesRequestDto lomUpdateRequest() {
        return UpdateItemInstancesRequestDto.builder()
            .orderId(1L)
            .itemInstances(List.of(
                ItemInstancesDto.builder()
                    .article("item[0].externalId")
                    .vendorId(OrderFactory.SUPPLIER_ID)
                    .instances(List.of(
                        Map.of(
                            "CIS", OrderFactory.CIS,
                            "CIS_FULL", OrderFactory.CIS_FULL
                        )
                    ))
                    .build()
            ))
            .build();
    }

    @Nonnull
    UpdateItemInstancesRequestDto lomUpdateRequestRemoveInstances() {
        return UpdateItemInstancesRequestDto.builder()
            .orderId(1L)
            .itemInstances(List.of(
                ItemInstancesDto.builder()
                    .article("item[0].externalId")
                    .vendorId(OrderFactory.SUPPLIER_ID)
                    .instances(null)
                    .build()
            ))
            .build();
    }

    @Nonnull
    private static ItemWithInstancesDto itemWithInstances() {
        return new ItemWithInstancesDto()
            .externalId("item[0].externalId")
            .instances(List.of(
                new ItemInstanceDto()
                    .cis(OrderFactory.CIS_FULL)
            ));
    }
}
