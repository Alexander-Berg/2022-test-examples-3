package ru.yandex.market.delivery.transport_manager.interactor.inbound;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.courier.MovementCourierSendingStateStatus;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterStatus;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.movement_courier.MovementCourierSendingStateMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.register.RegisterMapper;
import ru.yandex.market.delivery.transport_manager.util.LgwUtils;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Address;
import ru.yandex.market.logistic.gateway.common.model.common.Car;
import ru.yandex.market.logistic.gateway.common.model.common.Courier;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.common.Inbound;
import ru.yandex.market.logistic.gateway.common.model.common.InboundType;
import ru.yandex.market.logistic.gateway.common.model.common.LegalEntity;
import ru.yandex.market.logistic.gateway.common.model.common.LegalForm;
import ru.yandex.market.logistic.gateway.common.model.common.Location;
import ru.yandex.market.logistic.gateway.common.model.common.LogisticPoint;
import ru.yandex.market.logistic.gateway.common.model.common.PartialIdType;
import ru.yandex.market.logistic.gateway.common.model.common.Party;
import ru.yandex.market.logistic.gateway.common.model.common.Phone;
import ru.yandex.market.logistic.gateway.common.model.common.RegistryBox;
import ru.yandex.market.logistic.gateway.common.model.common.RegistryPallet;
import ru.yandex.market.logistic.gateway.common.model.common.RegistryType;
import ru.yandex.market.logistic.gateway.common.model.common.UnitCountType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Barcode;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.BarcodeSource;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.CargoType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Contractor;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundRegistry;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.RegistryItem;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.RemainingLifetimes;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Service;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ServiceType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ShelfLife;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ShelfLives;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitOperationType;
import ru.yandex.market.logistic.gateway.common.model.utils.DateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

@DatabaseSetup({
    "/repository/facade/interactor/transportation_with_deps.xml",
    "/repository/facade/courier.xml",
    "/repository/facade/interactor/metadata.xml",
    "/repository/facade/interactor/method.xml",
})
class LgwInboundMethodSenderTest extends AbstractContextualTest {

    @Autowired
    private TransportationMapper mapper;

    @Autowired
    private RegisterMapper registerMapper;

    @Autowired
    private LgwInboundMethodSender sender;

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Autowired
    private MovementCourierSendingStateMapper courierSendingStateMapper;

    @Test
    @SneakyThrows
    @DisplayName("Успешный запрос putInbound в LGW. В первый раз")
    void put() {
        sender.put(mapper.getById(1L), false);

        verify(fulfillmentClient).putInbound(
            eq(expectedInbound(null, null)),
            eq(LgwUtils.partner(6)),
            eq(LgwUtils.inboundRestrictedData("TM1", null, null, null)),
            isNull()
        );
        Assertions.assertEquals(
            Objects.requireNonNull(mapper.getById(1L)).getInboundUnit().getStatus(),
            TransportationUnitStatus.SENT
        );
    }

    @Test
    @SneakyThrows
    @DatabaseSetup(
        value = "/repository/facade/interactor/update/transportation_1_sent.xml",
        type = DatabaseOperation.UPDATE
    )
    @DisplayName("Успешный запрос putInbound в LGW. Повторная отправка")
    void putSecondTime() {
        sender.put(mapper.getById(1L), false);

        verify(fulfillmentClient).putInbound(
            eq(expectedInbound("EXT_2", "EXT_3")),
            eq(LgwUtils.partner(6)),
            eq(LgwUtils.inboundRestrictedData("TM1", null, null, null)),
            isNull()
        );
        Assertions.assertEquals(
            Objects.requireNonNull(mapper.getById(1L)).getInboundUnit().getStatus(),
            TransportationUnitStatus.ACCEPTED
        );
        Assertions.assertEquals(
            courierSendingStateMapper.findOne(2L).getStatus(),
            MovementCourierSendingStateStatus.SENT
        );
    }

