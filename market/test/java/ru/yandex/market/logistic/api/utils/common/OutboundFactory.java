package ru.yandex.market.logistic.api.utils.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ru.yandex.market.logistic.api.model.common.Car;
import ru.yandex.market.logistic.api.model.common.Courier;
import ru.yandex.market.logistic.api.model.common.LogisticPoint;
import ru.yandex.market.logistic.api.model.common.Outbound;
import ru.yandex.market.logistic.api.model.common.OutboundStatus;
import ru.yandex.market.logistic.api.model.common.OutboundStatusHistory;
import ru.yandex.market.logistic.api.model.common.Person;
import ru.yandex.market.logistic.api.model.common.Phone;
import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.model.common.Status;
import ru.yandex.market.logistic.api.model.common.StatusCode;
import ru.yandex.market.logistic.api.model.delivery.response.GetOutboundStatusHistoryResponse;
import ru.yandex.market.logistic.api.model.delivery.response.GetOutboundStatusResponse;
import ru.yandex.market.logistic.api.model.fulfillment.request.entities.restricted.PutOutboundRestrictedData;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;

public final class OutboundFactory {

    private OutboundFactory() {
        throw new UnsupportedOperationException();
    }

    public static Outbound createOutbound() {
        ResourceId id = new ResourceId("outbound_yandex_id", "outbound_partner_id");
        DateTimeInterval dateTimeInterval = DateTimeInterval
            .fromFormattedValue("2020-03-20T12:00:00+01:00/2020-04-20T12:00:00+01:00");
        return createOutbound(
            id,
            dateTimeInterval,
            "EXT_OUTLET_ID",
            "Россия",
            "Московская область",
            "Котельники",
            "Дропшип",
            "+79181234567",
            "А000МР777"
        );
    }

    public static Outbound createInvalidOutbound() {
        return createOutbound(
            null,
            null,
            null,
            "",
            "",
            "",
            "",
            "",
            ""
        );
    }

    public static Outbound createOutboundInvalidInterval() {
        ResourceId id = new ResourceId("outbound_yandex_id", "outbound_partner_id");
        DateTimeInterval dateTimeInterval = DateTimeInterval
            .fromFormattedValue("some invalid interval");
        return createOutbound(
            id,
            dateTimeInterval,
            "EXT_OUTLET_ID",
            "Россия",
            "Московская область",
            "Котельники",
            "Дропшип",
            "+79181234567",
            "А000МР777"
        );
    }

    public static PutOutboundRestrictedData createRestrictedData(String transportationId) {
        return new PutOutboundRestrictedData(transportationId);
    }

    @SuppressWarnings("checkstyle:parameterNumber")
    private static Outbound createOutbound(
        ResourceId id,
        DateTimeInterval interval,
        String partnerLogisticPointId,
        String country,
        String region,
        String locality,
        String personName,
        String phone,
        String car
    ) {
        return new Outbound.OutboundBuilder(id, interval)
            .setLogisticPoint(createLogisticPoint(partnerLogisticPointId, country, region, locality))
            .setCourier(createCourier(personName, phone, car))
            .setComment("comment about outbound")
            .build();
    }

    private static LogisticPoint createLogisticPoint(String partnerId, String country, String region, String locality) {
        return new LogisticPoint.LogisticPointBuilder(new ResourceId("10000012345", partnerId))
            .setLocation(DtoFactory.createLocation(country, region, locality))
            .build();
    }

    private static Courier createCourier(String personName, String phone, String car) {
        return new Courier.CourierBuilder()
            .setPartnerId(new ResourceId("106", "107"))
            .setPersons(Collections.singletonList((createPerson(personName))))
            .setPhone(createPhone(phone))
            .setCar(createCar(car))
            .setLegalEntity(DtoFactory.createLegalEntity())
            .build();
    }

    private static Person createPerson(String name) {
        return new Person.PersonBuilder(name)
            .setSurname("Кроссдоков")
            .setPatronymic("Фулфиллментович")
            .build();
    }

