package ru.yandex.market.logistics.nesu.base;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import NSprav.UnifierReplyOuterClass.UnifierReply;
import lombok.extern.slf4j.Slf4j;

import ru.yandex.altay.model.SignalOuterClass;
import ru.yandex.altay.model.tds.TDSEnums;
import ru.yandex.market.logistics.delivery.calculator.client.DeliveryCalculatorSearchEngineClient;
import ru.yandex.market.logistics.delivery.calculator.client.model.DeliveryOption;
import ru.yandex.market.logistics.delivery.calculator.client.model.DeliveryOptionService;
import ru.yandex.market.logistics.delivery.calculator.client.model.DeliverySearchRequest;
import ru.yandex.market.logistics.delivery.calculator.client.model.DeliverySearchResponse;
import ru.yandex.market.logistics.delivery.calculator.client.model.TariffType;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.schedule.CourierScheduleFilter;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.nesu.model.DeliveryOptionFactory;
import ru.yandex.market.logistics.nesu.model.LmsFactory;

import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;

@Slf4j
public final class OrderTestUtils {

    private OrderTestUtils() {
        throw new UnsupportedOperationException();
    }

    public static void mockDeliveryOptionValidation(
        int maxDeliveryDays,
        DeliveryCalculatorSearchEngineClient calculatorClient,
        LMSClient lmsClient
    ) {
        mockDeliveryOptionValidation(
            maxDeliveryDays,
            calculatorClient,
            lmsClient,
            defaultDeliverySearchRequestBuilder()
                .build()
        );
    }

    public static void mockDeliveryOptionValidation(
        int maxDeliveryDays,
        DeliveryCalculatorSearchEngineClient calculatorClient,
        LMSClient lmsClient,
        DeliverySearchRequest optionsFilter
    ) {
        log.info("Mock delivery option validation for request {}", optionsFilter);
        doReturn(
            DeliverySearchResponse.builder()
                .deliveryOptions(List.of(
                    DeliveryOption.builder()
                        .cost(100)
                        .deliveryServiceId(5L)
                        .tariffType(TariffType.COURIER)
                        .maxDays(maxDeliveryDays)
                        .services(defaultDeliveryOptionServices(100))
                        .build()
                ))
                .build()
        ).when(calculatorClient).deliverySearch(safeRefEq(optionsFilter));

        when(lmsClient.searchPartners(LmsFactory.createPartnerFilter(Set.of(5L), null, Set.of(PartnerStatus.ACTIVE))))
            .thenReturn(List.of(LmsFactory.createPartner(5L, PartnerType.DELIVERY)));
        when(lmsClient.getPartner(6L))
            .thenReturn(Optional.of(LmsFactory.createPartner(6L, 101L, PartnerType.SORTING_CENTER)));
    }

    @Nonnull
    public static List<DeliveryOptionService> defaultDeliveryOptionServices(int deliveryCost) {
        return defaultDeliveryOptionServices(deliveryCost, 0.017);
    }

    @Nonnull
    public static List<DeliveryOptionService> defaultDeliveryOptionServices(
        int deliveryCost,
        double cashServicePercent
    ) {
        return List.of(
            DeliveryOptionFactory.deliveryServiceBuilder()
                .minPrice(deliveryCost)
                .priceCalculationParameter(deliveryCost)
                .maxPrice(deliveryCost)
                .build(),
            DeliveryOptionFactory.insuranceServiceBuilder().build(),
            DeliveryOptionFactory.cashServiceBuilder()
                .priceCalculationParameter(cashServicePercent)
                .build(),
            DeliveryOptionFactory.returnServiceBuilder().build()
        );
    }

    @Nonnull
    public static DeliverySearchRequest.DeliverySearchRequestBuilder defaultDeliverySearchRequestBuilder() {
        return DeliverySearchRequest.builder()
            .locationFrom(213)
            .locationsTo(Set.of(42))
            .weight(BigDecimal.valueOf(50))
            .length(45)
            .width(30)
            .height(15)
            .deliveryServiceIds(Set.of(5L, 53916L, 45L))
            .tariffId(33L)
            .senderId(1L)
            .offerPrice(20000L)
            .tariffType(TariffType.COURIER)
            .pickupPoints(Set.of(101L));
    }

    public static void mockCourierSchedule(LMSClient lmsClient, int locationId, Set<Long> partnerIds) {
        mockCourierSchedule(lmsClient, Set.of(locationId), partnerIds);
    }

    public static void mockCourierSchedule(LMSClient lmsClient, Set<Integer> locationIds, Set<Long> partnerIds) {
        mockCourierSchedule(lmsClient, locationIds, partnerIds, Set.of(DayOfWeek.TUESDAY));
    }

    public static void mockCourierSchedule(
        LMSClient lmsClient,
        Set<Integer> locationIds,
        Set<Long> partnerIds,
        Set<DayOfWeek> daysOfWeek
    ) {
        when(lmsClient.getCourierScheduleDays(refEq(
            CourierScheduleFilter.newBuilder().partnerIds(partnerIds).locationIds(locationIds).build())
        ))
            .thenReturn(
                partnerIds.stream()
                    .map(partnerId -> LmsFactory.createCourierSchedule(
                        partnerId,
                        locationIds.stream().mapToInt(e -> e).max().orElse(225),
                        daysOfWeek
                    ))
                    .collect(Collectors.toList())
            );
    }

    public static UnifierReply getSimpleUnifierReplyWithAddress(SignalOuterClass.Address address) {
        return UnifierReply.newBuilder()
            .addAddress(address)
            .setSuccess(true)
            .build();
    }

    public static SignalOuterClass.Address.Builder getDefaultAddressBuilder() {
        return SignalOuterClass.Address.newBuilder()
            .setGeoId(213L)
            .setZipCode("121099")
            .setCoordinates(
                SignalOuterClass.Coordinates.newBuilder().setLon(37.5846221554295).setLat(55.7513100141919).build()
            )
            .setSource(SignalOuterClass.Address.Source.ONELINE)
            .setRegionCode("RU")
            .addAdHierarchy(
                SignalOuterClass.Toponym.newBuilder()
                    .setKind(SignalOuterClass.Toponym.Kind.COUNTRY)
                    .setName("Россия")
            )
            .addAdHierarchy(
                SignalOuterClass.Toponym.newBuilder()
                    .setKind(SignalOuterClass.Toponym.Kind.PROVINCE)
                    .setName("Центральный федеральный округ")
            )
            .addAdHierarchy(
                SignalOuterClass.Toponym.newBuilder()
                    .setKind(SignalOuterClass.Toponym.Kind.PROVINCE)
                    .setName("Москва")
            )
            .addAdHierarchy(
                SignalOuterClass.Toponym.newBuilder()
                    .setKind(SignalOuterClass.Toponym.Kind.LOCALITY)
                    .setName("Москва")
            )
            .addAdHierarchy(
                SignalOuterClass.Toponym.newBuilder()
                    .setKind(SignalOuterClass.Toponym.Kind.STREET)
                    .setName("Новинский бульвар")
            )
            .addAdHierarchy(
                SignalOuterClass.Toponym.newBuilder()
                    .setKind(SignalOuterClass.Toponym.Kind.HOUSE)
                    .setName("8")
            )
            .addAddInfoItems(
                SignalOuterClass.AddInfoItem.newBuilder()
                    .setType(TDSEnums.AddressInfoComponentType.RoomComponent)
                    .setValue("5.11")
                    .setKind("кабинет")
            );
    }
}
