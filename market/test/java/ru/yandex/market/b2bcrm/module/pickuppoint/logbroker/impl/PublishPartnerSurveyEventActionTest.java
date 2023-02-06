package ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.impl;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.b2bcrm.module.pickuppoint.PickupPointPartnerSurveyTicket;
import ru.yandex.market.b2bcrm.module.pickuppoint.config.B2bPickupPointTests;
import ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.model.PartnerSurveyPupEvent;
import ru.yandex.market.pvz.client.crm.dto.PartnerSurveyCrmDto;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.client.crm.dto.CrmPayloadType.PARTNER_SURVEY;
import static ru.yandex.market.pvz.client.model.survey.PartnerSurveyStatus.RESOLVED;

@B2bPickupPointTests
public class PublishPartnerSurveyEventActionTest extends AbstractPublishPupEventActionTest<PartnerSurveyPupEvent> {

    public static final String PARTNER_ID = "123456";

    private PickupPointPartnerSurveyTicket ticket;

    public PublishPartnerSurveyEventActionTest() {
        super(PARTNER_SURVEY);
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        ticket = ticketTestUtils.createTicket(PickupPointPartnerSurveyTicket.FQN, Map.of(
                PickupPointPartnerSurveyTicket.PARTNER_ID, PARTNER_ID
        ));
    }

    @Test
    public void shouldPublishPartnerSurveyEventOnStatusChange() throws IOException {
        ticketTestUtils.editTicketStatus(ticket, "resolved");
        assertEvent();
    }

    @Override
    protected void assertEventValue(Object eventValue) {
        PartnerSurveyCrmDto dto = (PartnerSurveyCrmDto) eventValue;
        assertThat(dto.getTicketGid()).isEqualTo(ticket.getGid());
        assertThat(dto.getPartnerId()).isEqualTo(Long.parseLong(PARTNER_ID));
        assertThat(dto.getStatus()).isEqualTo(RESOLVED);
    }

}
