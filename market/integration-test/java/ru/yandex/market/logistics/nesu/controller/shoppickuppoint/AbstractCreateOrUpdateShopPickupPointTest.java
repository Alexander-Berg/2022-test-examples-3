package ru.yandex.market.logistics.nesu.controller.shoppickuppoint;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import NSprav.UnifierReplyOuterClass;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.altay.model.SignalOuterClass;
import ru.yandex.altay.unifier.HttpUnifierClient;
import ru.yandex.altay.unifier.UnificationRequest;
import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.model.response.AddressInfo;
import ru.yandex.common.geocoder.model.response.AreaInfo;
import ru.yandex.common.geocoder.model.response.Boundary;
import ru.yandex.common.geocoder.model.response.CountryInfo;
import ru.yandex.common.geocoder.model.response.GeoObject;
import ru.yandex.common.geocoder.model.response.LocalityInfo;
import ru.yandex.common.geocoder.model.response.SimpleGeoObject;
import ru.yandex.common.geocoder.model.response.ToponymInfo;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.client.enums.PickupPointType;
import ru.yandex.market.logistics.nesu.client.enums.ShopPickupPointStatus;
import ru.yandex.market.logistics.nesu.client.model.shoppickuppoints.ShopPickupPointAddressDto;
import ru.yandex.market.logistics.nesu.client.model.shoppickuppoints.ShopPickupPointMetaRequest;
import ru.yandex.market.logistics.nesu.client.model.shoppickuppoints.ShopPickupPointPhoneDto;
import ru.yandex.market.logistics.nesu.client.model.shoppickuppoints.ShopPickupPointRequest;
import ru.yandex.market.logistics.nesu.client.model.shoppickuppoints.ShopPickupPointScheduleDayDto;
import ru.yandex.market.logistics.nesu.client.model.shoppickuppoints.ShopPickupPointTariffRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.base.OrderTestUtils.getDefaultAddressBuilder;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ErrorType.NOT_BLANK;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ErrorType.NOT_NULL;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ErrorType.POSITIVE;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ErrorType.POSITIVE_OR_ZERO;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ErrorType.max;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ErrorType.min;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ValidationErrorDataBuilder;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldErrorBuilder;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.objectError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DisplayName("Валидация тела запроса создания/обновления ПВЗ и тарифа")
abstract class AbstractCreateOrUpdateShopPickupPointTest extends AbstractContextualTest {

    protected static final Map<String, String> QUERY_PARAMS = Map.of(
        "shopId", "1",
        "userId", "2"
    );
    protected static final int LOCATION_ID = 213;
    protected static final BigDecimal LATITUDE = new BigDecimal("55.653415");
    protected static final BigDecimal LONGITUDE = new BigDecimal("37.646280");

    @Autowired
    protected GeoClient geoSearchClient;

    @Autowired
    protected HttpUnifierClient unifierClient;

    @AfterEach
    void tearDownAbstractCreateOrUpdateShopPickupPointTest() {
        verifyNoMoreInteractions(unifierClient, geoSearchClient);
    }

