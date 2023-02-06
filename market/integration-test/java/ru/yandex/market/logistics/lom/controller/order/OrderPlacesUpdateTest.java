package ru.yandex.market.logistics.lom.controller.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.model.dto.KorobyteDto;
import ru.yandex.market.logistics.lom.model.dto.UpdateOrderPlacesRequestDto;

import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createChangeOrderRequestPayload;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.lesOrderEventPayload;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Ручка обновления грузомест заказа")
@DatabaseSetup("/controller/order/update_order_places/before/prepare.xml")
@ParametersAreNonnullByDefault
class OrderPlacesUpdateTest extends AbstractContextualTest {

    @Test
    @DisplayName("Успех, других активных заявок нет, таска на обработку поставлена")
    @DatabaseSetup("/controller/order/update_order_places/before/waybill_good.xml")
    @ExpectedDatabase(
        value = "/controller/order/update_order_places/after/updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successNoActiveRequests() {
        performUpdate(
            1L,
            requestDtoBuilder(2L).build(),
            jsonContent(
                "controller/order/update_order_places/response/success.json",
                "created",
                "updated"
            ),
            status().isOk()
        );

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.EXPORT_ORDER_BOXES_CHANGED,
            lesOrderEventPayload(1, 1, "1", 1)
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            createChangeOrderRequestPayload(1, "2", 2)
        );
    }

    @Test
    @DisplayName("Успех, есть другая активная заявка, таска на обработку не поставлена")
    @DatabaseSetup("/controller/order/update_order_places/before/waybill_good.xml")
    @DatabaseSetup("/controller/order/update_order_places/before/active_change_request.xml")
    @ExpectedDatabase(
        value = "/controller/order/update_order_places/after/updated_with_active_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successHasActiveRequests() {
        performUpdate(
            1L,
            requestDtoBuilder(2L).build(),
            jsonContent(
                "controller/order/update_order_places/response/success_with_active_request.json",
                "created",
                "updated"
            ),
            status().isOk()
        );

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.CHANGE_ORDER_REQUEST);
    }

    @Test
    @DisplayName("Успех, зануленные ВГХ")
    @DatabaseSetup("/controller/order/update_order_places/before/waybill_good.xml")
    @ExpectedDatabase(
        value = "/controller/order/update_order_places/after/updated_null_dimensions.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successNullDimensions() {
        performUpdate(
            1L,
            requestDtoBuilder(2L)
                .places(List.of(
                    UpdateOrderPlacesRequestDto.Place.builder()
                        .externalId("new-place-external-id-0")
                        .build()
                ))
                .build(),
            jsonContent(
                "controller/order/update_order_places/response/success.json",
                "created",
                "updated"
            ),
            status().isOk()
        );

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.EXPORT_ORDER_BOXES_CHANGED,
            lesOrderEventPayload(1, 1, "1", 1)
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            createChangeOrderRequestPayload(1, "2", 2)
        );
    }

    @Test
    @DisplayName("Заказ не найден")
    void orderNotFound() {
        performUpdate(
            2L,
            requestDtoBuilder(2L).build(),
            jsonContent(
                "controller/order/update_order_places/response/order_not_found.json",
                "created",
                "updated"
            ),
            status().isNotFound()
        );
    }

    @Test
    @DisplayName("Сегмента из запроса нет в заказе")
    @DatabaseSetup("/controller/order/update_order_places/before/waybill_good.xml")
    void segmentNotFound() {
        performUpdate(
            1L,
            requestDtoBuilder(13L).build(),
            jsonContent(
                "controller/order/update_order_places/response/segment_not_found.json",
                "created",
                "updated"
            ),
            status().isNotFound()
        );
    }

    @Test
    @DisplayName("У одного из сегментов статус IN")
    @DatabaseSetup("/controller/order/update_order_places/before/waybill_in_segment.xml")
    void segmentsWithInStatusAffected() {
        performUpdate(
            1L,
            requestDtoBuilder(2L).build(),
            errorMessage("Places update unavailable for order id=1: segments [2] have IN status"),
            status().isBadRequest()
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("requestValidationArguments")
    @DisplayName("Валидация запроса")
    void requestValidation(
        String caseName,
        UpdateOrderPlacesRequestDto request,
        String errorField
    ) {
        performUpdate(
            1L,
            request,
            errorMessage(validationErrorMessage(errorField)),
            status().isBadRequest()
        );
    }

    @Test
    @DisplayName("Если запрос не изменяет коробки - заявка создается, пишется лог")
    @DatabaseSetup("/controller/order/update_order_places/before/waybill_good.xml")
    @ExpectedDatabase(
        value = "/controller/order/update_order_places/after/updated_redundant.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void redundantRequest() {
        performUpdate(
            1L,
            requestDtoBuilder(3L)
                .places(List.of(
                    UpdateOrderPlacesRequestDto.Place.builder()
                        .externalId("old place external id")
                        .dimensions(
                            korobyteDtoBuilder()
                                .length(2)
                                .height(4)
                                .width(6)
                                .weightGross(BigDecimal.valueOf(8))
                                .build()
                        )
                        .build()
                ))
                .build(),
            jsonContent(
                "controller/order/update_order_places/response/success_redundant.json",
                "created",
                "updated"
            ),
            status().isOk()
        );

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            createChangeOrderRequestPayload(1, "1", 1)
        );

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=UPDATE_PLACES_REDUNDANT\t" +
                "payload=Redundant UPDATE_PLACES change request created for order\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "entity_types=order,lom_order\t" +
                "entity_values=order:2,lom_order:1"
        );
    }

    @Nonnull
    private static Stream<Arguments> requestValidationArguments() {
        return Stream.of(
            Arguments.of(
                "Пустой waybillSegmentId",
                requestDtoBuilder(null).build(),
                "waybillSegmentId"
            ),
            Arguments.of(
                "Пустой externalId грузоместа",
                requestDtoBuilder(2L)
                    .places(List.of(
                        UpdateOrderPlacesRequestDto.Place.builder()
                            .externalId(null)
                            .dimensions(korobyteDtoBuilder().build())
                            .build()
                    ))
                    .build(),
                "places[0].externalId"
            ),
            Arguments.of(
                "Пустые размеры в dimensions: длина",
                requestDtoBuilder(2L)
                    .places(List.of(
                        UpdateOrderPlacesRequestDto.Place.builder()
                            .externalId("place-external-id")
                            .dimensions(korobyteDtoBuilder().length(null).build())
                            .build()
                    ))
                    .build(),
                "places[0].dimensions.length"
            ),
            Arguments.of(
                "Пустые размеры в dimensions: ширина",
                requestDtoBuilder(2L)
                    .places(List.of(
                        UpdateOrderPlacesRequestDto.Place.builder()
                            .externalId("place-external-id")
                            .dimensions(korobyteDtoBuilder().width(null).build())
                            .build()
                    ))
                    .build(),
                "places[0].dimensions.width"
            ),
            Arguments.of(
                "Пустые размеры в dimensions: высота",
                requestDtoBuilder(2L)
                    .places(List.of(
                        UpdateOrderPlacesRequestDto.Place.builder()
                            .externalId("place-external-id")
                            .dimensions(korobyteDtoBuilder().height(null).build())
                            .build()
                    ))
                    .build(),
                "places[0].dimensions.height"
            ),
            Arguments.of(
                "Пустые размеры в dimensions: вес",
                requestDtoBuilder(2L)
                    .places(List.of(
                        UpdateOrderPlacesRequestDto.Place.builder()
                            .externalId("place-external-id")
                            .dimensions(korobyteDtoBuilder().weightGross(null).build())
                            .build()
                    ))
                    .build(),
                "places[0].dimensions.weightGross"
            )
        );
    }

    @Nonnull
    private String validationErrorMessage(String errorField) {
        return "Following validation errors occurred:\nField: '" + errorField + "', message: 'must not be null'";
    }

    @Nonnull
    private static UpdateOrderPlacesRequestDto.UpdateOrderPlacesRequestDtoBuilder requestDtoBuilder(
        @Nullable Long waybillSegmentId
    ) {
        return UpdateOrderPlacesRequestDto.builder()
            .waybillSegmentId(waybillSegmentId)
            .places(List.of(
                UpdateOrderPlacesRequestDto.Place.builder()
                    .externalId("new-place-external-id-0")
                    .dimensions(korobyteDtoBuilder().build())
                    .build()
            ));
    }

    @Nonnull
    private static KorobyteDto.KorobyteDtoBuilder korobyteDtoBuilder() {
        return KorobyteDto.builder()
            .height(1)
            .length(2)
            .width(3)
            .weightGross(BigDecimal.valueOf(4.5));
    }

    @SneakyThrows
    private void performUpdate(
        long orderId,
        UpdateOrderPlacesRequestDto requestBody,
        ResultMatcher expectedResponse,
        ResultMatcher expectedStatus
    ) {
        mockMvc.perform(request(HttpMethod.PUT, "/orders/" + orderId + "/updatePlaces", requestBody))
            .andExpect(expectedStatus)
            .andExpect(expectedResponse);
    }
}
