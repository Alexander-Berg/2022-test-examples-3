package ru.yandex.market.delivery.transport_manager.service.external.ffwf;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.ff.client.dto.CargoUnitCountRequestDTO;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class FfwfRegisterCargoUnitCountServiceTest extends AbstractContextualTest {
    @Autowired
    private FfwfRegisterCargoUnitCountService cargoUnitCountService;

    @Autowired
    private FulfillmentWorkflowClientApi ffwfClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(ffwfClient);
    }

    @DatabaseSetup({
        "/repository/transportation/xdoc_to_dc_transportations.xml",
    })
    @Test
    void submitCargoUnitCount1p() {
        cargoUnitCountService.submitCargoUnitCount(21L, 31L);
    }

    @DatabaseSetup({
        "/repository/transportation/xdoc_to_dc_transportations.xml",
    })
    @Test
    void submitCargoUnitCountPallets3p() {
        cargoUnitCountService.submitCargoUnitCount(23L, 33L);

        CargoUnitCountRequestDTO counts = new CargoUnitCountRequestDTO();
        counts.setPallets(1);
        counts.setBoxes(0);

        verify(ffwfClient).pushCargoUnitCount(eq(5L), eq(counts));
    }

    @DatabaseSetup({
        "/repository/transportation/xdoc_to_dc_transportations.xml",
    })
    @Test
    void submitCargoUnitCountBoxes3p() {
        cargoUnitCountService.submitCargoUnitCount(22L, 32L);

        CargoUnitCountRequestDTO counts = new CargoUnitCountRequestDTO();
        counts.setPallets(0);
        counts.setBoxes(3);

        verify(ffwfClient).pushCargoUnitCount(eq(3L), eq(counts));
    }
}
