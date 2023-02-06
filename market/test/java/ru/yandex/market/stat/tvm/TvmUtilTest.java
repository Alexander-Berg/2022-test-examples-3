package ru.yandex.market.stat.tvm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TvmUtilTest {
    @Test
    public void parseResponse() throws IOException {
        String ticketResponse = "{\"2002380\":{\"ticket\":\"3:serv:XXXXXXXXXXXXXXXXXXXXXXXXXX:TICKETFORSERVICEA\"},\"2002382\":{\"ticket\":\"3:serv:XXXXXXXXXXXXXXXXXXXXXXXXXX:TICKETFORSERVICEB\"}}";
        TvmTickets tvmTickets = new ObjectMapper().readValue(ticketResponse, TvmTickets.class);
        assertThat(tvmTickets.getTicketForAsString(ClientId.Dst.of(2002380)), is("3:serv:XXXXXXXXXXXXXXXXXXXXXXXXXX:TICKETFORSERVICEA"));
        assertThat(tvmTickets.getTicketForAsString(ClientId.Dst.of(2002382)), is("3:serv:XXXXXXXXXXXXXXXXXXXXXXXXXX:TICKETFORSERVICEB"));
    }
}
