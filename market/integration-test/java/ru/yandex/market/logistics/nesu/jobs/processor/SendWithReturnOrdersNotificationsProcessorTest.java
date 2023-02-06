package ru.yandex.market.logistics.nesu.jobs.processor;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import one.util.streamex.EntryStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.AddressDto;
import ru.yandex.market.logistics.lom.model.dto.LocationDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.OrderIdWaybillDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentStatusHistoryDto;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.model.ShopIdsPayload;
import ru.yandex.market.logistics.nesu.jobs.producer.SendNotificationToShopProducer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DatabaseSetup("/jobs/executors/database_prepare.xml")
@DisplayName("Оповещения о наличии заказов, ожидающих возврата")
@ParametersAreNonnullByDefault
class SendWithReturnOrdersNotificationsProcessorTest extends AbstractContextualTest {
    private static final int MBI_NOTIFICATION_SHOP_RETURN_ORDERS_ID = 1579187364;
    private static final Instant INSTANT = Instant.parse("2020-06-20T12:00:00Z");
    protected static final Set<Long> ALL_SENDER_IDS = Set.of(1L, 2L, 3L);
    protected static final List<Long> ALL_SHOP_IDS = List.of(1L, 2L);

    @Autowired
    private LomClient lomClient;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private SendNotificationToShopProducer sendNotificationToShopProducer;

    @Autowired
    private SendReturnOrdersNotificationsProcessor shopsWithReturnOrdersNotificationsProcessor;

    @BeforeEach
    void setup() {
        doNothing().when(sendNotificationToShopProducer)
            .produceTask(anyInt(), anyLong(), isNull(), anyString());
        clock.setFixed(INSTANT, ZoneId.systemDefault());
    }

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(sendNotificationToShopProducer, lomClient, lmsClient);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("ordersWithoutNotificationSource")
    @DisplayName("Уведомление не отправляется")
    void ordersWithoutNotification(@SuppressWarnings("unused") String displayName, List<OrderDto> ordersBySearch) {
        mockLomWithOrders(ordersBySearch, ALL_SENDER_IDS);
        shopsWithReturnOrdersNotificationsProcessor.processPayload(new ShopIdsPayload("1", ALL_SHOP_IDS));
        verify(lomClient).searchOrdersSegments(createOrderSearchFilter(ALL_SENDER_IDS), Pageable.unpaged());
    }

    @Nonnull
    private static Stream<Arguments> ordersWithoutNotificationSource() {
        return Stream.of(
            Arguments.of(
                "Не нашлись заказы по переданному критерию",
                List.of()
            ),
            Arguments.of(
                "Нашлись заказы с нужными статусами, но проставлены недавно",
                List.of(orderIdWaybillDto(1, 1, 1, Map.of(SegmentStatus.RETURN_ARRIVED, 2)))
            ),
            Arguments.of(
                "Нашлись заказы только в 160 статусе, проставлен давно",
                List.of(orderIdWaybillDto(1, 1, 1, Map.of(SegmentStatus.RETURN_PREPARING, 10)))
            ),
            Arguments.of(
                "Нашелся подходящий заказ, но у него уже давно был проставлен 180 статус",
                List.of(orderIdWaybillDto(
                    1,
                    1,
                    1,
                    Map.of(
                        SegmentStatus.RETURN_ARRIVED, 7,
                        SegmentStatus.RETURN_PREPARING_SENDER, 7,
                        SegmentStatus.RETURNED, 7
                    )
                ))
            ),
            Arguments.of(
                "Есть возвратный заказ, но слишком давний",
                List.of(orderIdWaybillDto(1, 1, 1, Map.of(SegmentStatus.RETURN_ARRIVED, 100)))
            )
        );
    }

