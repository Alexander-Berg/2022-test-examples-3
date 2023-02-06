package ru.yandex.direct.api.v5.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.api.v5.result.ApiMassResult;
import ru.yandex.direct.api.v5.result.ApiResult;
import ru.yandex.direct.api.v5.result.ApiResultState;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.PathConverter;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class OperationOnListDelegateTest {
    private static final List<Object> INTERNAL_REQ = Arrays.asList(1, 2, 3);
    private TestDelegate delegateUnderTest;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    @Mock
    private Function<List<Object>, ApiMassResult<Object>> processList;
    @Mock
    private Function<List<Object>, ValidationResult<List<Object>, DefectType>> validateInternalRequest;

    @Before
    public void setUp() throws Exception {
        delegateUnderTest = new TestDelegate();
    }

    @Test
    public void processRequest_AllValid_CheckCalls() throws Exception {
        validateInternalRequestReturnsAllValid();
        processListReturnsAllValid();

        delegateUnderTest.processRequest(INTERNAL_REQ);

        verify(validateInternalRequest).apply(eq(INTERNAL_REQ));
        verify(processList).apply(eq(INTERNAL_REQ));
    }

    @Test
    public void processRequest_AllValid_ReturnsWholeResult() throws Exception {
        validateInternalRequestReturnsAllValid();
        processListReturnsAllValid();

        ApiResult<List<ApiResult<Object>>> result = delegateUnderTest.processRequest(INTERNAL_REQ);

        assertThat(result.getResult(), hasSize(INTERNAL_REQ.size()));
        assertThat(result.getSuccessfulObjectsCount(), equalTo(INTERNAL_REQ.size()));
    }

    @Test
    public void processRequest_ValidOnPreValidationInvalidOperation_ReturnsProcessListResult() throws Exception {
        validateInternalRequestReturnsAllValid();
        ApiMassResult apiMassResult = createBrokenMassResult();
        processListReturns(apiMassResult);

        ApiResult<List<ApiResult<Object>>> result = delegateUnderTest.processRequest(INTERNAL_REQ);
        assertThat("processResult должен вернуть результат processList в случае ошибки операции", result,
                sameInstance(apiMassResult));
    }

    @Test
    public void processRequest_ValidWithWarningOnPreValidationValidOperation_ReturnsResultWithWarnings() {
        validateInternalRequestReturnsAllSuccessfulOneWithWarning(0);
        processListReturnsAllValid();

        ApiResult<List<ApiResult<Object>>> result = delegateUnderTest.processRequest(INTERNAL_REQ);

        assertThat(result.getResult(), hasSize(INTERNAL_REQ.size()));
        assertThat(result.getSuccessfulObjectsCount(), equalTo(INTERNAL_REQ.size()));

        assertThat(result.getResult().stream()
                .flatMap(r -> r.getWarnings().stream())
                .collect(toList()), hasSize(1));
    }

    @Test
    public void processRequest_OneInvalidOnPreValidationAndOneOnOperation_CheckCalls() throws Exception {
        validateInternalRequestReturnsFirstOfThreeInvalid();
        processListReturnsFirstInvalid();

        delegateUnderTest.processRequest(INTERNAL_REQ);

        verify(processList).apply(eq(INTERNAL_REQ.subList(1, INTERNAL_REQ.size())));
    }

    @Test
    public void processRequest_AllInvalidOnPreValidation_CheckCalls() throws Exception {
        validateInternalRequestReturnsAllInvalid();

        delegateUnderTest.processRequest(INTERNAL_REQ);

        verify(processList, never()).apply(anyList());
    }

    @Test
    public void processRequest_AllInvalidOnPreValidation_ReturnsAllInvalid() throws Exception {
        validateInternalRequestReturnsAllInvalid();

        ApiResult<List<ApiResult<Object>>> result = delegateUnderTest.processRequest(INTERNAL_REQ);

        assertThat("processResult должен вернуть результат для каждого элемента исходного списка",
                result.getResult(), hasSize(INTERNAL_REQ.size()));
        assertThat("в результате все элементы должны быть невалидны",
                result.getUnsuccessfulObjectsCount(), equalTo(INTERNAL_REQ.size()));
        assertThat("Результат с индексом 0 должен быть неуспешным",
                result.getResult().get(0).isSuccessful(), equalTo(false));
        assertThat("результат с индексом 1 должен быть неуспешным",
                result.getResult().get(1).isSuccessful(), equalTo(false));
        assertThat("результат с индексом 2 должен быть корректным",
                result.getResult().get(2).isSuccessful(), equalTo(false));
    }

    @Test
    public void processRequest_OneInvalidOnPreValidationAndOneOnOperation_ReturnsOnlyOneValid() throws Exception {
        validateInternalRequestReturnsFirstOfThreeInvalid();
        processListReturnsFirstInvalid();

        ApiResult<List<ApiResult<Object>>> result = delegateUnderTest.processRequest(INTERNAL_REQ);

        assertThat("processResult должен вернуть результат для каждого элемента исходного списка",
                result.getResult(), hasSize(INTERNAL_REQ.size()));
        assertThat("в результате два элемента должны быть невалидны",
                result.getUnsuccessfulObjectsCount(), equalTo(2));
        assertThat("кол-во успешных элементов должно быть правильгое",
                result.getSuccessfulObjectsCount(), equalTo(INTERNAL_REQ.size() - 2));
        assertThat("Результат с индексом 0 должен быть неуспешным, т.к. его забраковали в validateInternalRequest",
                result.getResult().get(0).isSuccessful(), equalTo(false));
        assertThat("результат с индексом 1 должен быть неуспешным, т.к. его забраковали в processList",
                result.getResult().get(1).isSuccessful(), equalTo(false));
        assertThat("результат с индексом 2 должен быть корректным",
                result.getResult().get(2).isSuccessful(), equalTo(true));
    }

    @Test
    public void processRequest_SeveralWarningsOnPreValidation_ResultWithError_MergedCorrectly() {
        // ворнинг из validateInternalRequest на первом элементе, ошибка из processList на первом
        validateInternalRequestReturnsAllSuccessfulOneWithWarning(0);
        processListReturnsFirstInvalid();
        int unsuccessfulElementsCount = 1;
        int warningElementsCount = 1;

        ApiResult<List<ApiResult<Object>>> result = delegateUnderTest.processRequest(INTERNAL_REQ);

        assertThat("processResult должен вернуть результат для каждого элемента исходного списка",
                result.getResult(), hasSize(INTERNAL_REQ.size()));
        assertThat("кол-во успешных элементов должно быть равно числу всех минус число ошибочных",
                result.getSuccessfulObjectsCount(), equalTo(INTERNAL_REQ.size() - unsuccessfulElementsCount));

        assertThat("ровно один элемент должен содержать и ошибку, и ворнинг",
                result.getResult().stream().filter(t -> !t.getErrors().isEmpty() && !t.getWarnings().isEmpty()).count(),
                equalTo(Long.valueOf(unsuccessfulElementsCount)));
        assertThat("результат с индексом 0 должен быть неуспешным, т.к. его забраковали в processList",
                result.getResult().get(0).isSuccessful(), equalTo(false));
        assertThat("число ворнингов должно быть равно числу элементов с ворнингами в validateInternalRequest",
                result.getResult().stream()
                        .flatMap(r -> r.getWarnings().stream())
                        .collect(toList()), hasSize(warningElementsCount));
        assertThat("число ошибок должно быть равно числу ошибочных элементов в processList",
                result.getResult().stream()
                        .flatMap(r -> r.getErrors().stream())
                        .collect(toList()), hasSize(unsuccessfulElementsCount));
    }

    @Test
    public void processRequest_SingleWarningOnPreValidation_AnotherItemWithError_MergedCorrectly() {
        // ворнинг из validateInternalRequest на втором элементе, ошибка из processList на первом
        validateInternalRequestReturnsAllSuccessfulOneWithWarning(1);
        processListReturnsFirstInvalid();
        int unsuccessfulElementsCount = 1;
        int warningElementsCount = 1;

        ApiResult<List<ApiResult<Object>>> result = delegateUnderTest.processRequest(INTERNAL_REQ);

        assertThat("processResult должен вернуть результат для каждого элемента исходного списка",
                result.getResult(), hasSize(INTERNAL_REQ.size()));
        assertThat("кол-во успешных элементов должно быть равно числу всех минус число ошибочных",
                result.getSuccessfulObjectsCount(), equalTo(INTERNAL_REQ.size() - unsuccessfulElementsCount));

        assertThat("все элементы должны либо не содержать ошибок, либо не содержать ворнингов, или и то, и другое",
                result.getResult().stream().allMatch(t -> t.getErrors().isEmpty() || t.getWarnings().isEmpty()),
                is(true));
        assertThat("результат с индексом 0 должен быть неуспешным, т.к. его забраковали в processList",
                result.getResult().get(0).isSuccessful(), equalTo(false));
        assertThat("число ворнингов должно быть равно числу элементов с ворнингами в validateInternalRequest",
                result.getResult().stream()
                        .flatMap(r -> r.getWarnings().stream())
                        .collect(toList()), hasSize(warningElementsCount));
        assertThat("число ошибок должно быть равно числу ошибочных элементов в processList",
                result.getResult().stream()
                        .flatMap(r -> r.getErrors().stream())
                        .collect(toList()), hasSize(unsuccessfulElementsCount));
    }

    private ApiMassResult createBrokenMassResult() {
        ApiMassResult apiMassResult = mock(ApiMassResult.class);
        when(apiMassResult.isSuccessful()).thenReturn(false);
        return apiMassResult;
    }

    private void validateInternalRequestReturnsAllValid() {
        when(validateInternalRequest.apply(INTERNAL_REQ)).thenReturn(new ValidationResult<>(INTERNAL_REQ));
    }

    private void validateInternalRequestReturnsAllInvalid() {
        ValidationResult<List<Object>, DefectType> vr = new ValidationResult<>(INTERNAL_REQ);
        vr.getOrCreateSubValidationResult(index(0), INTERNAL_REQ.get(0)).addError(mock(DefectType.class));
        vr.getOrCreateSubValidationResult(index(1), INTERNAL_REQ.get(1)).addError(mock(DefectType.class));
        vr.getOrCreateSubValidationResult(index(2), INTERNAL_REQ.get(2)).addError(mock(DefectType.class));
        when(validateInternalRequest.apply(INTERNAL_REQ)).thenReturn(vr);
    }

    private void validateInternalRequestReturnsFirstOfThreeInvalid() {
        ValidationResult<List<Object>, DefectType> vr = new ValidationResult<>(INTERNAL_REQ);
        vr.getOrCreateSubValidationResult(index(0), INTERNAL_REQ.get(0)).addError(mock(DefectType.class));
        when(validateInternalRequest.apply(INTERNAL_REQ)).thenReturn(vr);
    }

    private void validateInternalRequestReturnsAllSuccessfulOneWithWarning(int index) {
        ValidationResult<List<Object>, DefectType> vr = new ValidationResult<>(INTERNAL_REQ);
        vr.getOrCreateSubValidationResult(index(index), INTERNAL_REQ.get(index)).addWarning(mock(DefectType.class));
        when(validateInternalRequest.apply(INTERNAL_REQ)).thenReturn(vr);
    }

    @SuppressWarnings("unchecked")
    private void processListReturnsAllValid() {
        when(processList.apply(anyList())).thenAnswer(invocation -> {
            List<Object> list = (List<Object>) invocation.getArguments()[0];
            return createMassResultAllSuccessful(mapList(list, e -> "ok"));
        });
    }

    private void processListReturns(ApiMassResult mr) {
        when(processList.apply(anyList())).thenAnswer(invocation -> mr);
    }

    @SuppressWarnings("unchecked")
    private void processListReturnsFirstInvalid() {
        when(processList.apply(anyList())).thenAnswer(invocation -> {
            List<Object> list = (List<Object>) invocation.getArguments()[0];
            return createMassResultFirstBroken(mapList(list, e -> "ok"));
        });
    }

    private ApiMassResult<Object> createMassResultAllSuccessful(List<Object> objects) {
        List<ApiResult<Object>> results = mapList(objects, ApiResult::successful);
        return new ApiMassResult<>(results, emptyList(), emptyList(), ApiResultState.SUCCESSFUL);
    }

    private ApiMassResult<Object> createMassResultFirstBroken(List<Object> objects) {
        List<ApiResult<Object>> results = new ArrayList<>(objects.size());
        results.add(ApiResult.broken(
                singletonList(new DefectInfo<>(path(), null, mock(DefectType.class))), emptyList()));
        results.addAll(mapList(objects.subList(1, objects.size()), ApiResult::successful));
        return new ApiMassResult<>(results, emptyList(), emptyList(), ApiResultState.SUCCESSFUL);
    }

    class TestDelegate extends OperationOnListDelegate<Object, Object, Object, Object> {
        TestDelegate() {
            super(PathConverter.identity(), mock(ApiAuthenticationSource.class));
        }

        @Nonnull
        @Override
        public ValidationResult<List<Object>, DefectType> validateInternalRequest(List<Object> internalRequest) {
            return validateInternalRequest.apply(internalRequest);
        }

        @Override
        public ApiMassResult<Object> processList(List<Object> validItems) {
            return processList.apply(validItems);
        }

        @Override
        public List<Object> convertRequest(Object externalRequest) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object convertResponse(ApiResult<List<ApiResult<Object>>> result) {
            throw new UnsupportedOperationException();
        }
    }
}
