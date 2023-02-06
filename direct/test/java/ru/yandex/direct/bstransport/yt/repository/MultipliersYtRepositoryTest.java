package ru.yandex.direct.bstransport.yt.repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import ru.yandex.adv.direct.expression.DeleteDirectMultipliersRow;
import ru.yandex.adv.direct.expression.DirectMultipliersRow;
import ru.yandex.adv.direct.expression.MultiplierAtom;
import ru.yandex.adv.direct.expression.MultiplierChangeRequest;
import ru.yandex.adv.direct.expression.TargetingExpression;
import ru.yandex.adv.direct.expression.TargetingExpressionAtom;
import ru.yandex.adv.direct.expression.keywords.KeywordEnum;
import ru.yandex.adv.direct.expression.multipler.type.MultiplierTypeEnum;
import ru.yandex.adv.direct.expression.operations.OperationEnum;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeProtoUtils;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultipliersYtRepositoryTest {
    @Test
    void getSchemaWithMappingTest() {
        var targetingExpression = TargetingExpression.newBuilder()
                .addAND(
                        TargetingExpression.Disjunction.newBuilder()
                                .addOR(
                                        TargetingExpressionAtom.newBuilder()
                                                .setKeyword(KeywordEnum.CryptaSocdemGender)
                                                .setOperation(OperationEnum.Equal)
                                                .setValue("1")
                                                .build()
                                )
                );
        MultiplierAtom multiplierAtom = MultiplierAtom.newBuilder()
                .setMultiplier(12)
                .setCondition(targetingExpression)
                .build();

        var row = DirectMultipliersRow.newBuilder()
                .setOrderID(12L)
                .setAdGroupID(124L)
                .setType(MultiplierTypeEnum.AutoVideoDirect)
                .addAllMultipliers(List.of(multiplierAtom))
                .setIsEnabled(true)
                .build();
        MultiplierChangeRequest request = MultiplierChangeRequest.newBuilder()
                .setUpsertRequest(row)
                .build();

        var multipliersYtRepository = mock(MultipliersYtRepository.class);
        when(multipliersYtRepository.getSchemaWithMapping()).thenCallRealMethod();
        var columnWithMappers = multipliersYtRepository.getSchemaWithMapping();
        var gotColumnNameToValue = columnWithMappers.stream()
                .collect(toMap(columnWithMapper -> columnWithMapper.getColumnSchema().getName(),
                        columnWithMapper -> columnWithMapper.getFromProtoToYtMapper().invoke(request)));

        var expectedMultipliers = YTree.builder().beginList()
                .value(YTreeProtoUtils.marshal(multiplierAtom))
                .buildList();
        var expectedColumnNameToValue = Map.of(
                "OrderID", 12L,
                "AdGroupID", 124L,
                "IsEnabled", true,
                "Type", "AutoVideoDirect",
                "Multipliers", expectedMultipliers
        );
        assertThat(gotColumnNameToValue).isEqualTo(expectedColumnNameToValue);
    }

    @Test
    void getSchemaWithMappingDeleteRequestTest() {

        var deleteRequest = DeleteDirectMultipliersRow.newBuilder()
                .setAdGroupID(125L)
                .setOrderID(15L)
                .setType(MultiplierTypeEnum.DeviceType);

        MultiplierChangeRequest request = MultiplierChangeRequest.newBuilder()
                .setDeleteRequest(deleteRequest)
                .build();

        var multipliersYtRepository = mock(MultipliersYtRepository.class);
        when(multipliersYtRepository.getSchemaWithMapping()).thenCallRealMethod();
        var columnWithMappers = multipliersYtRepository.getSchemaWithMapping();
        var gotColumnNameToValue = columnWithMappers.stream()
                .filter(columnWithMapper -> Objects.nonNull(columnWithMapper.getColumnSchema().getSortOrder()))
                .collect(toMap(columnWithMapper -> columnWithMapper.getColumnSchema().getName(),
                        columnWithMapper -> columnWithMapper.getFromProtoToYtMapper().invoke(request)));

        var expectedColumnNameToValue = Map.of(
                "OrderID", 15L,
                "AdGroupID", 125L,
                "Type", "DeviceType"
        );
        assertThat(gotColumnNameToValue).isEqualTo(expectedColumnNameToValue);
    }
}
