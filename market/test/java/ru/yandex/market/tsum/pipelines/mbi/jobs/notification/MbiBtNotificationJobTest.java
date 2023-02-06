package ru.yandex.market.tsum.pipelines.mbi.jobs.notification;

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import ru.yandex.market.tsum.clients.startrek.NotificationUtils;
import ru.yandex.market.tsum.core.notify.common.ContextBuilder;

/**
 * Unit-тесты на рендеринг нотификации о выкладке в Большой Тестинг.
 *
 * @author fbokovikov
 * @see ru.yandex.market.tsum.pipelines.mbi.jobs.notification.MbiBtNotificationJob
 */
public class MbiBtNotificationJobTest {

    private static final String BASE_TELEGRAM_PART =
        "\uD83D\uDE97 Релиз [2018.2.251](https://st.yandex-team.ru/MBI/filter?fixVersions=79052) выложен в БТ, " +
            "релизный тикет [MBI-28594](https://st.yandex-team.ru/MBI-28594)\n\n";

    private static final String NOTIFY_TAG_HINT =
        "\nДля уведомления о деплое добавьте в тикет тег `notify_deploy`.";

    @Test
    public void emptyScopedTickets() {
        String renderedMessage = NotificationUtils.render(
            MbiNotifications.MBI_INTEGRATIONAL_RELEASED.getDefaultMessages().getTelegramDefault(),
            ContextBuilder.create()
                .with("releaseName", "2018.2.251")
                .with("releaseFilterUrl", "https://st.yandex-team.ru/MBI/filter?fixVersions=79052")
                .with("releaseTicketKey", "MBI-28594")
                .with("releaseIssueUrl", "https://st.yandex-team.ru/MBI-28594")
                .with("testScopedIssues", List.of())
                .with("taggedForNotificationIssues", List.of())
                .build()
        );
        String expectedMessage =
            BASE_TELEGRAM_PART + "Релиз не содержит тикетов, требующих ручного тестирования" + NOTIFY_TAG_HINT;
        MatcherAssert.assertThat(
            renderedMessage,
            Matchers.equalTo(expectedMessage)
        );
    }

    @Test
    public void withScopedTickets() {
        String renderedMessage = NotificationUtils.render(
            MbiNotifications.MBI_INTEGRATIONAL_RELEASED.getDefaultMessages().getTelegramDefault(),
            ContextBuilder.create()
                .with("releaseName", "2018.2.251")
                .with("releaseFilterUrl", "https://st.yandex-team.ru/MBI/filter?fixVersions=79052")
                .with("releaseTicketKey", "MBI-28594")
                .with("releaseIssueUrl", "https://st.yandex-team.ru/MBI-28594")
                .with(
                    "testScopedIssues",
                    ImmutableList.of(
                        new MbiNotificationIssue.Builder()
                            .setAssigneeStaffLogin("fbokovikov")
                            .setIssueKey("MBI-12345")
                            .setIssueTitle("Очень важный тикет")
                            .setAssigneeName("Федор Боковиков")
                            .setAssigneeTelegramLogin("@fbokovikov")
                            .setAlreadyTested(true)
                            .build(),
                        new MbiNotificationIssue.Builder()
                            .setAssigneeStaffLogin("stac_mc")
                            .setAssigneeTelegramLogin("@stani")
                            .setAssigneeName("Станислав Мальченко")
                            .setIssueTitle("Менее важный тикет")
                            .setIssueKey("MBI-54321")
                            .setAlreadyTested(false)
                            .build()
                    )
                )
                .with("taggedForNotificationIssues", List.of())
                .build()
        );
        String expectedMessage = BASE_TELEGRAM_PART +
            "*Тикеты, требующие ручного тестирования:*\n" +
            "[MBI-12345](https://st.yandex-team.ru/MBI-12345) — повторный тест. ```* Очень важный тикет Федор " +
            "Боковиков``` @fbokovikov\n" +
            "[MBI-54321](https://st.yandex-team.ru/MBI-54321) ```* Менее важный тикет Станислав Мальченко``` " +
            "@stani\n" + NOTIFY_TAG_HINT;
        MatcherAssert.assertThat(
            renderedMessage,
            Matchers.equalTo(expectedMessage)
        );
    }

