package ru.yandex.market.tsum.pipelines.wms.jobs.testing;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tsum.context.TsumJobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResource;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogEntry;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogInfo;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersion;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;
import ru.yandex.market.tsum.pipelines.common.resources.StartrekSettings;
import ru.yandex.market.tsum.pipelines.wms.jobs.WmsVersions;
import ru.yandex.market.tsum.pipelines.wms.resources.WmsDeliveryParams;
import ru.yandex.market.tsum.pipelines.wms.resources.WmsProjectConfig;
import ru.yandex.startrek.client.Versions;
import ru.yandex.startrek.client.model.Version;

@Produces(single = {ReleaseInfo.class, ChangelogInfo.class})
public class WmsCreateSnapshotJob implements JobExecutor<TsumJobContext> {
    private static final Logger log = LogManager.getLogger(WmsCreateSnapshotJob.class);

    private static final String VERSION_PATTERN = "%s.snap.%s";

    @Autowired
    private Versions versions;

    @WiredResource
    private StartrekSettings startrekSettings;

    @WiredResource
    private WmsDeliveryParams params;

    @WiredResource
    private WmsProjectConfig wmsProjectConfig;

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("fe10c351-cfcc-449b-883c-2a317a9050fb");
    }

    @Override
    public void execute(TsumJobContext context) throws Exception {
        String revision = params.getRevision();
        Version version = new WmsVersions(versions, startrekSettings.getQueue()).lastVersion();
        String snapVersion = String.format(VERSION_PATTERN, version.getName(), randomRevision());
        ReleaseInfo releaseInfo = new ReleaseInfo(
            new FixVersion(0L, snapVersion));
        context.resources().produce(releaseInfo);
        context.resources().produce(new ChangelogInfo(
                List.of(
                    new ChangelogEntry(revision,
                        "Snapshot version from tsum pipeline for revision: " + revision)
                )
            )
        );
        context.progress().updateText(snapVersion);
    }

    private String randomRevision() {
        return String.valueOf(System.currentTimeMillis());
    }
}