    @Test
    @DisplayName("Нашлись просроченные заказы у разных сендеров")
    void foundLongLasting() {
        mockLomWithOrdersWithSegments(
            List.of(
                orderIdWaybillDto(
                    2,
                    3,
                    1,
                    Map.of(SegmentStatus.RETURN_ARRIVED, 1, SegmentStatus.RETURN_PREPARING_SENDER, 0)
                ),
                orderIdWaybillDto(3, 2, 1, Map.of(SegmentStatus.RETURN_ARRIVED, 7)),
                orderIdWaybillDto(1, 1, 1, Map.of(SegmentStatus.RETURN_PREPARING_SENDER, 6))
            ),
            SendWithReturnOrdersNotificationsProcessorTest.ALL_SENDER_IDS
        );

        Set<Long> sortingCentersIds = Set.of(1L);
        mockSortingCentersSearch(sortingCentersIds);
        mockPartnersSearch(sortingCentersIds);

        shopsWithReturnOrdersNotificationsProcessor.processPayload(new ShopIdsPayload("1", List.of(1L, 2L)));

        ArgumentCaptor<String> xmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendNotificationToShopProducer).produceTask(
            eq(MBI_NOTIFICATION_SHOP_RETURN_ORDERS_ID),
            eq(1L),
            isNull(),
            xmlCaptor.capture()
        );
        softly.assertThat(xmlCaptor.getValue())
            .isXmlEqualTo(extractFileContent("jobs/executors/return_orders/orders_for_shop_1_sc_1.xml"));
        verify(sendNotificationToShopProducer).produceTask(
            eq(MBI_NOTIFICATION_SHOP_RETURN_ORDERS_ID),
            eq(2L),
            isNull(),
            xmlCaptor.capture()
        );
        softly.assertThat(xmlCaptor.getValue())
            .isXmlEqualTo(extractFileContent("jobs/executors/return_orders/orders_for_shop_2_sc_1.xml"));

