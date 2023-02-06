package ru.yandex.market.sre.services.tms.tasks.startrek;

import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.bolts.collection.Option;
import ru.yandex.market.sre.services.tms.tasks.startrek.model.UserRefExt;
import ru.yandex.startrek.client.model.Issue;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MonitoringAttachingCronTaskTest extends StartekTestPreset {
    private final String INSERT_TAG = "==**Хронология событий**";
    private final String BLOCK_TAG = "==**Влияние на деньги**";
    private final String GRAPHICS_CUT_TAG = "<{Графики [создано автоматически]";
    @InjectMocks
    protected MonitoringAttachingCronTask monitoringAttachingCronTask;

    @Test
    public void prepareFinalText() {
        String issueText = "MyText\n==**Хронология событий**\nMyText";
        String monitoringBlock = "<block>";
        String expected = "MyText\n" + BLOCK_TAG + "\n<block>\n==**Хронология событий**\nMyText";
        assertEquals(expected, monitoringAttachingCronTask.prepareFinalDescription(issueText, monitoringBlock));
    }

    private Issue prepareIssue(String summary) {
        Issue issue = mock(Issue.class);
        when (issue.getSummary()).thenReturn(summary);
        return issue;
    }

    @Test
    public void prepareFinalText_emptyText() {
        String issueText = "";
        String monitoringBlock = "<block>";
        String expected = BLOCK_TAG + "\n<block>";
        assertEquals(expected, monitoringAttachingCronTask.prepareFinalDescription(issueText, monitoringBlock));
    }

    @Test
    public void prepareFinalText_hasBlock() {
        String issueText = "MyText\n" + BLOCK_TAG + "\n<old_block>\n==**Хронология событий**\nMyText";
        String monitoringBlock = "<block>";
        String expected = "MyText\n" + BLOCK_TAG + "\n<block>\n<old_block>\n==**Хронология событий**\nMyText";
        assertEquals(expected, monitoringAttachingCronTask.prepareFinalDescription(issueText, monitoringBlock));
    }


    @Test
    public void prepareFinalText_noTag() {
        String issueText = "MyText\n==**что-то**\nMyText";
        String monitoringBlock = "<block>";
        String expected = "MyText\n==**что-то**\nMyText\n" + BLOCK_TAG + "\n<block>";
        assertEquals(expected, monitoringAttachingCronTask.prepareFinalDescription(issueText, monitoringBlock));
    }

    @Test
    public void prepareFinalText_hasGraphic() {
        String issueText = "MyText\n" + BLOCK_TAG + "\n" + GRAPHICS_CUT_TAG + "\n<old_block>\n}>\n==**Хронология " +
                "событий**\nMyText";
        String monitoringBlock = GRAPHICS_CUT_TAG + "\n<block>";
        String expected = "MyText\n" + BLOCK_TAG + "\n" + GRAPHICS_CUT_TAG + "\n<block>\n==**Хронология " +
                "событий**\nMyText";
        assertEquals(expected, monitoringAttachingCronTask.prepareFinalDescription(issueText, monitoringBlock));
    }

    @Test
    public void incidentTime() {
        Issue issue = prepareIssue("[ 2020-03-18 9:00 ] Рост ошибок EXTREQUEST_OMM_REQUE");
        DateTime time = monitoringAttachingCronTask.incidentTime(issue);
        assertEquals(2020, time.getYear());
        assertEquals(3, time.getMonthOfYear());
        assertEquals(18, time.getDayOfMonth());
        assertEquals(9, time.getHourOfDay());
        assertEquals(0, time.getMinuteOfHour());

        issue = prepareIssue("[ 2020-03-18 09:01 ] Рост ошибок EXTREQUEST_OMM_REQUE");
        time = monitoringAttachingCronTask.incidentTime(issue);
        assertEquals(2020, time.getYear());
        assertEquals(3, time.getMonthOfYear());
        assertEquals(18, time.getDayOfMonth());
        assertEquals(9, time.getHourOfDay());
        assertEquals(1, time.getMinuteOfHour());

        issue = prepareIssue("[ 2020-03-18 09:1 ] Рост ошибок EXTREQUEST_OMM_REQUE");
        time = monitoringAttachingCronTask.incidentTime(issue);
        assertEquals(2020, time.getYear());
        assertEquals(3, time.getMonthOfYear());
        assertEquals(18, time.getDayOfMonth());
        assertEquals(9, time.getHourOfDay());
        assertEquals(1, time.getMinuteOfHour());

        issue = prepareIssue("[ 5020-3-08 09:10 ] Рост ошибок EXTREQUEST_OMM_REQUE");
        time = monitoringAttachingCronTask.incidentTime(issue);
        assertEquals(5020, time.getYear());
        assertEquals(3, time.getMonthOfYear());
        assertEquals(8, time.getDayOfMonth());
        assertEquals(9, time.getHourOfDay());
        assertEquals(10, time.getMinuteOfHour());

        issue = prepareIssue("[ 5020-03-08 22:59 ] Рост ошибок EXTREQUEST_OMM_REQUE");
        time = monitoringAttachingCronTask.incidentTime(issue);
        assertEquals(5020, time.getYear());
        assertEquals(3, time.getMonthOfYear());
        assertEquals(8, time.getDayOfMonth());
        assertEquals(22, time.getHourOfDay());
        assertEquals(59, time.getMinuteOfHour());

        issue = prepareIssue("[ 2020-03-11 11:40 – 12:00 ] Большой процент неответов ПП");
        time = monitoringAttachingCronTask.incidentTime(issue);
        assertEquals(2020, time.getYear());
        assertEquals(3, time.getMonthOfYear());
        assertEquals(11, time.getDayOfMonth());
        assertEquals(11, time.getHourOfDay());
        assertEquals(40, time.getMinuteOfHour());

        issue = prepareIssue("[ 2020-01-29 28-29 января ] Растут тайминги синего репорта в place=prime");
        time = monitoringAttachingCronTask.incidentTime(issue);
        assertEquals(2020, time.getYear());
        assertEquals(1, time.getMonthOfYear());
        assertEquals(29, time.getDayOfMonth());
        assertEquals(0, time.getHourOfDay());
        assertEquals(0, time.getMinuteOfHour());

        issue = prepareIssue("[ 2020-02-11 ночью ] Странное с Футболками и топами");
        time = monitoringAttachingCronTask.incidentTime(issue);
        assertEquals(2020, time.getYear());
        assertEquals(2, time.getMonthOfYear());
        assertEquals(11, time.getDayOfMonth());
        assertEquals(0, time.getHourOfDay());
        assertEquals(0, time.getMinuteOfHour());
    }
}
