package ru.yandex.direct.jobs.internal;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.core.entity.crypta.repository.CryptaSegmentRepository;
import ru.yandex.direct.core.entity.internalads.model.CryptaExportType;
import ru.yandex.direct.core.entity.internalads.model.CryptaSegment;
import ru.yandex.direct.core.entity.internalads.repository.InternalCryptaSegmentsYtRepository;
import ru.yandex.direct.core.entity.retargeting.model.CryptaGoalScope;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.direct.common.db.PpcPropertyNames.CRYPTA_SEGMENTS_LAST_IMPORT_DATE;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CRYPTA_INTERESTS_UPPER_BOUND;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalWithId;
import static ru.yandex.direct.jobs.internal.ImportCryptaSegmentsJob.YT_CLUSTER;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@ParametersAreNonnullByDefault
public class ImportCryptaSegmentsJobTest {

    private static final String GROUP_ID = "group-" + RandomStringUtils.randomAlphanumeric(7);
    private static final String PSEUDO_GROUP_ID = "pseudo-group-" + RandomStringUtils.randomAlphanumeric(7);

    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Mock
    private CryptaSegmentRepository cryptaSegmentRepository;

    @Mock
    private PpcProperty<LocalDate> lastImportDateProperty;

    @Mock
    private InternalCryptaSegmentsYtRepository ytRepository;

    @InjectMocks
    private ImportCryptaSegmentsJob job;

    private LocalDate ytTableLastImportDate;
    private List<Goal> expectedGoalsForImport;

    @BeforeEach
    void initTestData() {
        MockitoAnnotations.initMocks(this);

        List<CryptaSegment> cryptaSegments = getCryptaSegment();
        doReturn(cryptaSegments)
                .when(ytRepository).getAll(YT_CLUSTER);

        Goal goal = (Goal) defaultGoalWithId(CRYPTA_INTERESTS_UPPER_BOUND, GoalType.INTERNAL)
                .withTankerNameKey("crypta_segment-age_segment_0_17_name");
        doReturn(Map.of(goal.getId(), goal))
                .when(cryptaSegmentRepository).getAll(CryptaGoalScope.INTERNAL_AD);

        expectedGoalsForImport = getExpectedGoalForImport(cryptaSegments, goal);
        doReturn(expectedGoalsForImport.size())
                .when(cryptaSegmentRepository).add(anyCollection());

        ytTableLastImportDate = LocalDate.now();
        doReturn(ytTableLastImportDate)
                .when(ytRepository).getTableGenerateDate(YT_CLUSTER);

        doReturn(lastImportDateProperty)
                .when(ppcPropertiesSupport).get(CRYPTA_SEGMENTS_LAST_IMPORT_DATE);
    }


    @Test
    void checkJob() {
        job.execute();

        verify(ytRepository).getTableGenerateDate(YT_CLUSTER);
        verify(ppcPropertiesSupport).get(CRYPTA_SEGMENTS_LAST_IMPORT_DATE);
        verify(lastImportDateProperty).get();

        verify(ytRepository).getAll(YT_CLUSTER);
        verify(cryptaSegmentRepository).getAll(CryptaGoalScope.INTERNAL_AD);

        verify(cryptaSegmentRepository).add(eq(expectedGoalsForImport));

        verify(lastImportDateProperty).set(eq(ytTableLastImportDate));
    }

    @Test
    void checkJob_WhenNotNeedImport() {
        doReturn(ytTableLastImportDate)
                .when(lastImportDateProperty).get();

        job.execute();

        verify(ytRepository).getTableGenerateDate(YT_CLUSTER);
        verify(ppcPropertiesSupport).get(CRYPTA_SEGMENTS_LAST_IMPORT_DATE);
        verify(lastImportDateProperty).get();

        verifyNoMoreInteractions(ytRepository);
        verifyZeroInteractions(cryptaSegmentRepository);
        verifyNoMoreInteractions(lastImportDateProperty);
    }

    @Test
    void checkJob_WhenNothingImport_AllRecordsFromYtAreEqualsWithAllRecordsFromMysql() {
        doReturn(listToMap(expectedGoalsForImport, Goal::getId))
                .when(cryptaSegmentRepository).getAll(CryptaGoalScope.INTERNAL_AD);
        doReturn(0)
                .when(cryptaSegmentRepository).add(anyCollection());

        job.execute();

        verify(ytRepository).getTableGenerateDate(YT_CLUSTER);
        verify(ppcPropertiesSupport).get(CRYPTA_SEGMENTS_LAST_IMPORT_DATE);
        verify(lastImportDateProperty).get();

        verify(ytRepository).getAll(YT_CLUSTER);
        verify(cryptaSegmentRepository).getAll(CryptaGoalScope.INTERNAL_AD);

        verify(cryptaSegmentRepository).add(eq(Collections.emptyList()));

        verify(lastImportDateProperty).set(eq(ytTableLastImportDate));
    }

