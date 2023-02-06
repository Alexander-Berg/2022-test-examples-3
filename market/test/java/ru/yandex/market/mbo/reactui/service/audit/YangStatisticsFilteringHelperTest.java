package ru.yandex.market.mbo.reactui.service.audit;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.http.YangLogStorage;

import java.util.List;

/**
 * @author dergachevfv
 * @since 12/30/19
 */
public class YangStatisticsFilteringHelperTest {

    public static final long USER_1 = 1L;
    public static final long USER_2 = 2L;

    @Test
    public void testFilterOperatorStatistics() {
        YangLogStorage.YangLogStoreRequest statistics = YangLogStorage.YangLogStoreRequest.newBuilder()
            .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder()
                .setUid(USER_1)
                .build())
            .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder()
                .setUid(USER_2)
                .build())
            .addAllModelStatistic(List.of(
                YangLogStorage.ModelStatistic.newBuilder()
                    .setCreatedAction(YangLogStorage.ActionInfo.newBuilder().setAuditActionId(1L))
                    .setCreatedByUid(USER_2)
                    .setContractorActions(YangLogStorage.ModelActions.newBuilder().build())
                    .setInspectorActions(YangLogStorage.ModelActions.newBuilder().build())
                    .setCorrectionsActions(YangLogStorage.ModelActions.newBuilder().build())
                    .build()
            ))
            .addAllParameterStatistic(List.of(
                YangLogStorage.ParameterStatistic.newBuilder()
                    .addChanges(YangLogStorage.ParameterActions.newBuilder()
                        .setUid(USER_1))
                    .addChanges(YangLogStorage.ParameterActions.newBuilder()
                        .setUid(USER_2))
                    .build()
            ))
            .setCategoryStatistic(YangLogStorage.CategoryStatistic.newBuilder()
                .setContractorChanges(YangLogStorage.CategoryActions.newBuilder().build())
                .setInspectorChanges(YangLogStorage.CategoryActions.newBuilder().build())
                .setInspectorCorrections(YangLogStorage.CategoryActions.newBuilder().build())
                .build())
            .addAllMappingStatistic(List.of(
                YangLogStorage.MappingStatistic.newBuilder()
                    .setUid(USER_1)
                    .build(),
                YangLogStorage.MappingStatistic.newBuilder()
                    .setUid(USER_2)
                    .build()
            ))
            .addAllMappingModerationStatistic(List.of(
                YangLogStorage.MappingModerationStatistic.newBuilder()
                    .setUid(USER_1)
                    .build(),
                YangLogStorage.MappingModerationStatistic.newBuilder()
                    .setUid(USER_2)
                    .build()
            ))
            .addAllMatchingStatistic(List.of(
                YangLogStorage.MatchingStatistic.newBuilder()
                    .setUid(USER_1)
                    .build(),
                YangLogStorage.MatchingStatistic.newBuilder()
                    .setUid(USER_2)
                    .build()
            ))
            .build();

        YangLogStorage.YangLogStoreRequest operatorStatistics =
            YangStatisticsFilteringHelper.filterOperatorStatistics(statistics, USER_1);


        YangLogStorage.YangLogStoreRequest expected = YangLogStorage.YangLogStoreRequest.newBuilder()
            .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder()
                .setUid(USER_1)
                .build())
            .addAllModelStatistic(List.of(
                YangLogStorage.ModelStatistic.newBuilder()
                    .setContractorActions(YangLogStorage.ModelActions.newBuilder().build())
                    .build()
            ))
            .addAllParameterStatistic(List.of(
                YangLogStorage.ParameterStatistic.newBuilder()
                    .addChanges(YangLogStorage.ParameterActions.newBuilder()
                        .setUid(USER_1))
                    .build()
            ))
            .setCategoryStatistic(YangLogStorage.CategoryStatistic.newBuilder()
                .setContractorChanges(YangLogStorage.CategoryActions.newBuilder().build())
                .build())
            .addAllMappingStatistic(List.of(
                YangLogStorage.MappingStatistic.newBuilder()
                    .setUid(USER_1)
                    .build()
            ))
            .addAllMappingModerationStatistic(List.of(
                YangLogStorage.MappingModerationStatistic.newBuilder()
                    .setUid(USER_1)
                    .build()
            ))
            .addAllMatchingStatistic(List.of(
                YangLogStorage.MatchingStatistic.newBuilder()
                    .setUid(USER_1)
                    .build()
            ))
            .build();

        Assertions.assertThat(operatorStatistics).isEqualTo(expected);
    }
}
