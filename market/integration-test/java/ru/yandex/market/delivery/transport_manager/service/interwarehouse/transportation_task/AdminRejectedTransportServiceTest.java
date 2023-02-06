package ru.yandex.market.delivery.transport_manager.service.interwarehouse.transportation_task;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.admin.dto.NewRejectedTransportDto;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportMetadata;
import ru.yandex.market.delivery.transport_manager.service.AdminRejectedTransportService;
import ru.yandex.market.delivery.transport_manager.service.interwarehouse.enrichment.TransportSearchService;

public class AdminRejectedTransportServiceTest extends AbstractContextualTest {
    @Autowired
    private AdminRejectedTransportService adminRejectedTransportService;

    @Autowired
    private TransportSearchService transportSearchService;

    private static final NewRejectedTransportDto DTO =
        new NewRejectedTransportDto().setDay(LocalDate.of(2021, 3, 18)).setTransportId(1L);

    @Test
    @DatabaseSetup("/repository/transportation_task/before/admin_logs_search.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/after_rejected_added.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createCorrect() {
        adminRejectedTransportService.create(DTO);
        Mockito.verify(transportSearchService, Mockito.times(0)).getTransportById(Mockito.anyLong());
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/after_rejected_added_no_transport.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createLoadTransport() {

        TransportMetadata transportMetadata = new TransportMetadata()
            .setExternalId(1L)
            .setLogisticPointFromId(1L)
            .setLogisticPointToId(2L)
            .setPartnerId(1L)
            .setPrice(1000L)
            .setPalletCount(10)
            .setDuration(Duration.ofMinutes(30));

        Mockito.doReturn(Optional.of(transportMetadata)).when(transportSearchService).getTransportById(1L);

        NewRejectedTransportDto dto =
            new NewRejectedTransportDto().setDay(LocalDate.of(2021, 3, 18)).setTransportId(1L);

        adminRejectedTransportService.create(dto);
        Mockito.verify(transportSearchService, Mockito.times(1)).getTransportById(1L);
    }

    @Test
    void createNoTransport() {
        Mockito.doReturn(Optional.empty()).when(transportSearchService).getTransportById(1L);

        softly.assertThatThrownBy(() -> adminRejectedTransportService.create(DTO));
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/before/admin_logs_search.xml")
    void createDuplicate() {
        NewRejectedTransportDto existingDto = new NewRejectedTransportDto().setDay(LocalDate.of(2021, 3,
            15)).setTransportId(1L);
        softly.assertThatThrownBy(() -> adminRejectedTransportService.create(existingDto));
    }

}
