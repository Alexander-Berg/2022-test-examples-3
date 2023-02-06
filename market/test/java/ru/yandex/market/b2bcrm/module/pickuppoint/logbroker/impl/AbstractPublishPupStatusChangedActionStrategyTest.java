package ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.impl;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.b2bcrm.module.business.process.BpStatus;
import ru.yandex.market.b2bcrm.module.pickuppoint.PreLegalPartnerBpStatusMapping;
import ru.yandex.market.b2bcrm.module.pickuppoint.PupBpStatusMapping;
import ru.yandex.market.b2bcrm.module.pickuppoint.PupEventAwareTicket;
import ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.model.PupEvent;
import ru.yandex.market.crm.lb.writer.LbWriter;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.bcp.exceptions.RequiredAttributesValidationException;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.trigger.TriggerServiceException;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.pvz.client.crm.dto.CrmPayloadType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Transactional
@ExtendWith(SpringExtension.class)
abstract class AbstractPublishPupStatusChangedActionStrategyTest<E extends PupEvent<?>> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.registerModule(new JavaTimeModule());
    }

    private final Fqn ticketFqn;

    private final Fqn bpStatusMappingFqn;

    private final CrmPayloadType payloadType;

    @Inject
    protected BcpService bcpService;

    @Inject
    protected LbWriter lbWriter;

    @Inject
    protected TicketTestUtils ticketTestUtils;

    protected Object expectedEventStatus;

    protected ArgumentCaptor<byte[]> lbWriteCaptor;

    protected PupEventAwareTicket ticket;

    private BpStatus bpStatus;

    protected AbstractPublishPupStatusChangedActionStrategyTest(
            Fqn ticketFqn,
            Fqn bpStatusMappingFqn,
            CrmPayloadType payloadType
    ) {
        this.ticketFqn = ticketFqn;
        this.bpStatusMappingFqn = bpStatusMappingFqn;
        this.payloadType = payloadType;
    }

    @BeforeEach
    public void setUp() throws Exception {
        ticket = ticketTestUtils.createTicket(ticketFqn, Map.of(
                PupEventAwareTicket.PUP_ID, 4L
        ));
        bpStatus = bcpService.create(BpStatus.FQN,
                Maps.of(BpStatus.CODE, "someCode",
                        BpStatus.TITLE, "someTitle")
        );
        lbWriteCaptor = ArgumentCaptor.forClass(byte[].class);
    }

    @AfterEach
    public void tearDown() {
        Mockito.reset(lbWriter);
    }

    @Test
    public void shouldPublishStatusChangedEvent() throws IOException {
        createBpStatusMapping(true, expectedEventStatus);
        updateTicketBpStatus();
        assertEvent();
    }

    @Test
    public void shouldNotPublishStatusChangedEvent() {
        createBpStatusMapping(false, expectedEventStatus);
        updateTicketBpStatus();
        Mockito.verify(lbWriter, Mockito.times(0)).write(Mockito.any(byte[].class));
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

    protected void updateTicketBpStatus() {
        bcpService.edit(ticket, PupEventAwareTicket.CURRENT_STATUS, bpStatus);
    }

    protected PupBpStatusMapping createBpStatusMapping(boolean publishPupEventOnTransition, Object eventStatus) {
        expectedEventStatus = eventStatus;
        return bcpService.create(bpStatusMappingFqn, Maps.of(
                PreLegalPartnerBpStatusMapping.TITLE, "someTitle",
                PreLegalPartnerBpStatusMapping.CODE, eventStatus.toString(),
                PreLegalPartnerBpStatusMapping.BP_STATUS, bpStatus,
                PreLegalPartnerBpStatusMapping.PUBLISH_PUP_EVENT_ON_TRANSITION, publishPupEventOnTransition
        ));
    }

    protected void assertCheckRequiredAttributes(String... attributes) {
        assertThatExceptionOfType(TriggerServiceException.class)
                .isThrownBy(this::updateTicketBpStatus)
                .withCauseExactlyInstanceOf(RequiredAttributesValidationException.class)
                .withMessageContainingAll(attributes);
    }
}
