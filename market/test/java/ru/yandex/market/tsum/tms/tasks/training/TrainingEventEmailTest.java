package ru.yandex.market.tsum.tms.tasks.training;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Strakhov Artem <a href="mailto:dukeartem@yandex-team.ru"></a>
 * @date 31.08.17
 */
public class TrainingEventEmailTest {

    @Test
    public void getEmailMessage() throws Exception {
        CalendarDescription calendarDescription = new CalendarDescription();
        calendarDescription.setDescriptions("test descriptions training");

        TrainingEventEmail trainingEventEmail = new TrainingEventEmail(calendarDescription);

        Assert.assertTrue(trainingEventEmail.getEmailMessage().contains("test descriptions training"));
    }

    @Test
    public void getMarkdownMessage() throws Exception {
        CalendarDescription calendarDescription = new CalendarDescription();
        calendarDescription.setDescriptions("test descriptions training");

        TrainingEventEmail trainingEventEmail = new TrainingEventEmail(calendarDescription);

        Assert.assertTrue(trainingEventEmail.getMarkdownMessage().contains("test descriptions training"));
    }

    @Test
    public void getEmailSubject() throws Exception {
        CalendarDescription calendarDescription = new CalendarDescription();
        calendarDescription.setTrainingSubject("test subject training");

        TrainingEventEmail trainingEventEmail = new TrainingEventEmail(calendarDescription);

        Assert.assertEquals("test subject training", trainingEventEmail.getEmailSubject());
    }

}