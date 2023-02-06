package ru.yandex.market.delivery.mdbapp.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import steps.orderSteps.OrderSteps;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.health.HealthManager;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class OrderTest extends MockContextualTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private HealthManager healthManager;

    @MockBean
    private CheckouterAPI checkouterAPI;

    @Captor
    private ArgumentCaptor<List<ParcelBox>> parcelBoxCaptor;

    @Before
    public void beforeTest() {
        when(healthManager.isHealthyEnough()).thenReturn(true);
        when(checkouterAPI.getOrder(1L, ClientRole.SYSTEM, null, false)).thenReturn(OrderSteps.getFilledOrder(1L));
    }

    @Test
    public void successOrderWithPlacesWithoutItemsHandler() throws Exception {

        mvc.perform(
            MockMvcRequestBuilders.post("/orders/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("data/controller/request/order-request-places.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk());
        Mockito.verify(checkouterAPI).putParcelBoxes(
            eq(1L),
            eq(12L),
            parcelBoxCaptor.capture(),
            eq(ClientRole.SYSTEM),
            Mockito.any()
        );
        assertEquals("Parcel boxes count", 1, parcelBoxCaptor.getValue().size());
        assertEquals("Parcel box items is null", null, parcelBoxCaptor.getValue().get(0).getItems());
    }

    @Test
    public void successOrderWithPlacesWithItemsHandler() throws Exception {

        mvc.perform(
            MockMvcRequestBuilders.post("/orders/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("data/controller/request/order-request-places-with-items.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk());

        Mockito.verify(checkouterAPI).putParcelBoxes(
            eq(1L),
            eq(12L),
            parcelBoxCaptor.capture(),
            eq(ClientRole.SYSTEM),
            Mockito.any()
        );

        assertEquals("Parcel boxes count", 1, parcelBoxCaptor.getValue().size());
        assertEquals("Parcel box items count", 1, parcelBoxCaptor.getValue().get(0).getItems().size());
    }

    @Test
    public void successOrderWithPlacesWithItemsHandlerInconsistent() throws Exception {

        mvc.perform(
            MockMvcRequestBuilders.post("/orders/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    extractFileContent("data/controller/request/order-request-places-with-items-inconsistent.json"))
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError())
            .andExpect(MockMvcResultMatchers.content()
                .string(extractFileContent("data/controller/response/parcel_box_items_count_validation_error.json")));
    }

    @Test
    public void successOrderWithPlacesWithItemsHandlerPartlyEmptyBoxes() throws Exception {

        mvc.perform(
            MockMvcRequestBuilders.post("/orders/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "data/controller/request/order-request-places-with-items-partly-empty-boxes.json"))
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError());
    }

    @Test
    public void failOnOrderValidation() throws Exception {

        mvc.perform(
            MockMvcRequestBuilders.post("/orders/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("data/controller/request/invalid-order-request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().is(400));
    }

    private String extractFileContent(String filePath) throws IOException {
        return IOUtils.toString(
            getSystemResourceAsStream(filePath),
            StandardCharsets.UTF_8
        );
    }
}
