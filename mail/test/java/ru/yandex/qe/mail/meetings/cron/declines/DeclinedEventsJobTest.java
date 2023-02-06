package ru.yandex.qe.mail.meetings.cron.declines;

import java.util.Date;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.JobExecutionContext;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.qe.mail.meetings.utils.DateRangeTest;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Sergey Galyamichev
 */

@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MockConfiguration.class})
public class DeclinedEventsJobTest {
    @Inject
    private DeclinedEventsJob declinedEventsJob;
    @Inject
    private JavaMailSender mailSender;

    @Before
    public void setUp() {
        reset(mailSender);
    }

    @Test
    public void execute() {
        JobExecutionContext context = mock(JobExecutionContext.class);
        Date value = DateRangeTest.toDate("2019-10-01T12:30:00");
        when(context.getFireTime()).thenReturn(value);
        declinedEventsJob.execute(context);
        verify(mailSender, times(1)).send(any(MimeMessagePreparator.class));
        reset(mailSender);
        declinedEventsJob.execute(context);
        verify(mailSender, times(0)).send(any(MimeMessagePreparator.class));
    }
}
