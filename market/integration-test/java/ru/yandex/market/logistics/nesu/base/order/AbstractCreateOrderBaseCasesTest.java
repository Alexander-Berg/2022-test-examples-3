package ru.yandex.market.logistics.nesu.base.order;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import NSprav.UnifierReplyOuterClass;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.altay.unifier.HttpUnifierClient;
import ru.yandex.altay.unifier.UnificationRequest;
import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.model.response.AddressInfo;
import ru.yandex.common.geocoder.model.response.AreaInfo;
import ru.yandex.common.geocoder.model.response.Boundary;
import ru.yandex.common.geocoder.model.response.Component;
import ru.yandex.common.geocoder.model.response.CountryInfo;
import ru.yandex.common.geocoder.model.response.GeoObject;
import ru.yandex.common.geocoder.model.response.Kind;
import ru.yandex.common.geocoder.model.response.LocalityInfo;
import ru.yandex.common.geocoder.model.response.SimpleGeoObject;
import ru.yandex.common.geocoder.model.response.ToponymInfo;
import ru.yandex.common.util.collections.Quadruple;
import ru.yandex.market.logistics.delivery.calculator.client.DeliveryCalculatorSearchEngineClient;
import ru.yandex.market.logistics.delivery.calculator.client.model.DeliveryOption;
import ru.yandex.market.logistics.delivery.calculator.client.model.DeliverySearchRequest;
import ru.yandex.market.logistics.delivery.calculator.client.model.DeliverySearchResponse;
import ru.yandex.market.logistics.delivery.calculator.client.model.TariffType;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.AbstractOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.AddressDto;
import ru.yandex.market.logistics.lom.model.dto.DeliveryIntervalDto;
import ru.yandex.market.logistics.lom.model.dto.LocationDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.OrderServiceDto;
import ru.yandex.market.logistics.lom.model.dto.StorageUnitDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillOrderRequestDto;
import ru.yandex.market.logistics.lom.model.enums.PaymentMethod;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.logistics.lom.model.enums.ShipmentOption;
import ru.yandex.market.logistics.lom.model.enums.ShipmentType;
import ru.yandex.market.logistics.lom.model.enums.StorageUnitType;
import ru.yandex.market.logistics.lom.model.enums.VatType;
import ru.yandex.market.logistics.lom.model.enums.tags.OrderTag;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Direction;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.lom.model.search.Sort;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.api.model.Dimensions;
import ru.yandex.market.logistics.nesu.base.OrderTestUtils;
import ru.yandex.market.logistics.nesu.client.validation.ValidExternalId;
import ru.yandex.market.logistics.nesu.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils;
import ru.yandex.market.logistics.nesu.dto.MultiplaceItem;
import ru.yandex.market.logistics.nesu.dto.Place;
import ru.yandex.market.logistics.nesu.dto.order.OrderDraft;
import ru.yandex.market.logistics.nesu.dto.order.OrderDraftDeliveryOption;
import ru.yandex.market.logistics.nesu.dto.order.OrderDraftDeliveryOptionService;
import ru.yandex.market.logistics.nesu.dto.order.OrderDraftRecipient;
import ru.yandex.market.logistics.nesu.dto.order.OrderRecipientAddress;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.model.entity.ServiceType;
import ru.yandex.market.logistics.nesu.utils.SenderAvailableDeliveriesUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.base.OrderTestUtils.defaultDeliverySearchRequestBuilder;
import static ru.yandex.market.logistics.nesu.base.OrderTestUtils.getDefaultAddressBuilder;
import static ru.yandex.market.logistics.nesu.base.OrderTestUtils.getSimpleUnifierReplyWithAddress;
import static ru.yandex.market.logistics.nesu.base.OrderTestUtils.mockCourierSchedule;
import static ru.yandex.market.logistics.nesu.base.OrderTestUtils.mockDeliveryOptionValidation;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createDefaultPlace;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createDeliveryInterval;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createItem;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createKorobyte;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createLomOrderCost;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createOrderContact;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createOrderCost;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createPlace;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createPlaceUnitBuilder;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createRootUnit;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createWithdrawBuilder;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.defaultDeliveryOption;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.defaultDeliveryOptionServices;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.defaultLomDeliveryServices;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.deliveryServiceBuilder;
import static ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils.ownDeliveryFilter;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createLogisticsPointResponse;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createLogisticsPointResponseBuilder;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/repository/settings/delivery_type_service_settings.xml")
@DatabaseSetup("/repository/order/database_order_prepare.xml")
public abstract class AbstractCreateOrderBaseCasesTest extends AbstractContextualTest {

    protected static final LogisticsPointResponse WAREHOUSE_FROM =
        createLogisticsPointResponseBuilder(3L, null, "warehouse1", PointType.WAREHOUSE)
            .businessId(41L)
            .handlingTime(Duration.ofDays(2L))
            .build();
    protected static final LogisticsPointResponse WAREHOUSE_TO =
        createLogisticsPointResponse(4L, 202L, 5L, "warehouse2", PointType.WAREHOUSE);
    protected static final long SORTING_CENTER_ID = 6L;
    protected static final LogisticsPointResponse SORTING_CENTER_WAREHOUSE_TO =
        createLogisticsPointResponse(5L, 203L, SORTING_CENTER_ID, "warehouse3", PointType.WAREHOUSE);
    protected static final long SORTING_CENTER_ID_2 = 7L;
    protected static final LogisticsPointResponse SORTING_CENTER_WAREHOUSE_TO_2 =
        createLogisticsPointResponse(6L, 203L, SORTING_CENTER_ID_2, "warehouse4", PointType.WAREHOUSE);
    protected static final LogisticsPointResponse PICKUP_POINT =
        createLogisticsPointResponse(101L, 202L, 5L, "pick", PointType.PICKUP_POINT);
    protected static final int MAX_DELIVERY_DAYS = 5;
    protected static final LocalDate INITIAL_SHIPMENT_DATE = LocalDate.of(2019, 8, 1);
    private static final Instant INSTANT = Instant.parse("2019-02-02T12:00:00.00Z");
    protected static final long LOCAL_SORTING_CENTER_ID = 123456L;
    protected static final long LOCAL_SORTING_CENTER_PARTNER_ID = 123L;
    protected static final LogisticsPointResponse LOCAL_SORTING_CENTER =
        createLogisticsPointResponse(
            LOCAL_SORTING_CENTER_ID,
            204L,
            LOCAL_SORTING_CENTER_PARTNER_ID,
            "local sc for mk",
            PointType.WAREHOUSE
        );

    @Autowired
    protected LomClient lomClient;
    @Autowired
    protected LMSClient lmsClient;
    @Autowired
    protected GeoClient geoClient;
    @Autowired
    protected DeliveryCalculatorSearchEngineClient deliveryCalculatorSearchEngineClient;
    @Autowired
    protected HttpUnifierClient unifierClient;
    @Autowired
    protected FeatureProperties featureProperties;

    @BeforeEach
    void setup() {
        PartnerResponse sortingCenter = LmsFactory.createPartner(SORTING_CENTER_ID, 203L, PartnerType.SORTING_CENTER);

        List<PartnerResponse> deliveryServices = List.of(
            LmsFactory.createPartner(5L, PartnerType.DELIVERY),
            LmsFactory.createPartner(42L, PartnerType.DELIVERY),
            LmsFactory.createPartner(53916L, PartnerType.DELIVERY)
        );

        SenderAvailableDeliveriesUtils.mockGetSenderAvailableDeliveries(
            lmsClient,
            sortingCenter,
            deliveryServices,
            List.of(
                SORTING_CENTER_WAREHOUSE_TO,
                createLogisticsPointResponse(50L, 201L, 5L, "warehouse50", PointType.WAREHOUSE),
                createLogisticsPointResponse(420L, 201L, 42L, "warehouse420", PointType.WAREHOUSE)
            )
        );

        when(lmsClient.searchPartners(LmsFactory.createPartnerFilter(
            Set.of(5L),
            null,
            Set.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING)
        )))
            .thenReturn(List.of(LmsFactory.createPartner(5L, PartnerType.DELIVERY)));

