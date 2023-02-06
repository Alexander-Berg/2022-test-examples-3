package ru.yandex.market.logistics.nesu.base;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.base.Strings;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.model.response.AddressInfo;
import ru.yandex.common.geocoder.model.response.AreaInfo;
import ru.yandex.common.geocoder.model.response.Boundary;
import ru.yandex.common.geocoder.model.response.Component;
import ru.yandex.common.geocoder.model.response.CountryInfo;
import ru.yandex.common.geocoder.model.response.GeoObject;
import ru.yandex.common.geocoder.model.response.Kind;
import ru.yandex.common.geocoder.model.response.LocalityInfo;
import ru.yandex.common.geocoder.model.response.Precision;
import ru.yandex.common.geocoder.model.response.SimpleGeoObject;
import ru.yandex.common.geocoder.model.response.ToponymInfo;
import ru.yandex.market.logistics.delivery.calculator.client.DeliveryCalculatorSearchEngineClient;
import ru.yandex.market.logistics.delivery.calculator.client.model.DeliveryOption;
import ru.yandex.market.logistics.delivery.calculator.client.model.DeliverySearchRequest;
import ru.yandex.market.logistics.delivery.calculator.client.model.DeliverySearchResponse;
import ru.yandex.market.logistics.delivery.calculator.client.model.TariffType;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.ShipmentSearchDto;
import ru.yandex.market.logistics.lom.model.enums.ShipmentApplicationStatus;
import ru.yandex.market.logistics.lom.model.filter.ShipmentSearchFilter;
import ru.yandex.market.logistics.lom.model.filter.ShipmentSearchFilter.ShipmentSearchFilterBuilder;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerCourierDayScheduleResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse.LogisticsPointResponseBuilder;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.api.model.Dimensions;
import ru.yandex.market.logistics.nesu.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.nesu.dto.deliveryoptions.DeliveryOptionsDestinationLocation;
import ru.yandex.market.logistics.nesu.dto.deliveryoptions.DeliveryOptionsFilter;
import ru.yandex.market.logistics.nesu.dto.deliveryoptions.DeliveryOptionsFilterCost;
import ru.yandex.market.logistics.nesu.dto.deliveryoptions.DeliveryOptionsFilterPlace;
import ru.yandex.market.logistics.nesu.dto.deliveryoptions.DeliveryOptionsFilterShipment;
import ru.yandex.market.logistics.nesu.dto.deliveryoptions.DeliveryOptionsLocation;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.service.lms.PlatformClientId;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ValidationErrorDataBuilder;
import ru.yandex.market.logistics.tarifficator.client.TarifficatorClient;
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor;

import static java.lang.Integer.MAX_VALUE;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createLogisticsPointResponseBuilder;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createScheduleDayDtoSetWithSize;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldErrorBuilder;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/repository/delivery-options/data.xml")
public abstract class AbstractDeliveryOptionsSearchBaseCasesTest extends AbstractContextualTest {

    protected static final String LOCATION_ADDRESS = "3";
    protected static final int LOCATION_TO_GEO_ID = 42;
    protected static final long SORTING_CENTER_ID = 500L;
    protected static final long SORTING_CENTER_WAREHOUSE_ID = 602L;
    protected static final long SENDER_WAREHOUSE_ID = 1L;
    protected static final LocalDate TODAY = LocalDate.of(2019, 2, 2);
    protected static final Instant NOW = TODAY.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant();
    protected static final Pageable SHIPMENT_PAGEABLE = new Pageable(0, MAX_VALUE, null);

    @Autowired
    protected GeoClient geoSearchClient;
    @Autowired
    protected DeliveryCalculatorSearchEngineClient deliveryCalculatorClient;
    @Autowired
    protected LMSClient lmsClient;
    @Autowired
    protected LomClient lomClient;
    @Autowired
    protected TarifficatorClient tarifficatorClient;
    @Autowired
    protected FeatureProperties featureProperties;

    @RegisterExtension
    final BackLogCaptor backLogCaptor = new BackLogCaptor();

    @BeforeEach
    void setUp() {
        when(featureProperties.getShopIdsAllowedToCreateOrders()).thenReturn(Set.of(100L));
    }

    @Test
    @DisplayName("Пустые опции для магазинов не из списка разрешенных")
    void emptyDeliveryOptionsForNotAllowedShopId() throws Exception {
        when(featureProperties.getShopIdsAllowedToCreateOrders()).thenReturn(Set.of());
        search(defaultFilter())
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource({
        "filterValidationProvider",
        "filterValidationProviderDimensions",
    })
    @DisplayName("Валидация фильтра")
    void filterValidation(
        ValidationErrorDataBuilder fieldErrorBuilder,
        Consumer<DeliveryOptionsFilter> filterAdjuster
    ) throws Exception {
        search(defaultFilter().andThen(filterAdjuster))
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(fieldErrorBuilder.forObject(optionFilterObjectName())));
    }

    @Nonnull
    protected String optionFilterObjectName() {
        return "deliveryOptionsFilter";
    }

