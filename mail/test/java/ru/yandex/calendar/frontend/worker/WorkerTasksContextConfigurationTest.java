package ru.yandex.calendar.frontend.worker;

import java.util.List;

import javax.annotation.PostConstruct;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.layer.common.CommonLayersContextConfiguration;
import ru.yandex.calendar.logic.mailer.MailerContextConfiguration;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.calendar.test.generic.TestBaseContextConfiguration;
import ru.yandex.calendar.util.idlent.YandexUser;
import ru.yandex.commune.bazinga.scheduler.CronTask;
import ru.yandex.commune.bazinga.scheduler.OnetimeTask;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.login.PassportLogin;
import ru.yandex.misc.email.Email;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = WorkerTasksContextConfigurationTest.ContextConfiguration.class)
public class WorkerTasksContextConfigurationTest extends AbstractConfTest {

    @Autowired
    private List<OnetimeTask> onetimeTasks;
    @Autowired
    private List<CronTask> cronTasks;

    @Test
    public void tasksAreCalendarBased() {
        assertThat(onetimeTasks).allMatch(CalendarOnetimeTask.class::isInstance);

        assertThat(cronTasks).allMatch(CalendarCronTask.class::isInstance);
    }

    @Configuration
    @Import({
            WorkerTasksContextConfiguration.class,
            TestBaseContextConfiguration.class,
            CommonLayersContextConfiguration.class,
            MailerContextConfiguration.class,
    })
    public static class ContextConfiguration {

        @Autowired
        private UserManager userManager;

        @PostConstruct
        public void init() {
            userManager.registerYandexUserForTest(new YandexUser(
                    PassportUid.cons(1), PassportLogin.cons("robot-calendar"),
                    Option.empty(), Option.of(new Email("robot-calendar@yandex-team.ru")),
                    Option.empty(), Option.empty(), Option.empty())
            );
        }
    }
}
