package ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.impl;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.b2bcrm.module.pickuppoint.PrePickupPointBpStatusMapping;
import ru.yandex.market.b2bcrm.module.pickuppoint.PrePickupPointTicket;
import ru.yandex.market.b2bcrm.module.pickuppoint.config.B2bPickupPointTests;
import ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.model.PrePickupPointPupEvent;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.pvz.client.crm.dto.PrePickupPointCrmDto;
import ru.yandex.market.pvz.client.model.approve.PrePickupPointApproveStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.client.crm.dto.CrmPayloadType.PRE_PICKUP_POINT;
import static ru.yandex.market.pvz.client.crm.dto.PrePickupPointCrmDto.BrandingType.FULL;
import static ru.yandex.market.pvz.client.model.approve.PrePickupPointApproveStatus.LEASE_AGREEMENT_REQUIRED;

@SuppressWarnings("BigDecimalMethodWithoutRoundingCalled")
@B2bPickupPointTests
public class PublishPrePickupPointPupStatusChangedActionTest
        extends AbstractPublishPupStatusChangedActionStrategyTest<PrePickupPointPupEvent> {

    public PublishPrePickupPointPupStatusChangedActionTest() {
        super(PrePickupPointTicket.FQN, PrePickupPointBpStatusMapping.FQN, PRE_PICKUP_POINT);
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        expectedEventStatus = LEASE_AGREEMENT_REQUIRED;
    }

    @Test
    public void shouldCheckTariffsNotEmptyOnApprovedForNotBrandedPup() {
        bcpService.edit(ticket, Maps.of(
                PrePickupPointTicket.CASH_COMPENSATION, null,
                PrePickupPointTicket.CARD_COMPENSATION, null,
                PrePickupPointTicket.ORDER_TRANSMISSION_REWARD, null
        ));
        createBpStatusMapping(true, PrePickupPointApproveStatus.APPROVED);

        assertCheckRequiredAttributes(
                PrePickupPointTicket.CASH_COMPENSATION,
                PrePickupPointTicket.CARD_COMPENSATION,
                PrePickupPointTicket.ORDER_TRANSMISSION_REWARD
        );
    }

    @Test
    public void shouldPublishEventWithTariffsOnApprovedForNotBrandedPup() throws IOException {
        createBpStatusMapping(true, PrePickupPointApproveStatus.APPROVED);
        updateTicketBpStatus();
        PrePickupPointCrmDto eventDto = assertEvent().getValue();
        assertThat(eventDto.getCashCompensation()).isEqualByComparingTo("0.003");
        assertThat(eventDto.getCardCompensation()).isEqualByComparingTo("0.019");
        assertThat(eventDto.getOrderTransmissionReward().setScale(2)).isEqualByComparingTo("45.00");
    }

    @Test
    public void shouldCheckTariffsAndBrandingInfoNotEmptyOnApprovedForBrandedPup()  {
        bcpService.edit(ticket, Maps.of(
                PrePickupPointTicket.BRANDED_PUP, true,
                PrePickupPointTicket.CASH_COMPENSATION, null,
                PrePickupPointTicket.CARD_COMPENSATION, null,
                PrePickupPointTicket.ORDER_TRANSMISSION_REWARD, null
        ));
        createBpStatusMapping(true, PrePickupPointApproveStatus.APPROVED);

        assertCheckRequiredAttributes(
                PrePickupPointTicket.CASH_COMPENSATION,
                PrePickupPointTicket.CARD_COMPENSATION,
                PrePickupPointTicket.ORDER_TRANSMISSION_REWARD,
                PrePickupPointTicket.PUP_BRANDING_STATUS,
                PrePickupPointTicket.PUP_BRANDED_SINCE,
                PrePickupPointTicket.PUP_BRANDING_REGION
        );
    }

    @Test
    public void shouldPublishEventWithTariffsAndBrandingInfoOnApprovedForBrandedPup() throws IOException {
        createBpStatusMapping(true, PrePickupPointApproveStatus.APPROVED);
        PrePickupPointTicket prePickupPointTicket = bcpService.edit(ticket, Maps.of(
                PrePickupPointTicket.BRANDED_PUP, true,
                PrePickupPointTicket.PUP_BRANDING_STATUS, "fullyBranded",
                PrePickupPointTicket.PUP_BRANDED_SINCE, LocalDate.now(),
                PrePickupPointTicket.PUP_BRANDING_REGION, "moscowWithinMcad"
        ));
        updateTicketBpStatus();
        PrePickupPointCrmDto eventDto = assertEvent().getValue();
        assertThat(eventDto.getCashCompensation()).isEqualByComparingTo("0.003");
        assertThat(eventDto.getCardCompensation()).isEqualByComparingTo("0.019");
        assertThat(eventDto.getOrderTransmissionReward().setScale(2)).isEqualByComparingTo("45.00");
        assertThat(eventDto.getBrandingType()).isEqualTo(FULL);
        assertThat(eventDto.getBrandedSince()).isEqualTo(prePickupPointTicket.getPupBrandedSince());
        assertThat(eventDto.getBrandRegion()).isEqualTo("Город Москва (в пределах МКАД)");
    }

    @Override
    protected void assertEventValue(Object eventValue) {
        PrePickupPointCrmDto dto = (PrePickupPointCrmDto) eventValue;
        assertThat(dto.getId()).isEqualTo(4L);
        assertThat(dto.getStatus()).isEqualTo(expectedEventStatus);
    }
}
