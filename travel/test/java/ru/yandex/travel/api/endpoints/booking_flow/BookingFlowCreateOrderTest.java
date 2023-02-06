package ru.yandex.travel.api.endpoints.booking_flow;

import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.travel.api.services.hotels_booking_flow.HotelOrdersService;
import ru.yandex.travel.api.services.hotels_booking_flow.models.HotelOrder;
import ru.yandex.travel.commons.http.CommonHttpHeaders;
import ru.yandex.travel.testing.misc.TestResources;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class BookingFlowCreateOrderTest extends AbstractBookingFlowIntegrationTest {

    @MockBean
    private HotelOrdersService service;

    @Autowired
    private MockMvc mockMvc;

    private HotelOrder hotelOrder;

    @Before
    public void setUp() {
        hotelOrder = HotelOrder.builder().build();
    }

    private void createOrder(String name, ResultMatcher resultMatcher) throws Exception {
        when(service.createOrder(any()))
                .thenReturn(CompletableFuture.completedFuture(hotelOrder));

        String query = TestResources.readResource(
                String.format("booking_flow/create_order_%s.json", name));

        mockMvc.perform(post("/api/booking_flow/v1/create_order")
                        .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "ya-uid")
                        .header(CommonHttpHeaders.HeaderType.SESSION_KEY.getHeader(), "s-key")
                        .content(query)
                        .contentType("application/json"))
                .andExpect(resultMatcher);
    }

    @Test
    public void correctMessageCallsTheService() throws Exception {
        createOrder("correct", status().isOk());
        verify(service).createOrder(any());
    }

    @Test
    public void notGuestsListCreatesError400() throws Exception {
        createOrder("no_guests", status().is4xxClientError());
        verifyNoInteractions(service);
    }

    @Test
    public void emptyGuestsListCreatesError400() throws Exception {
        createOrder("empty_guests", status().is4xxClientError());
        verifyNoInteractions(service);
    }

    @Test
    public void oneGuestMustBeFilledTheOtherMightNotBeFilled() throws Exception {
        createOrder("2_guests_one_not_filled", status().isOk());
        verify(service).createOrder(any());
    }
}