    @Test
    void checkJob_WhenFetchedResourcesFromYt_IsEmpty() {
        doReturn(Collections.emptyList())
                .when(ytRepository).getAll(YT_CLUSTER);

        assertThatThrownBy(() -> job.execute())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("fetched records from YT can't be empty");

        verify(ytRepository).getTableGenerateDate(YT_CLUSTER);
        verify(ppcPropertiesSupport).get(CRYPTA_SEGMENTS_LAST_IMPORT_DATE);
        verify(lastImportDateProperty).get();
        verify(ytRepository).getAll(YT_CLUSTER);

        verifyNoMoreInteractions(ytRepository);
        verifyZeroInteractions(cryptaSegmentRepository);
        verifyNoMoreInteractions(lastImportDateProperty);
    }

    @Test
    void checkJob_WhenAddedRecordsCount_IsNotEqualToNumberOfNewRecords() {
        doReturn(0)
                .when(cryptaSegmentRepository).add(anyCollection());

        assertThatThrownBy(() -> job.execute())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("addedRecordsCount is not equal to number of new records=%s",
                        expectedGoalsForImport.size());
    }

    @Test
    void checkJob_WhenLastIdOfNewGoals_GreaterThanMax_CryptaInternalUpperBound() {
        Goal goal = (Goal) defaultGoalByType(GoalType.INTERNAL)
                .withId(Goal.CRYPTA_INTERNAL_UPPER_BOUND)
                .withTankerNameKey("crypta_segment-age_segment_0_18_name");
        doReturn(Map.of(goal.getId(), goal))
                .when(cryptaSegmentRepository).getAll(CryptaGoalScope.INTERNAL_AD);

        assertThatThrownBy(() -> job.execute())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("lastId=%s of new goals greater than max allowed id for internal_ad goals: %s",
                        Goal.CRYPTA_INTERNAL_UPPER_BOUND + expectedGoalsForImport.size(),
                        Goal.CRYPTA_INTERNAL_UPPER_BOUND);
    }

    @Test
    void checkExpectedData() {
        assertThat(expectedGoalsForImport)
                .hasSize(6);

        var cryptaSegmentDescriptors = mapList(expectedGoalsForImport, ImportCryptaSegmentsJob::toDescriptor);
        assertThat(cryptaSegmentDescriptors)
                .containsExactlyInAnyOrder(
                        //значения соответствуют тем, что используются в методе getCryptaSegment()
                        CryptaSegmentDescriptor.createForGroup("root_test_group"),
                        CryptaSegmentDescriptor.createForGroup(GROUP_ID),
                        CryptaSegmentDescriptor.create("segment-1", "1", "11"),
                        CryptaSegmentDescriptor.createForGroup(PSEUDO_GROUP_ID),
                        CryptaSegmentDescriptor.create(PSEUDO_GROUP_ID, "2", "22"),
                        CryptaSegmentDescriptor.create("segment-in-pseudo-group", "3", "33")
                );
    }


    private static List<CryptaSegment> getCryptaSegment() {
        return List.of(
                new CryptaSegment()
                        .withId(GROUP_ID)
                        .withParentId("root_test_group")
                        .withName("Crypta group")
                        .withExportKeywordId(null)
                        .withExportSegmentId(null)
                        .withExportType(null),
                new CryptaSegment()
                        .withId("segment-1")
                        .withParentId(GROUP_ID)
                        .withName("Crypta segment")
                        .withExportKeywordId(1L)
                        .withExportSegmentId(11L)
                        .withExportType(CryptaExportType.LONGTERM),
                new CryptaSegment()
                        .withId(PSEUDO_GROUP_ID)
                        .withParentId(GROUP_ID)
                        .withName("Crypta pseudo group")
                        .withExportKeywordId(2L)
                        .withExportSegmentId(22L)
                        .withExportType(CryptaExportType.LONGTERM),
                new CryptaSegment()
                        .withId("segment-in-pseudo-group")
                        .withParentId(PSEUDO_GROUP_ID)
                        .withName("Crypta segment in pseudo-group")
                        .withExportKeywordId(3L)
                        .withExportSegmentId(33L)
                        .withExportType(CryptaExportType.SHORTTERM)
        );
    }

    private static List<Goal> getExpectedGoalForImport(List<CryptaSegment> ytFetchedRecords, Goal mysqlGoal) {
        return ImportCryptaSegmentsJob.getNewRecordsToAdd(ytFetchedRecords, List.of(mysqlGoal));
    }

}
