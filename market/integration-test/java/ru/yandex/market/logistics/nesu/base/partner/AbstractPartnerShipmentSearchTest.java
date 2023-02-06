package ru.yandex.market.logistics.nesu.base.partner;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.delivery.transport_manager.model.dto.RegisterOrdersCountDto;
import ru.yandex.market.delivery.transport_manager.model.dto.RegisterOrdersCountRequestDto;
import ru.yandex.market.delivery.transport_manager.model.dto.StatusHistoryInfoDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationSearchDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationStatusHistoryInfoDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationStatusHistoryInfoRequestDto;
import ru.yandex.market.delivery.transport_manager.model.filter.TransportationSearchFilter;
import ru.yandex.market.delivery.transport_manager.model.filter.TransportationSearchFilter.TransportationSearchFilterBuilder;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentFilter;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentFilter.PartnerShipmentFilterBuilder;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentStatus;
import ru.yandex.market.logistics.nesu.model.L4ShopsFactory;
import ru.yandex.market.logistics.nesu.model.TMFactory;
import ru.yandex.market.logistics.nesu.utils.MatcherUtils;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ErrorType;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ValidationErrorDataBuilder;
import ru.yandex.market.logistics4shops.client.model.OutboundsListDto;
import ru.yandex.market.logistics4shops.client.model.OutboundsSearchRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.model.TMFactory.defaultOutbound;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldErrorBuilder;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractPartnerShipmentSearchTest extends AbstractPartnerShipmentTest {

    private static final long EXPRESS_PARTNER_SUBTYPE = 34;

    private static final LocalDate DATE_FROM = LocalDate.of(2021, 3, 1);
    private static final LocalDate DATE_TO = LocalDate.of(2021, 4, 6);

    @BeforeEach
    void setupWarehouses() {
        clock.setFixed(TMFactory.SHIPMENT_DATE.atTime(12, 15).toInstant(ZoneOffset.UTC), ZoneId.systemDefault());

        mockWarehouses();
        mockPartnerRelations(Set.of(TMFactory.PARTNER_ID));
        mockHandlingTime(0);
    }

    @AfterEach
    void verifyMocks() {
        verifyNoMoreInteractions(lmsClient, outboundApi);
    }

    @Test
    @DisplayName("Нет настроек партнёра магазина")
    void noShopPartner() throws Exception {
        searchShipments(shipmentFilter().build(), SECOND_SHOP_ID, 0, 10)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find dropship partner for shops [20]"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Валидация фильтра")
    void filterValidation(
        ValidationErrorDataBuilder error,
        PartnerShipmentFilterBuilder filterBuilder
    ) throws Exception {
        searchShipments(filterBuilder.build())
            .andExpect(status().isBadRequest())
            .andExpect(MatcherUtils.validationErrorMatcher(error.forObject("partnerShipmentFilter")));
    }

    @Nonnull
    private static Stream<Arguments> filterValidation() {
        return Stream.of(
            Arguments.of(
                fieldErrorBuilder("dateFrom", ErrorType.NOT_NULL),
                shipmentFilter().dateFrom(null)
            ),
            Arguments.of(
                fieldErrorBuilder("dateTo", ErrorType.NOT_NULL),
                shipmentFilter().dateTo(null)
            ),
            Arguments.of(
                fieldErrorBuilder("statuses", ErrorType.NOT_NULL_ELEMENTS),
                shipmentFilter().statuses(Collections.singletonList(null))
            ),
            Arguments.of(
                fieldErrorBuilder("orderIds", ErrorType.NOT_NULL_ELEMENTS),
                shipmentFilter().orderIds(Collections.singletonList(null))
            ),
            Arguments.of(
                fieldErrorBuilder("number", "must not be empty", "NullOrNotBlank"),
                shipmentFilter().number(" ")
            ),
            Arguments.of(
                fieldErrorBuilder("warehousesFrom", ErrorType.NOT_NULL_ELEMENTS),
                shipmentFilter().warehousesFrom(Collections.singletonList(null))
            ),
            Arguments.of(
                fieldErrorBuilder("warehousesTo", ErrorType.NOT_NULL_ELEMENTS),
                shipmentFilter().warehousesTo(Collections.singletonList(null))
            )
        );
    }

    @Test
    @DisplayName("Минимальный ответ")
    void minimal() throws Exception {
        mockTransportationSearch(
            transportationFilter(),
            defaultSearchResults(1)
        );

        searchShipments(shipmentFilter().build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/search/minimal.json"));

        verifyLmsClient();
        verifyGetOutbounds(1);
    }

    @Test
    @DisplayName("Применение сортировки")
    void sorted() throws Exception {
        clock.setFixed(TMFactory.SHIPMENT_DATE.atTime(15, 40).toInstant(ZoneOffset.UTC), ZoneId.systemDefault());

        when(transportManagerClient.getOrdersCount(new RegisterOrdersCountRequestDto(List.of(1000L))))
            .thenReturn(List.of(new RegisterOrdersCountDto(1000L, 2L)));

        mockTransportationSearchForStatusSorting();
        mockTransportationStatusHistory();
        mockL4sSearchOutbounds();

        searchShipments(shipmentFilter().sortedByStatus(true).build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/search/sorted_statuses.json"));

        verifyLmsClient();
        verifyGetOutbounds(7);
    }

    @Test
    @DisplayName("Минимальный ответ. Список shopId")
    void minimalWithShopIds() throws Exception {
        mockTransportationSearch(
            transportationFilter(),
            defaultSearchResults(1)
        );

        mockMvc.perform(
                request(HttpMethod.PUT, url(), shipmentFilter().build())
                    .param("userId", "100")
                    .param("shopIds", String.valueOf(SHOP_ID), String.valueOf(SECOND_SHOP_ID))
                    .param("page", String.valueOf(0))
                    .param("size", String.valueOf(10))
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/search/minimal.json"));

        verifyLmsClient();
        verifyGetOutbounds(1);
    }

    @Test
    @DisplayName("Ошибка поиска. Пустой shopId")
    void minimalWithOutShopIds() throws Exception {
        ValidationErrorData.ValidationErrorDataBuilder validationError =
            ValidationErrorData.objectErrorBuilder(
                "Must specify either shopId or shopIds",
                "ValidShopIds"
            );
        mockMvc.perform(
                request(HttpMethod.PUT, url(), shipmentFilter().build())
                    .param("userId", "100")
                    .param("page", String.valueOf(0))
                    .param("size", String.valueOf(10))
            )
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(validationError.forObject("shopIdsHolder")));
    }

    @Test
    @DisplayName("Счётчики заказов")
    void orderCount() throws Exception {
        TransportationSearchDto first = TMFactory.transportationSearch(
            TMFactory.defaultOutbound()
                .registers(List.of(TMFactory.outboundRegister().build()))
                .build(),
            TMFactory.defaultMovement().build()
        );
        TransportationSearchDto second = TMFactory.transportationSearch(
            TMFactory.defaultOutbound()
                .yandexId(TMFactory.outboundId(1))
                .build(),
            TMFactory.defaultMovement().build(),
            TMFactory.defaultInbound()
                .registers(List.of(TMFactory.inboundRegister().build()))
                .build()
        );
        second.setId(TMFactory.SHIPMENT_ID + 1);
        mockTransportationSearch(transportationFilter(), List.of(first, second));
        TMFactory.mockOutboundUnits(transportManagerClient, List.of());

        when(transportManagerClient.getOrdersCount(
            new RegisterOrdersCountRequestDto(List.of(TMFactory.OUTBOUND_REGISTER_ID, TMFactory.INBOUND_REGISTER_ID))
        )).thenReturn(List.of(
            new RegisterOrdersCountDto(TMFactory.OUTBOUND_REGISTER_ID, 10L),
            new RegisterOrdersCountDto(TMFactory.INBOUND_REGISTER_ID, 3L)
        ));

        mockOutbounds(2, List.of(1L, 2L, 3L, 4L));

        searchShipments(shipmentFilter().build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/search/orders_count.json"));

        verifyLmsClient();
        verifyGetOutbounds(2);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Фильтр из ТМ")
    void transientFilter(
        @SuppressWarnings("unused") String displayName,
        PartnerShipmentFilterBuilder shipmentFilter,
        TransportationSearchFilterBuilder transportationFilter
    ) throws Exception {
        mockTransportationSearch(transportationFilter, defaultSearchResults(1));
        searchShipments(shipmentFilter.build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/search/minimal.json"));

        verifyLmsClient();
        verifyGetOutbounds(1);
    }

    @Nonnull
    private static Stream<Arguments> transientFilter() {
        return Stream.of(
            Arguments.of(
                "orderIds",
                shipmentFilter().orderIds(List.of(3L, 2L, 1L)),
                transportationFilter().outboundOrderIds(Set.of(1L, 2L, 3L))
            ),
            Arguments.of(
                "warehousesFrom",
                shipmentFilter().warehousesFrom(List.of(900L, 901L)),
                transportationFilter().outboundLogisticPointIds(Set.of(900L, 901L))
            ),
            Arguments.of(
                "warehousesTo",
                shipmentFilter().warehousesTo(List.of(910L, 911L)),
                transportationFilter().inboundLogisticPointIds(Set.of(910L, 911L))
            )
        );
    }

    @Test
    @DisplayName("Статус")
    void shipmentStatus() throws Exception {
        mockTransportationSearch(
            transportationFilter().outboundOrderIds(Set.of(1L, 2L, 3L)),
            defaultSearchResults(2)
        );

        when(transportManagerClient.getTransportationsStatusHistory(
            new TransportationStatusHistoryInfoRequestDto()
                .setTransportationIds(List.of(TMFactory.SHIPMENT_ID, TMFactory.SHIPMENT_ID + 1))
        )).thenReturn(List.of(
            new TransportationStatusHistoryInfoDto()
                .setTransportationId(TMFactory.SHIPMENT_ID)
                .setStatusHistoryList(List.of(
                    new StatusHistoryInfoDto()
                        .setNewStatus("COULD_NOT_BE_MATCHED")
                        .setChangedAt(Instant.parse("2021-05-02T01:00:00Z"))
                )),
            new TransportationStatusHistoryInfoDto()
                .setTransportationId(TMFactory.SHIPMENT_ID + 1)
                .setStatusHistoryList(List.of(
                    new StatusHistoryInfoDto()
                        .setNewStatus("ERROR")
                        .setChangedAt(Instant.parse("2021-05-02T01:00:00Z"))
                ))
        ));

        mockOutbounds(2, TMFactory.outboundId(1), List.of(1L, 2L), "2021-05-03T00:00:00Z");

        searchShipments(shipmentFilter().orderIds(List.of(1L, 2L, 3L)).build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/search/status.json"));

        verifyLmsClient();
        verifyGetOutbounds(2);
    }

    @Test
    @DisplayName("Фильтр по статусу")
    void statusFilter() throws Exception {
        mockTransportationSearch(
            transportationFilter(),
            defaultSearchResults(2)
        );

        mockOutbounds(2, TMFactory.outboundId(1), List.of(1L, 2L), L4ShopsFactory.CONFIRMED);

        searchShipments(
            shipmentFilter()
                .statuses(List.of(PartnerShipmentStatus.OUTBOUND_CONFIRMED))
                .build()
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/search/status_filter.json"));

        verifyLmsClient();
        verifyGetOutbounds(2);
    }

    @Test
    @DisplayName("Фильтр по наличию заказов")
    void withOrdersFilter() throws Exception {
        TransportationSearchDto transportationWithOrders = TMFactory.transportationSearch(
            TMFactory.defaultOutbound()
                .registers(List.of(TMFactory.outboundRegister().build()))
                .build(),
            TMFactory.defaultMovement().build()
        );
        int index = 1;
        TransportationSearchDto transportationWithoutOrders = TMFactory.transportationSearch(
            TMFactory.defaultOutbound()
                .yandexId(TMFactory.outboundId(index))
                .registers(List.of(TMFactory.outboundRegister(index).build()))
                .build(),
            TMFactory.defaultMovement().build()
        );
        TransportationSearchDto transportationWithoutRegisters = TMFactory.transportationSearch(
            TMFactory.defaultOutbound()
                .yandexId(TMFactory.outboundId(2))
                .build(),
            TMFactory.defaultMovement().build()
        );

        mockTransportationSearch(
            transportationFilter(),
            List.of(transportationWithOrders, transportationWithoutOrders, transportationWithoutRegisters)
        );

        long outboundWithoutOrdersRegistryId = TMFactory.OUTBOUND_REGISTER_ID + index;
        when(transportManagerClient.getOrdersCount(
            new RegisterOrdersCountRequestDto(List.of(TMFactory.OUTBOUND_REGISTER_ID, outboundWithoutOrdersRegistryId))
        )).thenReturn(List.of(
            new RegisterOrdersCountDto(TMFactory.OUTBOUND_REGISTER_ID, 10L),
            new RegisterOrdersCountDto(outboundWithoutOrdersRegistryId, 0L)
        ));

        mockOutbounds(3, List.of(1L, 2L, 3L, 4L));

        searchShipments(shipmentFilter().withOrders(true).build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/search/with_orders_filter.json"));

        verifyLmsClient();
        verifyGetOutbounds(3);
    }

    @Test
    @DisplayName("Пагинация")
    void paging() throws Exception {
        mockTransportationSearch(
            transportationFilter(),
            defaultSearchResults(4)
        );

        searchShipments(shipmentFilter().build(), 1, 2)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/search/paging.json"));

        verifyLmsClient();
        verifyGetOutbounds(4);
    }

    @Test
    @DisplayName("Фильтр по номеру отгрузки")
    void number() throws Exception {
        mockTransportationSearch(
            transportationFilter(),
            List.of(
                TMFactory.transportationSearch(
                    TMFactory.defaultOutbound()
                        .registers(List.of(TMFactory.outboundRegister().build()))
                        .build(),
                    TMFactory.defaultMovement().build()
                ),
                TMFactory.transportationSearch(1),
                TMFactory.transportationSearch(2)
            )
        );

        mockOutbounds(3, TMFactory.outboundId(1), List.of(1L, 2L), L4ShopsFactory.CONFIRMED, "shipment number");

        searchShipments(shipmentFilter().number("T N").build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/search/number.json"));

        verifyLmsClient();
        verifyGetOutbounds(3);
    }

    @Test
    @DisplayName("Отправка ещё не создана в l4s")
    void notCreatedOutbound() throws Exception {
        mockTransportationSearch(
            transportationFilter(),
            List.of(
                TMFactory.transportationSearch(
                    TMFactory.defaultOutbound()
                        .yandexId(null)
                        .build(),
                    TMFactory.defaultMovement().build()
                )
            )
        );

        searchShipments(shipmentFilter().build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/search/minimal.json"));

        verify(outboundApi, never()).searchOutbounds(any());
        verifyLmsClient();
    }

    @Test
    @DisplayName("Перемещение удалено в TM")
    void deletedTransportation() throws Exception {
        TransportationSearchDto hasOrders = TMFactory.transportationSearch(
                TMFactory.defaultOutbound()
                    .registers(List.of(TMFactory.outboundRegister().build()))
                    .build(),
                TMFactory.defaultMovement().build()
            )
            .setDeleted(true);
        TransportationSearchDto zeroOrders = TMFactory.transportationSearch(
                TMFactory.defaultOutbound()
                    .registers(List.of(TMFactory.outboundRegister().id(TMFactory.OUTBOUND_REGISTER_ID + 1).build()))
                    .yandexId(TMFactory.outboundId(1))
                    .build(),
                TMFactory.defaultMovement().build()
            )
            .setDeleted(true);
        TransportationSearchDto noRegister = TMFactory.transportationSearch(2)
            .setDeleted(true);
        mockTransportationSearch(
            transportationFilter(),
            List.of(hasOrders, zeroOrders, noRegister)
        );

        when(transportManagerClient.getOrdersCount(new RegisterOrdersCountRequestDto(
            List.of(TMFactory.OUTBOUND_REGISTER_ID, TMFactory.OUTBOUND_REGISTER_ID + 1)
        ))).thenReturn(List.of(
            new RegisterOrdersCountDto(TMFactory.OUTBOUND_REGISTER_ID, 2L),
            new RegisterOrdersCountDto(TMFactory.OUTBOUND_REGISTER_ID + 1, 0L)
        ));

        searchShipments(shipmentFilter().build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/search/deleted.json"));

        verifyLmsClient();
        verifyGetOutbounds(3);
    }

    @Test
    @DisplayName("Экспресс-отгрузки исключаются")
    void expressPartnersExcluded() throws Exception {
        long expressPartnerId = 404;

        mockExpressPartner(expressPartnerId);
        mockTransportationSearch(
            transportationFilter().movementExcludePartnerIds(Set.of(expressPartnerId)),
            defaultSearchResults(1)
        );

        searchShipments(shipmentFilter().build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/search/minimal.json"));

        verifyLmsClient();
        verifyGetOutbounds(1);
    }

    private void mockTransportationSearchForStatusSorting() {
        List<TransportationSearchDto> result = defaultSearchResults(2);

        var confirmedTransportation = createTransportation(2, LocalDateTime.of(2021, 3, 4, 15, 37));
        result.add(confirmedTransportation);

        var errorTransportation = createTransportation(3, LocalDateTime.of(2021, 3, 4, 10, 10));
        result.add(errorTransportation);

        var readyForConfirmationTransportation = TMFactory.transportationSearch(
            defaultOutbound()
                .yandexId(TMFactory.outboundId(4))
                .registers(List.of(TMFactory.outboundRegister().build()))
                .build()
        );
        readyForConfirmationTransportation.setId(TMFactory.SHIPMENT_ID + 4);
        result.add(readyForConfirmationTransportation);

        var secondErrorTransportation = createTransportation(5, LocalDateTime.of(2021, 3, 4, 10, 5));
        result.add(secondErrorTransportation);

        var secondConfirmedTransportation = createTransportation(6, LocalDateTime.of(2021, 3, 4, 15, 38));
        result.add(secondConfirmedTransportation);

        mockTransportationSearch(transportationFilter(), result);
    }

    @Nonnull
    private TransportationSearchDto createTransportation(int index, LocalDateTime plannedIntervalStart) {
        var transportation = TMFactory.transportationSearch(
            defaultOutbound()
                .plannedIntervalStart(plannedIntervalStart)
                .yandexId(TMFactory.outboundId(index))
                .build()
        );
        transportation.setId(TMFactory.SHIPMENT_ID + index);
        return transportation;
    }

    private void mockL4sSearchOutbounds() {
        var readyForConfirmationL4sOutbound = L4ShopsFactory.outbound(
            TMFactory.outboundId(4),
            List.of("1", "2"),
            null
        );

        var confirmedL4sOutbound = L4ShopsFactory.outbound(
            TMFactory.outboundId(2),
            List.of("1", "2"),
            "2021-03-04T15:39:00Z"
        );

        var secondConfirmedL4sOutbound = L4ShopsFactory.outbound(
            TMFactory.outboundId(6),
            List.of("1", "2"),
            "2021-03-04T15:38:00Z"
        );

        when(outboundApi.searchOutbounds(safeRefEq(new OutboundsSearchRequest().yandexIds(List.of(
            TMFactory.outboundId(),
            TMFactory.outboundId(1),
            TMFactory.outboundId(2),
            TMFactory.outboundId(3),
            TMFactory.outboundId(4),
            TMFactory.outboundId(5),
            TMFactory.outboundId(6)
        )))))
            .thenReturn(new OutboundsListDto().outbounds(List.of(
                readyForConfirmationL4sOutbound,
                confirmedL4sOutbound,
                secondConfirmedL4sOutbound
            )));
    }

    private void mockTransportationStatusHistory() {
        when(transportManagerClient.getTransportationsStatusHistory(
            new TransportationStatusHistoryInfoRequestDto()
                .setTransportationIds(List.of(
                    TMFactory.SHIPMENT_ID,
                    TMFactory.SHIPMENT_ID + 1,
                    TMFactory.SHIPMENT_ID + 2,
                    TMFactory.SHIPMENT_ID + 3,
                    TMFactory.SHIPMENT_ID + 4,
                    TMFactory.SHIPMENT_ID + 5,
                    TMFactory.SHIPMENT_ID + 6
                ))
        )).thenReturn(List.of(
            new TransportationStatusHistoryInfoDto()
                .setTransportationId(TMFactory.SHIPMENT_ID)
                .setStatusHistoryList(List.of(
                    new StatusHistoryInfoDto()
                        .setNewStatus("OUTBOUND_CREATED")
                        .setChangedAt(Instant.parse("2021-05-02T01:00:00Z"))
                )),
            new TransportationStatusHistoryInfoDto()
                .setTransportationId(TMFactory.SHIPMENT_ID + 1)
                .setStatusHistoryList(List.of(
                    new StatusHistoryInfoDto()
                        .setNewStatus("DEPARTED")
                        .setChangedAt(Instant.parse("2021-05-02T01:00:00Z"))
                )),
            new TransportationStatusHistoryInfoDto()
                .setTransportationId(TMFactory.SHIPMENT_ID + 3)
                .setStatusHistoryList(List.of(
                    new StatusHistoryInfoDto()
                        .setNewStatus("ERROR")
                        .setChangedAt(Instant.parse("2021-05-02T01:00:00Z"))
                )),
            new TransportationStatusHistoryInfoDto()
                .setTransportationId(TMFactory.SHIPMENT_ID + 5)
                .setStatusHistoryList(List.of(
                    new StatusHistoryInfoDto()
                        .setNewStatus("ERROR")
                        .setChangedAt(Instant.parse("2021-05-02T01:00:00Z"))
                ))
        ));
    }

    @Nonnull
    private static PartnerShipmentFilterBuilder shipmentFilter() {
        return PartnerShipmentFilter.builder()
            .dateFrom(DATE_FROM)
            .dateTo(DATE_TO);
    }

    @Nonnull
    private static TransportationSearchFilterBuilder transportationFilter() {
        return TransportationSearchFilter.builder()
            .outboundPartnerIds(Set.of(TMFactory.PARTNER_ID, TMFactory.SECOND_PARTNER_ID))
            .outboundDateFrom(DATE_FROM)
            .outboundDateTo(DATE_TO);
    }

    @Nonnull
    private List<TransportationSearchDto> defaultSearchResults(int size) {
        return IntStream.range(0, size)
            .mapToObj(TMFactory::transportationSearch)
            .collect(Collectors.toList());
    }

    @Nonnull
    private ResultActions searchShipments(PartnerShipmentFilter filter) throws Exception {
        return searchShipments(filter, 0, 10);
    }

    @Nonnull
    private ResultActions searchShipments(PartnerShipmentFilter filter, int page, int pageSize) throws Exception {
        return searchShipments(filter, SHOP_ID, page, pageSize);
    }

    @Nonnull
    private ResultActions searchShipments(
        PartnerShipmentFilter filter,
        long shopId,
        int page,
        int pageSize
    ) throws Exception {
        return mockMvc.perform(
            request(HttpMethod.PUT, url(), filter)
                .param("userId", "100")
                .param("shopId", String.valueOf(shopId))
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(pageSize))
        );
    }

    private void verifyLmsClient() {
        verify(lmsClient).searchPartners(
            SearchPartnerFilter.builder()
                .setPartnerSubTypeIds(Set.of(EXPRESS_PARTNER_SUBTYPE))
                .build()
        );
        verify(lmsClient).getLogisticsPoints(
            LogisticsPointFilter.newBuilder()
                .ids(Set.of(TMFactory.WAREHOUSE_FROM, TMFactory.WAREHOUSE_TO))
                .build()
        );
        verify(lmsClient).searchPartnerRelation(
            PartnerRelationFilter.newBuilder()
                .fromPartnersIds(Set.of(TMFactory.PARTNER_ID))
                .build()
        );
        verify(lmsClient).getWarehouseHandlingDuration(TMFactory.PARTNER_ID);
    }

    @Nonnull
    protected abstract String url();

    protected abstract void mockOutbounds(int size, List<Long> orderIds);

    protected abstract void mockOutbounds(int size, String outboundId, List<Long> orderIds, String confirmed);

    protected abstract void mockOutbounds(
        int size,
        String outboundId,
        List<Long> orderIds,
        String confirmed,
        String externalId
    );

    protected abstract void verifyGetOutbounds(int size);
}
