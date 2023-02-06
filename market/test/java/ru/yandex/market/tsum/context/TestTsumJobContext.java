package ru.yandex.market.tsum.context;

import java.util.Collections;
import java.util.List;

import ru.yandex.market.tsum.context.impl.TestInternalJobContext;
import ru.yandex.market.tsum.entity.project.DeliveryMachineEntity;
import ru.yandex.market.tsum.multitesting.MultitestingDatacenterWeightService;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogEntry;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogInfo;
import ru.yandex.market.tsum.release.RepositoryType;
import ru.yandex.market.tsum.release.dao.CreateReleaseCommandBuilder;
import ru.yandex.market.tsum.release.dao.FinishCause;
import ru.yandex.market.tsum.release.dao.Release;
import ru.yandex.market.tsum.release.dao.ReleaseDao;
import ru.yandex.market.tsum.release.dao.ReleaseService;
import ru.yandex.market.tsum.release.dao.VcsSettings;
import ru.yandex.market.tsum.release.dao.delivery.launch_rules.LaunchRuleChecker;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 23/11/2018
 */
public class TestTsumJobContext extends TestJobContext
    implements TsumJobContext, ReleaseJobContext, CdReleaseJobContext, MtJobContext {
    private ReleaseService releaseService;
    private ReleaseDao releaseDao;
    private LaunchRuleChecker launchRuleChecker;

    public TestTsumJobContext(ReleaseService releaseService, String user) {
        super(user);
        this.releaseService = releaseService;
    }

    public TestTsumJobContext(ReleaseService releaseService, String user, LaunchRuleChecker launchRuleChecker) {
        super(user);
        this.releaseService = releaseService;
        this.launchRuleChecker = launchRuleChecker;
    }

    public TestTsumJobContext(ReleaseService releaseService, ReleaseDao releaseDao, String user) {
        super(user);
        this.releaseService = releaseService;
        this.releaseDao = releaseDao;
    }

    public TestTsumJobContext(ReleaseService releaseService, ReleaseDao releaseDao,
                              String user, LaunchRuleChecker launchRuleChecker) {
        super(user);
        this.releaseService = releaseService;
        this.releaseDao = releaseDao;
        this.launchRuleChecker = launchRuleChecker;
    }

    public TestTsumJobContext(String user) {
        super(user);
    }

    @Override
    public boolean isRelease() {
        return true;
    }

    @Override
    public ReleaseJobContext release() {
        return this;
    }

    @Override
    public boolean isMt() {
        return true;
    }

    @Override
    public MtJobContext mt() {
        return null;
    }

    @Override
    public boolean isCd() {
        return true;
    }

    @Override
    public CdReleaseJobContext cd() {
        return this;
    }

    @Override
    public boolean isPerCommit() {
        return false;
    }

    @Override
    public PerCommitContext perCommit() {
        return null;
    }

    @Override
    public InternalJobContext internal() {
        return new TestInternalJobContext();
    }

    @Override
    public Release findReleaseByTag(String tag) {
        return null;
    }

    @Override
    public Release launchRelease(CreateReleaseCommandBuilder command) {
        return null;
    }

    @Override
    public String getReleaseUri(String releaseId) {
        return "http://tsum.ru/release/" + releaseId;
    }

    @Override
    public String getCurrentReleaseUri() {
        return getReleaseUri(getCurrent().getId());
    }

    @Override
    public Release getCurrent() {
        return releaseDao.getReleaseByPipeLaunchId(this.pipeLaunch.getId());
    }

    @Override
    public void cancel(FinishCause finishCause, String reason) {
        releaseService.cancelRelease(this.pipeLaunch.getId(), finishCause, reason);
    }

    @Override
    public List<ChangelogInfo> getActualChangelog(List<ChangelogInfo> changelogInfoList) {
        return null;
    }

    @Override
    public List<ChangelogInfo> getChangelogStartingFromPreviousRunningRelease(List<ChangelogInfo> changelogInfoList) {
        return null;
    }

    @Override
    public String getProjectIsolationSecretId() {
        return null;
    }

    @Override
    public void setPrecheckState() {

    }

    @Override
    public void setInProgressState() {

    }

    @Override
    public List<Release> getOtherRunningReleases() {
        return releaseDao.getReleasesByPipeLaunchIds(Collections.emptyList());
    }

    @Override
    public void setStableRevision(String revision) {

    }

    @Override
    public <T extends VcsSettings> T getVcsSettings(Class<T> clazz) {
        return this.releaseService.getVcsSettings(this, clazz);
    }

    @Override
    public void saveChangelog(List<ChangelogEntry> changelogEntries, RepositoryType repositoryType) {
        this.releaseService.saveChangelog(this.pipeLaunch.getId(), changelogEntries, repositoryType);
    }

    @Override
    public DeliveryMachineEntity getDeliveryMachineSettings() {
        return this.releaseService.getDeliveryMachineSettings(this);
    }

    @Override
    public LaunchRuleChecker getLaunchRuleChecker() {
        return launchRuleChecker;
    }

    @Override
    public String getStableRevisionByMachineId(String id) {
        return null;
    }

    @Override
    public void setTooManyCommits(boolean isTooManyCommits) {

    }

    @Override
    public String getLocation() {
        return MultitestingDatacenterWeightService.DEFAULT_DC;
    }
}
