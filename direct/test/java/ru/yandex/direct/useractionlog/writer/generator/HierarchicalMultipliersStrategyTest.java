package ru.yandex.direct.useractionlog.writer.generator;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.JUnitSoftAssertions;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.TableField;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.direct.binlog.reader.EnrichedEvent;
import ru.yandex.direct.binlogclickhouse.schema.FieldValue;
import ru.yandex.direct.binlogclickhouse.schema.FieldValueList;
import ru.yandex.direct.dbschema.ppc.enums.DemographyMultiplierValuesAge;
import ru.yandex.direct.dbschema.ppc.enums.DemographyMultiplierValuesGender;
import ru.yandex.direct.dbschema.ppc.enums.HierarchicalMultipliersType;
import ru.yandex.direct.dbschema.ppc.tables.DemographyMultiplierValues;
import ru.yandex.direct.dbschema.ppc.tables.GeoMultiplierValues;
import ru.yandex.direct.dbschema.ppc.tables.HierarchicalMultipliers;
import ru.yandex.direct.dbschema.ppc.tables.RetargetingMultiplierValues;
import ru.yandex.direct.test.utils.TestUtils;
import ru.yandex.direct.tracing.data.DirectTraceInfo;
import ru.yandex.direct.useractionlog.CampaignId;
import ru.yandex.direct.useractionlog.ClientId;
import ru.yandex.direct.useractionlog.dict.DictDataCategory;
import ru.yandex.direct.useractionlog.dict.DictRequest;
import ru.yandex.direct.useractionlog.dict.MemoryDictRepository;
import ru.yandex.direct.useractionlog.model.HierarchicalMultipliersData;
import ru.yandex.direct.useractionlog.schema.ActionLogRecord;
import ru.yandex.direct.useractionlog.schema.ObjectPath;
import ru.yandex.direct.useractionlog.schema.Operation;
import ru.yandex.direct.useractionlog.schema.RecordSource;
import ru.yandex.direct.useractionlog.writer.BinlogFixtureGenerator;
import ru.yandex.direct.useractionlog.writer.ErrorWrapper;

import static org.hamcrest.core.Is.is;
import static ru.yandex.direct.dbschema.ppc.Ppc.PPC;

@ParametersAreNonnullByDefault
@SuppressWarnings("checkstyle:constantname")
public class HierarchicalMultipliersStrategyTest {
    private static final String RELATED_IDS = "related_ids";
    private static final HierarchicalMultipliers HM_TABLE = PPC.HIERARCHICAL_MULTIPLIERS;
    private static final DemographyMultiplierValues DMV_TABLE = PPC.DEMOGRAPHY_MULTIPLIER_VALUES;
    private static final GeoMultiplierValues GMV_TABLE = PPC.GEO_MULTIPLIER_VALUES;
    private static final RetargetingMultiplierValues RMV_TABLE = PPC.RETARGETING_MULTIPLIER_VALUES;
    private static final ObjectPath.CampaignPath CAMPAIGN_PATH =
            new ObjectPath.CampaignPath(new ClientId(1001), new CampaignId(2001));
    private static final LocalDateTime NOW = LocalDateTime.of(2017, 11, 23, 0, 0);
    private static final String VERSION = "%%version";
    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();
    private MemoryDictRepository dictRepository;
    private RowProcessingStrategy rowProcessingStrategy;
    private RecordSource recordSource;

    @Before
    public void setUp() {
        dictRepository = new MemoryDictRepository();
        dictRepository.addData(DictDataCategory.CAMPAIGN_PATH, 2001, CAMPAIGN_PATH);
        dictRepository.addData(DictDataCategory.RETARGETING_CONDITION_NAME, 5001, "my condition");
        recordSource = RecordSource.makeDaemonRecordSource();
        rowProcessingStrategy =
                RowProcessingDefaults.defaultRowToActionLog(recordSource);
    }

    private List<ActionLogRecord> processEvents(EnrichedEvent... events) {
        BatchRowDictProcessing.Result dictResult = BatchRowDictProcessing.handleEvents(dictRepository,
                rowProcessingStrategy,
                Stream.of(events).flatMap(EnrichedEvent::rowsStream).collect(Collectors.toList()),
                new ErrorWrapper(false));
        Assertions.assertThat(dictResult.unprocessed).isEmpty();
        return dictResult.processed.stream()
                .flatMap(pair -> rowProcessingStrategy.processEvent(pair.getLeft(), pair.getRight()).stream())
                .collect(Collectors.toList());
    }

    /**
     * {@link HierarchicalMultipliersStrategy} написан с расчётом, что все поля в таблицах корректировок ставок
     * присутствуют во всех событиях из бинлога.
     */
    @Test
    public void allTablesShouldNotHaveTextOrBlob() {
        for (Table table : new Table[]{HM_TABLE, DMV_TABLE, GMV_TABLE, RMV_TABLE}) {
            for (Field field : table.fields()) {
                softly.assertThat(field.getDataType())
                        .describedAs("Field " + field + " should not be TEXT or BLOB")
                        .isNotEqualTo(org.jooq.impl.SQLDataType.BLOB)
                        .isNotEqualTo(org.jooq.impl.SQLDataType.CLOB);
            }
        }
    }

    @SuppressWarnings("squid:S2970")  // это не тест
    private boolean mobileCreatedCorrectly(JUnitSoftAssertions customSoftly) {
        List<ActionLogRecord> records = processEvents(Fixtures.Mobile.create);
        customSoftly.assertThat(records)
                .hasSize(1);
        if (!records.isEmpty()) {
            customSoftly.assertThat(records.get(0)).isEqualToComparingFieldByFieldRecursively(ActionLogRecord.builder()
                    .withDateTime(NOW)
                    .withDb("ppc")
                    .withDirectTraceInfo(DirectTraceInfo.empty())
                    .withGtid(BinlogFixtureGenerator.SERVER_UUID + ":" + 100)
                    .withOperation(Operation.INSERT)
                    .withPath(CAMPAIGN_PATH)
                    .withQuerySerial(0)
                    .withRowSerial(0)
                    .withType("hierarchical_multipliers")
                    .withOldFields(new FieldValueList(Collections.singletonList(
                            new FieldValue<>(VERSION, "01"))))
                    .withNewFields(new FieldValueList(Arrays.asList(
                            new FieldValue<>(HM_TABLE.HIERARCHICAL_MULTIPLIER_ID.getName(), "8001"),
                            new FieldValue<>(HM_TABLE.IS_ENABLED.getName(), "1"),
                            new FieldValue<>(HM_TABLE.LAST_CHANGE.getName(), "2017-11-23 00:00:00"),
                            new FieldValue<>(HM_TABLE.MULTIPLIER_PCT.getName(), "80"),
                            new FieldValue<>(RELATED_IDS, ""),
                            new FieldValue<>(HM_TABLE.TYPE.getName(),
                                    HierarchicalMultipliersType.mobile_multiplier.getLiteral()),
                            new FieldValue<>(VERSION, "01"))))
                    .withRecordSource(recordSource)
                    .build());
        }
        return customSoftly.errorsCollected().isEmpty();
    }