    @Nonnull
    private static Stream<Arguments> filterValidationProvider() {
        return Stream.<Pair<ValidationErrorDataBuilder, Consumer<DeliveryOptionsFilter>>>of(
            Pair.of(
                fieldErrorBuilder("to", "must not be null", "NotNull"),
                f -> f.setTo(null)
            ),
            Pair.of(
                fieldErrorBuilder("to", "Location must have either address or geo ID", "ValidLocation"),
                f -> f.setTo(createLocation(null, null))
            ),
            Pair.of(
                fieldErrorBuilder("to", "Location must have either address or geo ID", "ValidLocation"),
                f -> f.setTo(createLocation("", null))
            ),
            Pair.of(
                fieldErrorBuilder("to.pickupPointIds", "size must be between 0 and 1000", "Size")
                    .withArguments(Map.of("min", 0, "max", 1000)),
                f -> f.setTo(createLocation(null, LOCATION_TO_GEO_ID)
                    .setPickupPointIds(LongStream.range(0, 1001).boxed().collect(Collectors.toSet())))
            ),
            Pair.of(
                fieldErrorBuilder("cost.assessedValue", "must be greater than or equal to 0", "PositiveOrZero"),
                f -> f.setCost(createCost().setAssessedValue(new BigDecimal("-100")))

            ),
            Pair.of(
                fieldErrorBuilder("cost.itemsSum", "must be greater than or equal to 0", "PositiveOrZero"),
                f -> f.setCost(createCost().setItemsSum(new BigDecimal("-100")))

            ),
            Pair.of(
                fieldErrorBuilder(
                    "cost.manualDeliveryForCustomer",
                    "must be greater than or equal to 0",
                    "PositiveOrZero"
                ),
                f -> f.setCost(createCost().setManualDeliveryForCustomer(new BigDecimal("-100")))

            ),
            Pair.of(
                fieldErrorBuilder(
                    "desiredDeliveryDate",
                    "Filter must not specify both shipment and desired delivery dates",
                    "ValidDeliveryOptionsFilterDates"
                ),
                f -> f.setDesiredDeliveryDate(LocalDate.of(2019, 2, 3))
            ),
            Pair.of(
                fieldErrorBuilder(
                    "desiredDeliveryDate",
                    "must be between 2019-02-02 and 2019-03-02",
                    "ValidDeliveryOptionsFilterDates"
                ),
                f -> {
                    Objects.requireNonNull(f.getShipment()).setDate(null);
                    f.setDesiredDeliveryDate(LocalDate.of(2019, 2, 1));
                }
            ),
            Pair.of(
                fieldErrorBuilder(
                    "desiredDeliveryDate",
                    "must be between 2019-02-02 and 2019-03-02",
                    "ValidDeliveryOptionsFilterDates"
                ),
                f -> {
                    Objects.requireNonNull(f.getShipment()).setDate(null);
                    f.setDesiredDeliveryDate(LocalDate.of(2019, 3, 3));
                }
            )
        ).map(v -> Arguments.of(v.getLeft(), v.getRight()));
    }

    @Nonnull
    private static Stream<Arguments> filterValidationProviderDimensions() {
        return Stream.<Pair<ValidationErrorDataBuilder, Consumer<DeliveryOptionsFilter>>>of(
            Pair.of(
                fieldErrorBuilder("dimensions.length", "must not be null", "NotNull"),
                f -> f.setDimensions(createDimensions().setLength(null))
            ),
            Pair.of(
                fieldErrorBuilder("dimensions.length", "must be greater than 0", "Positive"),
                f -> f.setDimensions(createDimensions().setLength(-1))
            ),
            Pair.of(
                fieldErrorBuilder("dimensions.width", "must not be null", "NotNull"),
                f -> f.setDimensions(createDimensions().setWidth(null))
            ),
            Pair.of(
                fieldErrorBuilder("dimensions.width", "must be greater than 0", "Positive"),
                f -> f.setDimensions(createDimensions().setWidth(-2))
            ),
            Pair.of(
                fieldErrorBuilder("dimensions.height", "must not be null", "NotNull"),
                f -> f.setDimensions(createDimensions().setHeight(null))
            ),
            Pair.of(
                fieldErrorBuilder("dimensions.height", "must be greater than 0", "Positive"),
                f -> f.setDimensions(createDimensions().setHeight(-3))
            ),
            Pair.of(
                fieldErrorBuilder("dimensions.weight", "must not be null", "NotNull"),
                f -> f.setDimensions(createDimensions().setWeight(null))
            ),
            Pair.of(
                fieldErrorBuilder("dimensions.weight", "must be greater than 0", "Positive"),
                f -> f.setDimensions(createDimensions().setWeight(new BigDecimal("-4")))
            ),
            Pair.of(
                fieldErrorBuilder("places", "must not contain nulls", "NotNullElements"),
                f -> f.setDimensions(null)
                    .setPlaces(Collections.singletonList(null))
            ),
            Pair.of(
                fieldErrorBuilder("places", "size must be between 0 and 100", "Size")
                    .withArguments(Map.of("min", 0, "max", 100)),
                f -> f.setPlaces(
                    IntStream.range(0, 101)
                        .mapToObj(i -> createDeliveryOptionsFilterPlace(d -> {
                        }))
                        .collect(Collectors.toList())
                )
            ),
            Pair.of(
                fieldErrorBuilder("places[0].dimensions.length", "must not be null", "NotNull"),
                f -> f.setDimensions(null)
                    .setPlaces(List.of(createDeliveryOptionsFilterPlace(d -> d.setLength(null))))
            ),
            Pair.of(
                fieldErrorBuilder("places[0].dimensions.length", "must be greater than 0", "Positive"),
                f -> f.setDimensions(null)
                    .setPlaces(List.of(createDeliveryOptionsFilterPlace(d -> d.setLength(-1))))
            ),
            Pair.of(
                fieldErrorBuilder("places[0].dimensions.width", "must not be null", "NotNull"),
                f -> f.setDimensions(null)
                    .setPlaces(List.of(createDeliveryOptionsFilterPlace(d -> d.setWidth(null))))
            ),
            Pair.of(
                fieldErrorBuilder("places[0].dimensions.width", "must be greater than 0", "Positive"),
                f -> f.setDimensions(null)
                    .setPlaces(List.of(createDeliveryOptionsFilterPlace(d -> d.setWidth(-2))))
            ),
            Pair.of(
                fieldErrorBuilder("places[0].dimensions.height", "must not be null", "NotNull"),
                f -> f.setDimensions(null)
                    .setPlaces(List.of(createDeliveryOptionsFilterPlace(d -> d.setHeight(null))))
            ),
            Pair.of(
                fieldErrorBuilder("places[0].dimensions.height", "must be greater than 0", "Positive"),
                f -> f.setDimensions(null)
                    .setPlaces(List.of(createDeliveryOptionsFilterPlace(d -> d.setHeight(-3))))
            ),
            Pair.of(
                fieldErrorBuilder("places[0].dimensions.weight", "must not be null", "NotNull"),
                f -> f.setDimensions(null)
                    .setPlaces(List.of(createDeliveryOptionsFilterPlace(d -> d.setWeight(null))))
            ),
            Pair.of(
                fieldErrorBuilder("places[0].dimensions.weight", "must be greater than 0", "Positive"),
                f -> f.setDimensions(null)
                    .setPlaces(List.of(createDeliveryOptionsFilterPlace(d -> d.setWeight(new BigDecimal("-4")))))

            )
        ).map(v -> Arguments.of(v.getLeft(), v.getRight()));
    }

