package ru.yandex.market.b2bcrm.module.pickuppoint;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import ru.yandex.market.b2bcrm.module.pickuppoint.config.B2bPickupPointTests;
import ru.yandex.market.b2bcrm.module.utils.AccountModuleTestUtils;
import ru.yandex.market.jmf.entity.test.assertions.EntityAssert;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;

import static ru.yandex.market.b2bcrm.module.pickuppoint.PickupPointPartnerSurveyTicket.PARTNER_ID;
import static ru.yandex.market.b2bcrm.module.ticket.B2bTicket.PARTNER;

@B2bPickupPointTests
public class PickupPointPartnerSurveyTicketTest {

    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private AccountModuleTestUtils accountModuleTestUtils;

    @Test
    void shouldSetPartnerOnCreate() {
        String partnerId = "123456";
        var partner = accountModuleTestUtils.createAccount(PickupPointOwner.FQN, Map.of(
                PickupPointOwner.CAMPAIGN_ID, partnerId,
                PickupPointOwner.TITLE, "Партнер"
        ));
        var survey = ticketTestUtils.createTicket(PickupPointPartnerSurveyTicket.FQN, Map.of(
                PARTNER_ID, partnerId
        ));
        EntityAssert.assertThat(survey)
                .hasAttributes(
                        PARTNER_ID, partnerId,
                        PARTNER, partner.getGid()
                );
    }

    @Test
    void shouldNotFailWhenNoSuitablePartnerExist() {
        String partnerId = "12345";
        var survey = ticketTestUtils.createTicket(PickupPointPartnerSurveyTicket.FQN, Map.of(
                PARTNER_ID, partnerId
        ));
        EntityAssert.assertThat(survey)
                .hasAttributes(
                        PARTNER_ID, partnerId,
                        PARTNER, null
                );
    }
}
