package ru.yandex.market.b2bcrm.module.ticket.test;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.b2bcrm.module.ticket.B2bLeadTicket;
import ru.yandex.market.b2bcrm.module.ticket.test.config.B2bTicketTests;
import ru.yandex.market.b2bcrm.module.ticket.test.utils.B2bTicketTestUtils;
import ru.yandex.market.jmf.attributes.hyperlink.Hyperlink;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.metadata.Fqn;

import static org.assertj.core.api.Assertions.assertThat;

@B2bTicketTests
public class B2bLeadTicketSiteAssignmentTest {

    private final static String DEFAULT_HREF = "https://test_case.yandex-team.ru";
    private final static String DEFAULT_REPLACEMENT = "https://my-test.ru";
    @Inject
    private B2bTicketTestUtils b2bTicketTestUtils;
    @Inject
    private BcpService bcpService;

    @Test
    @DisplayName("Проверка заполнения сайта из партнера при создании")
    public void b2bLeadTicketSetSiteOnCreate() {
        Entity partner = getDefaultPartner();
        B2bLeadTicket ticket = b2bTicketTestUtils.createB2bLead(
                Map.of(
                        B2bLeadTicket.PARTNER,
                        partner
                )
        );

        assertThat(ticket.getSite()).isEqualTo(DEFAULT_HREF);
    }

    @Test
    @DisplayName("Проверка возможности отредактировать сайт")
    public void b2bLeadTicketEditSite() {
        Entity partner = getDefaultPartner();
        B2bLeadTicket ticket = b2bTicketTestUtils.createB2bLead(
                Map.of(
                        B2bLeadTicket.PARTNER,
                        partner
                )
        );
        bcpService.edit(ticket, B2bLeadTicket.SITE, DEFAULT_REPLACEMENT);
        assertThat(ticket.getSite()).isEqualTo(DEFAULT_REPLACEMENT);
    }

    @Test
    @DisplayName("Проверка заполнения сайта из партнера при изменении партнера")
    public void b2bLeadTicketEditPartner() {
        Entity partner = getDefaultPartner();
        B2bLeadTicket ticket = b2bTicketTestUtils.createB2bLead(Map.of());
        bcpService.edit(ticket, B2bLeadTicket.PARTNER, partner);
        assertThat(ticket.getSite()).isEqualTo(DEFAULT_HREF);
    }

    @Test
    @DisplayName("Проверка сохранения старого сайта при создании партнера")
    public void b2bLeadTicketEditPartnerExistingSite() {
        Entity partner = getDefaultPartner();
        B2bLeadTicket ticket = b2bTicketTestUtils.createB2bLead(Map.of(
                B2bLeadTicket.SITE,
                DEFAULT_REPLACEMENT
        ));
        bcpService.edit(ticket, B2bLeadTicket.PARTNER, partner);
        assertThat(ticket.getSite()).isEqualTo(DEFAULT_REPLACEMENT);
    }

    private Entity getDefaultPartner() {
        return bcpService.create(Fqn.of("account$shop"), Map.of(
                "title", "Test Shop",
                "shopId", "111111",
                "campaignId", "1000661967",
                "domain", new Hyperlink(DEFAULT_HREF)
        ));
    }
}