    @Test
    @DisplayName("У сендера нет настроенных служб доставки")
    void senderWithNoSettingsEmptyResult() throws Exception {
        search(defaultFilter(), 2L)
            .andExpect(status().isOk())
            .andExpect(content().json(EMPTY_ARRAY));
    }

    @Test
    @DisplayName("У сендера нет активных служб доставки, удовлетворяющих фильтру")
    void senderWithNoActiveSettingsEmptyResult() throws Exception {
        search(defaultFilter(), 4L)
            .andExpect(status().isOk())
            .andExpect(content().json(EMPTY_ARRAY));

        verifyZeroInteractions(deliveryCalculatorClient);
    }

    @Test
    @DisplayName("Неизвестный адрес назначения")
    void unknownLocationToError() throws Exception {
        doReturn(List.of()).when(geoSearchClient).find(LOCATION_ADDRESS);

        search(defaultFilter())
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("No locations found for address: 3"));

        verify(geoSearchClient).find(LOCATION_ADDRESS);
        verifyNoMoreInteractions(geoSearchClient, deliveryCalculatorClient);
    }

    @Test
    @DisplayName("Адрес назначения соответствует более чем одной локации. Выбрана наиболее точная локация")
    void multipleLocationsToSuccess() throws Exception {
        doReturn(List.of(
                geoObject(2, "2", Precision.NEAR),
                geoObject(-1, "1", Precision.EXACT)
        )).when(geoSearchClient).find(LOCATION_ADDRESS);

        search(defaultFilter())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_required.json"));

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=WARN\t" +
                "format=plain\t" +
                "payload=More than one location found for address: 3. Found locations: " +
                "[SimpleGeoObject{"
                + "toponymInfo=ToponymInfo{"
                + "precision=NEAR, "
                + "kind=null, "
                + "geoid='2', "
                + "point='', "
                + "geocoderObjectId='', "
                + "components=[]"
                + "}, "
                + "boundary=Boundary{"
                + "envelopeLower='', "
                + "envelopeUpper=''"
                + "}, "
                + "addressInfo=AddressInfo{"
                + "countryInfo=CountryInfo{"
                + "countryName='', "
                + "countryCode=''"
                + "}, "
                + "areaInfo=AreaInfo{"
                + "administrativeAreaName='', "
                + "subAdministrativeAreaName=''"
                + "}, "
                + "localityInfo=LocalityInfo{"
                + "localityName='', "
                + "thoroughfareName='', "
                + "dependentLocalityName='', "
                + "postalCode='', "
                + "premiseName='', "
                + "premiseNumber=''"
                + "}, "
                + "addressLine='2'"
                + "}, "
                + "accuracy=''"
                + "}, "
                + "SimpleGeoObject{"
                + "toponymInfo=ToponymInfo{"
                + "precision=EXACT, "
                + "kind=null, "
                + "geoid='-1', "
                + "point='', "
                + "geocoderObjectId='', "
                + "components=[]"
                + "}, "
                + "boundary=Boundary{"
                + "envelopeLower='', "
                + "envelopeUpper=''"
                + "}, "
                + "addressInfo=AddressInfo{"
                + "countryInfo=CountryInfo{"
                + "countryName='', "
                + "countryCode=''"
                + "}, "
                + "areaInfo=AreaInfo{"
                + "administrativeAreaName='', "
                + "subAdministrativeAreaName=''"
                + "}, "
                + "localityInfo=LocalityInfo{"
                + "localityName='', "
                + "thoroughfareName='', "
                + "dependentLocalityName='', "
                + "postalCode='', "
                + "premiseName='', "
                + "premiseNumber=''"
                + "}, "
                + "addressLine='1'"
                + "}, "
                + "accuracy=''"
                + "}]\t"
                + "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t"
                + "tags=GEO_SEARCH"
        );

        verify(geoSearchClient).find(LOCATION_ADDRESS);
    }

    @Test
    @DisplayName("Не удалось найти город адреса назначения")
    void locationToCity() throws Exception {
        doReturn(List.of(geoObject(10000, "2"))).when(geoSearchClient).find(LOCATION_ADDRESS);

        search(defaultFilter())
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Failed to find city for address: 3"));

        verify(geoSearchClient).find(LOCATION_ADDRESS);
        verifyNoMoreInteractions(geoSearchClient, deliveryCalculatorClient);
    }

    @Test
    @DisplayName("Город адреса назначения найден по району")
    void locationToParentCity() throws Exception {
        doReturn(List.of(geoObject(-1, "2"))).when(geoSearchClient).find(LOCATION_ADDRESS);

        search(defaultFilter())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_required.json"));

        verify(geoSearchClient).find(LOCATION_ADDRESS);
    }

    @Test
    @DisplayName("Село адреса назначения найдено по району")
    void locationToVillage() throws Exception {
        int locationTo = 121908;

        doReturn(List.of(geoObject(locationTo, "2"))).when(geoSearchClient).find(LOCATION_ADDRESS);

        when(
            deliveryCalculatorClient.deliverySearch(defaultCalculatorRequest().locationsTo(Set.of(locationTo)).build())
        )
            .thenReturn(defaultResponse());

        search(defaultFilter())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_required.json"));

        verify(geoSearchClient).find(LOCATION_ADDRESS);
    }

    @Test
    @DisplayName("Идентификатор точки назначения")
    void locationToId() throws Exception {
        search(defaultFilter().andThen(f -> f.setTo(createLocation(null, LOCATION_TO_GEO_ID))))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_required.json"));

        verifyZeroInteractions(geoSearchClient);
    }

    @Test
    @DisplayName("Точка отправления")
    void locationFrom() throws Exception {
        doReturn(defaultResponse())
            .when(deliveryCalculatorClient).deliverySearch(defaultCalculatorRequest().locationFrom(213).build());

        search(defaultFilter().andThen(f -> f.setFrom(new DeliveryOptionsLocation().setGeoId(213))))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_required.json"));
    }

    @Test
    @DisplayName("Точка отправления подставляется костылём")
    void stubLocationFrom() throws Exception {
        doReturn(defaultResponse())
            .when(deliveryCalculatorClient).deliverySearch(defaultCalculatorRequest().locationFrom(213).build());

        search(defaultFilter().andThen(f -> f.setFrom(new DeliveryOptionsLocation().setGeoId(1))))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_required.json"));
    }

    @Test
    @DisplayName("Калькулятор доставки вернул пустой список вариантов")
    void noOptionsEmptyResult() throws Exception {
        doReturn(DeliverySearchResponse.builder()
            .deliveryOptions(List.of())
            .build())
            .when(deliveryCalculatorClient).deliverySearch(defaultCalculatorRequest().build());

        search(defaultFilter())
            .andExpect(status().isOk())
            .andExpect(content().json(EMPTY_ARRAY));

        verifyGetActiveWarehousesByIds(SORTING_CENTER_WAREHOUSE_ID, 600L, 601L);
        verifySearchSortingCenterPartnerRelations();
        verify(lmsClient).searchPartners(createFilter());
        verify(lmsClient).getLogisticsPoint(SORTING_CENTER_WAREHOUSE_ID);
        verify(lmsClient).getPartner(SORTING_CENTER_ID);
        verify(lmsClient).searchPartners(getFilterForOwnDeliveries());
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Фильтр с указанием несуществующего склада отправки")
    void shipmentWithNonExistentWarehouseId() throws Exception {
        search(defaultFilter().andThen(withSenderWarehouse()))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WAREHOUSE] with ids [1]"));

        verifyZeroInteractions(deliveryCalculatorClient);
    }

    @Test
    @DisplayName("Фильтр с указанием недоступного склада отправки")
    void shipmentWithUnavailableWarehouseId() throws Exception {
        mockSearchSenderWarehouse(point -> point.businessId(null));

        search(defaultFilter().andThen(withSenderWarehouse()))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WAREHOUSE] with ids [1]"));

        verifyZeroInteractions(deliveryCalculatorClient);
    }

    @Test
    @DisplayName("Фильтр с указанием недоступного по businessId склада отправки")
    void shipmentWithUnavailableWarehouseIdByBusinessId() throws Exception {
        mockSearchSenderWarehouse(point -> point.businessId(41L));

        search(defaultFilter().andThen(withSenderWarehouse()))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WAREHOUSE] with ids [1]"));

        verifyZeroInteractions(deliveryCalculatorClient);
    }

    @Test
    @DisplayName("Новая логика фильтрации - успех")
    void shipmentWithUnavailableWarehouseIdNewFilterSuccess() throws Exception {
        mockSearchSenderWarehouse(point -> point.businessId(41L).partnerId(null));

        search(defaultFilter())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_required.json"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Новая логика фильтрации - ошибка")
    @MethodSource
    void shipmentWithUnavailableWarehouseIdNewFilter(
        @SuppressWarnings("unused") String name,
        UnaryOperator<LogisticsPointResponseBuilder> modifier
    ) throws Exception {
        mockSearchSenderWarehouse(modifier);

        search(defaultFilter().andThen(withSenderWarehouse()))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WAREHOUSE] with ids [1]"));

        verifyZeroInteractions(deliveryCalculatorClient);
    }

    @Nonnull
    private static Stream<Arguments> shipmentWithUnavailableWarehouseIdNewFilter() {
        return Stream.of(
            Arguments.of(
                "Не совпадает BusinessId",
                (UnaryOperator<LogisticsPointResponseBuilder>) point -> point.businessId(420L).partnerId(null)
            ),
            Arguments.of(
                "Есть партнер",
                (UnaryOperator<LogisticsPointResponseBuilder>) point -> point.businessId(42L).partnerId(1L)
            ),
            Arguments.of(
                "Не совпадает businessId и есть партнер",
                (UnaryOperator<LogisticsPointResponseBuilder>) point -> point.businessId(420L).partnerId(1L)
            )
        );
    }

    @Test
    @DisplayName("Поиск по обязательным полям фильтра")
    void requiredFilterFields() throws Exception {
        search(defaultFilter())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_required.json"));
    }

    @Test
    @DisplayName("Поиск с одновременным указанием dimensions и places")
    void requiredFilterFieldsWithEmptyPlaces() throws Exception {
        search(defaultFilter().andThen(f -> f.setPlaces(List.of())))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_required.json"));
    }

    @Test
    @DisplayName("Вместо dimensions использовано поле places")
    void dimensionsInBox() throws Exception {
        search(defaultFilter().andThen(
            f -> f.setDimensions(null)
                .setPlaces(List.of(new DeliveryOptionsFilterPlace().setDimensions(createDimensions())))
        ))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_required.json"));
    }

    @Test
    @DatabaseSetup("/repository/delivery-options/sender_average_order_measures.xml")
    @DisplayName("Вместо dimensions и places использованы средние размеры заказа из настроек сендера")
    void useAverageOrderMeasuresOnEmptyDimensionsAndPlaces() throws Exception {
        search(defaultFilter().andThen(f -> f.setDimensions(null)).andThen(f -> f.setPlaces(List.of())))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_required.json"));
    }

    @Test
    @DisplayName("В фильтре отсутствуют dimensions и places, средние ВГХ не настроены, используются дефолтные")
    void averageOrderMeasuresNotFound() throws Exception {
        when(deliveryCalculatorClient.deliverySearch(
            defaultCalculatorRequest()
                .weight(BigDecimal.ONE)
                .length(10)
                .width(10)
                .height(10)
                .build()
        ))
            .thenReturn(defaultResponse());

        search(defaultFilter().andThen(f -> f.setDimensions(null)).andThen(f -> f.setPlaces(null)))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_required.json"));
    }

    @Test
    @DisplayName("В дополнение к dimensions использовано поле places, поиск проводится по полю dimensions")
    void dimensionsAndPlacesBothSpecified() throws Exception {
        search(defaultFilter().andThen(
            f -> f.setPlaces(List.of(
                new DeliveryOptionsFilterPlace().setDimensions(createDimensions(100, 10, 40, "3")),
                new DeliveryOptionsFilterPlace().setDimensions(createDimensions(5, 60, 60, "2")),
                new DeliveryOptionsFilterPlace().setDimensions(createDimensions(50, 5, 70, "1"))
            ))
        ))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_required.json"));
    }

    @Test
    @DisplayName("Поиск способа доставки нескольких коробок")
    void severalBoxesSearch() throws Exception {
        when(deliveryCalculatorClient.deliverySearch(
            defaultCalculatorRequest()
                .weight(BigDecimal.valueOf(6))
                .length(100)
                .width(60)
                .height(20)
                .build()
        ))
            .thenReturn(defaultResponse());

        search(defaultFilter().andThen(
            f -> f.setDimensions(null).setPlaces(List.of(
                new DeliveryOptionsFilterPlace().setDimensions(createDimensions(100, 10, 40, "3")),
                new DeliveryOptionsFilterPlace().setDimensions(createDimensions(5, 60, 60, "2")),
                new DeliveryOptionsFilterPlace().setDimensions(createDimensions(50, 5, 70, "1"))
            ))
        ))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_required.json"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("argumentsWithFractionalKopecks")
    @DisplayName("В фильтре указаны дробные значения копеек")
    void searchWithFractionalKopecks(
        @SuppressWarnings("unused") String name,
        DeliverySearchRequest request,
        DeliveryOptionsFilterCost cost,
        String responsePath
    ) throws Exception {
        when(deliveryCalculatorClient.deliverySearch(request))
            .thenReturn(defaultResponse());

        search(defaultFilter().andThen(
            filter -> filter.setCost(cost).setFrom(new DeliveryOptionsLocation().setGeoId(213))
        ))
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    private static Stream<Arguments> argumentsWithFractionalKopecks() {
        return Stream.of(
            Arguments.of(
                "Дробное значение копеек в объявленной стоимости",
                defaultCalculatorRequest().locationFrom(213).offerPrice(250000L).build(),
                createCost().setAssessedValue(BigDecimal.valueOf(9999.1234)),
                "controller/delivery-options/search_result_assessed_value_with_fractional_kopecks.json"
            ),
            Arguments.of(
                "Дробное значение копеек в суммарной стоимости товаров",
                defaultCalculatorRequest().locationFrom(213).offerPrice(300012L).build(),
                createCost().setItemsSum(BigDecimal.valueOf(3000.1234)),
                "controller/delivery-options/search_result_items_sum_with_fractional_kopecks.json"
            )
        );
    }

    @Nonnull
    protected Consumer<DeliveryOptionsFilter> defaultFilter() {
        return filter -> {
            filter.setTo(createLocation(LOCATION_ADDRESS, null));
            filter.setDimensions(createDimensions());
            DeliveryOptionsFilterShipment deliveryOptionsFilterShipment = new DeliveryOptionsFilterShipment();
            deliveryOptionsFilterShipment.setDate(LocalDate.of(2019, 8, 1));
            filter.setShipment(deliveryOptionsFilterShipment);
        };
    }

    @Nonnull
    protected Consumer<DeliveryOptionsFilter> withSenderWarehouse() {
        return withSenderWarehouse(SENDER_WAREHOUSE_ID);
    }

    @Nonnull
    protected Consumer<DeliveryOptionsFilter> withSenderWarehouse(Long warehouseId) {
        return filter -> filter.setShipment(new DeliveryOptionsFilterShipment().setWarehouseId(warehouseId));
    }

    @Nonnull
    protected static DeliveryOptionsFilterPlace createDeliveryOptionsFilterPlace(Consumer<Dimensions> modifier) {
        Dimensions dimensions = createDimensions();
        modifier.accept(dimensions);
        return new DeliveryOptionsFilterPlace().setDimensions(dimensions);
    }

    @Nonnull
    protected static Dimensions createDimensions() {
        return createDimensions(4, 5, 6, "7.80");
    }

    @Nonnull
    protected static Dimensions createDimensions(Integer length, Integer width, Integer height, String weight) {
        return new Dimensions()
            .setLength(length)
            .setWidth(width)
            .setHeight(height)
            .setWeight(new BigDecimal(weight));
    }

    @Nonnull
    private static DeliveryOptionsFilterCost createCost() {
        return new DeliveryOptionsFilterCost()
            .setAssessedValue(new BigDecimal("2500"))
            .setItemsSum(new BigDecimal("2500"))
            .setFullyPrepaid(true)
            .setManualDeliveryForCustomer(new BigDecimal("200"));
    }

    @Nonnull
    protected static DeliveryOptionsDestinationLocation createLocation(
        @Nullable String locationName,
        @Nullable Integer geoId
    ) {
        DeliveryOptionsDestinationLocation location = new DeliveryOptionsDestinationLocation();
        location
            .setLocation(locationName)
            .setGeoId(geoId);
        return location;
    }

    @Nonnull
    protected SearchPartnerFilter createFilter() {
        return LmsFactory.createPartnerFilter(Set.of(400L, 420L, 410L, 500L), null, Set.of(PartnerStatus.ACTIVE));
    }

    @Nonnull
    protected SearchPartnerFilter getFilterForOwnDeliveries() {
        return SearchPartnerFilter.builder()
            .setStatuses(Set.of(PartnerStatus.ACTIVE))
            .setTypes(Set.of(PartnerType.OWN_DELIVERY))
            .setMarketIds(Set.of(2000L))
            .setPlatformClientIds(Set.of(PlatformClientId.YANDEX_DELIVERY.getId()))
            .build();
    }

    @Nonnull
    protected static DeliverySearchRequest.DeliverySearchRequestBuilder defaultCalculatorRequest() {
        return DeliverySearchRequest.builder()
            .deliveryServiceIds(Set.of(400L, 420L))
            .length(4)
            .width(5)
            .height(6)
            .weight(new BigDecimal("7.80"))
            .locationFrom(213)
            .locationsTo(Set.of(LOCATION_TO_GEO_ID))
            .senderId(1L);
    }

    @Nonnull
    protected PartnerResponse partner(long id, PartnerType type) {
        return partnerBuilder(id, type).build();
    }

    @Nonnull
    protected PartnerResponse.PartnerResponseBuilder partnerBuilder(long id, PartnerType type) {
        return PartnerResponse.newBuilder()
            .id(id)
            .marketId(777L + id)
            .partnerType(type)
            .name(String.valueOf(id))
            .readableName(type + " partner " + id)
            .status(PartnerStatus.ACTIVE)
            .params(List.of(
                new PartnerExternalParam(PartnerExternalParamType.IS_MULTIPLACES_SUPPORTED.name(), "", "1")
            ))
            .intakeSchedule(List.of(
                scheduleDay(1000 + id),
                scheduleDay(3L)
            ))
            .businessId(42L)
            .calendarId(2000 + id);
    }

    @Nonnull
    protected LogisticsPointResponse warehouse(Long id, Long partnerId) {
        return warehouseBuilder(id, partnerId)
            .schedule(Set.of(
                scheduleDay(1000 + id),
                scheduleDay(3L),
                scheduleDay(5L)
            ))
            .businessId(42L)
            .build();
    }

    @Nonnull
    protected LogisticsPointResponseBuilder warehouseBuilder(Long id, Long partnerId) {
        return LogisticsPointResponse.newBuilder()
            .id(id)
            .partnerId(partnerId)
            .type(PointType.WAREHOUSE)
            .name("Warehouse " + id)
            .active(true)
            .isFrozen(false)
            .address(Address.newBuilder().locationId(213).build())
            .locationZoneId(213L);
    }

    @Nonnull
    protected LogisticsPointResponseBuilder pickupPointBuilder(
        Long id,
        Long partnerId,
        boolean cardAllowed,
        boolean cashAllowed,
        boolean prepaidAllowed
    ) {
        return LogisticsPointResponse.newBuilder()
            .id(id)
            .partnerId(partnerId)
            .type(PointType.PICKUP_POINT)
            .name("ПВЗ")
            .active(true)
            .isFrozen(false)
            .locationZoneId(213L)
            .cardAllowed(cardAllowed)
            .cashAllowed(cashAllowed)
            .prepayAllowed(prepaidAllowed);
    }

    protected void mockSearchSenderWarehouse(UnaryOperator<LogisticsPointResponseBuilder> modifier) {
        LogisticsPointResponseBuilder builder =
            createLogisticsPointResponseBuilder(SENDER_WAREHOUSE_ID,  null, "point", PointType.WAREHOUSE)
                .businessId(42L)
                .schedule(createScheduleDayDtoSetWithSize(7));

        when(lmsClient.getLogisticsPoint(SENDER_WAREHOUSE_ID)).thenReturn(Optional.of(modifier.apply(builder).build()));
    }

    protected void mockSearchSenderWarehouse() {
        mockSearchSenderWarehouse(UnaryOperator.identity());
    }

    @Nonnull
    protected ScheduleDayResponse scheduleDay(Long id) {
        return new ScheduleDayResponse(id, (int) (id % 7 + 1), LocalTime.of(1, 0), LocalTime.of(2, 0));
    }

    @Nonnull
    protected DeliverySearchResponse defaultResponse() {
        return DeliverySearchResponse.builder()
            .deliveryOptions(List.of(
                deliveryOption(10L, 400L, TariffType.POST, 1),
                deliveryOption(11L, 420L, TariffType.PICKUP, 2)
            ))
            .build();
    }

    @Nonnull
    protected DeliverySearchResponse courierResponse() {
        return DeliverySearchResponse.builder()
            .deliveryOptions(List.of(
                deliveryOption(13L, 400L, TariffType.COURIER, 1),
                deliveryOption(14L, 420L, TariffType.COURIER, 3)
            ))
            .build();
    }

    @Nonnull
    protected PartnerCourierDayScheduleResponse.PartnerCourierDayScheduleBuilder courierSchedule(
        int scheduleNumber,
        long partnerId
    ) {
        return PartnerCourierDayScheduleResponse.builder()
            .partnerId(partnerId)
            .locationId(LOCATION_TO_GEO_ID)
            .schedule(List.of(
                createScheduleDay(scheduleNumber + 1, scheduleNumber, scheduleNumber + 2),
                createScheduleDay(scheduleNumber, scheduleNumber, scheduleNumber + 1),
                createScheduleDay(scheduleNumber + 200, scheduleNumber + 1, scheduleNumber + 2)
            ));
    }

    protected void mockPickupPointSearch(DeliverySearchResponse response) {
        mockPickupPointSearch(response, id -> null);
    }

    protected void mockPickupPointSearch(
        DeliverySearchResponse response,
        Function<Long, Set<ScheduleDayResponse>> schedule
    ) {
        Set<Long> pickupPointIds = response.getDeliveryOptions()
            .stream()
            .map(DeliveryOption::getPickupPoints)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
        LogisticsPointFilter filter = LogisticsPointFilter.newBuilder()
            .ids(pickupPointIds)
            .active(true)
            .type(PointType.PICKUP_POINT)
            .build();
        List<LogisticsPointResponse> pickupPoints = pickupPointIds.stream()
            .map(id -> LogisticsPointResponse.newBuilder()
                .id(id)
                .schedule(schedule.apply(id))
                .cashAllowed(true)
                .build()
            )
            .collect(Collectors.toList());
        when(lmsClient.getLogisticsPoints(refEq(filter))).thenReturn(pickupPoints);
    }

    @Nonnull
    protected List<ScheduleDayResponse> fullSchedule() {
        return IntStream.rangeClosed(1, 7)
            .mapToObj(day -> createScheduleDay(day, day, 23))
            .collect(Collectors.toList());
    }

    @Nonnull
    protected ScheduleDayResponse createScheduleDay(long id, int day, int timeTo) {
        return new ScheduleDayResponse(
            id,
            day % 7,
            LocalTime.of(day % 24, 0),
            LocalTime.of(timeTo % 24, 0)
        );
    }

    @Nonnull
    protected DeliveryOption deliveryOption(Long id, Long deliveryServiceId, TariffType tariffType, int days) {
        return deliveryOptionBuilder(id, deliveryServiceId, tariffType, days)
            .pickupPoints(List.of(
                deliveryServiceId * 100,
                deliveryServiceId * 100 + 1,
                deliveryServiceId * 100 + 2
            ))
            .build();
    }

    @Nonnull
    protected DeliveryOption.DeliveryOptionBuilder deliveryOptionBuilder(
        Long id,
        Long deliveryServiceId,
        TariffType tariffType,
        int days
    ) {
        return DeliveryOption.builder()
            .tariffId(id)
            .deliveryServiceId(deliveryServiceId)
            .tariffType(tariffType)
            .minDays(days)
            .maxDays(days + 3)
            .cost(100500)
            .services(OrderTestUtils.defaultDeliveryOptionServices(100500));
    }

    @Nonnull
    protected GeoObject geoObject(int geoId, String addressLine) {
        return geoObject(geoId, addressLine, Precision.EXACT);
    }
    @Nonnull
    protected GeoObject geoObject(int geoId, String addressLine, Precision precision) {
        return SimpleGeoObject.newBuilder()
            .withToponymInfo(ToponymInfo.newBuilder()
                .withGeoid(String.valueOf(geoId))
                .withPrecision(precision)
                .build()
            )
            .withAddressInfo(AddressInfo.newBuilder()
                .withCountryInfo(CountryInfo.newBuilder().build())
                .withAreaInfo(AreaInfo.newBuilder().build())
                .withLocalityInfo(LocalityInfo.newBuilder().build())
                .withAddressLine(addressLine)
                .build()
            )
            .withBoundary(Boundary.newBuilder().build())
            .build();
    }

    @Nonnull
    protected GeoObject geoObject(
        int geoId,
        String addressLine,
        @Nullable String point,
        Kind kind,
        Component... components
    ) {
        return SimpleGeoObject.newBuilder()
            .withToponymInfo(ToponymInfo.newBuilder()
                .withGeoid(String.valueOf(geoId))
                .withKind(kind)
                .withPoint(Strings.nullToEmpty(point))
                .withComponents(List.of(components))
                .build()
            )
            .withAddressInfo(AddressInfo.newBuilder()
                .withCountryInfo(CountryInfo.newBuilder().build())
                .withAreaInfo(AreaInfo.newBuilder().build())
                .withLocalityInfo(LocalityInfo.newBuilder().build())
                .withAddressLine(addressLine)
                .build()
            )
            .withBoundary(Boundary.newBuilder().build())
            .build();
    }

    @Nonnull
    protected Component component(String name, Kind kind) {
        return new Component(name, List.of(kind));
    }

    @Nonnull
    protected PageResult<ShipmentSearchDto> shipmentPageResult(List<ShipmentSearchDto> result) {
        return PageResult.of(result, result.size(), 0, MAX_VALUE);
    }

    @Nonnull
    protected ShipmentSearchFilterBuilder getUpcomingShipmentApplicationsFilterBuilder() {
        return ShipmentSearchFilter.builder()
            .marketIdFrom(2000L)
            .warehousesFrom(Set.of(1L))
            .fromDate(TODAY)
            .withApplication(true)
            .statuses(EnumSet.of(
                ShipmentApplicationStatus.CREATED,
                ShipmentApplicationStatus.REGISTRY_SENT,
                ShipmentApplicationStatus.REGISTRY_PROCESSING_ERROR
            ));
    }

    @Nonnull
    protected ShipmentSearchFilter getUpcomingShipmentApplicationsFilter() {
        return getUpcomingShipmentApplicationsFilterBuilder().build();
    }

    protected void verifyLomSearchShipments(LocalDate date) {
        verifyLomSearchShipments(date, date);
    }

    protected void verifyLomSearchShipments(LocalDate fromDate, LocalDate toDate) {
        verify(lomClient).searchShipments(
            eq(ShipmentSearchFilter.builder()
                .marketIdFrom(2000L)
                .warehousesFrom(Set.of(1L))
                .fromDate(fromDate)
                .toDate(toDate)
                .build()),
            safeRefEq(SHIPMENT_PAGEABLE)
        );
    }

    protected void verifyLomSearchTodayShipmentApplications() {
        verify(lomClient).searchShipments(eq(getUpcomingShipmentApplicationsFilter()), safeRefEq(SHIPMENT_PAGEABLE));
    }

    protected void verifyGetActiveWarehousesByIds(Long... ids) {
        verify(lmsClient).getLogisticsPoints(
            LogisticsPointFilter.newBuilder()
                .type(PointType.WAREHOUSE)
                .ids(Set.of(ids))
                .active(true)
                .build()
        );
    }

    protected void verifySearchSortingCenterPartnerRelations() {
        verify(lmsClient).searchPartnerRelation(
            PartnerRelationFilter.newBuilder()
                .fromPartnersIds(Set.of(SORTING_CENTER_ID))
                .enabled(true)
                .build()
        );
    }

    @Nonnull
    protected ResultActions search(Consumer<DeliveryOptionsFilter> filterAdjuster) throws Exception {
        return search(filterAdjuster, 1L);
    }

    @Nonnull
    protected abstract ResultActions search(
        @Nonnull Consumer<DeliveryOptionsFilter> filterAdjuster,
        @Nullable Long senderId
    ) throws Exception;

    @Nonnull
    protected SearchPartnerFilter searchPartnerFilter(Set<Long> ids) {
        return SearchPartnerFilter.builder()
            .setPlatformClientIds(Set.of(PlatformClientId.YANDEX_DELIVERY.getId()))
            .setStatuses(EnumSet.of(
                PartnerStatus.ACTIVE,
                PartnerStatus.TESTING
            ))
            .setIds(ids)
            .setPlatformClientStatuses(Set.of(PartnerStatus.ACTIVE))
            .build();
    }
}
