package ru.yandex.market.delivery.transport_manager.service.external.lgw;

import java.math.BigDecimal;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import javassist.NotFoundException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.logistic.gateway.common.model.common.Address;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.common.LegalEntity;
import ru.yandex.market.logistic.gateway.common.model.common.LegalForm;
import ru.yandex.market.logistic.gateway.common.model.common.Location;
import ru.yandex.market.logistic.gateway.common.model.common.LogisticPoint;
import ru.yandex.market.logistic.gateway.common.model.common.Movement;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.common.Party;
import ru.yandex.market.logistic.gateway.common.model.common.Person;
import ru.yandex.market.logistic.gateway.common.model.common.Phone;
import ru.yandex.market.logistic.gateway.common.model.common.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.common.TripInfo;
import ru.yandex.market.logistic.gateway.common.model.common.TripType;
import ru.yandex.market.logistic.gateway.common.model.common.request.restricted.PutMovementRestrictedData;

@DatabaseSetup({
    "/repository/transportation/multiple_transportations_deps.xml",
    "/repository/transportation/multiple_transportations.xml",
    "/repository/transportation/metadata.xml",
})
class MovementCreatorServiceTest extends AbstractContextualTest {
    @Autowired
    private MovementCreatorService movementCreatorService;

    @Autowired
    private LgwClientExecutor lgwClientExecutor;

    @Autowired
    private TransportationMapper transportationMapper;