    @Test
    @SneakyThrows
    @DisplayName(
        "Успешный запрос putInbound в LGW c заполненными полями для XDock: NextReceiver, Зпер, confirmed, RealSupplier"
    )
    @DatabaseSetup(value = "/repository/facade/interactor/update_type.xml", type = DatabaseOperation.UPDATE)
    @DatabaseSetup(value = "/repository/facade/interactor/xdock_to_ff_metadata.xml", type = DatabaseOperation.INSERT)
    void putSupplyToDc() {
        sender.put(mapper.getById(1L), false);

        ArgumentCaptor<Inbound> inboundCaptor = ArgumentCaptor.forClass(Inbound.class);

        verify(fulfillmentClient).putInbound(
            inboundCaptor.capture(),
            eq(LgwUtils.partner(6)),
            eq(LgwUtils.inboundRestrictedData("TM1", "ЗПер000", true, "ООО ААА")),
            isNull()
            );

        Inbound inbound = inboundCaptor.getValue();

        softly.assertThat(inbound.getNextReceiver()).isEqualTo(nextReceiver());

        Assertions.assertEquals(
            Objects.requireNonNull(mapper.getById(1L)).getInboundUnit().getStatus(),
            TransportationUnitStatus.SENT
        );
    }

    private Party nextReceiver() {
        return Party.builder(
            LogisticPoint.builder(LgwUtils.id("1000002", "LP101"))
                .setContact(LgwUtils.person("ABC", "CBA", "AA"))
                .setLocation(
                    Location.builder("Russia", "Moscow", "Moscow")
                        .setFederalDistrict("Центральный")
                        .setStreet("Новинский бульвар")
                        .setLocationId(100501L)
                        .build()
                )
                .setPhones(List.of())
                .build()
        )
            .setLegalEntity(
                LegalEntity.builder()
                    .setLegalName("TargetFF")
                    .setLegalForm(LegalForm.IP)
                    .setOgrn("12345779")
                    .setInn("1234565891")
                    .setAddress(Address.builder("address").build())
                    .build()
            )
            .build();
    }

    @Test
    @SneakyThrows
    @DisplayName("Успешный запрос putInboundRegistry в LGW")
    @DatabaseSetup({
        "/repository/facade/interactor/register.xml",
        "/repository/facade/interactor/register_for_inbound.xml"
    })
    @DatabaseSetup(
        value = "/repository/facade/interactor/update/transportation_1_sent.xml",
        type = DatabaseOperation.UPDATE
    )
    void putRegistry() {
        sender.putRegistry(Objects.requireNonNull(mapper.getById(1L)));

        verify(fulfillmentClient).putInboundRegistry(eq(registry()), eq(LgwUtils.partner(6)));
        Assertions.assertEquals(
            Objects.requireNonNull(registerMapper.getById(1L)).getStatus(),
            RegisterStatus.SENT_TO_PARTNER
        );
    }

    private Inbound expectedInbound(String outboundExternalId, String inboundExternalId) {
        return Inbound.builder(
            LgwUtils.id("TMU3", inboundExternalId),
            InboundType.DS_SC,
            DateTimeInterval.fromFormattedValue("2020-07-12T12:00+03:00/2020-07-12T20:00+03:00")
        )
            .setLogisticPoint(
                LogisticPoint.builder(LgwUtils.id("102", "LP2"))
                    .setContact(LgwUtils.person("ABC", "CBA", "AA"))
                    .setLocation(
                        Location.builder("Russia", "Moscow", "Moscow")
                            .setFederalDistrict("Центральный")
                            .setStreet("Льва Толстого")
                            .setHouse("18Б")
                            .setLocationId(100600L)
                            .build()
                    )
                    .setPhones(List.of())
                    .build()
            )
            .setCourier(
                Courier.builder()
                    .setPartnerId(LgwUtils.id("7", null))
                    .setPersons(List.of(LgwUtils.person("Олег", "Егоров", "Васильевич")))
                    .setPhone(
                        Phone.builder("+7(904) 444-44-44")
                            .build()
                    )
                    .setCar(
                        Car.builder("О123НО790")
                            .setDescription("Белый форд транзит")
                            .build()
                    )
                    .setLegalEntity(
                        LegalEntity.builder()
                            .setLegalName("FastFurious")
                            .setLegalForm(LegalForm.IP)
                            .setOgrn("33333333")
                            .setInn("222222")
                            .setAddress(Address.builder("address").build())
                            .build()
                    )
                    .build()
            )
            .setOutboundIds(List.of(LgwUtils.id("TMU2", outboundExternalId)))
            .setShipper(
                Party.builder(
                    LogisticPoint.builder(LgwUtils.id("101", "LP1"))
                        .setContact(LgwUtils.person("ABC", "CBA", "AA"))
                        .setLocation(
                            Location.builder("Russia", "Moscow", "Moscow")
                                .setFederalDistrict("Северный")
                                .setStreet("Льва Толстого")
                                .setLocationId(100500L)
                                .build()
                        )
                        .setPhones(List.of())
                        .build()
                )
                    .setLegalEntity(
                        LegalEntity.builder()
                            .setLegalName("Roga'n'Kopyta")
                            .setLegalForm(LegalForm.IP)
                            .setOgrn("12345778")
                            .setInn("1234565890")
                            .setAddress(Address.builder("address").build())
                            .build()
                    )
                    .build()
            )
            .setExternalRequestId("TMU3")
            .build();
    }