    @Test
    @DisplayName("Нет магазина")
    void noShop() throws Exception {
        mockMvc.perform(requestBuilder(0))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [0]"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    void validateBody(
        ValidationErrorDataBuilder errorDataBuilder,
        UnaryOperator<ShopPickupPointMetaRequest> modifier
    ) throws Exception {
        mockMvc.perform(requestBuilder(modifier.apply(defaultMeta())))
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(errorDataBuilder.forObject("shopPickupPointMetaRequest")));
    }

    @Nonnull
    private static Stream<Arguments> validateBody() {
        return Stream.<Pair<ValidationErrorDataBuilder, UnaryOperator<ShopPickupPointMetaRequest>>>of(
                Pair.of(fieldErrorBuilder("mbiId", NOT_NULL), r -> r.setMbiId(null)),
                Pair.of(fieldErrorBuilder("status", NOT_NULL), r -> r.setStatus(null)),
                Pair.of(fieldErrorBuilder("pickupPoint", NOT_NULL), r -> r.setPickupPoint(null)),
                Pair.of(fieldErrorBuilder("tariff", NOT_NULL), r -> r.setTariff(null)),
                Pair.of(
                    fieldErrorBuilder("tariff.orderBeforeHour", POSITIVE_OR_ZERO),
                    modifier(ShopPickupPointMetaRequest::getTariff, r -> r.setOrderBeforeHour(-1))
                ),
                Pair.of(
                    fieldErrorBuilder("tariff.orderBeforeHour", max(24)),
                    modifier(ShopPickupPointMetaRequest::getTariff, r -> r.setOrderBeforeHour(25))
                ),
                Pair.of(
                    fieldErrorBuilder("tariff.daysFrom", POSITIVE_OR_ZERO),
                    modifier(ShopPickupPointMetaRequest::getTariff, r -> r.setDaysFrom(-1))
                ),
                Pair.of(
                    fieldErrorBuilder("tariff.daysTo", POSITIVE_OR_ZERO),
                    modifier(ShopPickupPointMetaRequest::getTariff, r -> r.setDaysTo(-1))
                ),
                Pair.of(
                    fieldErrorBuilder("pickupPoint.externalId", NOT_BLANK),
                    modifier(ShopPickupPointMetaRequest::getPickupPoint, r -> r.setExternalId(null))
                ),
                Pair.of(
                    fieldErrorBuilder("pickupPoint.externalId", NOT_BLANK),
                    modifier(ShopPickupPointMetaRequest::getPickupPoint, r -> r.setExternalId(""))
                ),
                Pair.of(
                    fieldErrorBuilder("pickupPoint.externalId", NOT_BLANK),
                    modifier(ShopPickupPointMetaRequest::getPickupPoint, r -> r.setExternalId("\t\n\r"))
                ),
                Pair.of(
                    fieldErrorBuilder("pickupPoint.name", NOT_BLANK),
                    modifier(ShopPickupPointMetaRequest::getPickupPoint, r -> r.setName(null))
                ),
                Pair.of(
                    fieldErrorBuilder("pickupPoint.name", NOT_BLANK),
                    modifier(ShopPickupPointMetaRequest::getPickupPoint, r -> r.setName(""))
                ),
                Pair.of(
                    fieldErrorBuilder("pickupPoint.name", NOT_BLANK),
                    modifier(ShopPickupPointMetaRequest::getPickupPoint, r -> r.setName("\t\n\r"))
                ),
                Pair.of(
                    fieldErrorBuilder("pickupPoint.type", NOT_NULL),
                    modifier(ShopPickupPointMetaRequest::getPickupPoint, r -> r.setType(null))
                ),
                Pair.of(
                    fieldErrorBuilder("pickupPoint.schedule[0]", NOT_NULL),
                    modifier(
                        ShopPickupPointMetaRequest::getPickupPoint,
                        r -> r.setSchedule(Collections.singletonList(null))
                    )
                ),
                Pair.of(
                    fieldErrorBuilder("pickupPoint.schedule[0].day", NOT_NULL),
                    modifier(r -> r.getPickupPoint().getSchedule().get(0), r -> r.setDay(null))
                ),
                Pair.of(
                    fieldErrorBuilder("pickupPoint.schedule[0].day", min(1)),
                    modifier(r -> r.getPickupPoint().getSchedule().get(0), r -> r.setDay(0))
                ),
                Pair.of(
                    fieldErrorBuilder("pickupPoint.schedule[0].day", max(7)),
                    modifier(r -> r.getPickupPoint().getSchedule().get(0), r -> r.setDay(8))
                ),
                Pair.of(
                    fieldErrorBuilder("pickupPoint.schedule[0].timeFrom", NOT_NULL),
                    modifier(r -> r.getPickupPoint().getSchedule().get(0), r -> r.setTimeFrom(null))
                ),
                Pair.of(
                    fieldErrorBuilder("pickupPoint.schedule[0].timeTo", NOT_NULL),
                    modifier(r -> r.getPickupPoint().getSchedule().get(0), r -> r.setTimeTo(null))
                ),
                Pair.of(
                    fieldErrorBuilder("pickupPoint.phones[0]", NOT_NULL),
                    modifier(
                        ShopPickupPointMetaRequest::getPickupPoint,
                        r -> r.setPhones(Collections.singletonList(null))
                    )
                ),
                Pair.of(
                    fieldErrorBuilder("pickupPoint.address.locationId", NOT_NULL),
                    modifier(r -> r.getPickupPoint().getAddress(), r -> r.setLocationId(null))
                ),
                Pair.of(
                    fieldErrorBuilder("pickupPoint.address.locationId", POSITIVE),
                    modifier(r -> r.getPickupPoint().getAddress(), r -> r.setLocationId(0))
                )
            )
            .map(pair -> Arguments.of(pair.getLeft(), pair.getRight()));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Валидация адреса: должны быть указаны оба или ни одно из полей: широта и долгота")
    void validateAddressCoordinates(
        String caseName,
        UnaryOperator<ShopPickupPointMetaRequest> modifier
    ) throws Exception {
        mockMvc.perform(requestBuilder(modifier.apply(defaultMeta())))
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(objectError(
                "pickupPoint.address",
                "both or neither latitude and longitude must be specified",
                "ValidShopPickupPointAddress"
            )));
    }

    @Nonnull
    private static Stream<Arguments> validateAddressCoordinates() {
        return Stream.of(
                Pair.of("latitude is null", modifier(r -> r.getPickupPoint().getAddress(), a -> a.setLatitude(null))),
                Pair.of("longitude is null", modifier(r -> r.getPickupPoint().getAddress(), a -> a.setLongitude(null)))
            )
            .map(pair -> Arguments.of(pair.getLeft(), pair.getRight()));
    }

    @Nonnull
    protected abstract MockHttpServletRequestBuilder requestBuilder(long shopId) throws Exception;

    @Nonnull
    private MockHttpServletRequestBuilder requestBuilder() throws Exception {
        return requestBuilder(200);
    }

    @Nonnull
    private MockHttpServletRequestBuilder requestBuilder(ShopPickupPointMetaRequest request) throws Exception {
        return requestBuilder()
            .params(toParams(QUERY_PARAMS))
            .content(objectMapper.writeValueAsString(request));
    }

    @Nonnull
    protected static ShopPickupPointMetaRequest defaultMeta() {
        return new ShopPickupPointMetaRequest()
            .setMbiId(1L)
            .setStatus(ShopPickupPointStatus.ACTIVE)
            .setPickupPoint(
                new ShopPickupPointRequest()
                    .setExternalId("externalId")
                    .setName("name")
                    .setType(PickupPointType.PICKUP_POINT)
                    .setSchedule(List.of(
                        new ShopPickupPointScheduleDayDto().setDay(1)
                            .setTimeFrom(LocalTime.of(10, 0))
                            .setTimeTo(LocalTime.of(18, 0)),
                        new ShopPickupPointScheduleDayDto().setDay(1)
                            .setTimeFrom(LocalTime.of(10, 30))
                            .setTimeTo(LocalTime.of(19, 45))
                    ))
                    .setPhones(List.of(
                        new ShopPickupPointPhoneDto()
                            .setPhoneNumber("+7 999 888 7766")
                    ))
                    .setAddress(
                        new ShopPickupPointAddressDto()
                            .setLocationId(LOCATION_ID)
                            .setLatitude(LATITUDE)
                            .setLongitude(LONGITUDE)
                    )
            )
            .setTariff(new ShopPickupPointTariffRequest());
    }

    @Nonnull
    public static <T> UnaryOperator<ShopPickupPointMetaRequest> modifier(
        Function<ShopPickupPointMetaRequest, T> getter,
        UnaryOperator<T> modifier
    ) {
        return request -> {
            modifier.apply(getter.apply(request));
            return request;
        };
    }

    @Nonnull
    protected GeoObject geoObject(
        int geoId
    ) {
        return SimpleGeoObject.newBuilder()
            .withToponymInfo(ToponymInfo.newBuilder()
                .withGeoid(String.valueOf(geoId))
                .build()
            )
            .withAddressInfo(AddressInfo.newBuilder()
                .withCountryInfo(CountryInfo.newBuilder().build())
                .withAreaInfo(AreaInfo.newBuilder().build())
                .withLocalityInfo(LocalityInfo.newBuilder().build())
                .build()
            )
            .withBoundary(Boundary.newBuilder().build())
            .build();
    }

    @Nonnull
    protected AutoCloseable mockUnifierClient() {
        UnificationRequest request = new UnificationRequest();
        request.setAddress(Optional.of("Москва и Московская область, Москва, Каширское шоссе, 26"));

        SignalOuterClass.Address address = getDefaultAddressBuilder()
            .setCoordinates(
                SignalOuterClass.Coordinates.newBuilder()
                    .setLat(55.653415)
                    .setLon(37.646280)
                    .build()
            ).build();

        UnifierReplyOuterClass.UnifierReply unifierReply = UnifierReplyOuterClass.UnifierReply.newBuilder()
            .addAddress(address)
            .setSuccess(true)
            .build();

        when(unifierClient.unify(any(UnificationRequest.class))).thenReturn(unifierReply);
        return () -> verify(unifierClient).unify(refEq(request));
    }
}
