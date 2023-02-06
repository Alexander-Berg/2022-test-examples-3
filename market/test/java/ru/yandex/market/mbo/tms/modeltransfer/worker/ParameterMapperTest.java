package ru.yandex.market.mbo.tms.modeltransfer.worker;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.db.transfer.ParameterMapper;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;

/**
 * @author ayratgdl
 * @date 05.03.18
 */
public class ParameterMapperTest {
    private static final Long CATEGORY_ID_1 = 101L;
    private static final Long CATEGORY_ID_2 = 102L;
    private static final Long PARAMETER_ID_1 = 201L;
    private static final Long PARAMETER_ID_2 = 202L;
    private static final Long OPTION_ID_1 = 301L;
    private static final Long OPTION_ID_2 = 302L;
    private static final Long GLOBAL_VENDOR_ID_1 = 401L;

    @Test
    public void getNewParamIdEmptyResult() {
        ParameterMapper mapper = new ParameterMapper(new CategoryEntities(CATEGORY_ID_1, Collections.emptyList()),
            new CategoryEntities(CATEGORY_ID_2, Collections.emptyList()));
        Assert.assertEquals(Optional.empty(), mapper.mapParamId(PARAMETER_ID_1));
    }

    @Test
    public void getNewOptionIdEmptyResult() {
        ParameterMapper mapper = new ParameterMapper(new CategoryEntities(CATEGORY_ID_1, Collections.emptyList()),
            new CategoryEntities(CATEGORY_ID_2, Collections.emptyList()));
        Assert.assertEquals(Optional.empty(), mapper.mapValueId(OPTION_ID_1));
    }

    @Test
    public void establishMapBetweenParametersByName() {
        CategoryEntities entities1 = new CategoryEntities(CATEGORY_ID_1, Collections.emptyList());
        entities1.addParameter(
            buildParameter(Param.Type.ENUM, PARAMETER_ID_1, "xsl_name_param",
                new OptionImpl(OPTION_ID_1, "option_name"))
        );

        CategoryEntities entities2 = new CategoryEntities(CATEGORY_ID_2, Collections.emptyList());
        entities2.addParameter(
            buildParameter(Param.Type.ENUM, PARAMETER_ID_2, "xsl_name_param",
                new OptionImpl(OPTION_ID_2, "option_name"))
        );

        ParameterMapper mapper = new ParameterMapper(entities1, entities2);
        Assert.assertEquals(Optional.of(PARAMETER_ID_2), mapper.mapParamId(PARAMETER_ID_1));
        Assert.assertEquals(Optional.of(OPTION_ID_2), mapper.mapValueId(OPTION_ID_1));
    }

    @Test
    public void establishMapBetweenParametersById() {
        CategoryEntities entities1 = new CategoryEntities(CATEGORY_ID_1, Collections.emptyList());
        entities1.addParameter(
            buildParameter(Param.Type.ENUM, PARAMETER_ID_1, "xsl_name_param",
                new OptionImpl(OPTION_ID_1, "option_name"))
        );

        CategoryEntities entities2 = new CategoryEntities(CATEGORY_ID_2, Collections.emptyList());
        entities2.addParameter(
            buildParameter(Param.Type.ENUM, PARAMETER_ID_1, "xsl_name_param",
                new OptionImpl(OPTION_ID_1, "changed_option_name"))
        );

        ParameterMapper mapper = new ParameterMapper(entities1, entities2);
        Assert.assertEquals(Optional.of(PARAMETER_ID_1), mapper.mapParamId(PARAMETER_ID_1));
        Assert.assertEquals(Optional.of(OPTION_ID_1), mapper.mapValueId(OPTION_ID_1));
    }

