package ru.yandex.market.api.controller.v2.market.vendor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import ru.yandex.market.api.controller.v2.vendor.VendorApiControllerV2;
import ru.yandex.market.api.controller.v2.vendor.request.AbstractModelParameter;
import ru.yandex.market.api.controller.v2.vendor.request.ModelParametersRequest;
import ru.yandex.market.api.controller.v2.vendor.request.NumericModelParameter;
import ru.yandex.market.api.controller.v2.vendor.request.TextModelParameter;
import ru.yandex.market.api.error.InvalidParameterValueException;
import ru.yandex.market.api.error.ValidationErrors;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.blackbox.data.OauthUser;
import ru.yandex.market.api.internal.market.vendor.VendorId;
import ru.yandex.market.api.internal.market.vendor.domain.Image;
import ru.yandex.market.api.server.sec.User;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;
import ru.yandex.market.api.util.httpclient.clients.VendorApiTestClient;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author dimkarp93
 */
@WithContext
public class VendorApiControllerV2Test extends BaseTest {
    @Inject
    private VendorApiControllerV2 vendorApiController;

    @Inject
    private VendorApiTestClient vendorApiTestClient;

    @Inject
    private ReportTestClient reportTestClient;

    private static final User USER = new User(
        new OauthUser(1L),
        null,
        null,
        null
    );

    @Test
    public void modelParameterSingleBoolean() {
        long modelId = 1L;
        long brandId = 2L;
        long vendorId = 3L;

        reportTestClient.getModelInfoById(modelId, "modelinfo_1.json");

        vendorApiTestClient.vendorByBrandId(brandId, "vendor_by_brand_id_2.json");
        vendorApiTestClient.vendorModelParamsByModel(vendorId, modelId, "boolean_params_model_1.json");

        List<AbstractModelParameter> parameters = getParams(modelId, brandId);
        assertThat(parameters, hasSize(1));

        List<Object> values = parameters.get(0).getValues();
        assertThat(values, hasSize(1));
        assertThat(values, contains(false));

        assertThat(parameters.get(0).isMandatory(), is(false));
    }

    @Test
    public void modelParameterSingleNumeric() {
        long modelId = 1L;
        long brandId = 2L;
        long vendorId = 3L;

        reportTestClient.getModelInfoById(modelId, "modelinfo_1.json");

        vendorApiTestClient.vendorByBrandId(brandId, "vendor_by_brand_id_2.json");
        vendorApiTestClient.vendorModelParamsByModel(vendorId, modelId, "single_numeric_params_model_1.json");


        List<AbstractModelParameter> parameters = getParams(modelId, brandId);
        assertThat(parameters, hasSize(1));

        List<Object> values = parameters.get(0).getValues();
        assertThat(values, hasSize(1));
        assertThat(values, contains(114.0));

        assertThat(parameters.get(0).isMandatory(), is(false));
    }

    @Test
    public void modelParameterSingleEnum() {
        long modelId = 1L;
        long brandId = 2L;
        long vendorId = 3L;

        reportTestClient.getModelInfoById(modelId, "modelinfo_1.json");

        vendorApiTestClient.vendorByBrandId(brandId, "vendor_by_brand_id_2.json");
        vendorApiTestClient.vendorModelParamsByModel(vendorId, modelId, "single_enum_params_model_1.json");

        List<AbstractModelParameter> parameters = getParams(modelId, brandId);
        assertThat(parameters, hasSize(1));

        List<Object> values = parameters.get(0).getValues();
        assertThat(values, hasSize(1));
        assertThat(values, contains(12104378L));

        assertThat(parameters.get(0).isMandatory(), is(false));
    }

