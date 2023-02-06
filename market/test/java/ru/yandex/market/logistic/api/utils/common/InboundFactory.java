package ru.yandex.market.logistic.api.utils.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ru.yandex.market.logistic.api.model.common.Car;
import ru.yandex.market.logistic.api.model.common.Courier;
import ru.yandex.market.logistic.api.model.common.Inbound;
import ru.yandex.market.logistic.api.model.common.InboundStatus;
import ru.yandex.market.logistic.api.model.common.InboundStatusHistory;
import ru.yandex.market.logistic.api.model.common.InboundType;
import ru.yandex.market.logistic.api.model.common.LogisticPoint;
import ru.yandex.market.logistic.api.model.common.Person;
import ru.yandex.market.logistic.api.model.common.Phone;
import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.model.common.Status;
import ru.yandex.market.logistic.api.model.common.StatusCode;
import ru.yandex.market.logistic.api.model.delivery.response.GetInboundStatusHistoryResponse;
import ru.yandex.market.logistic.api.model.delivery.response.GetInboundStatusResponse;
import ru.yandex.market.logistic.api.model.fulfillment.request.entities.restricted.PutInboundRestrictedData;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;

public final class InboundFactory {

    private InboundFactory() {
        throw new UnsupportedOperationException();
    }

    public static Inbound createInbound() {
        ResourceId id = new ResourceId("inbound_yandex_id", "inbound_partner_id");
        DateTimeInterval dateTimeInterval = DateTimeInterval
            .fromFormattedValue("2020-03-20T12:00:00+01:00/2020-04-20T12:00:00+01:00");
        return createInbound(
            id,
            dateTimeInterval,
            InboundType.DEFAULT,
            "EXT_OUTLET_ID",
            "Россия",
            "Московская область",
            "Котельники",
            "Дропшип",
            "+79181234567",
            "А000МР777"
        );
    }

    public static PutInboundRestrictedData createInboundRestrictedData(
        boolean confirmed,
        String supplierName,
        String axaptaMovementRequestId,
        String transportationId
    ) {
        return new PutInboundRestrictedData(confirmed, supplierName, axaptaMovementRequestId, transportationId);
    }

