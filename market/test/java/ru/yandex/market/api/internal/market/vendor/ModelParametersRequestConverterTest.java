package ru.yandex.market.api.internal.market.vendor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.api.controller.v2.vendor.request.BooleanModelParameter;
import ru.yandex.market.api.controller.v2.vendor.request.EnumModelParameter;
import ru.yandex.market.api.controller.v2.vendor.request.ModelParametersRequest;
import ru.yandex.market.api.controller.v2.vendor.request.NumericModelParameter;
import ru.yandex.market.api.controller.v2.vendor.request.TextModelParameter;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.market.vendor.converter.BooleanModelParameterConverter;
import ru.yandex.market.api.internal.market.vendor.converter.EnumModelParameterConverter;
import ru.yandex.market.api.internal.market.vendor.converter.ModelParametersRequestConverter;
import ru.yandex.market.api.internal.market.vendor.converter.NumericModelParameterConverter;
import ru.yandex.market.api.internal.market.vendor.converter.TextModelParameterConverter;
import ru.yandex.market.api.internal.market.vendor.domain.PostModelParametersRequest;
import ru.yandex.market.api.internal.market.vendor.domain.params.AbstractVendorApiModelParameter;
import ru.yandex.market.api.internal.market.vendor.domain.params.BooleanVendorApiModelParameter;
import ru.yandex.market.api.internal.market.vendor.domain.params.EnumVendorApiModelParameter;
import ru.yandex.market.api.internal.market.vendor.domain.params.EnumVendorApiModelParameterOption;
import ru.yandex.market.api.internal.market.vendor.domain.params.NumericVendorApiModelParameter;
import ru.yandex.market.api.internal.market.vendor.domain.params.TextVendorApiModelParameter;
import ru.yandex.market.api.internal.market.vendor.error.VendorApiException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertThat;

public class ModelParametersRequestConverterTest extends UnitTestBase {

    private static final int BRAND_ID = 123;
    private static final int VENDOR_ID = 456;

    private static final List<AbstractVendorApiModelParameter> KNOWN_PARAMETERS = Lists.newArrayList(
        createEnumParameterInternal(1, null, Lists.newArrayList(
            new EnumVendorApiModelParameterOption(){{
                setId(100);
                setName("Hundred");
            }},
            new EnumVendorApiModelParameterOption(){{
                setId(200);
                setName("Two hundred");
            }}
        )),
        createBooleanParameterInternal(2, null),
        createNumericParameterInternal(3, null, null, null),
        createTextParameterInternal(4, null)
    );

    private ModelParametersRequestConverter converter;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        converter = new ModelParametersRequestConverter(null, null, Sets.newHashSet(
            new BooleanModelParameterConverter(),
            new EnumModelParameterConverter(),
            new NumericModelParameterConverter(),
            new TextModelParameterConverter()
        ));
    }

    @Test
    public void shouldProcessCorrectPostRequest() throws Exception {
        ModelParametersRequest request = new ModelParametersRequest();
        request.setComment("Abc");
        request.setParameters(Lists.newArrayList(
                createEnumParameter(1L, 42L, null),
                createBooleanParameter(2L, true),
                createNumericParameter(3L, 123.456, BigDecimal.ONE, BigDecimal.TEN),
                createTextParameter(4L, "Text sample")
        ));


        PostModelParametersRequest result = converter.convert(request, KNOWN_PARAMETERS, BRAND_ID);

        assertThat(result.getModel().getParameters(), hasItems(
                createEnumParameterInternal(1L, 42L, null),
                createBooleanParameterInternal(2, true),
                createNumericParameterInternal(3, 123.456, BigDecimal.ONE, BigDecimal.TEN),
                createTextParameterInternal(4, "Text sample")
        ));
        assertThat(result.getComment(), is("Abc"));
    }

    @Test
    public void shouldThrowExceptionIfUnknownParameter() throws Exception {
        exception.expect(VendorApiException.class);
        exception.expectMessage("Parameter '1' not found for category '123'");

        ModelParametersRequest request = new ModelParametersRequest();
        request.setComment("Abc");
        request.setCategoryId(123L);
        request.setParameters(Lists.newArrayList(
            createEnumParameter(1L, 42L, null)
        ));

        converter.convert(request, Collections.emptyList(), BRAND_ID);
    }

    private static EnumVendorApiModelParameter createEnumParameterInternal(long id, Long value, List<EnumVendorApiModelParameterOption> options) {
        EnumVendorApiModelParameter result = new EnumVendorApiModelParameter();
        result.setParamId(id);
        result.setValue(value);
        result.setOptions(options);
        result.setMultivalue(false);
        return result;
    }

    private static EnumModelParameter createEnumParameter(
        Long id,
        Long value,
        List<ru.yandex.market.api.controller.v2.vendor.request.EnumModelParameterOption> options
    ) {
        ru.yandex.market.api.controller.v2.vendor.request.EnumModelParameter result = new ru.yandex.market.api.controller.v2.vendor.request.EnumModelParameter();
        result.setParameterId(id);
        result.setValues(Collections.singletonList(value));
        result.setOptions(options);
        return result;
    }

    private static BooleanVendorApiModelParameter createBooleanParameterInternal(long id, Boolean value) {
        BooleanVendorApiModelParameter result = new BooleanVendorApiModelParameter();
        result.setParamId(id);
        result.setValue(value);
        result.setMultivalue(false);
        return result;
    }

    private static BooleanModelParameter createBooleanParameter(Long id, Boolean value) {
        ru.yandex.market.api.controller.v2.vendor.request.BooleanModelParameter result = new ru.yandex.market.api.controller.v2.vendor.request.BooleanModelParameter();
        result.setParameterId(id);
        result.setValues(Collections.singletonList(value));
        return result;
    }

    private static NumericVendorApiModelParameter createNumericParameterInternal(long id, Double value,
                                                                                 BigDecimal minValue, BigDecimal maxValue) {
        NumericVendorApiModelParameter result = new NumericVendorApiModelParameter();
        result.setParamId(id);
        result.setValue(value);
        result.setMinValue(minValue);
        result.setMaxValue(maxValue);
        result.setMultivalue(false);
        return result;
    }

    private static NumericModelParameter createNumericParameter(Long id, Double value, BigDecimal minValue,
                                                                BigDecimal maxValue) {
        ru.yandex.market.api.controller.v2.vendor.request.NumericModelParameter result =
                new ru.yandex.market.api.controller.v2.vendor.request.NumericModelParameter();
        result.setParameterId(id);
        result.setValues(Collections.singletonList(value));
        result.setMinValue(minValue);
        result.setMaxValue(maxValue);
        return result;
    }

    private static TextVendorApiModelParameter createTextParameterInternal(long id, String value) {
        TextVendorApiModelParameter result = new TextVendorApiModelParameter();
        result.setParamId(id);
        result.setValue(value);
        result.setMultivalue(false);
        return result;
    }

    private static TextModelParameter createTextParameter(long id, String value) {
        ru.yandex.market.api.controller.v2.vendor.request.TextModelParameter result = new ru.yandex.market.api.controller.v2.vendor.request.TextModelParameter();
        result.setParameterId(id);
        result.setValues(Collections.singletonList(value));
        return result;
    }
}
