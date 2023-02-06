package ru.yandex.market.api.util.validators;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;

import org.junit.Test;

import ru.yandex.market.api.controller.v2.vendor.request.ModelParametersRequest;
import ru.yandex.market.api.controller.v2.vendor.request.ModelParametersRequestValidator;
import ru.yandex.market.api.controller.v2.vendor.request.ModelParametersRequestValidatorChecks;
import ru.yandex.market.api.controller.v2.vendor.request.TextModelParameter;
import ru.yandex.market.api.error.ValidationError;
import ru.yandex.market.api.error.ValidationErrors;
import ru.yandex.market.api.integration.UnitTestBase;

/**
 * @author dimkarp93
 */
public class ModelParametersRequestValidatorTest extends UnitTestBase {
    @Test
    public void shouldNotNullRequest() {
        expectMessage("empty");
        validate(null, ModelParametersRequestValidatorChecks::required);
    }

    @Test
    public void shouldNotNullTitleInRequest() {
        expectMessage("//request/@title");
        ModelParametersRequest request = new ModelParametersRequest();
        request.setTitle(null);
        validate(request, ModelParametersRequestValidatorChecks::title);
    }

    @Test
    public void shouldNotEmptyTitleInRequest() {
        expectMessage("//request/@title");
        ModelParametersRequest request = new ModelParametersRequest();
        request.setTitle("");
        validate(request, ModelParametersRequestValidatorChecks::title);
    }

    @Test
    public void shouldNotNullParamsInRequest() {
        expectMessage("//request/parameters");
        ModelParametersRequest request = new ModelParametersRequest();
        request.setParameters(null);
        validate(request, ModelParametersRequestValidatorChecks::parameters);
    }

    @Test
    public void shouldNotEmptyParamsInRequest() {
        expectMessage("//request/parameters");
        ModelParametersRequest request = new ModelParametersRequest();
        request.setParameters(Collections.emptyList());
        validate(request, ModelParametersRequestValidatorChecks::parameters);
    }

    @Test
    public void shouldNotEmptyParamValuesInRequest() {
        expectMessage("//request/parameters/parameter[]/@values");
        ModelParametersRequest request = new ModelParametersRequest();
        request.setParameters(
                Arrays.asList(
                        text(1L)
                )
        );
        validate(request, ModelParametersRequestValidatorChecks::parametersValues);
    }

    @Test
    public void shouldNotImagesParamsInRequest() {
        expectMessage("//request/images");
        ModelParametersRequest request = new ModelParametersRequest();
        request.setImages(null);
        validate(request, ModelParametersRequestValidatorChecks::images);
    }

    @Test
    public void shouldNotEmptyImagesInRequest() {
        expectMessage("//request/images");
        ModelParametersRequest request = new ModelParametersRequest();
        request.setImages(Collections.emptyList());
        validate(request, ModelParametersRequestValidatorChecks::images);
    }

    @Test
    public void shouldNotNullCategoryIdInRequest() {
        expectMessage("//request/@categoryId");
        ModelParametersRequest request = new ModelParametersRequest();
        request.setCategoryId(null);
        validate(request, ModelParametersRequestValidatorChecks::category);
    }

    @Test
    public void shouldCategoryIdNotLessThanZero() {
        expectMessage("//request/@categoryId");
        ModelParametersRequest request = new ModelParametersRequest();
        request.setCategoryId(-1L);
        validate(request, ModelParametersRequestValidatorChecks::category);
    }

    @Test
    public void shouldCategoryIdEqualsZero() {
        expectMessage("//request/@categoryId");
        ModelParametersRequest request = new ModelParametersRequest();
        request.setCategoryId(0L);
        validate(request, ModelParametersRequestValidatorChecks::category);
    }

    @Test
    public void shouldExistsAllMandatoryParametersInRequest() {
        expectMessage("mandatory", "2");
        ModelParametersRequest request = new ModelParametersRequest();

        request.setParameters(
            Arrays.asList(
                text(1L, "aa", "bb"),
                text(3L, "bb")
            )
        );

        validate(
            request,
            req -> ModelParametersRequestValidatorChecks.mandatoryParameters(
                req,
                Arrays.asList(
                    text(2L),
                    text(1L)
                )
            )
        );
    }

    @Test
    public void shouldValidateChecksThatSelected() {
        expectMessage(
            "//request/@title",
            "//request/parameters"
        );

        ModelParametersRequest request = new ModelParametersRequest();
        request.setTitle("");
        request.setParameters(null);
        request.setImages(null);
        validate(
            request,
            ModelParametersRequestValidatorChecks::required,
            ModelParametersRequestValidatorChecks::title,
            ModelParametersRequestValidatorChecks::parameters
        );
    }

    private void validate(ModelParametersRequest request,
                          Function<ModelParametersRequest, ValidationError> ... validators) {
        ValidationErrors errors = new ValidationErrors();
        new ModelParametersRequestValidator(validators).validate(request, errors);
        errors.throwIfHasErrors();

    }

    private static TextModelParameter text(long id, String ... values) {
        TextModelParameter param = new TextModelParameter();
        param.setParameterId(id);
        param.setValues(Arrays.asList(values));
        return param;
    }
}
