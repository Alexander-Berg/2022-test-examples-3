package ru.yandex.chemodan.app.psbilling.worker;

import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.chemodan.app.psbilling.core.config.YtExportSettings;
import ru.yandex.chemodan.app.psbilling.worker.monitor.YtJobsStatusMonitor;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.misc.test.Assert;

public class YtExportMonitorTest extends AbstractWorkerTest {
    @Autowired
    private YtJobsStatusMonitor ytJobsStatusMonitor;

    @Autowired
    @Qualifier("distributionPlatformPrimaryExportSettings")
    private YtExportSettings dpPrimarySettings;

    @Autowired
    @Qualifier("distributionPlatformSecondaryExportSettings")
    private YtExportSettings dpSecondarySettings;

    @Autowired
    @Qualifier("groupServicesPrimaryExportSettings")
    private YtExportSettings gsPrimarySettings;

    @Autowired
    @Qualifier("groupServicesSecondaryExportSettings")
    private YtExportSettings gsSecondarySettings;

    @Before
    public void init() {
        deleteExport(dpPrimarySettings);
        deleteExport(dpSecondarySettings);
        deleteExport(gsPrimarySettings);
        deleteExport(gsSecondarySettings);
    }

    @Test
    public void distributionPlatformExportStale() {
        // no export folders = stale
        deleteExport(dpPrimarySettings);
        deleteExport(dpSecondarySettings);
        Assert.equals(1, ytJobsStatusMonitor.getDistributionPlatformExportStale());

        // only primary folder is up to date = stale
        createExport(dpPrimarySettings, Instant.now());
        Assert.equals(1, ytJobsStatusMonitor.getDistributionPlatformExportStale());

        // all folders is up to date = not stale
        createExport(dpSecondarySettings, Instant.now());
        Assert.equals(0, ytJobsStatusMonitor.getDistributionPlatformExportStale());

        // only secondary folder is up to date = stale
        createExport(dpPrimarySettings, Instant.now().minus(YtJobsStatusMonitor.DP_EXPORTS_STALE_TIME.plus(1)));
        createExport(dpSecondarySettings, Instant.now());
        Assert.equals(1, ytJobsStatusMonitor.getDistributionPlatformExportStale());

        // default state is not stale
        ytJobsStatusMonitor.resetState();
        Assert.equals(0, ytJobsStatusMonitor.getDistributionPlatformExportStale());
    }

    @Test
    public void disabledExportIsNotStale() {
        // no export folders = stale
        deleteExport(dpPrimarySettings);
        deleteExport(dpSecondarySettings);
        Assert.equals(1, ytJobsStatusMonitor.getDistributionPlatformExportStale());

        // only primary folder is up to date = stale
        createExport(dpPrimarySettings, Instant.now());
        Assert.equals(1, ytJobsStatusMonitor.getDistributionPlatformExportStale());

        // stale folder is for disabled cluster
        dpSecondarySettings.setEnabled(false);
        ytJobsStatusMonitor.updateState();
        Assert.equals(0, ytJobsStatusMonitor.getDistributionPlatformExportStale());
    }

    @Test
    public void groupServiceExportStale() {
        // no export folders = stale
        deleteExport(gsPrimarySettings);
        deleteExport(gsSecondarySettings);
        Assert.equals(1, ytJobsStatusMonitor.getGroupBillingExportStale());

        // only primary folder is up to date = stale
        createExport(gsPrimarySettings, Instant.now());
        Assert.equals(1, ytJobsStatusMonitor.getGroupBillingExportStale());

        // all folders is up to date = not stale
        createExport(gsSecondarySettings, Instant.now());
        Assert.equals(0, ytJobsStatusMonitor.getGroupBillingExportStale());

        // only secondary folder is up to date = stale
        createExport(gsPrimarySettings, Instant.now().minus(YtJobsStatusMonitor.GS_EXPORTS_STALE_TIME.plus(1)));
        createExport(gsSecondarySettings, Instant.now());
        Assert.equals(1, ytJobsStatusMonitor.getGroupBillingExportStale());

        // default state is not stale
        ytJobsStatusMonitor.resetState();
        Assert.equals(0, ytJobsStatusMonitor.getGroupBillingExportStale());
    }

    private void createExport(YtExportSettings settings, Instant time) {
        YPath currentExportPath = settings.getRootExportPath().child("current");
        YPath modificationAttribute = currentExportPath.attribute("modification_time");

        Mockito.when(settings.getYt().cypress().exists(Mockito.eq(modificationAttribute))).thenReturn(true);
        Mockito.when(settings.getYt().cypress().get(Mockito.eq(modificationAttribute)))
                .thenReturn(new YTreeStringNodeImpl(time.toString(), Cf.map()));

        ytJobsStatusMonitor.updateState();
    }

    private void deleteExport(YtExportSettings settings) {
        YPath currentExportPath = settings.getRootExportPath().child("current");
        YPath modificationAttribute = currentExportPath.attribute("modification_time");
        Mockito.when(settings.getYt().cypress().exists(Mockito.eq(modificationAttribute))).thenReturn(false);
        ytJobsStatusMonitor.updateState();
    }
}