        verify(lomClient).searchOrdersSegments(createOrderSearchFilter(ALL_SENDER_IDS), Pageable.unpaged());
        verify(lmsClient).getLogisticsPoints(eq(createLogisticsPointFilter(sortingCentersIds)));
        verify(lmsClient).searchPartners(eq(createSearchPartnerFilter(sortingCentersIds)));
    }

    @Test
    @DisplayName("Нашлись просроченные заказы с фильтром по сендерам")
    void foundLongLastingWithSendersFilter() {

        mockLomWithOrdersWithSegments(
            List.of(
                orderIdWaybillDto(3, 2, 1, Map.of(SegmentStatus.RETURN_ARRIVED, 7))
            ),
            Set.of(2L)
        );

        Set<Long> sortingCentersIds = Set.of(1L);
        mockSortingCentersSearch(sortingCentersIds);
        mockPartnersSearch(sortingCentersIds);

        shopsWithReturnOrdersNotificationsProcessor.processPayload(new ShopIdsPayload("1", List.of(2L)));

        ArgumentCaptor<String> xmlCaptor = ArgumentCaptor.forClass(String.class);

        verify(sendNotificationToShopProducer).produceTask(
            eq(MBI_NOTIFICATION_SHOP_RETURN_ORDERS_ID),
            eq(2L),
            isNull(),
            xmlCaptor.capture()
        );

        softly.assertThat(xmlCaptor.getValue())
            .isXmlEqualTo(extractFileContent("jobs/executors/return_orders/orders_for_shop_2_sc_1.xml"));

        verify(lomClient).searchOrdersSegments(createOrderSearchFilter(Set.of(2L)), Pageable.unpaged());
        verify(lmsClient).getLogisticsPoints(eq(createLogisticsPointFilter(sortingCentersIds)));
        verify(lmsClient).searchPartners(eq(createSearchPartnerFilter(sortingCentersIds)));
    }

    @Test
    @DisplayName("У одного магазина заказы в разных СЦ")
    void severalSortingCenters() {
        mockLomWithOrdersWithSegments(List.of(
            orderIdWaybillDto(
                2,
                3,
                1,
                Map.of(SegmentStatus.RETURN_ARRIVED, 1, SegmentStatus.RETURN_PREPARING_SENDER, 0)
            ),
            orderIdWaybillDto(4, 1, 2, Map.of(SegmentStatus.RETURN_ARRIVED, 6)),
            orderIdWaybillDto(
                1,
                1,
                1,
                Map.of(SegmentStatus.RETURN_ARRIVED, 6, SegmentStatus.RETURN_PREPARING_SENDER, 5)
            ),
            orderIdWaybillDto(
                5,
                3,
                3,
                Map.of(SegmentStatus.RETURN_ARRIVED, 4, SegmentStatus.RETURN_PREPARING_SENDER, 3)
            )
        ), ALL_SENDER_IDS);

        Set<Long> sortingCentersIds = Set.of(1L, 2L, 3L);
        mockSortingCentersSearch(sortingCentersIds);
        mockPartnersSearch(sortingCentersIds);

        shopsWithReturnOrdersNotificationsProcessor.processPayload(new ShopIdsPayload("1", ALL_SHOP_IDS));

        ArgumentCaptor<String> xmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendNotificationToShopProducer, times(2)).produceTask(
            eq(MBI_NOTIFICATION_SHOP_RETURN_ORDERS_ID),
            eq(1L),
            isNull(),
            xmlCaptor.capture()
        );
        softly.assertThat(xmlCaptor.getAllValues())
            .hasSize(2)
            .anySatisfy(
                xml -> assertThat(xml)
                    .isXmlEqualTo(extractFileContent("jobs/executors/return_orders/orders_for_shop_1_sc_1.xml"))
            )
            .anySatisfy(
                xml -> assertThat(xml)
                    .isXmlEqualTo(extractFileContent("jobs/executors/return_orders/orders_for_shop_1_sc_2.xml"))
            );

        verify(lomClient).searchOrdersSegments(createOrderSearchFilter(ALL_SENDER_IDS), Pageable.unpaged());
        verify(lmsClient).getLogisticsPoints(eq(createLogisticsPointFilter(sortingCentersIds)));
        verify(lmsClient).searchPartners(eq(createSearchPartnerFilter(sortingCentersIds)));
    }

    @Test
    @DisplayName("Проверка работы с 160 статусами")
    void workWithStatus160() {
        mockLomWithOrdersWithSegments(List.of(
            orderIdWaybillDto(6, 1, 2, Map.of(SegmentStatus.RETURN_PREPARING, 6)),
            orderIdWaybillDto(
                7,
                1,
                2,
                Map.of(SegmentStatus.RETURN_PREPARING, 6, SegmentStatus.RETURN_ARRIVED, 4)
            ),
            orderIdWaybillDto(
                8,
                1,
                2,
                Map.of(
                    SegmentStatus.RETURN_PREPARING, 6,
                    SegmentStatus.RETURN_ARRIVED, 4,
                    SegmentStatus.RETURN_PREPARING_SENDER, 3
                )
            ),
            orderIdWaybillDto(
                9,
                1,
                3,
                Map.of(
                    SegmentStatus.RETURN_PREPARING, 4,
                    SegmentStatus.RETURN_ARRIVED, 8,
                    SegmentStatus.RETURN_PREPARING_SENDER, 7
                )
            ),
            orderIdWaybillDto(
                10,
                1,
                3,
                Map.of(SegmentStatus.RETURN_PREPARING, 4, SegmentStatus.RETURN_PREPARING_SENDER, 7)
            )
        ), ALL_SENDER_IDS);

        Set<Long> sortingCentersIds = Set.of(2L, 3L);
        mockSortingCentersSearch(sortingCentersIds);
        mockPartnersSearch(sortingCentersIds);

        shopsWithReturnOrdersNotificationsProcessor.processPayload(new ShopIdsPayload("1", ALL_SHOP_IDS));

        ArgumentCaptor<String> xmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendNotificationToShopProducer).produceTask(
            eq(MBI_NOTIFICATION_SHOP_RETURN_ORDERS_ID),
            eq(1L),
            isNull(),
            xmlCaptor.capture()
        );
        softly.assertThat(xmlCaptor.getValue())
            .isXmlEqualTo(extractFileContent("jobs/executors/return_orders/orders_for_shop_1_sc_3.xml"));

        verify(lomClient).searchOrdersSegments(createOrderSearchFilter(ALL_SENDER_IDS), Pageable.unpaged());
        verify(lmsClient).getLogisticsPoints(eq(createLogisticsPointFilter(sortingCentersIds)));
        verify(lmsClient).searchPartners(eq(createSearchPartnerFilter(sortingCentersIds)));
    }

    @Test
    @DisplayName("Сумма за хранение ненулевая")
    void storagePaymentCalculation() {
        mockLomWithOrdersWithSegments(List.of(
            orderIdWaybillDto(
                5,
                2,
                2,
                Map.of(
                    SegmentStatus.RETURN_PREPARING, 22,
                    SegmentStatus.RETURN_ARRIVED, 20,
                    SegmentStatus.RETURN_PREPARING_SENDER, 19
                )
            ),
            orderIdWaybillDto(1, 2, 2, Map.of(SegmentStatus.RETURN_ARRIVED, 100))
        ), ALL_SENDER_IDS);

        Set<Long> sortingCentersIds = Set.of(2L);
        mockSortingCentersSearch(sortingCentersIds);
        mockPartnersSearch(sortingCentersIds);

        shopsWithReturnOrdersNotificationsProcessor.processPayload(new ShopIdsPayload("1", ALL_SHOP_IDS));

        ArgumentCaptor<String> xmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendNotificationToShopProducer).produceTask(
            eq(MBI_NOTIFICATION_SHOP_RETURN_ORDERS_ID),
            eq(2L),
            isNull(),
            xmlCaptor.capture()
        );
        softly.assertThat(xmlCaptor.getValue())
            .isXmlEqualTo(extractFileContent("jobs/executors/return_orders/orders_for_shop_2_sc_2.xml"));

        verify(lomClient).searchOrdersSegments(createOrderSearchFilter(ALL_SENDER_IDS), Pageable.unpaged());
        verify(lmsClient).getLogisticsPoints(eq(createLogisticsPointFilter(sortingCentersIds)));
        verify(lmsClient).searchPartners(eq(createSearchPartnerFilter(sortingCentersIds)));
    }

    @Test
    @DisplayName("LMS вернул не все СЦ или не всех партнеров")
    void missingSortingCenterAndPartnerFromLms() {
        mockLomWithOrdersWithSegments(List.of(
            orderIdWaybillDto(
                2,
                3,
                1,
                Map.of(SegmentStatus.RETURN_ARRIVED, 1, SegmentStatus.RETURN_PREPARING_SENDER, 0)
            ),
            orderIdWaybillDto(4, 1, 2, Map.of(SegmentStatus.RETURN_ARRIVED, 6)),
            orderIdWaybillDto(
                1,
                1,
                1,
                Map.of(SegmentStatus.RETURN_ARRIVED, 6, SegmentStatus.RETURN_PREPARING_SENDER, 5)
            ),
            orderIdWaybillDto(
                5,
                3,
                3,
                Map.of(SegmentStatus.RETURN_ARRIVED, 4, SegmentStatus.RETURN_PREPARING_SENDER, 3)
            )
        ), ALL_SENDER_IDS);

        Set<Long> requestSortingCentersIds = Set.of(1L, 2L, 3L);
        Set<Long> responseSortingCentersIds = Set.of(1L);
        mockSortingCentersSearch(requestSortingCentersIds, responseSortingCentersIds);

        shopsWithReturnOrdersNotificationsProcessor.processPayload(new ShopIdsPayload("1", ALL_SHOP_IDS));

        ArgumentCaptor<String> xmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendNotificationToShopProducer, times(2)).produceTask(
            eq(MBI_NOTIFICATION_SHOP_RETURN_ORDERS_ID),
            eq(1L),
            isNull(),
            xmlCaptor.capture()
        );
        softly.assertThat(xmlCaptor.getAllValues())
            .hasSize(2)
            .anySatisfy(
                xml -> assertThat(xml)
                    .isXmlEqualTo(extractFileContent("jobs/executors/return_orders/orders_for_shop_1_sc_1.xml"))
            )
            .anySatisfy(
                xml -> assertThat(xml)
                    .isXmlEqualTo(extractFileContent(
                        "jobs/executors/return_orders/orders_for_shop_1_missing_sc_2.xml")
                    )
            );

        verify(lomClient).searchOrdersSegments(createOrderSearchFilter(ALL_SENDER_IDS), Pageable.unpaged());
        verify(lmsClient).getLogisticsPoints(eq(createLogisticsPointFilter(requestSortingCentersIds)));
        verify(lmsClient).searchPartners(eq(createSearchPartnerFilter(responseSortingCentersIds)));
    }

    private void mockLomWithOrders(List<OrderDto> returnOrders, Set<Long> senderIds) {
        when(lomClient.searchOrders(createOrderSearchFilter(senderIds), Pageable.unpaged()))
            .thenReturn(
                new PageResult<OrderDto>()
                    .setData(returnOrders)
                    .setTotalElements(returnOrders.size())
                    .setPageNumber(0)
                    .setSize(0)
                    .setTotalPages(1)
            );
    }

    private void mockLomWithOrdersWithSegments(List<OrderIdWaybillDto> returnOrders, Set<Long> senderIds) {
        when(lomClient.searchOrdersSegments(
            createOrderSearchFilter(senderIds),
            Pageable.unpaged()
        ))
            .thenReturn(returnOrders);
    }

    @Nonnull
    private static OrderIdWaybillDto orderIdWaybillDto(
        long orderId,
        long senderId,
        long sortingCenterId,
        Map<SegmentStatus, Integer> statusToDaysMap
    ) {
        return OrderIdWaybillDto.builder()
            .orderId(orderId)
            .senderId(senderId)
            .waybill(waybillWithLocalSc(sortingCenterId, statusToDaysMap))
            .build();
    }

    @Nonnull
    private static List<WaybillSegmentDto> waybillWithLocalSc(
        long sortingCenterId,
        Map<SegmentStatus, Integer> statusToDaysMap
    ) {
        return List.of(
            WaybillSegmentDto.builder()
                .partnerType(PartnerType.SORTING_CENTER)
                .partnerId(sortingCenterId)
                .partnerName("sorting-center-name-" + sortingCenterId)
                .shipment(
                    WaybillSegmentDto.ShipmentDto.builder()
                        .locationTo(
                            LocationDto.builder()
                                .warehouseId(sortingCenterId)
                                .address(
                                    AddressDto.builder()
                                        .zipCode("606060")
                                        .country("Russia")
                                        .federalDistrict("Fed Dis")
                                        .region("Region")
                                        .locality("City")
                                        .street("street")
                                        .house(String.valueOf(sortingCenterId))
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .segmentStatus(SegmentStatus.RETURN_PREPARING_SENDER)
                .waybillSegmentStatusHistory(getWaybillHistory(statusToDaysMap))
                .build(),
            WaybillSegmentDto.builder()
                .partnerType(PartnerType.SORTING_CENTER)
                .partnerId(2L)
                .segmentStatus(SegmentStatus.RETURN_PREPARING_SENDER)
                .waybillSegmentStatusHistory(getWaybillHistory(Map.of(SegmentStatus.RETURN_ARRIVED, 7)))
                .build(),
            WaybillSegmentDto.builder()
                .partnerId(3L)
                .partnerType(PartnerType.DELIVERY)
                .segmentType(SegmentType.MOVEMENT)
                .build()
        );
    }

    @Nonnull
    private static List<WaybillSegmentStatusHistoryDto> getWaybillHistory(Map<SegmentStatus, Integer> statusToDaysMap) {
        return EntryStream.of(statusToDaysMap)
            .filterValues(Objects::nonNull)
            .mapKeyValue((status, daysInStatus) ->
                WaybillSegmentStatusHistoryDto.builder()
                    .date(INSTANT.minus(Duration.ofDays(daysInStatus)))
                    .status(status)
                    .build()
            )
            // Имитируем сортировку по дате со стороны лома
            .sorted(Comparator.comparing(WaybillSegmentStatusHistoryDto::getDate).reversed())
            .collect(Collectors.toList());
    }

    @Nonnull
    private OrderSearchFilter createOrderSearchFilter(Set<Long> senderIds) {
        return OrderSearchFilter.builder()
            .platformClientId(3L)
            .segmentStatuses(Map.of(
                PartnerType.SORTING_CENTER,
                EnumSet.of(
                    SegmentStatus.RETURN_PREPARING_SENDER,
                    SegmentStatus.RETURN_ARRIVED,
                    SegmentStatus.RETURN_PREPARING
                )
            ))
            .senderIds(senderIds)
            .statuses(Set.of(
                OrderStatus.PROCESSING,
                OrderStatus.RETURNING,
                OrderStatus.RETURNED
            ))
            .build();
    }

    private void mockSortingCentersSearch(Set<Long> sortingCentersIds) {
        mockSortingCentersSearch(sortingCentersIds, sortingCentersIds);
    }

    private void mockSortingCentersSearch(Set<Long> requestSortingCentersIds, Set<Long> responseSortingCentersIds) {
        List<LogisticsPointResponse> response = responseSortingCentersIds.stream()
            .map(id -> LogisticsPointResponse.newBuilder()
                .id(id)
                .address(
                    Address.newBuilder()
                        .postCode("606060")
                        .country("Russia")
                        .region("Region")
                        .street("street")
                        .house(String.valueOf(id))
                        .build()
                )
                .partnerId(id)
                .build()
            )
            .collect(Collectors.toList());

        when(lmsClient.getLogisticsPoints(createLogisticsPointFilter(requestSortingCentersIds)))
            .thenReturn(response);
    }

    private void mockPartnersSearch(Set<Long> partnerIds) {
        List<PartnerResponse> response = partnerIds.stream()
            .map(id -> PartnerResponse.newBuilder()
                .id(id)
                .readableName("sorting-center-name-" + id)
                .build()
            )
            .collect(Collectors.toList());

        when(lmsClient.searchPartners(createSearchPartnerFilter(partnerIds)))
            .thenReturn(response);
    }

    @Nonnull
    private LogisticsPointFilter createLogisticsPointFilter(Set<Long> sortingCentersIds) {
        return LogisticsPointFilter.newBuilder()
            .ids(sortingCentersIds)
            .build();
    }

    @Nonnull
    private SearchPartnerFilter createSearchPartnerFilter(Set<Long> partnerIds) {
        return SearchPartnerFilter.builder().setIds(partnerIds).build();
    }
}
