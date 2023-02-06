package ru.yandex.market.logistics.logistics4shops.controller.excludefromshipment;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.client.api.ExcludeOrderFromShipmentApi;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ExcludeOrderFromShipmentRequestDto;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ExcludeOrderFromShipmentRequestStatus;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ExcludeOrderRequestsSearchRequest;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ValidationError;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.validatedWith;
import static ru.yandex.market.logistics.logistics4shops.client.api.model.ExcludeOrderFromShipmentRequestStatus.CREATED;
import static ru.yandex.market.logistics.logistics4shops.client.api.model.ExcludeOrderFromShipmentRequestStatus.FAIL;
import static ru.yandex.market.logistics.logistics4shops.client.api.model.ExcludeOrderFromShipmentRequestStatus.PROCESSING;
import static ru.yandex.market.logistics.logistics4shops.client.api.model.ExcludeOrderFromShipmentRequestStatus.SUCCESS;

@DisplayName("Поиск заявок на исключение заказа из отгрузки")
@ParametersAreNonnullByDefault
class SearchExcludeOrderRequestsTest extends AbstractIntegrationTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Поиск заявок по фильтру")
    @DatabaseSetup("/controller/excludefromshipment/search/before/requests_search.xml")
    void searchRequests(
        @SuppressWarnings("unused") String name,
        ExcludeOrderRequestsSearchRequest request,
        List<ExcludeOrderFromShipmentRequestDto> response
    ) {
        var searchRequestsResponse = apiOperation(request)
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(searchRequestsResponse.getExcludeOrderFromShipmentRequests())
            .containsExactlyInAnyOrderElementsOf(response);
    }

    @Test
    @DisplayName("Вернуть BAD_REQUEST, если в фильтре нет ни одного параметра")
    void exceptionOnEmptyFilter() {
        ValidationError error = apiOperation(buildFilter(null, null, null))
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);
        softly.assertThat(error.getMessage()).isEqualTo("Should be at least one filter parameter for requests search");
    }

    @Nonnull
    private ExcludeOrderFromShipmentApi.SearchExcludeOrderRequestsOper apiOperation(
        ExcludeOrderRequestsSearchRequest filter
    ) {
        return apiClient.excludeOrderFromShipment().searchExcludeOrderRequests().body(filter);
    }

    @Nonnull
    static Stream<Arguments> searchRequests() {
        return Stream.of(
            Arguments.of(
                "by orders",
                buildFilter(Set.of(100L, 101L, 103L), null, null),
                List.of(
                    buildRequest(100L, 1000L, CREATED),
                    buildRequest(101L, 1000L, PROCESSING),
                    buildRequest(103L, 1001L, FAIL)
                )
            ),
            Arguments.of(
                "by shipments",
                buildFilter(null, Set.of(1000L, 1002L), null),
                List.of(
                    buildRequest(100L, 1000L, CREATED),
                    buildRequest(101L, 1000L, PROCESSING),
                    buildRequest(102L, 1000L, SUCCESS),
                    buildRequest(104L, 1002L, CREATED)
                )
            ),
            Arguments.of(
                "by statuses",
                buildFilter(null, null, Set.of(CREATED, PROCESSING)),
                List.of(
                    buildRequest(100L, 1000L, CREATED),
                    buildRequest(101L, 1000L, PROCESSING),
                    buildRequest(104L, 1002L, CREATED)
                )
            ),
            Arguments.of(
                "by all",
                buildFilter(Set.of(100L, 102L, 103L, 104L), Set.of(1000L, 1002L), Set.of(CREATED, PROCESSING)),
                List.of(
                    buildRequest(100L, 1000L, CREATED),
                    buildRequest(104L, 1002L, CREATED)
                )
            ),
            Arguments.of(
                "empty result",
                buildFilter(null, Set.of(1000L), Set.of(FAIL)),
                List.of()
            )
        );
    }

    @Nonnull
    private static ExcludeOrderRequestsSearchRequest buildFilter(
        @Nullable Set<Long> orderIds,
        @Nullable Set<Long> shipmentIds,
        @Nullable Set<ExcludeOrderFromShipmentRequestStatus> statuses
    ) {
        return new ExcludeOrderRequestsSearchRequest()
            .orderIds(orderIds)
            .shipmentIds(shipmentIds)
            .statuses(statuses);
    }

    @Nonnull
    private static ExcludeOrderFromShipmentRequestDto buildRequest(
        long orderId,
        long shipmentId,
        ExcludeOrderFromShipmentRequestStatus status
    ) {
        return new ExcludeOrderFromShipmentRequestDto()
            .orderId(orderId)
            .shipmentId(shipmentId)
            .status(status);
    }
}
