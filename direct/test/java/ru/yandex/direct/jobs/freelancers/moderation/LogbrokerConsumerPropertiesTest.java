package ru.yandex.direct.jobs.freelancers.moderation;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.jobs.freelancers.moderation.receiving.FreelancerCardReceivingJob;

class LogbrokerConsumerPropertiesTest {

    static Object[] provideTestData() {
        return new Object[][]{
                {EnvironmentType.PRODUCTION, FreelancerCardReceivingJob.CONFIG_SECTION_NAME},
                {EnvironmentType.TESTING, FreelancerCardReceivingJob.CONFIG_SECTION_NAME},
                {EnvironmentType.DEVELOPMENT, FreelancerCardReceivingJob.CONFIG_SECTION_NAME},
        };
    }

    @ParameterizedTest
    @MethodSource("provideTestData")
    void createInstance_success_forEnvironmentAndSection(EnvironmentType environmentType,
                                                         String configSectionName) {
        DirectConfig directConfig = DirectConfigFactory.getConfig(environmentType);
        LogbrokerConsumerProperties properties =
                LogbrokerConsumerProperties.createInstance(directConfig, configSectionName);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(properties.getHost()).isNotEmpty();
            soft.assertThat(properties.getTimeoutSec()).isNotNegative();
            soft.assertThatCode(properties::getTvmService).doesNotThrowAnyException();
            soft.assertThat(properties.getReadTopic()).isNotEmpty();
            soft.assertThat(properties.getConsumerName()).isNotEmpty();
        });
    }
}
