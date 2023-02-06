package ru.yandex.direct.logicprocessor.processors.configuration;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.direct.core.entity.timetarget.repository.GeoTimezoneRepository;
import ru.yandex.direct.logicprocessor.processors.mysql2grut.replicationwriter.TimezoneCache;
import ru.yandex.direct.logicprocessor.processors.mysql2grut.steps.GrutReplicationSteps;
import ru.yandex.direct.test.grut.GrutTestClientFactory;
import ru.yandex.grut.client.GrutClient;

@Configuration
@Import(EssLogicProcessorTestConfiguration.class)
public class EssLogicProcessorGrutTestConfiguration {


    @Bean
    @Primary
    public GrutClient grutClient() {
        return GrutTestClientFactory.getGrutClient();
    }

    @Bean
    GrutReplicationSteps grutReplicationSteps() {
        return new GrutReplicationSteps();
    }

    @Bean
    @Primary
    TimezoneCache timezoneCache(GeoTimezoneRepository geoTimezoneRepository) {
        return new TimezoneCache(geoTimezoneRepository, Duration.ZERO);
    }

}
