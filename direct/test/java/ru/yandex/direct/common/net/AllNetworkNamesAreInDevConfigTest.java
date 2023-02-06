package ru.yandex.direct.common.net;

import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.liveresource.LiveResource;
import ru.yandex.direct.liveresource.LiveResourceFactory;

import static java.util.stream.Collectors.toList;

@RunWith(Parameterized.class)
public class AllNetworkNamesAreInDevConfigTest {
    private static final String NETWORK_CONFIG_KEY = "network_config";

    @Parameterized.Parameter
    public EnvironmentType env;

    private NetAcl netAcl;

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> allTestingEnvTypes() {
        // Проверяем, что в тестовых окружениях, где используются заранее определённые списки сетей, эти сети существуют
        return Stream.of(
                EnvironmentType.DEVELOPMENT,
                EnvironmentType.DB_TESTING,
                EnvironmentType.SANDBOX,
                EnvironmentType.SANDBOX_DEVELOPMENT,
                EnvironmentType.SANDBOX_TESTING
        ).map(e -> new Object[]{e})
                .collect(toList());
    }

    @Before
    public void setUp() {
        DirectConfig config = DirectConfigFactory.getConfigWithoutSystem(env);
        String networkConfig = config.getString(NETWORK_CONFIG_KEY);
        LiveResource liveResource = LiveResourceFactory.get(networkConfig);
        netAcl = new NetAcl(liveResource.getContent());
    }

    @Test
    public void checkAllNetworkNamesArePresent() {
        // Проверим, что все известные нам сети объявлены в конфигурации
        List<String> networkNames = Stream.of(NetworkName.values())
                .map(NetworkName::networkName)
                .collect(toList());
        SoftAssertions softly = new SoftAssertions();
        for (String networkName : networkNames) {
            softly.assertThat(netAcl.getIpRangeValidator(networkName))
                    .describedAs("Network '%s' from ACL defined by key '%s' in common-%s.conf",
                            networkName, NETWORK_CONFIG_KEY, env.toString().toLowerCase())
                    .isNotNull();
        }
        softly.assertAll();
    }
}
