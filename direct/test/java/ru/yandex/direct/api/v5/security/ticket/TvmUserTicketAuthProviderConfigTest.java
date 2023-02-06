package ru.yandex.direct.api.v5.security.ticket;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.tvm.TvmService;

import static java.util.stream.Collectors.toList;
import static ru.yandex.direct.tvm.TvmService.UNKNOWN;

@RunWith(Parameterized.class)
public class TvmUserTicketAuthProviderConfigTest {

    @Parameterized.Parameter
    public EnvironmentType env;

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> allNonProductionEnvTypes() {
        return Stream.of(
                EnvironmentType.DEVTEST,
                EnvironmentType.DEVELOPMENT,
                EnvironmentType.DEV7,
                EnvironmentType.TESTING,
                EnvironmentType.TESTING2,
                EnvironmentType.PRESTABLE,
                EnvironmentType.SANDBOX,
                EnvironmentType.SANDBOX_DEVELOPMENT,
                EnvironmentType.SANDBOX_TESTING
        ).map(e -> new Object[]{e})
                .collect(toList());
    }

    private static boolean checkServiceName(String tvmServiceName) {
        try {
            return TvmService.fromStringStrict(tvmServiceName) != UNKNOWN;
        } catch (IllegalArgumentException ignore) {
        }
        return false;
    }

    @Test
    public void tvmApiAuthBranch() {
        DirectConfig directConfig = DirectConfigFactory.getConfigWithoutSystem(env);
        DirectConfig branch = directConfig.getBranch("tvm_api_auth");
        Map<String, String> topicForSourcePrefix = branch.asMap();
        SoftAssertions soft = new SoftAssertions();
        for (var serviceName : topicForSourcePrefix.keySet()) {
            soft.assertThat(checkServiceName(serviceName)).as("'%s' is exist in TvmService enum", serviceName)
                    .isTrue();
        }
        for (var applicationId : topicForSourcePrefix.values()) {
            soft.assertThat(applicationId).as("applicationId is not null")
                    .isNotNull();
            soft.assertThat(applicationId).as("applicationId is not empty")
                    .isNotEmpty();
            soft.assertThat(applicationId).as("applicationId is not blank")
                    .isNotBlank();
        }
        soft.assertAll();
    }

}