    @Test
    public void establishMapBetweenParametersByGlobalVendorId() {
        Option globalVendorOption1 = new OptionImpl(GLOBAL_VENDOR_ID_1, "global_vendor_name");
        Option option1 = new OptionImpl(globalVendorOption1, Option.OptionType.VENDOR);
        option1.setId(OPTION_ID_1);
        CategoryEntities entities1 = new CategoryEntities(CATEGORY_ID_1, Collections.emptyList());
        entities1.addParameter(buildParameter(Param.Type.ENUM, PARAMETER_ID_1, "xsl_name_param", option1));

        Option globalVendorOption2 = new OptionImpl(GLOBAL_VENDOR_ID_1, "global_vendor_name");
        Option option2 = new OptionImpl(globalVendorOption2, Option.OptionType.VENDOR);
        option2.setId(OPTION_ID_2);
        CategoryEntities entities2 = new CategoryEntities(CATEGORY_ID_2, Collections.emptyList());
        entities2.addParameter(buildParameter(Param.Type.ENUM, PARAMETER_ID_1, "xsl_name_param", option2));

        ParameterMapper mapper = new ParameterMapper(entities1, entities2);
        Assert.assertEquals(Optional.of(PARAMETER_ID_1), mapper.mapParamId(PARAMETER_ID_1));
        Assert.assertEquals(Optional.of(GLOBAL_VENDOR_ID_1), mapper.mapValueId(GLOBAL_VENDOR_ID_1));
    }

    @Test
    public void mapEnumParameterValue() {
        Parameter parameter1 = buildParameter(Param.Type.ENUM, PARAMETER_ID_1, "xsl_name_param",
            new OptionImpl(OPTION_ID_1, "option_name"));
        CategoryEntities entities1 = new CategoryEntities(CATEGORY_ID_1, Collections.emptyList());
        entities1.addParameter(parameter1);

        Parameter parameter2 = buildParameter(Param.Type.ENUM, PARAMETER_ID_1, "xsl_name_param",
            new OptionImpl(OPTION_ID_2, "option_name"));
        CategoryEntities entities2 = new CategoryEntities(CATEGORY_ID_2, Collections.emptyList());
        entities2.addParameter(parameter2);

        ParameterMapper mapper = new ParameterMapper(entities1, entities2);

        ParameterValue sourceParameterValue = buildEnumParameterValue(parameter1, OPTION_ID_1);
        ParameterValue mappedParameterValue = mapper.map(sourceParameterValue).get();
        ParameterValue expectedMappedParameterValue = buildEnumParameterValue(parameter2, OPTION_ID_2);
        Assert.assertEquals(expectedMappedParameterValue, mappedParameterValue);
    }

    @Test
    public void mapNumberParameterValue() {
        Parameter parameter1 = buildParameter(Param.Type.NUMERIC, PARAMETER_ID_1, "number_param");
        CategoryEntities entities1 = new CategoryEntities(CATEGORY_ID_1, Collections.emptyList());
        entities1.addParameter(parameter1);

        Parameter parameter2 = buildParameter(Param.Type.NUMERIC, PARAMETER_ID_2, "number_param");
        CategoryEntities entities2 = new CategoryEntities(CATEGORY_ID_2, Collections.emptyList());
        entities2.addParameter(parameter2);

        ParameterMapper mapper = new ParameterMapper(entities1, entities2);

        ParameterValue sourceParameterValue = buildNumericParameterValue(parameter1, 1);
        ParameterValue mappedParameterValue = mapper.map(sourceParameterValue).get();
        ParameterValue expectedMappedParameterValue = buildNumericParameterValue(parameter2, 1);

        Assert.assertEquals(expectedMappedParameterValue, mappedParameterValue);
    }

    private Parameter buildParameter(Param.Type type, Long id, String xslName, Option... options) {
        Parameter parameter = new Parameter();
        parameter.setType(type);
        parameter.setId(id);
        parameter.setXslName(xslName);
        parameter.setOptions(Arrays.asList(options));
        return parameter;
    }

    private ParameterValue buildEnumParameterValue(CategoryParam param, Long optionId) {
        ParameterValue parameterValue = new ParameterValue(param);
        parameterValue.setOptionId(optionId);
        return parameterValue;
    }

    private ParameterValue buildNumericParameterValue(CategoryParam param, long value) {
        ParameterValue parameterValue = new ParameterValue(param);
        parameterValue.setNumericValue(BigDecimal.valueOf(value));
        return parameterValue;
    }
}
