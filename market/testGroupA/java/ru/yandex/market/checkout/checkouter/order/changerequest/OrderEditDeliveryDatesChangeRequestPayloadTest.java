package ru.yandex.market.checkout.checkouter.order.changerequest;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.DeliveryEditRequest;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.TimeInterval;
import ru.yandex.market.checkout.checkouter.util.DateUtils;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CLIENT_ROLE;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;

public class OrderEditDeliveryDatesChangeRequestPayloadTest extends AbstractWebTestBase {

    @Autowired
    private OrderServiceHelper orderServiceHelper;
    @Autowired
    private TestSerializationService testSerializationService;

    private DeliveryDates previousDeliveryDates;
    private Order order;
    private DeliveryEditRequest deliveryEditRequest;


    @BeforeEach
    public void setUp() {
        previousDeliveryDates = new DeliveryDates(
                Date.from(Instant.now().plus(2, ChronoUnit.DAYS)),
                Date.from(Instant.now().plus(2, ChronoUnit.DAYS)),
                LocalTime.of(12, 0),
                LocalTime.of(15, 0)
        );
        order = OrderProvider.getBlueOrder(o -> {
            o.setDelivery(DeliveryProvider.yandexDelivery()
                    .dates(previousDeliveryDates)
                    .build()
            );
        });

        order = orderServiceHelper.saveOrder(order);

        LocalDate fromDate = LocalDate.now().plusDays(3);
        LocalDate toDate = LocalDate.now().plusDays(3);
        TimeInterval interval = new TimeInterval();
        interval.setFromTime(LocalTime.of(17, 0));
        interval.setToTime(LocalTime.of(20, 0));
        LocalDate shipmentDate = LocalDate.now().plusDays(2);

        deliveryEditRequest = DeliveryEditRequest.newDeliveryEditRequest()
                .fromDate(fromDate)
                .toDate(toDate)
                .timeInterval(interval)
                .reason(HistoryEventReason.USER_MOVED_DELIVERY_DATES_BY_DS)
                .shipmentDate(shipmentDate)
                .build();
    }

    @Test
    @DisplayName("Payload запроса на изменение даты/времени доставки " +
            "содержит предыдущие значения даты/времени доставки")
    public void deliveryDatesChangeRequestPayloadContainsPreviousFieldsTest() {
        OrderEditRequest editRequest = new OrderEditRequest();
        editRequest.setDeliveryEditRequest(deliveryEditRequest);

        List<ChangeRequest> changeRequests = client.editOrder(
                order.getId(),
                ClientRole.SYSTEM,
                null,
                Collections.singletonList(BLUE),
                editRequest
        );
        assertNotNull(changeRequests);
        assertEquals(1, changeRequests.size());

        ChangeRequest changeRequest = changeRequests.get(0);
        assertEquals(ChangeRequestType.DELIVERY_DATES, changeRequest.getType());
        assertNotNull(changeRequest.getPayload());
        assertTrue(changeRequest.getPayload() instanceof DeliveryDatesChangeRequestPayload);


        DeliveryDatesChangeRequestPayload payload = (DeliveryDatesChangeRequestPayload) changeRequest.getPayload();
        assertEquals(HistoryEventReason.USER_MOVED_DELIVERY_DATES_BY_DS, payload.getReason());
        assertEquals(deliveryEditRequest.getFromDate(), payload.getFromDate());
        assertEquals(deliveryEditRequest.getToDate(), payload.getToDate());
        assertEquals(deliveryEditRequest.getTimeInterval(), payload.getTimeInterval());
        assertEquals(DateUtils.dateToLocalDate(previousDeliveryDates.getFromDate(), getClock()),
                payload.getPreviousFromDate());
        assertEquals(DateUtils.dateToLocalDate(previousDeliveryDates.getToDate(), getClock()),
                payload.getPreviousToDate());
        assertEquals(previousDeliveryDates.getFromTime(), payload.getPreviousTimeInterval().getFromTime());
        assertEquals(previousDeliveryDates.getToTime(), payload.getPreviousTimeInterval().getToTime());
    }

    @Test
    @DisplayName("Payload запроса на изменение даты/времени доставки содержит даты в верном формате")
    public void deliveryDatesChangeRequestPayloadDatesFormatTest() throws Exception {
        OrderEditRequest editRequest = new OrderEditRequest();
        editRequest.setDeliveryEditRequest(deliveryEditRequest);

        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/orders/{orderId}/edit", order.getId())
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CLIENT_ROLE, ClientRole.SYSTEM.name())
                .contentType(MediaType.APPLICATION_JSON)
                .content(testSerializationService.serializeCheckouterObject(editRequest));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[*]", hasSize(1)))
                .andExpect(jsonPath("$.[0].payload.fromDate").value(
                        Matchers.equalTo(deliveryEditRequest.getFromDate().format(formatter))))
                .andExpect(jsonPath("$.[0].payload.toDate").value(
                        Matchers.equalTo(deliveryEditRequest.getToDate().format(formatter))))
                .andExpect(jsonPath("$.[0].payload.prevFromDate").value(
                        Matchers.equalTo(df.format(order.getDelivery().getDeliveryDates().getFromDate()))))
                .andExpect(jsonPath("$.[0].payload.prevToDate").value(
                        Matchers.equalTo(df.format(order.getDelivery().getDeliveryDates().getToDate()))));
    }
}
