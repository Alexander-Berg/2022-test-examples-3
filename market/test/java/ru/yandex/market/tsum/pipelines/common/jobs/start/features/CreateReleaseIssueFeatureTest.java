package ru.yandex.market.tsum.pipelines.common.jobs.start.features;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipelines.test_data.TestVersionBuilder;
import ru.yandex.misc.test.Assert;
import ru.yandex.startrek.client.model.Version;

import static org.junit.Assert.assertNotNull;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 22.05.17
 */
public class CreateReleaseIssueFeatureTest {
    @Test
    public void renderReleaseIssueTitle() {
        String versionName = "2016.100500";
        Version version = TestVersionBuilder.aVersion().withName(versionName).build();
        String title = CreateReleaseIssueFeature.renderReleaseIssueTitle(version);

        assertNotNull(title);
        Assert.assertContains(title, versionName);

        System.out.println(title);
    }

    @Test
    public void renderReleaseIssueDescription() {
        JobContext context = Mockito.mock(JobContext.class);
        String description = CreateReleaseIssueFeature.renderReleaseIssueDescription(context);

        assertNotNull(description);
        System.out.println(description);
    }

}
