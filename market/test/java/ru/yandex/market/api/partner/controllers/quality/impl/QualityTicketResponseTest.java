package ru.yandex.market.api.partner.controllers.quality.impl;

import org.junit.Test;

import ru.yandex.market.api.partner.controllers.serialization.BaseJaxbSerializationTest;

/**
 * @author zoom
 */
public class QualityTicketResponseTest extends BaseJaxbSerializationTest {

    private static final String ORDER_ID_354 = "354";

    @Test
    public void shouldSerializeEmptyTicket() {
        QualityTicketResponse ticket = new QualityTicketResponse();
        testSerialization(ticket,
                "{\n" +
                        "  \"ticketId\": 0,\n" +
                        "  \"errorCode\": 0\n" +
                        "}",
                "<ticket \n" +
                        "        ticket-id=\"0\" \n" +
                        "        error-code=\"0\"/>");
    }

    @Test
    public void shouldSerializerOrderId() {
        QualityTicketResponse ticket = new QualityTicketResponse();
        ticket.setOrderId(ORDER_ID_354);
        testSerialization(ticket,
                "{\n" +
                        "  \"ticketId\": 0,\n" +
                        "  \"errorCode\": 0,\n" +
                        "  \"orderId\": \"354\"\n" +
                        "}",
                "<ticket\n" +
                        "        ticket-id=\"0\"\n" +
                        "        error-code=\"0\"\n" +
                        "        order-id=\"354\"/>");
    }
}