    @Test
    public void createMobileMultiplier() {
        mobileCreatedCorrectly(softly);
    }

    private void assumeMobileWasCreatedSuccessfully() {
        boolean mobileCreatedCorrectly = mobileCreatedCorrectly(softly);
        TestUtils.assumeThat("Mobile record was not created correctly, dict may be corrupted",
                mobileCreatedCorrectly,
                is(true));
    }

    @Test
    public void updateMobileMultiplier() {
        assumeMobileWasCreatedSuccessfully();
        List<ActionLogRecord> records = processEvents(Fixtures.Mobile.update);
        softly.assertThat(records)
                .hasSize(1);
        if (!records.isEmpty()) {
            softly.assertThat(records.get(0)).isEqualToComparingFieldByFieldRecursively(ActionLogRecord.builder()
                    .withDateTime(NOW)
                    .withDb("ppc")
                    .withDirectTraceInfo(DirectTraceInfo.empty())
                    .withGtid(BinlogFixtureGenerator.SERVER_UUID + ":" + 101)
                    .withOperation(Operation.UPDATE)
                    .withPath(CAMPAIGN_PATH)
                    .withQuerySerial(0)
                    .withRowSerial(0)
                    .withType("hierarchical_multipliers")
                    .withOldFields(new FieldValueList(Arrays.asList(
                            // id и type всегда есть
                            new FieldValue<>(HM_TABLE.HIERARCHICAL_MULTIPLIER_ID.getName(), "8001"),
                            new FieldValue<>(HM_TABLE.IS_ENABLED.getName(), "1"),
                            new FieldValue<>(HM_TABLE.LAST_CHANGE.getName(), "2017-11-23 00:00:00"),
                            new FieldValue<>(HM_TABLE.MULTIPLIER_PCT.getName(), "80"),
                            new FieldValue<>(RELATED_IDS, ""),
                            new FieldValue<>(HM_TABLE.TYPE.getName(),
                                    HierarchicalMultipliersType.mobile_multiplier.getLiteral()),
                            new FieldValue<>(VERSION, "01"))))
                    .withNewFields(new FieldValueList(Arrays.asList(
                            new FieldValue<>(HM_TABLE.IS_ENABLED.getName(), "0"),
                            new FieldValue<>(HM_TABLE.LAST_CHANGE.getName(), "2017-11-23 00:00:01"),
                            new FieldValue<>(HM_TABLE.MULTIPLIER_PCT.getName(), "70"),
                            new FieldValue<>(VERSION, "01"))))
                    .withRecordSource(recordSource)
                    .build());
        }
    }

    @Test
    public void deleteMobileMultiplier() {
        assumeMobileWasCreatedSuccessfully();
        List<ActionLogRecord> records = processEvents(Fixtures.Mobile.delete);
        softly.assertThat(records)
                .hasSize(1);
        if (!records.isEmpty()) {
            softly.assertThat(records.get(0)).isEqualToComparingFieldByFieldRecursively(ActionLogRecord.builder()
                    .withDateTime(NOW)
                    .withDb("ppc")
                    .withDirectTraceInfo(DirectTraceInfo.empty())
                    .withGtid(BinlogFixtureGenerator.SERVER_UUID + ":" + 101)
                    .withOperation(Operation.DELETE)
                    .withPath(CAMPAIGN_PATH)
                    .withQuerySerial(0)
                    .withRowSerial(0)
                    .withType("hierarchical_multipliers")
                    .withOldFields(new FieldValueList(Arrays.asList(
                            new FieldValue<>(HM_TABLE.HIERARCHICAL_MULTIPLIER_ID.getName(), "8001"),
                            new FieldValue<>(HM_TABLE.IS_ENABLED.getName(), "1"),
                            new FieldValue<>(HM_TABLE.LAST_CHANGE.getName(), "2017-11-23 00:00:00"),
                            new FieldValue<>(HM_TABLE.MULTIPLIER_PCT.getName(), "80"),
                            new FieldValue<>(RELATED_IDS, ""),
                            new FieldValue<>(HM_TABLE.TYPE.getName(),
                                    HierarchicalMultipliersType.mobile_multiplier.getLiteral()),
                            new FieldValue<>(VERSION, "01"))))
                    .withNewFields(new FieldValueList(Collections.singletonList(
                            new FieldValue<>(VERSION, "01"))))
                    .withRecordSource(recordSource)
                    .build());
        }
    }