    private static Phone createPhone(String number) {
        return new Phone.PhoneBuilder(number)
            .setAdditional("123")
            .build();
    }

    private static Car createCar(String number) {
        return new Car.CarBuilder(number)
            .setDescription("Вишневая девятка")
            .build();
    }

    public static List<ResourceId> createOutboundIds() {
        return Arrays.asList(
            ResourceId.builder()
                .setYandexId("S1")
                .setPartnerId("S2")
                .build(),
            ResourceId.builder()
                .setPartnerId("S3")
                .build(),
            ResourceId.builder()
                .setYandexId("S4")
                .setPartnerId("S5")
                .build(),
            ResourceId.builder()
                .setYandexId("S6")
                .setPartnerId("S7")
                .build(),
            ResourceId.builder()
                .setYandexId("S8")
                .setPartnerId("S9")
                .build()
        );
    }

    public static ResourceId createIdWithoutYandexId() {
        return ResourceId.builder()
            .setPartnerId("S1WithoutYId")
            .build();
    }

    public static List<ResourceId> createOutboundIdsWithoutPartnerId() {
        return Arrays.asList(
            ResourceId.builder()
                .setYandexId("S1")
                .setPartnerId("S2")
                .build(),
            ResourceId.builder()
                .setPartnerId("S3")
                .build(),
            ResourceId.builder()
                .setYandexId("S4")
                .build(),
            ResourceId.builder()
                .setYandexId("S6")
                .setPartnerId("S7")
                .build()
        );
    }

    public static GetOutboundStatusResponse createGetOutboundStatusResponseDS() {
        return GetOutboundStatusResponse
            .builder(
                Arrays.asList(
                    OutboundStatus
                        .builder(
                            resourceId("S1", "S2"),
                            status(StatusCode.CREATED, new DateTime("2020-10-10T09:00:00"), "optional message")
                        )
                        .build(),
                    OutboundStatus
                        .builder(
                            resourceId(null, "S3"),
                            status(StatusCode.ASSEMBLING, new DateTime("2020-10-10T10:00:00"), null)
                        )
                        .build(),
                    OutboundStatus
                        .builder(
                            resourceId("S4", "S5"),
                            status(StatusCode.ASSEMBLED, new DateTime("2020-10-10T11:00:00"), null)
                        )
                        .build(),
                    OutboundStatus
                        .builder(
                            resourceId("S6", "S7"),
                            status(StatusCode.TRANSFERRED, new DateTime("2020-10-10T12:00:00"), null)
                        )
                        .build(),
                    OutboundStatus
                        .builder(
                            resourceId("S8", "S9"),
                            status(StatusCode.CANCELLED_BY_PARTNER, new DateTime("2020-10-10T12:12:12"), null)
                        )
                        .build()
                )
            )
            .build();
    }

    public static GetOutboundStatusHistoryResponse createOutboundStatusHistoryResponseDS() {
        return GetOutboundStatusHistoryResponse
            .builder(
                Arrays.asList(
                    OutboundStatusHistory
                        .builder(
                            resourceId("S1", "S2"),
                            Arrays.asList(
                                status(StatusCode.CREATED, new DateTime("2020-10-10T09:00:00"), "optional message"),
                                status(StatusCode.ASSEMBLING, new DateTime("2020-10-10T09:10:00"), null),
                                status(StatusCode.ASSEMBLED, new DateTime("2020-10-10T09:20:00"), null),
                                status(StatusCode.TRANSFERRED, new DateTime("2020-10-10T09:30:00"), null)
                            )
                        )
                        .build(),
                    OutboundStatusHistory
                        .builder(
                            resourceId(null, "S3"),
                            Arrays.asList(
                                status(StatusCode.CREATED, new DateTime("2020-10-10T10:00:00"), null),
                                status(StatusCode.CANCELLED, new DateTime("2020-10-10T10:01:00"), null)
                            )
                        )
                        .build(),
                    OutboundStatusHistory
                        .builder(
                            resourceId("S4", "S5"),
                            Collections.emptyList()
                        )
                        .build(),
                    OutboundStatusHistory
                        .builder(
                            resourceId("S6", "S7"),
                            Arrays.asList(
                                status(StatusCode.TRANSFERRED, new DateTime("2020-10-10T12:00:00"), null)
                            )
                        )
                        .build()
                )
            )
            .build();
    }


