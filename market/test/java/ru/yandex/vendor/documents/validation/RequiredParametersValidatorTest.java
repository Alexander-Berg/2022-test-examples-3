package ru.yandex.vendor.documents.validation;

import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import ru.yandex.vendor.exception.BadParamException;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RequiredParametersValidatorTest {

    private MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();

    @Test
    public void noRequiredWithEmptyParametersMapShouldPass() throws Exception {
        RequiredParametersValidator validator = new RequiredParametersValidator(emptyList());
        try {
            validator.accept(requestParams);
        } catch (BadParamException e) {
            fail("Validation with no required parameters should pass.");
        }
    }

    @Test
    public void requiredWithParametersMapWithRequiredParameterShouldFail() throws Exception {
        String required = "required";
        RequiredParametersValidator validator = new RequiredParametersValidator(singletonList(required));
        requestParams.add("param1", "value1");
        requestParams.add("param2", "value2");
        requestParams.add(required, "value3");
        try {
            validator.accept(requestParams);
        } catch (BadParamException e) {
            fail("Validation with required request parameter should pass.");
        }
    }

    @Test
    public void requiredWithEmptyParametersMapShouldFail() throws Exception {
        String required = "required";
        RequiredParametersValidator validator = new RequiredParametersValidator(singletonList(required));
        try {
            validator.accept(requestParams);
        } catch (BadParamException e) {
            assertTrue("Validation should fail. No parameter '" + required + "' present.", e.getDetails().containsKey(required));
        }
    }

    @Test
    public void requiredWithParametersMapWithoutRequiredParameterShouldFail() throws Exception {
        String required = "required";
        RequiredParametersValidator validator = new RequiredParametersValidator(singletonList(required));
        requestParams.add("param1", "value1");
        requestParams.add("param2", "value2");
        try {
            validator.accept(requestParams);
        } catch (BadParamException e) {
            assertTrue("Validation should fail. No parameter '" + required + "' present.", e.getDetails().containsKey(required));
        }
    }

    @Test
    public void requiredWithParametersMapWithEmptyRequiredParameterShouldFail() throws Exception {
        String required = "required";
        RequiredParametersValidator validator = new RequiredParametersValidator(singletonList(required));
        requestParams.add("param1", "value1");
        requestParams.add("param2", "value2");
        requestParams.add(required, "");
        try {
            validator.accept(requestParams);
        } catch (BadParamException e) {
            assertTrue("Validation should fail. No parameter '" + required + "' present.", e.getDetails().containsKey(required));
        }
    }
}