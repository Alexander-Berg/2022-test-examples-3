package ru.yandex.market.api.controller.v2.market.vendor;

import org.junit.Test;
import ru.yandex.market.api.controller.v2.vendor.AgencyControllerV2;
import ru.yandex.market.api.controller.v2.vendor.request.ModelParametersRequest;
import ru.yandex.market.api.controller.v2.vendor.request.TextModelParameter;
import ru.yandex.market.api.error.InvalidParameterValueException;
import ru.yandex.market.api.error.ValidationErrors;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.market.vendor.domain.Image;
import ru.yandex.market.api.util.httpclient.clients.VendorApiTestClient;

import javax.inject.Inject;
import java.util.Arrays;

/**
 * @author dimkarp93
 */
public class AgencyControllerV2Test extends BaseTest {
    @Inject
    private AgencyControllerV2 agencyController;

    @Inject
    private VendorApiTestClient vendorApiTestClient;

    @Test
    public void shouldNotValidateEmptyCreateRequest() {
        exception.expect(InvalidParameterValueException.class);
        agencyController.createRequest(
            1L,
            1L,
            new ModelParametersRequest(),
            new ValidationErrors()
        ).waitResult();
    }

    @Test
    public void shouldNotValidateEmptyUpdateRequest() {
        exception.expect(InvalidParameterValueException.class);
        agencyController.updateRequest(
            1L,
            new ModelParametersRequest(),
            1L,
            1L,
            new ValidationErrors()
        ).waitResult();
    }

    @Test
    public void shouldNotValidateCreateRequestWithoutMandatoryParamter() {
        exception.expect(InvalidParameterValueException.class);

        ModelParametersRequest request = new ModelParametersRequest();
        request.setCategoryId(1L);
        request.setTitle("aa");
        request.setImages(Arrays.asList(new Image()));
        request.setParameters(Arrays.asList(text(1, "aa")));

        vendorApiTestClient.agencyCategoryParamsByModel(1L, 1L, "category_1_with_mandatories.json");

        agencyController.createRequest(
            1L,
            1L,
            request,
            new ValidationErrors()
        ).waitResult();
    }

    @Test
    public void shouldNotValidateUpdateRequestWithoutMandatoryParamter() {
        exception.expect(InvalidParameterValueException.class);

        ModelParametersRequest request = new ModelParametersRequest();
        request.setCategoryId(1L);
        request.setTitle("aa");
        request.setImages(Arrays.asList(new Image()));
        request.setParameters(Arrays.asList(text(1, "aa")));

        vendorApiTestClient.agencyCategoryParamsByModel(1L, 1L, "category_1_with_mandatories.json");

        agencyController.updateRequest(
            1L,
            request,
            1L,
            1L,
            new ValidationErrors()
        ).waitResult();
    }

    private TextModelParameter text(long id, String ... values) {
        TextModelParameter param = new TextModelParameter();
        param.setParameterId(id);
        param.setValues(Arrays.asList(values));
        return param;
    }

}
