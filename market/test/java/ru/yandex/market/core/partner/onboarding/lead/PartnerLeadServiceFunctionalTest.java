package ru.yandex.market.core.partner.onboarding.lead;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Функциональные тесты для {@link PartnerLeadService}.
 */
public class PartnerLeadServiceFunctionalTest extends FunctionalTest {
    @Autowired
    private PartnerLeadService partnerLeadService;

    @Test
    @DbUnitDataSet(after = "PartnerLeadServiceFunctionalTest.saveLead.after.csv")
    void saveLead() {
        var partnerLeadInfo = PartnerLeadInfo.builder()
                .setCounterId("1")
                .setYandexUid("2")
                .setFirstName("First")
                .setLastName("Last")
                .setEmail("some@mail.com")
                .setLogin("login")
                .setPhone("+79603332562")
                .setIsAgree(true)
                .setIsAdvAgree(false)
                .setAssortment("500-1000")
                .build();
        assertThat(partnerLeadService.insertLead(partnerLeadInfo)).isEqualTo(1L);
    }

    @Test
    @DbUnitDataSet(after = "PartnerLeadServiceFunctionalTest.saveLeadNullLogin.after.csv")
    void saveLeadNullLogin() {
        var partnerLeadInfo = PartnerLeadInfo.builder()
                .setCounterId("1")
                .setYandexUid("2")
                .setFirstName("First")
                .setLastName("Last")
                .setEmail("some@mail.com")
                .setPhone("+79305612238")
                .setIsAgree(false)
                .setIsAdvAgree(true)
                .setAssortment(">1000")
                .build();
        assertThat(partnerLeadService.insertLead(partnerLeadInfo)).isEqualTo(1L);
    }

    @Test
    @DbUnitDataSet(after = "PartnerLeadServiceFunctionalTest.saveLeadWithCategory.after.csv")
    void saveLeadWithCategory() {
        var partnerLeadInfo = PartnerLeadInfo.builder()
                .setCounterId("1")
                .setYandexUid("2")
                .setFirstName("Andrey")
                .setLastName("Rome")
                .setEmail("some@mail.com")
                .setLogin("testLogin")
                .setPhone("+79315612241")
                .setIsAgree(false)
                .setIsAdvAgree(true)
                .setAssortment(">1000")
                .setCategory("BOOKS")
                .build();
        assertThat(partnerLeadService.insertLead(partnerLeadInfo)).isEqualTo(1L);
    }

    @Test
    @DbUnitDataSet(before = "PartnerLeadServiceFunctionalTest.getLeadsWithoutTickets.before.csv")
    void getLeadsWithoutTickets() {
        var expected = List.of(
                PartnerLeadInfo.builder()
                        .setId(1L)
                        .setFirstName("John")
                        .setLastName("Little")
                        .setEmail("test1@mail.com")
                        .setPhone("+79305612238")
                        .setAssortment("500-1000")
                        .setLogin("login1")
                        .build(),
                PartnerLeadInfo.builder()
                        .setId(3L)
                        .setFirstName("Tony")
                        .setLastName("Huge")
                        .setEmail("test3@mail.com")
                        .setPhone("+79305612245")
                        .setAssortment("500-1000")
                        .setLogin("login3")
                        .build()
        );
        var leads = partnerLeadService.getLeadsWithoutTicket();
        assertThat(leads).usingElementComparatorIgnoringFields("createdAt", "updatedAt").hasSameElementsAs(expected);
    }

