package ru.yandex.market.delivery.rupostintegrationapp.functional.cancelorder;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import utils.FixtureRepository;

import ru.yandex.market.delivery.rupostintegrationapp.BaseContextualTest;
import ru.yandex.market.delivery.russianpostapiclient.bean.deleteordersfrombatch.DeleteOrdersFromBatchResponse;
import ru.yandex.market.delivery.russianpostapiclient.bean.getinfoaboutordersinbatch.Shipment;
import ru.yandex.market.delivery.russianpostapiclient.client.RussianPostApiClient;
import ru.yandex.market.delivery.russianpostapiclient.processor.ApiMethodProcessingException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static utils.FixtureRepository.getCancelOrderSuccessResponse;

class OrderCancellingByBarcodeTest extends BaseContextualTest {
    private static final String BARCODE = "EF000011935RU";

    @MockBean
    private RussianPostApiClient client;

    @BeforeEach
    void initMock() throws ApiMethodProcessingException {
        Shipment shipment = new Shipment();
        shipment.setId(123);
        shipment.setBarcode(BARCODE);
        Mockito.when(client.getShipmentByBarcode(Mockito.anyString())).thenReturn(shipment);

        DeleteOrdersFromBatchResponse response = new DeleteOrdersFromBatchResponse();
        ArrayList<Integer> resultIds = new ArrayList<>();
        resultIds.add(123);
        response.setResultIds(resultIds);

        Mockito.when(client.deleteOrderFromBatch(Mockito.anyInt())).thenReturn(response);

        Mockito.when(client.searchShipmentById(Mockito.matches(String.valueOf(shipment.getId()))))
            .thenThrow(new ApiMethodProcessingException());
    }

    @Test
    void testPositiveCancelOrder() throws Exception {
        mockMvc.perform(post(
            "/ds/cancelOrder"
        )
            .contentType(MediaType.APPLICATION_XML)
            .content(FixtureRepository.getCancelOrderRequest("", BARCODE)))
            .andExpect(status().isOk())
            .andExpect(content().xml(getCancelOrderSuccessResponse()));
    }
}