    public static ru.yandex.market.logistic.api.model.fulfillment.response.GetOutboundStatusResponse
    createGetOutboundStatusResponseFF() {
        return ru.yandex.market.logistic.api.model.fulfillment.response.GetOutboundStatusResponse
            .builder(
                Arrays.asList(
                    OutboundStatus
                        .builder(
                            resourceId("S1", "S2"),
                            status(StatusCode.CREATED, new DateTime("2020-10-10T09:00:00"), "optional message")
                        )
                        .build(),
                    OutboundStatus
                        .builder(
                            resourceId(null, "S3"),
                            status(StatusCode.ASSEMBLING, new DateTime("2020-10-10T10:00:00"), null)
                        )
                        .build(),
                    OutboundStatus
                        .builder(
                            resourceId("S4", "S5"),
                            status(StatusCode.ASSEMBLED, new DateTime("2020-10-10T11:00:00"), null)
                        )
                        .build(),
                    OutboundStatus
                        .builder(
                            resourceId("S6", "S7"),
                            status(StatusCode.TRANSFERRED, new DateTime("2020-10-10T12:00:00"), null)
                        )
                        .build(),
                    OutboundStatus
                        .builder(
                            resourceId("S8", "S9"),
                            status(StatusCode.CANCELLED_BY_PARTNER, new DateTime("2020-10-10T12:12:12"), null)
                        )
                        .build()
                )
            )
            .build();
    }


    public static ru.yandex.market.logistic.api.model.fulfillment.response.GetOutboundStatusHistoryResponse
    createOutboundStatusHistoryResponseFF() {
        return ru.yandex.market.logistic.api.model.fulfillment.response.GetOutboundStatusHistoryResponse
            .builder(
                Arrays.asList(
                    OutboundStatusHistory
                        .builder(
                            resourceId("S1", "S2"),
                            Arrays.asList(
                                status(StatusCode.CREATED, new DateTime("2020-10-10T09:00:00"), "optional message"),
                                status(StatusCode.ASSEMBLING, new DateTime("2020-10-10T09:10:00"), null),
                                status(StatusCode.ASSEMBLED, new DateTime("2020-10-10T09:20:00"), null),
                                status(StatusCode.TRANSFERRED, new DateTime("2020-10-10T09:30:00"), null)
                            )
                        )
                        .build(),
                    OutboundStatusHistory
                        .builder(
                            resourceId(null, "S3"),
                            Arrays.asList(
                                status(StatusCode.CREATED, new DateTime("2020-10-10T10:00:00"), null),
                                status(StatusCode.CANCELLED, new DateTime("2020-10-10T10:01:00"), null)
                            )
                        )
                        .build(),
                    OutboundStatusHistory
                        .builder(
                            resourceId("S4", "S5"),
                            Collections.emptyList()
                        )
                        .build(),
                    OutboundStatusHistory
                        .builder(
                            resourceId("S6", "S7"),
                            Arrays.asList(
                                status(StatusCode.TRANSFERRED, new DateTime("2020-10-10T12:00:00"), null)
                            )
                        )
                        .build()
                )
            )
            .build();
    }

    private static ResourceId resourceId(String yandexId, String partnerId) {
        return ResourceId
            .builder()
            .setYandexId(yandexId)
            .setPartnerId(partnerId)
            .build();
    }

    private static Status status(StatusCode code, DateTime time, String msg) {
        return Status
            .builder(code, time)
            .setMessage(msg)
            .build();
    }

}
