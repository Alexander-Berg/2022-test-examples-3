package ru.yandex.direct.jobs.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.direct.test.grut.GrutTestClientFactory;
import ru.yandex.grut.client.GrutClient;

import static ru.yandex.direct.core.configuration.CoreConfiguration.GRUT_CLIENT_FOR_WATCHLOG;

@Configuration
@Import({JobsTestingSpyConfiguration.class})
public class GrutJobsTestingConfiguration {
    @Bean
    @Primary
    public GrutClient grutClient() {
        return GrutTestClientFactory.getGrutClient();
    }

    @Bean(GRUT_CLIENT_FOR_WATCHLOG)
    public GrutClient grutClientForWatchlog(GrutClient grutClient) {
        return grutClient;
    }
}
