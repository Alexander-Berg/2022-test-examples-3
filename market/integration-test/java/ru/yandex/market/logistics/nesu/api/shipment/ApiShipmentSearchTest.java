package ru.yandex.market.logistics.nesu.api.shipment;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.ShipmentSearchDto;
import ru.yandex.market.logistics.lom.model.filter.ShipmentSearchFilter;
import ru.yandex.market.logistics.lom.model.filter.ShipmentSearchFilter.ShipmentSearchFilterBuilder;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Direction;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.lom.model.search.Sort;
import ru.yandex.market.logistics.nesu.api.AbstractApiTest;
import ru.yandex.market.logistics.nesu.api.model.shipment.ShopShipmentSearchFilter;
import ru.yandex.market.logistics.nesu.client.enums.ShipmentType;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/repository/shipments/database_prepare.xml")
class ApiShipmentSearchTest extends AbstractApiTest {

    public static final long SHOP_ID = 1L;

    @Autowired
    private MbiApiClient mbiApiClient;
    @Autowired
    private LomClient lomClient;

    @BeforeEach
    void setup() {
        authHolder.mockAccess(mbiApiClient, SHOP_ID);

        when(lomClient.searchShipments(any(), any()))
            .thenAnswer(i -> new PageResult<ShipmentSearchDto>().setData(List.of()));
    }

    @Test
    @DisplayName("Недоступный магазин")
    void noShopAccess() throws Exception {
        authHolder.mockNoAccess(mbiApiClient, SHOP_ID);

        search(defaultFilter())
            .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("validationSource")
    @DisplayName("Валидация фильтра")
    void filterValidation(
        ValidationErrorData fieldError,
        UnaryOperator<ShopShipmentSearchFilter> apiFilter
    ) throws Exception {
        search(apiFilter.apply(defaultFilter()))
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(fieldError));
    }

    private static Stream<Arguments> validationSource() {
        return Stream.<Pair<ValidationErrorData, UnaryOperator<ShopShipmentSearchFilter>>>of(
            Pair.of(
                fieldError(
                    "cabinetId",
                    "must not be null",
                    "shopShipmentSearchFilter",
                    "NotNull"
                ),
                f -> f.setCabinetId(null)
            ),
            Pair.of(
                fieldError(
                    "partnerIds",
                    "must not contain nulls",
                    "shopShipmentSearchFilter",
                    "NotNullElements"
                ),
                f -> f.setPartnerIds(Arrays.asList(12L, null))
            ),
            Pair.of(
                fieldError(
                    "partnerIds",
                    "size must be between 0 and 100",
                    "shopShipmentSearchFilter",
                    "Size",
                    Map.of("min", 0, "max", 100)
                ),
                f -> f.setPartnerIds(LongStream.rangeClosed(0, 101).boxed().collect(Collectors.toList()))
            )
        ).map(t -> Arguments.of(t.getFirst(), t.getSecond()));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("fieldsSource")
    @DisplayName("Поля фильтра")
    void filterFields(
        @SuppressWarnings("unused") String field,
        Consumer<ShopShipmentSearchFilter> apiFilter,
        Consumer<ShipmentSearchFilterBuilder> lomFilter
    ) throws Exception {
        ShopShipmentSearchFilter filter = defaultFilter();
        apiFilter.accept(filter);
        search(filter)
            .andExpect(status().isOk());

        ShipmentSearchFilterBuilder defaultLomFilter = defaultLomFilter();
        lomFilter.accept(defaultLomFilter);
        verify(lomClient).searchShipments(safeRefEq(defaultLomFilter.build()), any());
    }

    private static Stream<Arguments> fieldsSource() {
        return Stream.<Triple<String, Consumer<ShopShipmentSearchFilter>, Consumer<ShipmentSearchFilterBuilder>>>of(
            Triple.of(
                "partnerIds",
                f -> f.setPartnerIds(List.of(12L, 23L)),
                f -> f.partnerIdsTo(Set.of(12L, 23L))
            ),
            Triple.of(
                "shipmentType",
                f -> f.setShipmentType(ShipmentType.IMPORT),
                f -> f.shipmentType(ru.yandex.market.logistics.lom.model.enums.ShipmentType.IMPORT)
            ),
            Triple.of(
                "dateFrom",
                f -> f.setDateFrom(LocalDate.of(2020, 2, 20)),
                f -> f.fromDate(LocalDate.of(2020, 2, 20))
            ),
            Triple.of(
                "dateTo",
                f -> f.setDateTo(LocalDate.of(2021, 2, 21)),
                f -> f.toDate(LocalDate.of(2021, 2, 21))
            )
        ).map(t -> Arguments.of(t.getLeft(), t.getMiddle(), t.getRight()));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("pagingSource")
    @DisplayName("Поля страничного поиска")
    void pagingFields(
        @SuppressWarnings("unused") String field,
        Consumer<MultiValueMap<String, String>> params,
        Pageable pageable
    ) throws Exception {
        LinkedMultiValueMap<String, String> paramsMap = new LinkedMultiValueMap<>();
        params.accept(paramsMap);
        search(defaultFilter(), paramsMap)
            .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(lomClient).searchShipments(safeRefEq(defaultLomFilter().build()), captor.capture());

        softly.assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(pageable);
    }

    private static Stream<Arguments> pagingSource() {
        return Stream.<Triple<String, Consumer<MultiValueMap<String, String>>, Pageable>>of(
            Triple.of(
                "empty",
                p -> {
                },
                new Pageable(0, 10, null)
            ),
            Triple.of(
                "page",
                p -> p.add("page", "1"),
                new Pageable(1, 10, null)
            ),
            Triple.of(
                "size",
                p -> p.add("size", "100"),
                new Pageable(0, 100, null)
            ),
            Triple.of(
                "sort",
                p -> p.add("sort", "created,DESC"),
                new Pageable(0, 10, new Sort(Direction.DESC, "created"))
            )
        ).map(t -> Arguments.of(t.getLeft(), t.getMiddle(), t.getRight()));
    }

    private ShopShipmentSearchFilter defaultFilter() {
        return new ShopShipmentSearchFilter().setCabinetId(SHOP_ID);
    }

    private ShipmentSearchFilterBuilder defaultLomFilter() {
        return ShipmentSearchFilter.builder().marketIdFrom(100L);
    }

    private ResultActions search(ShopShipmentSearchFilter filter) throws Exception {
        return search(filter, new LinkedMultiValueMap<>());
    }

    private ResultActions search(ShopShipmentSearchFilter filter, MultiValueMap<String, String> params)
        throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/api/shipments/search", filter)
            .params(params));
    }

}
