package ru.yandex.market.logistics.logistics4go.controller.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import yandex.market.combinator.v0.CombinatorOuterClass;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.logistics4go.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4go.client.CombinatorGrpcClient;
import ru.yandex.market.logistics.logistics4go.client.model.CreateOrderRequest;
import ru.yandex.market.logistics.logistics4go.client.model.DeliveryType;
import ru.yandex.market.logistics.logistics4go.client.model.Dimensions;
import ru.yandex.market.logistics.logistics4go.client.model.ErrorType;
import ru.yandex.market.logistics.logistics4go.client.model.ItemInstanceDto;
import ru.yandex.market.logistics.logistics4go.client.model.OrderTag;
import ru.yandex.market.logistics.logistics4go.client.model.PaymentMethod;
import ru.yandex.market.logistics.logistics4go.client.model.ValidationError;
import ru.yandex.market.logistics.logistics4go.client.model.ValidationViolation;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.validatedWith;
import static ru.yandex.market.logistics.logistics4go.utils.CombinatorFactory.combinatorDeliveryRouteBuilder;
import static ru.yandex.market.logistics.logistics4go.utils.CombinatorFactory.combinatorPointBuilder;
import static ru.yandex.market.logistics.logistics4go.utils.CombinatorFactory.combinatorRequestBuilder;
import static ru.yandex.market.logistics.logistics4go.utils.CombinatorFactory.combinatorRouteBuilder;
import static ru.yandex.market.logistics.logistics4go.utils.OrderFactory.CIS_FULL;
import static ru.yandex.market.logistics.logistics4go.utils.OrderFactory.baseCreateOrderRequest;
import static ru.yandex.market.logistics.logistics4go.utils.OrderFactory.c2cModifier;
import static ru.yandex.market.logistics.logistics4go.utils.OrderFactory.item;
import static ru.yandex.market.logistics.logistics4go.utils.OrderFactory.modifier;
import static ru.yandex.market.logistics.logistics4go.utils.OrderFactory.place;

@DisplayName("Валидация полей при создании заказа")
class CreateOrderValidationTest extends AbstractIntegrationTest {

