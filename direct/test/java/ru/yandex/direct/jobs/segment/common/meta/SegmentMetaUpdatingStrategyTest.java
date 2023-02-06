package ru.yandex.direct.jobs.segment.common.meta;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.audience.client.model.SegmentContentType;
import ru.yandex.direct.core.entity.adgroup.model.ExternalAudienceStatus;
import ru.yandex.direct.core.entity.adgroup.model.InternalStatus;
import ru.yandex.direct.core.entity.adgroup.model.UsersSegment;
import ru.yandex.direct.core.entity.userssegments.repository.UsersSegmentRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.jobs.segment.common.result.SegmentUpdateResult;
import ru.yandex.direct.jobs.segment.common.result.SegmentUploadResult;
import ru.yandex.direct.jobs.segment.log.SegmentSourceData;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.testing.data.TestUserSegments.readyForCreateSegment;
import static ru.yandex.direct.core.testing.data.TestUserSegments.readyForUpdateSegment;
import static ru.yandex.direct.jobs.segment.SegmentTestUtils.cloneIt;
import static ru.yandex.direct.jobs.segment.common.SegmentUtils.segmentKeyExtractor;
import static ru.yandex.direct.jobs.segment.log.SegmentSourceData.noSourceData;
import static ru.yandex.direct.jobs.segment.log.SegmentSourceData.sourceData;

@JobsTest
@ExtendWith(SpringExtension.class)
public class SegmentMetaUpdatingStrategyTest {

    static final LocalDate TODAY = LocalDate.now();

    static final Long AUDIENCE_ID = 82162138L;
    static final ExternalAudienceStatus AUDIENCE_STATUS = ExternalAudienceStatus.IS_PROCESSED;

    static final CompareStrategy COMPARE_STRATEGY = allFieldsExcept(newPath(UsersSegment.TIME_CREATED.name()));

    @Autowired
    Steps steps;

    @Autowired
    UsersSegmentRepository usersSegmentRepository;

    SegmentMetaUpdatingStrategy segmentMetaUpdatingStrategy;

    @BeforeEach
    public void before() {
        segmentMetaUpdatingStrategy = new SegmentMetaUpdatingStrategy(usersSegmentRepository);
    }

    @Test
    public void dontFailOnEmptyMeta() {
        segmentMetaUpdatingStrategy.updateMeta(1, emptyList(), noSourceData(null), new SegmentUpdateResult());
    }

    @Test
    public void dontFailOnAbsentSourceData() {
        segmentMetaUpdatingStrategy.updateMeta(1, singletonList(readyForCreateSegment(Long.MAX_VALUE)),
                noSourceData(null), new SegmentUpdateResult());
    }

    @Test
    public void dontFailOnEmptySourceData() {
        UsersSegment segment = readyForCreateSegment(Long.MAX_VALUE);
        segmentMetaUpdatingStrategy.updateMeta(1, singletonList(readyForCreateSegment(Long.MAX_VALUE)),
                sourceData(List.of(segmentKey(segment)), emptyMap(), TODAY, TODAY, SegmentContentType.YUID),
                new SegmentUpdateResult());
    }

    @Test
    public void dontFailOnEmptyUidsInSourceData() {
        UsersSegment segment = readyForCreateSegment(Long.MAX_VALUE);
        segmentMetaUpdatingStrategy.updateMeta(1, singletonList(segment),
                oneSegmentSourceData(segmentKey(segment), emptySet(), TODAY), new SegmentUpdateResult());
    }

