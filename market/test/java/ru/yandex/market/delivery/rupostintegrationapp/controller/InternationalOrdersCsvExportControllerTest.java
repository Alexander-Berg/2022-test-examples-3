package ru.yandex.market.delivery.rupostintegrationapp.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.rupostintegrationapp.service.component.international.orders.csvexporter.InternationalOrdersCsvExporter;

@ExtendWith(MockitoExtension.class)
class InternationalOrdersCsvExportControllerTest extends BaseTest {

    @Mock
    private InternationalOrdersCsvExporter exporter;

    @InjectMocks
    private InternationalOrdersCsvExportController controller;

    @Test
    void testForceExportSuccess() {
        String response = controller.forceExport();
        Mockito.verify(exporter).exportOrders();
        softly.assertThat(response)
            .as("Asserting that the response is valid")
            .isEqualTo("Orders are successfully exported");
    }
}
