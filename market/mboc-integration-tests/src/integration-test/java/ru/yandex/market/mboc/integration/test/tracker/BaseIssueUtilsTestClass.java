package ru.yandex.market.mboc.integration.test.tracker;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.awaitility.core.ConditionFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.mbo.tracker.utils.IssueUtils;
import ru.yandex.market.mboc.common.IntegrationTestSourcesInitializer;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.config.TrackerConfig;
import ru.yandex.startrek.client.model.Issue;

/**
 * Тесты тестируют взаимодействие с трекером.
 * Вынесены в mboc-integration-tests в качестве исключения, чтобы не запускаться на каждом ПР.
 *
 * @author s-ermakov
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
    initializers = {IntegrationTestSourcesInitializer.class},
    classes = {TrackerConfig.class}
)
public abstract class BaseIssueUtilsTestClass {

    private Issue issue;

    @After
    public void tearDown() {
        if (issue != null) {
            IssueUtils.closeIssue(issue);
        }
    }

    /**
     * Один большой тест по нескольким причинам:
     * 1) Меньше насилуем трекер своими ненужными тикетами
     * 2) Нужно проверить корректную смену статусов. Это проще делается сценарными тестами,
     * но junit4 их не поддерживает. Поэтому пишем пока так.
     */
    @Test
    public void testResolvedClosedStatuses() {
        issue = createIssue();
        MbocAssertions.assertThat(issue).isOpen();

        // переводим в статус решен
        await10Sec().until(() -> {
            try {
                Assert.assertTrue(IssueUtils.resolveIssue(issue));
                return true;
            } catch (Exception e) {
                return false;
            }
        });

        await10Sec().untilAsserted(() -> {
            issue = getIssue(issue);
            MbocAssertions.assertThat(issue).isResolved();
        });

        // переводим в статус закрыт
        await10Sec().ignoreExceptions().untilAsserted(() -> Assert.assertTrue(IssueUtils.closeIssue(issue)));

        await10Sec().untilAsserted(() -> {
            issue = getIssue(issue);
            MbocAssertions.assertThat(issue).isClosed();
        });
    }

    @Test
    public void testResolvedOpenStatuses() {
        issue = createIssue();
        MbocAssertions.assertThat(issue).isOpen();

        // переводим в статус решен
        await10Sec().until(() -> {
            try {
                Assert.assertTrue(IssueUtils.resolveIssue(issue));
                return true;
            } catch (Exception e) {
                return false;
            }
        });

        await10Sec().untilAsserted(() -> {
            issue = getIssue(issue);
            MbocAssertions.assertThat(issue).isResolved();

        });


        // обратно переводим в статус открыт
        await10Sec().until(() -> {
            try {
                Assert.assertTrue(IssueUtils.reopenIssue(issue));
                return true;
            } catch (Exception e) {
                return false;
            }
        });

        await10Sec().untilAsserted(() -> {
            issue = getIssue(issue);
            MbocAssertions.assertThat(issue).isOpen();
        });
    }

    protected abstract Issue createIssue();

    protected abstract Issue getIssue(Issue oldIssue);

    private ConditionFactory await10Sec() {
        return Awaitility.await().atMost(Duration.TEN_SECONDS);
    }
}
