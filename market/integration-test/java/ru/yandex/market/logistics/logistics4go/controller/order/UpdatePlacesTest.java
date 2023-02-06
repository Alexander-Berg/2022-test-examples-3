package ru.yandex.market.logistics.logistics4go.controller.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import yandex.market.combinator.v0.CombinatorOuterClass;

import ru.yandex.market.logistics.logistics4go.client.model.ApiError;
import ru.yandex.market.logistics.logistics4go.client.model.ChangeOrderRequestDto;
import ru.yandex.market.logistics.logistics4go.client.model.ChangeRequestStatus;
import ru.yandex.market.logistics.logistics4go.client.model.ChangeRequestType;
import ru.yandex.market.logistics.logistics4go.client.model.Dimensions;
import ru.yandex.market.logistics.logistics4go.client.model.ErrorType;
import ru.yandex.market.logistics.logistics4go.client.model.Place;
import ru.yandex.market.logistics.logistics4go.client.model.UpdatePlacesRequest;
import ru.yandex.market.logistics.logistics4go.client.model.ValidationError;
import ru.yandex.market.logistics.logistics4go.client.model.ValidationViolation;
import ru.yandex.market.logistics.logistics4go.utils.LomFactory;
import ru.yandex.market.logistics.logistics4go.utils.OrderFactory;
import ru.yandex.market.logistics.lom.model.dto.UpdateOrderPlacesRequestDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Обновление информации о ВГХ и штрихкодах грузомест")
@DatabaseSetup("/controller/order/update_places/max_dimensions_logging_flag.xml")
class UpdatePlacesTest extends AbstractOrderTest {
    private static final long ORDER_ID = 13L;

    @BeforeEach
    void setUp() {
        mockSearchLomOrder(ORDER_ID, LomFactory.order(false));
        when(combinatorGrpcClient.getMaxDimensions(
            CombinatorOuterClass.OrderMaxDimensionsRequest.newBuilder()
                .setOrderId(ORDER_ID)
                .setOrderIdPrefix("LOinttest-")
                .build()
        ))
            .thenReturn(
                CombinatorOuterClass.OrderMaxDimensionsResponse.newBuilder()
                    .addAllMaxDimensions(List.of(10, 20, 30))
                    .setMaxWeight(1234)
                    .build()
            );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Валидация запроса")
    void requestValidation(
        @SuppressWarnings("unused") String displayName,
        UpdatePlacesRequest request,
        ValidationViolation expectedViolation
    ) {
        ValidationError actualValidationError = apiClient.orders().updatePlaces()
            .orderIdPath(ORDER_ID)
            .body(request)
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);
        softly.assertThat(actualValidationError.getCode()).isEqualTo(ErrorType.VALIDATION_ERROR);
        softly.assertThat(actualValidationError.getErrors())
            .containsExactly(expectedViolation);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Успешное обновление")
    void updateSuccess(
        @SuppressWarnings("unused") String displayName,
        UpdatePlacesRequest l4gRequest,
        String dimensionsDifferenceLogMessage
    ) {
        UpdateOrderPlacesRequestDto lomRequest = createLomRequest(l4gRequest);

        doReturn(
            ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto.builder()
                .id(1L)
                .requestType(ChangeOrderRequestType.UPDATE_PLACES)
                .status(ChangeOrderRequestStatus.PROCESSING)
                .build()
        )
            .when(lomClient).updatePlaces(ORDER_ID, lomRequest);

        ChangeOrderRequestDto response = apiClient.orders().updatePlaces()
            .orderIdPath(ORDER_ID)
            .body(l4gRequest)
            .execute(validatedWith(shouldBeCode(SC_OK)))
            .as(ru.yandex.market.logistics.logistics4go.client.model.ChangeOrderRequestDto.class);

        softly.assertThat(response).isEqualTo(
            new ChangeOrderRequestDto()
                .id(1L)
                .requestType(ChangeRequestType.UPDATE_PLACES)
                .status(ChangeRequestStatus.CREATED)
        );

        verifySearchLomOrder(ORDER_ID);
        verify(lomClient).updatePlaces(ORDER_ID, lomRequest);
        verify(combinatorGrpcClient).getMaxDimensions(
            CombinatorOuterClass.OrderMaxDimensionsRequest.newBuilder()
                .setOrderId(13L)
                .setOrderIdPrefix("LOinttest-")
                .build()
        );
        softly.assertThat(backLogCaptor.getResults().toString()).contains(dimensionsDifferenceLogMessage);
    }

    @Test
    @DisplayName("Обновление недоступно, ошибка из LOM - заказ отгружен")
    void updateUnavailableLomeErrorOrderShipped() {
        UpdatePlacesRequest l4gRequest = createRequest();
        UpdateOrderPlacesRequestDto lomRequest = createLomRequest(l4gRequest);

        String errorMessage = String.format(
            "Could not update places for order id=%s: segments %s have IN status",
            ORDER_ID,
            1001L
        );
        doThrow(new HttpTemplateException(
            SC_BAD_REQUEST,
            String.format("{\"message\": \"%s\"}", errorMessage)
        ))
            .when(lomClient).updatePlaces(ORDER_ID, lomRequest);

        ApiError error = apiClient.orders().updatePlaces()
            .orderIdPath(ORDER_ID)
            .body(l4gRequest)
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ApiError.class);

        softly.assertThat(error).isEqualTo(
            new ApiError()
                .code(ErrorType.OPERATION_NOT_ALLOWED)
                .message(errorMessage)
        );

        verifySearchLomOrder(ORDER_ID);
        verify(lomClient).updatePlaces(ORDER_ID, lomRequest);
        verify(combinatorGrpcClient).getMaxDimensions(
            CombinatorOuterClass.OrderMaxDimensionsRequest.newBuilder()
                .setOrderId(13L)
                .setOrderIdPrefix("LOinttest-")
                .build()
        );
    }

