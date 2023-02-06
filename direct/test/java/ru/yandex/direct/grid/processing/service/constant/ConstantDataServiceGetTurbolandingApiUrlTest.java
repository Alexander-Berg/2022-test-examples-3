package ru.yandex.direct.grid.processing.service.constant;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.servlet.http.HttpServletRequest;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ru.yandex.direct.common.enums.YandexDomain;
import ru.yandex.direct.core.entity.agencyofflinereport.service.AgencyOfflineReportParametersService;
import ru.yandex.direct.grid.processing.service.offlinereport.OfflineReportValidationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class ConstantDataServiceGetTurbolandingApiUrlTest {
    private static final String TURBOLANDING_API_URL_BASE = "turboapi.";
    private static final String TURBOLANDING_API_URL = TURBOLANDING_API_URL_BASE + YandexDomain.RU.getYandexDomain();

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private CrowdTestSettingsService crowdTestSettingsService;

    private ConstantDataService constantDataService;

    @Before
    public void initTestData() {
        MockitoAnnotations.initMocks(this);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpServletRequest));
        //noinspection ConstantConditions
        constantDataService = new ConstantDataService(TURBOLANDING_API_URL, null, null,null, null,
                new AgencyOfflineReportParametersService(null), null, null, null,
                null, null, null, null, null, null, new OfflineReportValidationService(null, null), null,
                crowdTestSettingsService, null);
    }

    @After
    public void afterTest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @SuppressWarnings("unused")
    private Object[] parametrizedTestData() {
        return new Object[][]{
                {YandexDomain.RU},
                {YandexDomain.TR},
                {YandexDomain.BY},
                {YandexDomain.UA},
                {YandexDomain.KZ},
                {YandexDomain.COM}
        };
    }

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("domain: {0}")
    public void testTurbolandingApiUrlForDomain(YandexDomain domain) {
        doReturn(domain.getYandexDomain()).when(httpServletRequest).getServerName();
        String expected = TURBOLANDING_API_URL_BASE + domain.getYandexDomain();
        assertThat(constantDataService.getTurbolandingApiUrl()).isEqualTo(expected);
    }

}
