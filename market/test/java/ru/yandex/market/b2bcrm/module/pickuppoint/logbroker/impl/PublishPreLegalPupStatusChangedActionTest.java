package ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.impl;

import java.io.IOException;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.b2bcrm.module.pickuppoint.PickupPointPotentialTicket;
import ru.yandex.market.b2bcrm.module.pickuppoint.PreLegalPartnerBpStatusMapping;
import ru.yandex.market.b2bcrm.module.pickuppoint.config.B2bPickupPointTests;
import ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.model.PreLegalPartnerPupEvent;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.pvz.client.crm.dto.PreLegalPartnerCrmDto;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.client.crm.dto.CrmPayloadType.PRE_LEGAL_PARTNER;
import static ru.yandex.market.pvz.client.model.approve.PreLegalPartnerApproveStatus.CHECKING;
import static ru.yandex.market.pvz.client.model.approve.PreLegalPartnerApproveStatus.REJECTED;

@B2bPickupPointTests
public class PublishPreLegalPupStatusChangedActionTest extends AbstractPublishPupStatusChangedActionStrategyTest<PreLegalPartnerPupEvent> {

    @Inject
    private ConfigurationService configurationService;

    public PublishPreLegalPupStatusChangedActionTest() {
        super(PickupPointPotentialTicket.FQN, PreLegalPartnerBpStatusMapping.FQN, PRE_LEGAL_PARTNER);
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configurationService.setValue("processPreLegalPartnerAsTicket", true);
        expectedEventStatus = CHECKING;
    }

    @Test
    public void shouldPublishStatusChangedEventWithRefusalReason() throws IOException {
        bcpService.edit(
                createBpStatusMapping(true, REJECTED),
                PreLegalPartnerBpStatusMapping.REFUSAL_REASON, "Отклонен СБ"
        );
        updateTicketBpStatus();
        PreLegalPartnerCrmDto eventDto = assertEvent().getValue();
        assertThat(eventDto.getRefusalReason()).isEqualTo("Отклонен СБ");
    }

    @Override
    protected void assertEventValue(Object eventValue) {
        PreLegalPartnerCrmDto dto = (PreLegalPartnerCrmDto) eventValue;
        assertThat(dto.getId()).isEqualTo(4L);
        assertThat(dto.getApproveStatus()).isEqualTo(expectedEventStatus);
    }


}