    @Test
    @DbUnitDataSet(before = "PartnerLeadServiceFunctionalTest.getLeadsWithoutTicketsOld.before.csv")
    void getLeadsWithoutTicketsOld() {
        var expected = List.of(
                PartnerLeadInfo.builder()
                        .setId(3L)
                        .setFirstName("Tony")
                        .setLastName("Huge")
                        .setEmail("test3@mail.com")
                        .setPhone("+79305612245")
                        .setAssortment("500-1000")
                        .setLogin("login3")
                        .build()
        );
        var leads = partnerLeadService.getLeadsWithoutTicket();
        assertThat(leads).usingElementComparatorIgnoringFields("createdAt", "updatedAt").hasSameElementsAs(expected);
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerLeadServiceFunctionalTest.setLeadTicket.before.csv",
            after = "PartnerLeadServiceFunctionalTest.setLeadTicket.after.csv"
    )
    void setLeadTicket() {
        partnerLeadService.setLeadTicket(1L, "ticket@236", false);
        partnerLeadService.setLeadTicket(3L, "ticket@237", true);
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerLeadServiceFunctionalTest.cleanOldLeads.before.csv",
            after = "PartnerLeadServiceFunctionalTest.cleanOldLeads.after.csv"
    )
    void cleanOldLeads() {
        partnerLeadService.cleanOldLeads();
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerLeadServiceFunctionalTest.cleanOldLeadsEnv.before.csv",
            after = "PartnerLeadServiceFunctionalTest.cleanOldLeadsEnv.after.csv"
    )
    void cleanOldLeadsEnv() {
        partnerLeadService.cleanOldLeads();
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerLeadServiceFunctionalTest.updateLeadTicket.before.csv",
            after = "PartnerLeadServiceFunctionalTest.updateLeadTicket.after.csv"
    )
    void updateLeadTicket() {
        partnerLeadService.updatePartnerLead(PartnerLeadInfo.builder()
                .setId(1L)
                .setFirstName("Andrey")
                .setLastName("Rome")
                .setEmail("some123@mail.com")
                .setLogin("testLogin")
                .setPhone("+79315612241")
                .setAssortment("<500")
                .setCategory("BOOKS")
                .setPartnerId(12L)
                .build());
    }

    @Test
    @DbUnitDataSet(before = "PartnerLeadServiceFunctionalTest.getLeadsToUpdateTicket.before.csv")
    void getLeadsToUpdateTicket() {
        var expected = List.of(
                PartnerLeadInfo.builder()
                        .setId(2L)
                        .setFirstName("First")
                        .setLastName("Last")
                        .setEmail("some@mail.com")
                        .setPhone("+79603332562")
                        .setTicketId("ticket@123")
                        .setPartnerId(234L)
                        .setAssortment("500-1000")
                        .setLogin("login")
                        .build(),
                PartnerLeadInfo.builder()
                        .setId(3L)
                        .setFirstName("Tony")
                        .setLastName("Huge")
                        .setEmail("test3@mail.com")
                        .setPhone("+79305612245")
                        .setPartnerId(123L)
                        .setTicketId("ticket@234")
                        .setPartnerId(123L)
                        .setCategory("Books")
                        .setAssortment("<500")
                        .setLogin("login")
                        .build()
        );
        var leads = partnerLeadService.getLeadsToUpdateTicket();
        assertThat(leads).usingElementComparatorIgnoringFields("createdAt", "updatedAt").hasSameElementsAs(expected);
    }

    @Test
    @DbUnitDataSet(before = "PartnerLeadServiceFunctionalTest.getLeadsWithoutTicketsAndDuplicates.before.csv")
    void getLeadsWithoutTicketsAndDuplicates() {
        var expected = List.of(
                PartnerLeadInfo.builder()
                        .setId(4L)
                        .setFirstName("Tony")
                        .setLastName("Huge")
                        .setEmail("test3@mail.com")
                        .setPhone("+79305612245")
                        .setAssortment("500-1000")
                        .setLogin("login3")
                        .build()
        );
        var leads = partnerLeadService.getLeadsWithoutTicket();
        assertThat(leads).usingElementComparatorIgnoringFields("createdAt", "updatedAt").hasSameElementsAs(expected);
    }

