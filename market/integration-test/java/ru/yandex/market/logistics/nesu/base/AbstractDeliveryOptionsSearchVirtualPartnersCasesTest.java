package ru.yandex.market.logistics.nesu.base;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.common.geocoder.model.response.Component;
import ru.yandex.common.geocoder.model.response.GeoObject;
import ru.yandex.common.geocoder.model.response.Kind;
import ru.yandex.market.logistics.delivery.calculator.client.model.DeliverySearchRequest;
import ru.yandex.market.logistics.delivery.calculator.client.model.DeliverySearchResponse;
import ru.yandex.market.logistics.delivery.calculator.client.model.TariffType;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.dto.enums.DeliveryType;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.service.lms.PlatformClientId;

import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.base.OrderTestUtils.mockCourierSchedule;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractDeliveryOptionsSearchVirtualPartnersCasesTest
    extends AbstractDeliveryOptionsSearchBaseCasesTest {

    static final String LOTTE_LOCATION_ADDRESS = "Москва, Новинский бульвар 8";
    static final String LOTTE_LOCATION_COORDINATES = "37.584613172276661,55.7513100141919";

    final LogisticsPointResponse sortingCenterWarehouse = warehouse(
        SORTING_CENTER_WAREHOUSE_ID,
        SORTING_CENTER_ID
    );

    @Test
    @DisplayName("Поиск опций для виртуальных СД")
    void virtualDeliveryServices() throws Exception {
        mockVirtualDeliveryService();
        mockGeoServiceLocalityResponse();

        DeliverySearchResponse response = DeliverySearchResponse.builder()
            .deliveryOptions(List.of(deliveryOption(11L, 420L, TariffType.PICKUP, 2)))
            .build();

        doReturn(response)
            .when(deliveryCalculatorClient)
            .deliverySearch(
                defaultCalculatorRequest()
                    .senderId(1000L)
                    .deliveryServiceIds(Set.of(420L, 421L))
                    .locationsTo(Set.of(213, 117065, 20279))
                    .tariffType(TariffType.COURIER)
                    .build()
            );

        mockPickupPointSearch(response);

        search(
            defaultFilter()
                .andThen(f -> f.setTo(createLocation(LOTTE_LOCATION_ADDRESS, null)))
                .andThen(f -> f.setDeliveryType(DeliveryType.COURIER)),
            1000L
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_virtual_delivery_service.json"));

        verify(geoSearchClient).find(LOTTE_LOCATION_ADDRESS);
        verify(geoSearchClient).find(LOTTE_LOCATION_COORDINATES);
        verifyNoMoreInteractions(geoSearchClient);
    }

    @Test
    @DisplayName("Поиск опций для виртуальных СД с указанием желаемой даты доставки. Две волны не должны схлопнуться")
    void virtualDeliveryServicesDesiredDeliveryDate() throws Exception {
        mockVirtualDeliveryService();
        mockGeoServiceLocalityResponse();
        mockCourierSchedule(lmsClient, Set.of(213, 20279, 117065), Set.of(420L, 421L), Set.of(DayOfWeek.values()));

        // Две волны - это два разных партнера в LMS с одинаковой географией доставки по тарифам,
        // но разными интервалами доставки.
        DeliverySearchResponse response = DeliverySearchResponse.builder()
            .deliveryOptions(List.of(
                deliveryOptionBuilder(10L, 420L, TariffType.COURIER, 2).build(),
                deliveryOptionBuilder(11L, 421L, TariffType.COURIER, 2).build()
            ))
            .build();

        doReturn(response)
            .when(deliveryCalculatorClient)
            .deliverySearch(
                defaultCalculatorRequest()
                    .senderId(1000L)
                    .deliveryServiceIds(Set.of(420L, 421L))
                    .locationsTo(Set.of(213, 117065, 20279))
                    .tariffType(TariffType.COURIER)
                    .build()
            );

        search(
            defaultFilter()
                .andThen(f -> f.setTo(createLocation(LOTTE_LOCATION_ADDRESS, null)))
                .andThen(f -> f.setDeliveryType(DeliveryType.COURIER))
                .andThen(f -> f.setDesiredDeliveryDate(LocalDate.of(2019, 2, 7)).setShipment(null)),
            1000L
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/delivery-options/search_result_virtual_delivery_service_desired_delivery_date.json"
            ));

        verify(geoSearchClient).find(LOTTE_LOCATION_ADDRESS);
        verify(geoSearchClient).find(LOTTE_LOCATION_COORDINATES);
        verifyNoMoreInteractions(geoSearchClient);
    }

    @Test
    @DisplayName("Виртуальные СД с типом доставки не COURIER")
    void virtualDeliveryDeliveryTypeNotCourier() throws Exception {
        mockVirtualDeliveryService();

        DeliverySearchResponse response = DeliverySearchResponse.builder()
            .deliveryOptions(List.of(deliveryOption(11L, 420L, TariffType.PICKUP, 2)))
            .build();

        doReturn(response)
            .when(deliveryCalculatorClient)
            .deliverySearch(
                defaultCalculatorRequest()
                    .senderId(1000L)
                    .deliveryServiceIds(Set.of(420L, 421L))
                    .locationsTo(Set.of(213, 20279, 120542))
                    .tariffType(TariffType.PICKUP)
                    .build()
            );

        mockPickupPointSearch(response);

        search(
            defaultFilter()
                .andThen(f -> f.setTo(createLocation(LOTTE_LOCATION_ADDRESS, 213)))
                .andThen(f -> f.setDeliveryType(DeliveryType.PICKUP)),
            1000L
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_virtual_delivery_service.json"));

        verifyZeroInteractions(geoSearchClient);
    }

    @Test
    @DisplayName("Виртуальные СД только с geoId в фильтре - нет похода за курьерскими опциями")
    void virtualDeliveryWithGeoId() throws Exception {
        mockVirtualDeliveryService();

        DeliverySearchResponse response = DeliverySearchResponse.builder()
            .deliveryOptions(List.of(deliveryOption(11L, 420L, TariffType.PICKUP, 2)))
            .build();

        DeliverySearchRequest pickupFilter = defaultCalculatorRequest()
            .senderId(1000L)
            .deliveryServiceIds(Set.of(420L, 421L))
            .locationsTo(Set.of(213, 20279, 120542))
            .tariffType(TariffType.PICKUP)
            .build();
        doReturn(response).when(deliveryCalculatorClient).deliverySearch(pickupFilter);

        mockPickupPointSearch(response);

        search(defaultFilter().andThen(f -> f.setTo(createLocation(null, 213))), 1000L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_virtual_delivery_service.json"));

        verify(deliveryCalculatorClient).deliverySearch(pickupFilter);
        verifyZeroInteractions(geoSearchClient);
        verifyNoMoreInteractions(deliveryCalculatorClient);
    }

    @Test
    @DisplayName("Виртуальные СД с адресом без указания типа доставки")
    void virtualDeliveryLocationWithoutDeliveryType() throws Exception {
        mockVirtualDeliveryService();
        mockGeoServiceLocalityResponse();

        DeliverySearchResponse courierOptions = DeliverySearchResponse.builder()
            .deliveryOptions(List.of(deliveryOptionBuilder(11L, 420L, TariffType.COURIER, 2).build()))
            .build();

        DeliverySearchResponse pickupPointOptions = DeliverySearchResponse.builder()
            .deliveryOptions(List.of(
                deliveryOptionBuilder(12L, 421L, TariffType.PICKUP, 2)
                    .pickupPoints(List.of(42100L))
                    .build()
            ))
            .build();

        DeliverySearchRequest courierFilter = defaultCalculatorRequest()
            .senderId(1000L)
            .deliveryServiceIds(Set.of(420L, 421L))
            .locationsTo(Set.of(213, 20279, 117065))
            .tariffType(TariffType.COURIER)
            .build();
        DeliverySearchRequest pickupFilter = defaultCalculatorRequest()
            .senderId(1000L)
            .deliveryServiceIds(Set.of(420L, 421L))
            .locationsTo(Set.of(213, 20279, 120542))
            .tariffType(TariffType.PICKUP)
            .build();
        doReturn(courierOptions).when(deliveryCalculatorClient).deliverySearch(courierFilter);
        doReturn(pickupPointOptions).when(deliveryCalculatorClient).deliverySearch(pickupFilter);

        mockPickupPointSearch(pickupPointOptions, id -> Set.of(scheduleDay(10 * id + 1)));
        mockCourierSchedule(lmsClient, Set.of(213, 20279, 117065, 120542), Set.of(420L, 421L));

        search(
            defaultFilter().andThen(f -> f.setTo(createLocation(LOTTE_LOCATION_ADDRESS, 213))),
            1000L
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/delivery-options/search_result_virtual_delivery_service_no_delivery_type.json"
            ));

        verify(deliveryCalculatorClient).deliverySearch(courierFilter);
        verify(deliveryCalculatorClient).deliverySearch(pickupFilter);
        verify(geoSearchClient).find(LOTTE_LOCATION_ADDRESS);
        verify(geoSearchClient).find(LOTTE_LOCATION_COORDINATES);
        verifyNoMoreInteractions(geoSearchClient, deliveryCalculatorClient);
    }

    @Test
    @DisplayName("Виртуальные СД с адресом с указанием дома и квартиры в фильтре")
    void virtualDeliveryLocationComponentsListContainsHouseKind() throws Exception {
        mockVirtualDeliveryService();
        String addressLine = "Россия, Москва, Литовский бульвар, 18, подъезд 6, этаж 7, кв. 344";
        String locationCoordinates = "37.53450514572847 55.612914012235976";
        List<GeoObject> geoSearchResultByTerm = List.of(
            geoObject(
                213,
                "1",
                locationCoordinates,
                Kind.UNKNOWN,
                component("Россия", Kind.COUNTRY),
                component("Центральный федеральный округ", Kind.PROVINCE),
                component("Москва", Kind.PROVINCE),
                component("Москва", Kind.LOCALITY),
                component("Литовский бульвар", Kind.STREET),
                component("18", Kind.HOUSE),
                component("подъезд 6", Kind.ENTRANCE),
                component("этаж 7", Kind.UNKNOWN),
                component("кв. 344", Kind.UNKNOWN)
            )
        );
        List<GeoObject> geoSearchResultByCoordinates = List.of(
            geoObject(213, addressLine, locationCoordinates, Kind.LOCALITY, component("Москва", Kind.AREA)),
            geoObject(
                120566,
                addressLine,
                locationCoordinates,
                Kind.DISTRICT,
                component("район Ясенево", Kind.AREA)
            )
        );
        doReturn(geoSearchResultByTerm).when(geoSearchClient).find(addressLine);
        doReturn(geoSearchResultByCoordinates).when(geoSearchClient).find(locationCoordinates);

        DeliverySearchResponse response = DeliverySearchResponse.builder()
            .deliveryOptions(List.of(deliveryOption(11L, 420L, TariffType.PICKUP, 2)))
            .build();

        doReturn(response)
            .when(deliveryCalculatorClient)
            .deliverySearch(
                defaultCalculatorRequest()
                    .senderId(1000L)
                    .deliveryServiceIds(Set.of(420L, 421L))
                    .locationsTo(Set.of(213, 120566))
                    .tariffType(TariffType.COURIER)
                    .build()
            );

        mockPickupPointSearch(response);

        search(
            defaultFilter()
                .andThen(f -> f.setTo(createLocation(addressLine, null)))
                .andThen(f -> f.setDeliveryType(DeliveryType.COURIER)),
            1000L
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_virtual_delivery_service.json"));

        verify(geoSearchClient).find(addressLine);
        verify(geoSearchClient).find(locationCoordinates);
        verifyNoMoreInteractions(geoSearchClient);
    }

    @Test
    @DisplayName("Виртуальные СД с адресом, который разрешается геопоиском в район субъекта федерации")
    void virtualDeliveryLocationResolvesToSubjectFederationDistrict() throws Exception {
        mockVirtualDeliveryService();
        String addressLine = "Россия, Московская область, городской округ Химки, 1-й Северный проезд, к1";
        String locationCoordinates = "37.419997 55.989461";
        List<GeoObject> geoSearchResultByTerm = List.of(
            geoObject(
                120988,
                addressLine,
                locationCoordinates,
                Kind.HOUSE,
                component("Россия", Kind.COUNTRY),
                component("Центральный федеральный округ", Kind.PROVINCE),
                component("Московская область", Kind.PROVINCE),
                component("городской округ Химки", Kind.AREA),
                component("1-й Северный проезд", Kind.STREET),
                component("к1", Kind.HOUSE)
            )
        );
        List<GeoObject> geoSearchResultByCoordinates = List.of(
            geoObject(120988, addressLine, locationCoordinates, Kind.PROVINCE),
            geoObject(120988, addressLine, locationCoordinates, Kind.AREA),
            geoObject(120988, addressLine, locationCoordinates, Kind.STREET),
            geoObject(120988, addressLine, locationCoordinates, Kind.HOUSE)
        );
        doReturn(geoSearchResultByTerm).when(geoSearchClient).find(addressLine);
        doReturn(geoSearchResultByCoordinates).when(geoSearchClient).find(locationCoordinates);

        DeliverySearchResponse response = DeliverySearchResponse.builder()
            .deliveryOptions(List.of(deliveryOption(11L, 420L, TariffType.PICKUP, 2)))
            .build();

        doReturn(response)
            .when(deliveryCalculatorClient)
            .deliverySearch(
                defaultCalculatorRequest()
                    .senderId(1000L)
                    .deliveryServiceIds(Set.of(420L, 421L))
                    .locationsTo(Set.of(120988))
                    .tariffType(TariffType.COURIER)
                    .build()
            );

        mockPickupPointSearch(response);

        search(
            defaultFilter()
                .andThen(f -> f.setTo(createLocation(addressLine, null)))
                .andThen(f -> f.setDeliveryType(DeliveryType.COURIER)),
            1000L
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_virtual_delivery_service.json"));

        verify(geoSearchClient).find(addressLine);
        verify(geoSearchClient).find(locationCoordinates);
        verifyNoMoreInteractions(geoSearchClient);
    }

    @Test
    @DisplayName("Виртуальные СД с адресом без указания дома в фильтре")
    void virtualDeliveryLocationWithoutHouseKind() throws Exception {
        mockVirtualDeliveryService();
        doReturn(List.of(geoObject(213, "1"))).when(geoSearchClient).find(LOCATION_ADDRESS);

        DeliverySearchResponse response = DeliverySearchResponse.builder()
            .deliveryOptions(List.of(deliveryOption(11L, 420L, TariffType.PICKUP, 2)))
            .build();

        doReturn(response)
            .when(deliveryCalculatorClient)
            .deliverySearch(
                defaultCalculatorRequest()
                    .senderId(1000L)
                    .deliveryServiceIds(Set.of(420L, 421L))
                    .locationsTo(Set.of(213))
                    .tariffType(TariffType.COURIER)
                    .build()
            );

        mockPickupPointSearch(response);

        search(
            defaultFilter()
                .andThen(f -> f.setTo(createLocation(LOCATION_ADDRESS, 213)))
                .andThen(f -> f.setDeliveryType(DeliveryType.COURIER)),
            1000L
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_virtual_delivery_service.json"));

        verify(geoSearchClient).find(LOCATION_ADDRESS);
        verifyNoMoreInteractions(geoSearchClient);
    }

    @Test
    @DisplayName("Виртуальные СД с невалидным адресом. GeoId указан, ищем опции по geoId")
    void virtualDeliveryInvalidLocation() throws Exception {
        mockVirtualDeliveryService();
        doReturn(List.of()).when(geoSearchClient).find(LOCATION_ADDRESS);

        DeliverySearchResponse response = DeliverySearchResponse.builder()
            .deliveryOptions(List.of(deliveryOption(11L, 420L, TariffType.PICKUP, 2)))
            .build();

        doReturn(response)
            .when(deliveryCalculatorClient)
            .deliverySearch(
                defaultCalculatorRequest()
                    .senderId(1000L)
                    .deliveryServiceIds(Set.of(420L, 421L))
                    .locationsTo(Set.of(213))
                    .tariffType(TariffType.COURIER)
                    .build()
            );

        mockPickupPointSearch(response);

        search(
            defaultFilter()
                .andThen(f -> f.setTo(createLocation(LOCATION_ADDRESS, 213)))
                .andThen(f -> f.setDeliveryType(DeliveryType.COURIER)),
            1000L
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_virtual_delivery_service.json"));

        verify(geoSearchClient).find(LOCATION_ADDRESS);
        verifyNoMoreInteractions(geoSearchClient);
    }

    @Test
    @DisplayName(
        "Виртуальные СД. Не найден geoId города локации. GeoId указан в запросе, ищем опции по geoId из запроса"
    )
    void virtualDeliveryNoLocalityGeoId() throws Exception {
        mockVirtualDeliveryService();
        doReturn(List.of(geoObject(777, "1"))).when(geoSearchClient).find(LOCATION_ADDRESS);

        DeliverySearchResponse response = DeliverySearchResponse.builder()
            .deliveryOptions(List.of(deliveryOption(11L, 420L, TariffType.PICKUP, 2)))
            .build();

        doReturn(response)
            .when(deliveryCalculatorClient)
            .deliverySearch(
                defaultCalculatorRequest()
                    .senderId(1000L)
                    .deliveryServiceIds(Set.of(420L, 421L))
                    .locationsTo(Set.of(213))
                    .tariffType(TariffType.COURIER)
                    .build()
            );

        mockPickupPointSearch(response);

        search(
            defaultFilter()
                .andThen(f -> f.setTo(createLocation(LOCATION_ADDRESS, 213)))
                .andThen(f -> f.setDeliveryType(DeliveryType.COURIER)),
            1000L
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_virtual_delivery_service.json"));

        verify(geoSearchClient).find(LOCATION_ADDRESS);
        verifyNoMoreInteractions(geoSearchClient);
    }

    @Test
    @DisplayName("Поиск курьерских опций без указания локации")
    void virtualDeliveryServicesDeliveryTypeCourierNoLocation() throws Exception {
        mockVirtualDeliveryService();
        mockGeoServiceLocalityResponse();

        DeliverySearchResponse response = DeliverySearchResponse.builder()
            .deliveryOptions(List.of(deliveryOption(11L, 420L, TariffType.PICKUP, 2)))
            .build();

        doReturn(response)
            .when(deliveryCalculatorClient)
            .deliverySearch(
                defaultCalculatorRequest()
                    .senderId(1000L)
                    .deliveryServiceIds(Set.of(420L, 421L))
                    .locationsTo(Set.of(213))
                    .tariffType(TariffType.COURIER)
                    .build()
            );

        mockPickupPointSearch(response);

        search(
            defaultFilter()
                .andThen(f -> f.setTo(createLocation(null, 213)))
                .andThen(f -> f.setDeliveryType(DeliveryType.COURIER)),
            1000L
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_virtual_delivery_service.json"));

        verifyNoMoreInteractions(geoSearchClient);
    }

    @Test
    @DisplayName("Виртуальные СД с указанием координат")
    void virtualDeliveryLocationWithCoordinates() throws Exception {
        BigDecimal longitude = BigDecimal.valueOf(37.289894);
        BigDecimal latitude = BigDecimal.valueOf(55.610916);
        mockVirtualDeliveryService();
        mockCourierSchedule(lmsClient, Set.of(213, 116996), Set.of(420L));
        when(geoSearchClient.find(longitude + " " + latitude)).thenReturn(findByCoordinatesResult());

        DeliverySearchResponse response = DeliverySearchResponse.builder()
            .deliveryOptions(List.of(
                deliveryOptionBuilder(11L, 420L, TariffType.COURIER, 2).build()
            ))
            .build();

        doReturn(response)
            .when(deliveryCalculatorClient)
            .deliverySearch(
                defaultCalculatorRequest()
                    .senderId(1000L)
                    .deliveryServiceIds(Set.of(420L, 421L))
                    .locationsTo(Set.of(213, 116996))
                    .tariffType(TariffType.COURIER)
                    .build()
            );

        search(
            defaultFilter()
                .andThen(f -> f.setTo(
                    createLocation(LOCATION_ADDRESS, 213)
                        .setLongitude(longitude)
                        .setLatitude(latitude)
                ))
                .andThen(f -> f.setDeliveryType(DeliveryType.COURIER)),
            1000L
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_courier_by_coordinates.json"));

        verify(geoSearchClient).find(longitude + " " + latitude);
        verifyNoMoreInteractions(geoSearchClient);
    }

    protected void mockVirtualDeliveryService() {
        doReturn(List.of(partnerBuilder(53916, PartnerType.DELIVERY).build()))
            .when(lmsClient)
            .searchPartners(LmsFactory.createPartnerFilter(Set.of(53916L), null));

        doReturn(
            List.of(
                partnerBuilder(420, PartnerType.DELIVERY)
                    .subtype(PartnerSubtypeResponse.newBuilder().id(2L).name("Маркет Курьер").build())
                    .build(),
                partnerBuilder(421, PartnerType.DELIVERY)
                    .subtype(PartnerSubtypeResponse.newBuilder().id(2L).name("Маркет Курьер").build())
                    .build()
            )
        )
            .when(lmsClient)
            .searchPartners(
                SearchPartnerFilter.builder()
                    .setPlatformClientIds(Set.of(PlatformClientId.YANDEX_DELIVERY.getId()))
                    .setPlatformClientStatuses(EnumSet.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING))
                    .setStatuses(Set.of(PartnerStatus.ACTIVE))
                    .setPartnerSubTypeIds(Set.of(2L))
                    .build()
            );

        doReturn(List.of(sortingCenterWarehouse))
            .when(lmsClient)
            .getLogisticsPoints(refEq(
                LogisticsPointFilter.newBuilder()
                    .type(PointType.WAREHOUSE)
                    .partnerIds(Set.of(420L, 421L, SORTING_CENTER_ID, 53916L))
                    .active(true)
                    .build()
            ));

        when(lmsClient.searchPartnerRelation(
            PartnerRelationFilter.newBuilder()
                .fromPartnersIds(Set.of(SORTING_CENTER_ID))
                .enabled(true)
                .build()
        )).thenReturn(
            List.of(
                PartnerRelationEntityDto.newBuilder()
                    .fromPartnerId(SORTING_CENTER_ID)
                    .toPartnerId(53916L)
                    .build()
            )
        );
    }

    protected void mockGeoServiceLocalityResponse() {
        Component countryComponent = component("Россия", Kind.COUNTRY);
        Component provinceComponent = component("Москва", Kind.PROVINCE);
        Component localityComponent = component("Москва", Kind.LOCALITY);
        Component cfoProvinceComponent = component("Центральный Федеральный Округ", Kind.PROVINCE);
        Component caoComponent = component("Центральный административный округ", Kind.DISTRICT);
        GeoObject houseGeoObject = geoObject(
            213,
            LOTTE_LOCATION_ADDRESS,
            LOTTE_LOCATION_COORDINATES,
            Kind.HOUSE,
            countryComponent,
            cfoProvinceComponent,
            provinceComponent,
            localityComponent,
            component("Новинский бульвар", Kind.STREET),
            component("8", Kind.HOUSE)
        );

        List<GeoObject> searchedByPointLocations = List.of(
            houseGeoObject,
            geoObject(
                117065,
                "Россия, Москва, Центральный административный округ, район Арбат",
                null,
                Kind.DISTRICT,
                countryComponent,
                cfoProvinceComponent,
                provinceComponent,
                localityComponent,
                caoComponent,
                component("район Арбат", Kind.DISTRICT)
            ),
            geoObject(
                20279,
                "Россия, Москва, Центральный административный округ",
                null,
                Kind.DISTRICT,
                countryComponent,
                cfoProvinceComponent,
                provinceComponent,
                localityComponent,
                caoComponent
            ),
            geoObject(
                213,
                "Россия, Москва",
                null,
                Kind.LOCALITY,
                countryComponent,
                cfoProvinceComponent,
                provinceComponent,
                localityComponent
            ),
            geoObject(
                213,
                "Россия, Москва",
                null,
                Kind.PROVINCE,
                countryComponent,
                cfoProvinceComponent,
                provinceComponent
            ),
            geoObject(
                3,
                "Россия, Центральный федеральный округ",
                null,
                Kind.PROVINCE,
                countryComponent,
                cfoProvinceComponent
            )
        );

        doReturn(List.of(houseGeoObject)).when(geoSearchClient).find(LOTTE_LOCATION_ADDRESS);
        doReturn(searchedByPointLocations).when(geoSearchClient).find(LOTTE_LOCATION_COORDINATES);
    }

    private List<GeoObject> findByCoordinatesResult() {
        Component country = component("Россия", Kind.COUNTRY);
        Component provinceCfo = component("Центральный федеральный округ", Kind.PROVINCE);
        Component province = component("Москва", Kind.PROVINCE);
        Component localityMoscow = component("Москва", Kind.LOCALITY);
        Component localityVnukovo = component("посёлок Внуково", Kind.LOCALITY);
        Component street = component("Аэрофлотская улица", Kind.STREET);
        Component house = component("1/7", Kind.HOUSE);
        return List.of(
            geoObject(
                213,
                "Россия, Москва, посёлок Внуково, Аэрофлотская улица, 1/7",
                "37.28989389 55.61091578",
                Kind.HOUSE,
                country,
                provinceCfo,
                province,
                localityVnukovo,
                street,
                house
            ),
            geoObject(
                213,
                "Россия, Москва, посёлок Внуково, Аэрофлотская улица",
                "37.28995678 55.61064121",
                Kind.STREET,
                country,
                provinceCfo,
                province,
                localityVnukovo,
                street
            ),
            geoObject(
                213,
                "Россия, Москва, посёлок Внуково",
                "37.29502327 55.60856151",
                Kind.LOCALITY,
                country,
                provinceCfo,
                province,
                localityVnukovo
            ),
            geoObject(
                116996,
                "Россия, Москва, Западный административный округ, район Внуково",
                "37.29668516 55.6108446",
                Kind.DISTRICT,
                country,
                provinceCfo,
                province,
                localityMoscow,
                component("Западный административный округ", Kind.DISTRICT),
                component("район Внуково", Kind.DISTRICT)
            ),
            geoObject(
                213,
                "Россия, Москва",
                "37.61764423 55.75581883",
                Kind.LOCALITY,
                country,
                provinceCfo,
                province,
                localityMoscow
            )
        );
    }
}