    @Nonnull
    private static Stream<Arguments> requestValidation() {
        return Stream.of(
            getArguments(
                modifyPlace(place -> place.setExternalId(null)),
                "places[0].externalId",
                "must not be null"
            ),
            getArguments(
                modifyDimensions(dimensions -> dimensions.setHeight(null)),
                "places[0].dimensions.height",
                "must not be null"
            ),
            getArguments(
                modifyDimensions(dimensions -> dimensions.setLength(null)),
                "places[0].dimensions.length",
                "must not be null"
            ),
            getArguments(
                modifyDimensions(dimensions -> dimensions.setWeight(null)),
                "places[0].dimensions.weight",
                "must not be null"
            ),
            getArguments(
                modifyDimensions(dimensions -> dimensions.setWidth(null)),
                "places[0].dimensions.width",
                "must not be null"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> updateSuccess() {
        return Stream.of(
            Arguments.of(
                "Максимальные ВГХ не превышены",
                new UpdatePlacesRequest()
                    .places(List.of(
                        new Place()
                            .externalId("place[0].externalId")
                            .dimensions(
                                new Dimensions()
                                    .length(10)
                                    .height(10)
                                    .width(10)
                                    .weight(BigDecimal.valueOf(1.233))
                            ),
                        new Place()
                            .externalId("place[1].externalId")
                            .dimensions(
                                new Dimensions()
                                    .length(10)
                                    .height(20)
                                    .width(30)
                                    .weight(BigDecimal.valueOf(1.234))
                            )
                    )),
                "level=INFO\t" +
                    "format=plain\t" +
                    "code=PLACES_MAX_DIMENSIONS_WITHIN_LIMIT\t" +
                    "payload=Max dimensions within limit on updatePlaces request\t" +
                    "request_id=test-request-id\t" +
                    "entity_types=lom_order\t" +
                    "entity_values=lom_order:13"
            ),
            Arguments.of(
                "Максимальные ВГХ превышены",
                new UpdatePlacesRequest()
                    .places(List.of(
                        new Place()
                            .externalId("place[0].externalId")
                            .dimensions(
                                new Dimensions()
                                    .length(20)
                                    .height(20)
                                    .width(20)
                                    .weight(BigDecimal.valueOf(1.235))
                            ),
                        new Place()
                            .externalId("place[1].externalId")
                            .dimensions(
                                new Dimensions()
                                    .length(10)
                                    .height(20)
                                    .width(30)
                                    .weight(BigDecimal.valueOf(1.234))
                            ),
                        new Place()
                            .externalId("place[2].externalId")
                            .dimensions(
                                new Dimensions()
                                    .length(11)
                                    .height(21)
                                    .width(31)
                                    .weight(BigDecimal.valueOf(1.234))
                            )
                    )),
                "level=INFO\t" +
                    "format=plain\t" +
                    "code=PLACES_MAX_DIMENSIONS_EXCEEDED\t" +
                    "payload=Max dimensions exceeded on updatePlaces request\t" +
                    "request_id=test-request-id\t" +
                    "entity_types=lom_order\t" +
                    "entity_values=lom_order:13\t" +
                    "extra_keys=place[0].externalId,place[2].externalId\t" +
                    "extra_values=" +
                    "length exceeded by 10.0cm;weight exceeded by 0.001kg;," +
                    "height exceeded by 1.0cm;width exceeded by 1.0cm;length exceeded by 1.0cm;"
            )
        );
    }

    @Nonnull
    private static UpdatePlacesRequest modifyPlace(Consumer<Place> modifier) {
        Place place = OrderFactory.place();
        modifier.accept(place);
        return new UpdatePlacesRequest()
            .places(List.of(place));
    }

    @Nonnull
    private static Arguments getArguments(UpdatePlacesRequest request, String field, String message) {
        return Arguments.of(
            field + " " + message,
            request,
            new ValidationViolation()
                .field(field)
                .message(message)
        );
    }

    @Nonnull
    private static UpdatePlacesRequest modifyDimensions(Consumer<Dimensions> modifier) {
        Place place = OrderFactory.place();
        modifier.accept(place.getDimensions());
        return new UpdatePlacesRequest()
            .places(List.of(place));
    }

    @Nonnull
    private UpdateOrderPlacesRequestDto createLomRequest(UpdatePlacesRequest l4gRequest) {
        List<UpdateOrderPlacesRequestDto.Place> lomPlaces = l4gRequest.getPlaces().stream()
            .map(l4gPlace -> UpdateOrderPlacesRequestDto.Place.builder()
                .externalId(l4gPlace.getExternalId())
                .dimensions(LomFactory.dimensions(
                    l4gPlace.getDimensions().getLength(),
                    l4gPlace.getDimensions().getWidth(),
                    l4gPlace.getDimensions().getHeight(),
                    l4gPlace.getDimensions().getWeight().doubleValue()
                ))
                .build()
            )
            .toList();

        return UpdateOrderPlacesRequestDto.builder()
            .waybillSegmentId(1001L)
            .places(lomPlaces)
            .build();
    }

    @Nonnull
    private UpdatePlacesRequest createRequest() {
        return new UpdatePlacesRequest()
            .places(List.of(OrderFactory.place()));
    }
}
