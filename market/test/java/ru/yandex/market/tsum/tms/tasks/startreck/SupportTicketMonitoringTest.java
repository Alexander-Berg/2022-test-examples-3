package ru.yandex.market.tsum.tms.tasks.startreck;

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;
import ru.yandex.market.monitoring.MonitoringUnit;
import ru.yandex.market.tsum.tms.tasks.startreck.SupportTicketProvider.SupportTicket;

import java.util.Collection;
import java.util.Collections;

@RunWith(SpringRunner.class)
public class SupportTicketMonitoringTest {

    @Autowired
    SupportTicketMonitoring supportTicketMonitoring;
    @Autowired
    SupportTicketProvider supportTicketProviderMock;
    @Autowired
    MonitoringUnit monitoringUnitMock;


    @Test
    public void fireMonitoring_skipMonitoringWithoutWeightlessTickets() {
        setupTicketProviderMock(Collections.emptyList());
        supportTicketMonitoring.fireMonitoringIfWeightlessSupportTicketsFound();
        Mockito.verify(monitoringUnitMock, Mockito.never())
            .warning(ArgumentMatchers.anyString());
    }

    @Test
    public void fireMonitoring_verifyMonitoringMessageContent() {
        ArgumentCaptor<String> monitoringMessageCaptor = ArgumentCaptor.forClass(String.class);
        String expectedMessage =
            "Found support tickets that requires manual weight assignment:\n" +
                "https://st.yandex-team.ru/MARKETINFRA-1\tMARKETINFRA-1 description\n" +
                "https://st.yandex-team.ru/MARKETINFRA-2\tMARKETINFRA-2 description\n";
        setupTicketProviderMock(supportTickets());

        supportTicketMonitoring.fireMonitoringIfWeightlessSupportTicketsFound();

        Mockito.verify(monitoringUnitMock, Mockito.times(1))
            .warning(monitoringMessageCaptor.capture());
        Assert.assertEquals(expectedMessage, monitoringMessageCaptor.getValue());
    }

    void setupTicketProviderMock(Collection<SupportTicket> supportTickets) {
        Mockito.when(supportTicketProviderMock.getWeightlessSupportTickets())
            .thenReturn(supportTickets);
    }

    private Collection<SupportTicket> supportTickets() {
        return ImmutableList.of(
            new SupportTicket(
                "MARKETINFRA-1",
                "MARKETINFRA-1 description",
                "https://st.yandex-team.ru"
            ),
            new SupportTicket(
                "MARKETINFRA-2",
                "MARKETINFRA-2 description",
                "https://st.yandex-team.ru"
            )
        );
    }


    @Configuration
    public static class Config {

        @Bean
        public SupportTicketProvider supportTicketProvider() {
            return Mockito.mock(SupportTicketProvider.class);
        }

        @Bean
        public MonitoringUnit weightlessSupportTicketsMonitoring() {
            return Mockito.mock(MonitoringUnit.class);
        }

        @Bean
        public SupportTicketMonitoring supportTicketValidator(SupportTicketProvider supportTicketProvider,
                                                              MonitoringUnit monitoringUnit) {
            return new SupportTicketMonitoring(supportTicketProvider, monitoringUnit);
        }
    }

}