    @Test
    public void updateLastReadLogDateForSegmentWithoutNewData() {
        LocalDate lastReadLogDate = TODAY.minusDays(2);
        LocalDate mostFreshLogDate = TODAY.minusDays(1);

        AdGroupInfo adGroupInfo = createAdGroup();
        UsersSegment segment = createSegmentWithExistingSegment(adGroupInfo, TODAY.minusDays(4));
        SegmentSourceData sourceData =
                sourceData(List.of(segmentKey(segment)), emptyMap(),
                        lastReadLogDate, mostFreshLogDate, SegmentContentType.YUID);

        UsersSegment expectedSegment = cloneIt(segment)
                .withLastSuccessUpdateTime(lastReadLogDate.atStartOfDay());

        segmentMetaUpdatingStrategy.updateMeta(adGroupInfo.getShard(), singletonList(segment),
                sourceData, new SegmentUpdateResult());

        UsersSegment actualSegment = usersSegmentRepository
                .getSegments(adGroupInfo.getShard(), singleton(adGroupInfo.getAdGroupId())).get(0);
        assertThat(actualSegment, beanDiffer(expectedSegment).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void updateLastReadLogDateForSegmentWithEmptyNewData() {
        LocalDate lastReadLogDate = TODAY.minusDays(2);
        LocalDate mostFreshLogDate = TODAY.minusDays(1);

        AdGroupInfo adGroupInfo = createAdGroup();
        UsersSegment segment = createSegmentWithExistingSegment(adGroupInfo, TODAY.minusDays(4));
        SegmentSourceData sourceData =
                oneSegmentSourceData(segmentKey(segment), emptySet(), lastReadLogDate, mostFreshLogDate);

        UsersSegment expectedSegment = cloneIt(segment)
                .withLastSuccessUpdateTime(lastReadLogDate.atStartOfDay());

        segmentMetaUpdatingStrategy.updateMeta(adGroupInfo.getShard(), singletonList(segment),
                sourceData, new SegmentUpdateResult());

        UsersSegment actualSegment = usersSegmentRepository
                .getSegments(adGroupInfo.getShard(), singleton(adGroupInfo.getAdGroupId())).get(0);
        assertThat(actualSegment, beanDiffer(expectedSegment).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void dontUpdateLastReadLogDateForNotFetchedSegment() {
        LocalDate lastReadLogDate = TODAY.minusDays(2);
        LocalDate mostFreshLogDate = TODAY.minusDays(1);

        AdGroupInfo adGroupInfo1 = createAdGroup();
        UsersSegment segment1 = createSegmentWithExistingSegment(adGroupInfo1, TODAY.minusDays(4));

        AdGroupInfo adGroupInfo2 = createAdGroup();
        UsersSegment segment2 = createSegmentWithExistingSegment(adGroupInfo2, TODAY.minusDays(4));
        SegmentSourceData sourceData =
                oneSegmentSourceData(segmentKey(segment1), emptySet(), lastReadLogDate, mostFreshLogDate);

        UsersSegment expectedSegment1 = cloneIt(segment1)
                .withLastSuccessUpdateTime(lastReadLogDate.atStartOfDay());

        UsersSegment expectedSegment2 = cloneIt(segment2);

        segmentMetaUpdatingStrategy.updateMeta(adGroupInfo1.getShard(), List.of(segment1, segment2),
                sourceData, new SegmentUpdateResult());

        UsersSegment actualSegment1 = usersSegmentRepository
                .getSegments(adGroupInfo1.getShard(), singleton(adGroupInfo1.getAdGroupId())).get(0);
        assertThat(actualSegment1, beanDiffer(expectedSegment1).useCompareStrategy(COMPARE_STRATEGY));

        UsersSegment actualSegment2 = usersSegmentRepository
                .getSegments(adGroupInfo2.getShard(), singleton(adGroupInfo2.getAdGroupId())).get(0);
        assertThat(actualSegment2, beanDiffer(expectedSegment2).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void dontUpdateLastReadLogDateForSegmentWithoutNewDataWhenDateInDatabaseIsMoreFresh() {
        LocalDate lastReadLogDate = TODAY.minusDays(4);

        AdGroupInfo adGroupInfo = createAdGroup();
        UsersSegment segment = createSegmentWithExistingSegment(adGroupInfo, TODAY.minusDays(3));
        SegmentSourceData sourceData = oneSegmentSourceData(segmentKey(segment), emptySet(), lastReadLogDate);

        UsersSegment expectedSegment = cloneIt(segment);

        segmentMetaUpdatingStrategy.updateMeta(adGroupInfo.getShard(), singletonList(segment),
                sourceData, new SegmentUpdateResult());

        UsersSegment actualSegment = usersSegmentRepository
                .getSegments(adGroupInfo.getShard(), singleton(adGroupInfo.getAdGroupId())).get(0);
        assertThat(actualSegment, beanDiffer(expectedSegment).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void updateMetaForSuccessfullyCreatedSegment() {
        LocalDate lastReadLogDateInDb = TODAY.minusDays(3);
        AdGroupInfo adGroupInfo = createAdGroup();
        UsersSegment segment = createSegmentWithUnexistingSegment(adGroupInfo, lastReadLogDateInDb);

        LocalDate lastReadLogDate = TODAY.minusDays(2);
        SegmentSourceData sourceData =
                oneSegmentSourceData(segmentKey(segment), singleton(BigInteger.ONE), lastReadLogDate);

        SegmentUpdateResult updateResult = new SegmentUpdateResult();
        SegmentUploadResult uploadResult = new SegmentUploadResult(adGroupInfo.getUid(), AUDIENCE_ID, AUDIENCE_STATUS);
        updateResult.segmentUploadedSuccessfully(segmentKey(segment), uploadResult);

        UsersSegment expectedSegment = cloneIt(segment)
                .withSegmentOwnerUid(adGroupInfo.getUid())
                .withErrorCount(0L)
                .withExternalAudienceId(AUDIENCE_ID)
                .withExternalAudienceStatus(AUDIENCE_STATUS)
                .withInternalStatus(InternalStatus.COMPLETE)
                .withLastSuccessUpdateTime(lastReadLogDate.atStartOfDay());

        segmentMetaUpdatingStrategy.updateMeta(adGroupInfo.getShard(), singletonList(segment),
                sourceData, updateResult);

        UsersSegment actual = usersSegmentRepository
                .getSegments(adGroupInfo.getShard(), singleton(adGroupInfo.getAdGroupId())).get(0);
        assertThat(actual, beanDiffer(expectedSegment).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void updateMetaForSuccessfullyUpdatedSegment() {
        LocalDate lastReadLogDateInDb = TODAY.minusDays(3);
        AdGroupInfo adGroupInfo = createAdGroup();
        UsersSegment segment = createSegmentWithExistingSegmentWithErrors(adGroupInfo, lastReadLogDateInDb);

        LocalDate lastReadLogDate = TODAY.minusDays(2);
        SegmentSourceData sourceData =
                oneSegmentSourceData(segmentKey(segment), singleton(BigInteger.ONE), lastReadLogDate);

        SegmentUpdateResult updateResult = new SegmentUpdateResult();
        SegmentUploadResult uploadResult = new SegmentUploadResult(adGroupInfo.getUid(), AUDIENCE_ID, AUDIENCE_STATUS);
        updateResult.segmentUploadedSuccessfully(segmentKey(segment), uploadResult);

        UsersSegment expectedSegment = cloneIt(segment)
                .withSegmentOwnerUid(adGroupInfo.getUid())
                .withErrorCount(0L)
                .withExternalAudienceId(AUDIENCE_ID)
                .withExternalAudienceStatus(AUDIENCE_STATUS)
                .withInternalStatus(InternalStatus.COMPLETE)
                .withLastSuccessUpdateTime(lastReadLogDate.atStartOfDay());

        segmentMetaUpdatingStrategy.updateMeta(adGroupInfo.getShard(), singletonList(segment),
                sourceData, updateResult);

        UsersSegment actual = usersSegmentRepository
                .getSegments(adGroupInfo.getShard(), singleton(adGroupInfo.getAdGroupId())).get(0);
        assertThat(actual, beanDiffer(expectedSegment).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void updateMetaForFailedToCreateSegment() {
        LocalDate lastReadLogDateInDb = TODAY.minusDays(4);
        AdGroupInfo adGroupInfo = createAdGroup();
        UsersSegment segment = createSegmentWithUnexistingSegment(adGroupInfo, lastReadLogDateInDb);

        LocalDate lastReadLogDate = TODAY.minusDays(2);
        SegmentSourceData sourceData =
                oneSegmentSourceData(segmentKey(segment), singleton(BigInteger.ONE), lastReadLogDate);

        SegmentUpdateResult updateResult = new SegmentUpdateResult();
        updateResult.segmentUploadFailed(segmentKey(segment));

        UsersSegment expectedSegment = cloneIt(segment)
                .withErrorCount(segment.getErrorCount() + 1)
                .withExternalAudienceStatus(ExternalAudienceStatus.IS_PROCESSED);

        segmentMetaUpdatingStrategy.updateMeta(adGroupInfo.getShard(), singletonList(segment),
                sourceData, updateResult);

        UsersSegment actual = usersSegmentRepository
                .getSegments(adGroupInfo.getShard(), singleton(adGroupInfo.getAdGroupId())).get(0);
        assertThat(actual, beanDiffer(expectedSegment).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void updateMetaForFailedToUpdateSegment() {
        LocalDate lastReadLogDateInDb = TODAY.minusDays(4);
        AdGroupInfo adGroupInfo = createAdGroup();
        UsersSegment segment = createSegmentWithExistingSegment(adGroupInfo, lastReadLogDateInDb);

        LocalDate lastReadLogDate = TODAY.minusDays(2);
        SegmentSourceData sourceData =
                oneSegmentSourceData(segmentKey(segment), singleton(BigInteger.ONE), lastReadLogDate);

        SegmentUpdateResult updateResult = new SegmentUpdateResult();
        updateResult.segmentUploadFailed(segmentKey(segment));

        UsersSegment expectedSegment = cloneIt(segment)
                .withErrorCount(segment.getErrorCount() + 1)
                .withExternalAudienceStatus(ExternalAudienceStatus.IS_PROCESSED);

        segmentMetaUpdatingStrategy.updateMeta(adGroupInfo.getShard(), singletonList(segment),
                sourceData, updateResult);

        UsersSegment actual = usersSegmentRepository
                .getSegments(adGroupInfo.getShard(), singleton(adGroupInfo.getAdGroupId())).get(0);
        assertThat(actual, beanDiffer(expectedSegment).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void mixedCase() {
        LocalDate lastReadLogDate = TODAY.minusDays(2);

        AdGroupInfo adGroupInfo1 = createAdGroup();
        UsersSegment segment1 = createSegmentWithUnexistingSegment(adGroupInfo1, TODAY.minusDays(4));
        SegmentUploadResult uploadResult1 = new SegmentUploadResult(adGroupInfo1.getUid(),
                AUDIENCE_ID, AUDIENCE_STATUS);

        AdGroupInfo adGroupInfo2 = createAdGroup();
        UsersSegment segment2 = createSegmentWithUnexistingSegment(adGroupInfo2, TODAY.minusDays(5));

        AdGroupInfo adGroupInfo3 = createAdGroup();
        UsersSegment segment3 = createSegmentWithExistingSegment(adGroupInfo3, TODAY.minusDays(3));

        AdGroupInfo adGroupInfo4 = createAdGroup();
        UsersSegment segment4 = createSegmentWithUnexistingSegment(adGroupInfo4, TODAY.minusDays(3));
        SegmentSourceData sourceData =
                sourceData(
                        List.of(segmentKey(segment1), segmentKey(segment2), segmentKey(segment3)),
                        ImmutableMap.of(
                                segmentKey(segment1), singleton(BigInteger.ONE),
                                segmentKey(segment2), singleton(BigInteger.TWO)),
                        lastReadLogDate,
                        lastReadLogDate,
                        SegmentContentType.YUID);

        SegmentUpdateResult updateResult = new SegmentUpdateResult();
        updateResult.segmentUploadedSuccessfully(segmentKey(segment1), uploadResult1);
        updateResult.segmentUploadFailed(segmentKey(segment2));

        UsersSegment expectedSegment1 = cloneIt(segment1)
                .withSegmentOwnerUid(adGroupInfo1.getUid())
                .withErrorCount(0L)
                .withExternalAudienceId(AUDIENCE_ID)
                .withExternalAudienceStatus(AUDIENCE_STATUS)
                .withInternalStatus(InternalStatus.COMPLETE)
                .withLastSuccessUpdateTime(lastReadLogDate.atStartOfDay());

        UsersSegment expectedSegment2 = cloneIt(segment2)
                .withErrorCount(segment2.getErrorCount() + 1)
                .withExternalAudienceStatus(ExternalAudienceStatus.IS_PROCESSED);

        UsersSegment expectedSegment3 = cloneIt(segment3)
                .withLastSuccessUpdateTime(lastReadLogDate.atStartOfDay());

        UsersSegment expectedSegment4 = cloneIt(segment4);

        segmentMetaUpdatingStrategy.updateMeta(adGroupInfo1.getShard(),
                asList(segment1, segment2, segment3, segment4),
                sourceData, updateResult);

        UsersSegment actual1 = usersSegmentRepository
                .getSegments(adGroupInfo1.getShard(), singleton(adGroupInfo1.getAdGroupId())).get(0);
        assertThat(actual1, beanDiffer(expectedSegment1).useCompareStrategy(COMPARE_STRATEGY));

        UsersSegment actual2 = usersSegmentRepository
                .getSegments(adGroupInfo2.getShard(), singleton(adGroupInfo2.getAdGroupId())).get(0);
        assertThat(actual2, beanDiffer(expectedSegment2).useCompareStrategy(COMPARE_STRATEGY));

        UsersSegment actual3 = usersSegmentRepository
                .getSegments(adGroupInfo3.getShard(), singleton(adGroupInfo3.getAdGroupId())).get(0);
        assertThat(actual3, beanDiffer(expectedSegment3).useCompareStrategy(COMPARE_STRATEGY));

        UsersSegment actual4 = usersSegmentRepository
                .getSegments(adGroupInfo4.getShard(), singleton(adGroupInfo4.getAdGroupId())).get(0);
        assertThat(actual4, beanDiffer(expectedSegment4).useCompareStrategy(COMPARE_STRATEGY));
    }

    AdGroupInfo createAdGroup() {
        return steps.adGroupSteps().createActiveCpmVideoAdGroup();
    }

    UsersSegment createSegmentWithExistingSegment(AdGroupInfo adGroupInfo, LocalDate lastReadLogDate) {
        UsersSegment segment = readyForUpdateSegment(adGroupInfo.getAdGroupId(), adGroupInfo.getUid())
                .withLastSuccessUpdateTime(lastReadLogDate.atStartOfDay())
                .withExternalAudienceId(AUDIENCE_ID)
                .withExternalAudienceStatus(AUDIENCE_STATUS);
        createSegment(adGroupInfo, segment);
        return segment;
    }

    UsersSegment createSegmentWithExistingSegmentWithErrors(AdGroupInfo adGroupInfo, LocalDate lastReadLogDate) {
        UsersSegment segment = readyForUpdateSegment(adGroupInfo.getAdGroupId(), adGroupInfo.getUid())
                .withLastSuccessUpdateTime(lastReadLogDate.atStartOfDay().withNano(0))
                .withErrorCount(3L);
        createSegment(adGroupInfo, segment);
        return segment;
    }

    UsersSegment createSegmentWithUnexistingSegment(AdGroupInfo adGroupInfo, LocalDate lastReadLogDate) {
        UsersSegment segment = readyForCreateSegment(adGroupInfo.getAdGroupId())
                .withLastSuccessUpdateTime(lastReadLogDate.atStartOfDay().withNano(0))
                .withErrorCount(3L);
        createSegment(adGroupInfo, segment);
        return segment;
    }

    void createSegment(AdGroupInfo adGroupInfo, UsersSegment segment) {
        usersSegmentRepository.addSegments(adGroupInfo.getShard(), singletonList(segment));
    }

    SegmentKey segmentKey(UsersSegment segment) {
        return segmentKeyExtractor().apply(segment);
    }

    SegmentSourceData oneSegmentSourceData(SegmentKey segmentKey, Set<BigInteger> uids, LocalDate lastReadLogDate) {
        return sourceData(List.of(segmentKey), Map.of(segmentKey, uids),
                lastReadLogDate, lastReadLogDate, SegmentContentType.YUID);
    }

    SegmentSourceData oneSegmentSourceData(SegmentKey segmentKey, Set<BigInteger> uids,
                                           LocalDate lastReadLogDate, LocalDate mostFreshLogDate) {
        return sourceData(List.of(segmentKey), Map.of(segmentKey, uids),
                lastReadLogDate, mostFreshLogDate, SegmentContentType.YUID);
    }
}
