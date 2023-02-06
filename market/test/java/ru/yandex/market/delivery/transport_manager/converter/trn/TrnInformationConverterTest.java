package ru.yandex.market.delivery.transport_manager.converter.trn;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.converter.prefix.IdPrefixConverter;
import ru.yandex.market.delivery.transport_manager.domain.entity.Address;
import ru.yandex.market.delivery.transport_manager.domain.entity.Korobyte;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.Phone;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationLegalInfo;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationMetadata;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.entity.courier.MovementCourier;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Register;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.UnitMeta;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitType;
import ru.yandex.market.delivery.transport_manager.model.enums.OwnershipType;
import ru.yandex.market.delivery.transport_manager.service.trn.dto.TrnCar;
import ru.yandex.market.delivery.transport_manager.service.trn.dto.TrnCourier;
import ru.yandex.market.delivery.transport_manager.service.trn.dto.TrnInformation;
import ru.yandex.market.delivery.transport_manager.service.trn.dto.TrnLegalInfo;
import ru.yandex.market.delivery.transport_manager.service.trn.dto.TrnRegisterCharacteristics;
import ru.yandex.market.delivery.transport_manager.service.trn.dto.TrnTemplateConvertingData;
import ru.yandex.market.delivery.transport_manager.service.trn.dto.TrnTransportationUnit;

public class TrnInformationConverterTest {
    private final TrnInformationConverter trnInformationConverter = new TrnInformationConverter(
        new IdPrefixConverter(),
        Clock.systemDefaultZone()
    );
    private static final String EIGHT_INDENTS = " ".repeat(8);

    @Test
    void testConvertation() {
        var transportation = createTransportation();
        var register = createRegister();
        var movementCourier = createMovementCourier();
        var outboundInfo = createOutboundUnitInfo();
        var inboundInfo = createInboundUnitInfo();
        var movementInfo = createMovementInfo();
        var phoneMap = createPhoneMap();
        var metadata = createTransportationMetadata();

        var info = TrnTemplateConvertingData.builder()
                .inboundInfo(inboundInfo)
                .outboundInfo(outboundInfo)
                .movementInfo(movementInfo)
                .transportationMetadata(metadata)
                .logisticsPointMetadataMap(phoneMap)
                .transportation(transportation)
                .register(register)
                .movementCourier(movementCourier)
                .build();

        TrnInformation result = trnInformationConverter.convert(info);

        checkLegalInfo(result.getLegalInfoSender(), outboundInfo, "+7 999 888 77 66");
        checkLegalInfo(result.getLegalInfoReceiver(), inboundInfo, "+7 800 400 22 00");
        checkLegalInfo(result.getTransporter(), movementInfo, movementCourier.getPhone());
        checkCar(result.getCar(), movementCourier);
        checkTransportationUnit(
            result.getInbound(),
            inboundInfo,
            transportation.getInboundUnit().getPlannedIntervalStart()
        );
        checkTransportationUnit(
            result.getOutbound(),
            outboundInfo,
            transportation.getOutboundUnit().getPlannedIntervalStart()
        );
        checkRegister(result.getRegister());
        checkCourier(result.getCourier(), movementCourier);

        Assertions.assertEquals("нет", result.getIsExpeditor());
    }

    @Test
    void assertIsExpeditorNullAndPriceIsNull() {
        var transportation = createTransportation();
        var register = createEmptyRegister();
        var movementCourier = createMovementCourier();
        var outboundInfo = createOutboundUnitInfo();
        var inboundInfo = createInboundUnitInfo();
        var movementInfo = createMovementInfo();
        var phoneMap = createPhoneMap();
        var metadata = createTransportationMetadata();

        movementInfo.setLegalAddress("Fake");

        var info = TrnTemplateConvertingData.builder()
                .inboundInfo(inboundInfo)
                .outboundInfo(outboundInfo)
                .movementInfo(movementInfo)
                .transportationMetadata(metadata)
                .logisticsPointMetadataMap(phoneMap)
                .transportation(transportation)
                .register(register)
                .movementCourier(movementCourier)
                .build();

        TrnInformation result = trnInformationConverter.convert(info);

        Assertions.assertEquals("нет", result.getIsExpeditor());
        Assertions.assertNull(result.getRegister().getPriceRub());
    }

    @Test
    void testMetrics() {
        var register = createRegister();

        Double height = trnInformationConverter.getUnitsSumMetric(register, RegisterUnit::getHeightCm);
        Double length = trnInformationConverter.getUnitsSumMetric(register, RegisterUnit::getLengthCm);
        Double width = trnInformationConverter.getUnitsSumMetric(register, RegisterUnit::getWidthCm);

        Assertions.assertEquals(0.01, height);
        Assertions.assertEquals(0.01, length);
        Assertions.assertEquals(0.01, width);
    }

    @Test
    void testNullMetrics() {
        var register = createRegisterWithEmptyPalletsKorobyte();

        Double height = trnInformationConverter.getUnitsSumMetric(register, RegisterUnit::getHeightCm);

        Assertions.assertEquals(0.0, height);
    }