    @SuppressWarnings("squid:S2970")  // это не тест
    private boolean demographyCreatedCorrectly(JUnitSoftAssertions customSoftly) {
        List<ActionLogRecord> records = processEvents(
                Fixtures.Demography.createHm,
                Fixtures.Demography.createDmv1,
                Fixtures.Demography.createDmv2);
        customSoftly.assertThat(records)
                .describedAs("By now merging of consequent multipliers changes is not implemented."
                        + " So there should be two records for two multiplier endpoint changes.")
                .hasSize(2);
        if (records.size() >= 1) {
            customSoftly.assertThat(records.get(0))
                    .describedAs("First demography_multiplier_values")
                    .isEqualToComparingFieldByFieldRecursively(ActionLogRecord.builder()
                            .withDateTime(NOW.plusSeconds(1))
                            .withDb("ppc")
                            .withDirectTraceInfo(DirectTraceInfo.empty())
                            .withGtid(BinlogFixtureGenerator.SERVER_UUID + ":" + 101)
                            .withOperation(Operation.INSERT)
                            .withPath(CAMPAIGN_PATH)
                            .withQuerySerial(0)
                            .withRowSerial(0)
                            .withType("hierarchical_multipliers")
                            .withOldFields(new FieldValueList(Collections.singletonList(
                                    new FieldValue<>(VERSION, "01"))))
                            .withNewFields(new FieldValueList(Arrays.asList(
                                    // Суффиксы - ключ в ассоциативном массиве related_ids
                                    new FieldValue<>(DMV_TABLE.AGE.getName() + "_0",
                                            DemographyMultiplierValuesAge._25_34.getLiteral()),
                                    new FieldValue<>(DMV_TABLE.GENDER.getName() + "_0",
                                            DemographyMultiplierValuesGender.male.getLiteral()),
                                    new FieldValue<>(HM_TABLE.HIERARCHICAL_MULTIPLIER_ID.getName(), "8001"),
                                    new FieldValue<>(HM_TABLE.IS_ENABLED.getName(), "1"),

                                    // Среди last_changed от двух таблиц всегда берётся свежайшая
                                    new FieldValue<>(HM_TABLE.LAST_CHANGE.getName(), "2017-11-23 00:00:01"),

                                    // multiplier_pct всегда берётся из внешней таблицы
                                    new FieldValue<>(DMV_TABLE.MULTIPLIER_PCT.getName() + "_0", "110"),

                                    // Карта идентификаторов через запятую
                                    new FieldValue<>(RELATED_IDS, "0-9001"),

                                    new FieldValue<>(HM_TABLE.TYPE.getName(),
                                            HierarchicalMultipliersType.demography_multiplier.getLiteral()),
                                    new FieldValue<>(VERSION, "01"))))
                            .withRecordSource(recordSource)
                            .build());
        }
        if (records.size() >= 2) {
            customSoftly.assertThat(records.get(1))
                    .describedAs("First and second demography_multiplier_values. This INSERT query"
                            + " should look like update of the previous hierarchical_multipliers record.")
                    .isEqualToComparingFieldByFieldRecursively(ActionLogRecord.builder()
                            .withDateTime(NOW.plusSeconds(2))
                            .withDb("ppc")
                            .withDirectTraceInfo(DirectTraceInfo.empty())
                            .withGtid(BinlogFixtureGenerator.SERVER_UUID + ":" + 102)
                            .withOperation(Operation.UPDATE)
                            .withPath(CAMPAIGN_PATH)
                            .withQuerySerial(0)
                            .withRowSerial(0)
                            .withType("hierarchical_multipliers")
                            .withOldFields(new FieldValueList(Arrays.asList(
                                    // В before есть полные данные обо всей группе корректировок
                                    // Суффиксы - позиция в массиве related_ids
                                    new FieldValue<>(DMV_TABLE.AGE.getName() + "_0",
                                            DemographyMultiplierValuesAge._25_34.getLiteral()),
                                    new FieldValue<>(DMV_TABLE.GENDER.getName() + "_0",
                                            DemographyMultiplierValuesGender.male.getLiteral()),
                                    new FieldValue<>(HM_TABLE.HIERARCHICAL_MULTIPLIER_ID.getName(), "8001"),
                                    new FieldValue<>(HM_TABLE.IS_ENABLED.getName(), "1"),

                                    // Среди last_changed от двух таблиц всегда берётся свежайшая
                                    new FieldValue<>(HM_TABLE.LAST_CHANGE.getName(), "2017-11-23 00:00:01"),

                                    // multiplier_pct всегда берётся из внешней таблицы
                                    new FieldValue<>(DMV_TABLE.MULTIPLIER_PCT.getName() + "_0", "110"),

                                    // Карта идентификаторов через запятую
                                    new FieldValue<>(RELATED_IDS, "0-9001"),

                                    new FieldValue<>(HM_TABLE.TYPE.getName(),
                                            HierarchicalMultipliersType.demography_multiplier.getLiteral()),
                                    new FieldValue<>(VERSION, "01"))))
                            .withNewFields(new FieldValueList(Arrays.asList(
                                    // В after только то, что изменилось и добавилось
                                    new FieldValue<>(DMV_TABLE.AGE.getName() + "_1",
                                            DemographyMultiplierValuesAge._18_24.getLiteral()),
                                    new FieldValue<>(DMV_TABLE.GENDER.getName() + "_1",
                                            DemographyMultiplierValuesGender.female.getLiteral()),

                                    // Среди last_changed от двух таблиц всегда берётся свежайшая
                                    new FieldValue<>(HM_TABLE.LAST_CHANGE.getName(), "2017-11-23 00:00:02"),

                                    // multiplier_pct всегда берётся из внешней таблицы
                                    new FieldValue<>(DMV_TABLE.MULTIPLIER_PCT.getName() + "_1", "130"),

                                    // Карта идентификаторов через запятую
                                    new FieldValue<>(RELATED_IDS, "0-9001,1-9002"),
                                    new FieldValue<>(VERSION, "01"))))
                            .withRecordSource(recordSource)
                            .build());
        }
        String dictValue = (String) dictRepository.repositoryMap.get(
                new DictRequest(DictDataCategory.HIERARCHICAL_MULTIPLIERS_RECORD, 8001));
        HierarchicalMultipliersData.Root expected = new HierarchicalMultipliersData.Root()
                .withIsEnabled("1")
                .withLastChange("2017-11-23 00:00:02")
                .withMultiplierPct("")
                .withPath(CAMPAIGN_PATH)
                .withType(HierarchicalMultipliersType.demography_multiplier.getLiteral());
        expected.putRelated(9001L, new HierarchicalMultipliersData.Demography()
                .withAge(DemographyMultiplierValuesAge._25_34.getLiteral())
                .withGender(DemographyMultiplierValuesGender.male.getLiteral())
                .withMultiplierPct("110"));
        expected.putRelated(9002L, new HierarchicalMultipliersData.Demography()
                .withAge(DemographyMultiplierValuesAge._18_24.getLiteral())
                .withGender(DemographyMultiplierValuesGender.female.getLiteral())
                .withMultiplierPct("130"));
        customSoftly.assertThat(dictValue)
                .isEqualTo(HierarchicalMultipliersData.toDictValue(expected));
        return customSoftly.errorsCollected().isEmpty();
    }

    @Test
    public void createDemographyMultiplier() {
        demographyCreatedCorrectly(softly);
    }

