package ru.yandex.market.delivery.transport_manager.service.transportation_unit;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;

class TransportationUnitServiceTest extends AbstractContextualTest {
    @Autowired
    TransportationUnitService transportationUnitService;

    @ExpectedDatabase(
        value = "/repository/transportation_unit/after/transportation_unit_new_with_history.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void persist() {
        transportationUnitService.persist(
            new TransportationUnit()
                .setPartnerId(2L)
                .setLogisticPointId(2L)
                .setType(TransportationUnitType.INBOUND)
                .setStatus(TransportationUnitStatus.ACCEPTED)
        );
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/before/units_get_by_partner_and_external_id.xml")
    void getByPartnerIdAndExternalId() {
        Assertions.assertThat(transportationUnitService
            .getByPartnerIdExternalIdAndType(2L, "missing", TransportationUnitType.OUTBOUND))
            .isEmpty();

        Assertions.assertThat(transportationUnitService
                .getByPartnerIdExternalIdAndType(2L, "missing", TransportationUnitType.INBOUND))
            .isEmpty();

        Assertions.assertThatThrownBy(
            () -> transportationUnitService
                .getByPartnerIdExternalIdAndType(2L, "error", TransportationUnitType.OUTBOUND)
        ).isInstanceOf(IllegalStateException.class);

        Assertions.assertThat(transportationUnitService
            .getByPartnerIdExternalIdAndType(2L, "duplicate", TransportationUnitType.INBOUND))
            .map(TransportationUnit::getId)
            .contains(2L);

        Assertions.assertThat(transportationUnitService
                .getByPartnerIdExternalIdAndType(2L, "duplicate", TransportationUnitType.OUTBOUND))
            .map(TransportationUnit::getId)
            .contains(3L);

        Assertions.assertThat(transportationUnitService
            .getByPartnerIdExternalIdAndType(2L, "unique", TransportationUnitType.INBOUND))
            .map(TransportationUnit::getId)
            .contains(1L);
    }

    @DatabaseSetup("/repository/transportation_unit/multiple_transportation_units.xml")
    @Test
    void findByLgwYandexId() {
        List<TransportationUnit> found = transportationUnitService.findByLgwYandexId(List.of(
            // ID
            "TMU1",
            // request_id
            "111",
            // Два ID одного юнита
            "777",
            "TMU2",
            // Не существующий ID
            "TMU10000",
            // Не существующий request_id
            "10000",
            // Некорректный префикс
            "TM3",
            // Нельзя распарсить
            "TMM"
        ));

        softly.assertThat(found)
            .extracting(TransportationUnit::getId)
            .containsExactlyInAnyOrder(
                1L, 2L, 4L
            );
    }
}
