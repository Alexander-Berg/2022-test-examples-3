package ru.yandex.chemodan.app.psbilling.core.config;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.chemodan.util.yt.YtHelper;
import ru.yandex.inside.yt.kosher.cypress.YPath;

@Configuration
public class YtExportMockConfiguration {

    @Bean
    public YtExportSettings distributionPlatformPrimaryExportSettings(
            @Value("${billing.yt.distribution_platform.primary.export.path}") String path) {
        return new YtExportSettings(true, YPath.simple(path), createHelperMock());
    }

    @Bean
    public YtExportSettings distributionPlatformSecondaryExportSettings(
            @Value("${billing.yt.distribution_platform.secondary.export.path}") String path) {

        return new YtExportSettings(true, YPath.simple(path), createHelperMock());
    }

    @Bean
    public YtExportSettings groupServicesPrimaryExportSettings(
            @Value("${billing.yt.group_services.primary.export.path}") String path) {
        return new YtExportSettings(true, YPath.simple(path), createHelperMock());
    }

    @Bean
    public YtExportSettings groupServicesSecondaryExportSettings(
            @Value("${billing.yt.group_services.secondary.export.path}") String path) {
        return new YtExportSettings(true, YPath.simple(path), createHelperMock());
    }


    private YtHelper createHelperMock() {
        return Mockito.mock(YtHelper.class, Mockito.RETURNS_DEEP_STUBS);
    }
}
