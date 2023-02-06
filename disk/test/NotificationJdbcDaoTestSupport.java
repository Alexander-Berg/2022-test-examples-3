package ru.yandex.chemodan.app.notifier.admin.dao.test;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.chemodan.test.TestHelper;
import ru.yandex.commune.test.random.RunWithRandomTestRunner;
import ru.yandex.devtools.test.annotations.YaIgnore;

/**
 * @author akirakozov
 */
@YaIgnore
@RunWith(RunWithRandomTestRunner.class)
@ContextConfiguration(classes = NotifierJdbcDaoTestsContextConfiguration.class)
@ActivateNotificationEmbeddedPg
public abstract class NotificationJdbcDaoTestSupport {
    @Before
    public void init() {
        TestHelper.initialize();
    }
}
