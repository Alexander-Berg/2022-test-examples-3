package ru.yandex.market.delivery.transport_manager.service.transportation_unit;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.ff.client.dto.ShopRequestDTO;
import ru.yandex.market.ff.client.dto.ShopRequestDTOContainer;
import ru.yandex.market.ff.client.dto.ShopRequestFilterDTO;
import ru.yandex.market.ff.client.enums.RequestStatus;

import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class TransportationUnitEnricherTest extends AbstractContextualTest {
    @Autowired
    private TransportationUnitEnricher transportationUnitEnricher;
    @Autowired
    private TransportationUnitService transportationUnitService;
    @Autowired
    private FulfillmentWorkflowClientApi fulfillmentWorkflowClient;

    @DatabaseSetup("/repository/transportation_unit/transportation_unit_with_external_id.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_unit/after/transportation_unit_with_external_id_processed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void enrichRequestId() {
        ShopRequestFilterDTO filter = new ShopRequestFilterDTO();
        filter.setRequestIds(List.of("ABC123"));
        filter.setServiceIds(List.of(3L));

        ShopRequestDTO request = new ShopRequestDTO();
        request.setId(15L);
        request.setStatus(RequestStatus.PROCESSED);

        ShopRequestDTOContainer requestDTOContainer = new ShopRequestDTOContainer();
        requestDTOContainer.addRequest(request);

        when(fulfillmentWorkflowClient.getRequests(refEq(filter))).thenReturn(requestDTOContainer);

        transportationUnitEnricher.enrichRequestId(transportationUnitService.getById(1L));

        verify(fulfillmentWorkflowClient).getRequests(refEq(filter));
        verifyNoMoreInteractions(fulfillmentWorkflowClient);
    }
}
