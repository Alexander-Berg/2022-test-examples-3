package ru.yandex.market.ir.autogeneration.common.helpers;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.export.MboSizeMeasures;
import ru.yandex.market.robot.db.ParameterValueComposer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CategoryDataParamMigrationTest {
    private static final String BOOL_PARAM_XSL_NAME = "BOOL_PARAM";
    private static final String ENUM_PARAM_XSL_NAME = "ENUM_PARAM";
    private static final String NUMERIC_ENUM_PARAM_XSL_NAME = "NUMERIC_ENUM_PARAM";
    private static final String NUMERIC_PARAM_XSL_NAME = "NUMERIC_PARAM";
    private static final String STRING_PARAM_XSL_NAME = "STRING_PARAM";

    private static final long BOOL_PARAM_ID = 1;
    public static final long ENUM_PARAM_ID = 2;
    private static final long NUMERIC_ENUM_PARAM_ID = 3;
    private static final long NUMERIC_PARAM_ID = 4;
    public static final long STRING_PARAM_ID = 5;
    public static final long SKU_DEFINING_PARAM_ID = 6;
    private static final long SKU_HYPOTHESIS_PARAM_ID = 7;
    private static final long SKU_INFO_PARAM_ID = 8;

    public static final long OLD_ENUM_PARAM_ID = 92;
    private static final long NEW_ENUM_PARAM_ID = 82;
    private static final long OLD_NUMERIC_PARAM_ID = 94;
    public static final long OLD_STRING_PARAM_ID = 95;

    public static final int VENDOR_OPTION = 123;
    private static final int BOOL_OPTION_TRUE = 11;
    private static final int BOOL_OPTION_FALSE = 12;
    public static final int ENUM_OPTION_1 = 21;
    public static final int ENUM_OPTION_2 = 22;
    private static final int NUMERIC_ENUM_OPTION_1 = 31;
    private static final int NUMERIC_ENUM_OPTION_2 = 32;

    public static final int OLD_ENUM_OPTION_1 = 921;
    public static final int OLD_ENUM_OPTION_2 = 922;
    private static final int NEW_ENUM_OPTION_1 = 821;
    private static final int NEW_ENUM_OPTION_2 = 822;

    private final List<MboSizeMeasures.GetSizeMeasuresInfoResponse.CategoryResponse> sizeMeasuresList =
            new ArrayList<>();
    private final List<MboParameters.ParameterValueLinks> parameterValueLinks = new ArrayList<>();


    private MboParameters.Category.Builder getCategoryDataBuilder() {
        return MboParameters.Category.newBuilder()
                .setLeaf(true)
                .addParameter(
                        MboParameters.Parameter.newBuilder()
                                .setId(ParameterValueComposer.VENDOR_ID)
                                .setXslName(ParameterValueComposer.VENDOR)
                                .setValueType(MboParameters.ValueType.ENUM)
                                .setParamType(MboParameters.ParameterLevel.MODEL_LEVEL)
                                .setIsUseForGuru(true)
                                .setSkuMode(MboParameters.SKUParameterMode.SKU_NONE)
                                .addOption(
                                        MboParameters.Option.newBuilder()
                                                .setId(VENDOR_OPTION)
                                                .addName(MboParameters.Word.newBuilder().setName("vendor").build())
                                                .build()
                                )
                                .build()
                )
                .addParameter(
                        MboParameters.Parameter.newBuilder()
                                .setId(BOOL_PARAM_ID)
                                .setXslName(BOOL_PARAM_XSL_NAME)
                                .setValueType(MboParameters.ValueType.BOOLEAN)
                                .setParamType(MboParameters.ParameterLevel.MODEL_LEVEL)
                                .setIsUseForGuru(true)
                                .setSkuMode(MboParameters.SKUParameterMode.SKU_NONE)
                                .addOption(
                                        MboParameters.Option.newBuilder()
                                                .setId(BOOL_OPTION_TRUE)
                                                .addName(MboParameters.Word.newBuilder().setName("true").build())
                                                .build()
                                )
                                .addOption(
                                        MboParameters.Option.newBuilder()
                                                .setId(BOOL_OPTION_FALSE)
                                                .addName(MboParameters.Word.newBuilder().setName("false").build())
                                                .build()
                                )
                                .setMandatoryForSignature(true)
                                .setImportant(true)
                                .setMultivalue(false)
                                .build()
                )
                .addParameter(
                        MboParameters.Parameter.newBuilder()
                                .setId(ENUM_PARAM_ID)
                                .setXslName(ENUM_PARAM_XSL_NAME)
                                .setValueType(MboParameters.ValueType.ENUM)
                                .setParamType(MboParameters.ParameterLevel.MODEL_LEVEL)
                                .setIsUseForGuru(true)
                                .setSkuMode(MboParameters.SKUParameterMode.SKU_NONE)
                                .addOption(
                                        MboParameters.Option.newBuilder()
                                                .setId(ENUM_OPTION_1)
                                                .addName(MboParameters.Word.newBuilder().setName("ENUM_OPTION_1").build())
                                                .build()
                                )
                                .addOption(
                                        MboParameters.Option.newBuilder()
                                                .setId(ENUM_OPTION_2)
                                                .addName(MboParameters.Word.newBuilder().setName("ENUM_OPTION_2").build())
                                                .build()
                                )
                                .setMultivalue(true)
                                .build()
                )
                .addParameter(
                        MboParameters.Parameter.newBuilder()
                                .setId(NUMERIC_ENUM_PARAM_ID)
                                .setXslName(NUMERIC_ENUM_PARAM_XSL_NAME)
                                .setValueType(MboParameters.ValueType.NUMERIC_ENUM)
                                .setParamType(MboParameters.ParameterLevel.MODEL_LEVEL)
                                .setIsUseForGuru(true)
                                .setSkuMode(MboParameters.SKUParameterMode.SKU_NONE)
                                .addOption(
                                        MboParameters.Option.newBuilder()
                                                .setId(NUMERIC_ENUM_OPTION_1)
                                                .addName(MboParameters.Word.newBuilder().setName("1").build())
                                                .build()
                                )
                                .addOption(
                                        MboParameters.Option.newBuilder()
                                                .setId(NUMERIC_ENUM_OPTION_2)
                                                .addName(MboParameters.Word.newBuilder().setName("2").build())
                                                .build()
                                )
                                .addParameterLink(
                                        MboParameters.ParameterLink.newBuilder()
                                                .setParameterId(ENUM_PARAM_ID)
                                                .setReversed(false)
                                                .setType(MboParameters.ParameterLinkType.CONDITION)
                                                .build()
                                )
                                .build()
                )
                .addParameter(
                        MboParameters.Parameter.newBuilder()
                                .setId(NUMERIC_PARAM_ID)
                                .setXslName(NUMERIC_PARAM_XSL_NAME)
                                .setValueType(MboParameters.ValueType.NUMERIC)
                                .setParamType(MboParameters.ParameterLevel.MODEL_LEVEL)
                                .setIsUseForGuru(true)
                                .setSkuMode(MboParameters.SKUParameterMode.SKU_NONE)
                                .build()
                )
                .addParameter(
                        MboParameters.Parameter.newBuilder()
                                .setId(STRING_PARAM_ID)
                                .setXslName(STRING_PARAM_XSL_NAME)
                                .setValueType(MboParameters.ValueType.STRING)
                                .setParamType(MboParameters.ParameterLevel.MODEL_LEVEL)
                                .setIsUseForGuru(true)
                                .setSkuMode(MboParameters.SKUParameterMode.SKU_NONE)
                                .build()
                )
                .addParameter(
                        MboParameters.Parameter.newBuilder()
                                .setId(SKU_DEFINING_PARAM_ID)
                                .setXslName("SKU_DEFINING")
                                .setValueType(MboParameters.ValueType.STRING)
                                .setParamType(MboParameters.ParameterLevel.MODEL_LEVEL)
                                .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
                                .setExtractInSkubd(false)
                                .build()
                )
                .addParameter(
                        MboParameters.Parameter.newBuilder()
                                .setId(SKU_HYPOTHESIS_PARAM_ID)
                                .setXslName("SKU_HYPOTHESIS")
                                .setValueType(MboParameters.ValueType.ENUM)
                                .setParamType(MboParameters.ParameterLevel.MODEL_LEVEL)
                                .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
                                .setExtractInSkubd(true)
                                .setMandatory(true)
                                .build()
                )
                .addParameter(
                        MboParameters.Parameter.newBuilder()
                                .setId(SKU_INFO_PARAM_ID)
                                .setXslName("SKU_INFO")
                                .setValueType(MboParameters.ValueType.STRING)
                                .setParamType(MboParameters.ParameterLevel.MODEL_LEVEL)
                                .setSkuMode(MboParameters.SKUParameterMode.SKU_INFORMATIONAL)
                                .build()
                )
                .addParametersMapping(
                        MboParameters.ParameterMigration.newBuilder()
                                .setSourceParamId(OLD_ENUM_PARAM_ID)
                                .setTargetParamId(ENUM_PARAM_ID)
                                .addOptionsMigration(
                                        MboParameters.OptionMigration.newBuilder()
                                                .setSourceOptionId(OLD_ENUM_OPTION_1)
                                                .setTargetOptionId(ENUM_OPTION_1)
                                                .build()
                                )
                                .addOptionsMigration(
                                        MboParameters.OptionMigration.newBuilder()
                                                .setSourceOptionId(OLD_ENUM_OPTION_2)
                                                .setTargetOptionId(ENUM_OPTION_2)
                                                .build()
                                )
                                .build()
                )
                .addParametersMapping(
                        MboParameters.ParameterMigration.newBuilder()
                                .setSourceParamId(OLD_STRING_PARAM_ID)
                                .setTargetParamId(STRING_PARAM_ID)
                                .build()
                )
                .addParametersMapping(
                        MboParameters.ParameterMigration.newBuilder()
                                .setSourceParamId(OLD_NUMERIC_PARAM_ID)
                                .setTargetParamId(NUMERIC_PARAM_ID)
                                .build()
                );
    }

    @Test
    public void parameterMigrationWorksForOldParams() {
        CategoryData categoryData = CategoryData.build(getCategoryDataBuilder().build());
        MboParameters.Parameter param = categoryData.getParamById(OLD_ENUM_PARAM_ID);
        Assert.assertEquals(param.getId(), ENUM_PARAM_ID);

        long optionId = categoryData.getMigratedOptionId(OLD_ENUM_PARAM_ID, OLD_ENUM_OPTION_1);
        Assert.assertEquals(optionId, ENUM_OPTION_1);
    }

    @Test
    public void parameterMigrationWithTransitions() {
        CategoryData categoryData = CategoryData.build(
                getCategoryDataBuilder()
                        .addParameter(
                                MboParameters.Parameter.newBuilder()
                                        .setId(NEW_ENUM_PARAM_ID)
                                        .setValueType(MboParameters.ValueType.ENUM)
                                        .addOption(
                                                MboParameters.Option.newBuilder()
                                                        .setId(NEW_ENUM_OPTION_1)
                                                        .addName(MboParameters.Word.newBuilder().setName("E1").build())
                                                        .build()
                                        )
                                        .addOption(
                                                MboParameters.Option.newBuilder()
                                                        .setId(NEW_ENUM_OPTION_2)
                                                        .addName(MboParameters.Word.newBuilder().setName("E2").build())
                                                        .build()
                                        )
                                        .build()
                        )
                        .addParametersMapping(
                                MboParameters.ParameterMigration.newBuilder()
                                        .setSourceParamId(ENUM_PARAM_ID)
                                        .setTargetParamId(NEW_ENUM_PARAM_ID)
                                        .addOptionsMigration(
                                                MboParameters.OptionMigration.newBuilder()
                                                        .setSourceOptionId(ENUM_OPTION_1)
                                                        .setTargetOptionId(NEW_ENUM_OPTION_1)
                                                        .build()
                                        )
                                        .addOptionsMigration(
                                                MboParameters.OptionMigration.newBuilder()
                                                        .setSourceOptionId(ENUM_OPTION_2)
                                                        .setTargetOptionId(NEW_ENUM_OPTION_2)
                                                        .build()
                                        )
                                        .build()
                        )
                        .build()
        );

        MboParameters.Parameter param = categoryData.getParamById(OLD_ENUM_PARAM_ID);
        Assert.assertEquals(param.getId(), NEW_ENUM_PARAM_ID);

        long optionId = categoryData.getMigratedOptionId(OLD_ENUM_PARAM_ID, OLD_ENUM_OPTION_1);
        Assert.assertEquals(optionId, NEW_ENUM_OPTION_1);
    }

    @Test
    public void throwExceptionIfParameterMigrationHasCycles() {
        assertThatThrownBy(() -> CategoryData.build(
                getCategoryDataBuilder()
                        .addParametersMapping(
                                MboParameters.ParameterMigration.newBuilder()
                                        .setSourceParamId(ENUM_PARAM_ID)
                                        .setTargetParamId(OLD_ENUM_PARAM_ID)
                                        .addOptionsMigration(
                                                MboParameters.OptionMigration.newBuilder()
                                                        .setSourceOptionId(ENUM_OPTION_1)
                                                        .setTargetOptionId(OLD_ENUM_OPTION_1)
                                                        .build()
                                        )
                                        .addOptionsMigration(
                                                MboParameters.OptionMigration.newBuilder()
                                                        .setSourceOptionId(ENUM_OPTION_2)
                                                        .setTargetOptionId(OLD_ENUM_OPTION_2)
                                                        .build()
                                        )
                                        .build()
                        )
                        .build()
        ))
                .hasMessage("Seems to cycles exist in parameter migration");

    }

}
