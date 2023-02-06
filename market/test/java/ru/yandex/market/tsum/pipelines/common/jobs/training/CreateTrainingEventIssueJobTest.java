package ru.yandex.market.tsum.pipelines.common.jobs.training;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.pipelines.sre.resources.TrainingEvent;

/**
 * @author Strakhov Artem <a href="mailto:dukeartem@yandex-team.ru"></a>
 * @date 29.08.17
 */
public class CreateTrainingEventIssueJobTest {

    @Test
    public void createDescription() throws Exception {
        TrainingEvent trainingEvent = new TrainingEvent();
        trainingEvent.setSubject("test subject");

        Assert.assertTrue(CreateTrainingEventIssueJob.createDescription(trainingEvent).contains("test subject"));
    }

}
