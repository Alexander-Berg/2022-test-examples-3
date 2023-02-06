package ru.yandex.travel.api.endpoints.avia_booking_flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.avia.booking.service.dto.form.CreateOrderForm;
import ru.yandex.travel.api.services.avia.legacy.AviaLegacyJsonMapper;
import ru.yandex.travel.api.services.avia.orders.AviaOrderService;
import ru.yandex.travel.api.services.avia.references.AviaGeobaseCountryService;
import ru.yandex.travel.commons.http.CommonHttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {"avia-booking.enabled=true"}
)
@ActiveProfiles(value = "test")
@AutoConfigureMockMvc
public class AviaBookingFlowControllerOrderCreationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = AviaLegacyJsonMapper.objectMapper;

    @MockBean
    private AviaOrderService orderService;

    @MockBean
    private AviaGeobaseCountryService geobaseCountryService;

    @Test
    public void ordersControllerCreateValidationError() throws Exception {
        MockHttpServletRequestBuilder rqBuilder = post("/api/avia_booking_flow/v1/orders")
                .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "0")
                .header(CommonHttpHeaders.HeaderType.SESSION_KEY.getHeader(), "0")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createIncorrectTestObject()));
        mockMvc.perform(rqBuilder)
                .andExpect(request().asyncNotStarted())
                .andExpect(status().is4xxClientError());
    }

    private CreateOrderForm createIncorrectTestObject() {
        return CreateOrderForm.builder().build();
    }

}