    @Test
    void testMetricsBulkyCargo() {
        var register = createRegisterBulkyCargo();

        Double height = trnInformationConverter.getUnitsSumMetric(register, RegisterUnit::getHeightCm);
        Double length = trnInformationConverter.getUnitsSumMetric(register, RegisterUnit::getLengthCm);
        Double width = trnInformationConverter.getUnitsSumMetric(register, RegisterUnit::getWidthCm);

        Assertions.assertEquals(30.0, height);
        Assertions.assertEquals(40.0, length);
        Assertions.assertEquals(5.0, width);
    }

    @Test
    void testZeroPalletAndOneBox() {
        var transportation = createTransportation();
        var register = createRegisterWithBoxes();
        var movementCourier = createMovementCourier();
        var outboundInfo = createOutboundUnitInfo();
        var inboundInfo = createInboundUnitInfo();
        var movementInfo = createMovementInfo();
        var phoneMap = createPhoneMap();
        var metadata = createTransportationMetadata();

        var info = TrnTemplateConvertingData.builder()
            .inboundInfo(inboundInfo)
            .outboundInfo(outboundInfo)
            .movementInfo(movementInfo)
            .transportationMetadata(metadata)
            .logisticsPointMetadataMap(phoneMap)
            .transportation(transportation)
            .register(register)
            .movementCourier(movementCourier)
            .build();

        var car = trnInformationConverter.convert(info).getCar();

        Assertions.assertEquals(car.getCargoQuantity(), 1);
        Assertions.assertEquals(car.getCargoType(), "коробов");
    }

    private Transportation createTransportation() {
        var stubDateStart = LocalDateTime.of(2000, 12, 12, 1, 1);
        var stubDateEnd = LocalDateTime.of(2000, 12, 12, 2, 1);

        Movement movement = new Movement();
        movement.setPrice(1000L);

        TransportationUnit outbound = new TransportationUnit();
        outbound.setType(TransportationUnitType.OUTBOUND);
        outbound.setPlannedIntervalStart(stubDateStart);
        outbound.setPlannedIntervalEnd(stubDateEnd);
        outbound.setId(1L);

        TransportationUnit inbound = new TransportationUnit();
        inbound.setType(TransportationUnitType.INBOUND);
        inbound.setPlannedIntervalStart(stubDateStart);
        inbound.setPlannedIntervalEnd(stubDateEnd);
        inbound.setId(2L);

        Transportation transportation = new Transportation();
        transportation.setOutboundUnit(outbound);
        transportation.setInboundUnit(inbound);
        transportation.setMovement(movement);

        return transportation;
    }

    private Register createRegister() {
        var items = List.of(createItem(), createItem());

        Register register = new Register();
        register.setItems(items);
        register.setPallets(List.of(createPallet()));
        register.setShipmentManagerName("Клим Саныч");
        return register;
    }

    private Register createRegisterWithBoxes() {
        return new Register().setBoxes(List.of(createBox()));
    }

    private Register createRegisterWithEmptyPalletsKorobyte() {
        var items = List.of(createItem(), createItem());

        Register register = new Register();
        register.setItems(items);
        register.setPallets(List.of(createPalletWithNoKorobyte()));
        return register;
    }

    private RegisterUnit createBox() {
        return new RegisterUnit().setType(UnitType.BOX);
    }

    private RegisterUnit createPallet() {
        var pallet = new RegisterUnit();
        var korobyte = new Korobyte();
        korobyte.setWidth(1);
        korobyte.setHeight(1);
        korobyte.setLength(1);

        pallet.setType(UnitType.PALLET);
        pallet.setKorobyte(korobyte);
        return pallet;
    }

    private RegisterUnit createPalletWithNoKorobyte() {
        return new RegisterUnit().setType(UnitType.PALLET);
    }

    private Register createRegisterBulkyCargo() {
        var items = List.of(createItemForBulkyCargo());

        return new Register().setPallets(items);
    }

    private Register createEmptyRegister() {
        return new Register();
    }

    private RegisterUnit createItem() {
        Korobyte korobyte = new Korobyte();
        korobyte.setHeight(10);
        korobyte.setLength(20);
        korobyte.setWidth(30);
        korobyte.setWeightGross(BigDecimal.valueOf(5));

        UnitMeta unitMeta = new UnitMeta();
        unitMeta.setPrice(BigDecimal.valueOf(5));

        RegisterUnit registerUnit = new RegisterUnit();
        registerUnit.setKorobyte(korobyte);
        registerUnit.setUnitMeta(unitMeta);
        return registerUnit;
    }

    private RegisterUnit createItemForBulkyCargo() {
        Korobyte korobyte = new Korobyte();
        korobyte.setHeight(3000);
        korobyte.setLength(4000);
        korobyte.setWidth(500);
        korobyte.setWeightGross(BigDecimal.valueOf(5));

        UnitMeta unitMeta = new UnitMeta();
        unitMeta.setPrice(BigDecimal.valueOf(5));

        RegisterUnit registerUnit = new RegisterUnit();
        registerUnit.setKorobyte(korobyte);
        registerUnit.setUnitMeta(unitMeta);
        return registerUnit;
    }

