package ru.yandex.market.delivery.transport_manager.service.interwarehouse.transportation_task;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.queue.task.calendaring.booking.BookingSlotProducer;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.delivery.transport_manager.service.checker.TransportationExternalInfoSaver;
import ru.yandex.market.delivery.transport_manager.service.checker.dto.EnrichedTransportation;
import ru.yandex.market.delivery.transport_manager.service.checker.validation.TransportationValidator;
import ru.yandex.market.delivery.transport_manager.service.interwarehouse.CarSelectionFacade;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

/**
 * Тестовые данные не соответствуют реальности (например, одно перемещение в двух задачах
 * на перемещение. Его нужно переработать.
 */
@DbUnitConfiguration(
    databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"},
    dataSetLoader = ReplacementDataSetLoader.class
)
@DatabaseSetup({
    "/repository/register/register.xml",
    "/repository/transportation_task/for_car_selection.xml",
})
class CarSelectionFacadeTest extends AbstractContextualTest {

    @Autowired
    private CarSelectionFacade selectionService;

    @Autowired
    private TransportationValidator transportationValidator;

    @Autowired
    private TransportationExternalInfoSaver transportationExternalInfoSaver;

    @Autowired
    private TransportationMapper transportationMapper;

    @Autowired
    private BookingSlotProducer bookingSlotProducer;

    @BeforeEach
    void init() {
        clock.setFixed(Instant.parse("2021-09-01T09:00:00.00Z"), ZoneId.of("Europe/Moscow"));
        doNothing().when(transportationExternalInfoSaver).save(any());
    }

    @Test
    @DatabaseSetup(
        value = "/repository/health/dbqueue/empty.xml",
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/car_selection_found_all.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/transportation_task/dbqueue/two_book_slots_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void allFound() {
        mockProperty(TmPropertyKey.ENABLE_INTERWAREHOUSE_CALENDARING, true);
        doReturn(List.of()).when(transportationValidator).getErrors(any(EnrichedTransportation.class));
        selectionService.selectCarsForTransportations(getTransportationIdsByTask(1L));
    }

    @Test
    @DatabaseSetup(
        value = "/repository/health/dbqueue/empty.xml",
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/car_selection_found_partially.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/transportation_task/dbqueue/one_book_slots_task_with_cancellation.xml",
        assertionMode = NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void partiallyFound() {
        mockProperty(TmPropertyKey.ENABLE_INTERWAREHOUSE_CALENDARING, true);
        doReturn(List.of()).when(transportationValidator).getErrors(any(EnrichedTransportation.class));
        selectionService.selectCarsForTransportations(getTransportationIdsByTask(2L));
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/car_selection_found_partially_failed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void failOnAllTransportationsCheckFailedOrCancelled() {
        selectionService.selectCarsForTransportations(getTransportationIdsByTask(2L));
    }

    @Test
    @DatabaseSetup(value = "/repository/transportation_task/pallets.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/car_selection_found_partially_failed_pallets.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void failWithPallets() {
        selectionService.selectCarsForTransportations(getTransportationIdsByTask(2L));
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/car_selection_found_nothing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void nothingFound() {
        selectionService.selectCarsForTransportations(getTransportationIdsByTask(3L));
    }

    @Test
    @DatabaseSetup(
        value = "/repository/health/dbqueue/empty.xml",
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @DatabaseSetup("/repository/interwarehouse/regular_xdoc.xml")
    @ExpectedDatabase(
        value = "/repository/interwarehouse/after/after_car_selection.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/interwarehouse/dbqueue/book_slot_task.xml",
        assertionMode = NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void singleRegularXdocTransportation() {
        mockProperty(TmPropertyKey.ENABLE_XDOC_TRANSPORT_CALENDARING, true);
        doReturn(List.of()).when(transportationValidator).getErrors(any(EnrichedTransportation.class));
        selectionService.selectCarsForTransportations(List.of(101L));
    }

    @Test
    @DatabaseSetup(
        value = "/repository/interwarehouse/two_transportations_with_common_route.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @DatabaseSetup(value = "/repository/transportation_task/two_registers.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/two_transportations_with_common_route.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void commonCarForTwoTransportations() {
        doReturn(List.of()).when(transportationValidator).getErrors(any(EnrichedTransportation.class));
        selectionService.selectCarsForTransportations(List.of(100L, 101L));
    }

    @Test
    @DatabaseSetup(
        "/repository/transportation_task/with_predefined_car.xml"
    )
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/car_selection_predefined.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void withPredefinedCar() {
        doReturn(List.of()).when(transportationValidator).getErrors(any(EnrichedTransportation.class));
        selectionService.selectCarsForTransportations(List.of(13L));
    }

    @Test
    @DatabaseSetup(
        "/repository/transportation/with_predefined_car_asap.xml"
    )
    @DatabaseSetup(
        value = "/repository/health/dbqueue/empty.xml",
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/transportation_task/dbqueue/put_movement_task.xml",
        assertionMode = NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void withPredefinedCarAsap() {
        mockProperty(TmPropertyKey.ENABLE_XDOC_TRANSPORT_CALENDARING, true);
        doReturn(List.of()).when(transportationValidator).getErrors(any(EnrichedTransportation.class));
        selectionService.selectCarsForTransportations(List.of(1L));
    }

    @Test
    @DatabaseSetup(
        value = "/repository/health/dbqueue/empty.xml",
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @DatabaseSetup(
            value = "/repository/interwarehouse/two_transportations_with_same_movement.xml"
    )
    @ExpectedDatabase(
            value = "/repository/transportation_task/two_transportations_with_same_movement.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/transportation_task/dbqueue/several_transportations_one_movement.xml",
        assertionMode = NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void withSeveralTransportationsForOneMovement() {
        doReturn(List.of()).when(transportationValidator).getErrors(any(EnrichedTransportation.class));
        selectionService.selectCarsForTransportations(List.of(100L, 101L));
    }

    private List<Long> getTransportationIdsByTask(Long taskId) {
        return transportationMapper.getByTransportationTaskId(taskId)
            .stream()
            .map(Transportation::getId)
            .collect(Collectors.toList());
    }
}

