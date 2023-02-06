package ru.yandex.market.tsum.pipelines.common.jobs.issues;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.tsum.context.TestTsumJobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobInstanceBuilder;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogEntry;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogInfo;
import ru.yandex.market.tsum.pipelines.common.resources.DeliveryPipelineParams;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersion;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;
import ru.yandex.market.tsum.release.ReleaseIssueService;
import ru.yandex.market.tsum.release.dao.ReleaseDao;
import ru.yandex.market.tsum.release.dao.ReleaseService;


public class ParseTicketsListFromChangelogTest {
    private static final String RELEASE_QUEUE_NAME = "TEST";
    private static final String ANOTHER_QUEUE_NAME = "ANOTHERTEST";

    @Test
    public void testWithoutConfig() throws Exception {
        List<String> stTickets = getStTicketsWithNumber(RELEASE_QUEUE_NAME, 0, 1, 2);
        new ParseTicketsListLauncher()
            .withChanges(stTickets)
            .checkFilteredByReleaseInfo();
    }

    @Test
    public void testFilterByQueue() throws Exception {
        List<String> stTickets = getStTicketsWithNumber(RELEASE_QUEUE_NAME, 0);
        List<String> anotherStTickets = getStTicketsWithNumber(ANOTHER_QUEUE_NAME, 0);
        List<String> changes = concat(stTickets, anotherStTickets);
        List<String> queueFilter = Collections.singletonList(ANOTHER_QUEUE_NAME);

        new ParseTicketsListLauncher()
            .withChanges(changes)
            .withFiltrationType(ChangelogFiltrationType.FILTER_BY_QUEUE)
            .withQueueFilter(queueFilter)
            .checkFilteredByQueue(queueFilter);
    }

    @Test
    public void testDefaultFilter() throws Exception {
        List<String> stTickets = getStTicketsWithNumber(RELEASE_QUEUE_NAME, 0);
        List<String> anotherStTickets = getStTicketsWithNumber(ANOTHER_QUEUE_NAME, 0);
        List<String> changes = concat(stTickets, anotherStTickets);
        List<String> queueFilter = Collections.singletonList(ANOTHER_QUEUE_NAME);

        new ParseTicketsListLauncher()
            .withChanges(changes)
            .withFiltrationType(ChangelogFiltrationType.DEFAULT)
            .withQueueFilter(queueFilter)
            .checkFilteredByQueue(queueFilter);
    }

    @Test
    public void testFilterByLaunchRules() throws Exception {
        List<String> stTickets = getStTicketsWithNumber(RELEASE_QUEUE_NAME, 0, 1, 2);
        List<String> anotherStTickets = getStTicketsWithNumber(ANOTHER_QUEUE_NAME, 0, 1, 2);
        List<String> changes = concat(stTickets, anotherStTickets);

        List<String> filteredStTickets = getStTicketsWithNumber(RELEASE_QUEUE_NAME, 2);
        List<String> filteredAnotherStTickets = getStTicketsWithNumber(ANOTHER_QUEUE_NAME, 2);
        List<String> filteredChanges = concat(filteredStTickets, filteredAnotherStTickets);

        new ParseTicketsListLauncher()
            .withChanges(changes)
            .withFilteredChanges(filteredChanges)
            .withFiltrationType(ChangelogFiltrationType.FILTER_BY_LAUNCH_RULES)
            .checkFilteredByLaunchRules();
    }

    @Test
    public void testHotfix() throws Exception {
        List<String> stTickets = getStTicketsWithNumber(RELEASE_QUEUE_NAME, 0);
        List<String> anotherStTickets = getStTicketsWithNumber(ANOTHER_QUEUE_NAME, 0);
        List<String> changes = concat(stTickets, anotherStTickets);
        List<String> hotfixQueueFilter = Collections.singletonList(ANOTHER_QUEUE_NAME);

        new ParseTicketsListLauncher()
            .withChanges(changes)
            .withFiltrationType(ChangelogFiltrationType.FILTER_BY_LAUNCH_RULES)
            .withHotfix()
            .withHotfixQueueFilter(hotfixQueueFilter)
            .withHotfixFiltrationType(ChangelogFiltrationType.FILTER_BY_QUEUE)
            .checkFilteredByQueue(hotfixQueueFilter);
    }

    private List<String> getStTicketsWithNumber(String queueName, int... numbers) {
        return Arrays.stream(numbers)
            .mapToObj(n -> queueName + "-" + n)
            .collect(Collectors.toList());
    }

    private List<String> concat(List<String> list1, List<String> list2) {
        return Stream.concat(list1.stream(), list2.stream()).collect(Collectors.toList());
    }

    private static class ParseTicketsListLauncher {
        private final JobInstanceBuilder<ParseTicketsListFromChangelog> sutBuilder;
        private boolean hotfix;

        private List<String> queueFilter;
        private ChangelogFiltrationType filtrationType;
        private List<String> hotfixQueueFilter;
        private ChangelogFiltrationType hotfixFiltrationType;
        private List<String> changes;
        private List<String> filteredChanges;

        private ChangelogInfo changelogInfo;
        private ReleaseInfo releaseInfo;
        private ReleaseIssueService releaseIssueService;

        ParseTicketsListLauncher() {
            this.sutBuilder = JobInstanceBuilder.create(ParseTicketsListFromChangelog.class);
            this.hotfix = false;
        }

        public void checkFilteredByReleaseInfo() throws Exception {
            launchParseTicketsListFromChangelog();

            Mockito.verify(releaseIssueService, Mockito.atLeast(1))
                .getIssuesByChangelogInfoList(Collections.singletonList(changelogInfo), releaseInfo);
        }

