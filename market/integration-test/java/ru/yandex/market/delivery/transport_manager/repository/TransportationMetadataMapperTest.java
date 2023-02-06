package ru.yandex.market.delivery.transport_manager.repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Address;
import ru.yandex.market.delivery.transport_manager.domain.entity.LogisticsPointMetadata;
import ru.yandex.market.delivery.transport_manager.domain.entity.Phone;
import ru.yandex.market.delivery.transport_manager.domain.entity.Schedule;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationMetadata;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMetadataMapper;

import static org.hamcrest.MatcherAssert.assertThat;

@DatabaseSetup({
    "/repository/transportation/multiple_transportations_deps.xml",
    "/repository/transportation/multiple_transportations.xml",
    "/repository/transportation/metadata.xml"
})
class TransportationMetadataMapperTest extends AbstractContextualTest {
    private final Address secondAddress = new Address()
        .setId(2L)
        .setCountry("Russia")
        .setLocality("Moscow")
        .setRegion("Moscow")
        .setFederalDistrict("Центральный")
        .setStreet("Льва Толстого")
        .setHouse("18Б")
        .setLocationId(100600);
    private final Address firstAddress = new Address()
        .setId(1L)
        .setCountry("Russia")
        .setLocality("Moscow")
        .setRegion("Moscow")
        .setFederalDistrict("Северный")
        .setStreet("Льва Толстого")
        .setLocationId(100500);
    @Autowired
    private TransportationMetadataMapper mapper;

    @Test
    void get() {
        TransportationMetadata transportationMetadata = mapper.get(1L);

        softly.assertThat(transportationMetadata).isNotNull();
        softly.assertThat(transportationMetadata.getAddressFrom().getId())
            .isEqualTo(1L);
        softly.assertThat(transportationMetadata.getAddressFrom().getLocationId())
            .isEqualTo(100500);
        softly.assertThat(transportationMetadata.getAddressTo().getId())
            .isEqualTo(2L);
        softly.assertThat(transportationMetadata.getAddressTo().getLocationId())
            .isEqualTo(100600);
    }

    @Test
    void getMap() {

        Map<Long, TransportationMetadata> metadataMap = mapper.get(Set.of(1L, 2L));

        assertThat(metadataMap, Is.is(Map.of(
            1L,
            new TransportationMetadata().setId(1L).setTransportationId(1L).setAddressFrom(firstAddress).setAddressTo(
                secondAddress),
            2L,
            new TransportationMetadata().setId(5L).setTransportationId(2L).setAddressFrom(secondAddress).setAddressTo(
                firstAddress)
        )));

    }

    @Test
    void insert() {
        mapper.insert(3L, 1L, 2L);

        var metadata = mapper.get(3L);

        softly.assertThat(metadata).isNotNull();
        softly.assertThat(metadata.getAddressFrom().getId()).isEqualTo(1L);
        softly.assertThat(metadata.getAddressTo().getId()).isEqualTo(2L);
    }

    @Test
    void insertObject() {
        mapper.insertObject(
            metadata(
                3L,
                address(123, "Россия"),
                address(345, "Иран")
            ),
            false
        );

        var metadata = mapper.get(3L);

        softly.assertThat(metadata).isNotNull();
        softly.assertThat(metadata.getAddressFrom().getLocationId()).isEqualTo(123);
        softly.assertThat(metadata.getAddressFrom().getCountry()).isEqualTo("Россия");
        softly.assertThat(metadata.getAddressTo().getLocationId()).isEqualTo(345);
        softly.assertThat(metadata.getAddressTo().getCountry()).isEqualTo("Иран");
    }

    @Test
    void insertObjectXDoc() {
        mapper.insertObject(
            metadata(
                3L,
                null,
                address(345, "Иран")
            ),
            true
        );

        var metadata = mapper.get(3L);

        softly.assertThat(metadata).isNotNull();
        softly.assertThat(metadata.getAddressFrom()).isNull();
        softly.assertThat(metadata.getAddressTo().getLocationId()).isEqualTo(345);
        softly.assertThat(metadata.getAddressTo().getCountry()).isEqualTo("Иран");
    }

