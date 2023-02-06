package ru.yandex.direct.jobs.segment.common.target;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.audience.client.model.SegmentContentType;
import ru.yandex.direct.core.entity.adgroup.model.AdShowType;
import ru.yandex.direct.core.entity.adgroup.model.UsersSegment;
import ru.yandex.direct.jobs.segment.common.meta.SegmentKey;
import ru.yandex.direct.jobs.segment.common.result.SegmentUpdateResult;
import ru.yandex.direct.jobs.segment.common.result.SegmentUploadResult;
import ru.yandex.direct.jobs.segment.log.SegmentSourceData;

import static java.math.BigInteger.TEN;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.adgroup.model.ExternalAudienceStatus.DELETED;
import static ru.yandex.direct.core.entity.adgroup.model.ExternalAudienceStatus.IS_PROCESSED;
import static ru.yandex.direct.core.testing.data.TestUserSegments.defaultSegment;
import static ru.yandex.direct.jobs.segment.SegmentTestDataUtils.oneSegmentSourceData;
import static ru.yandex.direct.jobs.segment.SegmentTestDataUtils.segmentKey;
import static ru.yandex.direct.jobs.segment.common.SegmentUtils.segmentKeyExtractor;
import static ru.yandex.direct.jobs.segment.log.SegmentSourceData.noSourceData;
import static ru.yandex.direct.jobs.segment.log.SegmentSourceData.sourceData;

public class SegmentTargetUpdatingStrategyTest {

    static final Long ADGROUP_ID_1 = 828232L;
    static final Long ADGROUP_ID_2 = 921392L;

    static final Long UID_1 = 123L;
    static final Long UID_2 = 789L;

    static final Long AUDIENCE_ID_1 = 2492138L;
    static final Long AUDIENCE_ID_2 = 9128934L;

    Map<SegmentKey, SegmentUploadResult> segmentKeyToUploadResult = new HashMap<>();
    Set<SegmentKey> segmentKeyToThrowExceptionOnUpload = new HashSet<>();

    YaAudienceSegmentUploadStrategy yaAudienceSegmentUploadStrategy;

    SegmentTargetUpdatingStrategy segmentTargetUpdatingStrategy;

    @BeforeEach
    public void before() throws Exception {
        yaAudienceSegmentUploadStrategy = mock(YaAudienceSegmentUploadStrategy.class);

        when(yaAudienceSegmentUploadStrategy.upload(any(), any(), any()))
                .thenAnswer(invocation -> {
                    UsersSegment segment = invocation.getArgument(0);
                    SegmentKey segmentKey = segmentKeyExtractor().apply(segment);
                    if (segmentKeyToThrowExceptionOnUpload.contains(segmentKey)) {
                        throw new TimeoutException();
                    }
                    return segmentKeyToUploadResult.get(segmentKey);
                });

        segmentTargetUpdatingStrategy =
                new SegmentTargetUpdatingStrategy(yaAudienceSegmentUploadStrategy);
    }

    @Test
    public void noMeta() {
        SegmentUpdateResult expectedResult = new SegmentUpdateResult();
        SegmentUpdateResult actualResult = segmentTargetUpdatingStrategy
                .updateTarget(emptyList(), noSourceData(null));

        checkResult(expectedResult, actualResult);
    }

    @Test
    public void absentSourceData() {
        UsersSegment segment = defaultSegment(ADGROUP_ID_1, AdShowType.START);

        SegmentUpdateResult expectedResult = new SegmentUpdateResult();
        SegmentUpdateResult actualResult = segmentTargetUpdatingStrategy
                .updateTarget(singletonList(segment), noSourceData(null));

        checkResult(expectedResult, actualResult);
    }

    @Test
    public void oneSegmentWithoutUids() {
        UsersSegment segment = defaultSegment(ADGROUP_ID_1, AdShowType.START);
        SegmentSourceData sourceData = oneSegmentSourceData(segmentKey(segment), emptySet());

        SegmentUploadResult uploadResult = new SegmentUploadResult(UID_1, AUDIENCE_ID_1, IS_PROCESSED);
        mockUploadResultForSegment(segment, uploadResult);

        SegmentUpdateResult expectedResult = new SegmentUpdateResult();
        SegmentUpdateResult actualResult = segmentTargetUpdatingStrategy
                .updateTarget(singletonList(segment), sourceData);

        checkResult(expectedResult, actualResult);
    }

    @Test
    public void oneSegmentWithUids() {
        UsersSegment segment = defaultSegment(ADGROUP_ID_1, AdShowType.START);
        SegmentSourceData sourceData = oneSegmentSourceData(segmentKey(segment), singleton(BigInteger.ONE));

        SegmentUploadResult uploadResult = new SegmentUploadResult(UID_1, AUDIENCE_ID_1, IS_PROCESSED);
        mockUploadResultForSegment(segment, uploadResult);

        SegmentUpdateResult expectedResult = resultWithSuccessfulUpload(segment, uploadResult);
        SegmentUpdateResult actualResult = segmentTargetUpdatingStrategy
                .updateTarget(singletonList(segment), sourceData);

        checkResult(expectedResult, actualResult);
    }

