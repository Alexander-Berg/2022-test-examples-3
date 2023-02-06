package ru.yandex.market.logistics.lom.client;

import java.time.Instant;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.lom.model.dto.ContactDto;
import ru.yandex.market.logistics.lom.model.dto.OrderIdWaybillDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentStatusHistoryAdditionalInfoDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentStatusHistoryDto;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.logistics.lom.model.search.Pageable;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static ru.yandex.market.logistics.lom.model.dto.DtoBuilderFactory.orderSearchFilterBuilder;

class OrderWaybillSearchTest extends AbstractClientTest {
    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Поиск сегментов заказов с историей статусов")
    void searchOrders() {
        prepareMockRequest(
            HttpMethod.PUT,
            "/orders/searchOrderSegments",
            "request/order/search.json",
            "response/order/segments/response/search.json"
        )
            .andExpect(queryParam("size", "10"))
            .andExpect(queryParam("page", "0"));
        List<OrderIdWaybillDto> result = lomClient.searchOrdersSegments(
            orderSearchFilterBuilder().build(),
            new Pageable(0, 10, null)
        );

        softly.assertThat(result).usingRecursiveComparison().isEqualTo(expected());
    }

    @Nonnull
    private List<OrderIdWaybillDto> expected() {
        return List.of(
            OrderIdWaybillDto.builder()
                .orderId(1L)
                .senderId(3L)
                .waybill(List.of(
                    baseSegment(6, 1, SegmentType.COURIER)
                        .partnerType(PartnerType.OWN_DELIVERY)
                        .trackerId(1L)
                        .segmentType(SegmentType.COURIER)
                        .rootStorageUnitExternalId("root-external-id")
                        .segmentStatus(SegmentStatus.INFO_RECEIVED)
                        .courier(new WaybillSegmentDto.CourierDto(ContactDto.builder().lastName("lastName").build()))
                        .build(),
                    baseSegment(7, 1, SegmentType.GO_PLATFORM)
                        .partnerType(PartnerType.OWN_DELIVERY)
                        .trackerId(1L)
                        .rootStorageUnitExternalId("root-external-id")
                        .segmentStatus(SegmentStatus.INFO_RECEIVED)
                        .build(),
                    baseSegment(8, 1, SegmentType.GO_PLATFORM)
                        .partnerType(PartnerType.OWN_DELIVERY)
                        .trackerId(1L)
                        .segmentStatus(SegmentStatus.STARTED)
                        .rootStorageUnitExternalId("root-external-id")
                        .waybillSegmentStatusHistory(List.of(
                            WaybillSegmentStatusHistoryDto.builder()
                                .id(12L)
                                .status(SegmentStatus.IN)
                                .trackerStatus("test_status")
                                .created(Instant.parse("2022-07-25T21:12:10.00Z"))
                                .date(Instant.parse("2022-07-25T21:12:11.00Z"))
                                .additionalData(
                                    WaybillSegmentStatusHistoryAdditionalInfoDto.builder()
                                        .country("test_country")
                                        .city("test_city")
                                        .location("test_location")
                                        .zipCode("test_zipcode")
                                        .build()
                                )
                                .build()
                        ))
                        .build()
                ))
                .build(),
            OrderIdWaybillDto.builder()
                .senderId(5L)
                .orderId(4L)
                .waybill(List.of(baseSegment(5, 70, SegmentType.COURIER).build()))
                .build(),
            OrderIdWaybillDto.builder().orderId(5L).waybill(List.of()).build()
        );
    }

    @Nonnull
    private WaybillSegmentDto.WaybillSegmentDtoBuilder baseSegment(long id, long partnerId, SegmentType segmentType) {
        return WaybillSegmentDto.builder()
            .id(id)
            .options(List.of())
            .partnerId(partnerId)
            .segmentType(segmentType)
            .waybillSegmentStatusHistory(List.of())
            .waybillSegmentTags(List.of());
    }
}