    @Test
    @DbUnitDataSet(before = "PartnerLeadServiceFunctionalTest.getLeadsWithoutTicketsEnvironment.before.csv")
    void getLeadsWithoutTicketsEnvironment() {
        var expected = List.of(
                PartnerLeadInfo.builder()
                        .setId(4L)
                        .setFirstName("Tom")
                        .setLastName("Moth")
                        .setEmail("test4@mail.com")
                        .setPhone("+79305612246")
                        .setAssortment("500-1000")
                        .setLogin("login4")
                        .build()
        );
        var leads = partnerLeadService.getLeadsWithoutTicket();
        assertThat(leads).usingElementComparatorIgnoringFields("createdAt", "updatedAt").hasSameElementsAs(expected);
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerLeadServiceFunctionalTest.updateLeadTicketStat.before.csv",
            after = "PartnerLeadServiceFunctionalTest.updateLeadTicketStat.after.csv"
    )
    void updateLeadTicketStat() {
        partnerLeadService.updatePartnerLead(PartnerLeadInfo.builder()
                .setId(1L)
                .setFirstName("Andrey")
                .setLastName("Rome")
                .setEmail("some123@mail.com")
                .setLogin("testLogin")
                .setPhone("+79315612241")
                .setAssortment("<500")
                .setCategory("BOOKS")
                .setPartnerId(12L)
                .build());
        partnerLeadService.updatePartnerLead(PartnerLeadInfo.builder()
                .setId(2L)
                .setFirstName("Sergei")
                .setLastName("Petrov")
                .setEmail("some125@mail.com")
                .setLogin("testLogin2")
                .setPhone("+79315612245")
                .setAssortment("<500")
                .setCategory("BOOKS")
                .setPartnerId(15L)
                .build());
        partnerLeadService.updatePartnerLead(PartnerLeadInfo.builder()
                .setId(3L)
                .setFirstName("John")
                .setLastName("Smith")
                .setEmail("smith@mail.com")
                .setLogin("smith")
                .setPhone("+79603332563")
                .setAssortment("500-1000")
                .setCategory("TOYS")
                .setPartnerId(20L)
                .build());
        partnerLeadService.updatePartnerLead(PartnerLeadInfo.builder()
                .setId(4L)
                .setFirstName("Ben")
                .setLastName("Guardian")
                .setEmail("guardian@mail.com")
                .setLogin("ben")
                .setPhone("+79603332564")
                .setAssortment(">1000")
                .setCategory("GENERAL")
                .setPartnerId(21L)
                .build());
    }

    @Test
    @DbUnitDataSet(before = "PartnerLeadServiceFunctionalTest.getPartnersToCreate.before.csv")
    void getPartnersToCreate() {
        var expected = List.of(
                PartnerLeadInfo.builder()
                        .setId(3L)
                        .setFirstName("Tony")
                        .setLastName("Huge")
                        .setEmail("test3@mail.com")
                        .setPhone("+79305612245")
                        .setAssortment("500-1000")
                        .setLogin("login3")
                        .setPartnerId(536L)
                        .build(),
                PartnerLeadInfo.builder()
                        .setId(4L)
                        .setFirstName("Tom")
                        .setLastName("Firestone")
                        .setEmail("test4@mail.com")
                        .setPhone("+79315712240")
                        .setAssortment("500-1000")
                        .setLogin("login4")
                        .setPartnerId(537L)
                        .setTicketId("ticket@124")
                        .build()
        );
        var partnersToCreate = partnerLeadService.getPartnersToCreate();
        assertThat(partnersToCreate).usingElementComparatorIgnoringFields("createdAt", "updatedAt")
                .hasSameElementsAs(expected);
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerLeadServiceFunctionalTest.setPartnerCreated.before.csv",
            after = "PartnerLeadServiceFunctionalTest.setPartnerCreated.after.csv"
    )
    void setPartnerCreated() {
        partnerLeadService.setPartnerCreated(537L);
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerLeadServiceFunctionalTest.updatePartnerOnly.before.csv",
            after = "PartnerLeadServiceFunctionalTest.updatePartnerOnly.after.csv"
    )
    void updatePartnerOnly() {
        partnerLeadService.updatePartnerLeadPartnerOnly(4L,537L);
    }
}