    private void assumeDemographyWasCreatedSuccessfully() {
        boolean demographyCreatedCorrectly = demographyCreatedCorrectly(softly);
        TestUtils.assumeThat("demography record was not created correctly, dict may be corrupted",
                demographyCreatedCorrectly,
                is(true));
    }

    @Test
    public void updateDemographyMultiplier() {
        assumeDemographyWasCreatedSuccessfully();
        List<ActionLogRecord> records = processEvents(
                Fixtures.Demography.updateHm,
                Fixtures.Demography.updateDmv1);
        softly.assertThat(records)
                .hasSize(2);
        if (records.size() >= 1) {
            softly.assertThat(records.get(0))
                    .describedAs("Changed root table")
                    .isEqualToComparingFieldByFieldRecursively(ActionLogRecord.builder()
                            .withDateTime(NOW.plusSeconds(3))
                            .withDb("ppc")
                            .withDirectTraceInfo(DirectTraceInfo.empty())
                            .withGtid(BinlogFixtureGenerator.SERVER_UUID + ":" + 103)
                            .withOperation(Operation.UPDATE)
                            .withPath(CAMPAIGN_PATH)
                            .withQuerySerial(0)
                            .withRowSerial(0)
                            .withType("hierarchical_multipliers")
                            .withOldFields(new FieldValueList(Arrays.asList(
                                    // В before есть полные данные обо всей группе корректировок
                                    new FieldValue<>(DMV_TABLE.AGE.getName() + "_0",
                                            DemographyMultiplierValuesAge._25_34.getLiteral()),
                                    new FieldValue<>(DMV_TABLE.AGE.getName() + "_1",
                                            DemographyMultiplierValuesAge._18_24.getLiteral()),
                                    new FieldValue<>(DMV_TABLE.GENDER.getName() + "_0",
                                            DemographyMultiplierValuesGender.male.getLiteral()),
                                    new FieldValue<>(DMV_TABLE.GENDER.getName() + "_1",
                                            DemographyMultiplierValuesGender.female.getLiteral()),
                                    new FieldValue<>(HM_TABLE.HIERARCHICAL_MULTIPLIER_ID.getName(), "8001"),
                                    new FieldValue<>(HM_TABLE.IS_ENABLED.getName(), "1"),
                                    new FieldValue<>(HM_TABLE.LAST_CHANGE.getName(), "2017-11-23 00:00:02"),
                                    new FieldValue<>(DMV_TABLE.MULTIPLIER_PCT.getName() + "_0", "110"),
                                    new FieldValue<>(DMV_TABLE.MULTIPLIER_PCT.getName() + "_1", "130"),
                                    new FieldValue<>(RELATED_IDS, "0-9001,1-9002"),
                                    new FieldValue<>(HM_TABLE.TYPE.getName(),
                                            HierarchicalMultipliersType.demography_multiplier.getLiteral()),
                                    new FieldValue<>(VERSION, "01"))))
                            .withNewFields(new FieldValueList(Arrays.asList(
                                    // В after остаётся только то, что изменилось
                                    new FieldValue<>(HM_TABLE.IS_ENABLED.getName(), "0"),
                                    new FieldValue<>(HM_TABLE.LAST_CHANGE.getName(), "2017-11-23 00:00:03"),
                                    new FieldValue<>(VERSION, "01"))))
                            .withRecordSource(recordSource)
                            .build());
        }
        if (records.size() >= 2) {
            softly.assertThat(records.get(1))
                    .describedAs("Changed related table")
                    .isEqualToComparingFieldByFieldRecursively(ActionLogRecord.builder()
                            .withDateTime(NOW.plusSeconds(4))
                            .withDb("ppc")
                            .withDirectTraceInfo(DirectTraceInfo.empty())
                            .withGtid(BinlogFixtureGenerator.SERVER_UUID + ":" + 104)
                            .withOperation(Operation.UPDATE)
                            .withPath(CAMPAIGN_PATH)
                            .withQuerySerial(0)
                            .withRowSerial(0)
                            .withType("hierarchical_multipliers")
                            .withOldFields(new FieldValueList(Arrays.asList(
                                    // В before есть полные данные обо всей группе корректировок
                                    new FieldValue<>(DMV_TABLE.AGE.getName() + "_0",
                                            DemographyMultiplierValuesAge._25_34.getLiteral()),
                                    new FieldValue<>(DMV_TABLE.AGE.getName() + "_1",
                                            DemographyMultiplierValuesAge._18_24.getLiteral()),
                                    new FieldValue<>(DMV_TABLE.GENDER.getName() + "_0",
                                            DemographyMultiplierValuesGender.male.getLiteral()),
                                    new FieldValue<>(DMV_TABLE.GENDER.getName() + "_1",
                                            DemographyMultiplierValuesGender.female.getLiteral()),
                                    new FieldValue<>(HM_TABLE.HIERARCHICAL_MULTIPLIER_ID.getName(), "8001"),

                                    // Здесь уже в качестве старых данных - данные, которые были новыми
                                    // на прошлом UPDATE
                                    new FieldValue<>(HM_TABLE.IS_ENABLED.getName(), "0"),
                                    new FieldValue<>(HM_TABLE.LAST_CHANGE.getName(), "2017-11-23 00:00:03"),

                                    new FieldValue<>(DMV_TABLE.MULTIPLIER_PCT.getName() + "_0", "110"),
                                    new FieldValue<>(DMV_TABLE.MULTIPLIER_PCT.getName() + "_1", "130"),
                                    new FieldValue<>(RELATED_IDS, "0-9001,1-9002"),
                                    new FieldValue<>(HM_TABLE.TYPE.getName(),
                                            HierarchicalMultipliersType.demography_multiplier.getLiteral()),
                                    new FieldValue<>(VERSION, "01"))))
                            .withNewFields(new FieldValueList(Arrays.asList(
                                    // В after остаётся только то, что изменилось
                                    // Среди last_changed от двух таблиц всегда берётся свежайшая
                                    new FieldValue<>(DMV_TABLE.LAST_CHANGE.getName(), "2017-11-23 00:00:04"),
                                    // multiplier_pct всегда берётся из внешней таблицы
                                    new FieldValue<>(DMV_TABLE.MULTIPLIER_PCT.getName() + "_0", "90"),
                                    new FieldValue<>(VERSION, "01"))))
                            .withRecordSource(recordSource)
                            .build());
        }
    }

