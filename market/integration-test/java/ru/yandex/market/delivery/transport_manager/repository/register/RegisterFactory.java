package ru.yandex.market.delivery.transport_manager.repository.register;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import lombok.experimental.UtilityClass;

import ru.yandex.market.delivery.transport_manager.domain.entity.Korobyte;
import ru.yandex.market.delivery.transport_manager.domain.entity.dto.RegisterOrdersCountDto;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Barcode;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Contractor;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.DataEnteredByMerchant;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.InboundPrimaryDocument;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.InboundService;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.PartialId;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Register;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterRestrictedData;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RemainingLifetimes;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.ShelfLives;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.UnitCount;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.UnitMeta;
import ru.yandex.market.delivery.transport_manager.domain.enums.BarcodeSource;
import ru.yandex.market.delivery.transport_manager.domain.enums.CargoType;
import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;
import ru.yandex.market.delivery.transport_manager.domain.enums.IdType;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterStatus;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterType;
import ru.yandex.market.delivery.transport_manager.domain.enums.ServiceCode;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitOperationType;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitType;

import static ru.yandex.market.delivery.transport_manager.domain.enums.RegisterStatus.DO_NOT_NEED_TO_SEND;

@UtilityClass
public class RegisterFactory {

    @Nonnull
    public UnitCount newUnitCountFit100() {
        return new UnitCount()
            .setCountType(CountType.FIT)
            .setQuantity(100);
    }

    @Nonnull
    public UnitCount newUnitCountDefect3() {
        return new UnitCount()
            .setCountType(CountType.DEFECT)
            .setQuantity(3);
    }

    @Nonnull
    public Korobyte newKorobyte300x50x25() {
        return new Korobyte()
            .setWidth(300)
            .setHeight(50)
            .setLength(25)
            .setWeightGross(new BigDecimal("39.5000"))
            .setWeightNet(new BigDecimal("35.0000"))
            .setWeightTare(new BigDecimal("4.5000"));
    }

    @Nonnull
    public Barcode newMinimalBarcode() {
        return new Barcode()
            .setCode("1207454248");
    }

    @Nonnull
    public Barcode newFullBarcode() {
        return new Barcode()
            .setCode("27714676")
            .setSource(BarcodeSource.PARTNER)
            .setType("Code12");
    }

    @Nonnull
    public UnitMeta newFullUnitMeta() {
        return new UnitMeta()
            .setName("Пластиковый мешочек")
            .setPrice(new BigDecimal("5.35"))
            .setCargoTypes(List.of(CargoType.VALUABLE, CargoType.JEWELRY))
            .setVendorCodes(List.of("BT-94362", "KL2R-X"))
            .setInboundServices(List.of(
                new InboundService()
                    .setCode(ServiceCode.TRYING)
                    .setName("Примерка")
                    .setDescription("Возможность примерить товар")
                    .setOptional(true),
                new InboundService()
                    .setCode(ServiceCode.PACK)
                    .setName("Упаковка")
                    .setOptional(false)
            ))
            .setBarcodes(List.of(newMinimalBarcode(), newFullBarcode()))
            .setHasLifeTime(true)
            .setLifeTime(90)
            .setBoxCapacity(10)
            .setBoxCount(8)
            .setComment("Ограниченная серия")
            .setContractor(
                new Contractor()
                    .setId("5262")
                    .setName("Производитель мешочков")
            )
            .setRemainingLifetimes(
                new RemainingLifetimes()
                    .setInbound(new ShelfLives().setDays(90))
                    .setOutbound(new ShelfLives().setPercentage(63))
            )
            .setUpdated(Instant.parse("2020-10-23T21:13:52.00Z"))
            .setCategoryId(42)
            .setUnitOperationType(UnitOperationType.FULFILLMENT)
            .setRemovableIfAbsent(false)
            .setUrls(List.of(
                "https://plastic-mesho4ek.com/product/92582",
                "https://mesho4ek.biz/product/92582"
            ));
    }