        public void checkFilteredByQueue(List<String> expectedQueue) throws Exception {
            launchParseTicketsListFromChangelog();

            Mockito.verify(releaseIssueService, Mockito.atLeast(1))
                .getIssuesByChangelogInfoList(Collections.singletonList(changelogInfo), expectedQueue);
        }

        public void checkFilteredByLaunchRules() throws Exception {
            launchParseTicketsListFromChangelog();

            Mockito.verify(releaseIssueService, Mockito.atLeast(1))
                .getIssuesByChangelogEntries(changelogInfo.getFilteredChangelogEntries());
        }

        private void launchParseTicketsListFromChangelog() throws Exception {
            initReleaseInfo();
            initParseTicketsConfig();
            initParseTicketsHotfixConfig();
            initChangelogInfo();
            initDeliveryPipelineParams();

            TestTsumJobContext tsumJobContext = createTestTsumJobContext();
            sutBuilder.create().execute(tsumJobContext);
        }

        private void initReleaseInfo() {
            releaseInfo = new ReleaseInfo(new FixVersion(0, "fixVersionName"), null, RELEASE_QUEUE_NAME);

            releaseIssueService = Mockito.mock(ReleaseIssueService.class);
            Mockito.when(releaseIssueService.getIssuesByChangelogInfoList(Mockito.anyList(), Mockito.eq(releaseInfo)))
                .thenReturn(Collections.emptyList());
            Mockito.when(releaseIssueService.getIssuesByChangelogInfoList(Mockito.anyList(), Mockito.anyList()))
                .thenReturn(Collections.emptyList());
            Mockito.when(releaseIssueService.getIssuesByChangelogEntries(Mockito.anyList()))
                .thenReturn(Collections.emptyList());

            sutBuilder
                .withBean(releaseIssueService)
                .withResources(releaseInfo);
        }

        private void initParseTicketsConfig() {
            if (filtrationType == null) {
                return;
            }

            initParseTicketsConfigResource(
                Mockito.mock(ParseTicketsListFromChangelogConfig.class),
                filtrationType,
                queueFilter
            );
        }

        private void initParseTicketsHotfixConfig() {
            if (hotfixFiltrationType == null) {
                return;
            }

            initParseTicketsConfigResource(
                Mockito.mock(ParseTicketsListFromChangelogHotfixConfig.class),
                hotfixFiltrationType,
                hotfixQueueFilter
            );
        }

        private void initChangelogInfo() {
            if (changes == null) {
                changelogInfo = null;
                return;
            }
            changelogInfo = new ChangelogInfo(
                createChangelogEntries(changes),
                createChangelogEntries(filteredChanges)
            );
            sutBuilder.withResource(changelogInfo);
        }

        private void initDeliveryPipelineParams() {
            if (hotfix) {
                DeliveryPipelineParams params = Mockito.mock(DeliveryPipelineParams.class);
                Mockito.when(params.isHotfix()).thenReturn(true);
                sutBuilder.withResource(params);
            }
        }

        private TestTsumJobContext createTestTsumJobContext() {
            return new ParseTicketTestTsumJobContext(null, Mockito.mock(ReleaseDao.class), "test");
        }

        private void initParseTicketsConfigResource(
            BaseParseTicketsListFromChangelogConfig config,
            ChangelogFiltrationType filtrationType,
            List<String> queueFilter
        ) {
            Mockito.when(config.getQueueFilter()).thenReturn(queueFilter);
            Mockito.when(config.getChangelogFiltrationType()).thenReturn(filtrationType);
            sutBuilder.withResource(config);
        }

        private List<ChangelogEntry> createChangelogEntries(List<String> changes) {
            if (changes == null) {
                return Collections.emptyList();
            }
            return changes.stream()
                .map(change -> new ChangelogEntry("0", change))
                .collect(Collectors.toList());
        }

        public ParseTicketsListLauncher withQueueFilter(List<String> queueFilter) {
            this.queueFilter = queueFilter;
            return this;
        }

        public ParseTicketsListLauncher withFiltrationType(
            ChangelogFiltrationType filtrationType
        ) {
            this.filtrationType = filtrationType;
            return this;
        }

        public ParseTicketsListLauncher withHotfixQueueFilter(List<String> hotfixQueueFilter) {
            this.hotfixQueueFilter = hotfixQueueFilter;
            return this;
        }

        public ParseTicketsListLauncher withHotfixFiltrationType(
            ChangelogFiltrationType hotfixFiltrationType
        ) {
            this.hotfixFiltrationType = hotfixFiltrationType;
            return this;
        }

        public ParseTicketsListLauncher withChanges(List<String> changes) {
            this.changes = changes;
            return this;
        }

        public ParseTicketsListLauncher withFilteredChanges(List<String> filteredChanges) {
            this.filteredChanges = filteredChanges;
            return this;
        }

        public ParseTicketsListLauncher withHotfix() {
            this.hotfix = true;
            return this;
        }
    }

    private static class ParseTicketTestTsumJobContext extends TestTsumJobContext {
        ParseTicketTestTsumJobContext(ReleaseService releaseService, ReleaseDao releaseDao, String user) {
            super(releaseService, releaseDao, user);
        }

        @Override
        public List<ChangelogInfo> getChangelogStartingFromPreviousRunningRelease(
            List<ChangelogInfo> changelogInfoList
        ) {
            return changelogInfoList;
        }
    }
}
