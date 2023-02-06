package ru.yandex.market.tsum.release;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.market.tsum.core.notify.common.startrek.ReleaseType;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipelines.common.jobs.github.CreateReleaseBranchJob;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersion;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseTypeResource;
import ru.yandex.market.tsum.pipelines.test_data.TestQueueBuilder;
import ru.yandex.market.tsum.pipelines.test_data.TestVersionBuilder;
import ru.yandex.startrek.client.Queues;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.Versions;
import ru.yandex.startrek.client.model.Queue;
import ru.yandex.startrek.client.model.QueueRef;
import ru.yandex.startrek.client.model.Version;
import ru.yandex.startrek.client.model.VersionCreate;
import ru.yandex.startrek.client.model.VersionRef;

@RunWith(Parameterized.class)
public class FixVersionServiceTest {
    private static final String QUEUE = "MARKETCHECKOUT";
    @Mock
    private Queues queues;
    @Mock
    private Versions versions;
    @Mock
    private Session session;
    @InjectMocks
    private FixVersionService fixVersionService;

    // parameters
    private final String inputName;
    private final boolean checkForExistingVersion;
    private final String createdName;
    private final List<String> versionNames;


    public FixVersionServiceTest(String inputName, boolean checkForExistingVersion, String createdName,
                                 List<String> versionNames) {
        this.inputName = inputName;
        this.checkForExistingVersion = checkForExistingVersion;
        this.createdName = createdName;
        this.versionNames = versionNames;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(session.versions()).thenReturn(versions);
        Mockito.when(session.queues()).thenReturn(queues);
    }

