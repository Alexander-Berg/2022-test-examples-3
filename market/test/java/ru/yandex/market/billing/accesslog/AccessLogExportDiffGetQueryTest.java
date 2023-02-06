package ru.yandex.market.billing.accesslog;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DataSetType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.common.repository.GenericRepository;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author zoom
 */
class AccessLogExportDiffGetQueryTest extends FunctionalTest {

    @Autowired
    private GenericRepository repository;

    @Test
    @DbUnitDataSet
    void shouldReturnNothingForEmptyTables() {
        repository.query(AccessLogExportDiffGetQuery.sinceYesterday(), cursor -> fail("must return nothing"));
    }

    @Test
    @DbUnitDataSet(
            before = "AccessLogExportDiffGetQueryTest.shouldReturnNothingForEmptyAccessLogTable.before.csv",
            type = DataSetType.SINGLE_CSV
    )
    void shouldReturnNothingForEmptyAccessLogTable() {
        repository.query(AccessLogExportDiffGetQuery.sinceYesterday(), cursor -> fail("must return nothing"));
    }

    @Test
    @DbUnitDataSet(
            before = "AccessLogExportDiffGetQueryTest.shouldReturnAllAccessLogRowsWhenEmptyExportTable.before.csv",
            type = DataSetType.SINGLE_CSV
    )
    void shouldReturnAllAccessLogRowsWhenEmptyExportTable() {
        List<Diff> diffs = new ArrayList<>();
        repository.query(AccessLogExportDiffGetQuery.since(2017, Month.MAY, 1), cursor -> diffs.add(
                new Diff(
                        cursor.getDate(),
                        cursor.getDistrTypeId(),
                        cursor.getClid(),
                        cursor.getVid(),
                        cursor.getClickCount()
                )
        ));
        Assertions.assertEquals(
                new HashSet<>(Collections.singletonList(new Diff(LocalDate.of(2017, Month.MAY, 1), 1, 2, "3", 4))),
                new HashSet<>(diffs)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "AccessLogExportDiffGetQueryTest.shouldReturnRecordsWithNewClid.before.csv",
            type = DataSetType.SINGLE_CSV
    )
    void shouldReturnRecordsWithNewClid() {
        List<Diff> diffs = new ArrayList<>();
        repository.query(AccessLogExportDiffGetQuery.since(2017, Month.MAY, 1), cursor -> diffs.add(
                new Diff(
                        cursor.getDate(),
                        cursor.getDistrTypeId(),
                        cursor.getClid(),
                        cursor.getVid(),
                        cursor.getClickCount()
                )
        ));
        Assertions.assertEquals(
                new HashSet<>(Collections.singletonList(new Diff(LocalDate.of(2017, Month.MAY, 1), 1, 21, "3", 4))),
                new HashSet<>(diffs)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "AccessLogExportDiffGetQueryTest.shouldReturnRecordsModifiedClidClicks.before.csv",
            type = DataSetType.SINGLE_CSV
    )
    void shouldReturnRecordsModifiedClidClicks() {
        List<Diff> diffs = new ArrayList<>();
        repository.query(AccessLogExportDiffGetQuery.since(2017, Month.MAY, 1), cursor -> diffs.add(
                new Diff(
                        cursor.getDate(),
                        cursor.getDistrTypeId(),
                        cursor.getClid(),
                        cursor.getVid(),
                        cursor.getClickCount()
                )
        ));
        Assertions.assertEquals(
                new HashSet<>(Collections.singletonList(new Diff(LocalDate.of(2017, Month.MAY, 1), 1, 2, "3", 44))),
                new HashSet<>(diffs)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "AccessLogExportDiffGetQueryTest.shouldReturnRecordsModifiedClidClicksWithNullVid.before.csv",
            type = DataSetType.SINGLE_CSV
    )
    void shouldReturnRecordsModifiedClidClicksWithNullVid() {
        List<Diff> diffs = new ArrayList<>();
        repository.query(AccessLogExportDiffGetQuery.since(2017, Month.MAY, 1), cursor -> diffs.add(
                new Diff(
                        cursor.getDate(),
                        cursor.getDistrTypeId(),
                        cursor.getClid(),
                        cursor.getVid(),
                        cursor.getClickCount()
                )
        ));
        Assertions.assertEquals(
                new HashSet<>(
                        Arrays.asList(
                                new Diff(LocalDate.of(2017, Month.MAY, 1), 1, 2, null, 44),
                                new Diff(LocalDate.of(2017, Month.MAY, 1), 11, 22, "33", 4)
                        )
                ),
                new HashSet<>(diffs)
        );
    }

    /**
     *
     */
    @Test
    @DbUnitDataSet(
            before = "AccessLogExportDiffGetQueryTest.shouldGroupExportedRowsByDate.before.csv",
            type = DataSetType.SINGLE_CSV
    )
    void shouldGroupExportedRowsByDate() {
        List<Diff> diffs = new ArrayList<>();
        repository.query(AccessLogExportDiffGetQuery.since(2017, Month.MAY, 1), cursor -> diffs.add(
                new Diff(
                        cursor.getDate(),
                        cursor.getDistrTypeId(),
                        cursor.getClid(),
                        cursor.getVid(),
                        cursor.getClickCount()
                )
        ));
        Assertions.assertEquals(
                new HashSet<>(
                        Collections.singletonList(
                                new Diff(LocalDate.of(2017, Month.MAY, 1), 1, 2, "3", 5)
                        )
                ),
                new HashSet<>(diffs)
        );
    }

    private static class Diff {
        private final LocalDate date;
        private final long distrTypeId;
        private final long clid;
        private final String vid;
        private final int clickCount;

        private Diff(LocalDate date, long distrTypeId, long clid, String vid, int clickCount) {
            this.date = date;
            this.distrTypeId = distrTypeId;
            this.clid = clid;
            this.vid = vid;
            this.clickCount = clickCount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Diff diff = (Diff) o;
            return distrTypeId == diff.distrTypeId &&
                    clid == diff.clid &&
                    clickCount == diff.clickCount &&
                    Objects.equals(date, diff.date) &&
                    Objects.equals(vid, diff.vid);
        }

        @Override
        public int hashCode() {
            return Objects.hash(date, distrTypeId, clid, vid, clickCount);
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
        }
    }


}