    public static Inbound createInvalidInbound() {
        return createInbound(
            null,
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

    public static Inbound createInboundInvalidInterval() {
        ResourceId id = new ResourceId("inbound_yandex_id", "inbound_partner_id");
        DateTimeInterval dateTimeInterval = DateTimeInterval
            .fromFormattedValue("some invalid interval");
        return createInbound(
            id,
            dateTimeInterval,
            InboundType.DEFAULT,
            "EXT_OUTLET_ID",
            "Россия",
            "Московская область",
            "Котельники",
            "Дропшип",
            "+79181234567",
            "А000МР777"
        );
    }

    @SuppressWarnings("checkstyle:parameterNumber")
    private static Inbound createInbound(
        ResourceId id,
        DateTimeInterval interval,
        InboundType type,
        String partnerLogisticPointId,
        String country,
        String region,
        String locality,
        String personName,
        String phone,
        String car
    ) {
        return new Inbound.InboundBuilder(id, type, interval)
            .setLogisticPoint(createLogisticPoint(partnerLogisticPointId, country, region, locality))
            .setCourier(createCourier(personName, phone, car))
            .setComment("comment about inbound")
            .setOutboundIds(
                Arrays.asList(
                    DtoFactory.createResourceId(123),
                    DtoFactory.createResourceId(456)
                )
            )
            .setShipper(DtoFactory.createParty("111325", "111326"))
            .setExternalRequestId("AX_123")
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

    public static ResourceId createIdWithoutYandexId() {
        return ResourceId.builder()
            .setPartnerId("inboundPartnerId")
            .build();
    }

    public static List<ResourceId> createInboundIds() {
        return Arrays.asList(
            ResourceId.builder()
                .setYandexId("S1")
                .setPartnerId("S2")
                .build(),
            ResourceId.builder()
                .setPartnerId("3")
                .build(),
            ResourceId.builder()
                .setYandexId("S4")
                .setPartnerId("S5")
                .build(),
            ResourceId.builder()
                .setYandexId("S6")
                .setPartnerId("S7")
                .build()
        );
    }

    public static List<ResourceId> createInboundIdsWithoutPartnerId() {
        return Arrays.asList(
            ResourceId.builder()
                .setYandexId("S1")
                .setPartnerId("S2")
                .build(),
            ResourceId.builder()
                .setPartnerId("3")
                .build(),
            ResourceId.builder()
                .setYandexId("S4")
                .build()
        );
    }

    public static GetInboundStatusResponse createGetInboundStatusResponseDS() {
        return GetInboundStatusResponse
            .builder(
                Arrays.asList(
                    InboundStatus
                        .builder(
                            resourceId("S1", "S2"),
                            status(StatusCode.CREATED, new DateTime("2020-10-10T09:00:00"), "optional message")
                        )
                        .build(),
                    InboundStatus
                        .builder(
                            resourceId(null, "3"),
                            status(StatusCode.ARRIVED, new DateTime("2020-10-10T10:00:00"), null)
                        )
                        .build(),
                    InboundStatus
                        .builder(
                            resourceId("S4", "S5"),
                            status(StatusCode.ACCEPTANCE, new DateTime("2020-10-10T11:00:00"), null)
                        )
                        .build()
                )
            )
            .build();
    }

    public static GetInboundStatusHistoryResponse createInboundStatusHistoryResponseDS() {
        return GetInboundStatusHistoryResponse
            .builder(
                Arrays.asList(
                    InboundStatusHistory
                        .builder(
                            resourceId("S1", "S2"),
                            Arrays.asList(
                                status(StatusCode.CREATED, new DateTime("2020-10-10T09:00:00"), "optional message"),
                                status(StatusCode.ARRIVED, new DateTime("2020-10-10T09:10:00"), null),
                                status(StatusCode.ACCEPTANCE, new DateTime("2020-10-10T09:20:00"), null),
                                status(StatusCode.ACCEPTED, new DateTime("2020-10-10T09:30:00"), null)
                            )
                        )
                        .build(),
                    InboundStatusHistory
                        .builder(
                            resourceId(null, "3"),
                            Arrays.asList(
                                status(StatusCode.CREATED, new DateTime("2020-10-10T10:00:00"), null),
                                status(StatusCode.CANCELLED, new DateTime("2020-10-10T10:01:00"), null)
                            )
                        )
                        .build(),
                    InboundStatusHistory
                        .builder(
                            resourceId("S4", "S5"),
                            Collections.emptyList()
                        )
                        .build(),
                    InboundStatusHistory
                        .builder(
                            resourceId("S6", "S7"),
                            Arrays.asList(
                                status(StatusCode.CREATED, new DateTime("2020-10-10T10:00:00"), null),
                                status(StatusCode.CANCELLED_BY_PARTNER, new DateTime("2020-10-10T10:01:00"), null)
                            )
                        )
                        .build()
                )
            )
            .build();
    }

    public static ru.yandex.market.logistic.api.model.fulfillment.response.GetInboundStatusResponse
    createGetInboundStatusResponseFF() {
        return ru.yandex.market.logistic.api.model.fulfillment.response.GetInboundStatusResponse
            .builder(
                Arrays.asList(
                    InboundStatus
                        .builder(
                            resourceId("S1", "S2"),
                            status(StatusCode.CREATED, new DateTime("2020-10-10T09:00:00"), "optional message")
                        )
                        .build(),
                    InboundStatus
                        .builder(
                            resourceId(null, "3"),
                            status(StatusCode.ARRIVED, new DateTime("2020-10-10T10:00:00"), null)
                        )
                        .build(),
                    InboundStatus
                        .builder(
                            resourceId("S4", "S5"),
                            status(StatusCode.ACCEPTANCE, new DateTime("2020-10-10T11:00:00"), null)
                        )
                        .build()
                )
            )
            .build();
    }

    public static ru.yandex.market.logistic.api.model.fulfillment.response.GetInboundStatusHistoryResponse
    createInboundStatusHistoryResponseFF() {
        return ru.yandex.market.logistic.api.model.fulfillment.response.GetInboundStatusHistoryResponse
            .builder(
                Arrays.asList(
                    InboundStatusHistory
                        .builder(
                            resourceId("S1", "S2"),
                            Arrays.asList(
                                status(StatusCode.CREATED, new DateTime("2020-10-10T09:00:00"), "optional message"),
                                status(StatusCode.ARRIVED, new DateTime("2020-10-10T09:10:00"), null),
                                status(StatusCode.ACCEPTANCE, new DateTime("2020-10-10T09:20:00"), null),
                                status(StatusCode.ACCEPTED, new DateTime("2020-10-10T09:30:00"), null)
                            )
                        )
                        .build(),
                    InboundStatusHistory
                        .builder(
                            resourceId(null, "3"),
                            Arrays.asList(
                                status(StatusCode.CREATED, new DateTime("2020-10-10T10:00:00"), null),
                                status(StatusCode.CANCELLED, new DateTime("2020-10-10T10:01:00"), null)
                            )
                        )
                        .build(),
                    InboundStatusHistory
                        .builder(
                            resourceId("S4", "S5"),
                            Collections.emptyList()
                        )
                        .build(),
                    InboundStatusHistory
                        .builder(
                            resourceId("S6", "S7"),
                            Arrays.asList(
                                status(StatusCode.CREATED, new DateTime("2020-10-10T10:00:00"), null),
                                status(StatusCode.CANCELLED_BY_PARTNER, new DateTime("2020-10-10T10:01:00"), null)
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