    @Test
    public void deleteDemographyMultiplier() {
        assumeDemographyWasCreatedSuccessfully();
        List<ActionLogRecord> records1 = processEvents(Fixtures.Demography.deleteDmv2);
        List<ActionLogRecord> records2 = processEvents(Fixtures.Demography.deleteDmv1);
        List<ActionLogRecord> records3 = processEvents(Fixtures.Demography.deleteHm);
        softly.assertThat(records1)
                .hasSize(1);
        if (!records1.isEmpty()) {
            softly.assertThat(records1.get(0))
                    .describedAs("Deleted one demography multiplier of two")
                    .isEqualToComparingFieldByFieldRecursively(ActionLogRecord.builder()
                            .withDateTime(NOW.plusSeconds(3))
                            .withDb("ppc")
                            .withDirectTraceInfo(DirectTraceInfo.empty())
                            .withGtid(BinlogFixtureGenerator.SERVER_UUID + ":" + 103)
                            .withOperation(Operation.UPDATE)
                            .withPath(CAMPAIGN_PATH)
                            .withQuerySerial(0)
                            .withRowSerial(0)
                            .withType("hierarchical_multipliers")
                            .withOldFields(new FieldValueList(Arrays.asList(
                                    // В before есть полные данные обо всей группе корректировок
                                    new FieldValue<>(DMV_TABLE.AGE.getName() + "_0",
                                            DemographyMultiplierValuesAge._25_34.getLiteral()),
                                    new FieldValue<>(DMV_TABLE.AGE.getName() + "_1",
                                            DemographyMultiplierValuesAge._18_24.getLiteral()),
                                    new FieldValue<>(DMV_TABLE.GENDER.getName() + "_0",
                                            DemographyMultiplierValuesGender.male.getLiteral()),
                                    new FieldValue<>(DMV_TABLE.GENDER.getName() + "_1",
                                            DemographyMultiplierValuesGender.female.getLiteral()),
                                    new FieldValue<>(HM_TABLE.HIERARCHICAL_MULTIPLIER_ID.getName(), "8001"),
                                    new FieldValue<>(HM_TABLE.IS_ENABLED.getName(), "1"),
                                    new FieldValue<>(HM_TABLE.LAST_CHANGE.getName(), "2017-11-23 00:00:02"),
                                    new FieldValue<>(DMV_TABLE.MULTIPLIER_PCT.getName() + "_0", "110"),
                                    new FieldValue<>(DMV_TABLE.MULTIPLIER_PCT.getName() + "_1", "130"),
                                    new FieldValue<>(RELATED_IDS, "0-9001,1-9002"),
                                    new FieldValue<>(HM_TABLE.TYPE.getName(),
                                            HierarchicalMultipliersType.demography_multiplier.getLiteral()),
                                    new FieldValue<>(VERSION, "01"))))
                            .withNewFields(new FieldValueList(Arrays.asList(
                                    // При удалении не меняется ни одно из реальных полей в таблице, в том числе не
                                    // меняется last_change. Так что last_change остаётся тем же, каким он и был. Увы.
                                    // Об удалении корректировки можно узнать через изменение related_ids
                                    new FieldValue<>(RELATED_IDS, "0-9001"),
                                    new FieldValue<>(VERSION, "01"))))
                            .withRecordSource(recordSource)
                            .build());
        }
        softly.assertThat(records2)
                .hasSize(1);
        if (!records2.isEmpty()) {
            softly.assertThat(records2.get(0))
                    .describedAs("Deleted two demography multipliers of two")
                    .isEqualToComparingFieldByFieldRecursively(ActionLogRecord.builder()
                            .withDateTime(NOW.plusSeconds(4))
                            .withDb("ppc")
                            .withDirectTraceInfo(DirectTraceInfo.empty())
                            .withGtid(BinlogFixtureGenerator.SERVER_UUID + ":" + 104)
                            .withOperation(Operation.DELETE)
                            .withPath(CAMPAIGN_PATH)
                            .withQuerySerial(0)
                            .withRowSerial(0)
                            .withType("hierarchical_multipliers")
                            .withOldFields(new FieldValueList(Arrays.asList(
                                    // В before есть полные данные обо всей группе корректировок
                                    new FieldValue<>(DMV_TABLE.AGE.getName() + "_0",
                                            DemographyMultiplierValuesAge._25_34.getLiteral()),
                                    new FieldValue<>(DMV_TABLE.GENDER.getName() + "_0",
                                            DemographyMultiplierValuesGender.male.getLiteral()),
                                    new FieldValue<>(HM_TABLE.HIERARCHICAL_MULTIPLIER_ID.getName(), "8001"),
                                    new FieldValue<>(HM_TABLE.IS_ENABLED.getName(), "1"),
                                    // При удалении не меняется ни одно из реальных полей в таблице, в том числе не
                                    // меняется last_change. Так что last_change остаётся тем же, каким он и был. Увы.
                                    new FieldValue<>(HM_TABLE.LAST_CHANGE.getName(), "2017-11-23 00:00:02"),
                                    new FieldValue<>(DMV_TABLE.MULTIPLIER_PCT.getName() + "_0", "110"),
                                    new FieldValue<>(RELATED_IDS, "0-9001"),
                                    new FieldValue<>(HM_TABLE.TYPE.getName(),
                                            HierarchicalMultipliersType.demography_multiplier.getLiteral()),
                                    new FieldValue<>(VERSION, "01"))))
                            .withNewFields(new FieldValueList(Collections.singletonList(
                                    new FieldValue<>(VERSION, "01"))))
                            .withRecordSource(recordSource)
                            .build());
        }
        softly.assertThat(records3)
                .describedAs("Deleting of record from root table should not be seen in log")
                .isEmpty();
    }

