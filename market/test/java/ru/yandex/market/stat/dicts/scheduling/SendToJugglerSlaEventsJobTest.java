package ru.yandex.market.stat.dicts.scheduling;

import com.codahale.metrics.MetricRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.commune.bazinga.scheduler.schedule.Schedule;
import ru.yandex.market.stat.dicts.bazinga.BazingaHelper;
import ru.yandex.market.stat.dicts.common.LoadSlaStatus;
import ru.yandex.market.stat.dicts.common.SlaDict;
import ru.yandex.market.stat.dicts.common.SlaDictionariesHolder;
import ru.yandex.market.stat.dicts.config.loaders.DictionaryLoadersHolder;
import ru.yandex.market.stat.dicts.services.JugglerEventsSender;
import ru.yandex.market.stat.dicts.services.MetadataService;
import ru.yandex.market.stat.yt.YtClusterProvider;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class SendToJugglerSlaEventsJobTest {

    @Mock
    private YtClusterProvider ytClusterProvider;

    @Mock
    private MetricRegistry metricRegistry;

    private Schedule schedule;

    @Mock
    private MetadataService metadataService;

    @Mock
    private JugglerEventsSender jugglerEventsSender;

    @Mock
    private List<DictionaryLoadersHolder> dictionaryLoadersHolders;

    @Mock
    private SlaDictionariesHolder slaCommonDictionariesHolder;

    private final String stand = "production";

    private SendToJugglerSlaEventsJob sendToJugglerSlaEventsJob;

    private LocalDate localDate;
    private LocalTime localTime;
    private LocalDateTime localDateTime;
    private LoadSlaStatus loadSlaStatus;
    private SlaDict slaDict;

    @Before
    public void setUp() {
        schedule = BazingaHelper.everyMinutes(5);
        Mockito.when(ytClusterProvider.getClusterName()).thenReturn("hahn");

        sendToJugglerSlaEventsJob = new SendToJugglerSlaEventsJob(
            ytClusterProvider,
                metricRegistry,
                schedule,
                metadataService,
                jugglerEventsSender,
                dictionaryLoadersHolders,
                slaCommonDictionariesHolder,
                stand
        );

        localDate = LocalDate.of(2020, 7, 13);
        localTime = LocalTime.of(12, 0, 0);
        localDateTime = LocalDateTime.of(localDate, localTime);

        loadSlaStatus = new LoadSlaStatus(
                "abracababraepta", localDate, LocalDateTime.of(localDate, localTime)
        );

        slaDict = new SlaDict(
                "abracababraepta",
                8,
                false
        );

    }

    @Test
    public void testGetLastLoadedTodayFine() {
        LocalDate dateToCheck = LocalDate.of(2020,7, 14);

        assertThat(sendToJugglerSlaEventsJob.getFirstLoadedToday(loadSlaStatus, dateToCheck), is(loadSlaStatus.getFirstDaytimeLoad()));
    }

    @Test
    public void testGetLastLoadedTodayBad() {
        LocalDate dateToCheck = LocalDate.of(2020,7, 15);

        assertNull(sendToJugglerSlaEventsJob.getFirstLoadedToday(loadSlaStatus, dateToCheck));
    }


    @Test
    public void testIsSlaTime() {
        LocalDateTime timeToCheck = LocalDateTime.of(
                        LocalDate.of(2020, 7, 13),
                        LocalTime.of(8,0)
        );

        assertTrue(sendToJugglerSlaEventsJob.isSlaTime(slaDict, timeToCheck));
    }

    @Test
    public void testCheckIfBreakSla() {
        assertTrue(sendToJugglerSlaEventsJob.checkIfBreakSla(slaDict, localDateTime));
    }
}
