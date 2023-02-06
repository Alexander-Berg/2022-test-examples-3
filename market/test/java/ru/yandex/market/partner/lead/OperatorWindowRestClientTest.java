package ru.yandex.market.partner.lead;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.partner.onboarding.lead.PartnerLeadInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link OperatorWindowRestClient}.
 */
public class OperatorWindowRestClientTest {
    private PartnerLeadCommentBuilder partnerLeadCommentBuilder = mock(PartnerLeadCommentBuilder.class);
    private OperatorWindowRestClient operatorWindowRestClient = new OperatorWindowRestClient(
            "host", null, "service", partnerLeadCommentBuilder, "category", OperatorWindowApiType.OW
    );

    @Test
    void TestConvert() {
        PartnerLeadInfo partnerLeadInfo = PartnerLeadInfo.builder()
                .setCreatedAt(Instant.parse("2021-01-03T10:00:00.00Z"))
                .setLogin("login")
                .setFirstName("Sam")
                .setLastName("Smith")
                .setEmail("some@mail.com")
                .setPhone("+77777777777")
                .setSourceName("express")
                .build();
        when(partnerLeadCommentBuilder.buildComment(partnerLeadInfo)).thenReturn("comment");

        PartnerLeadTicketDTO partnerLeadTicketDTO = operatorWindowRestClient.convert(partnerLeadInfo, false);
        assertThat(partnerLeadTicketDTO)
                .extracting("title", "clientName", "clientPhone", "clientEmail", "source")
                .containsExactly("Лид январь 2021; Email: some@mail.com; Login: login", "Sam Smith", "+77777777777",
                        "some@mail.com", PartnerLeadTicketSource.EXPRESS);
    }
}
