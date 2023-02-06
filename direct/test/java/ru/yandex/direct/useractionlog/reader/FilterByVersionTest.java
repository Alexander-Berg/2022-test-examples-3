package ru.yandex.direct.useractionlog.reader;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.PeekingIterator;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.binlogclickhouse.schema.FieldValueList;
import ru.yandex.direct.test.utils.TestUtils;
import ru.yandex.direct.tracing.data.DirectTraceInfo;
import ru.yandex.direct.useractionlog.ClientId;
import ru.yandex.direct.useractionlog.schema.ActionLogRecord;
import ru.yandex.direct.useractionlog.schema.ObjectPath;
import ru.yandex.direct.useractionlog.schema.Operation;
import ru.yandex.direct.useractionlog.schema.RecordSource;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class FilterByVersionTest {
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private ActionLogRecord record1 = fillOtherPartsAndBuild(ActionLogRecord.builder()
            .withDateTime(LocalDateTime.of(2000, 1, 1, 0, 0))
            .withGtid("serveruuid:1")
            .withRecordSource(
                    new RecordSource(RecordSource.RECORD_SOURCE_DAEMON, LocalDateTime.of(2000, 1, 1, 0, 0))));
    private ActionLogRecord record2 = fillOtherPartsAndBuild(ActionLogRecord.builder()
            .withDateTime(LocalDateTime.of(2000, 1, 2, 0, 0))
            .withGtid("serveruuid:2")
            .withRecordSource(
                    new RecordSource(RecordSource.RECORD_SOURCE_DAEMON, LocalDateTime.of(2000, 1, 2, 0, 0))));
    private ActionLogRecord record3 = fillOtherPartsAndBuild(ActionLogRecord.builder()
            .withDateTime(LocalDateTime.of(2000, 1, 3, 0, 0))
            .withGtid("serveruuid:3")
            .withRecordSource(
                    new RecordSource(RecordSource.RECORD_SOURCE_DAEMON, LocalDateTime.of(2018, 1, 3, 0, 0))));

    private ActionLogRecord fillOtherPartsAndBuild(ActionLogRecord.Builder builder) {
        return builder.withPath(new ObjectPath.ClientPath(new ClientId(1)))
                .withQuerySerial(0)
                .withRowSerial(0)
                .withDirectTraceInfo(DirectTraceInfo.empty())
                .withDb("test")
                .withType("test")
                .withOperation(Operation.INSERT)
                .withOldFields(FieldValueList.empty())
                .withNewFields(FieldValueList.empty())
                .build();
    }

    private List<ActionLogRecord> applyTarget(List<ActionLogRecord> source) {
        List<ActionLogRecord> result = null;
        for (int peeks = 0; peeks < 3; ++peeks) {
            result = new ArrayList<>();
            PeekingIterator<ActionLogRecord> target = new FilterByVersion(source.iterator());
            while (target.hasNext()) {
                Set<String> peekedGtids = new HashSet<>();
                for (int ignored = 0; ignored < peeks; ++ignored) {
                    ActionLogRecord peek = target.peek();
                    softly.assertThat(peek.isDeleted())
                            .describedAs("Should be never peeked deleted element")
                            .isFalse();
                    peekedGtids.add(peek.getGtid());
                }
                ActionLogRecord record = target.next();
                result.add(record);
                peekedGtids.add(record.getGtid());
                softly.assertThat(peekedGtids)
                        .describedAs("peek() should always return the same element that will be returned by next()")
                        .hasSize(1);
            }
        }
        return Objects.requireNonNull(result);
    }

    /**
     * Если есть несколько записей с одинаковым первичным ключом и одинаковым {@link RecordSource},
     * то оставить только одну из них. Какую именно - не важно.
     */
    @Parameters({"1", "2", "3", "4", "5"})
    @Test
    public void squashDuplicates(int duplicatesCount) {
        List<ActionLogRecord> duplicates = IntStream.range(0, duplicatesCount)
                .mapToObj(ignored -> record2).collect(Collectors.toList());
        List<ActionLogRecord> surroundedDuplicates = new ArrayList<>();
        surroundedDuplicates.add(record1);
        surroundedDuplicates.addAll(duplicates);
        surroundedDuplicates.add(record3);

        softly.assertThat(applyTarget(duplicates))
                .describedAs("Duplicates should be squashed into one record")
                .extracting(ActionLogRecord::getDateTime)
                .isEqualTo(Collections.singletonList(record2.getDateTime()));

        softly.assertThat(applyTarget(surroundedDuplicates))
                .describedAs("Should work for elements in the middle of the stream too")
                .extracting(ActionLogRecord::getDateTime)
                .isEqualTo(Arrays.asList(record1.getDateTime(), record2.getDateTime(), record3.getDateTime()));
    }

    /**
     * Если есть несколько записей с одинаковым первичным ключом, но разным {@link RecordSource},
     * и если запись с наибольшим {@link RecordSource} имеет установленный флаг {@link ActionLogRecord#isDeleted()},
     * то эти несколько записей следует выкинуть.
     */
    @Parameters({
            "1,head",
            "1,tail",

            "4,head",
            "4,middle",
            "4,tail",
    })
    @Test
    public void deleted(int duplicatesCount, String whereToInsertDuplicate) {
        List<ActionLogRecord> duplicates = IntStream.range(0, duplicatesCount)
                .mapToObj(ignored -> record2).collect(Collectors.toList());
        ActionLogRecord record2deleted = fillOtherPartsAndBuild(ActionLogRecord.builder()
                .withDateTime(record2.getDateTime())
                .withGtid(record2.getGtid())
                .withRecordSource(new RecordSource(
                        RecordSource.RECORD_SOURCE_MANUAL, LocalDateTime.of(2018, 1, 1, 0, 0)))
                .withDeleted(true));
        TestUtils.assumeThat(softAssertions ->
                softAssertions.assertThat(record2deleted.getRecordSource())
                        .isGreaterThan(record2.getRecordSource()));
        switch (whereToInsertDuplicate) {
            case "head":
                duplicates.add(0, record2deleted);
                break;
            case "middle":
                duplicates.add(duplicates.size() / 2, record2deleted);
                break;
            case "tail":
                duplicates.add(record2deleted);
                break;
            default:
                throw new UnsupportedOperationException(whereToInsertDuplicate);
        }
        List<ActionLogRecord> surroundedDuplicates = new ArrayList<>();
        surroundedDuplicates.add(record1);
        surroundedDuplicates.addAll(duplicates);
        surroundedDuplicates.add(record3);

        softly.assertThat(applyTarget(duplicates))
                .describedAs("Collapsed records should be removed")
                .extracting(ActionLogRecord::getDateTime)
                .isEqualTo(Collections.emptyList());

        softly.assertThat(applyTarget(surroundedDuplicates))
                .describedAs("Should work for elements in the middle of the stream too")
                .extracting(ActionLogRecord::getDateTime)
                .isEqualTo(Arrays.asList(record1.getDateTime(), record3.getDateTime()));
    }

    /**
     * Если есть несколько записей с одинаковым первичным ключом, но разным {@link RecordSource},
     * то следует выдать только ту запись, у которой наибольший {@link RecordSource}.
     */
    @Parameters({
            "0",
            "0 1",
            "1 0",
            "0 1 3 7",
            "7 3 1 0",
            "0 7 1 3",
    })
    @Test
    public void multipleVersions(String plusMinutesArrayRaw) {
        int[] plusMinutesArray = Arrays.stream(plusMinutesArrayRaw.split(" ")).mapToInt(Integer::parseInt).toArray();
        List<ActionLogRecord> versions = IntStream.of(plusMinutesArray)
                .mapToObj(plusMinutes -> fillOtherPartsAndBuild(ActionLogRecord.builder()
                        .withDateTime(record2.getDateTime().plusMinutes(plusMinutes))
                        .withGtid(record2.getGtid())
                        .withRecordSource(new RecordSource(record2.getRecordSource().getType(),
                                record2.getRecordSource().getTimestamp().plusMinutes(plusMinutes)))))
                .collect(Collectors.toList());
        int maxPlusSeconds = IntStream.of(plusMinutesArray).max().orElseThrow(IllegalStateException::new);
        List<ActionLogRecord> surroundedVersions = new ArrayList<>();
        surroundedVersions.add(record1);
        surroundedVersions.addAll(versions);
        surroundedVersions.add(record3);

        softly.assertThat(applyTarget(versions))
                .describedAs("Should be kept only record with maximal version")
                .extracting(ActionLogRecord::getDateTime)
                .isEqualTo(Collections.singletonList(
                        record2.getDateTime().plusMinutes(maxPlusSeconds)));

        softly.assertThat(applyTarget(surroundedVersions))
                .describedAs("Should work for elements in the middle of the stream too")
                .extracting(ActionLogRecord::getDateTime)
                .isEqualTo(Arrays.asList(
                        record1.getDateTime(),
                        record2.getDateTime().plusMinutes(maxPlusSeconds),
                        record3.getDateTime()));
    }
}