    @SuppressWarnings("squid:S2970")  // это не тест
    private boolean retargetingCreatedCorrectly(JUnitSoftAssertions customSoftly) {
        List<ActionLogRecord> records = processEvents(
                Fixtures.Retargeting.createHm,
                Fixtures.Retargeting.createRmv);
        customSoftly.assertThat(records)
                .hasSize(1);
        if (records.size() >= 1) {
            customSoftly.assertThat(records.get(0))
                    .describedAs("First retargeting_multiplier_values")
                    .isEqualToComparingFieldByFieldRecursively(ActionLogRecord.builder()
                            .withDateTime(NOW.plusSeconds(1))
                            .withDb("ppc")
                            .withDirectTraceInfo(DirectTraceInfo.empty())
                            .withGtid(BinlogFixtureGenerator.SERVER_UUID + ":" + 101)
                            .withOperation(Operation.INSERT)
                            .withPath(CAMPAIGN_PATH)
                            .withQuerySerial(0)
                            .withRowSerial(0)
                            .withType("hierarchical_multipliers")
                            .withOldFields(new FieldValueList(Collections.singletonList(
                                    new FieldValue<>(VERSION, "01"))))
                            .withNewFields(new FieldValueList(Arrays.asList(
                                    // Суффиксы - ключ в ассоциативном массиве related_ids
                                    new FieldValue<>(HM_TABLE.HIERARCHICAL_MULTIPLIER_ID.getName(), "8001"),
                                    new FieldValue<>(HM_TABLE.IS_ENABLED.getName(), "1"),

                                    // Среди last_changed от двух таблиц всегда берётся свежайшая
                                    new FieldValue<>(HM_TABLE.LAST_CHANGE.getName(), "2017-11-23 00:00:01"),

                                    // multiplier_pct всегда берётся из внешней таблицы
                                    new FieldValue<>(RMV_TABLE.MULTIPLIER_PCT.getName() + "_0", "80"),

                                    // Карта идентификаторов через запятую
                                    new FieldValue<>(RELATED_IDS, "0-9001"),

                                    new FieldValue<>(RMV_TABLE.RET_COND_ID.getName() + "_0", "5001"),
                                    new FieldValue<>("ret_cond_name_0", "my condition"),
                                    new FieldValue<>(HM_TABLE.TYPE.getName(),
                                            HierarchicalMultipliersType.retargeting_multiplier.getLiteral()),
                                    new FieldValue<>(VERSION, "01"))))
                            .withRecordSource(recordSource)
                            .build());
        }
        String dictValue = (String) dictRepository.repositoryMap.get(
                new DictRequest(DictDataCategory.HIERARCHICAL_MULTIPLIERS_RECORD, 8001L));
        HierarchicalMultipliersData.Root expected = new HierarchicalMultipliersData.Root()
                .withIsEnabled("1")
                .withLastChange("2017-11-23 00:00:01")
                .withMultiplierPct("")
                .withPath(CAMPAIGN_PATH)
                .withType(HierarchicalMultipliersType.retargeting_multiplier.getLiteral());
        expected.putRelated(9001L, new HierarchicalMultipliersData.Retargeting()
                .withMultiplierPct("80")
                .withRetCondId("5001")
                .withRetCondName("my condition"));
        customSoftly.assertThat(dictValue)
                .isEqualTo(HierarchicalMultipliersData.toDictValue(expected));
        return customSoftly.errorsCollected().isEmpty();
    }

    @Test
    public void createRetargetingMultiplier() {
        retargetingCreatedCorrectly(softly);
    }

    private void assumeRetargetingWasCreatedSuccessfully() {
        boolean retargetingCreatedCorrectly = retargetingCreatedCorrectly(softly);
        TestUtils.assumeThat("retargeting record was not created correctly, dict may be corrupted",
                retargetingCreatedCorrectly,
                is(true));
    }

    @Test
    public void updateRetargetingMultiplier() {
        assumeRetargetingWasCreatedSuccessfully();
        List<ActionLogRecord> records = processEvents(
                Fixtures.Retargeting.updateHm,
                Fixtures.Retargeting.updateRmv);
        softly.assertThat(records)
                .hasSize(2);
        if (records.size() >= 1) {
            softly.assertThat(records.get(0))
                    .describedAs("Changed only root table")
                    .isEqualToComparingFieldByFieldRecursively(ActionLogRecord.builder()
                            .withDateTime(NOW.plusSeconds(2))
                            .withDb("ppc")
                            .withDirectTraceInfo(DirectTraceInfo.empty())
                            .withGtid(BinlogFixtureGenerator.SERVER_UUID + ":" + 102)
                            .withOperation(Operation.UPDATE)
                            .withPath(CAMPAIGN_PATH)
                            .withQuerySerial(0)
                            .withRowSerial(0)
                            .withType("hierarchical_multipliers")
                            .withOldFields(new FieldValueList(Arrays.asList(
                                    // Суффиксы - ключ в ассоциативном массиве related_ids
                                    new FieldValue<>(HM_TABLE.HIERARCHICAL_MULTIPLIER_ID.getName(), "8001"),
                                    new FieldValue<>(HM_TABLE.IS_ENABLED.getName(), "1"),

                                    // Среди last_changed от двух таблиц всегда берётся свежайшая
                                    new FieldValue<>(HM_TABLE.LAST_CHANGE.getName(), "2017-11-23 00:00:01"),

                                    // multiplier_pct всегда берётся из внешней таблицы
                                    new FieldValue<>(RMV_TABLE.MULTIPLIER_PCT.getName() + "_0", "80"),

                                    // Карта идентификаторов через запятую
                                    new FieldValue<>(RELATED_IDS, "0-9001"),

                                    new FieldValue<>(RMV_TABLE.RET_COND_ID.getName() + "_0", "5001"),
                                    new FieldValue<>("ret_cond_name_0", "my condition"),
                                    new FieldValue<>(HM_TABLE.TYPE.getName(),
                                            HierarchicalMultipliersType.retargeting_multiplier.getLiteral()),
                                    new FieldValue<>(VERSION, "01"))))
                            .withNewFields(new FieldValueList(Arrays.asList(
                                    new FieldValue<>(HM_TABLE.IS_ENABLED.getName(), "0"),
                                    new FieldValue<>(HM_TABLE.LAST_CHANGE.getName(), "2017-11-23 00:00:02"),
                                    new FieldValue<>(VERSION, "01"))))
                            .withRecordSource(recordSource)
                            .build());
        }
        if (records.size() >= 2) {
            softly.assertThat(records.get(1))
                    .describedAs("Changed only related table")
                    .isEqualToComparingFieldByFieldRecursively(ActionLogRecord.builder()
                            .withDateTime(NOW.plusSeconds(3))
                            .withDb("ppc")
                            .withDirectTraceInfo(DirectTraceInfo.empty())
                            .withGtid(BinlogFixtureGenerator.SERVER_UUID + ":" + 103)
                            .withOperation(Operation.UPDATE)
                            .withPath(CAMPAIGN_PATH)
                            .withQuerySerial(0)
                            .withRowSerial(0)
                            .withType("hierarchical_multipliers")
                            .withOldFields(new FieldValueList(Arrays.asList(
                                    // Суффиксы - ключ в ассоциативном массиве related_ids
                                    new FieldValue<>(HM_TABLE.HIERARCHICAL_MULTIPLIER_ID.getName(), "8001"),
                                    new FieldValue<>(HM_TABLE.IS_ENABLED.getName(), "0"),

                                    // Среди last_changed от двух таблиц всегда берётся свежайшая
                                    new FieldValue<>(HM_TABLE.LAST_CHANGE.getName(), "2017-11-23 00:00:02"),

                                    // multiplier_pct всегда берётся из внешней таблицы
                                    new FieldValue<>(RMV_TABLE.MULTIPLIER_PCT.getName() + "_0", "80"),

                                    // Карта идентификаторов через запятую
                                    new FieldValue<>(RELATED_IDS, "0-9001"),

                                    new FieldValue<>(RMV_TABLE.RET_COND_ID.getName() + "_0", "5001"),
                                    new FieldValue<>("ret_cond_name_0", "my condition"),
                                    new FieldValue<>(HM_TABLE.TYPE.getName(),
                                            HierarchicalMultipliersType.retargeting_multiplier.getLiteral()),
                                    new FieldValue<>(VERSION, "01"))))
                            .withNewFields(new FieldValueList(Arrays.asList(
                                    new FieldValue<>(RMV_TABLE.LAST_CHANGE.getName(), "2017-11-23 00:00:03"),
                                    new FieldValue<>(RMV_TABLE.MULTIPLIER_PCT.getName() + "_0", "90"),
                                    new FieldValue<>(VERSION, "01"))))
                            .withRecordSource(recordSource)
                            .build());
        }
    }

