package ru.yandex.direct.jobs.segment.log.greedy;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.adgroup.model.UsersSegment;
import ru.yandex.direct.jobs.segment.SegmentTestDataUtils;
import ru.yandex.direct.jobs.segment.common.meta.SegmentKey;
import ru.yandex.direct.jobs.segment.log.IntermediateSegmentYtRepositoryMock;
import ru.yandex.direct.jobs.segment.log.SegmentSourceData;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.TWO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.direct.jobs.segment.SegmentTestDataUtils.segmentKey;

public class GreedySegmentLogFetchingStrategyLimitTest {

    private static final LocalDate TODAY = LocalDate.now();
    private static final long PACK_LIMIT = 5;

    private static final BigInteger THREE = BigInteger.valueOf(3L);
    private static final BigInteger FOUR = BigInteger.valueOf(4L);
    private static final BigInteger FIVE = BigInteger.valueOf(5L);

    private IntermediateSegmentYtRepositoryMock repository;

    private GreedySegmentLogFetchingStrategy strategy;

    @BeforeEach
    public void beforeEach() {
        repository = new IntermediateSegmentYtRepositoryMock();
        strategy = new GreedySegmentLogFetchingStrategy(repository, () -> PACK_LIMIT);
    }

    @Test
    public void oneSegmentWithMaxDataInOneDay() {
        UsersSegment segmentMeta = SegmentTestDataUtils.usersSegment(TODAY.minusDays(2));
        SegmentKey segmentKey = segmentKey(segmentMeta);
        repository.putData(TODAY.minusDays(1), segmentKey, List.of(ONE, TWO, THREE, FOUR, FIVE));

        SegmentSourceData segmentSourceData = strategy.fetch(List.of(segmentMeta));
        assertThat(segmentSourceData.getFetchedSegmentsKeys())
                .containsExactlyInAnyOrder(segmentKey);
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey))
                .containsExactlyInAnyOrder(ONE, TWO, THREE, FOUR, FIVE);
        assertThat(segmentSourceData.getLastReadLogDate())
                .isEqualTo(TODAY.minusDays(1));
    }

    @Test
    public void oneSegmentWithMaxDataInSeveralDays() {
        UsersSegment segmentMeta = SegmentTestDataUtils.usersSegment(TODAY.minusDays(4));
        SegmentKey segmentKey = segmentKey(segmentMeta);
        repository.putData(TODAY.minusDays(3), segmentKey, List.of(ONE, TWO, THREE));
        repository.putData(TODAY.minusDays(1), segmentKey, List.of(ONE, FOUR));

        SegmentSourceData segmentSourceData = strategy.fetch(List.of(segmentMeta));
        assertThat(segmentSourceData.getFetchedSegmentsKeys())
                .containsExactlyInAnyOrder(segmentKey);
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey))
                .containsExactlyInAnyOrder(ONE, TWO, THREE, FOUR);
        assertThat(segmentSourceData.getLastReadLogDate())
                .isEqualTo(TODAY.minusDays(1));
    }

    @Test
    public void oneSegmentWithExceededDataInOneDay() {
        UsersSegment segmentMeta = SegmentTestDataUtils.usersSegment(TODAY.minusDays(2));
        SegmentKey segmentKey = segmentKey(segmentMeta);
        repository.putData(TODAY.minusDays(1), segmentKey, List.of(ONE, TWO, THREE, FOUR, FIVE, TEN));

        assertThatThrownBy(() -> strategy.fetch(List.of(segmentMeta)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void oneSegmentWithExceededDataAndPeriodCut() {
        UsersSegment segmentMeta = SegmentTestDataUtils.usersSegment(TODAY.minusDays(5));
        SegmentKey segmentKey = segmentKey(segmentMeta);
        repository.putData(TODAY.minusDays(4), segmentKey, List.of(ONE));
        repository.putData(TODAY.minusDays(3), segmentKey, List.of(TWO, THREE));
        repository.putData(TODAY.minusDays(2), segmentKey, List.of(FOUR, FIVE));
        repository.putData(TODAY.minusDays(1), segmentKey, List.of(TEN));

        SegmentSourceData segmentSourceData = strategy.fetch(List.of(segmentMeta));
        assertThat(segmentSourceData.getFetchedSegmentsKeys())
                .containsExactlyInAnyOrder(segmentKey);
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey))
                .containsExactlyInAnyOrder(ONE, TWO, THREE);
        assertThat(segmentSourceData.getLastReadLogDate())
                .isEqualTo(TODAY.minusDays(3));
    }

    @Test
    public void twoSegmentsWithMaxDataInOneDay() {
        UsersSegment segmentMeta1 = SegmentTestDataUtils.usersSegment(123L, TODAY.minusDays(2));
        SegmentKey segmentKey1 = segmentKey(segmentMeta1);
        repository.putData(TODAY.minusDays(1), segmentKey1, List.of(ONE, TWO));

        UsersSegment segmentMeta2 = SegmentTestDataUtils.usersSegment(124L, TODAY.minusDays(2));
        SegmentKey segmentKey2 = segmentKey(segmentMeta2);
        repository.putData(TODAY.minusDays(1), segmentKey2, List.of(ONE, TWO, THREE));

        SegmentSourceData segmentSourceData = strategy.fetch(List.of(segmentMeta1, segmentMeta2));
        assertThat(segmentSourceData.getFetchedSegmentsKeys())
                .containsExactlyInAnyOrder(segmentKey1, segmentKey2);
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey1))
                .containsExactlyInAnyOrder(ONE, TWO);
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey2))
                .containsExactlyInAnyOrder(ONE, TWO, THREE);
        assertThat(segmentSourceData.getLastReadLogDate())
                .isEqualTo(TODAY.minusDays(1));
    }

    @Test
    public void twoSegmentsWithMaxDataInSeveralDays() {
        UsersSegment segmentMeta1 = SegmentTestDataUtils.usersSegment(123L, TODAY.minusDays(3));
        SegmentKey segmentKey1 = segmentKey(segmentMeta1);
        repository.putData(TODAY.minusDays(2), segmentKey1, List.of(ONE));
        repository.putData(TODAY.minusDays(1), segmentKey1, List.of(TWO));

        UsersSegment segmentMeta2 = SegmentTestDataUtils.usersSegment(124L, TODAY.minusDays(3));
        SegmentKey segmentKey2 = segmentKey(segmentMeta2);
        repository.putData(TODAY.minusDays(2), segmentKey2, List.of(ONE, TWO));
        repository.putData(TODAY.minusDays(1), segmentKey2, List.of(THREE));

        SegmentSourceData segmentSourceData = strategy.fetch(List.of(segmentMeta1, segmentMeta2));
        assertThat(segmentSourceData.getFetchedSegmentsKeys())
                .containsExactlyInAnyOrder(segmentKey1, segmentKey2);
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey1))
                .containsExactlyInAnyOrder(ONE, TWO);
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey2))
                .containsExactlyInAnyOrder(ONE, TWO, THREE);
        assertThat(segmentSourceData.getLastReadLogDate())
                .isEqualTo(TODAY.minusDays(1));
    }

    @Test
    public void twoSegmentsWithExceededDataInOneDay() {
        UsersSegment segmentMeta1 = SegmentTestDataUtils.usersSegment(123L, TODAY.minusDays(2));
        SegmentKey segmentKey1 = segmentKey(segmentMeta1);
        repository.putData(TODAY.minusDays(1), segmentKey1, List.of(ONE, TWO, THREE));

        UsersSegment segmentMeta2 = SegmentTestDataUtils.usersSegment(124L, TODAY.minusDays(2));
        SegmentKey segmentKey2 = segmentKey(segmentMeta2);
        repository.putData(TODAY.minusDays(1), segmentKey2, List.of(TWO, THREE, FOUR, FIVE));

        SegmentSourceData segmentSourceData = strategy.fetch(List.of(segmentMeta1, segmentMeta2));
        assertThat(segmentSourceData.getFetchedSegmentsKeys())
                .containsExactlyInAnyOrder(segmentKey2);
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey1))
                .isNullOrEmpty();
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey2))
                .containsExactlyInAnyOrder(TWO, THREE, FOUR, FIVE);
        assertThat(segmentSourceData.getLastReadLogDate())
                .isEqualTo(TODAY.minusDays(1));
    }

    @Test
    public void twoSegmentsWithExceededDataAndNoPeriodCuts() {
        UsersSegment segmentMeta1 = SegmentTestDataUtils.usersSegment(123L, TODAY.minusDays(5));
        SegmentKey segmentKey1 = segmentKey(segmentMeta1);
        repository.putData(TODAY.minusDays(4), segmentKey1, List.of(ONE));
        repository.putData(TODAY.minusDays(2), segmentKey1, List.of(TWO));
        repository.putData(TODAY.minusDays(1), segmentKey1, List.of(THREE, FIVE));

        UsersSegment segmentMeta2 = SegmentTestDataUtils.usersSegment(124L, TODAY.minusDays(5));
        SegmentKey segmentKey2 = segmentKey(segmentMeta2);
        repository.putData(TODAY.minusDays(4), segmentKey2, List.of(TWO));
        repository.putData(TODAY.minusDays(3), segmentKey2, List.of(THREE));
        repository.putData(TODAY.minusDays(1), segmentKey2, List.of(FOUR));

        SegmentSourceData segmentSourceData = strategy.fetch(List.of(segmentMeta1, segmentMeta2));
        assertThat(segmentSourceData.getFetchedSegmentsKeys())
                .containsExactlyInAnyOrder(segmentKey1);
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey1))
                .containsExactlyInAnyOrder(ONE, TWO, THREE, FIVE);
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey2))
                .isNullOrEmpty();
        assertThat(segmentSourceData.getLastReadLogDate())
                .isEqualTo(TODAY.minusDays(1));
    }

    @Test
    public void twoSegmentsWithExceededDataAndOnePeriodCutAndFetchingBoth() {
        UsersSegment segmentMeta1 = SegmentTestDataUtils.usersSegment(123L, TODAY.minusDays(5));
        SegmentKey segmentKey1 = segmentKey(segmentMeta1);
        repository.putData(TODAY.minusDays(4), segmentKey1, List.of(ONE, TWO, THREE));
        repository.putData(TODAY.minusDays(2), segmentKey1, List.of(FOUR));
        repository.putData(TODAY.minusDays(1), segmentKey1, List.of(FIVE, TEN));

        UsersSegment segmentMeta2 = SegmentTestDataUtils.usersSegment(124L, TODAY.minusDays(5));
        SegmentKey segmentKey2 = segmentKey(segmentMeta2);
        repository.putData(TODAY.minusDays(4), segmentKey2, List.of(ONE));
        repository.putData(TODAY.minusDays(3), segmentKey2, List.of(TWO));
        repository.putData(TODAY.minusDays(1), segmentKey2, List.of(THREE, FOUR, FIVE, TEN));

        SegmentSourceData segmentSourceData = strategy.fetch(List.of(segmentMeta1, segmentMeta2));
        assertThat(segmentSourceData.getFetchedSegmentsKeys())
                .containsExactlyInAnyOrder(segmentKey1, segmentKey2);
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey1))
                .containsExactlyInAnyOrder(ONE, TWO, THREE);
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey2))
                .containsExactlyInAnyOrder(ONE, TWO);
        assertThat(segmentSourceData.getLastReadLogDate())
                .isEqualTo(TODAY.minusDays(3));
    }

    @Test
    public void twoSegmentsWithExceededDataAndOnePeriodCutAndFetchingSecond() {
        UsersSegment segmentMeta1 = SegmentTestDataUtils.usersSegment(123L, TODAY.minusDays(5));
        SegmentKey segmentKey1 = segmentKey(segmentMeta1);
        repository.putData(TODAY.minusDays(4), segmentKey1, List.of(ONE, TWO, THREE, FOUR, FIVE, TEN));
        repository.putData(TODAY.minusDays(2), segmentKey1, List.of(TEN));
        repository.putData(TODAY.minusDays(1), segmentKey1, List.of(THREE));

        UsersSegment segmentMeta2 = SegmentTestDataUtils.usersSegment(124L, TODAY.minusDays(5));
        SegmentKey segmentKey2 = segmentKey(segmentMeta2);
        repository.putData(TODAY.minusDays(4), segmentKey2, List.of(ONE, TWO));
        repository.putData(TODAY.minusDays(3), segmentKey2, List.of(THREE, FOUR, FIVE));
        repository.putData(TODAY.minusDays(1), segmentKey2, List.of(TEN));

        SegmentSourceData segmentSourceData = strategy.fetch(List.of(segmentMeta1, segmentMeta2));
        assertThat(segmentSourceData.getFetchedSegmentsKeys())
                .containsExactlyInAnyOrder(segmentKey2);
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey1))
                .isNullOrEmpty();
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey2))
                .containsExactlyInAnyOrder(ONE, TWO, THREE, FOUR, FIVE);
        assertThat(segmentSourceData.getLastReadLogDate())
                .isEqualTo(TODAY.minusDays(3));
    }

    @Test
    public void twoSegmentsWithExceededDataAndTwoPeriodCutsAndFetchingBoth() {
        UsersSegment segmentMeta1 = SegmentTestDataUtils.usersSegment(123L, TODAY.minusDays(5));
        SegmentKey segmentKey1 = segmentKey(segmentMeta1);
        repository.putData(TODAY.minusDays(4), segmentKey1, List.of(ONE, TWO, THREE));
        repository.putData(TODAY.minusDays(3), segmentKey1, List.of(ONE, TWO, THREE));
        repository.putData(TODAY.minusDays(2), segmentKey1, List.of(FOUR, FIVE, TEN));
        repository.putData(TODAY.minusDays(1), segmentKey1, List.of(FIVE));

        UsersSegment segmentMeta2 = SegmentTestDataUtils.usersSegment(124L, TODAY.minusDays(5));
        SegmentKey segmentKey2 = segmentKey(segmentMeta2);
        repository.putData(TODAY.minusDays(4), segmentKey2, List.of(ONE, TWO));
        repository.putData(TODAY.minusDays(3), segmentKey2, List.of(THREE, FOUR, FIVE, TEN));
        repository.putData(TODAY.minusDays(1), segmentKey2, List.of(FIVE));

        SegmentSourceData segmentSourceData = strategy.fetch(List.of(segmentMeta1, segmentMeta2));
        assertThat(segmentSourceData.getFetchedSegmentsKeys())
                .containsExactlyInAnyOrder(segmentKey1, segmentKey2);
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey1))
                .containsExactlyInAnyOrder(ONE, TWO, THREE);
        assertThat(segmentSourceData.getSegmentKeyToUids().get(segmentKey2))
                .containsExactlyInAnyOrder(ONE, TWO);
        assertThat(segmentSourceData.getLastReadLogDate())
                .isEqualTo(TODAY.minusDays(4));
    }

    @Test
    public void twoSegmentsWithExceededDataAndNoWayToFetch() {
        UsersSegment segmentMeta1 = SegmentTestDataUtils.usersSegment(123L, TODAY.minusDays(5));
        SegmentKey segmentKey1 = segmentKey(segmentMeta1);
        repository.putData(TODAY.minusDays(4), segmentKey1, List.of(ONE, TWO, THREE, FOUR, FIVE, TEN));
        repository.putData(TODAY.minusDays(2), segmentKey1, List.of(FIVE));
        repository.putData(TODAY.minusDays(1), segmentKey1, List.of(THREE));

        UsersSegment segmentMeta2 = SegmentTestDataUtils.usersSegment(124L, TODAY.minusDays(3));
        SegmentKey segmentKey2 = segmentKey(segmentMeta2);
        repository.putData(TODAY.minusDays(4), segmentKey2, List.of(ONE, TWO, THREE, FOUR, FIVE, TEN));
        repository.putData(TODAY.minusDays(3), segmentKey2, List.of(THREE));
        repository.putData(TODAY.minusDays(1), segmentKey2, List.of(FOUR));

        assertThatThrownBy(() -> strategy.fetch(List.of(segmentMeta1, segmentMeta2)))
                .isInstanceOf(IllegalStateException.class);
    }
}
