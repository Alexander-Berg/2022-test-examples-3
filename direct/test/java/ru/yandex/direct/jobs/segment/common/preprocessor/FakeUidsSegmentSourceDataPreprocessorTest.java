package ru.yandex.direct.jobs.segment.common.preprocessor;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.audience.client.model.SegmentContentType;
import ru.yandex.direct.core.entity.adgroup.model.UsersSegment;
import ru.yandex.direct.jobs.segment.common.meta.SegmentKey;
import ru.yandex.direct.jobs.segment.log.SegmentSourceData;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestUserSegments.readyForCreateSegment;
import static ru.yandex.direct.jobs.segment.SegmentTestDataUtils.oneSegmentSourceData;
import static ru.yandex.direct.jobs.segment.SegmentTestDataUtils.segmentKey;
import static ru.yandex.direct.jobs.segment.common.SegmentUtils.segmentKeyExtractor;
import static ru.yandex.direct.jobs.segment.common.metrica.UploadSegmentsService.MIN_SEGMENT_SIZE;
import static ru.yandex.direct.jobs.segment.common.preprocessor.FakeUidsSegmentSourceDataPreprocessor.MAX_GAP_TO_CREATE;
import static ru.yandex.direct.jobs.segment.log.SegmentSourceData.noSourceData;
import static ru.yandex.direct.jobs.segment.log.SegmentSourceData.sourceData;

public class FakeUidsSegmentSourceDataPreprocessorTest {

    static final LocalDate TODAY = LocalDate.now();

    FakeUidsHolder fakeUidsHolder = new FakeUidsHolder();

    FakeUidsSegmentSourceDataPreprocessor preprocessor =
            new FakeUidsSegmentSourceDataPreprocessor(fakeUidsHolder);

    UsersSegment segment = readyForCreateSegment(123L);
    SegmentKey segmentKey = segmentKeyExtractor().apply(segment);

    @Test
    public void dontFailOnEmptyMeta() {
        preprocessor.preprocess(emptyList(), noSourceData(null));
    }

    @Test
    public void dontFailOnAbsentSourceData() {
        preprocessor.preprocess(singletonList(segment), noSourceData(null));
    }

    @Test
    public void addFakeUidsWhenNoRealUidsAndMetaIsOldEnough() {
        segment.setTimeCreated(TODAY.minusDays(MAX_GAP_TO_CREATE + 1).atStartOfDay());

        SegmentSourceData sourceData =
                sourceData(List.of(segmentKey), emptyMap(), TODAY, TODAY, SegmentContentType.YUID);

        sourceData = preprocessor.preprocess(singletonList(segment), sourceData);
        assertThat(sourceData.getUidsOrEmptySet(segmentKey)).hasSize(MIN_SEGMENT_SIZE);
    }

    @Test
    public void addFakeUidsWhenEmptyRealUidsAndMetaIsOldEnough() {
        segment.setTimeCreated(TODAY.minusDays(MAX_GAP_TO_CREATE + 1).atStartOfDay());

        SegmentSourceData sourceData = oneSegmentSourceData(segmentKey, emptySet());

        sourceData = preprocessor.preprocess(singletonList(segment), sourceData);
        assertThat(sourceData.getUidsOrEmptySet(segmentKey)).hasSize(MIN_SEGMENT_SIZE);
    }

    @Test
    public void dontAddFakeUidsWhenAllLogsWereNotReadAndMetaIsOldEnough() {
        segment.setTimeCreated(TODAY.minusDays(MAX_GAP_TO_CREATE + 1).atStartOfDay());

        SegmentSourceData sourceData = noSourceData(null);

        sourceData = preprocessor.preprocess(singletonList(segment), sourceData);
        assertThat(sourceData.getUidsOrEmptySet(segmentKey)).isEmpty();
    }

    @Test
    public void dontAddFakeUidsWhenSegmentLogsWereNotReadAndMetaIsOldEnough() {
        segment.setTimeCreated(TODAY.minusDays(MAX_GAP_TO_CREATE + 1).atStartOfDay());

        UsersSegment segment2 = readyForCreateSegment(124L);
        Set<BigInteger> realUids = ImmutableSet.of(BigInteger.TWO, BigInteger.TEN);
        SegmentSourceData sourceData = oneSegmentSourceData(segmentKey(segment2), realUids);

        sourceData = preprocessor.preprocess(List.of(this.segment, segment2), sourceData);
        assertThat(sourceData.getUidsOrEmptySet(segmentKey)).isEmpty();
    }

    @Test
    public void dontAddFakeUidsWhenEmptyRealUidsButMetaIsNotOldEnough() {
        segment.setTimeCreated(TODAY.minusDays(MAX_GAP_TO_CREATE).atStartOfDay());

        SegmentSourceData sourceData = oneSegmentSourceData(segmentKey, emptySet());

        sourceData = preprocessor.preprocess(singletonList(segment), sourceData);
        assertThat(sourceData.getUidsOrEmptySet(segmentKey)).isEmpty();
    }

    @Test
    public void addFakeUidsWhenNotEnoughRealUidsAndMetaIsOld() {
        segment.setTimeCreated(TODAY.minusDays(MAX_GAP_TO_CREATE + 1).atStartOfDay());

        Set<BigInteger> realUids = ImmutableSet.of(BigInteger.TWO, BigInteger.TEN);
        SegmentSourceData sourceData = oneSegmentSourceData(segmentKey, realUids);

        sourceData = preprocessor.preprocess(singletonList(segment), sourceData);
        assertThat(sourceData.getUidsOrEmptySet(segmentKey)).hasSize(MIN_SEGMENT_SIZE);
        assertThat(sourceData.getUidsOrEmptySet(segmentKey)).containsAll(realUids);
    }

    @Test
    public void addFakeUidsWhenNotEnoughRealUidsAndMetaIsNotOld() {
        segment.setTimeCreated(TODAY.minusDays(MAX_GAP_TO_CREATE).atStartOfDay());

        Set<BigInteger> realUids = ImmutableSet.of(BigInteger.TWO, BigInteger.TEN);
        SegmentSourceData sourceData = oneSegmentSourceData(segmentKey, realUids);

        sourceData = preprocessor.preprocess(singletonList(segment), sourceData);
        assertThat(sourceData.getUidsOrEmptySet(segmentKey)).hasSize(MIN_SEGMENT_SIZE);
        assertThat(sourceData.getUidsOrEmptySet(segmentKey)).containsAll(realUids);
    }

    @Test
    public void dontAddFakeUidsWhenEnoughRealUidsAndMetaIsOldEnough() {
        segment.setTimeCreated(TODAY.minusDays(MAX_GAP_TO_CREATE + 5).atStartOfDay());

        Set<BigInteger> realUids = generateUids(MIN_SEGMENT_SIZE);
        SegmentSourceData sourceData = oneSegmentSourceData(segmentKey, realUids);

        sourceData = preprocessor.preprocess(singletonList(segment), sourceData);
        assertThat(sourceData.getUidsOrEmptySet(segmentKey)).hasSize(MIN_SEGMENT_SIZE);
        assertThat(sourceData.getUidsOrEmptySet(segmentKey)).containsAll(realUids);
    }

    private Set<BigInteger> generateUids(int size) {
        return IntStream.range(0, size)
                .mapToObj(BigInteger::valueOf)
                .collect(toSet());
    }
}
