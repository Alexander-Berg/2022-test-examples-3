package ru.yandex.market.wms.shippingsorter.sorting.repository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.shippingsorter.configuration.ShippingSorterSecurityTestConfiguration;
import ru.yandex.market.wms.shippingsorter.core.sorting.domain.SorterOrderStatus;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.BoxId;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.LocationId;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.PackStationId;
import ru.yandex.market.wms.shippingsorter.sorting.IntegrationTest;
import ru.yandex.market.wms.shippingsorter.sorting.entity.SorterOrderEntity;
import ru.yandex.market.wms.shippingsorter.sorting.entity.id.SorterExitId;
import ru.yandex.market.wms.shippingsorter.sorting.exception.UpdateSorterOrderException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import(ShippingSorterSecurityTestConfiguration.class)
public class SorterOrderRepositoryTest extends IntegrationTest {

    @Autowired
    private SorterOrderRepository sorterOrderRepository;

    @Autowired
    private Clock clock;

    @Test
    @DatabaseSetup("/sorting/repository/sorter-order/create/init-state.xml")
    @ExpectedDatabase(value = "/sorting/repository/sorter-order/create/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void shouldSuccessInsert() {
        SorterOrderEntity sorterOrder = SorterOrderEntity.builder()
                .packStationId(PackStationId.of("UPACK-01"))
                .sorterExitId(SorterExitId.of("SRT-01-01"))
                .actualLocationId(LocationId.of("UPACK-01"))
                .alternateSorterExitId(SorterExitId.of("SRT-01-02"))
                .errorSorterExitId(SorterExitId.of("SRT-01-03"))
                .boxId(BoxId.of("P00035"))
                .status(SorterOrderStatus.ASSIGNED)
                .assignee("conveyor")
                .weightMin(50)
                .weightMax(500)
                .addWho("TEST")
                .editWho("TEST")
                .addDate(LocalDateTime.now(clock))
                .editDate(LocalDateTime.now(clock))
                .build();

        sorterOrderRepository.insertAndReturnId(sorterOrder);
    }

    @Test
    @DatabaseSetup("/sorting/repository/sorter-order/update/init-state.xml")
    @ExpectedDatabase(value = "/sorting/repository/sorter-order/update/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void shouldSuccessUpdate() {
        SorterOrderEntity sorterOrder = SorterOrderEntity.builder()
                .id(1L)
                .actualLocationId(LocationId.of("SRT-01-02"))
                .status(SorterOrderStatus.IN_PROGRESS)
                .editWho("TEST")
                .rowVersion(1234)
                .build();

        sorterOrderRepository.update(sorterOrder);
    }

    @Test
    @DatabaseSetup("/sorting/repository/sorter-order/update/init-state.xml")
    public void shouldNotUpdateIfVersionIsNotValid() {
        SorterOrderEntity sorterOrder = SorterOrderEntity.builder()
                .id(1L)
                .actualLocationId(LocationId.of("SRT-01-02"))
                .status(SorterOrderStatus.IN_PROGRESS)
                .editWho("TEST")
                .rowVersion(12344555)
                .build();

        assertThrows(UpdateSorterOrderException.class, () -> sorterOrderRepository.update(sorterOrder));
    }

    @Test
    @DatabaseSetup("/sorting/repository/sorter-order/update/init-state.xml")
    public void shouldNotUpdateIfListOfSorterOrderIsEmpty() {
        assertThrows(UpdateSorterOrderException.class, () -> sorterOrderRepository.update(Collections.emptyList()));
    }

    @Test
    @DatabaseSetup("/sorting/repository/sorter-order/find-active/immutable.xml")
    @ExpectedDatabase(
            value = "/sorting/repository/sorter-order/find-active/immutable.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void findActiveFromDate() {
        LocalDateTime fromDate = LocalDateTime.parse("2020-04-01T14:00:00");
        List<SorterOrderEntity> sorterOrders = sorterOrderRepository.findActiveFromDate(fromDate);

        assertAll(
                () -> assertEquals(2, sorterOrders.size()),
                () -> assertEquals(2, sorterOrders.stream().filter(m -> !m.getStatus().isCompleted()).count()),
                () -> assertEquals(2, sorterOrders.stream().filter(m -> m.getEditDate().isAfter(fromDate)).count())
        );
    }

    @Test
    @DatabaseSetup("/sorting/repository/sorter-order/find/immutable.xml")
    @ExpectedDatabase(value = "/sorting/repository/sorter-order/find/immutable.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void findLastSorterOrderWhenItIsNotCompleted() {
        SorterOrderEntity expectedSorterOrder = SorterOrderEntity.builder()
                .id(3L)
                .packStationId(PackStationId.of("SRT-NOK-01"))
                .sorterExitId(SorterExitId.of("SRT-01-01"))
                .actualLocationId(LocationId.of("S_TRANSIT"))
                .boxId(BoxId.of("P00035"))
                .status(SorterOrderStatus.IN_PROGRESS)
                .addWho("TEST")
                .editWho("TEST")
                .addDate(LocalDateTime.parse("2020-04-01T14:10:56.789"))
                .editDate(LocalDateTime.parse("2020-04-01T14:10:56.789"))
                .rowVersion(2)
                .assignee("conveyor")
                .build();

        Optional<SorterOrderEntity> sorterOrder = sorterOrderRepository.findLastByBoxId("P00035");

        Assertions.assertAll(
                () -> Assertions.assertTrue(sorterOrder.isPresent()),
                () -> Assertions.assertEquals(expectedSorterOrder, sorterOrder.orElseThrow())
        );
    }

