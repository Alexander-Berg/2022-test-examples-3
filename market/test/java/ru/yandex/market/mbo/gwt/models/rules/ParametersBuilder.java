package ru.yandex.market.mbo.gwt.models.rules;

import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.gwt.models.ImageType;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author gilmulla
 *
 */
public class ParametersBuilder<T> {

    public static final long STRING_PARAM_ID = 2L;
    public static final long NUMERIC_PARAM_ID = 3L;
    public static final long ENUM_PARAM_ID = 4L;
    public static final long BOOL_PARAM_ID = 5L;
    public static final long NUMERIC_ENUM_PARAM_ID = 6L;
    public static final long MATCHING_DISABLED_PARAM_ID = 7L;

    public static final long LOCAL_VENDOR_1_ID = 1001L;
    public static final long GLOBAL_VENDOR_1_ID = 1002L;
    public static final long LOCAL_VENDOR_2_ID = 2001L;
    public static final long GLOBAL_VENDOR_2_ID = 2002L;

    public static final long ENUM3_OPTION = 3L;
    public static final long ENUM4_OPTION = 4L;
    public static final long ENUM5_OPTION = 5L;
    public static final long BOOL_TRUE_OPTION = 6L;
    public static final long BOOL_FALSE_OPTION = 7L;
    public static final long NUMERIC_ENUM8_OPTION = 8L;
    public static final long NUMERIC_ENUM9_OPTION = 9L;
    public static final long NUMERIC_ENUM10_OPTION = 10L;
    public static final long IS_PARTNER_TRUE_OPTION = 11L;
    public static final long IS_PARTNER_FALSE_OPTION = 12L;
    public static final long MATCHING_DISABLED_TRUE_OPTION = 13L;
    public static final long MATCHING_DISABLED_FALSE_OPTION = 14L;

    private List<CategoryParam> parameters = new ArrayList<>();
    private long id = 1L;
    private long optionId = 1L;
    private Function<List<CategoryParam>, T> endParamsListener;

    ParametersBuilder(Function<List<CategoryParam>, T> endParamsListener) {
        this.endParamsListener = endParamsListener;
    }

    public static <T> ParametersBuilder<T> startParameters(
            Function<List<CategoryParam>, T> endParamsListener) {
        return new ParametersBuilder<>(endParamsListener);
    }

    public static ParametersBuilder<List<CategoryParam>> startParameters() {
        return new ParametersBuilder<>(Function.identity());
    }

    public static <T> ParametersBuilder<T> builder(
            Function<List<CategoryParam>, T> endParamsListener) {
        return new ParametersBuilder<>(endParamsListener);
    }

    public ParameterBuilder<ParametersBuilder<T>> startParameter() {
        ParameterBuilder<ParametersBuilder<T>> builder = ParameterBuilder.builder(
            param -> {
                this.parameters.add(param);
                return this;
            }
        );
        return builder.id(id++).level(CategoryParam.Level.MODEL).useForGuru(true);
    }

    public ParametersBuilder<T> parameter(String xslName, Param.Type type) {
        return parameter(xslName, type, p -> { });
    }

    public ParametersBuilder<T> parameter(String xslName, Param.Type type,
                                          Consumer<CategoryParamBuilder> initialiser) {
        CategoryParamBuilder parameter = CategoryParamBuilder.newBuilder(id++, xslName)
            .setName(xslName)
            .setType(type)
            .setLevel(CategoryParam.Level.MODEL)
            .setUseForGuru(true);

        initialiser.accept(parameter);

        this.parameters.add(parameter.build());
        return this;
    }

    public ParametersBuilder<T> imageParameters(String xslName) {
        ImageType imageType = ImageType.getImageType(xslName);
        parameter(xslName, Param.Type.STRING);
        parameter(imageType.getUrlParamName(xslName), Param.Type.STRING);
        parameter(imageType.getRawUrlParamName(xslName), Param.Type.STRING);
        parameter(imageType.getWidthParamName(xslName), Param.Type.NUMERIC);
        parameter(imageType.getHeightParamName(xslName), Param.Type.NUMERIC);
        parameter(imageType.getColornessParamName(xslName), Param.Type.NUMERIC);
        parameter(imageType.getColornessAvgParamName(xslName), Param.Type.NUMERIC);
        return this;
    }

    @SuppressWarnings("checkstyle:magicnumber")
    public static ParametersBuilder<CommonModelBuilder<CommonModel>> defaultBuilder() {
        return ParametersBuilder
            .startParameters(p -> CommonModelBuilder.builder(Function.identity()).parameters(p))
                .startParameter()
                    .id(KnownIds.VENDOR_PARAM_ID)
                    .type(Param.Type.ENUM)
                    .xsl("vendor")
                    .localVendor(GLOBAL_VENDOR_1_ID, LOCAL_VENDOR_1_ID, "vendor1")
                    .localVendor(GLOBAL_VENDOR_2_ID, LOCAL_VENDOR_2_ID, "vendor2")
                    .name("Vendor")
                .endParameter()
                .startParameter()
                    .id(STRING_PARAM_ID)
                    .type(Param.Type.STRING)
                    .xsl("name")
                    .name("Name")
                .endParameter()
                .startParameter()
                    .id(NUMERIC_PARAM_ID)
                    .type(Param.Type.NUMERIC)
                    .xsl("numeric")
                    .name("Numeric")
                .endParameter()
                .startParameter()
                    .id(ENUM_PARAM_ID)
                    .type(Param.Type.ENUM)
                    .xsl("enum")
                    .option(ENUM3_OPTION, "enum3")
                    .option(ENUM4_OPTION, "enum4")
                    .option(ENUM5_OPTION, "enum5")
                    .name("Enum")
                .endParameter()
                .startParameter()
                    .id(BOOL_PARAM_ID)
                    .type(Param.Type.BOOLEAN)
                    .xsl("IsSku")
                    .option(BOOL_TRUE_OPTION, "TRUE")
                    .option(BOOL_FALSE_OPTION, "FALSE")
                    .name("IsSku")
                .endParameter()
                .startParameter()
                    .id(NUMERIC_ENUM_PARAM_ID)
                    .type(Param.Type.NUMERIC_ENUM)
                    .xsl("numericEnum")
                    .option(NUMERIC_ENUM8_OPTION, "100")
                    .option(NUMERIC_ENUM9_OPTION, "200")
                    .option(NUMERIC_ENUM10_OPTION, "300")
                    .name("NumericEnum")
                .endParameter()
                .startParameter()
                    .id(KnownIds.IS_PARTNER_PARAM_ID)
                    .type(Param.Type.BOOLEAN)
                    .xsl(XslNames.IS_PARTNER)
                    .option(IS_PARTNER_TRUE_OPTION, "TRUE")
                    .option(IS_PARTNER_FALSE_OPTION, "FALSE")
                    .name("Партнерская")
                .endParameter()
                .startParameter()
                    .id(MATCHING_DISABLED_PARAM_ID)
                    .type(Param.Type.BOOLEAN)
                    .xsl(XslNames.MATCHING_DISABLED)
                    .option(MATCHING_DISABLED_TRUE_OPTION, "TRUE")
                    .option(MATCHING_DISABLED_FALSE_OPTION, "FALSE")
                    .name("Не использовать в Матчере")
                .endParameter();
    }

    public List<CategoryParam> getParameters() {
        return this.parameters;
    }

    public T endParameters() {
        return this.endParamsListener.apply(this.parameters);
    }
}
