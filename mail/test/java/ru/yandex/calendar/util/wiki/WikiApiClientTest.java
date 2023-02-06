package ru.yandex.calendar.util.wiki;

import java.io.File;
import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import ru.yandex.calendar.logic.user.CenterContextConfiguration;
import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.calendar.test.generic.CalendarSpringJUnit4ClassRunner;
import ru.yandex.calendar.test.generic.CalendarTestInitContextConfiguration;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.test.Assert;
import ru.yandex.oauth.OAuth;

import static org.apache.commons.io.FileUtils.readFileToString;

@RunWith(CalendarSpringJUnit4ClassRunner.class)
@ContextConfiguration(
        loader = AnnotationConfigContextLoader.class,
        classes = WikiApiClientTest.TestContextConfiguration.class
)
public class WikiApiClientTest extends CalendarTestBase {

    @Configuration
    @Import(CalendarTestInitContextConfiguration.class)
    public static class TestContextConfiguration {
        @Value("${yt.staff.token:-}")
        private String staffApiToken;
        @Value("${yt.staff.token.path:-}")
        private String staffApiTokenPath;

        @Autowired
        OAuth oauth;

        @Bean
        public WikiApiClient wikiApiClient() throws IOException {
            HttpClient httpClient = ApacheHttpClientUtils.trustAllMultiThreadedHttpsClient(
                    Timeout.seconds(10), 1, 1, "YandexCalendarUnitTests");

            if (staffApiToken.isEmpty()) {
                if (staffApiTokenPath.isEmpty()) {
                    staffApiToken = oauth.getOauthToken(CenterContextConfiguration.oauthClientId, CenterContextConfiguration.oauthClientPassword).get(0);
                } else {
                    staffApiToken = readFileToString(new File(staffApiTokenPath)).trim();
                }
            }
            return new WikiApiClient("https://wiki-api.test.yandex-team.ru/_api/v1/pages/",
                    staffApiToken, httpClient);
        }
    }

    @Autowired
    private WikiApiClient wikiApiClient;

    @Test
    public void getFormatted() {
        try {
            Assert.isTrue(wikiApiClient.getFormatted("/constitution")
                    .contains("<strong class=\"wiki-bold\">Мы тоже пользователи</strong>"));
        } catch (Exception e) {
            if (!e.getMessage().contains("Connection timed out")) {
                Assert.fail(e.getMessage());
            }
        }
    }

}