        doReturn(List.of(TestOwnDeliveryUtils.partnerBuilder().build()))
            .when(lmsClient).searchPartners(ownDeliveryFilter().setMarketIds(Set.of(201L)).build());

        mockGetLogisticsPoints(WAREHOUSE_FROM, WAREHOUSE_TO, PICKUP_POINT);
        mockGetLogisticsPoints(WAREHOUSE_FROM, WAREHOUSE_TO);
        mockGetLogisticsPoints(PICKUP_POINT);

        mockGetLogisticsPoint(WAREHOUSE_FROM);
        mockGetLogisticsPoint(WAREHOUSE_TO);
        mockGetLogisticsPoint(SORTING_CENTER_WAREHOUSE_TO);
        mockGetLogisticsPoint(SORTING_CENTER_WAREHOUSE_TO_2);

        when(lmsClient.getPartner(SORTING_CENTER_ID_2))
            .thenReturn(Optional.of(LmsFactory.createPartner(SORTING_CENTER_ID_2, 203L, PartnerType.SORTING_CENTER)));

        when(lomClient.createOrder(any(WaybillOrderRequestDto.class), eq(false)))
            .thenReturn(new OrderDto().setId(1L));
        when(lomClient.searchShipments(any(), any()))
            .thenReturn(PageResult.empty(new Pageable(0, 1, new Sort(Direction.ASC, "id"))));

        when(lmsClient.getScheduleDay(1L))
            .thenReturn(Optional.of(LmsFactory.createScheduleDayDto(2)));

        mockCourierSchedule(lmsClient, 42, Set.of(5L));

