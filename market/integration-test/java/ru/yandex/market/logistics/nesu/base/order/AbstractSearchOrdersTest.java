package ru.yandex.market.logistics.nesu.base.order;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.CancellationOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestPayloadDto;
import ru.yandex.market.logistics.lom.model.dto.OrderActionsDto;
import ru.yandex.market.logistics.lom.model.dto.OrderContactDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.UpdateOrderRecipientRequestDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentStatusHistoryDto;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter.OrderSearchFilterBuilder;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Direction;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.lom.model.search.Sort;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.dto.filter.AbstractOrderSearchFilter;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ValidationErrorDataBuilder;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldErrorBuilder;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractSearchOrdersTest extends AbstractContextualTest {

    @Autowired
    private LomClient lomClient;

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Валидация запроса")
    @MethodSource("filterValidationSource")
    void filterValidation(
        ValidationErrorDataBuilder fieldError,
        Consumer<AbstractOrderSearchFilter> filterAdjuster
    ) throws Exception {
        search(filterAdjuster)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(fieldError.forObject(orderSearchObjectName())));

        verifyNoMoreInteractions(lomClient);
    }

    @Nonnull
    private static Stream<Arguments> filterValidationSource() {
        return Stream.<Pair<ValidationErrorDataBuilder, Consumer<AbstractOrderSearchFilter>>>of(
            Pair.of(
                fieldErrorBuilder("orderIds", "must not contain nulls", "NotNullElements"),
                f -> f.setOrderIds(Collections.singleton(null))
            ),
            Pair.of(
                fieldErrorBuilder("orderIds", "size must be between 0 and 100", "Size")
                    .withArguments(Map.of("min", 0, "max", 100)),
                f -> f.setOrderIds(LongStream.range(0, 101).boxed().collect(Collectors.toSet()))
            ),
            Pair.of(
                fieldErrorBuilder("partnerIds", "must not contain nulls", "NotNullElements"),
                f -> f.setPartnerIds(Collections.singleton(null))
            ),
            Pair.of(
                fieldErrorBuilder("partnerIds", "size must be between 0 and 100", "Size")
                    .withArguments(Map.of("min", 0, "max", 100)),
                f -> f.setPartnerIds(LongStream.range(0, 101).boxed().collect(Collectors.toSet()))
            ),
            Pair.of(
                fieldErrorBuilder("statuses", "must not contain nulls", "NotNullElements"),
                f -> f.setStatuses(Collections.singleton(null))
            ),
            Pair.of(
                fieldErrorBuilder(
                    "cancellationStatuses",
                    "must not contain nulls",
                    "NotNullElements"
                ),
                f -> f.setCancellationStatuses(Collections.singleton(null))
            )
        ).map(pair -> Arguments.of(pair.getLeft(), pair.getRight()));
    }

    @Test
    @DisplayName("Невалидный сендер")
    void searchOrdersInvalidSender() throws Exception {
        search("controller/order/search/request/by_invalid_senders.json")
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/order/search/response/invalid_senders.json"));

        verifyNoMoreInteractions(lomClient);
    }

    @Test
    @DisplayName("Пустой список сендеров")
    void searchOrdersEmptySenderIds() throws Exception {
        search("controller/order/search/request/empty_senders.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/search/response/empty.json"));

        verifyNoMoreInteractions(lomClient);
    }

    @Test
    @DisplayName("Длинный список сендеров")
    void searchOrdersLongSenderIds() throws Exception {
        search("controller/order/search/request/too_many_senders.json")
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(
                fieldError(
                    "senderIds",
                    "size must be between 0 and 100",
                    orderSearchObjectName(),
                    "Size",
                    Map.of("min", 0, "max", 100)
                )
            ));

        verifyNoMoreInteractions(lomClient);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Поля фильтра")
    @MethodSource("searchArguments")
    void searchOrders(
        @SuppressWarnings("unused") String displayName,
        String requestPath,
        OrderSearchFilterBuilder filter
    ) throws Exception {
        mockSearchOrders(filter.build(), defaultResult(), PAGE_DEFAULTS);

        search(requestPath)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/search/response/success.json"));
    }

    @Nonnull
    private static Stream<Arguments> searchArguments() {
        return Stream.of(
            Arguments.of(
                "Только сендер",
                "controller/order/search/request/all.json",
                defaultFilter()
            ),
            Arguments.of(
                "Идентификаторы заказов",
                "controller/order/search/request/by_order_ids.json",
                defaultFilter().orderIds(Set.of(1L, 1000L))
            ),
            Arguments.of(
                "Идентификаторы сендеров",
                "controller/order/search/request/by_sender_ids.json",
                defaultFilter().senderIds(Set.of(11L))
            ),
            Arguments.of(
                "Идентификаторы партнёров",
                "controller/order/search/request/by_partner_ids.json",
                defaultFilter().partnerIds(Set.of(2L, 2000L))
            ),
            Arguments.of(
                "Только OrderStatus-статусы",
                "controller/order/search/request/by_only_order_statuses.json",
                defaultFilter()
                    .statuses(Set.of(OrderStatus.DRAFT))
            ),
            Arguments.of(
                "Только статусы сегментов",
                "controller/order/search/request/by_only_cargo_statuses.json",
                defaultFilter()
                    .segmentStatuses(Map.of(PartnerType.DELIVERY, Set.of(SegmentStatus.PENDING)))
            ),
            Arguments.of(
                "Статусы",
                "controller/order/search/request/by_statuses.json",
                defaultFilter()
                    .statuses(Set.of(OrderStatus.DRAFT, OrderStatus.VALIDATING, OrderStatus.VALIDATION_ERROR))
                    .segmentStatuses(Map.of(
                        PartnerType.DELIVERY,
                        Set.of(SegmentStatus.IN, SegmentStatus.ERROR, SegmentStatus.ERROR_NOT_FOUND),
                        PartnerType.SORTING_CENTER,
                        Set.of(SegmentStatus.ERROR, SegmentStatus.ERROR_NOT_FOUND)
                    ))
            ),
            Arguments.of(
                "Статусы заявок на отмену",
                "controller/order/search/request/by_cancellation_order_statuses.json",
                defaultFilter()
                    .lastCancellationOrderStatuses(EnumSet.of(
                        CancellationOrderStatus.CREATED,
                        CancellationOrderStatus.PROCESSING,
                        CancellationOrderStatus.SYNC_FAIL,
                        CancellationOrderStatus.SUCCESS,
                        CancellationOrderStatus.REQUIRED_SEGMENT_SUCCESS,
                        CancellationOrderStatus.MANUALLY_CONFIRMED
                    ))
            )
        );
    }

    @Test
    @DisplayName("Поиск заказов с сортировкой asc")
    void searchOrdersWithSortingAsc() throws Exception {
        OrderSearchFilter filter = defaultFilter().build();

        PageResult<OrderDto> result = defaultResult();

        mockSearchOrders(filter, result, new Pageable(0, 10, new Sort(Direction.ASC, "created")));

        MultiValueMap<String, String> sortingParam = new LinkedMultiValueMap<>();
        sortingParam.put("sort", List.of("created"));

        search("controller/order/search/request/all.json", sortingParam)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/search/response/success.json"));
    }

    @Test
    @DisplayName("Поиск заказов с сортировкой desc")
    void searchOrdersWithSortingDesc() throws Exception {
        OrderSearchFilter filter = defaultFilter().build();

        PageResult<OrderDto> result = defaultResult();

        mockSearchOrders(filter, result, new Pageable(0, 10, new Sort(Direction.DESC, "created")));

        MultiValueMap<String, String> sortingParam = new LinkedMultiValueMap<>();
        sortingParam.put("sort", List.of("created,desc"));

        search("controller/order/search/request/all.json", sortingParam)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/search/response/success.json"));
    }

    @Test
    @DisplayName("Поиск заказов с заявками на обновление данных получателя")
    void searchOrdersWithUpdateRecipientRequests() throws Exception {
        PageResult<OrderDto> result = defaultResult(List.of(
            defaultOrder()
                .setId(1000L)
                .setChangeOrderRequests(List.of(
                    ChangeOrderRequestDto.builder()
                        .id(1L)
                        .requestType(ChangeOrderRequestType.RECIPIENT)
                        .status(ChangeOrderRequestStatus.FAIL)
                        .payloads(createChangeOrderRequestPayloads())
                        .build(),
                    ChangeOrderRequestDto.builder()
                        .id(2L)
                        .requestType(ChangeOrderRequestType.RECIPIENT)
                        .status(ChangeOrderRequestStatus.SUCCESS)
                        .payloads(createChangeOrderRequestPayloads())
                        .build()
                )),
            defaultOrder()
                .setId(1001L)
                .setChangeOrderRequests(List.of(
                    ChangeOrderRequestDto.builder()
                        .id(3L)
                        .requestType(ChangeOrderRequestType.RECIPIENT)
                        .status(ChangeOrderRequestStatus.REJECTED)
                        .payloads(createChangeOrderRequestPayloads())
                        .build()
                )),
            defaultOrder()
                .setId(1002L)
                .setChangeOrderRequests(List.of(
                    ChangeOrderRequestDto.builder()
                        .id(4L)
                        .requestType(ChangeOrderRequestType.RECIPIENT)
                        .status(ChangeOrderRequestStatus.PROCESSING)
                        .payloads(createChangeOrderRequestPayloads())
                        .build()
                ))
        ));

        mockSearchOrders(defaultFilter().build(), result, PAGE_DEFAULTS);

        search("controller/order/search/request/all.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/search/response/orders_with_update_recipient_requests.json"));
    }

    @Test
    @DisplayName("Поиск заказов с заявками на отмену")
    void searchOrdersWithCancellationRequests() throws Exception {
        OrderSearchFilter filter = defaultFilter().build();

        PageResult<OrderDto> result = defaultResult(List.of(
            defaultOrder()
                .setId(1000L)
                .setSenderId(11L)
                .setAvailableActions(
                    OrderActionsDto.builder()
                        .cancel(false)
                        .build()
                )
                .setCancellationOrderRequests(List.of(
                    CancellationOrderRequestDto.builder()
                        .id(1L)
                        .status(CancellationOrderStatus.FAIL)
                        .build(),
                    CancellationOrderRequestDto.builder()
                        .id(2L)
                        .status(CancellationOrderStatus.SUCCESS)
                        .build()
                )),
            defaultOrder()
                .setId(1001L)
                .setSenderId(11L)
                .setAvailableActions(
                    OrderActionsDto.builder()
                        .cancel(true)
                        .build()
                )
                .setCancellationOrderRequests(List.of(
                    CancellationOrderRequestDto.builder()
                        .id(3L)
                        .status(CancellationOrderStatus.REJECTED)
                        .cancellationErrorMessage("error")
                        .build()
                )),
            defaultOrder()
                .setId(1002L)
                .setSenderId(11L)
                .setAvailableActions(
                    OrderActionsDto.builder()
                        .cancel(null)
                        .build()
                )
                .setCancellationOrderRequests(List.of(
                    CancellationOrderRequestDto.builder()
                        .id(4L)
                        .status(CancellationOrderStatus.PROCESSING)
                        .build()
                ))
        ));

        mockSearchOrders(filter, result, PAGE_DEFAULTS);

        search("controller/order/search/request/all.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/search/response/orders_with_cancellation_requests.json"));
    }

    protected void mockSearchOrders(OrderSearchFilter filter, PageResult<OrderDto> result, Pageable expected) {
        when(lomClient.searchOrders(
            safeRefEq(filter),
            eq(Set.of(OptionalOrderPart.CANCELLATION_REQUESTS, OptionalOrderPart.CHANGE_REQUESTS)),
            ArgumentMatchers.argThat(pageable -> pageable.toUriParams().equals(expected.toUriParams()))
        )).thenReturn(result);
    }

    @Nonnull
    protected static OrderSearchFilterBuilder defaultFilter() {
        return OrderSearchFilter.builder()
            .senderIds(Set.of(11L, 12L))
            .marketIdFrom(1000L);
    }

    @Nonnull
    protected abstract String orderSearchObjectName();

    @Nonnull
    protected ResultActions search(String requestPath) throws Exception {
        return search(requestPath, new LinkedMultiValueMap<>());
    }

    @Nonnull
    protected abstract ResultActions search(String requestPath, MultiValueMap<String, String> params) throws Exception;

    @Nonnull
    protected abstract ResultActions search(Consumer<AbstractOrderSearchFilter> filterAdjuster) throws Exception;

    @Nonnull
    protected PageResult<OrderDto> defaultResult() {
        return defaultResult(List.of(defaultOrder().setId(1000L).setSenderId(11L)));
    }

    @Nonnull
    private PageResult<OrderDto> defaultResult(List<OrderDto> data) {
        return new PageResult<OrderDto>()
            .setData(data)
            .setTotalPages((int) Math.ceil((double) data.size() / (double) 10))
            .setPageNumber(0)
            .setTotalElements(data.size())
            .setSize(10);
    }

    @Nonnull
    private OrderDto defaultOrder() {
        return new OrderDto()
            .setCreated(Instant.parse("2019-07-03T17:00:00Z"))
            .setStatus(OrderStatus.PROCESSING)
            .setPlatformClientId(3L)
            .setSenderId(11L)
            .setWaybill(
                List.of(WaybillSegmentDto.builder()
                    .partnerType(PartnerType.DELIVERY)
                    .segmentStatus(SegmentStatus.PENDING)
                    .waybillSegmentStatusHistory(
                        List.of(
                            WaybillSegmentStatusHistoryDto.builder()
                                .date(Instant.parse("2019-07-04T17:00:00Z"))
                                .status(SegmentStatus.INFO_RECEIVED)
                                .build()
                        )
                    )
                    .build())
            );
    }

    @Nonnull
    private Set<ChangeOrderRequestPayloadDto> createChangeOrderRequestPayloads() {
        return Set.of(
            ChangeOrderRequestPayloadDto.builder()
                .status(ChangeOrderRequestStatus.INFO_RECEIVED)
                .payload(objectMapper.valueToTree(
                    UpdateOrderRecipientRequestDto.builder()
                        .barcode("barcode")
                        .checkouterRequestId(null)
                        .email("test@test.test")
                        .contact(
                            OrderContactDto.builder()
                                .firstName("Ivan")
                                .lastName("Ivanov")
                                .middleName("Ivanovich")
                                .phone("+7999999")
                                .extension("+7888888")
                                .build()
                        ).build()
                )).build()
        );
    }
}
