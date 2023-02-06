package ru.yandex.market.wms.shippingsorter.sorting.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.shippingsorter.configuration.ShippingSorterSecurityTestConfiguration;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.BoxId;
import ru.yandex.market.wms.shippingsorter.sorting.IntegrationTest;
import ru.yandex.market.wms.shippingsorter.sorting.entity.ArrivalToNokEntity;
import ru.yandex.market.wms.shippingsorter.sorting.model.ArrivalToNokReason;

@Import(ShippingSorterSecurityTestConfiguration.class)
public class ArrivalToNokRepositoryTest extends IntegrationTest {

    @Autowired
    private ArrivalToNokRepository arrivalToNokRepository;

    @Test
    @DatabaseSetup("/sorting/repository/arrival-to-nok/find/immutable-state.xml")
    @ExpectedDatabase(value = "/sorting/repository/arrival-to-nok/find/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void findAllByBoxIdTest_exist() {
        List<ArrivalToNokEntity> expectedArrivals = List.of(
                ArrivalToNokEntity.builder()
                        .id(1L)
                        .boxId(BoxId.of("P123456780"))
                        .arrivalToNokReason(ArrivalToNokReason.NO_READ_OR_NO_SCAN)
                        .sorterOrderKey(6693493L)
                        .determinationTime(LocalDateTime.parse("2020-04-01T12:34:56.789"))
                        .build(),
                ArrivalToNokEntity.builder()
                        .id(2L)
                        .boxId(BoxId.of("P123456780"))
                        .arrivalToNokReason(ArrivalToNokReason.OVERFLOW)
                        .sorterOrderKey(6693494L)
                        .determinationTime(LocalDateTime.parse("2020-04-01T12:36:56.789"))
                        .build(),
                ArrivalToNokEntity.builder()
                        .id(3L)
                        .boxId(BoxId.of("P123456780"))
                        .arrivalToNokReason(ArrivalToNokReason.NO_WMS_ORDER)
                        .sorterOrderKey(null)
                        .determinationTime(LocalDateTime.parse("2020-04-01T12:38:56.789"))
                        .build()
        );

        List<ArrivalToNokEntity> arrivals = arrivalToNokRepository.findAllByBoxId("P123456780");

        Assertions.assertEquals(expectedArrivals, arrivals);
    }

    @Test
    @DatabaseSetup("/sorting/repository/arrival-to-nok/find/immutable-state.xml")
    @ExpectedDatabase(value = "/sorting/repository/arrival-to-nok/find/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void findAllByBoxIdTest_notExist() {
        List<ArrivalToNokEntity> expectedArrivals = List.of();

        List<ArrivalToNokEntity> arrivals = arrivalToNokRepository.findAllByBoxId("P123456789");

        Assertions.assertEquals(expectedArrivals, arrivals);
    }

    @Test
    @DatabaseSetup("/sorting/repository/arrival-to-nok/find/immutable-state.xml")
    @ExpectedDatabase(value = "/sorting/repository/arrival-to-nok/find/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void findLastByBoxIdTest_exist() {
        Optional<ArrivalToNokEntity> expectedArrival = Optional.of(
                ArrivalToNokEntity.builder()
                        .id(3L)
                        .boxId(BoxId.of("P123456780"))
                        .arrivalToNokReason(ArrivalToNokReason.NO_WMS_ORDER)
                        .sorterOrderKey(null)
                        .determinationTime(LocalDateTime.parse("2020-04-01T12:38:56.789"))
                        .build()
        );

        Optional<ArrivalToNokEntity> arrival = arrivalToNokRepository.findLastByBoxId("P123456780");

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/repository/arrival-to-nok/find/immutable-state.xml")
    @ExpectedDatabase(value = "/sorting/repository/arrival-to-nok/find/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void findLastByBoxIdTest_notExist() {
        Optional<ArrivalToNokEntity> expectedArrival = Optional.empty();

        Optional<ArrivalToNokEntity> arrival = arrivalToNokRepository.findLastByBoxId("P123456789");

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/repository/arrival-to-nok/find/immutable-state.xml")
    @ExpectedDatabase(value = "/sorting/repository/arrival-to-nok/find/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void findBySorterOrderKeyTest_exist() {
        Optional<ArrivalToNokEntity> expectedArrival = Optional.of(
                ArrivalToNokEntity.builder()
                        .id(1L)
                        .boxId(BoxId.of("P123456780"))
                        .arrivalToNokReason(ArrivalToNokReason.NO_READ_OR_NO_SCAN)
                        .sorterOrderKey(6693493L)
                        .determinationTime(LocalDateTime.parse("2020-04-01T12:34:56.789"))
                        .build()
        );

        Optional<ArrivalToNokEntity> arrival = arrivalToNokRepository.findBySorterOrderKey(6693493L);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/repository/arrival-to-nok/find/immutable-state.xml")
    @ExpectedDatabase(value = "/sorting/repository/arrival-to-nok/find/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void findBySorterOrderKeyTest_notExist() {
        Optional<ArrivalToNokEntity> expectedArrival = Optional.empty();

        Optional<ArrivalToNokEntity> arrival = arrivalToNokRepository.findBySorterOrderKey(6693495L);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/repository/arrival-to-nok/insert/before.xml")
    @ExpectedDatabase(value = "/sorting/repository/arrival-to-nok/insert/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void insertTest() {
        ArrivalToNokEntity arrival = ArrivalToNokEntity.builder()
                .boxId(BoxId.of("P123456780"))
                .arrivalToNokReason(ArrivalToNokReason.OVERFLOW)
                .sorterOrderKey(6693494L)
                .build();

        arrivalToNokRepository.insert(arrival);
    }
}
