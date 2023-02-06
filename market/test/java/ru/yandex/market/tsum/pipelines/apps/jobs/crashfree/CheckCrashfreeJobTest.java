package ru.yandex.market.tsum.pipelines.apps.jobs.crashfree;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.pipelines.apps.MobilePlatform;
import ru.yandex.market.tsum.pipelines.apps.jobs.crashfree.version.ProductionAppVersionName;
import ru.yandex.market.tsum.pipelines.apps.resources.AppConfigResource;
import ru.yandex.market.tsum.pipelines.apps.resources.AppmetricaResource;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class CheckCrashfreeJobTest {

    private static final String EXP = "https://appmetrica.yandex.ru/statistic?appId=1389598&report=crash-logs-android" +
        "&filters={\"values\":[{\"id\":\"commonAppVersion\",\"data\":{\"values\":[{\"value\":\"2.21.1972+(Android)\"," +
        "\"operator\":\"==\"}]}}]}&sampling=1";

    @InjectMocks
    private CheckCrashfreeJob job = new CheckCrashfreeJob();
    @Mock
    private AppmetricaResource appmetricaResource;
    @Mock
    private AppConfigResource appConfigResource;
    @Mock
    private ProductionAppVersionName signerUploadedAppVersion;

    @Before
    public void setUp() {
        when(appmetricaResource.getAppId()).thenReturn("1389598");
        when(appConfigResource.getPlatform()).thenReturn(MobilePlatform.ANDROID);
        when(signerUploadedAppVersion.getVersionName()).thenReturn("2.21.1972");
    }

    @Test
    public void testBuildAppMetricaLink() {
        String res = job.buildAppMetricaLink();
        assertEquals(res, EXP);
    }
}
