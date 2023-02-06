package ru.yandex.market.logistic.api.client.delivery;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.common.LocationFilter;
import ru.yandex.market.logistic.api.model.delivery.DateBool;
import ru.yandex.market.logistic.api.model.delivery.Location;
import ru.yandex.market.logistic.api.model.delivery.Phone;
import ru.yandex.market.logistic.api.model.delivery.PickupPoint;
import ru.yandex.market.logistic.api.model.delivery.PickupPointType;
import ru.yandex.market.logistic.api.model.delivery.WorkTime;
import ru.yandex.market.logistic.api.model.delivery.response.GetReferencePickupPointsResponse;
import ru.yandex.market.logistic.api.model.validation.LogisticApiResponseFilter;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;
import ru.yandex.market.logistic.api.utils.TimeInterval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.logistic.api.utils.ValidationUtil.validate;

class GetReferencePickupPointsTest extends CommonServiceClientTest {

    private static final LocationFilter LOCATION_FILTER = LocationFilter.builder().setCountry("Россия").build();

    private static final String CODE = "test-code";

    private static final DateTimeInterval CALENDAR_INTERVAL =
        DateTimeInterval.fromFormattedValue("2019-02-07/2019-02-14");

    @Autowired
    private DeliveryServiceClient deliveryServiceClient;

    @Test
    void testSuccessfulResponse() throws Exception {
        prepareMockServiceNormalized("ds_get_reference_pickup_points", PARTNER_URL);

        GetReferencePickupPointsResponse actualResponse = deliveryServiceClient.getReferencePickupPoints(
            Collections.singletonList(LOCATION_FILTER),
            Collections.singletonList(CODE),
            CALENDAR_INTERVAL,
            createFilter(),
            getPartnerProperties()
        );

        GetReferencePickupPointsResponse expectedResponse =
            new GetReferencePickupPointsResponse.GetReferencePickupPointsResponseBuilder(
                Arrays.asList(getPickupPoint1(), getPickupPoint2())).build();
        assertEquals(
            expectedResponse,
            actualResponse,
            "Должен вернуть корректный ответ GetReferencePickupPointsResponse"
        );
    }

    @Test
    void testErrorResponse() throws Exception {
        prepareMockServiceNormalized(
            "ds_get_reference_pickup_points",
            "ds_get_reference_pickup_points_error",
            PARTNER_URL
        );

        assertThrows(
            RequestStateErrorException.class,
            () -> deliveryServiceClient.getReferencePickupPoints(
                Collections.singletonList(LOCATION_FILTER),
                Collections.singletonList(CODE),
                CALENDAR_INTERVAL,
                createFilter(),
                getPartnerProperties()
            ));
    }

    @Test
    void testResponseWithBadPoints() throws Exception {
        prepareMockServiceNormalized(
            "ds_get_reference_pickup_points",
            "ds_get_reference_pickup_points_with_bad_point",
            PARTNER_URL
        );

        GetReferencePickupPointsResponse actualResponse = deliveryServiceClient.getReferencePickupPoints(
            Collections.singletonList(LOCATION_FILTER),
            Collections.singletonList(CODE),
            CALENDAR_INTERVAL,
            createFilter(),
            getPartnerProperties()
        );

        GetReferencePickupPointsResponse expectedResponse =
            new GetReferencePickupPointsResponse.GetReferencePickupPointsResponseBuilder(
                Collections.singletonList(getPickupPoint1())).build();
        assertEquals(
            expectedResponse,
            actualResponse,
            "Должен вернуть ответ GetReferencePickupPointsResponse c одной из двух полученных точек"
        );
    }

    private PickupPoint getPickupPoint2() {
        return new PickupPoint.PickupPointBuilder("test-code2", getLocation(), getPhones(), false)
            .setType(PickupPointType.UNKNOWN)
            .setSchedule(getSchedule())
            .setCashAllowed(false)
            .setPrepayAllowed(false)
            .setMaxLength(new BigDecimal("40"))
            .setMaxWidth(new BigDecimal("10.00"))
            .setWorkDays(Collections.singletonList(new DateBool(new DateTime("2021-11-07"), true)))
            .setDayOffs(Collections.singletonList(new DateTime("2021-11-01")))
            .build();
    }

    private PickupPoint getPickupPoint1() {
        return new PickupPoint.PickupPointBuilder("test-code1", getLocation(), getPhones(), true)
            .setType(PickupPointType.TERMINAL)
            .setSchedule(getSchedule())
            .setCashAllowed(true)
            .setPrepayAllowed(true)
            .setAvailableForC2C(true)
            .build();
    }

    private List<WorkTime> getSchedule() {
        return Collections.singletonList(
            new WorkTime.WorkTimeBuilder(
                1,
                Collections.singletonList(new TimeInterval("03:00:00+03:00/02:59:00+03:00"))
            ).build()
        );
    }

    private List<Phone> getPhones() {
        return Collections.singletonList(new Phone.PhoneBuilder("+79999999999").build());
    }

    private Location getLocation() {
        return new Location.LocationBuilder("Россия", "Новосибирская область", "Новосибирск")
            .setStreet("Николаева")
            .setHouse("11")
            .build();
    }

    @Nonnull
    private LogisticApiResponseFilter<GetReferencePickupPointsResponse> createFilter() {
        return instance -> {
            List<PickupPoint> pickupPoints = instance.getPickupPoints();
            return new GetReferencePickupPointsResponse.GetReferencePickupPointsResponseBuilder(
                pickupPoints.stream().filter(point -> validate(point).isEmpty()).collect(Collectors.toList()))
                .build();
        };
    }
}