    private static class Fixtures {
        // Все эти таблицы не содержат text/blob-полей.
        // Благодаря режиму noblob в before и в after всегда полные кортежи.

        private Fixtures() {
        }

        static class Mobile {
            private static final ImmutableList<Pair<TableField, Serializable>> createRowPairs = ImmutableList.of(
                    Pair.of(HM_TABLE.CID, 2001),
                    Pair.of(HM_TABLE.HIERARCHICAL_MULTIPLIER_ID, 8001),
                    Pair.of(HM_TABLE.IS_ENABLED, 1),
                    Pair.of(HM_TABLE.LAST_CHANGE, "2017-11-23 00:00:00"),
                    Pair.of(HM_TABLE.MULTIPLIER_PCT, 80),
                    Pair.of(HM_TABLE.PID, null),
                    Pair.of(HM_TABLE.TYPE, HierarchicalMultipliersType.mobile_multiplier.getLiteral()));
            static final EnrichedEvent create = BinlogFixtureGenerator.createInsertEvent(NOW,
                    100,
                    HM_TABLE,
                    createRowPairs);
            static final EnrichedEvent update = BinlogFixtureGenerator.createUpdateEvent(NOW,
                    101,
                    HM_TABLE,
                    createRowPairs,
                    ImmutableList.of(
                            Pair.of(HM_TABLE.CID, 2001),
                            Pair.of(HM_TABLE.HIERARCHICAL_MULTIPLIER_ID, 8001),
                            Pair.of(HM_TABLE.IS_ENABLED, 0),  // change
                            Pair.of(HM_TABLE.LAST_CHANGE, "2017-11-23 00:00:01"),  // change
                            Pair.of(HM_TABLE.MULTIPLIER_PCT, 70),  // change
                            Pair.of(HM_TABLE.PID, null),
                            Pair.of(HM_TABLE.TYPE, HierarchicalMultipliersType.mobile_multiplier.getLiteral())));
            static final EnrichedEvent delete = BinlogFixtureGenerator.createDeleteEvent(NOW,
                    101,
                    HM_TABLE,
                    createRowPairs);
        }