    @Test
    public void oneSegmentWithUidsAndOneWithout() {
        UsersSegment segment1 = defaultSegment(ADGROUP_ID_1, AdShowType.START);
        UsersSegment segment2 = defaultSegment(ADGROUP_ID_2, AdShowType.COMPLETE);

        SegmentUploadResult uploadResult1 = new SegmentUploadResult(UID_1, AUDIENCE_ID_1, IS_PROCESSED);
        SegmentUploadResult uploadResult2 = new SegmentUploadResult(UID_2, AUDIENCE_ID_2, DELETED);
        mockUploadResultForSegment(segment1, uploadResult1);
        mockUploadResultForSegment(segment2, uploadResult2);

        SegmentSourceData sourceData = oneSegmentSourceData(segmentKey(segment2), singleton(TEN));

        SegmentUpdateResult expectedResult = new SegmentUpdateResult();
        addSuccessfulUploadToResult(expectedResult, segment2, uploadResult2);

        SegmentUpdateResult actualResult = segmentTargetUpdatingStrategy
                .updateTarget(asList(segment1, segment2), sourceData);

        checkResult(expectedResult, actualResult);
    }

    @Test
    public void oneSegmentUploadFailed() {
        UsersSegment segment = defaultSegment(ADGROUP_ID_1, AdShowType.START);

        SegmentSourceData sourceData = oneSegmentSourceData(segmentKey(segment), singleton(BigInteger.ONE));
        mockExceptionOnUploadForSegment(segment);

        SegmentUpdateResult expectedResult = new SegmentUpdateResult();
        addFailedUploadToResult(expectedResult, segment);

        SegmentUpdateResult actualResult = segmentTargetUpdatingStrategy
                .updateTarget(singletonList(segment), sourceData);

        checkResult(expectedResult, actualResult);
    }

    @Test
    public void oneSegmentUploadSuccessfulAndOneFailed() {
        UsersSegment segment1 = defaultSegment(ADGROUP_ID_1, AdShowType.START);
        UsersSegment segment2 = defaultSegment(ADGROUP_ID_2, AdShowType.COMPLETE);

        SegmentUploadResult uploadResult2 = new SegmentUploadResult(UID_2, AUDIENCE_ID_2, DELETED);
        mockUploadResultForSegment(segment2, uploadResult2);
        mockExceptionOnUploadForSegment(segment1);

        SegmentSourceData sourceData = sourceData(
                List.of(segmentKey(segment1), segmentKey(segment2)),
                Map.of(segmentKey(segment1), singleton(BigInteger.ONE),
                        segmentKey(segment2), singleton(TEN)),
                LocalDate.now(),
                LocalDate.now(),
                SegmentContentType.YUID);

        SegmentUpdateResult expectedResult = new SegmentUpdateResult();
        addFailedUploadToResult(expectedResult, segment1);
        addSuccessfulUploadToResult(expectedResult, segment2, uploadResult2);

        SegmentUpdateResult actualResult = segmentTargetUpdatingStrategy
                .updateTarget(asList(segment1, segment2), sourceData);

        checkResult(expectedResult, actualResult);
    }

    SegmentUpdateResult resultWithSuccessfulUpload(UsersSegment segment, SegmentUploadResult uploadResult) {
        SegmentUpdateResult updateResult = new SegmentUpdateResult();
        addSuccessfulUploadToResult(updateResult, segment, uploadResult);
        return updateResult;
    }

    void addSuccessfulUploadToResult(SegmentUpdateResult segmentUpdateResult,
                                     UsersSegment segment,
                                     SegmentUploadResult uploadResult) {
        SegmentKey segmentKey = segmentKeyExtractor().apply(segment);
        segmentUpdateResult.segmentUploadedSuccessfully(segmentKey, uploadResult);
    }

    void addFailedUploadToResult(SegmentUpdateResult segmentUpdateResult,
                                 UsersSegment segment) {
        SegmentKey segmentKey = segmentKeyExtractor().apply(segment);
        segmentUpdateResult.segmentUploadFailed(segmentKey);
    }

    void mockUploadResultForSegment(UsersSegment segment, SegmentUploadResult segmentUploadResult) {
        segmentKeyToUploadResult.put(segmentKeyExtractor().apply(segment), segmentUploadResult);
    }

    void mockExceptionOnUploadForSegment(UsersSegment segment) {
        segmentKeyToThrowExceptionOnUpload.add(segmentKeyExtractor().apply(segment));
    }

    void checkResult(SegmentUpdateResult expectedResult, SegmentUpdateResult actualResult) {
        assertThat(actualResult.getSegmentKeyToUploadResult())
                .hasSize(expectedResult.getSegmentKeyToUploadResult().size());
        assertThat(actualResult.getSegmentKeyToUploadResult())
                .containsAllEntriesOf(expectedResult.getSegmentKeyToUploadResult());

        assertThat(actualResult.getSegmentKeysOfFailedUploads())
                .hasSize(expectedResult.getSegmentKeysOfFailedUploads().size());
        assertThat(actualResult.getSegmentKeysOfFailedUploads())
                .containsExactlyElementsOf(expectedResult.getSegmentKeysOfFailedUploads());
    }
}
