package ru.yandex.market.logistics.lom.controller.order;

import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.model.dto.ChangedItemDto;
import ru.yandex.market.logistics.lom.model.dto.CreateOrderItemIsNotSuppliedRequestDto;
import ru.yandex.market.logistics.lom.model.dto.CreateOrderItemIsNotSuppliedRequestsDto;
import ru.yandex.market.logistics.lom.model.enums.ItemChangeReason;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.lom.utils.TestUtils.validationErrorsJsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

class OrderItemIsNotSuppliedRequestCreateTest extends AbstractContextualTest {

    @BeforeEach
    private void setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Test
    @DisplayName("Создание заявки о товаре, не указанном в поставке, для одного заказа")
    @DatabaseSetup("/controller/order/itemisnotsupplied/before/order_id_1.xml")
    @ExpectedDatabase(
        value = "/controller/order/itemisnotsupplied/after/order_id_1.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void singleOrderItemIsNotSuppliedRequestCreationSuccess() throws Exception {
        performOrderItemIsNotSuppliedRequestCreate(
            CreateOrderItemIsNotSuppliedRequestsDto.builder()
                .createItemIsNotSuppliedRequests(List.of(validCreateOrderItemIsNotSuppliedRequestBuilder(1L).build()))
                .build()
        )
            .andExpect(status().isOk())
            .andExpect(noContent());
        OrderHistoryTestUtil.assertOrderSnapshot(
            jdbcTemplate,
            1L,
            "controller/order/itemisnotsupplied/after/order_1_snapshot.json",
            "created",
            "updated",
            "changeOrderRequests[0].created",
            "changeOrderRequests[0].updated"
        );
    }

    @Test
    @DisplayName("Создание заявки о товаре, не указанном в поставке, для нескольких заказов")
    @DatabaseSetup("/controller/order/itemisnotsupplied/before/order_id_1_2.xml")
    @ExpectedDatabase(
        value = "/controller/order/itemisnotsupplied/after/order_id_1_2.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void multipleOrderItemIsNotSuppliedRequestCreationSuccess() throws Exception {
        performOrderItemIsNotSuppliedRequestCreate(
            CreateOrderItemIsNotSuppliedRequestsDto.builder()
                .createItemIsNotSuppliedRequests(
                    List.of(
                        validCreateOrderItemIsNotSuppliedRequestBuilder(1L).build(),
                        validCreateOrderItemIsNotSuppliedRequestBuilder(2L).build()
                    )
                )
                .build()
        )
            .andExpect(status().isOk())
            .andExpect(noContent());
        OrderHistoryTestUtil.assertOrderSnapshot(
            jdbcTemplate,
            1L,
            "controller/order/itemisnotsupplied/after/order_1_snapshot.json",
            "created",
            "updated",
            "changeOrderRequests[0].created",
            "changeOrderRequests[0].updated"
        );
        OrderHistoryTestUtil.assertOrderSnapshot(
            jdbcTemplate,
            2L,
            "controller/order/itemisnotsupplied/after/order_2_snapshot.json",
            "created",
            "updated",
            "changeOrderRequests[0].created",
            "changeOrderRequests[0].updated"
        );
    }

