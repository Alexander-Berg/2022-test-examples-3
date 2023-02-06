package ru.yandex.direct.appmetrika;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.appmetrika.model.response.Application;
import ru.yandex.direct.asynchttp.FetcherSettings;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.http.smart.examples.TestingConfiguration;
import ru.yandex.direct.tvm.TvmIntegrationImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.tvm.TvmService.APP_METRIKA_API_TEST;
import static ru.yandex.direct.tvm.TvmService.DIRECT_SCRIPTS_TEST;

/**
 * Для ручного вызова
 *
 * Аналоги вызова в командной строке:
 * Приложения
 * curl "https://mobmet-intapi-test.metrika.yandex.net/direct/v1/applications?uid=450666075"
 * -H "X-Ya-Service-Ticket: $(tvmknife get_service_ticket sshkey -s 2009921 -d 2002950 2> /dev/null)"
 *
 * События
 * curl "https://mobmet-intapi-test.metrika.yandex.net/direct/v1/events?uid=450666075&app_id=806976"
 * -H "X-Ya-Service-Ticket: $(tvmknife get_service_ticket sshkey -s 2009921 -d 2002950 2> /dev/null)"
 */
@Ignore("For manual run")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestingConfiguration.class})
public class AppMetrikaClientManualTest {
    public static final long OPERATOR_UID = 450666075L;
    public static final int APP_ID = 806976;
    AppMetrikaClient appMetrikaClient;

    @Before
    public void setup() {
        DirectConfig directConfig = getDirectConfig();
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        TvmIntegrationImpl tvmIntegration = TvmIntegrationImpl.create(directConfig, scheduler);
        ParallelFetcherFactory fetcherFactory = new ParallelFetcherFactory(new DefaultAsyncHttpClient(),
                new FetcherSettings());
        appMetrikaClient = new AppMetrikaClient("https://mobmet-intapi-test.metrika.yandex.net/direct/v1",
                tvmIntegration, APP_METRIKA_API_TEST, fetcherFactory);
    }

    private DirectConfig getDirectConfig() {
        Map<String, Object> conf = new HashMap<>();
        conf.put("tvm.enabled", true);
        conf.put("tvm.app_id", DIRECT_SCRIPTS_TEST.getId());
        conf.put("tvm.secret", "file://~/.direct-tokens/tvm2_direct-scripts-test");
        return DirectConfigFactory.getConfig(EnvironmentType.DB_TESTING, conf);
    }

    @Test
    public void getApplications() {
        List<Application> applications = appMetrikaClient.getApplications(OPERATOR_UID, null, null, null, null, null);
        System.out.println(applications);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(applications.get(0).getId()).isEqualTo(APP_ID);
            soft.assertThat(applications.get(0).getName()).isEqualTo("AppMetrica (Prod)");
        });
    }

    @Test
    public void getEvents() {
        List<String> events = appMetrikaClient.getClientEvents(OPERATOR_UID, APP_ID);
        System.out.println(events);
        assertThat(events.get(0)).isEqualTo("Application search");
    }
}
