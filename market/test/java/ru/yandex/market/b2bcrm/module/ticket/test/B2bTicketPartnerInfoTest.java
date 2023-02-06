package ru.yandex.market.b2bcrm.module.ticket.test;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.b2bcrm.module.ticket.B2bTicket;
import ru.yandex.market.b2bcrm.module.ticket.test.config.B2bTicketTests;
import ru.yandex.market.b2bcrm.module.ticket.test.utils.B2bTicketTestUtils;
import ru.yandex.market.b2bcrm.module.utils.AccountModuleTestUtils;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.Entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

@B2bTicketTests
public class B2bTicketPartnerInfoTest {
    @Inject
    protected AccountModuleTestUtils accountModuleTestUtils;
    @Inject
    private B2bTicketTestUtils b2bTicketTestUtils;
    @Inject
    private BcpService bcpService;

    @Test
    @DisplayName("MBI Partner ID and Business ID are set to a newly created ticket with a partner")
    public void createdWithPartnerTest() {
        B2bTicket ticket = b2bTicketTestUtils.createB2bTicket(Map.of(B2bTicket.PARTNER, getShop()));

        assertThat(ticket.getMbiPartnerId()).isEqualTo("111111");
        assertThat(ticket.getPartnerBusinessId()).isEqualTo(12345);
    }

    @Test
    @DisplayName("MBI Partner ID and Business ID are null when a ticket is created without a partner")
    public void noPartnerTest() {
        B2bTicket ticket = getDefaultTicket();

        assertNull(ticket.getMbiPartnerId());
        assertNull(ticket.getPartnerBusinessId());
    }

    @Test
    @DisplayName("MBI Partner ID and Business ID are set when a partner is added to an existing ticket")
    public void addedPartnerTest() {
        B2bTicket ticket = getDefaultTicket();

        bcpService.edit(ticket, B2bTicket.PARTNER, getShop());

        assertThat(ticket.getMbiPartnerId()).isEqualTo("111111");
        assertThat(ticket.getPartnerBusinessId()).isEqualTo(12345);
    }

    @Nonnull
    private Entity getShop() {
        return accountModuleTestUtils.createShopWithBusinessID(
                "Test",
                "111111",
                "1000661967",
                "12345"
        );
    }

    @Nonnull
    private B2bTicket getDefaultTicket() {
        return b2bTicketTestUtils.createB2bTicket(Collections.emptyMap());
    }
}