    private InboundRegistry registry() {
        return InboundRegistry.builder(
            LgwUtils.id("TMR1", "EXT_1"),
            LgwUtils.id("TMU3", "EXT_3"),
            RegistryType.PLANNED
        )
            .setDate(DateTime.fromLocalDateTime(LocalDateTime.parse("2020-10-24T16:17:29")))
            .setPallets(List.of(new RegistryPallet(
                LgwUtils.unitInfo(LgwUtils.count(UnitCountType.FIT, 1), LgwUtils.id(PartialIdType.PALLET_ID, "100"))
            )))
            .setBoxes(List.of(new RegistryBox(
                LgwUtils.unitInfo(LgwUtils.count(UnitCountType.DEFECT, 1), LgwUtils.id(PartialIdType.BOX_ID, "10"))
            )))
            .setItems(List.of(
                RegistryItem.builder(
                    LgwUtils.unitInfo(
                        LgwUtils.count(UnitCountType.FIT, 3),
                        "В заводской упаковке",
                        LgwUtils.id(PartialIdType.VENDOR_ID, "VENDOR3"),
                        LgwUtils.id(PartialIdType.ARTICLE, "SKU3")
                    )
                )
                    .setBarcodes(List.of(
                        new Barcode("1207454248", null, null),
                        new Barcode("27714676", "Code12", BarcodeSource.PARTNER)
                    ))
                    .setName("Пластиковый мешочек")
                    .setPrice(BigDecimal.valueOf(5.35))
                    .setCargoTypes(List.of(CargoType.VALUABLE, CargoType.JEWELRY))
                    .setInboundServices(List.of(
                        new Service(ServiceType.TRYING, "Примерка", "Возможность примерить товар", true),
                        new Service(ServiceType.PACK, "Упаковка", null, false)
                    ))
                    .setHasLifeTime(true)
                    .setLifeTime(90)
                    .setBoxCapacity(10)
                    .setBoxCount(8)
                    .setComment("Ограниченная серия")
                    .setContractor(new Contractor("5262", "Производитель мешочков"))
                    .setRemainingLifetimes(new RemainingLifetimes(
                        new ShelfLives(new ShelfLife(90), new ShelfLife(null)),
                        new ShelfLives(new ShelfLife(null), new ShelfLife(63))
                    ))
                    .setUpdated(DateTime.fromLocalDateTime(LocalDateTime.parse("2020-10-24T00:13:52")))
                    .setCategoryId(42L)
                    .setRemovableIfAbsent(false)
                    .setUnitOperationType(UnitOperationType.FULFILLMENT)
                    .setUrls(List.of("https://mesho4ek.biz/product/92582"))
                    .build()
            ))
            .build();
    }

    @Test
    @DisplayName("Исключение при попытке отправить putInbound без external_id, но с updateOnly = true")
    @DatabaseSetup({
        "/repository/facade/interactor/register.xml",
        "/repository/facade/interactor/register_for_inbound.xml"
    })
    void testPutInboundFailed() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> sender.put(Objects.requireNonNull(mapper.getById(1L)), true)
        );
    }

    @Test
    @SneakyThrows
    @DatabaseSetup(
        value = "/repository/facade/interactor/update/transportation_1_sent.xml",
        type = DatabaseOperation.UPDATE
    )
    @DisplayName("Успешный запрос getInbound в LGW")
    void get() {
        sender.get(Objects.requireNonNull(mapper.getById(1L)));

        verify(fulfillmentClient).getInbound(
            eq(LgwUtils.id("TMU3", "EXT_3")),
            eq(LgwUtils.partner(6))
        );
    }

    @Test
    @DatabaseSetup(
        value = "/repository/facade/transportations_for_cancellation.xml"
    )
    @SneakyThrows
    @DisplayName("Успешный запрос cancelInbound в LGW")
    void testCancelInbound() {
        Transportation transportation = mapper.getById(3L);
        sender.cancelUnit(transportation.getInboundUnit());

        var resourceId = ResourceId.builder().setYandexId("TMU7").build();

        verify(fulfillmentClient).cancelInbound(
            eq(resourceId),
            eq(LgwUtils.partner(6))
        );
    }
}
