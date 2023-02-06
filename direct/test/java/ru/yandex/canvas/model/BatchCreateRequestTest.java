package ru.yandex.canvas.model;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.canvas.controllers.html5.BatchCreateRequest;
import ru.yandex.canvas.model.html5.SourceWithId;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BatchCreateRequestTest {
    private static Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static final int MAX_NAME_LEN = 255;

    SourceWithId sourceWithId = new SourceWithId();
    SourceWithId sourceNullId = new SourceWithId();
    SourceWithId sourceWithEmptyId = new SourceWithId();
    SourceWithId sourceWithWhiteSpaceId = new SourceWithId();

    @Before
    public void init() {
        sourceWithId.setId("ab123123ab");
        sourceWithWhiteSpaceId.setId("   ");
        sourceWithEmptyId.setId("");
    }

    private BatchCreateRequest requestOkName(List<SourceWithId> sources) {
        BatchCreateRequest request = new BatchCreateRequest();
        request.setName("Test request");
        request.setSources(sources);
        return request;
    }

    private BatchCreateRequest requestOkSources(String name) {
        BatchCreateRequest request = new BatchCreateRequest();
        request.setName(name);
        request.setSources(Arrays.asList(sourceWithId));
        return request;
    }

    @Test
    public void nullSourcesContainerFails() {
        BatchCreateRequest nullSourcesContainers = requestOkName(null);
        Set<ConstraintViolation<BatchCreateRequest>> validationResult = validator.validate(nullSourcesContainers);
        assertFalse("Null as items is invalid", validationResult.isEmpty());
    }

    @Test
    public void oneItemWithoutIdFails() {
        List<SourceWithId> sources = Arrays.asList(sourceNullId, sourceWithId);
        BatchCreateRequest oneItemWithoutId = requestOkName(sources);
        Set<ConstraintViolation<BatchCreateRequest>> validationResult = validator.validate(oneItemWithoutId);
        assertFalse("Null id item in items is invalid", validationResult.isEmpty());
    }

    @Test
    public void oneItemWithEmptyIdFails() {
        List<SourceWithId> sources = Arrays.asList(sourceWithEmptyId, sourceWithId);
        BatchCreateRequest oneItemWithoutId = requestOkName(sources);
        Set<ConstraintViolation<BatchCreateRequest>> validationResult = validator.validate(oneItemWithoutId);
        assertFalse("Null id item in items is invalid", validationResult.isEmpty());
    }

    @Test
    public void oneItemWithWhiteSpaceIddFails() {
        List<SourceWithId> sources = Arrays.asList(sourceWithWhiteSpaceId, sourceWithId);
        BatchCreateRequest oneItemWithoutId = requestOkName(sources);
        Set<ConstraintViolation<BatchCreateRequest>> validationResult = validator.validate(oneItemWithoutId);
        assertFalse("Null id item in items is invalid", validationResult.isEmpty());
    }

    @Test
    public void properRequest() {
        BatchCreateRequest okRequest = requestOkName(Arrays.asList(sourceWithId));
        Set<ConstraintViolation<BatchCreateRequest>> validationResult = validator.validate(okRequest);
        assertTrue("Source container with items with one item with id passes", validationResult.isEmpty());
    }

    @Test
    public void noNameFails() {
        Set<ConstraintViolation<BatchCreateRequest>> validationResult = validator.validate(requestOkSources(null));
        assertFalse("Not name request is invalid", validationResult.isEmpty());
    }

    @Test
    public void emptpyNameFails() {
        Set<ConstraintViolation<BatchCreateRequest>> validationResult = validator.validate(requestOkSources(""));
        assertFalse("Empty name is invalid", validationResult.isEmpty());
    }

    @Test
    public void whiteSpaceNameFails() {
        Set<ConstraintViolation<BatchCreateRequest>> validationResult = validator.validate(requestOkSources(
                StringUtils.repeat(" ", 10)));
        assertFalse("White space name is invalid", validationResult.isEmpty());
    }

    @Test
    public void minLenNameOk() {
        Set<ConstraintViolation<BatchCreateRequest>> validationResult = validator.validate(requestOkSources("a"));
        assertTrue("Min name length is ok", validationResult.isEmpty());
    }

    @Test
    public void maxLenNameOk() {
        Set<ConstraintViolation<BatchCreateRequest>> validationResult = validator.validate(requestOkSources(
                StringUtils.repeat("a", MAX_NAME_LEN)));
        assertTrue("Max name length is ok", validationResult.isEmpty());
    }

    @Test
    public void exceedLenNameFails() {
        Set<ConstraintViolation<BatchCreateRequest>> validationResult = validator.validate(requestOkSources(
                StringUtils.repeat("a", MAX_NAME_LEN + 1)));
        assertFalse("Exceeded name length fails", validationResult.isEmpty());
    }
}
