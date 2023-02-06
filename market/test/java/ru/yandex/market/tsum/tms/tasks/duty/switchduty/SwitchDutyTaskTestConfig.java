package ru.yandex.market.tsum.tms.tasks.duty.switchduty;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.commune.bazinga.scheduler.ExecutionContext;
import ru.yandex.market.tsum.clients.juggler.JugglerApiClient;
import ru.yandex.market.tsum.core.duty.DutyManager;
import ru.yandex.market.tsum.tms.service.CronTaskDutySwitcher;
import ru.yandex.market.tsum.tms.service.StaffGroupNotifier;
import ru.yandex.market.tsum.tms.tasks.duty.shuffle_escalation_logins.ShuffleDeliveryNskPhoneEscalation;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;

@ComponentScan(basePackageClasses = SwitchDutyCronTask.class)
@Configuration
public class SwitchDutyTaskTestConfig {

    @Bean
    public StaffGroupNotifier staffGroupNotifier() {
        return Mockito.mock(StaffGroupNotifier.class);
    }

    @Bean
    public CronTaskDutySwitcher cronTaskDutySwitcher() {
        return Mockito.mock(CronTaskDutySwitcher.class);
    }

    @Bean
    public DutyManager dutyManager() {
        return Mockito.mock(DutyManager.class);
    }

    @Bean
    public JugglerApiClient jugglerApiClient() {
        return Mockito.mock(JugglerApiClient.class);
    }

    @Bean
    public ShuffleDeliveryNskPhoneEscalation shuffleDeliveryNskPhoneEscalation() {
        return new ShuffleDeliveryNskPhoneEscalation();
    }

    @Bean
    public ExecutionContext executionContext() {
        return Mockito.mock(ExecutionContext.class);
    }

    @Bean
    public Issues issues() {
        Issues issues = Mockito.mock(Issues.class);
        Mockito.when(issues.find(Mockito.anyString(), Mockito.any(ListF.class))).thenReturn(Cf.emptyIterator());
        Mockito.when(issues.create(Mockito.any(IssueCreate.class))).thenReturn(Mockito.mock(Issue.class));
        return issues;
    }
}