    @Test
    @DatabaseSetup("/sorting/repository/sorter-order/find/immutable.xml")
    @ExpectedDatabase(value = "/sorting/repository/sorter-order/find/immutable.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void findLastSorterOrderWhenItIsCompleted() {
        SorterOrderEntity expectedSorterOrder = SorterOrderEntity.builder()
                .id(5L)
                .packStationId(PackStationId.of("SRT-NOK-01"))
                .sorterExitId(SorterExitId.of("SRT-01-04"))
                .actualLocationId(LocationId.of("SRT-01-04"))
                .boxId(BoxId.of("P00036"))
                .status(SorterOrderStatus.FINISHED)
                .addWho("TEST")
                .editWho("TEST")
                .addDate(LocalDateTime.parse("2020-04-01T14:25:56.789"))
                .editDate(LocalDateTime.parse("2020-04-01T14:25:56.789"))
                .rowVersion(3)
                .assignee("conveyor")
                .build();

        Optional<SorterOrderEntity> sorterOrder = sorterOrderRepository.findLastByBoxId("P00036");

        Assertions.assertAll(
                () -> Assertions.assertTrue(sorterOrder.isPresent()),
                () -> Assertions.assertEquals(expectedSorterOrder, sorterOrder.orElseThrow())
        );
    }

    @Test
    @DatabaseSetup("/sorting/repository/sorter-order/find/immutable.xml")
    @ExpectedDatabase(value = "/sorting/repository/sorter-order/find/immutable.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void findLastSorterOrderWhenNoSorterOrdersForBox() {
        Optional<SorterOrderEntity> sorterOrder = sorterOrderRepository.findLastByBoxId("P00037");

        Assertions.assertTrue(sorterOrder.isEmpty());
    }

    @Test
    @DatabaseSetup("/sorting/repository/sorter-order/find/immutable.xml")
    @ExpectedDatabase(value = "/sorting/repository/sorter-order/find/immutable.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void findNotCompletedSorterOrdersWhenTheyExistForBox() {
        List<SorterOrderEntity> expectedSorterOrders = List.of(
                SorterOrderEntity.builder()
                        .id(3L)
                        .packStationId(PackStationId.of("SRT-NOK-01"))
                        .sorterExitId(SorterExitId.of("SRT-01-01"))
                        .actualLocationId(LocationId.of("S_TRANSIT"))
                        .boxId(BoxId.of("P00035"))
                        .status(SorterOrderStatus.IN_PROGRESS)
                        .addWho("TEST")
                        .editWho("TEST")
                        .addDate(LocalDateTime.parse("2020-04-01T14:10:56.789"))
                        .editDate(LocalDateTime.parse("2020-04-01T14:10:56.789"))
                        .rowVersion(2)
                        .assignee("conveyor")
                        .build()
        );

        List<SorterOrderEntity> sorterOrders = sorterOrderRepository.findNotCompletedByBoxId("P00035");

        Assertions.assertEquals(expectedSorterOrders, sorterOrders);
    }

    @Test
    @DatabaseSetup("/sorting/repository/sorter-order/find/immutable.xml")
    @ExpectedDatabase(value = "/sorting/repository/sorter-order/find/immutable.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void findNotCompletedSorterOrdersWhenTheyNotExistForBox() {
        List<SorterOrderEntity> sorterOrders = sorterOrderRepository.findNotCompletedByBoxId("P00036");

        Assertions.assertTrue(sorterOrders.isEmpty());
    }

    @Test
    @DatabaseSetup("/sorting/repository/sorter-order/find/immutable.xml")
    @ExpectedDatabase(value = "/sorting/repository/sorter-order/find/immutable.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void findNotCompletedSorterOrdersWhenNoSorterOrdersForBox() {
        List<SorterOrderEntity> sorterOrders = sorterOrderRepository.findNotCompletedByBoxId("P00037");

        Assertions.assertTrue(sorterOrders.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("isActualLocationInFactLocationTestArgs")
    @DatabaseSetup("/sorting/repository/sorter-order/update/init-state.xml")
    @ExpectedDatabase(
            value = "/sorting/repository/sorter-order/update/init-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void isActualLocationInFactLocationTest(BoxId boxId, List<String> locations, boolean expectedResult) {
        boolean actualLocationInFactLocation = sorterOrderRepository.isActualLocationInFactLocation(boxId, locations);

        Assertions.assertEquals(expectedResult, actualLocationInFactLocation);

    }

    private static Stream<Arguments> isActualLocationInFactLocationTestArgs() {
        return Stream.of(
                Arguments.of(
                        BoxId.of("P00035"),
                        Collections.singletonList("UPACK-01"),
                        true
                ),
                Arguments.of(
                        BoxId.of("P00035"),
                        Collections.singletonList("UPACK-02"),
                        false
                )
        );
    }
}
