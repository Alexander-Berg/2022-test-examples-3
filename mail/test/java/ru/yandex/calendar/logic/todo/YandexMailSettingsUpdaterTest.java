package ru.yandex.calendar.logic.todo;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import ru.yandex.calendar.boot.CalendarContextConfiguration;
import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.calendar.test.generic.CalendarSpringJUnit4ClassRunner;
import ru.yandex.calendar.test.generic.CalendarTestInitContextConfiguration;
import ru.yandex.calendar.test.generic.MeterRegistryStubTestConfiguration;
import ru.yandex.calendar.test.generic.TvmClientTestConfiguration;
import ru.yandex.calendar.util.HttpClientConfiguration;
import ru.yandex.inside.passport.PassportUid;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(
        loader = AnnotationConfigContextLoader.class,
        classes = {
            CalendarContextConfiguration.class,
            TestSettingsUpdaterConfiguration.class,
            TvmClientTestConfiguration.class
        }
)
@RunWith(CalendarSpringJUnit4ClassRunner.class)
public class YandexMailSettingsUpdaterTest extends CalendarTestBase {
    @Autowired
    private YandexMailSettingsUpdater yandexMailSettingsUpdater;
    private static final long CALENDARTESTUSER_UID = 1120000000004717L;

    @Test
    public void showTodoSettingsUpdate() {
        val uid = new PassportUid(CALENDARTESTUSER_UID);
        setTodoAndCheck(uid, false);
        setTodoAndCheck(uid, true);
        setTodoAndCheck(uid, false);
    }

    private void setTodoAndCheck(PassportUid uid, boolean on) {
        yandexMailSettingsUpdater.updateShowTodo(uid, on);
        assertThat(yandexMailSettingsUpdater.getCurrentShowTodoState(uid))
                .isEqualTo("{\"settings\":{\"single_settings\":{\"show_todo\":\"" + (on ? "on" : "") + "\"}}}");
    }
}

@Configuration
@Import({CalendarTestInitContextConfiguration.class,
        MeterRegistryStubTestConfiguration.class})
class TestSettingsUpdaterConfiguration {
    @Bean
    public YandexMailSettingsUpdater yandexMailSettingsUpdater(MeterRegistry meterRegistry,
                                                               @Value("${mail-settings.url}") String mailSettingsUrl,
                                                               @Value("${mail-settings.tvm-id}") int mailSettingsTVMid) {
        val httpClient = HttpClientConfiguration.forTest()
                .consTrustAllClient(meterRegistry, "mail-settings");
        return new YandexMailSettingsUpdater(mailSettingsUrl, mailSettingsTVMid, httpClient);
    }
}