    @Test
    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_legal_info.xml",
        "/repository/transportation/multuple_transportations_logistics_points.xml"
    })
    void testCorrectCreation() throws NotFoundException {
        movementCreatorService.create(1L);

        ArgumentCaptor<Movement> movementCaptor = ArgumentCaptor.forClass(Movement.class);
        ArgumentCaptor<Partner> partnerCaptor = ArgumentCaptor.forClass(Partner.class);
        Mockito.verify(lgwClientExecutor).putMovement(
            movementCaptor.capture(),
            partnerCaptor.capture(),
            Mockito.eq(null)
        );

        Movement movement = movementCaptor.getValue();
        Partner partner = partnerCaptor.getValue();

        assertThatModelEquals(
            party(
                logisticPoint(
                    resourceId(
                        "100600",
                        "ext2"
                    ),
                    "name2",
                    location(
                        "Russia",
                        "Moscow",
                        "Moscow",
                        "Центральный",
                        "Льва Толстого",
                        "18Б",
                        100600L
                    ),
                    contact("contactName2", "surname2", "patronymic2"),
                    List.of()
                ),
                legalEntity(
                    address("new_addr"),
                    "Компания1",
                    LegalForm.IP,
                    "98769",
                    "1234567891"
                )
            ),
            movement.getReceiver()
        );

        assertThatModelEquals(
            party(
                logisticPoint(
                    resourceId(
                        "100500",
                        "ext1"
                    ),
                    "name",
                    location(
                        "Russia",
                        "Moscow",
                        "Moscow",
                        "Северный",
                        "Льва Толстого",
                        null,
                        100500L
                    ),
                    contact("contactName", "surname", "patronymic"),
                    List.of()
                ),
                legalEntity(
                    address("адрес"),
                    "Компания",
                    LegalForm.OOO,
                    "98768",
                    "1234567890"
                )
            ),
            movement.getShipper()
        );
        softly.assertThat(movement.getVolume()).isEqualTo(BigDecimal.valueOf(2.4));
        softly.assertThat(movement.getWeight()).isEqualTo(BigDecimal.valueOf(1));

        softly.assertThat(partner.getId()).isEqualTo(15);
    }

    @Test
    @SneakyThrows
    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_legal_info.xml",
        "/repository/transportation/multuple_transportations_logistics_points.xml"
    })
    @DatabaseSetup(
        type = DatabaseOperation.REFRESH,
        value = "/repository/transportation/transportation_with_booked_slots.xml"
    )
    void testCorrectCreationWithBookedSlots() {
        movementCreatorService.create(1L);

        ArgumentCaptor<Movement> movementCaptor = ArgumentCaptor.forClass(Movement.class);
        ArgumentCaptor<Partner> partnerCaptor = ArgumentCaptor.forClass(Partner.class);
        Mockito.verify(lgwClientExecutor).putMovement(
            movementCaptor.capture(),
            partnerCaptor.capture(),
            Mockito.eq(null)
        );
        Movement movement = movementCaptor.getValue();

        // Проверяем, что для putMovement используется время из забронированных слотов, а не плановое время
        softly.assertThat(movement.getOutboundInterval())
            .isEqualTo(DateTimeInterval.fromFormattedValue("2021-06-07T12:00:00+03:00/2021-06-07T13:00:00+03:00"));
        softly.assertThat(movement.getInboundInterval())
            .isEqualTo(DateTimeInterval.fromFormattedValue("2021-06-07T15:00:00+03:00/2021-06-07T16:00:00+03:00"));
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_legal_info_empty_address.xml",
        "/repository/transportation/multuple_transportations_logistics_points.xml"
    })
    void testCorrectCreationWithEmptyLegalAddress() throws NotFoundException {
        movementCreatorService.create(1L);

        ArgumentCaptor<Movement> movementCaptor = ArgumentCaptor.forClass(Movement.class);
        ArgumentCaptor<Partner> partnerCaptor = ArgumentCaptor.forClass(Partner.class);
        Mockito.verify(lgwClientExecutor).putMovement(
            movementCaptor.capture(),
            partnerCaptor.capture(),
            Mockito.eq(null)
        );

        Movement movement = movementCaptor.getValue();
        Partner partner = partnerCaptor.getValue();

        assertThatModelEquals(
            party(
                logisticPoint(
                    resourceId(
                        "100600",
                        "ext2"
                    ),
                    "name2",
                    location(
                        "Russia",
                        "Moscow",
                        "Moscow",
                        "Центральный",
                        "Льва Толстого",
                        "18Б",
                        100600L
                    ),
                    contact("contactName2", "surname2", "patronymic2"),
                    List.of()
                ),
                legalEntity(
                    null,
                    "Компания1",
                    LegalForm.IP,
                    "98769",
                    "1234567891"
                )
            ),
            movement.getReceiver()
        );

        assertThatModelEquals(
            party(
                logisticPoint(
                    resourceId(
                        "100500",
                        "ext1"
                    ),
                    "name",
                    location(
                        "Russia",
                        "Moscow",
                        "Moscow",
                        "Северный",
                        "Льва Толстого",
                        null,
                        100500L
                    ),
                    contact("contactName", "surname", "patronymic"),
                    List.of()
                ),
                legalEntity(
                    null,
                    "Компания",
                    LegalForm.OOO,
                    "98768",
                    "1234567890"
                )
            ),
            movement.getShipper()
        );
        softly.assertThat(movement.getVolume()).isEqualTo(BigDecimal.valueOf(2.4));
        softly.assertThat(movement.getWeight()).isEqualTo(BigDecimal.valueOf(1));

        softly.assertThat(partner.getId()).isEqualTo(15);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_legal_info.xml",
        "/repository/transportation/multuple_transportations_logistics_points.xml",
        "/repository/transportation/trip_data.xml"
    })
    @DatabaseSetup(
        value = "/repository/transportation/update/update_to_linehaul_main.xml",
        type = DatabaseOperation.UPDATE
    )
    @SneakyThrows
    void testCorrectCreationWithTripData() {
        movementCreatorService.create(1L);

        ArgumentCaptor<Movement> movementCaptor = ArgumentCaptor.forClass(Movement.class);
        ArgumentCaptor<Partner> partnerCaptor = ArgumentCaptor.forClass(Partner.class);
        Mockito.verify(lgwClientExecutor).putMovement(
            movementCaptor.capture(),
            partnerCaptor.capture(),
            Mockito.eq(null)
        );

        Movement movement = movementCaptor.getValue();

        softly.assertThat(movement.getTrip()).isEqualTo(
            new TripInfo.TripInfoBuilder()
                .setTripId(ResourceId.builder().setYandexId("TMT10").build())
                .setFromIndex(0)
                .setToIndex(1)
                .setTotalCount(2)
                .setType(TripType.MAIN)
                .build()
        );

    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_legal_info.xml",
        "/repository/transportation/multuple_transportations_logistics_points.xml",
        "/repository/transportation/trip_data_with_run_id.xml"
    })
    @DatabaseSetup(
        value = "/repository/transportation/update/update_to_linehaul_main.xml",
        type = DatabaseOperation.UPDATE
    )
    @SneakyThrows
    void testCorrectCreationWithTripWithRunId() {
        movementCreatorService.create(1L);

        ArgumentCaptor<Movement> movementCaptor = ArgumentCaptor.forClass(Movement.class);
        ArgumentCaptor<Partner> partnerCaptor = ArgumentCaptor.forClass(Partner.class);
        Mockito.verify(lgwClientExecutor).putMovement(
            movementCaptor.capture(),
            partnerCaptor.capture(),
            Mockito.eq(null)
        );

        Movement movement = movementCaptor.getValue();

        softly.assertThat(movement.getTrip()).isEqualTo(
            new TripInfo.TripInfoBuilder()
                .setTripId(ResourceId.builder().setYandexId("TMT10").setPartnerId("10001").build())
                .setFromIndex(0)
                .setToIndex(1)
                .setTotalCount(2)
                .setType(TripType.MAIN)
                .build()
        );

    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_legal_info.xml",
        "/repository/transportation/multuple_transportations_logistics_points.xml"
    })
    void testMovementStatus() throws NotFoundException {
        movementCreatorService.create(1L);
        Transportation transportation = transportationMapper.getById(1L);
        softly.assertThat(transportation.getMovement().getStatus())
            .isEqualTo(MovementStatus.LGW_SENT);
    }

    @Test
    void testMovementStatusAlreadyLgwSent() throws NotFoundException {
        softly.assertThatThrownBy(() -> movementCreatorService.create(3L))
            .isInstanceOf(IllegalStateException.class);
        Transportation transportation = transportationMapper.getById(3L);
        softly.assertThat(transportation.getMovement().getStatus())
            .isEqualTo(MovementStatus.LGW_CREATED);
        Mockito.verifyNoMoreInteractions(lgwClientExecutor);
    }

    @Test
    void testNotExistsTransportation() {
        Assertions.assertThrows(
            NotFoundException.class,
            () -> movementCreatorService.create(15L)
        );
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_legal_info.xml",
        "/repository/transportation/multuple_transportations_logistics_points.xml",
        "/repository/register/register_with_volume.xml"
    })
    void testOutboundRegisterVolume() throws NotFoundException {
        movementCreatorService.create(1L);

        ArgumentCaptor<Movement> movementCaptor = ArgumentCaptor.forClass(Movement.class);
        Mockito.verify(lgwClientExecutor).putMovement(
            movementCaptor.capture(),
            Mockito.any(),
            Mockito.eq(null)
        );

        Movement movement = movementCaptor.getValue();
        softly.assertThat(movement.getVolume()).isEqualTo(BigDecimal.valueOf(0.111));
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_legal_info.xml",
        "/repository/transportation/multuple_transportations_logistics_points_without_contacts.xml",
        "/repository/register/register_with_volume.xml"
    })
    void testOutboundEmptyContact() throws NotFoundException {
        movementCreatorService.create(1L);

        ArgumentCaptor<Movement> movementCaptor = ArgumentCaptor.forClass(Movement.class);
        Mockito.verify(lgwClientExecutor).putMovement(
            movementCaptor.capture(),
            Mockito.any(),
            Mockito.eq(null)
        );

        Movement movement = movementCaptor.getValue();
        softly.assertThat(movement.getShipper().getLogisticPoint().getContact()).isNull();
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_legal_info.xml",
        "/repository/transportation/multuple_transportations_logistics_points.xml"
    })
    @DatabaseSetup(
        type = DatabaseOperation.UPDATE,
        value = "/repository/transportation/update/movement_with_predefined_courier.xml"
    )
    void testWitCourier() throws Exception {
        movementCreatorService.create(1L);

        ArgumentCaptor<PutMovementRestrictedData> restrictedDataCaptor =
            ArgumentCaptor.forClass(PutMovementRestrictedData.class);

        Mockito.verify(lgwClientExecutor).putMovement(
            Mockito.any(),
            Mockito.any(),
            restrictedDataCaptor.capture()
        );

        PutMovementRestrictedData restrictedData = restrictedDataCaptor.getValue();

        softly.assertThat(restrictedData.getCourierId()).isEqualTo(12L);
        softly.assertThat(restrictedData.getTransportId()).isEqualTo(15L);
    }

    private Party party(LogisticPoint logisticPoint, LegalEntity legalEntity) {
        return Party.builder(logisticPoint)
            .setLegalEntity(legalEntity)
            .build();
    }

    private LogisticPoint logisticPoint(
        ResourceId resourceId,
        String name,
        Location location,
        Person contact,
        List<Phone> phones
    ) {
        return LogisticPoint.builder(resourceId)
            .setName(name)
            .setLocation(location)
            .setContact(contact)
            .setPhones(phones)
            .build();
    }

    private Location location(
        String country,
        String region,
        String locality,
        String federalDistrict,
        String street,
        String house,
        Long locationId
    ) {
        return Location.builder(country, region, locality)
            .setFederalDistrict(federalDistrict)
            .setStreet(street)
            .setHouse(house)
            .setLocationId(locationId)
            .build();
    }

    private Person contact(
        String name,
        String surname,
        String patronymic
    ) {
        return Person.builder(name)
            .setSurname(surname)
            .setPatronymic(patronymic)
            .build();
    }

    private LegalEntity legalEntity(
        Address address,
        String legalName,
        LegalForm legalForm,
        String ogrn,
        String inn
    ) {
        return LegalEntity.builder()
            .setAddress(address)
            .setLegalName(legalName)
            .setLegalForm(legalForm)
            .setOgrn(ogrn)
            .setInn(inn)
            .build();
    }

    private Address address(String combinedAddress) {
        return Address.builder(combinedAddress)
            .build();
    }

    private ResourceId resourceId(String yandexId, String partnerId) {
        return ResourceId.builder()
            .setYandexId(yandexId)
            .setPartnerId(partnerId)
            .build();
    }
}
