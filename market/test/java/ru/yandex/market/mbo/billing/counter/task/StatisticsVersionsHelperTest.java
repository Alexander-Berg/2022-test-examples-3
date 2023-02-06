package ru.yandex.market.mbo.billing.counter.task;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.YangLogStorage;
import ru.yandex.market.mbo.statistic.model.RawStatistics;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author dergachevfv
 * @since 11/29/19
 */
public class StatisticsVersionsHelperTest {

    private static final Long MODEL_ID = 1L;
    private static final long USER1 = 10L;
    private static final long USER2 = 20L;

    @Test
    public void testModelStatisticToRecentVersion() {
        RawStatistics rawStatistics = new RawStatistics(new Date(),
            YangLogStorage.YangLogStoreRequest.newBuilder()
                .addModelStatistic(YangLogStorage.ModelStatistic.newBuilder()
                    .setModelId(MODEL_ID)
                    .setType(ModelStorage.ModelType.GURU)
                    .setContractorChanges(YangLogStorage.ActionCount.newBuilder()
                        .setAliases(2)
                        .setBarCode(1)
                        .setCutOffWord(1)
                        .setIsSku(1)
                        .setParam(1)
                        .addParamIds(100L)
                        .setPickerAdded(1)
                        .setPictureUploaded(1)
                        .setVendorCode(1)
                        .build())
                    .setInspectorChanges(YangLogStorage.ActionCount.newBuilder()
                        .setAliases(2)
                        .setBarCode(1)
                        .setCutOffWord(1)
                        .setIsSku(1)
                        .setParam(1)
                        .addParamIds(100L)
                        .setPickerAdded(1)
                        .setPictureUploaded(1)
                        .setVendorCode(1)
                        .build())
                    .setInspectorCorrections(YangLogStorage.ActionCount.newBuilder()
                        .setAliases(2)
                        .setBarCode(1)
                        .setCutOffWord(1)
                        .setIsSku(1)
                        .setParam(1)
                        .addParamIds(100L)
                        .setPickerAdded(1)
                        .setPictureUploaded(1)
                        .setVendorCode(1)
                        .build())
                    .build())
                .build());

        RawStatistics actualizedRawStatistics = StatisticsVersionsHelper.toRecentVersion(rawStatistics);

        YangLogStorage.YangLogStoreRequest data = actualizedRawStatistics.getData();

        Assertions.assertThat(data.getModelStatisticCount()).isEqualTo(1);
        Assertions.assertThat(data.getModelStatistic(0)).isEqualTo(
            YangLogStorage.ModelStatistic.newBuilder()
                .setModelId(MODEL_ID)
                .setType(ModelStorage.ModelType.GURU)
                .setContractorActions(YangLogStorage.ModelActions.newBuilder()
                    .addAllAliases(toActionInfos(2))
                    .addAllBarCode(toActionInfos(1))
                    .addAllCutOffWord(toActionInfos(1))
                    .addAllIsSku(toActionInfos(1))
                    .addAllParam(toActionInfos(Collections.singletonList(100L)))
                    .addAllPickerAdded(toActionInfos(1))
                    .addAllPictureUploaded(toActionInfos(1))
                    .addAllVendorCode(toActionInfos(1))
                    .build())
                .setInspectorActions(YangLogStorage.ModelActions.newBuilder()
                    .addAllAliases(toActionInfos(2))
                    .addAllBarCode(toActionInfos(1))
                    .addAllCutOffWord(toActionInfos(1))
                    .addAllIsSku(toActionInfos(1))
                    .addAllParam(toActionInfos(Collections.singletonList(100L)))
                    .addAllPickerAdded(toActionInfos(1))
                    .addAllPictureUploaded(toActionInfos(1))
                    .addAllVendorCode(toActionInfos(1))
                    .build())
                .setCorrectionsActions(YangLogStorage.ModelActions.newBuilder()
                    .addAllAliases(toActionInfos(2))
                    .addAllBarCode(toActionInfos(1))
                    .addAllCutOffWord(toActionInfos(1))
                    .addAllIsSku(toActionInfos(1))
                    .addAllParam(toActionInfos(Collections.singletonList(100L)))
                    .addAllPickerAdded(toActionInfos(1))
                    .addAllPictureUploaded(toActionInfos(1))
                    .addAllVendorCode(toActionInfos(1))
                    .build())
                .build()
        );
    }