    @Autowired
    private CombinatorGrpcClient combinatorGrpcClient;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2022-02-22T15:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(combinatorGrpcClient);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Валидация запроса")
    void validation(
        String caseName,
        UnaryOperator<CreateOrderRequest> requestModifier,
        ValidationViolation expectedViolation
    ) {
        CreateOrderRequest request = baseCreateOrderRequest(false, true);
        ValidationError actualValidationError = apiClient.orders().createOrder()
            .body(requestModifier.apply(request))
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);
        softly.assertThat(actualValidationError.getCode()).isEqualTo(ErrorType.VALIDATION_ERROR);
        softly.assertThat(actualValidationError.getErrors())
            .containsExactly(expectedViolation);
    }

    @Test
    @DisplayName("Упало несколько валидаций на проверке дат (все даты раньше сегодняшнего дня + нарушен порядок)")
    void validateDatesMultipleBeforeToday() {
        CreateOrderRequest request = modifier(
            r -> r.getDeliveryOption().getHanding(),
            r -> r.dateMax(LocalDate.of(2022, 2, 20)).dateMin(LocalDate.of(2022, 2, 21)),
            CreateOrderRequest.class
        ).apply(baseCreateOrderRequest(false, true));

        ValidationError actualValidationError = apiClient.orders().createOrder()
            .body(request)
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);
        softly.assertThat(actualValidationError.getCode()).isEqualTo(ErrorType.VALIDATION_ERROR);
        softly.assertThat(actualValidationError.getErrors())
            .containsExactlyInAnyOrderElementsOf(List.of(
                new ValidationViolation()
                    .field("deliveryOption.handing.dateMin")
                    .message("must not be earlier than today"),
                new ValidationViolation()
                    .field("deliveryOption.handing.dateMax")
                    .message("must not be earlier than today"),
                new ValidationViolation()
                    .field("deliveryOption.handing.dateMax")
                    .message("must not be earlier than 'deliveryOption.handing.dateMin'")
            ));
    }

    @Test
    @DisplayName("Упало несколько валидаций на проверке дат (все даты позже 30 дней + нарушен порядок)")
    void validateDatesMultipleAfter30Days() {
        CreateOrderRequest request = modifier(
            r -> r.getDeliveryOption().getHanding(),
            r -> r.dateMax(LocalDate.of(2022, 3, 25)).dateMin(LocalDate.of(2022, 3, 26)),
            CreateOrderRequest.class
        ).apply(baseCreateOrderRequest(false, true));

        ValidationError actualValidationError = apiClient.orders().createOrder()
            .body(request)
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);
        softly.assertThat(actualValidationError.getCode()).isEqualTo(ErrorType.VALIDATION_ERROR);
        softly.assertThat(actualValidationError.getErrors())
            .containsExactlyInAnyOrderElementsOf(List.of(
                new ValidationViolation()
                    .field("deliveryOption.handing.dateMin")
                    .message("must not be later than now + 30 days"),
                new ValidationViolation()
                    .field("deliveryOption.handing.dateMax")
                    .message("must not be later than now + 30 days"),
                new ValidationViolation()
                    .field("deliveryOption.handing.dateMax")
                    .message("must not be earlier than 'deliveryOption.handing.dateMin'")
            ));
    }

    @Test
    @DisplayName("Началом маршрута указан сегмент партнера не DELIVERY и не SORTING_CENTER")
    void invalidRouteStart() {
        when(combinatorGrpcClient.getDeliveryRouteFromPoint(combinatorRequestBuilder(false, true).build()))
            .thenReturn(
                combinatorDeliveryRouteBuilder(true).setRoute(
                        combinatorRouteBuilder(true)
                            .addPoints(3, combinatorPointBuilder().setPartnerType("FULFILLMENT"))
                            .addPaths(
                                CombinatorOuterClass.Route.Path.newBuilder().setPointTo(1).setPointFrom(3).build()
                            )
                            .build()
                    )
                    .build()
            );

        ValidationError error = apiClient.orders().createOrder()
            .body(baseCreateOrderRequest(false, true))
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);

        softly.assertThat(error.getCode()).isEqualTo(ErrorType.VALIDATION_ERROR);

        ValidationViolation violation = error.getErrors().get(0);
        softly.assertThat(violation.getField()).isEqualTo("deliveryOption.inbound.toSegmentId");
        softly
            .assertThat(violation.getMessage())
            .isEqualTo(
                "Invalid partner type for inbound segment: FULFILLMENT. " +
                    "Allowed partner types: [DELIVERY, SORTING_CENTER]"
            );

        verify(combinatorGrpcClient).getDeliveryRouteFromPoint(combinatorRequestBuilder(false, true).build());
    }

    @Nonnull
    private static Stream<Arguments> validation() {
        return StreamEx.of(
                senderId(),
                externalId(),
                comment(),
                dimensions(r -> r.getPlaces().get(0).getDimensions(), "places[0].dimensions"),
                dimensions(r -> r.getItems().get(0).getDimensions(), "items[0].dimensions"),
                cost(),
                recipient(),
                items(),
                places(),
                deliveryOption(),
                dates(),
                intervals(),
                address(),
                tags(),
                c2cOrder()
            )
            .flatMap(Function.identity())
            .mapToEntry(Pair::getRight)
            .mapValues(v -> "%s %s".formatted(v.getField(), v.getMessage()))
            .mapKeyValue((pair, caseName) -> Arguments.of(caseName, pair.getLeft(), pair.getRight()));
    }

    @Nonnull
    private static Stream<Pair<UnaryOperator<CreateOrderRequest>, ValidationViolation>> senderId() {
        return Stream.of(
            Pair.of(
                r -> r.senderId(null),
                new ValidationViolation().field("senderId").message("must not be null")
            )
        );
    }

    @Nonnull
    private static Stream<Pair<UnaryOperator<CreateOrderRequest>, ValidationViolation>> externalId() {
        return Stream.of(
            Pair.of(
                r -> r.externalId(null),
                new ValidationViolation().field("externalId").message("must not be null")
            ),
            Pair.of(
                r -> r.externalId(""),
                new ValidationViolation().field("externalId").message("size must be between 1 and 50")
            ),
            Pair.of(
                r -> r.externalId("a".repeat(51)),
                new ValidationViolation().field("externalId").message("size must be between 1 and 50")
            ),
            Pair.of(
                r -> r.externalId("#1234567890"),
                new ValidationViolation().field("externalId").message("must match \"^[a-zA-Z0-9\\-_\\/]*$\"")
            )
        );
    }

    @Nonnull
    private static Stream<Pair<UnaryOperator<CreateOrderRequest>, ValidationViolation>> comment() {
        return Stream.of(
            Pair.of(
                r -> r.comment(""),
                new ValidationViolation().field("comment").message("size must be between 1 and 1000")
            ),
            Pair.of(
                r -> r.comment("a".repeat(1001)),
                new ValidationViolation().field("comment").message("size must be between 1 and 1000")
            )
        );
    }

    @Nonnull
    private static Stream<Pair<UnaryOperator<CreateOrderRequest>, ValidationViolation>> dimensions(
        Function<CreateOrderRequest, Dimensions> getter,
        String pathPrefix
    ) {
        return Stream.of(
            Pair.of(
                modifier(getter, r -> r.height(null), CreateOrderRequest.class),
                new ValidationViolation()
                    .field(String.join(".", pathPrefix, "height"))
                    .message("must not be null")
            ),
            Pair.of(
                modifier(getter, r -> r.height(501), CreateOrderRequest.class),
                new ValidationViolation()
                    .field(String.join(".", pathPrefix, "height"))
                    .message("must be less than or equal to 500")
            ),
            Pair.of(
                modifier(getter, r -> r.height(0), CreateOrderRequest.class),
                new ValidationViolation()
                    .field(String.join(".", pathPrefix, "height"))
                    .message("must be greater than or equal to 1")
            ),
            Pair.of(
                modifier(getter, r -> r.length(null), CreateOrderRequest.class),
                new ValidationViolation()
                    .field(String.join(".", pathPrefix, "length"))
                    .message("must not be null")
            ),
            Pair.of(
                modifier(getter, r -> r.length(501), CreateOrderRequest.class),
                new ValidationViolation()
                    .field(String.join(".", pathPrefix, "length"))
                    .message("must be less than or equal to 500")
            ),
            Pair.of(
                modifier(getter, r -> r.length(0), CreateOrderRequest.class),
                new ValidationViolation()
                    .field(String.join(".", pathPrefix, "length"))
                    .message("must be greater than or equal to 1")
            ),
            Pair.of(
                modifier(getter, r -> r.width(null), CreateOrderRequest.class),
                new ValidationViolation()
                    .field(String.join(".", pathPrefix, "width"))
                    .message("must not be null")
            ),
            Pair.of(
                modifier(getter, r -> r.width(501), CreateOrderRequest.class),
                new ValidationViolation()
                    .field(String.join(".", pathPrefix, "width"))
                    .message("must be less than or equal to 500")
            ),
            Pair.of(
                modifier(getter, r -> r.width(0), CreateOrderRequest.class),
                new ValidationViolation()
                    .field(String.join(".", pathPrefix, "width"))
                    .message("must be greater than or equal to 1")
            ),
            Pair.of(
                modifier(getter, r -> r.weight(null), CreateOrderRequest.class),
                new ValidationViolation()
                    .field(String.join(".", pathPrefix, "weight"))
                    .message("must not be null")
            ),
            Pair.of(
                modifier(getter, r -> r.weight(new BigDecimal("1500.1")), CreateOrderRequest.class),
                new ValidationViolation()
                    .field(String.join(".", pathPrefix, "weight"))
                    .message("must be less than or equal to 1500")
            ),
            Pair.of(
                modifier(getter, r -> r.weight(BigDecimal.ZERO), CreateOrderRequest.class),
                new ValidationViolation()
                    .field(String.join(".", pathPrefix, "weight"))
                    .message("must be positive")
            )
        );
    }

    @Nonnull
    private static Stream<Pair<UnaryOperator<CreateOrderRequest>, ValidationViolation>> cost() {
        return StreamEx.of(PaymentMethod.values())
            .filter(Predicate.not(PaymentMethod.PREPAID::equals))
            .map(
                paymentMethod -> Pair.of(
                    (UnaryOperator<CreateOrderRequest>) r -> {
                        r.getItems().forEach(i -> i.setPrice(BigDecimal.ZERO));
                        r.getCost().setDeliveryForCustomer(BigDecimal.ZERO);
                        r.getCost().setPaymentMethod(paymentMethod);
                        return r;
                    },
                    new ValidationViolation()
                        .field("cost.paymentMethod")
                        .message("postpaid order must not have zero cost")
                )
            )
            .append(
                Pair.of(
                    modifier(CreateOrderRequest::getCost, r -> r.paymentMethod(null), CreateOrderRequest.class),
                    new ValidationViolation().field("cost.paymentMethod").message("must not be null")
                )
            );
    }

    @Nonnull
    private static Stream<Pair<UnaryOperator<CreateOrderRequest>, ValidationViolation>> recipient() {
        return Stream.of(
            Pair.of(
                modifier(CreateOrderRequest::getRecipient, r -> r.name(null), CreateOrderRequest.class),
                new ValidationViolation().field("recipient.name").message("must not be null")
            ),
            Pair.of(
                modifier(r -> r.getRecipient().getName(), r -> r.lastName(null), CreateOrderRequest.class),
                new ValidationViolation().field("recipient.name.lastName").message("must not be null")
            ),
            Pair.of(
                modifier(r -> r.getRecipient().getName(), r -> r.lastName(""), CreateOrderRequest.class),
                new ValidationViolation().field("recipient.name.lastName").message("size must be between 1 and 100")
            ),
            Pair.of(
                modifier(r -> r.getRecipient().getName(), r -> r.lastName("a".repeat(101)), CreateOrderRequest.class),
                new ValidationViolation().field("recipient.name.lastName").message("size must be between 1 and 100")
            ),
            Pair.of(
                modifier(r -> r.getRecipient().getName(), r -> r.firstName(null), CreateOrderRequest.class),
                new ValidationViolation().field("recipient.name.firstName").message("must not be null")
            ),
            Pair.of(
                modifier(r -> r.getRecipient().getName(), r -> r.firstName(""), CreateOrderRequest.class),
                new ValidationViolation().field("recipient.name.firstName").message("size must be between 1 and 100")
            ),
            Pair.of(
                modifier(r -> r.getRecipient().getName(), r -> r.firstName("a".repeat(101)), CreateOrderRequest.class),
                new ValidationViolation().field("recipient.name.firstName").message("size must be between 1 and 100")
            ),
            Pair.of(
                modifier(r -> r.getRecipient().getName(), r -> r.middleName(""), CreateOrderRequest.class),
                new ValidationViolation().field("recipient.name.middleName").message("size must be between 1 and 100")
            ),
            Pair.of(
                modifier(r -> r.getRecipient().getName(), r -> r.middleName("a".repeat(101)), CreateOrderRequest.class),
                new ValidationViolation().field("recipient.name.middleName").message("size must be between 1 and 100")
            ),
            Pair.of(
                modifier(CreateOrderRequest::getRecipient, r -> r.email(""), CreateOrderRequest.class),
                new ValidationViolation().field("recipient.email").message("size must be between 1 and 100")
            ),
            Pair.of(
                modifier(CreateOrderRequest::getRecipient, r -> r.email("a".repeat(101)), CreateOrderRequest.class),
                new ValidationViolation().field("recipient.email").message("size must be between 1 and 100")
            ),
            Pair.of(
                modifier(r -> r.getRecipient().getPhone(), r -> r.number(null), CreateOrderRequest.class),
                new ValidationViolation().field("recipient.phone.number").message("must not be null")
            ),
            Pair.of(
                modifier(r -> r.getRecipient().getPhone(), r -> r.number(""), CreateOrderRequest.class),
                new ValidationViolation().field("recipient.phone.number").message("size must be between 1 and 50")
            ),
            Pair.of(
                modifier(r -> r.getRecipient().getPhone(), r -> r.number("8".repeat(51)), CreateOrderRequest.class),
                new ValidationViolation().field("recipient.phone.number").message("size must be between 1 and 50")
            ),
            Pair.of(
                modifier(
                    r -> r.getRecipient().getPhone(),
                    r -> r.number("invalid_phone_number"),
                    CreateOrderRequest.class
                ),
                new ValidationViolation().field("recipient.phone.number").message("invalid phone number")
            )
        );
    }

    @Nonnull
    private static Stream<Pair<UnaryOperator<CreateOrderRequest>, ValidationViolation>> items() {
        return Stream.of(
            Pair.of(
                r -> r.items(null),
                new ValidationViolation().field("items").message("must not be null")
            ),
            Pair.of(
                r -> r.items(List.of()),
                new ValidationViolation().field("items").message("size must be between 1 and 100")
            ),
            Pair.of(
                r -> r.items(IntStream.range(0, 101).mapToObj(i -> item(false)).collect(Collectors.toList())),
                new ValidationViolation().field("items").message("size must be between 1 and 100")
            ),
            Pair.of(
                modifier(r -> r.getItems().get(0), r -> r.externalId(null), CreateOrderRequest.class),
                new ValidationViolation().field("items[0].externalId").message("must not be null")
            ),
            Pair.of(
                modifier(r -> r.getItems().get(0), r -> r.externalId(""), CreateOrderRequest.class),
                new ValidationViolation().field("items[0].externalId").message("size must be between 1 and 50")
            ),
            Pair.of(
                modifier(r -> r.getItems().get(0), r -> r.externalId("a".repeat(51)), CreateOrderRequest.class),
                new ValidationViolation().field("items[0].externalId").message("size must be between 1 and 50")
            ),
            Pair.of(
                modifier(r -> r.getItems().get(0), r -> r.name(null), CreateOrderRequest.class),
                new ValidationViolation().field("items[0].name").message("must not be null")
            ),
            Pair.of(
                modifier(r -> r.getItems().get(0), r -> r.name(""), CreateOrderRequest.class),
                new ValidationViolation().field("items[0].name").message("size must be between 1 and 280")
            ),
            Pair.of(
                modifier(r -> r.getItems().get(0), r -> r.name("a".repeat(281)), CreateOrderRequest.class),
                new ValidationViolation().field("items[0].name").message("size must be between 1 and 280")
            ),
            Pair.of(
                modifier(r -> r.getItems().get(0), r -> r.count(null), CreateOrderRequest.class),
                new ValidationViolation().field("items[0].count").message("must not be null")
            ),
            Pair.of(
                modifier(r -> r.getItems().get(0), r -> r.price(null), CreateOrderRequest.class),
                new ValidationViolation().field("items[0].price").message("must not be null")
            ),
            Pair.of(
                modifier(r -> r.getItems().get(0), r -> r.tax(null), CreateOrderRequest.class),
                new ValidationViolation().field("items[0].tax").message("must not be null")
            ),
            Pair.of(
                modifier(r -> r.getItems().get(0), r -> r.supplier(null), CreateOrderRequest.class),
                new ValidationViolation().field("items[0].supplier").message("must not be null")
            ),
            Pair.of(
                modifier(r -> r.getItems().get(0).getSupplier(), r -> r.inn(null), CreateOrderRequest.class),
                new ValidationViolation().field("items[0].supplier.inn").message("must not be null")
            ),
            Pair.of(
                modifier(r -> r.getItems().get(0).getSupplier(), r -> r.inn("7736207544"), CreateOrderRequest.class),
                new ValidationViolation().field("items[0].supplier.inn").message("invalid supplier inn")
            ),
            Pair.of(
                modifier(r -> r.getItems().get(0).getInstances().get(0), r -> r.cis(null), CreateOrderRequest.class),
                new ValidationViolation()
                    .field("items[0].instances[0].cis")
                    .message("must not be null")
            ),
            Pair.of(
                modifier(
                    r -> r.getItems().get(0),
                    r -> r.placesExternalIds(List.of("unknown-ext-id")),
                    CreateOrderRequest.class
                ),
                new ValidationViolation()
                    .field("items[0].placesExternalIds")
                    .message("externalId 'unknown-ext-id' not found in 'places'")
            ),
            Pair.of(
                r -> r.items(List.of(item(false), item(false))),
                new ValidationViolation()
                    .field("items")
                    .message("externalId 'item[0].externalId' is not unique")
            ),
            Pair.of(
                modifier(
                    r -> r.getItems().get(0).getInstances().get(0),
                    r -> r.cis("invalid cis"),
                    CreateOrderRequest.class
                ),
                new ValidationViolation()
                    .field("items[0].instances[0].cis")
                    .message("item with external id 'item[0].externalId' has invalid cis: 'invalid cis'")
            ),
            Pair.of(
                modifier(
                    r -> r.getItems().get(0),
                    r -> r.instances(List.of(new ItemInstanceDto().cis(CIS_FULL), new ItemInstanceDto().cis(CIS_FULL))),
                    CreateOrderRequest.class
                ),
                new ValidationViolation()
                    .field("items[0]")
                    .message(
                        "instances size must not be greater than item count in order, "
                            + "instances size = 2, item count = 1"
                    )
            )
        );
    }

    @Nonnull
    private static Stream<Pair<UnaryOperator<CreateOrderRequest>, ValidationViolation>> places() {
        return Stream.of(
            Pair.of(
                r -> r.places(List.of()),
                new ValidationViolation().field("places").message("size must be between 1 and 100")
            ),
            Pair.of(
                r -> r.places(IntStream.range(0, 101).mapToObj(i -> place()).collect(Collectors.toList())),
                new ValidationViolation().field("places").message("size must be between 1 and 100")
            ),
            Pair.of(
                modifier(r -> r.getPlaces().get(0), r -> r.externalId(null), CreateOrderRequest.class),
                new ValidationViolation().field("places[0].externalId").message("must not be null")
            ),
            Pair.of(
                modifier(r -> r.getPlaces().get(0), r -> r.externalId(""), CreateOrderRequest.class),
                new ValidationViolation().field("places[0].externalId").message("size must be between 1 and 50")
            ),
            Pair.of(
                modifier(r -> r.getPlaces().get(0), r -> r.externalId("a".repeat(51)), CreateOrderRequest.class),
                new ValidationViolation().field("places[0].externalId").message("size must be between 1 and 50")
            ),
            Pair.of(
                modifier(r -> r.getPlaces().get(0), r -> r.dimensions(null), CreateOrderRequest.class),
                new ValidationViolation().field("places[0].dimensions").message("must not be null")
            ),
            Pair.of(
                modifier(r -> r.getPlaces().get(0).getDimensions(), r -> r.height(null), CreateOrderRequest.class),
                new ValidationViolation().field("places[0].dimensions.height").message("must not be null")
            ),
            Pair.of(
                modifier(r -> r.getPlaces().get(0).getDimensions(), r -> r.length(null), CreateOrderRequest.class),
                new ValidationViolation().field("places[0].dimensions.length").message("must not be null")
            ),
            Pair.of(
                modifier(r -> r.getPlaces().get(0).getDimensions(), r -> r.width(null), CreateOrderRequest.class),
                new ValidationViolation().field("places[0].dimensions.width").message("must not be null")
            ),
            Pair.of(
                modifier(r -> r.getPlaces().get(0).getDimensions(), r -> r.weight(null), CreateOrderRequest.class),
                new ValidationViolation().field("places[0].dimensions.weight").message("must not be null")
            )
        );
    }

    @Nonnull
    private static Stream<Pair<UnaryOperator<CreateOrderRequest>, ValidationViolation>> deliveryOption() {
        return Stream.of(
            Pair.of(
                r -> r.deliveryOption(null),
                new ValidationViolation().field("deliveryOption").message("must not be null")
            ),
            Pair.of(
                modifier(CreateOrderRequest::getDeliveryOption, r -> r.inbound(null), CreateOrderRequest.class),
                new ValidationViolation().field("deliveryOption.inbound").message("must not be null")
            ),
            Pair.of(
                modifier(r -> r.getDeliveryOption().getInbound(), r -> r.type(null), CreateOrderRequest.class),
                new ValidationViolation().field("deliveryOption.inbound.type").message("must not be null")
            ),
            Pair.of(
                modifier(r -> r.getDeliveryOption().getInbound(), r -> r.toSegmentId(null), CreateOrderRequest.class),
                new ValidationViolation().field("deliveryOption.inbound.toSegmentId").message("must not be null")
            ),
            Pair.of(
                modifier(CreateOrderRequest::getDeliveryOption, r -> r.handing(null), CreateOrderRequest.class),
                new ValidationViolation().field("deliveryOption.handing").message("must not be null")
            ),
            Pair.of(
                modifier(r -> r.getDeliveryOption().getHanding(), r -> r.deliveryType(null), CreateOrderRequest.class),
                new ValidationViolation().field("deliveryOption.handing.deliveryType").message("must not be null")
            ),
            Pair.of(
                modifier(
                    r -> r.getDeliveryOption().getHanding(),
                    r -> r.deliveryType(DeliveryType.PICKUP).pickupPointId(null),
                    CreateOrderRequest.class
                ),
                new ValidationViolation().field("deliveryOption.handing.pickupPointId").message("must not be null")
            )
        );
    }

    @Nonnull
    private static Stream<Pair<UnaryOperator<CreateOrderRequest>, ValidationViolation>> dates() {
        return Stream.of(
            Pair.of(
                modifier(r -> r.getDeliveryOption().getInbound(), r -> r.dateTime(null), CreateOrderRequest.class),
                new ValidationViolation().field("deliveryOption.inbound.dateTime").message("must not be null")
            ),
            Pair.of(
                modifier(r -> r.getDeliveryOption().getHanding(), r -> r.dateMin(null), CreateOrderRequest.class),
                new ValidationViolation().field("deliveryOption.handing.dateMin").message("must not be null")
            ),
            Pair.of(
                modifier(r -> r.getDeliveryOption().getHanding(), r -> r.dateMax(null), CreateOrderRequest.class),
                new ValidationViolation().field("deliveryOption.handing.dateMax").message("must not be null")
            ),
            Pair.of(
                modifier(
                    r -> r.getDeliveryOption().getInbound(),
                    r -> r.dateTime(Instant.parse("2022-02-22T14:59:59.00Z")),
                    CreateOrderRequest.class
                ),
                new ValidationViolation()
                    .field("deliveryOption.inbound.dateTime")
                    .message("must not be earlier than now")
            ),
            Pair.of(
                modifier(
                    r -> r.getDeliveryOption().getInbound(),
                    r -> r.dateTime(Instant.parse("2022-03-24T15:00:01.00Z")),
                    CreateOrderRequest.class
                ),
                new ValidationViolation()
                    .field("deliveryOption.inbound.dateTime")
                    .message("must not be later than now + 30 days")
            ),
            Pair.of(
                modifier(
                    r -> r.getDeliveryOption().getHanding(),
                    r -> r.dateMin(LocalDate.of(2022, 2, 21)),
                    CreateOrderRequest.class
                ),
                new ValidationViolation()
                    .field("deliveryOption.handing.dateMin")
                    .message("must not be earlier than today")
            ),
            Pair.of(
                modifier(
                    r -> r.getDeliveryOption().getHanding(),
                    r -> r.dateMax(LocalDate.of(2022, 3, 25)),
                    CreateOrderRequest.class
                ),
                new ValidationViolation()
                    .field("deliveryOption.handing.dateMax")
                    .message("must not be later than now + 30 days")
            ),
            Pair.of(
                modifier(
                    r -> r.getDeliveryOption().getHanding(),
                    r -> r.dateMin(LocalDate.of(2022, 2, 26)),
                    CreateOrderRequest.class
                ),
                new ValidationViolation()
                    .field("deliveryOption.handing.dateMax")
                    .message("must not be earlier than 'deliveryOption.handing.dateMin'")
            )
        );
    }

    @Nonnull
    private static Stream<Pair<UnaryOperator<CreateOrderRequest>, ValidationViolation>> intervals() {
        return Stream.of(
            Pair.of(
                modifier(r -> r.getDeliveryOption().getHanding(), r -> r.interval(null), CreateOrderRequest.class),
                new ValidationViolation().field("deliveryOption.handing.interval").message("must not be null")
            ),
            Pair.of(
                modifier(
                    r -> r.getDeliveryOption().getHanding().getInterval(),
                    r -> r.start(null),
                    CreateOrderRequest.class
                ),
                new ValidationViolation().field("deliveryOption.handing.interval.start").message("must not be null")
            ),
            Pair.of(
                modifier(
                    r -> r.getDeliveryOption().getHanding().getInterval(),
                    r -> r.end(null),
                    CreateOrderRequest.class
                ),
                new ValidationViolation().field("deliveryOption.handing.interval.end").message("must not be null")
            ),
            Pair.of(
                modifier(
                    r -> r.getDeliveryOption().getHanding().getInterval(),
                    r -> r.end(LocalTime.of(9, 59, 59)),
                    CreateOrderRequest.class
                ),
                new ValidationViolation()
                    .field("deliveryOption.handing.interval.end")
                    .message("must not be earlier than 'deliveryOption.handing.interval.start'")
            )
        );
    }

    @Nonnull
    private static Stream<Pair<UnaryOperator<CreateOrderRequest>, ValidationViolation>> address() {
        return Stream.of(
            Pair.of(
                modifier(r -> r.getDeliveryOption().getHanding(), r -> r.address(null), CreateOrderRequest.class),
                new ValidationViolation().field("deliveryOption.handing.address").message("must not be null")
            ),
            Pair.of(
                modifier(
                    r -> r.getDeliveryOption().getHanding().getAddress(),
                    r -> r.geoId(null),
                    CreateOrderRequest.class
                ),
                new ValidationViolation().field("deliveryOption.handing.address.geoId").message("must not be null")
            ),
            Pair.of(
                modifier(
                    r -> r.getDeliveryOption().getHanding().getAddress(),
                    r -> r.country(null),
                    CreateOrderRequest.class
                ),
                new ValidationViolation().field("deliveryOption.handing.address.country").message("must not be null")
            ),
            Pair.of(
                modifier(
                    r -> r.getDeliveryOption().getHanding().getAddress(),
                    r -> r.region(null),
                    CreateOrderRequest.class
                ),
                new ValidationViolation().field("deliveryOption.handing.address.region").message("must not be null")
            ),
            Pair.of(
                modifier(
                    r -> r.getDeliveryOption().getHanding().getAddress(),
                    r -> r.locality(null),
                    CreateOrderRequest.class
                ),
                new ValidationViolation().field("deliveryOption.handing.address.locality").message("must not be null")
            ),
            Pair.of(
                modifier(
                    r -> r.getDeliveryOption().getHanding().getAddress(),
                    r -> r.house(null),
                    CreateOrderRequest.class
                ),
                new ValidationViolation().field("deliveryOption.handing.address.house").message("must not be null")
            )
        );
    }

    @Nonnull
    private static Stream<Pair<UnaryOperator<CreateOrderRequest>, ValidationViolation>> tags() {
        return Stream.of(
            Pair.of(
                r -> r.tags(List.of()),
                new ValidationViolation().field("tags").message("size must be between 1 and 100")
            ),
            Pair.of(
                r -> r.tags(IntStream.range(0, 101).mapToObj(i -> OrderTag.C2C).collect(Collectors.toList())),
                new ValidationViolation().field("tags").message("size must be between 1 and 100")
            )
        );
    }

    @Nonnull
    private static Stream<Pair<UnaryOperator<CreateOrderRequest>, ValidationViolation>> c2cOrder() {
        return Stream.of(
            Pair.of(
                (UnaryOperator<CreateOrderRequest>) (r -> r.physicalPersonSender(null)),
                new ValidationViolation().field("physicalPersonSender").message("must not be null")
            ),
            Pair.of(
                modifier(
                    CreateOrderRequest::getPhysicalPersonSender,
                    r -> r.name(null),
                    CreateOrderRequest.class
                ),
                new ValidationViolation().field("physicalPersonSender.name").message("must not be null")
            ),
            Pair.of(
                modifier(
                    CreateOrderRequest::getPhysicalPersonSender,
                    r -> r.phone(null),
                    CreateOrderRequest.class
                ),
                new ValidationViolation().field("physicalPersonSender.phone").message("must not be null")
            ),
            Pair.of(
                modifier(
                    r -> r.getPhysicalPersonSender().getName(),
                    r -> r.firstName(null),
                    CreateOrderRequest.class
                ),
                new ValidationViolation().field("physicalPersonSender.name.firstName").message("must not be null")
            ),
            Pair.of(
                modifier(
                    r -> r.getPhysicalPersonSender().getName(),
                    r -> r.lastName(null),
                    CreateOrderRequest.class
                ),
                new ValidationViolation().field("physicalPersonSender.name.lastName").message("must not be null")
            ),
            Pair.of(
                modifier(
                    r -> r.getPhysicalPersonSender().getPhone(),
                    r -> r.number(null),
                    CreateOrderRequest.class
                ),
                new ValidationViolation().field("physicalPersonSender.phone.number").message("must not be null")
            ),
            Pair.of(
                modifier(
                    r -> r.getPhysicalPersonSender().getPhone(),
                    r -> r.number(""),
                    CreateOrderRequest.class
                ),
                new ValidationViolation()
                    .field("physicalPersonSender.phone.number")
                    .message("size must be between 1 and 50")
            ),
            Pair.of(
                modifier(
                    r -> r.getPhysicalPersonSender().getPhone(),
                    r -> r.number("8".repeat(51)),
                    CreateOrderRequest.class
                ),
                new ValidationViolation()
                    .field("physicalPersonSender.phone.number")
                    .message("size must be between 1 and 50")
            ),
            Pair.of(
                modifier(
                    r -> r.getPhysicalPersonSender().getPhone(),
                    r -> r.number("invalid_phone_number"),
                    CreateOrderRequest.class
                ),
                new ValidationViolation()
                    .field("physicalPersonSender.phone.number")
                    .message("invalid phone number")
            )
        ).map(pair -> Pair.of(c2cModifier().andThen(pair.getLeft())::apply, pair.getRight()));
    }
}
