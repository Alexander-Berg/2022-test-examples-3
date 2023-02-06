package ru.yandex.direct.grid.processing.service.constant;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.common.enums.YandexDomain;
import ru.yandex.direct.env.EnvironmentType;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static ru.yandex.direct.grid.processing.service.constant.CrowdTestSettingsService.CROWD_TEST_TURBOLANDINGS_API_URL_PREFIX;

@RunWith(JUnitParamsRunner.class)
public class CrowdTestSettingsServiceTest {
    public static final String CROWD_TEST_DOMAIN = "release.crowdtest.direct.yandex.ru";

    private CrowdTestSettingsService crowdTestSettingsService;

    @Test
    public void getCrowdTestTurbolandingApiUrl() {
        crowdTestSettingsService = spy(new CrowdTestSettingsService(EnvironmentType.TESTING));
        doReturn(CROWD_TEST_DOMAIN).when(crowdTestSettingsService).getHostHeader();
        var result = crowdTestSettingsService.isCrowdTestRequest(YandexDomain.RU);
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void getTurbolandingApiUrl() {
        crowdTestSettingsService = new CrowdTestSettingsService(EnvironmentType.PRODUCTION);
        var result = crowdTestSettingsService.isCrowdTestRequest(YandexDomain.RU);
        Assertions.assertThat(result).isFalse();
    }

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("get url domain: {0}")
    public void getCrowdTestTurbolandingApiUrl(YandexDomain domain, String expectedString) {
        crowdTestSettingsService = new CrowdTestSettingsService(EnvironmentType.TESTING);
        var result = crowdTestSettingsService.getCrowdTestTurbolandingApiUrl(domain);
        Assertions.assertThat(result).isEqualTo(expectedString);
    }

    @SuppressWarnings("unused")
    private Object[] parametrizedTestData() {
        var params = new Object[YandexDomain.values().length][2];
        int i = 0;
        for (YandexDomain value : YandexDomain.values()) {
            params[i][0] = value;
            params[i][1] = CROWD_TEST_TURBOLANDINGS_API_URL_PREFIX + value.getYandexDomain();
            i++;
        }
        return params;
    }
}