        mockDeliveryOption(defaultDeliverySearchRequestBuilder().build());
        clock.setFixed(INSTANT, ZoneId.systemDefault());
        mockUnifierClient(getSimpleUnifierReplyWithAddress(getDefaultAddressBuilder().build()));
        when(featureProperties.getShopIdsAllowedToCreateOrders()).thenReturn(Set.of(1L));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME + " {1}")
    @MethodSource("orderDraftValidationProvider")
    @DisplayName("Валидация полей")
    void createOrderDraftValidation(
        String field,
        String error,
        String code,
        Map<String, Object> arguments,
        Consumer<OrderDraft> orderDraftAdjuster
    ) throws Exception {
        createOrder(OrderDtoFactory.defaultOrderDraft().andThen(orderDraftAdjuster))
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(fieldError(
                field,
                error,
                orderDraftObjectName(),
                code,
                arguments
            )));
    }

    protected abstract String orderDraftObjectName();

    @Nonnull
    private static Stream<Arguments> orderDraftValidationProvider() {
        return Stream.concat(
            Stream.of(
                    places(),
                    deliveryOption(),
                    cost()
                )
                .flatMap(Function.identity())
                .map(v -> Arguments.of(v.getFirst(), v.getSecond(), v.getThird(), Map.of(), v.getFourth())),
            Stream.of(
                    misc(),
                    dimensions(),
                    items(),
                    placesArgs()
                )
                .flatMap(Function.identity())
        );
    }

    @Nonnull
    private static Stream<Arguments> items() {
        return Stream.of(
            Arguments.of(
                "items",
                "size must be between 1 and 100",
                "Size",
                Map.of("min", 1, "max", 100),
                (Consumer<OrderDraft>) f -> f.setItems(List.of())
            ),
            Arguments.of(
                "items",
                "size must be between 1 and 100",
                "Size",
                Map.of("min", 1, "max", 100),
                (Consumer<OrderDraft>) f -> f.setItems(
                    IntStream.range(0, 101).mapToObj(i -> createItem()).collect(Collectors.toList())
                )
            ),
            Arguments.of(
                "items[0].placeExternalIds[0]",
                "place does not exist",
                "ValidItemPlaces",
                Map.of(),
                (Consumer<OrderDraft>) f -> {
                    f.setPlaces(List.of(createPlace(45, 30, 15, 50, List.of(createItem().setCount(1)))));
                    f.setItems(List.of(createItem().setPlaceExternalIds(List.of("non-exist"))));
                }
            ),
            Arguments.of(
                "items[0].placeExternalIds",
                "size must be between 1 and 100",
                "Size",
                Map.of("min", 1, "max", 100),
                (Consumer<OrderDraft>) f -> f.setItems(List.of(createItem().setPlaceExternalIds(List.of())))
            ),
            Arguments.of(
                "items[0].placeExternalIds",
                "size must be between 1 and 100",
                "Size",
                Map.of("min", 1, "max", 100),
                (Consumer<OrderDraft>) f -> {
                    f.setPlaces(List.of(createPlace(45, 30, 15, 50, null).setExternalId("id")));
                    f.setItems(List.of(createItem().setPlaceExternalIds(
                        IntStream.range(0, 101).mapToObj(i -> "id").collect(Collectors.toList())
                    )));
                }
            ),
            Arguments.of(
                "items[0].externalId",
                "size must be between 1 and 50",
                "Size",
                Map.of("min", 1, "max", 50),
                (Consumer<OrderDraft>) f -> f.setItems(List.of((MultiplaceItem) createItem().setExternalId("")))
            ),
            Arguments.of(
                "items[0].externalId",
                "size must be between 1 and 50",
                "Size",
                Map.of("min", 1, "max", 50),
                (Consumer<OrderDraft>) f -> f.setItems(
                    List.of((MultiplaceItem) createItem().setExternalId("1".repeat(51)))
                )
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> placesArgs() {
        return Stream.of(
            Arguments.of(
                "places",
                "size must be between 1 and 100",
                "Size",
                Map.of("min", 1, "max", 100),
                (Consumer<OrderDraft>) f -> f.setPlaces(
                    IntStream.range(0, 101).mapToObj(i -> createDefaultPlace(createItem())).collect(Collectors.toList())
                )
            ),
            Arguments.of(
                "places",
                "size must be between 1 and 100",
                "Size",
                Map.of("min", 1, "max", 100),
                (Consumer<OrderDraft>) f -> f.setPlaces(List.of())
            ),
            Arguments.of(
                "places[0].externalId",
                "size must be between 1 and 30",
                "Size",
                Map.of("min", 1, "max", 30),
                (Consumer<OrderDraft>) f -> f.setPlaces(List.of(createDefaultPlace(createItem()).setExternalId("")))
            ),
            Arguments.of(
                "places[0].externalId",
                "must contain only latin letters, digits, dashes, back and forward slashes, and underscore",
                "Pattern",
                Map.of("regexp", ValidExternalId.DEFAULT_EXTERNAL_ID_REGEXP),
                (Consumer<OrderDraft>) f -> f.setPlaces(List.of(createDefaultPlace(createItem()).setExternalId(" ")))
            ),
            Arguments.of(
                "places[0].externalId",
                "must contain only latin letters, digits, dashes, back and forward slashes, and underscore",
                "Pattern",
                Map.of("regexp", ValidExternalId.DEFAULT_EXTERNAL_ID_REGEXP),
                (Consumer<OrderDraft>) f -> f.setPlaces(List.of(
                    createDefaultPlace(createItem()).setExternalId("идентификатор_на_русском")
                ))
            ),
            Arguments.of(
                "places[0].externalId",
                "size must be between 1 and 30",
                "Size",
                Map.of("min", 1, "max", 30),
                (Consumer<OrderDraft>) f -> f.setPlaces(List.of(
                    createDefaultPlace(createItem()).setExternalId("1".repeat(31))
                ))
            ),
            Arguments.of(
                "places[0].items",
                "size must be between 1 and 100",
                "Size",
                Map.of("min", 1, "max", 100),
                (Consumer<OrderDraft>) f -> f.setPlaces(List.of(createPlace(45, 30, 15, 50, List.of())))
            ),
            Arguments.of(
                "places[0].items",
                "size must be between 1 and 100",
                "Size",
                Map.of("min", 1, "max", 100),
                (Consumer<OrderDraft>) f -> f.setPlaces(List.of(createPlace(
                    45,
                    30,
                    15,
                    50,
                    IntStream.range(0, 101).mapToObj(i -> createItem()).collect(Collectors.toList())
                )))
            ),
            Arguments.of(
                "place[0].items",
                "must not be empty",
                "ValidItemPlaces",
                Map.of(),
                (Consumer<OrderDraft>) f -> f.setPlaces(List.of(createPlace(45, 30, 15, 50, null))).setItems(null)
            ),
            Arguments.of(
                "place[1].items",
                "must not be empty",
                "ValidItemPlaces",
                Map.of(),
                (Consumer<OrderDraft>) f -> f.setPlaces(List.of(
                    createPlace(45, 30, 15, 50, List.of(createItem())),
                    createPlace(45, 30, 15, 50, null).setExternalId("ext_place_id-2")
                ))
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> misc() {
        return Stream.of(
            Arguments.of(
                "externalId",
                "must contain only latin letters, digits, dashes, back and forward slashes, and underscore",
                "Pattern",
                Map.of("regexp", ValidExternalId.DEFAULT_EXTERNAL_ID_REGEXP),
                (Consumer<OrderDraft>) f -> f.setExternalId(" abc ")
            ),
            Arguments.of(
                "externalId",
                "size must be between 1 and 10",
                "Size",
                Map.of("min", 1, "max", 10),
                (Consumer<OrderDraft>) f -> f.setExternalId("12345678901")
            ),
            Arguments.of(
                "externalId",
                "size must be between 1 and 10",
                "Size",
                Map.of("min", 1, "max", 10),
                (Consumer<OrderDraft>) f -> f.setExternalId("")
            ),
            Arguments.of(
                "contacts",
                "must not contain nulls",
                "NotNullElements",
                Map.of(),
                (Consumer<OrderDraft>) f -> f.setContacts(Collections.singletonList(null))
            ),
            Arguments.of(
                "contacts",
                "size must be between 0 and 100",
                "Size",
                Map.of("min", 0, "max", 100),
                (Consumer<OrderDraft>) f -> f.setContacts(
                    IntStream.range(0, 101).mapToObj(i -> createOrderContact()).collect(Collectors.toList())
                )
            ),
            Arguments.of(
                "deliveryOption.services",
                "size must be between 0 and 100",
                "Size",
                Map.of("min", 0, "max", 100),
                (Consumer<OrderDraft>) f -> {
                    OrderDraftDeliveryOption deliveryOption = defaultDeliveryOption();
                    deliveryOption.setServices(
                        IntStream.range(0, 101)
                            .mapToObj(i -> deliveryServiceBuilder().setCode(ServiceType.INSURANCE))
                            .collect(Collectors.toList())
                    );
                    f.setDeliveryOption(deliveryOption);
                }
            ),
            Arguments.of(
                "recipient.email",
                "must be a well-formed email address",
                "Email",
                Map.of("regexp", ".*"),
                (Consumer<OrderDraft>) f -> f.getRecipient().setEmail("not_email")
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME + " {1}")
    @MethodSource
    @DisplayName("Валидация полей")
    void extendedOrderDraftChecks(
        String field,
        String error,
        String code,
        Map<String, Object> arguments,
        Consumer<OrderDraft> orderDraftAdjuster
    ) throws Exception {
        when(featureProperties.getShopIdsForExtendedNeeds()).thenReturn(List.of(1L));
        createOrder(OrderDtoFactory.defaultOrderDraft().andThen(orderDraftAdjuster))
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(fieldError(
                field,
                error,
                orderDraftObjectName(),
                code,
                arguments
            )));
    }

    @Nonnull
    private static Stream<Arguments> extendedOrderDraftChecks() {
        return Stream.of(
            Arguments.of(
                "externalId",
                "size must be between 1 and 20",
                "Size",
                Map.of("min", 1, "max", 20),
                (Consumer<OrderDraft>) f -> f.setExternalId("0123456789_0123456789")
            ),
            Arguments.of(
                "places[0].items[0].supplierInn",
                "must not be null",
                "NotNull",
                Map.of(),
                (Consumer<OrderDraft>)
                    f -> f.setPlaces(List.of(new Place().setItems(List.of(createItem().setSupplierInn(null)))))
            ),
            Arguments.of(
                "places[0].items[0].supplierInn",
                "invalid inn",
                "ValidInn",
                Map.of(),
                (Consumer<OrderDraft>)
                    f -> f.setPlaces(List.of(new Place().setItems(
                        List.of(createItem().setSupplierInn("1".repeat(13)))
                    )))
            ),
            Arguments.of(
                "places[0].items[0].tax",
                "must not be null",
                "NotNull",
                Map.of(),
                (Consumer<OrderDraft>)
                    f -> f.setPlaces(List.of(new Place().setItems(List.of(createItem().setTax(null)))))
            )
        );
    }

    private static Stream<Quadruple<String, String, String, Consumer<OrderDraft>>> places() {
        return Stream.of(
            Quadruple.of(
                "places",
                "must not contain nulls",
                "NotNullElements",
                f -> f.setPlaces(Collections.singletonList(null))
            ),
            Quadruple.of(
                "places[0].items",
                "must not contain nulls",
                "NotNullElements",
                f -> f.setPlaces(List.of(createPlace(10, 10, 10, 10, Collections.singletonList(null))))
            ),
            Quadruple.of(
                "places[0].items[0].count",
                "must be greater than 0",
                "Positive",
                f -> f.setPlaces(List.of(createPlace(10, 10, 10, 10, List.of(createItem(10, -10, 10)))))
            ),
            Quadruple.of(
                "places[0].items[0].count",
                "must be greater than 0",
                "Positive",
                f -> f.setPlaces(List.of(createPlace(10, 10, 10, 10, List.of(createItem(10, 0, 10)))))
            ),
            Quadruple.of(
                "places[0].items[0].price",
                "must be greater than or equal to 0",
                "PositiveOrZero",
                f -> f.setPlaces(List.of(createPlace(10, 10, 10, 10, List.of(createItem(-10, 10, 10)))))
            ),
            Quadruple.of(
                "places[0].items[0].assessedValue",
                "must be greater than or equal to 0",
                "PositiveOrZero",
                f -> f.setPlaces(List.of(createPlace(10, 10, 10, 10, List.of(createItem(10, 10, -10)))))
            ),
            Quadruple.of(
                "dimensions.weight",
                "order weight must be greater than or equal to the sum of the weights of all places",
                "ValidOrderDimensions",
                f -> f
                    .setDimensions(
                        new Dimensions().setLength(300).setWidth(340).setHeight(220).setWeight(BigDecimal.valueOf(98))
                    )
                    .setPlaces(List.of(
                        createPlace(150, 170, 110, 50, List.of(createItem())),
                        createPlace(150, 170, 110, 50, List.of(createItem()))
                    ))
            ),
            Quadruple.of(
                "dimensions",
                "order volume must be greater than or equal to the sum of the volumes of all places",
                "ValidOrderDimensions",
                f -> f
                    .setDimensions(
                        new Dimensions().setLength(160).setWidth(180).setHeight(120).setWeight(BigDecimal.valueOf(100))
                    )
                    .setPlaces(List.of(
                        createPlace(150, 170, 110, 50, List.of(createItem())),
                        createPlace(150, 170, 110, 50, List.of(createItem()))
                    ))
            ),
            Quadruple.of(
                "places[1].dimensions",
                "order dimensions must not be less than dimensions of a place",
                "ValidOrderDimensions",
                f -> f
                    .setDimensions(
                        new Dimensions().setLength(500).setWidth(500).setHeight(100).setWeight(BigDecimal.valueOf(100))
                    )
                    .setPlaces(List.of(
                        createPlace(150, 170, 100, 50, List.of(createItem())),
                        createPlace(150, 170, 110, 50, List.of(createItem()))
                    ))
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> dimensions() {
        return Stream.of(
            Arguments.of(
                "places[0].dimensions.length",
                "must be less than or equal to 500",
                "Max",
                Map.of("value", 500),
                (Consumer<OrderDraft>) f -> f.setPlaces(List.of(createPlace(600, 10, 10, 10, List.of(createItem()))))
            ),
            Arguments.of(
                "places[0].dimensions.width",
                "must be less than or equal to 500",
                "Max",
                Map.of("value", 500),
                (Consumer<OrderDraft>) f -> f.setPlaces(List.of(createPlace(10, 600, 10, 10, List.of(createItem()))))
            ),
            Arguments.of(
                "places[0].dimensions.height",
                "must be less than or equal to 500",
                "Max",
                Map.of("value", 500),
                (Consumer<OrderDraft>) f -> f.setPlaces(List.of(createPlace(10, 10, 600, 10, List.of(createItem()))))
            ),
            Arguments.of(
                "places[0].dimensions.weight",
                "must be less than or equal to 1500",
                "Max",
                Map.of("value", 1500),
                (Consumer<OrderDraft>) f -> f.setPlaces(List.of(createPlace(10, 10, 10, 2000, List.of(createItem()))))
            ),
            Arguments.of(
                "places[0].items[0].dimensions.length",
                "must be less than or equal to 500",
                "Max",
                Map.of("value", 500),
                (Consumer<OrderDraft>) f -> f.setPlaces(List.of(createPlace(10, 10, 10, 10, List.of(
                    createItem().setDimensions(
                        new Dimensions().setLength(600).setWidth(10).setHeight(10).setWeight(BigDecimal.valueOf(10))
                    )
                ))))
            ),
            Arguments.of(
                "places[0].items[0].dimensions.width",
                "must be less than or equal to 500",
                "Max",
                Map.of("value", 500),
                (Consumer<OrderDraft>) f -> f.setPlaces(List.of(createPlace(10, 10, 10, 10, List.of(
                    createItem().setDimensions(
                        new Dimensions().setLength(10).setWidth(600).setHeight(10).setWeight(BigDecimal.valueOf(10))
                    )
                ))))
            ),
            Arguments.of(
                "places[0].items[0].dimensions.height",
                "must be less than or equal to 500",
                "Max",
                Map.of("value", 500),
                (Consumer<OrderDraft>) f -> f.setPlaces(List.of(createPlace(10, 10, 10, 10, List.of(
                    createItem().setDimensions(
                        new Dimensions().setLength(10).setWidth(10).setHeight(600).setWeight(BigDecimal.valueOf(10))
                    )
                ))))
            ),
            Arguments.of(
                "places[0].items[0].dimensions.weight",
                "must be less than or equal to 1500",
                "Max",
                Map.of("value", 1500),
                (Consumer<OrderDraft>) f -> f.setPlaces(List.of(createPlace(10, 10, 10, 10, List.of(
                    createItem().setDimensions(
                        new Dimensions().setLength(10).setWidth(10).setHeight(10).setWeight(BigDecimal.valueOf(2000))
                    )
                ))))
            )
        );
    }

    private static Stream<Quadruple<String, String, String, Consumer<OrderDraft>>> deliveryOption() {
        return Stream.of(
            Quadruple.of(
                "deliveryType",
                "must not be null",
                "NotNull",
                f -> f.setDeliveryType(null)
            ),
            Quadruple.of(
                "deliveryOption.delivery",
                "must be greater than or equal to 0",
                "PositiveOrZero",
                f -> f.getDeliveryOption().setDelivery(new BigDecimal(-1))
            ),
            Quadruple.of(
                "deliveryOption.tariffId",
                "must not be null",
                "NotNull",
                f -> f.getDeliveryOption().setTariffId(null)
            ),
            Quadruple.of(
                "deliveryOption.delivery",
                "must not be null",
                "NotNull",
                f -> f.getDeliveryOption().setDelivery(null)
            ),
            Quadruple.of(
                "deliveryOption.deliveryForCustomer",
                "must be greater than or equal to 0",
                "PositiveOrZero",
                f -> f.getDeliveryOption().setDeliveryForCustomer(new BigDecimal(-1))
            ),
            Quadruple.of(
                "deliveryOption.partnerId",
                "must not be null",
                "NotNull",
                f -> f.getDeliveryOption().setPartnerId(null)
            ),
            Quadruple.of(
                "deliveryOption.services",
                "must not contain nulls",
                "NotNullElements",
                f -> f.getDeliveryOption().setServices(Arrays.asList(
                    deliveryServiceBuilder().setCode(ServiceType.CASH_SERVICE),
                    null
                ))
            ),
            Quadruple.of(
                "deliveryOption.services[0].code",
                "must not be null",
                "NotNull",
                f -> f.getDeliveryOption().setServices(List.of(deliveryServiceBuilder()))
            ),
            Quadruple.of(
                "deliveryOption.services[0].cost",
                "must be greater than or equal to 0",
                "PositiveOrZero",
                f -> f.getDeliveryOption().setServices(List.of(
                    deliveryServiceBuilder().setCode(ServiceType.CASH_SERVICE).setCost(BigDecimal.ONE.negate())
                ))
            ),
            Quadruple.of(
                "deliveryOption.services[0].customerPay",
                "must not be null",
                "NotNull",
                f -> f.getDeliveryOption().setServices(List.of(
                    deliveryServiceBuilder().setCode(ServiceType.CASH_SERVICE).setCustomerPay(null)
                ))
            )
        );
    }

    private static Stream<Quadruple<String, String, String, Consumer<OrderDraft>>> cost() {
        return Stream.of(
            Quadruple.of(
                "cost.assessedValue",
                "must be greater than or equal to 0",
                "PositiveOrZero",
                f -> f.setCost(createOrderCost().setAssessedValue(new BigDecimal(-1)))
            ),
            Quadruple.of(
                "cost.manualDeliveryForCustomer",
                "must be greater than or equal to 0",
                "PositiveOrZero",
                f -> f.getCost().setManualDeliveryForCustomer(BigDecimal.ONE.negate())
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Успешное создание черновика заказа")
    @MethodSource("orderDraftModifier")
    void createOrderDraftTest(
        @SuppressWarnings("unused") String displayName,
        Consumer<OrderDraft> draftConsumer,
        Consumer<WaybillOrderRequestDto> lomOrderConsumer
    ) throws Exception {
        mockGetLogisticsPoints(WAREHOUSE_FROM, SORTING_CENTER_WAREHOUSE_TO, PICKUP_POINT);
        mockDeliveryOption(
            defaultDeliverySearchRequestBuilder()
                .weight(BigDecimal.valueOf(5))
                .length(50)
                .width(20)
                .height(30)
                .build()
        );

        createOrder(OrderDtoFactory.defaultOrderDraft().andThen(draftConsumer))
            .andExpect(status().isOk())
            .andExpect(content().json("1"));

        WaybillOrderRequestDto orderRequestDto = createLomOrderRequest();
        lomOrderConsumer.accept(orderRequestDto);
        verifyLomOrderCreate(orderRequestDto);
    }

    @Test
    @DisplayName("Успешное создание черновика заказа. Замена недоступного способа оплаты в ПВЗ CASH на доступный CARD")
    void createOrderDraftCorrectPaymentMethod() throws Exception {
        doReturn(List.of(
            WAREHOUSE_FROM,
            WAREHOUSE_TO,
            createLogisticsPointResponseBuilder(101L, 5L, "pick", PointType.PICKUP_POINT)
                .cardAllowed(true)
                .cashAllowed(false)
                .build()
        ))
            .when(lmsClient).getLogisticsPoints(LmsFactory.createLogisticsPointsFilter(Set.of(3L, 4L, 101L), true));

        createOrder(OrderDtoFactory.defaultOrderDraft())
            .andExpect(status().isOk())
            .andExpect(content().json("1"));

        WaybillOrderRequestDto orderRequestDto = createLomOrderRequest();
        orderRequestDto.setCost(createLomOrderCost().paymentMethod(PaymentMethod.CARD).build());
        verifyLomOrderCreate(orderRequestDto);
    }

    @Nonnull
    protected WaybillOrderRequestDto createMultiplaceLomOrderRequest() {
        return (WaybillOrderRequestDto) OrderDtoFactory.createMultiplaceLomOrderRequest()
            .setTags(Set.of(getTag()));
    }

    @Nonnull
    protected WaybillOrderRequestDto createLomOrderRequest() {
        return (WaybillOrderRequestDto) OrderDtoFactory.createLomOrderRequest().setTags(Set.of(getTag()));
    }

    @Nonnull
    private static Stream<Arguments> orderDraftModifier() {
        return Stream.of(
                miscModifiers(),
                recipientModifiers(),
                deliveryOptionModifiers(),
                costModifiers(),
                placesModifiers()
            )
            .flatMap(Function.identity())
            .map(t -> Arguments.of(t.getLeft(), t.getMiddle(), t.getRight()));
    }

    private static Stream<Triple<String, Consumer<OrderDraft>, Consumer<WaybillOrderRequestDto>>> miscModifiers() {
        return Stream.of(
            Triple.of(
                "(default)",
                orderDraft -> {
                },
                lomRequest -> {
                }
            ),
            Triple.of(
                "contacts",
                orderDraft -> orderDraft.setContacts(null),
                lomRequest -> lomRequest.setContacts(null)
            ),
            Triple.of(
                "externalId",
                orderDraft -> orderDraft.setExternalId(null),
                lomRequest -> lomRequest.setExternalId(null)
            ),
            Triple.of(
                "dimensions",
                orderDraft -> orderDraft
                    .setDimensions(
                        new Dimensions().setLength(50).setWidth(20).setHeight(30).setWeight(BigDecimal.valueOf(5))
                    )
                    .setPlaces(List.of(
                        createPlace(20, 20, 40, 2, List.of(createItem(10, 10, 100))),
                        createPlace(20, 50, 10, 3, List.of(createItem(10, 10, 100)))
                    )),
                lomRequest -> lomRequest
                    .setUnits(List.of(
                        createPlaceUnitBuilder()
                            .dimensions(createKorobyte(40, 20, 20, 2))
                            .build(),
                        createPlaceUnitBuilder()
                            .dimensions(createKorobyte(10, 50, 20, 3))
                            .build(),
                        StorageUnitDto.builder()
                            .type(StorageUnitType.ROOT)
                            .externalId("generated-0")
                            .dimensions(createKorobyte(30, 20, 50, 5))
                            .build()
                    ))
                    .setItems(List.of(
                        OrderDtoFactory.createLomItemBuilder()
                            .count(10)
                            .boxes(List.of(OrderDtoFactory.createItemBoxBuilder(List.of(0)).build()))
                            .build(),
                        OrderDtoFactory.createLomItemBuilder()
                            .count(10)
                            .boxes(List.of(OrderDtoFactory.createItemBoxBuilder(List.of(1)).build()))
                            .build()
                    ))
            )
        );
    }

    private static Stream<Triple<String, Consumer<OrderDraft>, Consumer<WaybillOrderRequestDto>>> recipientModifiers() {
        return Stream.of(
            Triple.of(
                "recipient.email with surrounding spaces",
                orderDraft -> orderDraft.getRecipient().setEmail(" recipient@email.com "),
                lomRequest -> lomRequest.setRecipient(
                    OrderDtoFactory
                        .createRecipientBuilder()
                        .email("recipient@email.com")
                        .build()
                )
            ),
            Triple.of(
                "recipient.email with russian letters",
                orderDraft -> orderDraft.getRecipient().setEmail("получатель@почта.рф"),
                lomRequest -> lomRequest.setRecipient(
                    OrderDtoFactory
                        .createRecipientBuilder()
                        .email("получатель@почта.рф")
                        .build()
                )
            ),
            Triple.of(
                "recipient",
                orderDraft -> orderDraft.setRecipient(null),
                lomRequest -> lomRequest
                    .setRecipient(null)
                    .setPickupPointId(null)
                    .setCost(createLomOrderCost(null).build())
            ),
            Triple.of(
                "recipient.address",
                orderDraft -> orderDraft.getRecipient().setAddress(null),
                lomRequest -> lomRequest
                    .setRecipient(OrderDtoFactory.createRecipientBuilder().address(null).build())
                    .setCost(createLomOrderCost(null).build())
            ),
            Triple.of(
                "recipient.address.geoId переопределение адреса",
                getRecipientAddressUpdater(
                    addressBuilder -> addressBuilder
                        .country("some_country")
                        .region("some_region")
                        .subRegion("some_subregion")
                        .locality("some_locality")
                ),
                lomRequest -> {
                }
            ),
            Triple.of(
                "recipient.address.geoId",
                getRecipientAddressUpdater(addressBuilder -> addressBuilder.geoId(null)),
                getLomRequestRecipientAddressUpdater(OrderDtoFactory.createAddressBuilder())
            ),
            Triple.of(
                "recipient.address.country",
                getRecipientAddressUpdater(
                    addressBuilder -> addressBuilder
                        .geoId(null)
                        .country(null)
                ),
                getLomRequestRecipientAddressUpdater(OrderDtoFactory.createAddressBuilder().country(null))
            ),
            Triple.of(
                "recipient.address.region",
                getRecipientAddressUpdater(
                    addressBuilder -> addressBuilder
                        .geoId(null)
                        .region(null)
                ),
                getLomRequestRecipientAddressUpdater(OrderDtoFactory.createAddressBuilder().region(null))
            ),
            Triple.of(
                "recipient.address.locality from subRegion",
                getRecipientAddressUpdater(
                    addressBuilder -> addressBuilder
                        .geoId(null)
                        .locality(null)
                        .subRegion("some_subregion")
                ),
                getLomRequestRecipientAddressUpdater(
                    OrderDtoFactory.createAddressBuilder()
                        .locality("some_subregion")
                        .subRegion("some_subregion")
                )
            ),
            Triple.of(
                "recipient.address.locality from region",
                getRecipientAddressUpdater(
                    addressBuilder -> addressBuilder
                        .geoId(null)
                        .locality(null)
                        .subRegion(null)
                        .region("some_region")
                ),
                getLomRequestRecipientAddressUpdater(
                    OrderDtoFactory.createAddressBuilder()
                        .locality("some_region")
                        .subRegion(null)
                        .region("some_region")
                )
            ),
            Triple.of(
                "recipient.address.postCode вместо postalCode",
                getRecipientAddressUpdater(
                    addressBuilder -> addressBuilder
                        .postCode("recipient_zip").postalCode(null)
                ),
                lomRequest -> {
                }
            ),
            Triple.of(
                "recipient.lastName",
                orderDraft -> orderDraft.getRecipient().setLastName(null),
                lomRequest -> lomRequest.setRecipient(OrderDtoFactory.createRecipientBuilder().lastName(null).build())
            ),
            Triple.of(
                "recipient.firstName",
                orderDraft -> orderDraft.getRecipient().setFirstName(null),
                lomRequest -> lomRequest.setRecipient(OrderDtoFactory.createRecipientBuilder().firstName(null).build())
            ),
            Triple.of(
                "recipient.middleName",
                orderDraft -> orderDraft.getRecipient().setMiddleName(null),
                lomRequest -> lomRequest.setRecipient(OrderDtoFactory.createRecipientBuilder().middleName(null).build())
            ),
            Triple.of(
                "recipient.email",
                orderDraft -> orderDraft.getRecipient().setEmail(null),
                lomRequest -> lomRequest.setRecipient(OrderDtoFactory.createRecipientBuilder().email(null).build())
            ),
            Triple.of(
                "recipient.email is empty",
                orderDraft -> orderDraft.getRecipient().setEmail(""),
                lomRequest -> lomRequest.setRecipient(
                    OrderDtoFactory
                        .createRecipientBuilder()
                        .email(null)
                        .build()
                )
            )
        );
    }

    private static Consumer<OrderDraft> getRecipientAddressUpdater(
        Consumer<OrderRecipientAddress.OrderRecipientAddressBuilder> addressBuilderConsumer
    ) {
        return orderDraft -> {
            OrderDraftRecipient recipient = orderDraft.getRecipient();
            OrderRecipientAddress.OrderRecipientAddressBuilder addressBuilder = recipient.getAddress().toBuilder();
            addressBuilderConsumer.accept(addressBuilder);
            recipient.setAddress(addressBuilder.build());
        };
    }

    private static Consumer<WaybillOrderRequestDto> getLomRequestRecipientAddressUpdater(
        AddressDto.AddressDtoBuilder addressDtoBuilder
    ) {
        return lomRequest -> lomRequest
            .setRecipient(
                OrderDtoFactory.createRecipientBuilder().address(
                    addressDtoBuilder
                        .geoId(null)
                        .latitude(null)
                        .longitude(null)
                        .build()
                ).build()
            )
            .setCost(createLomOrderCost(null).build());
    }

    private static Stream<Triple<String, Consumer<OrderDraft>, Consumer<WaybillOrderRequestDto>>>
    deliveryOptionModifiers() {
        return Stream.of(
            Triple.of(
                "deliveryOption.deliveryForCustomer",
                orderDraft -> orderDraft.getDeliveryOption().setDeliveryForCustomer(null),
                lomRequest -> lomRequest.setCost(
                    OrderDtoFactory.createLomOrderCost().deliveryForCustomer(null).build()
                )
            ),
            Triple.of(
                "deliveryOption, shipment",
                orderDraft -> {
                    orderDraft.setDeliveryOption(null);
                    orderDraft.setShipment(null);
                },
                lomRequest -> lomRequest
                    .setWaybill(List.of())
                    .setReturnSortingCenterId(null)
                    .setDeliveryInterval(DeliveryIntervalDto.builder().build())
                    .setCost(OrderDtoFactory.createLomOrderCost(null)
                        .delivery(null)
                        .deliveryForCustomer(null)
                        .services(null)
                        .tariffId(null).build())
            ),
            Triple.of(
                "deliveryOption.deliveryIntervalId",
                orderDraft -> orderDraft.getDeliveryOption().setDeliveryIntervalId(null),
                lomRequest -> lomRequest.setDeliveryInterval(
                    createDeliveryInterval().deliveryIntervalId(null).fromTime(null).toTime(null).build()
                )
            )
        );
    }

    private static Stream<Triple<String, Consumer<OrderDraft>, Consumer<WaybillOrderRequestDto>>> costModifiers() {
        return Stream.of(
            Triple.of(
                "cost",
                orderDraft -> {
                    orderDraft.setCost(null);
                    orderDraft.getDeliveryOption().setServices(defaultDeliveryOptionServices(null));
                },
                lomRequest -> lomRequest.setCost(
                    createLomOrderCost()
                        .paymentMethod(null)
                        .assessedValue(null)
                        .isFullyPrepaid(null)
                        .services(defaultLomDeliveryServices(null))
                        .build()
                )
            ),
            Triple.of(
                "cost.paymentMethod",
                orderDraft -> orderDraft.getCost().setPaymentMethod(null),
                lomRequest -> lomRequest.setCost(OrderDtoFactory.createLomOrderCost().paymentMethod(null).build())
            ),
            Triple.of(
                "cost.fullyPrepaid",
                orderDraft -> orderDraft.getCost().setFullyPrepaid(null),
                lomRequest -> lomRequest.setCost(OrderDtoFactory.createLomOrderCost().isFullyPrepaid(null).build())
            ),
            Triple.of(
                "cost.fullyPrepaid false",
                orderDraft -> orderDraft.getCost().setFullyPrepaid(false),
                lomRequest -> lomRequest.setCost(OrderDtoFactory.createLomOrderCost().isFullyPrepaid(false).build())
            ),
            Triple.of(
                "cost.assessedValue",
                orderDraft -> {
                    orderDraft.getCost().setAssessedValue(null);
                    orderDraft.getDeliveryOption().setServices(defaultDeliveryOptionServices(null));
                },
                lomRequest -> lomRequest.setCost(
                    OrderDtoFactory.createLomOrderCost()
                        .services(defaultLomDeliveryServices(null))
                        .assessedValue(null).build()
                )
            ),
            Triple.of(
                "cost.assessedValue 0",
                orderDraft -> {
                    orderDraft.getCost().setAssessedValue(BigDecimal.ZERO);
                    orderDraft.getDeliveryOption().setServices(defaultDeliveryOptionServices("0"));

                },
                lomRequest -> lomRequest.setCost(
                    OrderDtoFactory.createLomOrderCost()
                        .services(defaultLomDeliveryServices("0"))
                        .assessedValue(BigDecimal.ZERO).build()
                )
            ),
            Triple.of(
                "cost.manualDeliveryForCustomer",
                orderDraft -> orderDraft
                    .setDeliveryOption(null)
                    .setShipment(null)
                    .getCost().setManualDeliveryForCustomer(BigDecimal.valueOf(100)),
                lomRequest -> lomRequest
                    .setWaybill(List.of())
                    .setReturnSortingCenterId(null)
                    .setDeliveryInterval(DeliveryIntervalDto.builder().build())
                    .setCost(
                        OrderDtoFactory.createLomOrderCost(null)
                            .manualDeliveryForCustomer(BigDecimal.valueOf(100))
                            .tariffId(null)
                            .delivery(null)
                            .deliveryForCustomer(null)
                            .services(null)
                            .build()
                    )
            ),
            Triple.of(
                "cost.fullyPrepaid true",
                orderDraft -> {
                    orderDraft.getDeliveryOption()
                        .setServices(filterCashService(defaultDeliveryOptionServices("0.75")));
                    orderDraft.getCost().setFullyPrepaid(true);
                },
                lomRequest -> lomRequest
                    .setCost(OrderDtoFactory.createLomOrderCost()
                        .isFullyPrepaid(true)
                        .paymentMethod(PaymentMethod.PREPAID)
                        .services(filterLomCashService(defaultLomDeliveryServices("0.459", "0.75")))
                        .build()
                    )
            ),
            Triple.of(
                "cost.fullyPrepaid false, PaymentMethod PREPAID",
                orderDraft -> {
                    orderDraft.getCost().setFullyPrepaid(false);
                    orderDraft.getCost().setPaymentMethod(
                        ru.yandex.market.logistics.nesu.dto.enums.PaymentMethod.PREPAID
                    );
                },
                lomRequest -> lomRequest
                    .setCost(OrderDtoFactory.createLomOrderCost()
                        .isFullyPrepaid(false)
                        .paymentMethod(PaymentMethod.CASH)
                        .build()
                    )
            )
        );
    }

    private static Stream<Triple<String, Consumer<OrderDraft>, Consumer<WaybillOrderRequestDto>>> placesModifiers() {
        return Stream.of(
            Triple.of(
                "places",
                orderDraft -> orderDraft.setPlaces(null).setItems(List.of(createItem())),
                lomRequest -> lomRequest
                    .setWaybill(List.of(
                        createWithdrawBuilder(false).rootStorageUnitExternalId("generated-0").build())
                    )
                    .setItems(List.of(
                        OrderDtoFactory.createLomItemBuilder()
                            .boxes(List.of(
                                OrderDtoFactory.createItemBoxBuilder()
                                    .storageUnitExternalIds(null)
                                    .storageUnitIndexes(null)
                                    .build()
                            ))
                            .build()
                    ))
                    .setUnits(List.of(createRootUnit(null)))
                    .setCost(OrderDtoFactory.createLomOrderCost(null).build())
            ),
            Triple.of(
                "places.externalId",
                orderDraft -> orderDraft.setPlaces(List.of(createDefaultPlace(createItem()).setExternalId(null))),
                lomRequest -> lomRequest
                    .setItems(List.of(
                        OrderDtoFactory.createLomItemBuilder()
                            .boxes(List.of(
                                OrderDtoFactory.createItemBoxBuilder()
                                    .storageUnitExternalIds(null)
                                    .build()
                            ))
                            .build()
                    ))
                    .setUnits(List.of(
                        OrderDtoFactory.createPlaceUnitBuilder().externalId(null).build(),
                        OrderDtoFactory.createRootUnit()
                    ))
            ),
            Triple.of(
                "places.items.externalId",
                orderDraft -> orderDraft.setPlaces(List.of(createDefaultPlace(createItem().setExternalId(null)))),
                lomRequest -> lomRequest
                    .setItems(List.of(
                        OrderDtoFactory.createLomItemBuilder()
                            .article(null)
                            .build()
                    ))
            ),
            Triple.of(
                "places.items.name",
                orderDraft -> orderDraft.setPlaces(List.of(createDefaultPlace(createItem().setName(null)))),
                lomRequest -> lomRequest
                    .setItems(List.of(
                        OrderDtoFactory.createLomItemBuilder()
                            .name(null)
                            .build()
                    ))
            ),
            Triple.of(
                "places.items.dimensions",
                orderDraft -> orderDraft.setPlaces(List.of(createDefaultPlace(createItem().setDimensions(null)))),
                lomRequest -> lomRequest
                    .setItems(List.of(
                        OrderDtoFactory.createLomItemBuilder()
                            .dimensions(null)
                            .boxes(List.of(
                                OrderDtoFactory.createItemBoxBuilder().dimensions(null).build()
                            ))
                            .build()
                    ))
            ),
            Triple.of(
                "places.items.tax",
                orderDraft -> orderDraft.setPlaces(List.of(createDefaultPlace(createItem().setTax(null)))),
                lomRequest -> lomRequest
                    .setItems(List.of(
                        OrderDtoFactory.createLomItemBuilder()
                            .vatType(null)
                            .build()
                    ))
            ),
            Triple.of(
                "places.items.assessedValue",
                orderDraft -> orderDraft.setPlaces(List.of(createDefaultPlace(createItem().setAssessedValue(null)))),
                lomRequest -> lomRequest
                    .setItems(List.of(
                        OrderDtoFactory.createLomItemBuilder()
                            .assessedValue(OrderDtoFactory.createMonetary(null))
                            .build()
                    ))
            )
        );
    }

    @Nonnull
    protected Consumer<OrderDraft> createOrderThroughSortingCenter() {
        return OrderDtoFactory.defaultOrderDraft()
            .andThen(
                d -> d.getDeliveryOption()
                    .setPartnerId(5L)
                    .setServices(addSortService(OrderDtoFactory.defaultDeliveryOptionServices("3.86", "0.75")))
            )
            .andThen(d -> d.getShipment().setPartnerTo(SORTING_CENTER_ID).setWarehouseTo(5L))
            .andThen(d -> d.getRecipient().setPickupPointId(null));
    }

    @Nonnull
    protected WaybillOrderRequestDto sortingCenterOrder(LocationDto sortingCenterWarehouse) {
        WaybillOrderRequestDto orderDto = createLomOrderRequest();
        orderDto
            .setWaybill(
                List.of(
                    OrderDtoFactory.createWaybillSegmentBuilder(
                            INITIAL_SHIPMENT_DATE,
                            OrderDtoFactory.createLocation(3L),
                            sortingCenterWarehouse,
                            SORTING_CENTER_ID,
                            ShipmentType.WITHDRAW
                        )
                        .segmentType(SegmentType.SORTING_CENTER)
                        .build(),
                    OrderDtoFactory.createWaybillSegmentBuilder(
                            INITIAL_SHIPMENT_DATE,
                            sortingCenterWarehouse,
                            null,
                            5L,
                            ShipmentType.WITHDRAW
                        )
                        .segmentType(SegmentType.COURIER)
                        .build()
                )
            )
            .setPickupPointId(null)
            .setCost(
                OrderDtoFactory.createLomOrderCost()
                    .services(addLomSortService(OrderDtoFactory.defaultLomDeliveryServices("3.86", "0.75")))
                    .build()
            );
        return orderDto;
    }

    @Nonnull
    protected Consumer<OrderDraft> orderDraftWithDeliveryService() {
        return OrderDtoFactory.defaultOrderDraft().andThen(
            orderDraft -> orderDraft.getDeliveryOption().setServices(
                List.of(
                    new OrderDraftDeliveryOptionService()
                        .setCode(ServiceType.DELIVERY)
                        .setCost(BigDecimal.ONE)
                        .setCustomerPay(false),
                    new OrderDraftDeliveryOptionService()
                        .setCode(ServiceType.INSURANCE)
                        .setCost(BigDecimal.valueOf(0.75))
                        .setCustomerPay(false),
                    new OrderDraftDeliveryOptionService()
                        .setCode(ServiceType.CASH_SERVICE)
                        .setCost(BigDecimal.valueOf(3.400))
                        .setCustomerPay(false),
                    new OrderDraftDeliveryOptionService()
                        .setCode(ServiceType.RETURN)
                        .setCost(BigDecimal.valueOf(0.75))
                        .setCustomerPay(false),
                    new OrderDraftDeliveryOptionService()
                        .setCode(ServiceType.RETURN_SORT)
                        .setCost(BigDecimal.valueOf(20.000))
                        .setCustomerPay(false)
                )
            )
        );
    }

    @Nonnull
    protected GeoObject geoObjectWithParameters(Kind kind, String geoId) {
        return defaultGeoObjectWithParameters(kind, geoId, "", List.of());
    }

    @Nonnull
    protected GeoObject defaultGeoObjectWithParameters(
        Kind kind,
        String geoId,
        String point,
        List<Component> components
    ) {
        return SimpleGeoObject.newBuilder()
            .withToponymInfo(ToponymInfo.newBuilder()
                .withKind(kind)
                .withPoint(point)
                .withGeoid(geoId)
                .withComponents(components)
                .build()
            )
            .withAddressInfo(AddressInfo.newBuilder()
                .withCountryInfo(CountryInfo.newBuilder()
                    .withCountryName("Russia")
                    .build()
                )
                .withAreaInfo(AreaInfo.newBuilder()
                    .withAdministrativeAreaName("Novosibirsk region")
                    .withSubAdministrativeAreaName("Novosibirsk city area")
                    .build()
                )
                .withLocalityInfo(LocalityInfo.newBuilder()
                    .withLocalityName("Novosibirsk")
                    .withThoroughfareName("Nikolaeva")
                    .withPremiseNumber("11")
                    .withPostalCode("630630")
                    .build()
                )
                .build()
            )
            .withBoundary(Boundary.newBuilder().build())
            .build();
    }

    protected void mockGetLogisticsPoints(LogisticsPointResponse... logisticPoints) {
        Set<Long> ids = Arrays.stream(logisticPoints).map(LogisticsPointResponse::getId).collect(Collectors.toSet());
        when(lmsClient.getLogisticsPoints(refEq(LmsFactory.createLogisticsPointsFilter(ids, true))))
            .thenReturn(List.of(logisticPoints));
    }

    protected void mockGetLogisticsPoint(LogisticsPointResponse logisticPoint) {
        when(lmsClient.getLogisticsPoint(logisticPoint.getId())).thenReturn(Optional.of(logisticPoint));
    }

    protected void mockSearchSortingCenter(List<PartnerResponse> partners) {
        when(lmsClient.searchPartners(
            refEq(LmsFactory.createPartnerFilter(
                Set.of(SORTING_CENTER_ID, 5L),
                null,
                Set.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING)
            )))
        )
            .thenReturn(partners);
    }

    protected void mockSearchSortingCenter() {
        when(lmsClient.searchPartners(
            refEq(LmsFactory.createPartnerFilter(
                Set.of(SORTING_CENTER_ID, 5L),
                null,
                Set.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING)
            )))
        )
            .thenReturn(List.of(
                LmsFactory.createPartner(SORTING_CENTER_ID, PartnerType.SORTING_CENTER),
                LmsFactory.createPartner(5L, PartnerType.DELIVERY)
            ));
    }

    protected void mockDeliveryOptionNoPickupPoint() {
        mockDeliveryOption(defaultDeliverySearchRequestBuilder().pickupPoints(null).build());
    }

    protected void mockDeliveryOption(DeliverySearchRequest filter) {
        mockDeliveryOptionValidation(MAX_DELIVERY_DAYS, deliveryCalculatorSearchEngineClient, lmsClient, filter);
    }

    protected void mockUnifierClient(UnifierReplyOuterClass.UnifierReply expectedReply) {
        when(unifierClient.unify(any(UnificationRequest.class))).thenReturn(expectedReply);
    }

    protected void mockOwnDelivery() {
        doReturn(
            DeliverySearchResponse.builder()
                .deliveryOptions(List.of(
                    DeliveryOption.builder()
                        .cost(100)
                        .deliveryServiceId(45L)
                        .tariffType(TariffType.COURIER)
                        .maxDays(MAX_DELIVERY_DAYS)
                        .services(OrderTestUtils.defaultDeliveryOptionServices(100))
                        .build()
                ))
                .build()
        ).when(deliveryCalculatorSearchEngineClient).deliverySearch(safeRefEq(
            defaultDeliverySearchRequestBuilder()
                .deliveryServiceIds(Set.of(45L))
                .pickupPoints(null)
                .build())
        );

        doReturn(List.of(LmsFactory.createPartner(45L, PartnerType.OWN_DELIVERY)))
            .when(lmsClient)
            .searchPartners(LmsFactory.createPartnerFilter(
                Set.of(45L),
                null,
                Set.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING)
            ));

        doReturn(List.of(
            LmsFactory.createPartner(6L, PartnerType.SORTING_CENTER)
        ))
            .when(lmsClient)
            .searchPartners(LmsFactory.createPartnerFilter(Set.of(5L, /*53916L,*/ 6L, 42L), null));

        doReturn(List.of())
            .when(lmsClient)
            .searchPartnerRelation(
                PartnerRelationFilter.newBuilder()
                    .fromPartnersIds(Set.of(6L))
                    .enabled(true)
                    .build()
            );

        mockCourierSchedule(lmsClient, 42, Set.of(45L));
    }

    @Nonnull
    protected static List<OrderServiceDto> filterLomCashService(List<OrderServiceDto> orderServices) {
        return orderServices.stream()
            .filter(service -> service.getCode() != ShipmentOption.CASH_SERVICE)
            .collect(Collectors.toList());
    }

    @Nonnull
    protected static List<OrderDraftDeliveryOptionService> filterCashService(
        List<OrderDraftDeliveryOptionService> orderDraftDeliveryOptionServices
    ) {
        return orderDraftDeliveryOptionServices.stream()
            .filter(service -> service.getCode() != ServiceType.CASH_SERVICE)
            .collect(Collectors.toList());
    }

    @Nonnull
    protected List<OrderServiceDto> addLomSortService(List<OrderServiceDto> orderServices) {
        return StreamEx.of(orderServices)
            .append(createLomOrderService(ShipmentOption.SORT, new BigDecimal("27"), true))
            .toList();
    }

    @Nonnull
    protected List<OrderDraftDeliveryOptionService> addSortService(
        List<OrderDraftDeliveryOptionService> orderDraftDeliveryOptionServices
    ) {
        return StreamEx.of(orderDraftDeliveryOptionServices)
            .append(deliveryServiceBuilder(ServiceType.SORT, new BigDecimal("27"), true))
            .toList();
    }

    @Nonnull
    protected OrderServiceDto createLomOrderService(ShipmentOption type, BigDecimal cost) {
        return createLomOrderService(type, cost, false);
    }

    @Nonnull
    protected OrderServiceDto createLomOrderService(ShipmentOption type, BigDecimal cost, Boolean customerPay) {
        return OrderServiceDto.builder()
            .code(type)
            .cost(cost)
            .customerPay(customerPay)
            .build();
    }

    @Nonnull
    protected OrderServiceDto deliveryLomOrderService(SortedSet<VatType> vatTypes) {
        return OrderServiceDto.builder()
            .code(ShipmentOption.DELIVERY)
            .cost(BigDecimal.ONE)
            .customerPay(false)
            .taxes(vatTypes)
            .build();
    }

    protected void verifyLomOrderCreate(AbstractOrderRequestDto order) {
        ArgumentCaptor<WaybillOrderRequestDto> orderDtoCaptor = ArgumentCaptor.forClass(WaybillOrderRequestDto.class);
        verify(lomClient).createOrder(orderDtoCaptor.capture(), eq(false));
        softly.assertThat(orderDtoCaptor.getValue()).usingRecursiveComparison().isEqualTo(order);
    }

    @Nonnull
    protected ResultActions createOrder(Consumer<OrderDraft> orderDraftConsumer) throws Exception {
        return createOrder(orderDraftConsumer, 1L);
    }

    @Nonnull
    protected abstract ResultActions createOrder(Consumer<OrderDraft> orderDraftConsumer, Long senderId)
        throws Exception;

    @Nonnull
    protected abstract ResultActions createOrder(String fileName, Long senderId)
        throws Exception;

    @Nonnull
    protected abstract ResultActions createOrder(Consumer<OrderDraft> orderDraftConsumer, Long senderId, Long shopId)
        throws Exception;

    @Nonnull
    protected abstract OrderTag getTag();
}