    @Nonnull
    public RegisterUnit newMinimalRegisterUnit() {
        return new RegisterUnit()
            .setType(UnitType.PALLET)
            .setId(1L)
            .setRegisterId(2L);
    }

    @Nonnull
    public RegisterUnit newRegisterUnit() {
        return new RegisterUnit()
            .setId(2L)
            .setBarcode("B108324521")
            .setPartialIds(
                List.of(
                    new PartialId()
                        .setIdType(IdType.PALLET_ID)
                        .setValue("QT391-65Z"),
                    new PartialId()
                        .setIdType(IdType.BOX_ID)
                        .setValue("B6320")
                )
            )
            .setType(UnitType.ITEM)
            .setRegisterId(2L)
            .setCounts(List.of(newUnitCountFit100(), newUnitCountDefect3()))
            .setKorobyte(newKorobyte300x50x25())
            .setDescription("В заводской упаковке")
            .setUnitMeta(newFullUnitMeta())
            .setLgwTask("1")
            .setParentIds(Set.of(1L));
    }

    @Nonnull
    public RegisterUnit newPallet(long id) {
        return new RegisterUnit()
            .setType(UnitType.PALLET)
            .setId(id)
            .setRegisterId(2L);
    }

    @Nonnull
    public RegisterUnit newBox(long id) {
        return new RegisterUnit()
            .setType(UnitType.BOX)
            .setId(id)
            .setRegisterId(2L);
    }

    @Nonnull
    public Register newMinimalRegister() {
        return newRegister(1L, RegisterStatus.DRAFT);
    }

    @Nonnull
    public Register newFullRegister() {
        return newRegister(2L, RegisterStatus.PREPARING)
            .setType(RegisterType.PLAN)
            .setExternalId("register1")
            .setDocumentId("abc123")
            .setPartnerId(2L)
            .setFfwfId(3L)
            .setDate(Instant.parse("2020-10-24T13:17:29.00Z"))
            .setComment("Очень важный комментарий")
            .setDocumentsReady(false);
    }

    @Nonnull
    public Register newFullRegisterWithUnits() {
        return newFullRegister()
            .setPallets(List.of(newMinimalRegisterUnit(), newPallet(4L)))
            .setBoxes(
                List.of(
                    newBox(5L)
                        .setPartialIds(List.of(
                            new PartialId()
                                .setIdType(IdType.BOX_ID)
                                .setValue("P001322492")
                        )),
                    newBox(6L)
                )
            )
            .setItems(List.of(newRegisterUnit()));
    }

    @Nonnull
    public Register newRegisterFactXDocWithRestrictedData() {
        return newRegister(4L, DO_NOT_NEED_TO_SEND)
                .setType(RegisterType.FACT)
                .setExternalId("xdoc-fact")
                .setPartnerId(6L)
                .setFfwfId(10L)
                .setDate(Instant.parse("2020-10-24T13:17:29.00Z"))
                .setComment("Принятая на РЦ поставка с информацией для AXAPTA")
                .setRestrictedData(new RegisterRestrictedData()
                    .setPrimaryDocument(new InboundPrimaryDocument()
                        .setArrivalDate(OffsetDateTime.parse("2020-10-10T00:00:00+03:00"))
                        .setDataEnteredByMerchant(
                            new DataEnteredByMerchant()
                                .setDate(OffsetDateTime.parse("2020-10-09T00:00:00+03:00"))
                                .setNumber("number-1")
                                .setPrice(BigDecimal.valueOf(100.99))
                                .setTax(BigDecimal.valueOf(20.19))
                                .setUntaxedPrice(BigDecimal.valueOf(80.8))
                        )));

    }

    @Nonnull
    public Register newRegister(Long id, RegisterStatus status) {
        return new Register()
            .setId(id)
            .setType(RegisterType.FACT)
            .setStatus(status)
            .setDocumentsReady(false);
    }

    @Nonnull
    public List<RegisterOrdersCountDto> newItemsCountDtos() {
        return List.of(
            new RegisterOrdersCountDto(1L, 2L),
            new RegisterOrdersCountDto(2L, 2L)
        );
    }
}
