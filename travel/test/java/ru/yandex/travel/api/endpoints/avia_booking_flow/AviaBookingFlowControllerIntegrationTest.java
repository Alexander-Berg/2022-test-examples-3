package ru.yandex.travel.api.endpoints.avia_booking_flow;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
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

import ru.yandex.avia.booking.service.dto.OrderDTO;
import ru.yandex.avia.booking.service.dto.form.CreateOrderForm;
import ru.yandex.travel.api.infrastucture.ApiTokenEncrypter;
import ru.yandex.travel.api.services.avia.fares.AviaFareFamilyService;
import ru.yandex.travel.api.services.avia.orders.AviaOrchestratorClientAdapter;
import ru.yandex.travel.api.services.avia.references.AviaGeobaseCountryService;
import ru.yandex.travel.api.services.dictionaries.avia.AviaAirlineDictionary;
import ru.yandex.travel.api.services.dictionaries.avia.AviaAirportDictionary;
import ru.yandex.travel.api.services.dictionaries.avia.AviaSettlementDictionary;
import ru.yandex.travel.commons.http.CommonHttpHeaders;
import ru.yandex.travel.commons.jackson.MoneySerializersModule;
import ru.yandex.travel.dicts.avia.TAirline;
import ru.yandex.travel.dicts.avia.TAirport;
import ru.yandex.travel.dicts.avia.TSettlement;
import ru.yandex.travel.orders.commons.proto.EAviaCheckAvailabilityOnRedirOutcome;
import ru.yandex.travel.orders.commons.proto.EAviaCheckAvailabilityOutcome;
import ru.yandex.travel.orders.commons.proto.EAviaConfirmationOutcome;
import ru.yandex.travel.orders.commons.proto.EAviaMqEventOutcome;
import ru.yandex.travel.orders.commons.proto.TAviaTestContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "avia-booking.new-orders-enabled=true",
                "test-context.enabled=true",
                "encryption.encryption-key=1234876asdbxcvlkjshd"
        }
)
@ActiveProfiles(value = "test")
@AutoConfigureMockMvc
public class AviaBookingFlowControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApiTokenEncrypter tokenEncrypter;

    @Autowired
    private AviaFareFamilyService fareFamilyService;

    @MockBean
    private AviaAirportDictionary aviaAirportDictionary;
    @MockBean
    private AviaAirlineDictionary aviaAirlineDictionary;
    @MockBean
    private AviaSettlementDictionary aviaSettlementDictionary;
    @MockBean
    private AviaGeobaseCountryService geobaseCountryService;
    @MockBean
    private AviaOrchestratorClientAdapter orchestratorClientAdapter;

    @Test
    public void testForwardTestContextFromCheck() throws Exception {
        // Defining global vars
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new MoneySerializersModule());
        TAviaTestContext testContext = TAviaTestContext.newBuilder()
                .setCheckAvailabilityOnRedirOutcome(EAviaCheckAvailabilityOnRedirOutcome.CAOR_SUCCESS)
                .setCheckAvailabilityOutcome(EAviaCheckAvailabilityOutcome.CAO_PRICE_CHANGED)
                .setMqEventOutcome(EAviaMqEventOutcome.MEO_SUCCESS)
                .setConfirmationOutcome(EAviaConfirmationOutcome.CO_SUCCESS)
                .build();
        String testContextToken = tokenEncrypter.toAviaTestContextToken(testContext);

        // Defining mocks and vars for CHECK_AVAILABILITY call
        TAirport airport = TAirport.newBuilder()
                .setSettlementId(1)
                .setId(1)
                .setCode("1")
                .setTimeZoneId("1")
                .setTitle("mockedAirport")
                .setSettlementId(1)
                .build();
        when(aviaAirportDictionary.getById(any())).thenReturn(airport);
        when(aviaAirportDictionary.getByIataCode(any())).thenReturn(airport);
        when(aviaSettlementDictionary.getById(any())).thenReturn(TSettlement.newBuilder().build());
        when(aviaAirlineDictionary.getById(any())).thenReturn(TAirline.newBuilder().setId(1).build());
        when(aviaAirlineDictionary.getByIataCode(any())).thenReturn(TAirline.newBuilder().setId(1).build());
        when(geobaseCountryService.getIsoName(any())).thenReturn("MockedISOName");
        JsonNode jsonNode = createJsonNode();
        ((ObjectNode) jsonNode).remove("variantTestContext");
        ((ObjectNode) jsonNode).put("variantTestContext", testContextToken);
        JsonNode bookingInfo = jsonNode.at("/order_data/booking_info");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String today = LocalDateTime.now().plus(3, ChronoUnit.DAYS).format(dateTimeFormatter);
        String airShoppingRS = bookingInfo.get("AirShoppingRS").asText()
                .replaceAll("2020-09-09", today)
                .replaceAll("2020-09-23", today);
        ((ObjectNode) bookingInfo).put("AirShoppingRS", airShoppingRS);
        String data = objectMapper.writeValueAsString(jsonNode);

        // Do CHECK_AVAILABILITY call
        MockHttpServletRequestBuilder rqBuilder = post("/api/avia_booking_flow/v1/variants/availability_check")
                .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "yaUid")
                .header(CommonHttpHeaders.HeaderType.SESSION_KEY.getHeader(), "sKey")
                .content(data)
                .contentType(MediaType.APPLICATION_JSON);
        MvcResult asyncResult = mockMvc.perform(rqBuilder)
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk());

        // Defining mocks and vars for ORDERS call
        when(orchestratorClientAdapter.createOrder(any(), any(), any(), ArgumentMatchers.eq(testContext))) // check that testContext was saved
                .thenReturn(CompletableFuture.completedFuture(new OrderDTO()));
        String token = objectMapper.readTree(asyncResult.getAsyncResult().toString()).get("redirectUrl").textValue().substring(16);
        CreateOrderForm orderForm = AviaCreateOrderDataUtils.createTestObject();
        orderForm.setVariantToken(token);

        // Do ORDERS call
        MockHttpServletRequestBuilder rqBuilder2 = post("/api/avia_booking_flow/v1/orders")
                .header(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), "yaUid")
                .header(CommonHttpHeaders.HeaderType.SESSION_KEY.getHeader(), "sKey")
                .content(objectMapper.writeValueAsString(orderForm))
                .contentType(MediaType.APPLICATION_JSON);
        MvcResult asyncResult2 = mockMvc.perform(rqBuilder2)
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(asyncResult2))
                .andExpect(status().isOk());
    }

    private JsonNode createJsonNode() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("avia/ticket_daemon/booking_with_test_context.json");
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(inputStream);
    }
}