    @Test
    public void withScopedAndTaggedTickets() {
        String renderedMessage = NotificationUtils.render(
            MbiNotifications.MBI_INTEGRATIONAL_RELEASED.getDefaultMessages().getTelegramDefault(),
            ContextBuilder.create()
                .with("releaseName", "2018.2.251")
                .with("releaseFilterUrl", "https://st.yandex-team.ru/MBI/filter?fixVersions=79052")
                .with("releaseTicketKey", "MBI-28594")
                .with("releaseIssueUrl", "https://st.yandex-team.ru/MBI-28594")
                .with(
                    "testScopedIssues",
                    List.of(
                        new MbiNotificationIssue.Builder()
                            .setAssigneeStaffLogin("fbokovikov")
                            .setIssueKey("MBI-12345")
                            .setIssueTitle("Очень важный тикет")
                            .setAssigneeName("Федор Боковиков")
                            .setAssigneeTelegramLogin("@fbokovikov")
                            .setAlreadyTested(false)
                            .build()
                    )
                )
                .with(
                    "taggedForNotificationIssues",
                    List.of(
                        new MbiNotificationIssue.Builder()
                            .setAssigneeStaffLogin("stac_mc")
                            .setAssigneeTelegramLogin("@stani")
                            .setAssigneeName("Станислав Мальченко")
                            .setIssueTitle("Менее важный тикет")
                            .setIssueKey("MBI-54321")
                            .setAlreadyTested(false)
                            .build()
                    )
                )
                .build()
        );
        String expectedMessage = BASE_TELEGRAM_PART +
            "*Тикеты, требующие ручного тестирования:*\n" +
            "[MBI-12345](https://st.yandex-team.ru/MBI-12345) ```* Очень важный тикет Федор Боковиков``` " +
            "@fbokovikov\n" +
            "\n\n" +
            "*Тикеты c тегами уведомлений:*\n" +
            "[MBI-54321](https://st.yandex-team.ru/MBI-54321) — @stani\n" + NOTIFY_TAG_HINT;
        MatcherAssert.assertThat(
            renderedMessage,
            Matchers.equalTo(expectedMessage)
        );
    }


    @Test
    public void withoutScopedAndWithTaggedTickets() {
        String renderedMessage = NotificationUtils.render(
            MbiNotifications.MBI_INTEGRATIONAL_RELEASED.getDefaultMessages().getTelegramDefault(),
            ContextBuilder.create()
                .with("releaseName", "2018.2.251")
                .with("releaseFilterUrl", "https://st.yandex-team.ru/MBI/filter?fixVersions=79052")
                .with("releaseTicketKey", "MBI-28594")
                .with("releaseIssueUrl", "https://st.yandex-team.ru/MBI-28594")
                .with("testScopedIssues", List.of())
                .with(
                    "taggedForNotificationIssues",
                    List.of(
                        new MbiNotificationIssue.Builder()
                            .setAssigneeStaffLogin("stac_mc")
                            .setAssigneeTelegramLogin("@stani")
                            .setAssigneeName("Станислав Мальченко")
                            .setIssueTitle("Тикикет")
                            .setIssueKey("MBI-54321")
                            .setAlreadyTested(false)
                            .build()
                    )
                )
                .build()
        );
        String expectedMessage = BASE_TELEGRAM_PART +
            "Релиз не содержит тикетов, требующих ручного тестирования\n" +
            "\n" +
            "*Тикеты c тегами уведомлений:*\n" +
            "[MBI-54321](https://st.yandex-team.ru/MBI-54321) — @stani\n" + NOTIFY_TAG_HINT;
        MatcherAssert.assertThat(
            renderedMessage,
            Matchers.equalTo(expectedMessage)
        );
    }

}
