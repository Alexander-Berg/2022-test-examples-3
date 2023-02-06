package ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.impl;

import java.io.IOException;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.model.PupEvent;
import ru.yandex.market.crm.lb.writer.LbWriter;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.pvz.client.crm.dto.CrmPayloadType;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractPublishPupEventActionTest<E extends PupEvent<?>> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.registerModule(new JavaTimeModule());
    }

    private final CrmPayloadType payloadType;

    @Inject
    protected BcpService bcpService;

    @Inject
    protected LbWriter lbWriter;

    @Inject
    protected TicketTestUtils ticketTestUtils;

    protected ArgumentCaptor<byte[]> lbWriteCaptor;

    protected AbstractPublishPupEventActionTest(CrmPayloadType payloadType) {
        this.payloadType = payloadType;
    }

    @BeforeEach
    public void setUp() throws Exception {
        lbWriteCaptor = ArgumentCaptor.forClass(byte[].class);
    }

    @AfterEach
    public void tearDown() {
        Mockito.reset(lbWriter);
    }

    @SuppressWarnings("unchecked")
    protected E assertEvent() throws IOException {
        Mockito.verify(lbWriter).write(lbWriteCaptor.capture());
        PupEvent<?> event = MAPPER.readValue(lbWriteCaptor.getValue(), PupEvent.class);
        assertThat(event.getEventDateTime()).isNotNull();
        assertThat(event.getType()).isEqualTo(payloadType);
        assertEventValue(event.getValue());
        return (E) event;
    }

    protected abstract void assertEventValue(Object eventValue);

}
