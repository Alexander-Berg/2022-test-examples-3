package ru.yandex.market.delivery.transport_manager.facade.trip;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.dto.trip_included_outbounds.DirectionAndType;
import ru.yandex.market.delivery.transport_manager.domain.entity.DistributionCenterUnitCargoType;
import ru.yandex.market.delivery.transport_manager.domain.entity.TimeSlot;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"}
)
class TripIncludedOutboundsModificationAddFacadeTest extends AbstractContextualTest {
    @Autowired
    private TripIncludedOutboundsModificationAddFacade facade;

    @DatabaseSetup({
        "/repository/trip/insert_transportation_full_example.xml",
        "/repository/trip/new_units_1_to_2.xml",
        "/repository/trip/insert_transportation_full_example_partner_methods.xml",
    })
    @ExpectedDatabase(
        value = "/repository/trip/after/insert_transportation_non_existing_direction.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/trip/after/insert_transportation_non_existing_direction_qbqueue.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    @SneakyThrows
    void addNewDirection() {
        facade.add("TMT1", List.of("TMU1001", "10003"));
    }

    @DatabaseSetup({
        "/repository/trip/insert_transportation_full_example.xml",
        "/repository/trip/new_units_1_to_2_overflow.xml",
        "/repository/trip/insert_transportation_full_example_partner_methods.xml",
    })
    @ExpectedDatabase(
        value = "/repository/trip/insert_transportation_full_example.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/route/before/empty_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    @SneakyThrows
    void addNewDirectionOverflow() {
        softly.assertThatThrownBy(() -> facade.add("TMT1", List.of("TMU1001", "10003")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Не могу добавить поставку к рейсу 1: превышена паллетовместимость машины в точке 3 (TMU3)! "
                + "Максимум 5, пытаемся положить 8.");
    }

    @DatabaseSetup({
        "/repository/trip/insert_transportation_full_example.xml",
        "/repository/trip/new_units_1_to_2.xml",
    })
    @ExpectedDatabase(
        value = "/repository/trip/after/insert_transportation_non_existing_direction.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/trip/after/insert_transportation_non_existing_direction_no_put_trip_qbqueue.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    @SneakyThrows
    void addNewDirectionWithoutPutTrip() {
        facade.add("TMT1", List.of("TMU1001", "10003"));
    }

    @DatabaseSetup({
        "/repository/trip/insert_transportation_full_example.xml",
        "/repository/trip/new_units_1_to_2_xdock.xml",
        "/repository/trip/insert_transportation_full_example_partner_methods.xml",
    })
    @ExpectedDatabase(
        value = "/repository/trip/after/insert_transportation_non_existing_direction_xdock.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/trip/after/insert_transportation_non_existing_direction_with_booking_qbqueue.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    @SneakyThrows
    void addNewDirectionWithBooking() {
        facade.add("TMT1", List.of("TMU1001", "10003"));
    }

    @DatabaseSetup({
        "/repository/trip/insert_transportation_full_example.xml",
        "/repository/trip/new_units_2_to_3.xml",
        "/repository/trip/insert_transportation_full_example_partner_methods.xml",
    })
    @ExpectedDatabase(
        value = "/repository/trip/after/insert_transportation_existing_direction.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/trip/after/insert_transportation_existing_direction_qbqueue.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    @SneakyThrows
    void addExistingDirection() {
        facade.add("TMT1", List.of("TMU1001", "10003"));
    }

    @DatabaseSetup({
        "/repository/trip/insert_transportation_full_example.xml",
        "/repository/trip/new_units_2_to_3_interwarehouse_fit.xml",
        "/repository/trip/insert_transportation_full_example_partner_methods.xml",
    })
    @ExpectedDatabase(
        value = "/repository/trip/after/add_to_existing_transportation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/route/before/empty_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    @SneakyThrows
    void addAddToExisting() {
        facade.add("TMT1", List.of("TMU1001", "10003"));
    }

    @DatabaseSetup({
        "/repository/trip/insert_transportation_full_example.xml",
        "/repository/trip/new_units_2_to_3_interwarehouse_defect.xml",
        "/repository/trip/insert_transportation_full_example_partner_methods.xml",
    })
    @ExpectedDatabase(
        value = "/repository/trip/after/insert_transportation_existing_direction_different_subtype.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/trip/after/insert_transportation_existing_direction_qbqueue.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    @SneakyThrows
    void addExistingDirectionDifferentSubtype() {
        facade.add("TMT1", List.of("TMU1001", "10003"));
    }

    @DatabaseSetup({
        "/repository/trip/insert_transportation_full_example.xml",
        "/repository/trip/new_units_2_to_4.xml",
        "/repository/trip/insert_transportation_full_example_partner_methods.xml",
    })
    @ExpectedDatabase(
        value = "/repository/trip/insert_transportation_full_example.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/trip/after/insert_transportation_to_unknown_point_failed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/route/before/empty_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    @SneakyThrows
    void addToUnknownPoint() {
        softly.assertThatThrownBy(() -> facade.add("TMT1", List.of("TMU1001", "10003")))
            .isInstanceOf(IllegalStateException.class);
    }


    @DisplayName("Получить ID существующего слота для отгрузки в точке, где уже происходит отгрузка со слотом")
    @Test
    void getCsSlotIdOutboundToOutbound() {
        assertSlotIdFound(10L, 30L, TransportationUnitType.OUTBOUND, 10011L);
    }


    @DisplayName("Получить ID существующего слота для приёмки в точке, где уже происходит приёмка со слотом")
    @Test
    void getCsSlotIdInboundToInbound() {
        assertSlotIdFound(10L, 30L, TransportationUnitType.INBOUND, 10012L);
    }

    @DisplayName(
        "Получить ID существующего слота для отгрузки в точке, где уже происходит приёмка со слотом, но нет отгрузки"
    )
    @Test
    void getCsSlotIdInboundToOutbound() {
        assertSlotIdFound(30L, 40L, TransportationUnitType.OUTBOUND, 10012L);
    }

    @DisplayName(
        "Получить ID существующего слота для приёмки в точке, где уже происходит отгрузка со слотом, но нет отгрузки"
    )
    @Test
    void getCsSlotIdOutboundToInbound() {
        assertSlotIdFound(1L, 10L, TransportationUnitType.INBOUND, 10011L);
    }

    /**
     *
     * @param logisticPointFromId откуда новое перемещение
     * @param logisticPointToId куда новое перемещение
     * @param preferredType для какой из точек ищем слот
     * @param expectedSlotId ожидаемый ID слота
     */
    private void assertSlotIdFound(
        long logisticPointFromId,
        long logisticPointToId,
        TransportationUnitType preferredType,
        long expectedSlotId
    ) {
        softly.assertThat(facade.getCsSlotId(
                List.of(
                    new Transportation()
                        .setOutboundUnit(
                            new TransportationUnit()
                                .setType(TransportationUnitType.OUTBOUND)
                                .setLogisticPointId(10L)
                                .setSelectedCalendaringServiceId(10011L)
                        )
                        .setInboundUnit(
                            new TransportationUnit()
                                .setType(TransportationUnitType.INBOUND)
                                .setLogisticPointId(30L)
                                .setBookedTimeSlot(new TimeSlot()
                                    .setCalendaringServiceId(10012L)
                                )
                        )
                ),
                new DirectionAndType(logisticPointFromId, logisticPointToId, DistributionCenterUnitCargoType.ORDER),
                preferredType
            ))
            .contains(expectedSlotId);
    }
}