    private MovementCourier createMovementCourier() {
        MovementCourier movementCourier = new MovementCourier();
        movementCourier.setCarNumber("A999AA");
        movementCourier.setOwnershipType(OwnershipType.PROPRIETARY);
        movementCourier.setCarModel("MAN");
        movementCourier.setName("Курьер");
        movementCourier.setSurname("Курьеров");
        movementCourier.setPatronymic("Курьерович");
        movementCourier.setPhone("+78005553535");
        return movementCourier;
    }

    private TransportationLegalInfo createOutboundUnitInfo() {
        TransportationLegalInfo outboundInfo = new TransportationLegalInfo();
        outboundInfo.setInn("INN");
        outboundInfo.setLegalName("MARKET");
        outboundInfo.setLegalAddress("PushkinaOut");
        outboundInfo.setLegalType("OOO");
        outboundInfo.setOgrn("some");
        return outboundInfo;
    }

    private TransportationLegalInfo createInboundUnitInfo() {
        TransportationLegalInfo inboundInfo = new TransportationLegalInfo();
        inboundInfo.setInn("INN");
        inboundInfo.setLegalName("Владелец ИП");
        inboundInfo.setLegalAddress("PushkinaIn");
        inboundInfo.setLegalType("IP");
        inboundInfo.setOgrn("OGRN");
        return inboundInfo;
    }

    private TransportationLegalInfo createMovementInfo() {
        TransportationLegalInfo movementInfo = new TransportationLegalInfo();
        movementInfo.setInn("INN");
        movementInfo.setLegalName("MARKET");
        movementInfo.setLegalAddress("Pushkina");
        movementInfo.setLegalType("OOO");
        movementInfo.setOgrn("some");
        return movementInfo;
    }

    private TransportationMetadata createTransportationMetadata() {
        var data = new TransportationMetadata();
        data.setAddressFrom(new Address());
        data.setAddressTo(new Address());
        return data;
    }

    private Map<Long, Set<Phone>> createPhoneMap() {
        Phone outboundPhone = new Phone();
        outboundPhone.setNumber("+7 999 888 77 66");

        Phone inboundPhone = new Phone();
        inboundPhone.setNumber("+7 800 400 22 00");

        return Map.of(1L, Set.of(outboundPhone), 2L, Set.of(inboundPhone));
    }

    private void checkLegalInfo(TrnLegalInfo trnLegalInfo, TransportationLegalInfo info, String phone) {
        Assertions.assertEquals(info.getInn(), trnLegalInfo.getInn());
        Assertions.assertEquals(info.getLegalName(), trnLegalInfo.getLegalName());
        Assertions.assertEquals(info.getLegalAddress(), trnLegalInfo.getLegalAddress());
        Assertions.assertEquals(phone, trnLegalInfo.getPhoneNumber());
    }

    private void checkCar(TrnCar car, MovementCourier courier) {
        Assertions.assertEquals(courier.getCarModel(), car.getModel());
        Assertions.assertEquals(courier.getCarNumber(), car.getNumber());
        Assertions.assertEquals(courier.getCarTrailerNumber(), car.getTrailerNumber());
        Assertions.assertEquals(
                Optional.ofNullable(
                        courier.getOwnershipType())
                        .map(OwnershipType::getCode)
                        .orElse(null),
                car.getOwnershipType());
        Assertions.assertNull(car.getBrand());
        Assertions.assertEquals(car.getCargoQuantity(), 1);
        Assertions.assertEquals(car.getCargoType(), "палет");
    }

    private void checkTransportationUnit(
        TrnTransportationUnit trnUnit,
        TransportationLegalInfo info,
        LocalDateTime plannedStart
    ) {
        Assertions.assertEquals(trnUnit.getInn(), info.getInn());
        Assertions.assertEquals(trnUnit.getLegalName(), info.getLegalName());
        Assertions.assertEquals(trnUnit.getLegalAddress(), info.getLegalAddress());
        Assertions.assertEquals(
            trnUnit.getPlannedIntervalStart(),
            plannedStart.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))
        );
    }

    private void checkRegister(TrnRegisterCharacteristics trnRegisterCharacteristics) {
        Assertions.assertNull(trnRegisterCharacteristics.getVolume());
        Assertions.assertEquals("Клим Саныч", trnRegisterCharacteristics.getShipper());
        Assertions.assertEquals("10.00 руб.", trnRegisterCharacteristics.getPriceRub());
        Assertions.assertEquals("150,00", trnRegisterCharacteristics.getWeight());
    }

    private void checkCourier(TrnCourier courier, MovementCourier movementCourier) {
        Assertions.assertEquals(movementCourier.getName(), courier.getName());
        Assertions.assertEquals(movementCourier.getSurname(), courier.getSurName());
        Assertions.assertEquals(movementCourier.getPatronymic(), courier.getPatronymic());
        Assertions.assertEquals(movementCourier.getPhone(), courier.getPhoneNumber());
        Assertions.assertEquals(courier.getInn(), EIGHT_INDENTS);
    }
}
