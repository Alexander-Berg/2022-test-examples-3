package ru.yandex.market.logistic.api.client.delivery;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.delivery.Location;
import ru.yandex.market.logistic.api.model.delivery.TimetableCourier;
import ru.yandex.market.logistic.api.model.delivery.WorkTime;
import ru.yandex.market.logistic.api.model.delivery.response.GetReferenceTimetableCouriersResponse;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.ResponseValidationUtils;
import ru.yandex.market.logistic.api.utils.TimeInterval;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GetReferenceTimetableCouriersTest extends CommonServiceClientTest {
    private static final Location LOCATION =
        new Location.LocationBuilder("Россия", "Новосибирская область", "Новосибирск").build();

    @Autowired
    private DeliveryServiceClient deliveryServiceClient;

    @Test
    void successfulResponse() throws Exception {
        prepareMockServiceNormalized(
            "ds_get_reference_timetable_couriers",
            "ds_get_reference_timetable_couriers/ds_get_reference_timetable_couriers",
            PARTNER_URL
        );

        GetReferenceTimetableCouriersResponse actualResponse = deliveryServiceClient.getReferenceTimetableCouriers(
            Collections.singletonList(LOCATION),
            getPartnerProperties()
        );

        GetReferenceTimetableCouriersResponse expectedResponse =
            new GetReferenceTimetableCouriersResponse.GetReferenceTimetableCouriersResponseBuilder(
                Collections.singletonList(getTimeTableCourier())).build();
        assertEquals(
            expectedResponse,
            actualResponse,
            "Должен вернуть корректный ответ GetReferenceTimetableCouriersResponse"
        );
    }

    @Test
    void errorResponse() throws Exception {
        prepareMockServiceNormalized(
            "ds_get_reference_timetable_couriers",
            "ds_get_reference_timetable_couriers/ds_get_reference_timetable_couriers_error",
            PARTNER_URL
        );

        assertThrows(
            RequestStateErrorException.class,
            () -> deliveryServiceClient.getReferenceTimetableCouriers(
                Collections.singletonList(LOCATION),
                getPartnerProperties()
            ));
    }

    @Test
    void emptyResponse() throws Exception {
        prepareMockServiceNormalized(
            "ds_get_reference_timetable_couriers",
            "ds_get_reference_timetable_couriers/ds_get_reference_timetable_couriers_empty",
            PARTNER_URL
        );

        assertThrows(
            ValidationException.class,
            () -> deliveryServiceClient.getReferenceTimetableCouriers(
                Collections.singletonList(LOCATION),
                getPartnerProperties()
            )
        );
    }

    @Test
    void timetableCourierLocationValidationError() throws Exception {
        prepareMockServiceNormalized(
            "ds_get_reference_timetable_couriers",
            "ds_get_reference_timetable_couriers/ds_get_reference_timetable_couriers_location_validation_error",
            PARTNER_URL
        );

        assertThatThrownBy(() -> deliveryServiceClient.getReferenceTimetableCouriers(
            Collections.singletonList(LOCATION),
            getPartnerProperties()
        )).isInstanceOf(ValidationException.class)
            .hasMessageContaining(ResponseValidationUtils.getNotNullErrorMessage(
                "delivery",
                "GetReferenceTimetableCouriersResponse",
                "timetableCouriers[0].address"
            ));
    }

    @Test
    void timetableCourierLocationContentValidationError() throws Exception {
        prepareMockServiceNormalized(
            "ds_get_reference_timetable_couriers",
            "ds_get_reference_timetable_couriers/ds_get_reference_timetable_couriers_location_content_validation_error",
            PARTNER_URL
        );

        assertThatThrownBy(() -> deliveryServiceClient.getReferenceTimetableCouriers(
            Collections.singletonList(LOCATION),
            getPartnerProperties()
        )).isInstanceOf(ValidationException.class)
            .hasMessageContaining(ResponseValidationUtils.getNotBlankErrorMessage(
                "delivery",
                "GetReferenceTimetableCouriersResponse",
                "timetableCouriers[0].address.country"
            ))
            .hasMessageContaining(ResponseValidationUtils.getNotBlankErrorMessage(
                "delivery",
                "GetReferenceTimetableCouriersResponse",
                "timetableCouriers[0].address.locality"
            ))
            .hasMessageContaining(ResponseValidationUtils.getNotBlankErrorMessage(
                "delivery",
                "GetReferenceTimetableCouriersResponse",
                "timetableCouriers[0].address.region"
            ));
    }

    @Test
    void timetableCourierScheduleValidationError() throws Exception {
        prepareMockServiceNormalized(
            "ds_get_reference_timetable_couriers",
            "ds_get_reference_timetable_couriers/ds_get_reference_timetable_couriers_schedule_validation_error",
            PARTNER_URL
        );

        assertThatThrownBy(() -> deliveryServiceClient.getReferenceTimetableCouriers(
            Collections.singletonList(LOCATION),
            getPartnerProperties()
        )).isInstanceOf(ValidationException.class)
            .hasMessageContaining(ResponseValidationUtils.getNotEmptyErrorMessage(
                "delivery",
                "GetReferenceTimetableCouriersResponse",
                "timetableCouriers[0].schedule"
            ));
    }

    @Test
    void timetableCourierScheduleContentValidationError() throws Exception {
        prepareMockServiceNormalized(
            "ds_get_reference_timetable_couriers",
            "ds_get_reference_timetable_couriers/ds_get_reference_timetable_couriers_schedule_content_validation_error",
            PARTNER_URL
        );

        assertThatThrownBy(() -> deliveryServiceClient.getReferenceTimetableCouriers(
            Collections.singletonList(LOCATION),
            getPartnerProperties()
        )).isInstanceOf(ValidationException.class)
            .hasMessageContaining(ResponseValidationUtils.getNotEmptyErrorMessage(
                "delivery",
                "GetReferenceTimetableCouriersResponse",
                "timetableCouriers[0].schedule[0].periods"
            ))
            .hasMessageContaining(ResponseValidationUtils.getNotNullErrorMessage(
                "delivery",
                "GetReferenceTimetableCouriersResponse",
                "timetableCouriers[0].schedule[0].day"
            ));
    }

    @Test
    void timetableCourierSchedulePeriodsValidationError() throws Exception {
        prepareMockServiceNormalized(
            "ds_get_reference_timetable_couriers",
            "ds_get_reference_timetable_couriers/" +
                "ds_get_reference_timetable_couriers_schedule_periods_invalid_time_interval",
            PARTNER_URL
        );

        assertThatThrownBy(() -> deliveryServiceClient.getReferenceTimetableCouriers(
            Collections.singletonList(LOCATION),
            getPartnerProperties()
        )).isInstanceOf(ValidationException.class)
            .hasMessageContaining(ResponseValidationUtils.getNotNullErrorMessage(
                "delivery",
                "GetReferenceTimetableCouriersResponse",
                "timetableCouriers[0].schedule[0].periods[0].<list element>"
            ));
    }

    @Test
    void timetableCourierScheduleDayValidationError() throws Exception {
        prepareMockServiceNormalized(
            "ds_get_reference_timetable_couriers",
            "ds_get_reference_timetable_couriers/" +
                "ds_get_reference_timetable_couriers_schedule_invalid_day",
            PARTNER_URL
        );

        assertThatThrownBy(() -> deliveryServiceClient.getReferenceTimetableCouriers(
            Collections.singletonList(LOCATION),
            getPartnerProperties()
        )).isInstanceOf(ValidationException.class)
            .hasMessageContaining(ResponseValidationUtils.getMaxErrorMessage(
                "delivery",
                "GetReferenceTimetableCouriersResponse",
                "timetableCouriers[0].schedule[0].day",
                7
            ))
            .hasMessageContaining(ResponseValidationUtils.getMinErrorMessage(
                "delivery",
                "GetReferenceTimetableCouriersResponse",
                "timetableCouriers[0].schedule[1].day",
                1
            ));
    }

    private TimetableCourier getTimeTableCourier() {
        return new TimetableCourier.TimetableCourierBuilder(getLocation(), getSchedule())
            .setHolidays(Collections.singletonList(new DateTime("2020-09-23")))
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

    private Location getLocation() {
        return new Location.LocationBuilder("Россия", "Новосибирская область", "Новосибирск")
            .setStreet("Николаева")
            .setHouse("11")
            .build();
    }
}