    @Test
    public void modelParameterSingleText() {
        long modelId = 1L;
        long brandId = 2L;
        long vendorId = 3L;

        reportTestClient.getModelInfoById(modelId, "modelinfo_1.json");

        vendorApiTestClient.vendorByBrandId(brandId, "vendor_by_brand_id_2.json");
        vendorApiTestClient.vendorModelParamsByModel(vendorId, modelId, "single_text_params_model_1.json");

        List<AbstractModelParameter> parameters = getParams(modelId, brandId);
        assertThat(parameters, hasSize(1));

        List<Object> values = parameters.get(0).getValues();
        assertThat(values, hasSize(1));
        assertThat(values, contains("Energy"));

        assertThat(parameters.get(0).isMandatory(), is(false));

    }

    @Test
    public void modelParameterMultiNumeric() {
        long modelId = 1L;
        long brandId = 2L;
        long vendorId = 3L;

        reportTestClient.getModelInfoById(modelId, "modelinfo_1.json");

        vendorApiTestClient.vendorByBrandId(brandId, "vendor_by_brand_id_2.json");
        vendorApiTestClient.vendorModelParamsByModel(vendorId, modelId, "multi_numeric_params_model_1.json");

        List<AbstractModelParameter> parameters = getParams(modelId, brandId);
        assertThat(parameters, hasSize(1));

        List<Object> values = parameters.get(0).getValues();
        assertThat(values, hasSize(2));
        assertThat(values, contains(114.0, 115.1));

        assertThat(parameters.get(0).isMandatory(), is(false));
    }

    @Test
    public void modelParameterMultiEnum() {
        long modelId = 1L;
        long brandId = 2L;
        long vendorId = 3L;

        reportTestClient.getModelInfoById(modelId, "modelinfo_1.json");

        vendorApiTestClient.vendorByBrandId(brandId, "vendor_by_brand_id_2.json");
        vendorApiTestClient.vendorModelParamsByModel(vendorId, modelId, "multi_enum_params_model_1.json");

        List<AbstractModelParameter> parameters = getParams(modelId, brandId);
        assertThat(parameters, hasSize(1));

        List<Object> values = parameters.get(0).getValues();
        assertThat(values, hasSize(2));
        assertThat(values, contains(12104378L, 12104382L));

        assertThat(parameters.get(0).isMandatory(), is(false));
    }

    @Test
    public void modelParameterMultiText() {
        long modelId = 1L;
        long brandId = 2L;
        long vendorId = 3L;

        reportTestClient.getModelInfoById(modelId, "modelinfo_1.json");

        vendorApiTestClient.vendorByBrandId(brandId, "vendor_by_brand_id_2.json");
        vendorApiTestClient.vendorModelParamsByModel(vendorId, modelId, "multi_text_params_model_1.json");

        List<AbstractModelParameter> parameters = getParams(modelId, brandId);
        assertThat(parameters, hasSize(1));

        List<Object> values = parameters.get(0).getValues();
        assertThat(values, hasSize(2));
        assertThat(values, contains("Energy", "Power"));

        assertThat(parameters.get(0).isMandatory(), is(false));
    }

    @Test
    public void modelParameterNumericNullSafe() {
        long modelId = 1L;
        long brandId = 2L;
        long vendorId = 3L;

        reportTestClient.getModelInfoById(modelId, "modelinfo_1.json");

        vendorApiTestClient.vendorByBrandId(brandId, "vendor_by_brand_id_2.json");
        vendorApiTestClient.vendorModelParamsByModel(vendorId, modelId, "nullsafe_numeric_params_model_1.json");

        List<AbstractModelParameter> parameters = getParams(modelId, brandId);
        assertThat(parameters, hasSize(1));
        assertThat(parameters.get(0).getValues(), nullValue());

        assertThat(parameters.get(0).isMandatory(), is(false));
    }

    @Test
    public void shouldNotValidateEmptyCreateRequest() {
        exception.expect(InvalidParameterValueException.class);
        vendorApiController.createRequest(
            new ModelParametersRequest(),
            new VendorId(1L),
            USER,
            new ValidationErrors()
        ).waitResult();
    }

    @Test
    public void shouldNotValidateEmptyUpdateRequest() {
        exception.expect(InvalidParameterValueException.class);
        vendorApiController.updateRequest(
            new ModelParametersRequest(),
            1L,
            new VendorId(1L),
            USER,
            new ValidationErrors()
        ).waitResult();
    }

