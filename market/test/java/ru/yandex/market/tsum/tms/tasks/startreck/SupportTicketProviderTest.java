package ru.yandex.market.tsum.tms.tasks.startreck;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.model.Issue;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

@ContextConfiguration
@RunWith(SpringRunner.class)
public class SupportTicketProviderTest {

    private static final String STARTREK_URL = "https://st.yandex-team.ru";
    private static final String EXPECTED_KEY = "someKey";
    @Autowired
    SupportTicketProvider supportTicketProvider;
    @Autowired
    Issues issuesMock;
    private String EXPECTED_DESCRIPTION = "someDescription";

    @Test
    public void weightlessTickets_readFromIssues() {
        setupTicketProviderMock();
        List<SupportTicketProvider.SupportTicket> weightlessSupportTickets =
            (List<SupportTicketProvider.SupportTicket>) supportTicketProvider.getWeightlessSupportTickets();
        SupportTicketProvider.SupportTicket supportTicket = weightlessSupportTickets.get(0);

        assertEquals(1, weightlessSupportTickets.size());
        assertEquals(STARTREK_URL + "/" + EXPECTED_KEY, supportTicket.getTicketUrl());
        assertEquals(EXPECTED_DESCRIPTION, supportTicket.getTicketDescription());
    }

    @SuppressWarnings("unchecked")
    public void setupTicketProviderMock() {
        Issue issue = Mockito.mock(Issue.class);
        Mockito.when(issue.getKey())
            .thenReturn(EXPECTED_KEY);
        Mockito.when(issue.getDisplay())
            .thenReturn(EXPECTED_DESCRIPTION);

        IteratorF iterator = Mockito.mock(IteratorF.class);
        Mockito.when(iterator.stream())
            .thenReturn(Stream.of(issue));

        Mockito.when(issuesMock.find(SupportTicketProvider.WEIGHTLESS_SUPPORT_TICKETS_QUERY))
            .thenReturn(iterator);
    }

    @Configuration
    public static class Config {

        @Bean
        public Issues issues() {
            return Mockito.mock(Issues.class);
        }

        @Bean
        public SupportTicketProvider supportTicketProvider(Issues issues) {
            return new SupportTicketProvider(STARTREK_URL, issues);
        }


    }
}