    @Parameterized.Parameters(name = "input: {0}, expected: {1}, versions: {2}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(
            new Object[]{"asdasd", true, "2017.3.1 asdasd", Collections.emptyList()},
            new Object[]{"asdasd2", true, "2017.3.2 asdasd2", Collections.singletonList("2017.3.1")},
            new Object[]{"asdasd3", true, "2017.3.6 asdasd3", Arrays.asList("2017.3.1", "2017.3.5")},
            new Object[]{"asdasd", true, "2017.3.1 asdasd", Arrays.asList("2017.3.1 asdasd", "2017.3.4 asdsasd4")},
            new Object[]{"asdasd", true, "2017.3.1 asdasd", Arrays.asList("2017.3.1 asdasd   ", "2017.3.4 asdsasd4")},
            new Object[]{"asdasd", false, "2017.3.5 asdasd", Arrays.asList("2017.3.1 asdasd", "2017.3.4 asdsasd4")}
        );
    }

    private static IteratorF<Version> createVersionIterator(List<String> versionNames) {
        return Cf.toArrayList(versionNames)
            .iterator()
            .map(vn -> TestVersionBuilder.aVersion()
                .withName(vn)
                .build());
    }


    @Test
    public void shouldCreateExpectedVersion() {
        // setup
        mockCreateVersion();
        mockGetVersions();
        mockGetQueue();

        injectFirstOfJuly();

        // when
        Version version = fixVersionService.getOrCreateReleaseVersion(
            QUEUE, inputName, checkForExistingVersion, new TestJobContext()
        );

        // then
        Assert.assertEquals(createdName, version.getName().trim());
    }

    private void mockGetVersions() {
        Mockito.when(versions.getAll(Mockito.any(QueueRef.class))).thenReturn(createVersionIterator(versionNames));
    }

    private void injectFirstOfJuly() {
        Instant firstOfJuly = LocalDateTime.of(2017, Month.JULY, 1, 10, 0, 0)
            .toInstant(ZoneOffset.UTC);
        fixVersionService.setClock(Clock.fixed(firstOfJuly, ZoneOffset.UTC));
    }

    private void mockCreateVersion() {
        Mockito.when(versions.create(Mockito.any(VersionCreate.class)))
            .then(inv -> {
                VersionCreate versionCreate = (VersionCreate) inv.getArguments()[0];
                return TestVersionBuilder.aVersion().withName(versionCreate.getName()).build();
            });
    }

    private void mockGetQueue() {
        Mockito.when(queues.get(QUEUE))
            .then(inv -> TestQueueBuilder.aQueue().build());
    }

    public static class GetVersionTests {
        @Mock
        private Queues queues;
        @Mock
        private Versions versions;
        @Mock
        private Session session;
        @Mock
        private Queue queue;
        @Mock
        ru.yandex.startrek.client.model.Version startrekVersion;
        @InjectMocks
        private FixVersionService fixVersionService;

        @Before
        public void setUp() {
            MockitoAnnotations.initMocks(this);
            Mockito.when(session.versions()).thenReturn(versions);
            Mockito.when(session.queues()).thenReturn(queues);
            Mockito.when(queue.getKey()).thenReturn(QUEUE);
        }

        private void mockGetVersions(List<String> versionNames) {
            Mockito.when(queue.getVersions()).thenReturn(createVersionRefIterator(versionNames));
        }

        private static IteratorF<VersionRef> createVersionRefIterator(List<String> versionNames) {
            return Cf.toArrayList(versionNames)
                .iterator()
                .map(vn -> {
                    VersionRef versionRef = Mockito.mock(VersionRef.class);
                    Version version = Mockito.mock(Version.class);
                    Mockito.when(version.isArchived()).thenReturn(false);
                    Mockito.when(version.getName()).thenReturn(vn);
                    Mockito.when(versionRef.getDisplay()).thenReturn(vn);
                    Mockito.when(versionRef.load()).thenReturn(version);
                    return versionRef;
                });
        }

        @Test
        public void checkCreateBranchByVersion() {
            //setup
            Instant firstOfJuly = LocalDateTime.of(2017, Month.JULY, 1, 10, 0, 0)
                .toInstant(ZoneOffset.UTC);
            fixVersionService.setClock(Clock.fixed(firstOfJuly, ZoneOffset.UTC));
            Mockito.when(queues.get(QUEUE)).then(inv -> queue);
            Mockito.when(versions.getAll((QueueRef) Mockito.any())).then(inv -> Cf.arrayList().iterator());
            Mockito.when(versions.create(Mockito.any())).then(inv -> {
                VersionCreate vc = inv.getArgument(0);
                Mockito.when(startrekVersion.getDescription()).then(o -> vc.getDescription());
                Mockito.when(startrekVersion.getName()).then(o -> vc.getName());
                Mockito.when(startrekVersion.getId()).then(o -> 123L);
                return startrekVersion;
            });

            //when
            Version version = fixVersionService.getOrCreateReleaseVersion(
                QUEUE, null, false, new TestJobContext()
            );

            String branchName = CreateReleaseBranchJob.createBranchName(
                new ReleaseTypeResource(ReleaseType.RELEASE),
                new ReleaseInfo(FixVersion.fromVersion(version), "MARKETCHECKOUT-1234")
            );

            //then
            Assert.assertEquals("release/2017.3.1_MARKETCHECKOUT-1234", branchName);
        }

        @Test
        public void getsVersion() {
            String testName = "test name";
            // setup
            mockGetVersions(Arrays.asList(testName, "second test name"));
            Mockito.when(queues.get(QUEUE)).then(inv -> queue);

            // when
            Version version = fixVersionService.getVersion(QUEUE, testName);

            // then
            Assert.assertEquals(testName, version.getName().trim());
        }

        @Test
        public void getsVersionWithSpacesOnEnd() {
            String testName = "test name";
            // setup
            mockGetVersions(Arrays.asList(testName + " ", "second test name"));
            Mockito.when(queues.get(QUEUE)).then(inv -> queue);

            // when
            Version version = fixVersionService.getVersion(QUEUE, testName);

            // then
            Assert.assertEquals(testName, version.getName().trim());
        }
    }

    @RunWith(Parameterized.class)
    public static class QuarterFromMonthTests {
        private final Month month;
        private final int quarter;


        public QuarterFromMonthTests(Month month, int quarter) {
            this.month = month;
            this.quarter = quarter;
        }

        @Parameterized.Parameters(name = "Case: month: {0} - quarter: {1}")
        public static List<Object[]> parameters() {
            return Arrays.asList(
                new Object[]{Month.JANUARY, 1},
                new Object[]{Month.FEBRUARY, 1},
                new Object[]{Month.MARCH, 1},
                new Object[]{Month.APRIL, 2},
                new Object[]{Month.MAY, 2},
                new Object[]{Month.JUNE, 2},
                new Object[]{Month.JULY, 3},
                new Object[]{Month.AUGUST, 3},
                new Object[]{Month.SEPTEMBER, 3},
                new Object[]{Month.OCTOBER, 4},
                new Object[]{Month.NOVEMBER, 4},
                new Object[]{Month.DECEMBER, 4}
            );
        }

        @Test
        public void runQuarterFromMonth() {
            Assert.assertEquals(quarter, FixVersionService.getQuarterFromMonth(month));
        }
    }

}