    @Test
    void insertObjectNullAddressFrom() {
        softly.assertThatThrownBy(() ->
            mapper.insertObject(
                metadata(
                    3L,
                    null,
                    address(345, "Иран")
                ),
                false
            ))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @ExpectedDatabase(value = "/repository/metadata/expected_logistics_point_meta.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void insertLogisticsPointTest() {
        Set<Schedule> schedules = Set.of(
            new Schedule().setDay(1).setTimeFrom(LocalTime.MIDNIGHT).setTimeTo(LocalTime.NOON),
            new Schedule().setDay(2).setTimeFrom(LocalTime.MIDNIGHT).setTimeTo(LocalTime.NOON)
        );
        Set<Phone> phones = Set.of(
            new Phone().setNumber("12345678").setInternalNumber("555"),
            new Phone().setNumber("999999999").setInternalNumber("999")
        );

        LogisticsPointMetadata logisticsPointMetadata = new LogisticsPointMetadata()
            .setId(1L)
            .setTransportationUnitId(5L)
            .setInstruction("instruction")
            .setExternalId("exId1")
            .setName("name")
            .setContactName("contactName")
            .setContactSurname("surname")
            .setContactPatronymic("patronymic")
            .setLogisticsPointId(101L)
            .setAddressId(2L)
            .setSchedules(schedules)
            .setPhones(phones);

        mapper.createFullLogisticsPoint(logisticsPointMetadata);

        LogisticsPointMetadata logisticsPoint = mapper.getLogisticsPointForUnit(5L);
        softly.assertThat(logisticsPoint).isNotNull();
        softly.assertThat(logisticsPoint.getInstruction()).isEqualTo("instruction");
        softly.assertThat(logisticsPoint.getPhones().size()).isEqualTo(2);
        softly.assertThat(logisticsPoint.getSchedules().size()).isEqualTo(2);
        softly.assertThat(logisticsPoint.getTransportationUnitId()).isEqualTo(5L);
        softly.assertThat(logisticsPoint.getAddress()).isEqualTo(secondAddress);
    }

    @Test
    @ExpectedDatabase(value = "/repository/metadata/expected_logistics_point_meta.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void insertLogisticsPointsTest() {
        Set<Schedule> schedules = Set.of(
            new Schedule().setDay(1).setTimeFrom(LocalTime.MIDNIGHT).setTimeTo(LocalTime.NOON),
            new Schedule().setDay(2).setTimeFrom(LocalTime.MIDNIGHT).setTimeTo(LocalTime.NOON)
        );
        Set<Phone> phones = Set.of(
            new Phone().setNumber("12345678").setInternalNumber("555"),
            new Phone().setNumber("999999999").setInternalNumber("999")
        );

        LogisticsPointMetadata logisticsPointMetadata = new LogisticsPointMetadata()
            .setId(1L)
            .setTransportationUnitId(5L)
            .setInstruction("instruction")
            .setExternalId("exId1")
            .setName("name")
            .setContactName("contactName")
            .setContactSurname("surname")
            .setContactPatronymic("patronymic")
            .setLogisticsPointId(101L)
            .setAddressId(2L)
            .setSchedules(schedules)
            .setPhones(phones);

        mapper.createFullLogisticsPoint(logisticsPointMetadata);

        List<LogisticsPointMetadata> logisticsPoints = mapper.getLogisticsPointsForUnits(Set.of(5L));
        softly.assertThat(logisticsPoints).hasSize(1);
        LogisticsPointMetadata logisticsPoint = logisticsPoints.get(0);
        softly.assertThat(logisticsPoint).isNotNull();
        softly.assertThat(logisticsPoint.getInstruction()).isEqualTo("instruction");
        softly.assertThat(logisticsPoint.getPhones().size()).isEqualTo(2);
        softly.assertThat(logisticsPoint.getSchedules().size()).isEqualTo(2);
        softly.assertThat(logisticsPoint.getTransportationUnitId()).isEqualTo(5L);
        softly.assertThat(logisticsPoint.getAddress()).isEqualTo(secondAddress);
    }

    @Test
    @DatabaseSetup("/repository/metadata/logistics_point_metadata.xml")
    @ExpectedDatabase(value = "/repository/metadata/single_logistics_point_metadata.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void deleteLogisticsPointTest() {
        mapper.deleteLogisticsPoint(6);
    }

    private static Address address(Integer locationId, String country) {
        return new Address()
            .setLocationId(locationId)
            .setCountry(country);
    }

    private static TransportationMetadata metadata(
        Long transportationId,
        Address addressFrom,
        Address addressTo
    ) {
        return new TransportationMetadata()
            .setTransportationId(transportationId)
            .setAddressFrom(addressFrom)
            .setAddressTo(addressTo);
    }
}