    @Test
    public void shouldReturnMandatoryProperty() {
        long modelId = 1L;
        long brandId = 2L;
        long vendorId = 3L;

        reportTestClient.getModelInfoById(modelId, "modelinfo_1.json");

        vendorApiTestClient.vendorByBrandId(brandId, "vendor_by_brand_id_2.json");
        vendorApiTestClient.vendorModelParamsByModel(vendorId, modelId, "mandatory_params_model_1.json");

        List<AbstractModelParameter> parameters = getParams(modelId, brandId);
        assertThat(parameters, hasSize(1));
        assertThat(parameters.get(0).isMandatory(), is(true));
    }

    @Test
    public void shouldNotValidateCreateRequestWithoutMandatoryParameter() {
        long brandId = 2L;
        long vendorId = 3L;

        exception.expect(InvalidParameterValueException.class);

        ModelParametersRequest request = new ModelParametersRequest();
        request.setCategoryId(1L);
        request.setTitle("aa");
        request.setImages(Arrays.asList(new Image()));
        request.setParameters(Arrays.asList(text(1, "aa")));

        vendorApiTestClient.vendorCategoryParamsByModel(vendorId, 1L, "category_1_with_mandatories.json");
        vendorApiTestClient.vendorByBrandId(brandId, "vendor_by_brand_id_2.json");

        vendorApiController.createRequest(
            request,
            new VendorId(brandId),
            USER,
            new ValidationErrors()
        ).waitResult();
    }

    @Test
    public void shouldNotValidateUpdateRequestWithoutMandatoryParameter() {
        long brandId = 2L;
        long vendorId = 3L;

        exception.expect(InvalidParameterValueException.class);

        ModelParametersRequest request = new ModelParametersRequest();
        request.setCategoryId(1L);
        request.setTitle("aa");
        request.setImages(Arrays.asList(new Image()));
        request.setParameters(Arrays.asList(text(1, "aa")));

        vendorApiTestClient.vendorCategoryParamsByModel(vendorId, 1L, "category_1_with_mandatories.json");
        vendorApiTestClient.vendorByBrandId(brandId, "vendor_by_brand_id_2.json");

        vendorApiController.updateRequest(
            request,
            1L,
            new VendorId(brandId),
            USER,
            new ValidationErrors()
        ).waitResult();
    }

    @Test
    public void categoryParameterNumericWithRange() {
        long categoryId = 1L;
        long brandId = 2L;
        long vendorId = 3L;

        vendorApiTestClient.vendorByBrandId(brandId, "vendor_by_brand_id_2.json");
        vendorApiTestClient.vendorCategoryParamsByModel(vendorId, categoryId, "category_param_with_range.json");

        List<AbstractModelParameter> parameters = getCategoryParams(categoryId, brandId);
        assertThat(parameters, hasSize(1));
        assertTrue(parameters.get(0) instanceof NumericModelParameter);
        NumericModelParameter numericModelParameter = (NumericModelParameter) parameters.get(0);
        assertEquals(BigDecimal.valueOf(16.0), numericModelParameter.getMinValue());
        assertEquals(BigDecimal.valueOf(140.0), numericModelParameter.getMaxValue());
    }

    private List<AbstractModelParameter> getParams(long modelId, long brandId) {
        return vendorApiController.getModelParameters(modelId, new VendorId(brandId), USER)
            .waitResult()
            .getParameters();
    }

    private List<AbstractModelParameter> getCategoryParams(long categoryId, long brandId) {
        return vendorApiController.getCategoryParameters(categoryId, new VendorId(brandId), USER)
                .waitResult()
                .getParameters();
    }

    private TextModelParameter text(long id, String ... values) {
        TextModelParameter param = new TextModelParameter();
        param.setParameterId(id);
        param.setValues(Arrays.asList(values));
        return param;
    }
}