    @Test
    @DisplayName("Создание заявки для несуществующего заказа")
    void singleOrderItemIsNotSuppliedRequestCreationNotFound() throws Exception {
        performOrderItemIsNotSuppliedRequestCreate(
            CreateOrderItemIsNotSuppliedRequestsDto.builder()
                .createItemIsNotSuppliedRequests(
                    List.of(
                        validCreateOrderItemIsNotSuppliedRequestBuilder(1L).build(),
                        validCreateOrderItemIsNotSuppliedRequestBuilder(2L).build()
                    )
                )
                .build()
        )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("message").value("Failed to find [ORDER] with id [LO-1, LO-2]"));
    }

    @DisplayName("Валидация запроса")
    @MethodSource("validatingArguments")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void invalidRequests(
        String displayName,
        CreateOrderItemIsNotSuppliedRequestsDto request,
        String field,
        String message
    ) throws Exception {
        performOrderItemIsNotSuppliedRequestCreate(request)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorsJsonContent(field, message));
    }

    @Nonnull
    private static Stream<Arguments> validatingArguments() {
        return Stream.of(
            Arguments.of(
                "Список заказов, товары которых не указаны в поставке, не указан",
                CreateOrderItemIsNotSuppliedRequestsDto.builder().build(),
                "createItemIsNotSuppliedRequests",
                "must not be empty"
            ),
            Arguments.of(
                "Список заказов, товары которых не указаны в поставке, пуст",
                CreateOrderItemIsNotSuppliedRequestsDto.builder().createItemIsNotSuppliedRequests(List.of()).build(),
                "createItemIsNotSuppliedRequests",
                "must not be empty"
            ),
            Arguments.of(
                "Заказ, товары которого не указаны в поставке, не указан",
                CreateOrderItemIsNotSuppliedRequestsDto.builder()
                    .createItemIsNotSuppliedRequests(Collections.singletonList(null))
                    .build(),
                "createItemIsNotSuppliedRequests[0]",
                "must not be null"
            ),
            Arguments.of(
                "Идентификатор заказа, товары которого не указаны в поставке, не указан",
                CreateOrderItemIsNotSuppliedRequestsDto.builder()
                    .createItemIsNotSuppliedRequests(List.of(
                        validCreateOrderItemIsNotSuppliedRequestBuilder(1L).barcode(null).build()
                    ))
                    .build(),
                "createItemIsNotSuppliedRequests[0].barcode",
                "must not be null"
            ),
            Arguments.of(
                "Товар, который не указан в поставке, не указан",
                CreateOrderItemIsNotSuppliedRequestsDto.builder()
                    .createItemIsNotSuppliedRequests(List.of(
                        validCreateOrderItemIsNotSuppliedRequestBuilder(1L)
                            .items(Collections.singletonList(null))
                            .build()
                    ))
                    .build(),
                "createItemIsNotSuppliedRequests[0].items[0]",
                "must not be null"
            ),
            Arguments.of(
                "Артикул товара, который не указан в поставке, не указан",
                CreateOrderItemIsNotSuppliedRequestsDto.builder()
                    .createItemIsNotSuppliedRequests(List.of(
                        validCreateOrderItemIsNotSuppliedRequestBuilder(1L)
                            .items(List.of(validChangedItemBuilder(1L).article(null).build()))
                            .build()
                    ))
                    .build(),
                "createItemIsNotSuppliedRequests[0].items[0].article",
                "must not be null"
            ),
            Arguments.of(
                "Поставщик товара, который не указан в поставке, не указан",
                CreateOrderItemIsNotSuppliedRequestsDto.builder()
                    .createItemIsNotSuppliedRequests(List.of(
                        validCreateOrderItemIsNotSuppliedRequestBuilder(1L)
                            .items(List.of(validChangedItemBuilder(1L).vendorId(null).build()))
                            .build()
                    ))
                    .build(),
                "createItemIsNotSuppliedRequests[0].items[0].vendorId",
                "must not be null"
            ),
            Arguments.of(
                "Количество товара, который не указан в поставке, не указано",
                CreateOrderItemIsNotSuppliedRequestsDto.builder()
                    .createItemIsNotSuppliedRequests(List.of(
                        validCreateOrderItemIsNotSuppliedRequestBuilder(1L)
                            .items(List.of(validChangedItemBuilder(1L).count(null).build()))
                            .build()
                    ))
                    .build(),
                "createItemIsNotSuppliedRequests[0].items[0].count",
                "must not be null"
            ),
            Arguments.of(
                "Количество товара, который не указан в поставке, отрицательно",
                CreateOrderItemIsNotSuppliedRequestsDto.builder()
                    .createItemIsNotSuppliedRequests(List.of(
                        validCreateOrderItemIsNotSuppliedRequestBuilder(1L)
                            .items(List.of(validChangedItemBuilder(1L).count(-1L).build()))
                            .build()
                    ))
                    .build(),
                "createItemIsNotSuppliedRequests[0].items[0].count",
                "must be greater than or equal to 0"
            )
        );
    }

    @Nonnull
    private static CreateOrderItemIsNotSuppliedRequestDto.CreateOrderItemIsNotSuppliedRequestDtoBuilder
    validCreateOrderItemIsNotSuppliedRequestBuilder(long id) {
        return CreateOrderItemIsNotSuppliedRequestDto.builder()
            .barcode("LO-" + id)
            .items(List.of(validChangedItemBuilder(id).build()));
    }

    @Nonnull
    private static ChangedItemDto.ChangedItemDtoBuilder validChangedItemBuilder(long id) {
        return ChangedItemDto.builder()
            .article("article " + id)
            .reason(ItemChangeReason.ITEM_IS_NOT_SUPPLIED)
            .count(id)
            .vendorId(id);
    }

    @Nonnull
    private ResultActions performOrderItemIsNotSuppliedRequestCreate(
        CreateOrderItemIsNotSuppliedRequestsDto request
    ) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/orders/itemIsNotSupplied", request));
    }
}
