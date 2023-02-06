package ru.yandex.market.ir.autogeneration.common.rating;

import java.util.Map;
import java.util.Set;

import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.robot.db.ParameterValueComposer;

public class SkuRatingTestUtils {
    public static final long PARAM_ID_IMPORTANT_1 = 4749254L;
    public static final long PARAM_ID_IMPORTANT_2 = 4749253L;
    public static final long PARAM_ID_IMPORTANT_3 = 4749255L;
    public static final long PARAM_ID_IMPORTANT_4 = 4749256L;
    public static final long PARAM_ID_1 = 4749252L;
    public static final long PARAM_ID_2 = 4749251L;
    public static final long PARAM_ID_3 = 4749250L;

    public static final Map<Long, MboParameters.Parameter> ALL_PARAMS = Map.of(
            PARAM_ID_IMPORTANT_1, createImportantParam(PARAM_ID_IMPORTANT_1, false, 0),
            PARAM_ID_IMPORTANT_2, createImportantParam(PARAM_ID_IMPORTANT_2, true, null),
            PARAM_ID_IMPORTANT_3, createImportantParam(PARAM_ID_IMPORTANT_3, true, null),
            PARAM_ID_IMPORTANT_4, createImportantParam(PARAM_ID_IMPORTANT_4, true, null),
            PARAM_ID_1, createCommonParam(PARAM_ID_1),
            PARAM_ID_2, createCommonParam(PARAM_ID_2),
            PARAM_ID_3, createCommonParam(PARAM_ID_3)
    );

    private static MboParameters.Parameter createImportantParam(long paramId, boolean mandatoryForSignature,
                                                                Integer commonFilterIndex) {
        var builder = MboParameters.Parameter.newBuilder()
                .setId(paramId)
                .setValueType(MboParameters.ValueType.ENUM)
                .setMandatoryForSignature(mandatoryForSignature);

        if (commonFilterIndex != null) {
            builder.setCommonFilterIndex(commonFilterIndex);
        }

        return builder.build();
    }

    private static MboParameters.Parameter createCommonParam(long paramId) {
        return MboParameters.Parameter.newBuilder()
                .setId(paramId)
                .setValueType(MboParameters.ValueType.STRING)
                .build();
    }

    public static SkuRatingFormula buildFormula(long categoryId, Set<Long> params) {
        var builder = MboParameters.Category.newBuilder()
                .setHid(categoryId)
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(ParameterValueComposer.NAME_ID)
                        .setXslName(CategoryData.NAME)
                        .setValueType(MboParameters.ValueType.STRING)
                        .build())
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(ParameterValueComposer.VENDOR_ID)
                        .setXslName(CategoryData.VENDOR)
                        .setValueType(MboParameters.ValueType.ENUM)
                        .build())
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(SkuRatingFormula.DESCRIPTION_ID)
                        .setXslName("description")
                        .setValueType(MboParameters.ValueType.STRING)
                        .build());

        params.forEach(id -> builder.addParameter(ALL_PARAMS.get(id)));
        MboParameters.Category category = builder.build();

        return CategoryData.build(category).getSkuRatingFormula();
    }

}
