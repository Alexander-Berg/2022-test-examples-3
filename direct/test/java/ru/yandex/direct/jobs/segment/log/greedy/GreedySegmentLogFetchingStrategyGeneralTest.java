package ru.yandex.direct.jobs.segment.log.greedy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.adgroup.model.AdShowType;
import ru.yandex.direct.core.entity.adgroup.model.UsersSegment;
import ru.yandex.direct.jobs.segment.SegmentTestDataUtils;
import ru.yandex.direct.jobs.segment.common.meta.SegmentKey;
import ru.yandex.direct.jobs.segment.log.IntermediateSegmentYtRepositoryMock;
import ru.yandex.direct.jobs.segment.log.SegmentSourceData;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.TWO;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.jobs.segment.SegmentTestDataUtils.segmentKey;

public class GreedySegmentLogFetchingStrategyGeneralTest {

    private static final LocalDate TODAY = LocalDate.now();
    private static final long PACK_LIMIT = 5;

    private IntermediateSegmentYtRepositoryMock repository;

    private GreedySegmentLogFetchingStrategy strategy;

    @BeforeEach
    public void beforeEach() {
        repository = new IntermediateSegmentYtRepositoryMock();
        strategy = new GreedySegmentLogFetchingStrategy(repository, () -> PACK_LIMIT);
    }

