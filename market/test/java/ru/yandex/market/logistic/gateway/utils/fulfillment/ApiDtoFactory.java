package ru.yandex.market.logistic.gateway.utils.fulfillment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;

import ru.yandex.market.logistic.api.model.fulfillment.Intake;
import ru.yandex.market.logistic.api.model.fulfillment.LegalForm;
import ru.yandex.market.logistic.api.model.fulfillment.Location;
import ru.yandex.market.logistic.api.model.fulfillment.Person;
import ru.yandex.market.logistic.api.model.fulfillment.Phone;
import ru.yandex.market.logistic.api.model.fulfillment.Register;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.ReturnRegister;
import ru.yandex.market.logistic.api.model.fulfillment.Sender;
import ru.yandex.market.logistic.api.model.fulfillment.Taxation;
import ru.yandex.market.logistic.api.model.fulfillment.Warehouse;
import ru.yandex.market.logistic.api.model.fulfillment.WorkTime;
import ru.yandex.market.logistic.api.model.fulfillment.request.entities.ShipmentType;
import ru.yandex.market.logistic.api.model.fulfillment.response.CreateIntakeResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.CreateRegisterResponse;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;
import ru.yandex.market.logistic.api.utils.TimeInterval;

public final class ApiDtoFactory {

    private ApiDtoFactory() {
        throw new AssertionError();
    }

    public static Register createRegister() {
        return new Register.RegisterBuilder(createResourceId("111", "111"),
            Arrays.asList(
                createResourceId("222", "222"),
                createResourceId("333", "333")
            ),
            DateTime.fromLocalDateTime(LocalDateTime.of(2019, 4, 2, 0, 0, 0)),
            createSender())
            .setShipmentId(createResourceId("444", "444"))
            .setShipmentType(ShipmentType.ACCEPTANCE)
            .build();
    }

    public static CreateRegisterResponse createCreateRegisterResponse() {
        return new CreateRegisterResponse(createResourceId());
    }

    public static CreateIntakeResponse createCreateIntakeResponse() {
        return new CreateIntakeResponse(createResourceId("intake-yandex-id-1", "intake-partner-id-1"), null);
    }

    public static Intake createIntake() {
        return new Intake.IntakeBuilder(createResourceId("intake-yandex-id-1", "intake-partner-id-1"),
            createWarehouse(),
            DateTimeInterval.fromFormattedValue("2019-08-15T12:00:00+07:00/2019-08-15T15:00:00+07:00"))
            .setVolume(new BigDecimal("2.71"))
            .setWeight(new BigDecimal("3.14"))
            .build();
    }

    private static Warehouse createWarehouse() {
        ResourceId warehouseId = createResourceId("warehouse-yandex-id-1", "warehouse-partner-id-1");
        return new Warehouse.WarehouseBuilder(
            warehouseId,
            createLocation(),
            IntStream.rangeClosed(1, 7).boxed()
                .map(ApiDtoFactory::createWorkTime)
                .collect(Collectors.toList())
            , "ООО ТЕСТ")
            .setResourceId(warehouseId)
            .setContact(createPerson())
            .setPhones(createPhones())
            .setInstruction("Первый вход")
            .build();
    }

    private static WorkTime createWorkTime(int dayOfWeek) {
        return new WorkTime(dayOfWeek,
            ImmutableList.of(new TimeInterval("10:00:00+07:00/23:00:00+07:00")));
    }

    public static ReturnRegister createReturnRegister() {
        return new ReturnRegister(
            Arrays.asList(
                createResourceId("111", "111"),
                createResourceId("222", "222")
            ),
            createSender());
    }

    private static ResourceId createResourceId() {
        return new ResourceId.ResourceIdBuilder()
            .setYandexId("111")
            .setPartnerId("Zakaz")
            .build();
    }

    private static ResourceId createResourceId(String yandexId, String partnerId) {
        return new ResourceId.ResourceIdBuilder()
            .setYandexId(yandexId)
            .setPartnerId(partnerId)
            .build();
    }

    private static Sender createSender() {
        return new Sender.SenderBuilder(createResourceId(), "ООО «Яндекс Маркет»", "test.ru")
            .setLegalForm(LegalForm.OOO)
            .setPhones(createPhones())
            .setName("БЕРУ")
            .setEmail("test@test.ru")
            .setContact(createPerson())
            .setOgrn("1167746491395")
            .setInn("2342342342356")
            .setAddress(createLocation())
            .setType("ip")
            .setTaxation(Taxation.OSN)
            .build();
    }

    private static List<Phone> createPhones() {
        return Collections.singletonList(new Phone.PhoneBuilder("79099999999").build());
    }

    private static Person createPerson() {
        return new Person.PersonBuilder("Василий").setSurname("Пупкин").build();
    }

    private static Location createLocation() {
        return new Location.LocationBuilder("Russia", "The federal city of Moscow", "Moscow").build();
    }
}
