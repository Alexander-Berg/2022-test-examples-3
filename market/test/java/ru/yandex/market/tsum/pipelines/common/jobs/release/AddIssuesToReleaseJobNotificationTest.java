package ru.yandex.market.tsum.pipelines.common.jobs.release;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.clients.notifications.telegram.TelegramNotification;
import ru.yandex.market.tsum.clients.startrek.IssueBuilder;
import ru.yandex.market.tsum.pipelines.test_data.TestVersionBuilder;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.Version;

import static org.hamcrest.Matchers.containsString;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 26.09.17
 */
public class AddIssuesToReleaseJobNotificationTest {
    private static final Issue ISSUE_1 = IssueBuilder.newBuilder("TEST-1")
        .setDisplay("Тестовый тикет #1")
        .setAssignee("s-ermakov", "Sergey Ermakov")
        .build();
    private static final Issue ISSUE_2 = IssueBuilder.newBuilder("TEST-2")
        .setDisplay("Абракадабра")
        .setAssignee("jenkl", "Anton Tsyganov")
        .build();
    private static final Issue ISSUE_3 = IssueBuilder.newBuilder("TEST-3")
        .setDisplay("Тикет без исполнителя")
        .build();
    private static final Issue ISSUE_WITH_INVALID_SYMBOLS = IssueBuilder.newBuilder("TEST-4")
        .setDisplay("[Тест] Символы квадратных кавычек в телеграме запрещены")
        .build();


    private static final List<Issue> ISSUES = Arrays.asList(ISSUE_1, ISSUE_2, ISSUE_3);

    private final AddIssuesToReleaseJob addIssuesToReleaseJob = new AddIssuesToReleaseJob();
    private final Version version = TestVersionBuilder.aVersion().withId(1).withName("2017.9.13").build();

    @Test
    public void noSuitableIssuesForReleaseNotificationTest() {
        String telegramMsg = addIssuesToReleaseJob.noSuitableIssuesForReleaseNotification(version).getTelegramMessage();

        Assert.assertThat(
            telegramMsg, containsString("Не найдено ни одного подходящего тикета для релиза "
                + version.getName())
        );
    }

    @Test
    public void issueForReleaseNotificationTest() {
        String telegramMsg = addIssuesToReleaseJob.issueForReleaseNotification(version, ISSUES).getTelegramMessage();

        Assert.assertThat(
            telegramMsg, Matchers.allOf(
                containsString("Следующие тикеты выбраны для релиза " + version.getName()),
                containsString("[TEST-1 Тестовый тикет #1](https://st.yandex-team.ru/TEST-1) " +
                    "[s-ermakov@](https://staff.yandex-team.ru/s-ermakov)"),
                containsString("[TEST-2 Абракадабра](https://st.yandex-team.ru/TEST-2) " +
                    "[jenkl@](https://staff.yandex-team.ru/jenkl)"),
                containsString("[TEST-3 Тикет без исполнителя](https://st.yandex-team.ru/TEST-3)")
            )
        );
    }

    @Test
    public void testTitleEscaping() {
        TelegramNotification telegramNotification = addIssuesToReleaseJob.issueForReleaseNotification(version,
            Collections.singletonList(ISSUE_WITH_INVALID_SYMBOLS));

        String telegramMsg = telegramNotification.getTelegramMessage();
        Assert.assertThat(telegramMsg, containsString(
            "[TEST-4 Тест Символы квадратных кавычек в телеграме запрещены]" +
                "(https://st.yandex-team.ru/TEST-4)"));

    }
}
