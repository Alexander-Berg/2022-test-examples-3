package ru.yandex.market.delivery.transport_manager.service.dropoff;

import java.time.LocalDate;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.delivery.transport_manager.service.TmPropertyService;

import static org.mockito.Mockito.when;

public class DropoffReturnServiceTest extends AbstractContextualTest {
    @Autowired
    private DropoffReturnService dropoffReturnService;
    @Autowired
    private TransportationMapper mapper;
    @MockBean
    private TmPropertyService propertyService;

    @Test
    @DatabaseSetup({
        "/repository/distribution_unit_center/dropoff_returns.xml",
        "/repository/dropoff/dropoff_return_transportation_base.xml"
    })
    @ExpectedDatabase(
        value = "/repository/dropoff/after/after_dropoff_return_transportations_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testTransportationCreation() {
        dropoffReturnService.createForDate(LocalDate.of(2021, 10, 12));
    }

    @Test
    @DatabaseSetup({
        "/repository/distribution_unit_center/dropoff_returns.xml",
        "/repository/dropoff/dropoff_return_transportation_base.xml"
    })
    @ExpectedDatabase(
        value = "/repository/dropoff/after/after_dropoff_return_transportations_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testTransportationCreationDropOffFilterDisabled() {
        when(propertyService.getBoolean(TmPropertyKey.ENABLE_FILTER_DROPOFF_WITH_RETURN)).thenReturn(Boolean.FALSE);
        dropoffReturnService.createForDate(LocalDate.of(2021, 10, 12));
    }

    @Test
    @DatabaseSetup("/repository/dropoff/after/after_dropoff_return_transportations_created.xml")
    @ExpectedDatabase(
        value = "/repository/dropoff/after/after_dropoff_return_transportations_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testTransportationsAlreadyExist() {
        dropoffReturnService.createForDate(LocalDate.of(2021, 10, 12));
    }

    @Test
    @DisplayName("Создается перемещение при переключенном дропоффе на другое СЦ")
    @DatabaseSetup({
        "/repository/distribution_unit_center/dropoff_returns.xml",
        "/repository/dropoff/switched_points.xml"
    })
    @ExpectedDatabase()
    void testCreatedTransportationForSwitchedDropoff() {
        LocalDate today = LocalDate.parse("2022-05-23");
        dropoffReturnService.createForDate(today);

        // DO id=8 переключился c учетом перевозчика для ДО, а для DO id=4 нет СЦ (id=3) в графе, пропускаем
        List<Transportation> transportations =
            mapper.findUpcomingByType(TransportationType.RETURN_FROM_SC_TO_DROPOFF, today);
        softly.assertThat(transportations).hasSize(1);
        var t = transportations.get(0);

        softly.assertThat(t.getOutboundUnit().getPartnerId()).isEqualTo(7);
        softly.assertThat(t.getOutboundUnit().getLogisticPointId()).isEqualTo(7);

        softly.assertThat(t.getMovement().getPartnerId()).isEqualTo(800);

        softly.assertThat(t.getInboundUnit().getPartnerId()).isEqualTo(8);
        softly.assertThat(t.getInboundUnit().getLogisticPointId()).isEqualTo(8);
    }
}
