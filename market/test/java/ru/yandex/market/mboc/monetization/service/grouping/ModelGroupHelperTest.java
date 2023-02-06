package ru.yandex.market.mboc.monetization.service.grouping;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage.Model;
import ru.yandex.market.mbo.http.ModelStorage.ParameterValue;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.enums.ConfigParameterType;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.enums.GroupType;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.ConfigParameter;
import ru.yandex.market.mboc.common.repo.bindings.pojos.ModelGroupParameterValue;
import ru.yandex.market.mboc.common.repo.bindings.pojos.ModelGroupParameterValues;
import ru.yandex.market.mboc.common.repo.bindings.pojos.MskuParameterWithOptionName;

/**
 * @author danfertev
 * @since 13.11.2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ModelGroupHelperTest {
    private static final long ENUM_PARAM_ID = 1L;
    private static final long NUMERIC_PARAM_ID = 2L;
    private static final long BOOLEAN_PARAM_ID = 3L;

    private static final String ENUM_PARAM_NAME = "enum_param";
    private static final String NUMERIC_PARAM_NAME = "numeric_param";
    private static final String BOOLEAN_PARAM_NAME = "boolean_param";

    private static final String OPTION_NAME1 = "option1";
    private static final String OPTION_NAME2 = "option2";

    private static final Model MODEL = Model.newBuilder()
        .addParameterValues(
            ParameterValue.newBuilder()
                .setParamId(ENUM_PARAM_ID)
                .setValueType(MboParameters.ValueType.ENUM)
                .setOptionId(11)
        )
        .addParameterValues(
            ParameterValue.newBuilder()
                .setParamId(NUMERIC_PARAM_ID)
                .setValueType(MboParameters.ValueType.NUMERIC)
                .setNumericValue("2.0")
        )
        .addParameterValues(
            ParameterValue.newBuilder()
                .setParamId(BOOLEAN_PARAM_ID)
                .setValueType(MboParameters.ValueType.BOOLEAN)
                .setOptionId(3)
                .setBoolValue(true)
        )
        .build();

    @Test
    public void testGroupIdSignificant() {
        var enumParam = new ConfigParameter()
            .setParamGroupingType(ConfigParameterType.SIGNIFICANT_PARAMETER)
            .setParamId(ENUM_PARAM_ID);
        var numericParam = new ConfigParameter()
            .setParamGroupingType(ConfigParameterType.SIGNIFICANT_PARAMETER)
            .setParamId(NUMERIC_PARAM_ID);
        var booleanParam = new ConfigParameter()
            .setParamGroupingType(ConfigParameterType.SIGNIFICANT_PARAMETER)
            .setParamId(BOOLEAN_PARAM_ID);


        var config = new YtGroupingConfig()
            .setConfigParameters(List.of(enumParam, numericParam, booleanParam));

        String groupId = ModelGroupHelper.createGroupId(config, MODEL, GroupType.GOODS_GROUP);

        Assertions.assertThat(groupId).isEqualTo("1=11__2=2.0__3=true__");
    }

    @Test
    public void testGroupIdGoodsGroup() {
        var enumParam = new ConfigParameter()
            .setParamGroupingType(ConfigParameterType.SIGNIFICANT_PARAMETER)
            .setParamId(ENUM_PARAM_ID);

        var numericParam = new ConfigParameter()
            .setParamGroupingType(ConfigParameterType.DETERMINANT)
            .setParamId(BOOLEAN_PARAM_ID);

        var config = new YtGroupingConfig()
            .setConfigParameters(List.of(enumParam, numericParam));

        String groupId = ModelGroupHelper.createGroupId(config, MODEL, GroupType.GOODS_GROUP);

        Assertions.assertThat(groupId).isEqualTo("1=11__3=true__");
    }

    @Test
    public void testGroupIdAssortmentLine() {
        var enumParam = new ConfigParameter()
            .setParamGroupingType(ConfigParameterType.SIGNIFICANT_PARAMETER)
            .setParamId(ENUM_PARAM_ID);

        var numericParam = new ConfigParameter()
            .setParamGroupingType(ConfigParameterType.NORM_COEFFICIENT)
            .setParamId(NUMERIC_PARAM_ID);

        var config = new YtGroupingConfig()
            .setConfigParameters(List.of(enumParam, numericParam));

        String groupId = ModelGroupHelper.createGroupId(config, MODEL, GroupType.ASSORTMENT_LINE);

        Assertions.assertThat(groupId).isEqualTo("1=11__2=2.0__");
    }

    @Test
    public void testGroupIdNoSignificant() {
        var numericParam = new ConfigParameter()
            .setParamGroupingType(ConfigParameterType.NORM_COEFFICIENT)
            .setParamId(NUMERIC_PARAM_ID);

        var config = new YtGroupingConfig()
            .setConfigParameters(List.of(numericParam));

        String groupId = ModelGroupHelper.createGroupId(config, MODEL, GroupType.ASSORTMENT_LINE);

        Assertions.assertThat(groupId).isEqualTo("2=2.0__");
    }

    @Test
    public void testGroupName() {
        var enumValue1 = new MskuParameterWithOptionName()
            .setParamName(ENUM_PARAM_NAME)
            .setParamId(ENUM_PARAM_ID)
            .setValueType(MboParameters.ValueType.ENUM)
            .setOptionName(OPTION_NAME1);
        var enumValue2 = new MskuParameterWithOptionName()
            .setParamName(ENUM_PARAM_NAME)
            .setParamId(ENUM_PARAM_ID)
            .setValueType(MboParameters.ValueType.ENUM)
            .setOptionName(OPTION_NAME2);
        var numericValue = new MskuParameterWithOptionName()
            .setParamName(NUMERIC_PARAM_NAME)
            .setParamId(NUMERIC_PARAM_ID)
            .setValueType(MboParameters.ValueType.NUMERIC)
            .setNumericValue("1.1");
        var booleanValue = new MskuParameterWithOptionName()
            .setParamName(BOOLEAN_PARAM_NAME)
            .setParamId(BOOLEAN_PARAM_ID)
            .setValueType(MboParameters.ValueType.BOOLEAN)
            .setBooleanValue(true);

        var values = new ModelGroupParameterValues()
            .setModelGroupParameterValues(List.of(
                new ModelGroupParameterValue()
                    .setConfigParameterType(ConfigParameterType.SIGNIFICANT_PARAMETER)
                    .setParameterValues(List.of(enumValue1, enumValue2)),
                new ModelGroupParameterValue()
                    .setConfigParameterType(ConfigParameterType.DETERMINANT)
                    .setParameterValues(List.of(booleanValue)),
                new ModelGroupParameterValue()
                    .setConfigParameterType(ConfigParameterType.NORM_COEFFICIENT)
                    .setParameterValues(List.of(numericValue))
            ));

        String groupName = ModelGroupHelper.createGroupName(values);
        Assertions.assertThat(groupName).isEqualTo("option1 option2 boolean_param - да numeric_param - 1.1");
    }
}
