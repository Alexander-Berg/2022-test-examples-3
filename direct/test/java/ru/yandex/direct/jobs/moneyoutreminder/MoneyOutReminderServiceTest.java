package ru.yandex.direct.jobs.moneyoutreminder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.eventlog.model.EventCampaignAndTimeData;
import ru.yandex.direct.core.entity.eventlog.model.EventLog;
import ru.yandex.direct.core.entity.eventlog.model.EventLogType;
import ru.yandex.direct.core.entity.eventlog.repository.EventLogRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestEventLogRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static ru.yandex.direct.core.entity.eventlog.model.EventLogType.MONEY_OUT_WALLET;
import static ru.yandex.direct.jobs.moneyoutreminder.MoneyOutReminderJobTestBase.createEventLogWithDefaults;
import static ru.yandex.direct.jobs.moneyoutreminder.MoneyOutReminderNotificationType.SEVEN_DAYS_OFF;

@JobsTest
@ExtendWith(SpringExtension.class)
public class MoneyOutReminderServiceTest {


    @Autowired
    private EventLogRepository eventLogRepository;
    @Autowired
    private Steps steps;
    @Autowired
    DslContextProvider provider;
    @Autowired
    private MoneyOutReminderService moneyOutReminderService;
    @Autowired
    private TestEventLogRepository testEventLogRepository;

    private ClientInfo clientInfo;

    private int shard;

    private final int progressCid = 10;

    //cid кампаний и список дней, когда для них произошло событие money_out_wallet
    public static final Map<Long, List<Integer>> CID_TO_DAYS_AGO = Map.of(
            1L, List.of(3, 5),
            2L, List.of(8),
            10L, List.of(3),
            11L, List.of(5, 7, 2),
            12L, List.of(11),
            13L, List.of(3, 4)
    );

    @BeforeEach
    void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        for (Long campaignId : CID_TO_DAYS_AGO.keySet()) {
            testEventLogRepository.deleteCampaignEvents(clientInfo.getShard(), clientInfo.getClientId(), campaignId);
        }
    }

    /**
     * Проверить, что верно отобрались события(за последнюю неделю, минимальные для данной кампании и с учётом
     * прогресса)
     */
    @Test
    void testGetEventsForMoneyOutReminder() {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDate today = currentTime.toLocalDate();
        Set<Long> campaignIds = CID_TO_DAYS_AGO.keySet();

        //для каждой кампании добавляются события и из них выбирается самое позднее
        List<EventCampaignAndTimeData> expectedEventData = StreamEx.of(campaignIds)
                .map(campaignId -> addEventsAndGetExpectedEventDataForCampaign(campaignId, currentTime))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        //cобытие другого типа
        eventLogRepository.addEventLog(
                shard,
                createEventLogWithDefaults(clientInfo, 14L, 3, currentTime, EventLogType.MONEY_IN));

        List<EventCampaignAndTimeData> result =
                moneyOutReminderService.getEventsForMoneyOutReminder(
                        clientInfo.getShard(),
                        today,
                        today.minusDays(1),
                        String.valueOf(progressCid));

        Assertions.assertThat(result)
                .as("События")
                .hasSize(expectedEventData.size())
                .containsAll(expectedEventData);
    }

    EventCampaignAndTimeData addEventsAndGetExpectedEventDataForCampaign(Long campaignId, LocalDateTime today) {
        List<EventLog> eventLogs = StreamEx.of(CID_TO_DAYS_AGO.get(campaignId))
                .map(eventDaysAgo ->
                        createEventLogWithDefaults(clientInfo, campaignId, eventDaysAgo, today, MONEY_OUT_WALLET))
                .collect(Collectors.toList());
        eventLogRepository.addEventLogs(clientInfo.getShard(), eventLogs);

        EventCampaignAndTimeData expectedEventData =
                new EventCampaignAndTimeData()
                        .fromEventLog(eventLogs
                                .stream()
                                .max(Comparator.comparing(EventLog::getEventTime)).get());

        //возвращаем только если кампания ещё не была обработана и событие произошло не раньше, чем 7 дней назад
        if (campaignId > progressCid
                && expectedEventData.getEventTime().isAfter(today.minusDays(SEVEN_DAYS_OFF.days))) {
            return expectedEventData;
        }
        return null;
    }
}