    @Test
    public void oneSegmentWithDataInOneDay() {
        UsersSegment segmentMeta = SegmentTestDataUtils.usersSegment(TODAY.minusDays(2));
        SegmentKey segmentKey = segmentKey(segmentMeta);
        repository.putData(TODAY.minusDays(1), segmentKey, List.of(ONE, TWO));

        SegmentSourceData segmentSourceData = strategy.fetch(List.of(segmentMeta));
        assertThat(segmentSourceData.getFetchedSegmentsKeys())
                .containsExactlyInAnyOrder(segmentKey);
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey))
                .containsExactlyInAnyOrder(ONE, TWO);
    }

    @Test
    public void oneSegmentWithoutDataInOneDay() {
        UsersSegment segmentMeta = SegmentTestDataUtils.usersSegment(123L, TODAY.minusDays(2));
        SegmentKey segmentKey = segmentKey(segmentMeta);
        repository.putData(TODAY.minusDays(1), new SegmentKey(124L, AdShowType.START), List.of(ONE, TWO));

        SegmentSourceData segmentSourceData = strategy.fetch(List.of(segmentMeta));
        assertThat(segmentSourceData.getFetchedSegmentsKeys())
                .containsExactlyInAnyOrder(segmentKey);
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey)).isNullOrEmpty();
    }

    @Test
    public void oneSegmentWithDataInSeveralDays() {
        UsersSegment segmentMeta = SegmentTestDataUtils.usersSegment(TODAY.minusDays(3));
        SegmentKey segmentKey = segmentKey(segmentMeta);
        repository.putData(TODAY.minusDays(2), segmentKey, List.of(ONE, TWO));
        repository.putData(TODAY.minusDays(1), segmentKey, List.of(TWO, TEN));

        SegmentSourceData segmentSourceData = strategy.fetch(List.of(segmentMeta));
        assertThat(segmentSourceData.getFetchedSegmentsKeys())
                .containsExactlyInAnyOrder(segmentKey);
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey))
                .containsExactlyInAnyOrder(ONE, TWO, TEN);
    }

    @Test
    public void severalSegmentWithSameUpdateDateWithData() {
        UsersSegment segmentMeta1 = SegmentTestDataUtils.usersSegment(123L, TODAY.minusDays(3));
        SegmentKey segmentKey1 = segmentKey(segmentMeta1);
        repository.putData(TODAY.minusDays(2), segmentKey1, List.of(ONE, TWO));
        repository.putData(TODAY.minusDays(1), segmentKey1, List.of(TWO, TEN));

        UsersSegment segmentMeta2 = SegmentTestDataUtils.usersSegment(124L, TODAY.minusDays(3));
        SegmentKey segmentKey2 = segmentKey(segmentMeta2);
        repository.putData(TODAY.minusDays(1), segmentKey2, List.of(TEN));

        SegmentSourceData segmentSourceData = strategy.fetch(List.of(segmentMeta1, segmentMeta2));
        assertThat(segmentSourceData.getFetchedSegmentsKeys())
                .containsExactlyInAnyOrder(segmentKey1, segmentKey2);
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey1))
                .containsExactlyInAnyOrder(ONE, TWO, TEN);
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey2))
                .containsExactlyInAnyOrder(TEN);
    }

    @Test
    public void severalSegmentWithSameUpdateDateWithAndWithoutData() {
        UsersSegment segmentMeta1 = SegmentTestDataUtils.usersSegment(123L, TODAY.minusDays(3));
        SegmentKey segmentKey1 = segmentKey(segmentMeta1);
        repository.putData(TODAY.minusDays(2), segmentKey1, List.of(ONE, TWO));
        repository.putData(TODAY.minusDays(1), segmentKey1, List.of(TWO, TEN));

        UsersSegment segmentMeta2 = SegmentTestDataUtils.usersSegment(124L, TODAY.minusDays(3));
        SegmentKey segmentKey2 = segmentKey(segmentMeta2);

        SegmentSourceData segmentSourceData = strategy.fetch(List.of(segmentMeta1, segmentMeta2));
        assertThat(segmentSourceData.getFetchedSegmentsKeys())
                .containsExactlyInAnyOrder(segmentKey1, segmentKey2);
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey1))
                .containsExactlyInAnyOrder(ONE, TWO, TEN);
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey2))
                .isNullOrEmpty();
    }

    @Test
    public void severalSegmentWithDifferentUpdateDateWithData() {
        UsersSegment segmentMeta1 = SegmentTestDataUtils.usersSegment(123L, TODAY.minusDays(3));
        SegmentKey segmentKey1 = segmentKey(segmentMeta1);
        repository.putData(TODAY.minusDays(2), segmentKey1, List.of(ONE, TWO));
        repository.putData(TODAY.minusDays(1), segmentKey1, List.of(TWO, TEN));

        UsersSegment segmentMeta2 = SegmentTestDataUtils.usersSegment(124L, TODAY.minusDays(2));
        SegmentKey segmentKey2 = segmentKey(segmentMeta2);
        repository.putData(TODAY.minusDays(1), segmentKey2, List.of(TEN));

        SegmentSourceData segmentSourceData = strategy.fetch(List.of(segmentMeta1, segmentMeta2));
        assertThat(segmentSourceData.getFetchedSegmentsKeys())
                .containsExactlyInAnyOrder(segmentKey1);
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey1))
                .containsExactlyInAnyOrder(ONE, TWO, TEN);
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey2))
                .isNullOrEmpty();
    }

    @Test
    public void oneSegmentWithUpdateDateEqualToMostFreshLogDate() {
        UsersSegment segmentMeta = SegmentTestDataUtils.usersSegment(TODAY.minusDays(2));
        SegmentKey segmentKey = segmentKey(segmentMeta);
        repository.putData(TODAY.minusDays(3), segmentKey, List.of(ONE));
        repository.putData(TODAY.minusDays(2), segmentKey, List.of(TWO));

        SegmentSourceData segmentSourceData = strategy.fetch(List.of(segmentMeta));
        assertThat(segmentSourceData.getFetchedSegmentsKeys()).isEmpty();
        assertThat(segmentSourceData.getSegmentKeyToUids()).isEmpty();
        assertThat(segmentSourceData.getLastReadLogDate()).isNull();
        assertThat(segmentSourceData.getMostFreshLogDate()).isEqualTo(TODAY.minusDays(2));
    }

    @Test
    public void oneSegmentWithUpdateDateAfterMostFreshLogDate() {
        UsersSegment segmentMeta = SegmentTestDataUtils.usersSegment(TODAY);
        SegmentKey segmentKey = segmentKey(segmentMeta);
        repository.putData(TODAY.minusDays(3), segmentKey, List.of(ONE));
        repository.putData(TODAY.minusDays(2), segmentKey, List.of(TWO));

        SegmentSourceData segmentSourceData = strategy.fetch(List.of(segmentMeta));
        assertThat(segmentSourceData.getFetchedSegmentsKeys()).isEmpty();
        assertThat(segmentSourceData.getSegmentKeyToUids()).isEmpty();
        assertThat(segmentSourceData.getLastReadLogDate()).isNull();
        assertThat(segmentSourceData.getMostFreshLogDate()).isEqualTo(TODAY.minusDays(2));
    }

    @Test
    public void doesntFetchDataOutOfPeriod() {
        UsersSegment segmentMeta = SegmentTestDataUtils.usersSegment(TODAY.minusDays(2));
        SegmentKey segmentKey = segmentKey(segmentMeta);
        repository.putData(TODAY.minusDays(2), segmentKey, List.of(ONE));
        repository.putData(TODAY.minusDays(1), segmentKey, List.of(TWO));

        SegmentSourceData segmentSourceData = strategy.fetch(List.of(segmentMeta));
        assertThat(segmentSourceData.getFetchedSegmentsKeys())
                .containsExactlyInAnyOrder(segmentKey);
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey))
                .containsExactlyInAnyOrder(TWO);
    }
}
