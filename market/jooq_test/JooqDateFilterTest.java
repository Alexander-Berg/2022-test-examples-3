package ru.yandex.market.mboc.common.services.jooq_test;

import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.JooqTestEntity;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 29.06.2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class JooqDateFilterTest extends BaseDbTestClass {

    private EnhancedRandom random;

    @Autowired
    private JooqTestRepository repository;

    public static JooqTestRepository.Filter notDefined() {
        return new JooqTestRepository.Filter();
    }

    private static LocalDate createDateTimeWithDay(int dayFrom) {
        return LocalDate.of(2017, Month.DECEMBER, dayFrom);
    }

    @Before
    public void setUp() {
        random = new EnhancedRandomBuilder()
            .seed(123)
            .exclude(OffsetDateTime.class)
            .build();
    }

    @Test
    public void dontAddIfTouch() {
        repository.save(entitiesWithRanges(
            to(5), in(4, 5), in(8, 14), from(8)
        ));

        List<JooqTestEntity> values = repository.find(withDates(in(5, 8)));

        assertThat(values).extracting(this::rangeFromEntity).isEmpty();
    }

    @Test
    public void addEnclose() {
        repository.save(entitiesWithRanges(in(7, 9)));

        assertThat(repository.find(withDates(in(7, 9))))
            .extracting(this::rangeFromEntity)
            .contains(in(7, 9));
    }

    @Test
    public void fromOpen() {
        repository.save(entitiesWithRanges(
            to(1), to(3), to(5), to(6), to(7)
        ));

        assertThat(repository.find(withDates(in(2, 6))))
            .extracting(this::rangeFromEntity)
            .containsExactlyInAnyOrder(to(3), to(5), to(6), to(7));
    }

    @Test
    public void toOpen() {
        repository.save(entitiesWithRanges(
            from(1), from(2), from(3), from(5), from(7)
        ));

        assertThat(repository.find(withDates(in(2, 6))))
            .extracting(this::rangeFromEntity)
            .containsExactlyInAnyOrder(from(1), from(2), from(3), from(5));
    }

    @Test
    public void filterTo() {
        repository.save(entitiesWithRanges(
            to(1), to(2), to(3), in(1, 3), from(2), from(3)
        ));

        assertThat(repository.find(withDates(to(3))))
            .extracting(this::rangeFromEntity)
            .containsExactlyInAnyOrder(to(1), to(2), to(3), in(1, 3), from(2));
    }

    @Test
    public void filterFrom() {
        repository.save(entitiesWithRanges(
            to(1), to(2), to(3), to(4), in(3, 5), from(2), from(3)
        ));

        assertThat(repository.find(withDates(from(3))))
            .extracting(this::rangeFromEntity)
            .containsExactlyInAnyOrder(in(3, 5), from(2), from(3), to(4));
    }

    @Test
    public void filterNotDefined() {
        repository.save(entitiesWithRanges(
            to(1), to(2), to(3), to(4), in(3, 5), from(2), from(3)
        ));

        assertThat(repository.find(notDefined()))
            .extracting(this::rangeFromEntity)
            .containsExactlyInAnyOrder(
                to(1), to(2), to(3), to(4), in(3, 5), from(2), from(3)
            );
    }

    public JooqTestRepository.Filter withDates(Range range) {
        return new JooqTestRepository.Filter()
            .setFromDate(range.getFrom()).setToDate(range.getTo());
    }

    public List<JooqTestEntity> entitiesWithRanges(Range... ranges) {
        return Arrays.stream(ranges)
            .map(this::schedule)
            .collect(Collectors.toList());
    }

    private Range rangeFromEntity(JooqTestEntity schedule) {
        return new Range(schedule.getFromDate(), schedule.getToDate());
    }

    public Range in(int dayFrom, int dayTo) {
        return createRange(dayFrom, dayTo);
    }

    public Range from(int dayFrom) {
        return createRange(dayFrom, null);
    }

    public Range to(int dayTo) {
        return createRange(null, dayTo);
    }

    public Range infinite() {
        return createRange(null, null);
    }

    public Range createRange(Integer dayFrom, Integer dayTo) {
        return new Range(dayFrom, dayTo);
    }

    public JooqTestEntity schedule(Range range) {
        JooqTestEntity schedule = random.nextObject(JooqTestEntity.class);
        schedule.setId(null);
        schedule.setFromDate(range.getFrom());
        schedule.setToDate(range.getTo());
        return schedule;
    }

    private static class Range {
        private final LocalDate from;
        private final LocalDate to;

        Range(LocalDate from, LocalDate to) {
            this.from = from;
            this.to = to;
        }

        Range(Integer from, Integer to) {
            this.from = from != null
                ? createDateTimeWithDay(from)
                : null;
            this.to = to != null
                ? createDateTimeWithDay(to)
                : null;
        }

        public LocalDate getFrom() {
            return from;
        }

        public LocalDate getTo() {
            return to;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (from != null) {
                sb.append('[').append(from.getDayOfMonth());
            } else {
                sb.append("(-inf");
            }
            sb.append(", ");
            if (to != null) {
                sb.append(to.getDayOfMonth()).append("]");
            } else {
                sb.append("+inf)");
            }

            return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Range range = (Range) o;
            return Objects.equals(from, range.from) &&
                Objects.equals(to, range.to);
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }
    }
}
