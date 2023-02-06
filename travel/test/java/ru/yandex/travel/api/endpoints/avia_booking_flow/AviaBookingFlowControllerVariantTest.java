package ru.yandex.travel.api.endpoints.avia_booking_flow;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.avia.booking.service.dto.VariantCheckResponseDTO;
import ru.yandex.avia.booking.service.dto.VariantCheckToken;
import ru.yandex.travel.api.services.avia.references.AviaGeobaseCountryService;
import ru.yandex.travel.api.services.avia.variants.AviaVariantNotSupportedException;
import ru.yandex.travel.api.services.avia.variants.AviaVariantService;
import ru.yandex.travel.api.services.avia.variants.AviaVariantsNotFoundException;
import ru.yandex.travel.commons.http.CommonHttpHeaders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
public class AviaBookingFlowControllerVariantTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AviaVariantService variantService;
    @MockBean
    private AviaGeobaseCountryService geobaseCountryService;

    @Test
    public void testGetVariantMethodSignature() throws Exception {
        VariantCheckToken token = new VariantCheckToken(UUID.randomUUID(), "1ADT-some-dst.id");
        when(variantService.getVariantInfoFuture(any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        MockHttpServletRequestBuilder rqBuilder = get("/api/avia_booking_flow/v1/variants")
                .param("id", token.toString())
                .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "yaUid")
                .header(CommonHttpHeaders.HeaderType.SESSION_KEY.getHeader(), "sKey")
                .contentType(MediaType.APPLICATION_JSON);
        MvcResult asyncResult = mockMvc.perform(rqBuilder)
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk());
    }

    @Test
    public void checkAvailabilityOkStatus() throws Exception {
        when(variantService.checkAvailability(any()))
                .thenReturn(CompletableFuture.completedFuture(new VariantCheckResponseDTO("Http://...", null)));
        MvcResult asyncResult = ensureAsyncResult(defaultReq("/api/avia_booking_flow/v1/variants/availability_check"));
        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk());
    }

    @Test
    public void checkAvailabilityNotFoundStatus() throws Exception {
        when(variantService.checkAvailability(any()))
                .thenReturn(CompletableFuture.failedFuture(new AviaVariantsNotFoundException("not found")));
        MvcResult asyncResult = ensureAsyncResult(defaultReq("/api/avia_booking_flow/v1/variants/availability_check"));
        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isNotFound());
    }

    @Test
    public void checkAvailabilityUnhandledErrorStatus() throws Exception {
        when(variantService.checkAvailability(any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("unhandled")));
        MvcResult asyncResult = ensureAsyncResult(defaultReq("/api/avia_booking_flow/v1/variants/availability_check"));
        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().is(400));
    }

    @Test
    public void checkAvailability_unsupportedVariant() throws Exception {
        when(variantService.checkAvailability(any()))
                .thenReturn(CompletableFuture.failedFuture(new AviaVariantNotSupportedException("test_use_deeplink")));
        MvcResult asyncResult = ensureAsyncResult(defaultReq("/api/avia_booking_flow/v1/variants/availability_check"));
        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isUnprocessableEntity());
    }

    @SuppressWarnings("SameParameterValue")
    private MockHttpServletRequestBuilder defaultReq(String address) {
        return post(address)
                .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "yaUid")
                .header(CommonHttpHeaders.HeaderType.SESSION_KEY.getHeader(), "sKey")
                .content("{}")
                .contentType(MediaType.APPLICATION_JSON);
    }

    private MvcResult ensureAsyncResult(MockHttpServletRequestBuilder rqBuilder) throws Exception {
        return mockMvc.perform(rqBuilder)
                .andExpect(request().asyncStarted())
                .andReturn();
    }
}
