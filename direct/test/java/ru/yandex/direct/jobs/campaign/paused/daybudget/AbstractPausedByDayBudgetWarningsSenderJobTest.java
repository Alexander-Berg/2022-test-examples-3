package ru.yandex.direct.jobs.campaign.paused.daybudget;


import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.scheduler.support.DirectShardedJob;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;

public abstract class AbstractPausedByDayBudgetWarningsSenderJobTest {
    @Autowired
    protected PausedByDayBudgetService pausedByDayBudgetService;

    @Autowired
    protected CampaignRepository campaignRepository;

    protected Map<Long, Set<PausedByDayBudgetNotificationType>> sentNotificationsById;

    @Autowired
    protected PausedByDayBudgetSenderService senderService;
    protected PausedByDayBudgetSenderService pausedByDayBudgetSenderService;

    @BeforeEach
    public void setUp() {
        sentNotificationsById = new HashMap<>();
        pausedByDayBudgetSenderService = spy(senderService);
        Mockito.doAnswer(invocation -> {
            addNotification(invocation.getArgument(1), PausedByDayBudgetNotificationType.SMS);
            return true;
        }).when(pausedByDayBudgetSenderService).sendSms(anyInt(), any());

        Mockito.doAnswer(invocation -> {
            addNotification(invocation.getArgument(0), PausedByDayBudgetNotificationType.EMAIL);
            return PausedByDayBudgetSenderService.MailSendingResult.SUCCESS;
        }).when(pausedByDayBudgetSenderService).sendMail(any(), anyBoolean());

        Mockito.doAnswer(invocation -> {
            addNotification(invocation.getArgument(0), PausedByDayBudgetNotificationType.EVENT_LOG);
            return true;
        }).when(pausedByDayBudgetSenderService).addToEventLog(any());
    }

    protected void addNotification(@NotNull Campaign campaign, PausedByDayBudgetNotificationType type) {
        if (!sentNotificationsById.containsKey(campaign.getId())) {
            sentNotificationsById.put(
                    campaign.getId(), EnumSet.noneOf(PausedByDayBudgetNotificationType.class)
            );
        }
        sentNotificationsById.get(campaign.getId()).add(type);
    }

    protected abstract DirectShardedJob initJob(int shard);

    protected void initAndExecuteJobs(@NotNull Set<Integer> shards) {
        for (int shard : shards) {
            var job = initJob(shard);
            //job.execute();
            assertThatCode(job::execute).doesNotThrowAnyException();
        }
    }

    protected void initAndExecuteJobs(ClientInfo... infos) {
        initAndExecuteJobs(Arrays.stream(infos).map(ClientInfo::getShard).collect(Collectors.toSet()));
    }


}