        static class Demography {
            private static final ImmutableList<Pair<TableField, Serializable>> createHmPairs = ImmutableList.of(
                    Pair.of(HM_TABLE.CID, 2001),
                    Pair.of(HM_TABLE.HIERARCHICAL_MULTIPLIER_ID, 8001),
                    Pair.of(HM_TABLE.IS_ENABLED, 1),
                    Pair.of(HM_TABLE.LAST_CHANGE, "2017-11-23 00:00:00"),
                    Pair.of(HM_TABLE.MULTIPLIER_PCT, null),
                    Pair.of(HM_TABLE.PID, null),
                    Pair.of(HM_TABLE.TYPE, HierarchicalMultipliersType.demography_multiplier.getLiteral()));
            static final EnrichedEvent createHm = BinlogFixtureGenerator.createInsertEvent(NOW,
                    100,
                    HM_TABLE,
                    createHmPairs);
            static final EnrichedEvent updateHm = BinlogFixtureGenerator.createUpdateEvent(NOW.plusSeconds(3),
                    103,
                    HM_TABLE,
                    createHmPairs,
                    ImmutableList.of(
                            Pair.of(HM_TABLE.CID, 2001),
                            Pair.of(HM_TABLE.HIERARCHICAL_MULTIPLIER_ID, 8001),
                            Pair.of(HM_TABLE.IS_ENABLED, 0),  // change
                            Pair.of(HM_TABLE.LAST_CHANGE, "2017-11-23 00:00:03"),  // change
                            Pair.of(HM_TABLE.MULTIPLIER_PCT, null),
                            Pair.of(HM_TABLE.PID, null),
                            Pair.of(HM_TABLE.TYPE, HierarchicalMultipliersType.demography_multiplier.getLiteral())));
            static final EnrichedEvent deleteHm = BinlogFixtureGenerator.createDeleteEvent(NOW.plusSeconds(5),
                    105,
                    HM_TABLE,
                    createHmPairs);
            private static final ImmutableList<Pair<TableField, Serializable>> createDmv1Pairs = ImmutableList.of(
                    Pair.of(DMV_TABLE.DEMOGRAPHY_MULTIPLIER_VALUE_ID, 9001),
                    Pair.of(DMV_TABLE.LAST_CHANGE, "2017-11-23 00:00:01"),
                    Pair.of(DMV_TABLE.HIERARCHICAL_MULTIPLIER_ID, 8001),
                    Pair.of(DMV_TABLE.GENDER, DemographyMultiplierValuesGender.male.getLiteral()),
                    Pair.of(DMV_TABLE.AGE, DemographyMultiplierValuesAge._25_34.getLiteral()),
                    Pair.of(DMV_TABLE.MULTIPLIER_PCT, 110));
            static final EnrichedEvent createDmv1 = BinlogFixtureGenerator.createInsertEvent(NOW.plusSeconds(1),
                    101,
                    DMV_TABLE,
                    createDmv1Pairs);
            static final EnrichedEvent updateDmv1 = BinlogFixtureGenerator.createUpdateEvent(NOW.plusSeconds(4),
                    104,
                    DMV_TABLE,
                    createDmv1Pairs,
                    ImmutableList.of(
                            Pair.of(DMV_TABLE.DEMOGRAPHY_MULTIPLIER_VALUE_ID, 9001),
                            Pair.of(DMV_TABLE.LAST_CHANGE, "2017-11-23 00:00:04"),  // change
                            Pair.of(DMV_TABLE.HIERARCHICAL_MULTIPLIER_ID, 8001),
                            Pair.of(DMV_TABLE.GENDER, DemographyMultiplierValuesGender.male.getLiteral()),
                            Pair.of(DMV_TABLE.AGE, DemographyMultiplierValuesAge._25_34.getLiteral()),
                            Pair.of(DMV_TABLE.MULTIPLIER_PCT, 90))); // change
            static final EnrichedEvent deleteDmv1 = BinlogFixtureGenerator.createDeleteEvent(NOW.plusSeconds(4),
                    104,
                    DMV_TABLE,
                    createDmv1Pairs);
            private static final ImmutableList<Pair<TableField, Serializable>> createDmv2Pairs = ImmutableList.of(
                    Pair.of(DMV_TABLE.DEMOGRAPHY_MULTIPLIER_VALUE_ID, 9002),
                    Pair.of(DMV_TABLE.LAST_CHANGE, "2017-11-23 00:00:02"),
                    Pair.of(DMV_TABLE.HIERARCHICAL_MULTIPLIER_ID, 8001),
                    Pair.of(DMV_TABLE.GENDER, DemographyMultiplierValuesGender.female.getLiteral()),
                    Pair.of(DMV_TABLE.AGE, DemographyMultiplierValuesAge._18_24.getLiteral()),
                    Pair.of(DMV_TABLE.MULTIPLIER_PCT, 130));
            static final EnrichedEvent createDmv2 = BinlogFixtureGenerator.createInsertEvent(NOW.plusSeconds(2),
                    102,
                    DMV_TABLE,
                    createDmv2Pairs);
            static final EnrichedEvent deleteDmv2 = BinlogFixtureGenerator.createDeleteEvent(NOW.plusSeconds(3),
                    103,
                    DMV_TABLE,
                    createDmv2Pairs);
        }

        static class Retargeting {
            private static final ImmutableList<Pair<TableField, Serializable>> createHmPairs = ImmutableList.of(
                    Pair.of(HM_TABLE.CID, 2001),
                    Pair.of(HM_TABLE.HIERARCHICAL_MULTIPLIER_ID, 8001),
                    Pair.of(HM_TABLE.IS_ENABLED, 1),
                    Pair.of(HM_TABLE.LAST_CHANGE, "2017-11-23 00:00:00"),
                    Pair.of(HM_TABLE.MULTIPLIER_PCT, null),
                    Pair.of(HM_TABLE.PID, null),
                    Pair.of(HM_TABLE.TYPE, HierarchicalMultipliersType.retargeting_multiplier.getLiteral()));
            static final EnrichedEvent createHm = BinlogFixtureGenerator.createInsertEvent(NOW,
                    100,
                    HM_TABLE,
                    createHmPairs);
            static final EnrichedEvent updateHm = BinlogFixtureGenerator.createUpdateEvent(NOW.plusSeconds(2),
                    102,
                    HM_TABLE,
                    createHmPairs,
                    ImmutableList.of(
                            Pair.of(HM_TABLE.CID, 2001),
                            Pair.of(HM_TABLE.HIERARCHICAL_MULTIPLIER_ID, 8001),
                            Pair.of(HM_TABLE.IS_ENABLED, 0),  // change
                            Pair.of(HM_TABLE.LAST_CHANGE, "2017-11-23 00:00:02"),  // change
                            Pair.of(HM_TABLE.MULTIPLIER_PCT, null),
                            Pair.of(HM_TABLE.PID, null),
                            Pair.of(HM_TABLE.TYPE, HierarchicalMultipliersType.retargeting_multiplier.getLiteral())));
            private static final ImmutableList<Pair<TableField, Serializable>> createRmvPairs = ImmutableList.of(
                    Pair.of(RMV_TABLE.RETARGETING_MULTIPLIER_VALUE_ID, 9001),
                    Pair.of(RMV_TABLE.LAST_CHANGE, "2017-11-23 00:00:01"),
                    Pair.of(RMV_TABLE.HIERARCHICAL_MULTIPLIER_ID, 8001),
                    Pair.of(RMV_TABLE.RET_COND_ID, 5001),
                    Pair.of(RMV_TABLE.MULTIPLIER_PCT, 80));
            static final EnrichedEvent createRmv = BinlogFixtureGenerator.createInsertEvent(NOW.plusSeconds(1),
                    101,
                    RMV_TABLE,
                    createRmvPairs);
            static final EnrichedEvent updateRmv = BinlogFixtureGenerator.createUpdateEvent(NOW.plusSeconds(3),
                    103,
                    RMV_TABLE,
                    createRmvPairs,
                    ImmutableList.of(
                            Pair.of(RMV_TABLE.RETARGETING_MULTIPLIER_VALUE_ID, 9001),
                            Pair.of(RMV_TABLE.LAST_CHANGE, "2017-11-23 00:00:03"),  // change
                            Pair.of(RMV_TABLE.HIERARCHICAL_MULTIPLIER_ID, 8001),
                            Pair.of(RMV_TABLE.RET_COND_ID, 5001),
                            Pair.of(RMV_TABLE.MULTIPLIER_PCT, 90)));  // change
        }
    }
}
