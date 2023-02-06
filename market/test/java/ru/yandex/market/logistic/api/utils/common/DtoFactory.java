package ru.yandex.market.logistic.api.utils.common;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;

import ru.yandex.market.logistic.api.model.common.Address;
import ru.yandex.market.logistic.api.model.common.Car;
import ru.yandex.market.logistic.api.model.common.Courier;
import ru.yandex.market.logistic.api.model.common.LegalEntity;
import ru.yandex.market.logistic.api.model.common.LegalForm;
import ru.yandex.market.logistic.api.model.common.Location;
import ru.yandex.market.logistic.api.model.common.LogisticPoint;
import ru.yandex.market.logistic.api.model.common.OrderTransferCode;
import ru.yandex.market.logistic.api.model.common.OrderTransferCodes;
import ru.yandex.market.logistic.api.model.common.Party;
import ru.yandex.market.logistic.api.model.common.Person;
import ru.yandex.market.logistic.api.model.common.Phone;
import ru.yandex.market.logistic.api.model.common.RegistryBox;
import ru.yandex.market.logistic.api.model.common.RegistryPallet;
import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.model.common.UnitInfo;

public final class DtoFactory {

    private DtoFactory() {
        throw new UnsupportedOperationException();
    }

    public static LegalEntity createLegalEntity() {
        Location legalEntityLocation = new Location.LocationBuilder("Россия", "Уфа", "Уфа")
            .setStreet("Складская")
            .setHouse("9")
            .setBuilding("3")
            .setHousing("0")
            .setRoom("15")
            .setZipCode("159009")
            .setPorch("0")
            .setFloor(7)
            .setMetro("Бутово")
            .setLat(BigDecimal.valueOf(55933957, 6))
            .setLng(BigDecimal.valueOf(38188274, 6))
            .setLocationId(5204L)
            .build();

        Address legalEntityAddress = new Address.AddressBuilder("combinedAddress")
            .setStructuredAddress(legalEntityLocation)
            .build();

        return new LegalEntity.LegalEntityBuilder()
            .setName("name")
            .setLegalName("legalName")
            .setLegalForm(LegalForm.OOO)
            .setOgrn("1223455")
            .setInn("5543221")
            .setKpp("123")
            .setAddress(legalEntityAddress)
            .setBank("bank")
            .setAccount("1234")
            .setBik("123456")
            .setCorrespondentAccount("1234567")
            .build();
    }

    public static Location createLocation(String country, String region, String locality) {
        return new Location.LocationBuilder(country, region, locality)
            .setFederalDistrict("Центральный федеральный округ")
            .setSubRegion("Яничкин проезд")
            .setStreet("Яничкин проезд")
            .setHouse("7")
            .setLocationId(213L)
            .setBuilding("1")
            .setFloor(2)
            .setHousing("3")
            .setIntercom("4")
            .setLat(BigDecimal.valueOf(12.345))
            .setLng(BigDecimal.valueOf(23.456))
            .setMetro("Смоленская")
            .setPorch("5")
            .setRoom("6")
            .setSettlement("н.п. Котельники")
            .setZipCode("012345")
            .build();
    }

    public static OrderTransferCodes createOrderTransferCodes() {
        return new OrderTransferCodes.OrderTransferCodesBuilder()
            .setInbound(new OrderTransferCode.OrderTransferCodeBuilder()
                .setVerification("123456")
                .setElectronicAcceptanceCertificate("eac123")
                .build()
            )
            .setOutbound(new OrderTransferCode.OrderTransferCodeBuilder()
                .setVerification("654321")
                .setElectronicAcceptanceCertificate("eac321")
                .build()
            )
            .build();
    }

    private static Car createCar() {
        Car.CarBuilder carBuilder = new Car.CarBuilder("А019МР199");
        carBuilder.setDescription("Моя красная тачка");
        return carBuilder.build();
    }

    public static Courier createCourier(String surname) {
        return new Courier.CourierBuilder()
            .setPersons(Collections.singletonList(new Person("Иван", surname, "Иванович")))
            .setCar(createCar())
            .setPhone(new Phone.PhoneBuilder("++74951234567").build())
            .setUrl("http://dostavka.yandex.ru/delivery/courier")
            .build();
    }

    public static UnitInfo createUnitInfo(String description) {
        return new UnitInfo(null, null, null, null, description);
    }

    public static ResourceId createResourceId(int id) {
        return new ResourceId("yId" + id, "pId" + id);
    }

    public static List<RegistryPallet> createSingleRegistryPallet(String description) {
        return Collections.singletonList(new RegistryPallet(createUnitInfo(description)));
    }

    public static List<RegistryBox> createSingleRegistryBox(String description) {
        return Collections.singletonList(new RegistryBox(createUnitInfo(description)));
    }

    public static Party createParty(String yandexId, String partnerId) {

        LogisticPoint logisticPoint = LogisticPoint.builder(new ResourceId(yandexId, partnerId))
            .setName("Точка в Новосибирске")
            .setLocation(Location.builder("Россия", "Новосибирская область", "Новосибирск").build())
            .setContact(
                Person.builder("Новосибирск")
                    .setPatronymic("Новосибирскович")
                    .setSurname("Новосибирский")
                    .build()
            )
            .setPhones(ImmutableList.of(
                Phone.builder("88005553535").build(),
                Phone.builder("88005553535").setAdditional("123").build()
            ))
            .build();

        return Party.builder(logisticPoint)
            .setLegalEntity(createLegalEntity())
            .build();
    }
}