    @Test
    public void testParameterStatisticToRecentVersion() {
        RawStatistics rawStatistics = new RawStatistics(new Date(),
            YangLogStorage.YangLogStoreRequest.newBuilder()
                .addParameterStatistic(YangLogStorage.ParameterStatistic.newBuilder()
                    .setEntityId(MODEL_ID)
                    .addChanges(YangLogStorage.ParameterActions.newBuilder()
                        .setUid(USER1)
                        .setChangesType(YangLogStorage.ChangesType.CONTRACTOR)
                        .setCreatedInTask(true)
                        .setCutOffWords(1))
                    .addChanges(YangLogStorage.ParameterActions.newBuilder()
                        .setUid(USER2)
                        .setChangesType(YangLogStorage.ChangesType.INSPECTOR)
                        .setCreatedInTask(false)
                        .setAliases(2))
                    .addChanges(YangLogStorage.ParameterActions.newBuilder()
                        .setUid(USER2)
                        .setChangesType(YangLogStorage.ChangesType.CORRECTIONS)
                        .setCreatedInTask(false)
                        .setAliases(1)
                        .setCutOffWords(2)))
                .build());

        RawStatistics actualizedRawStatistics = StatisticsVersionsHelper.toRecentVersion(rawStatistics);

        YangLogStorage.YangLogStoreRequest data = actualizedRawStatistics.getData();

        Assertions.assertThat(data.getParameterStatisticCount()).isEqualTo(1);
        Assertions.assertThat(data.getParameterStatistic(0)).isEqualTo(
            YangLogStorage.ParameterStatistic.newBuilder()
                .setEntityId(MODEL_ID)
                .addChanges(YangLogStorage.ParameterActions.newBuilder()
                    .setUid(USER1)
                    .setChangesType(YangLogStorage.ChangesType.CONTRACTOR)
                    .setCreatedInTask(true)
                    .setCreatedAction(createDefaultActionInfo())
                    .addAllCutOffWordsActions(toActionInfos(1)))
                .addChanges(YangLogStorage.ParameterActions.newBuilder()
                    .setUid(USER2)
                    .setChangesType(YangLogStorage.ChangesType.INSPECTOR)
                    .setCreatedInTask(false)
                    .addAllAliasesActions(toActionInfos(2)))
                .addChanges(YangLogStorage.ParameterActions.newBuilder()
                    .setUid(USER2)
                    .setChangesType(YangLogStorage.ChangesType.CORRECTIONS)
                    .setCreatedInTask(false)
                    .addAllAliasesActions(toActionInfos(1))
                    .addAllCutOffWordsActions(toActionInfos(2)))
                .build()
        );
    }

    @Test
    public void testCategoryStatisticToRecentVersion() {
        RawStatistics rawStatistics = new RawStatistics(new Date(),
            YangLogStorage.YangLogStoreRequest.newBuilder()
                .setCategoryStatistic(YangLogStorage.CategoryStatistic.newBuilder()
                    .setContractorChanges(
                        YangLogStorage.CategoryActions.newBuilder()
                            .setCutOffWords(1))
                    .setInspectorChanges(
                        YangLogStorage.CategoryActions.newBuilder()
                            .setCutOffWords(2))
                    .setInspectorCorrections(
                        YangLogStorage.CategoryActions.newBuilder()
                            .setCutOffWords(1 + 2)))
                .build());

        RawStatistics actualizedRawStatistics = StatisticsVersionsHelper.toRecentVersion(rawStatistics);

        YangLogStorage.YangLogStoreRequest data = actualizedRawStatistics.getData();

        Assertions.assertThat(data.hasCategoryStatistic()).isTrue();
        Assertions.assertThat(data.getCategoryStatistic()).isEqualTo(
            YangLogStorage.CategoryStatistic.newBuilder()
                .setContractorChanges(
                    YangLogStorage.CategoryActions.newBuilder()
                        .addAllCutOffWordsActions(toActionInfos(1)))
                .setInspectorChanges(
                    YangLogStorage.CategoryActions.newBuilder()
                        .addAllCutOffWordsActions(toActionInfos(2)))
                .setInspectorCorrections(
                    YangLogStorage.CategoryActions.newBuilder()
                        .addAllCutOffWordsActions(toActionInfos(1 + 2)))
                .build()
        );
    }

    private static List<YangLogStorage.ActionInfo> toActionInfos(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> createDefaultActionInfo().build()
            ).collect(Collectors.toList());
    }

    private static List<YangLogStorage.ActionInfo> toActionInfos(List<Long> entityIds) {
        return entityIds.stream()
            .map(entityId -> createDefaultActionInfo()
                .setEntityId(entityId)
                .build()
            ).collect(Collectors.toList());
    }

    public static YangLogStorage.ActionInfo.Builder createDefaultActionInfo() {
        return YangLogStorage.ActionInfo.newBuilder()
            .setAuditActionId(-1);
    }
}
