package ru.yandex.market.mbo.common.processing;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author s-ermakov
 */
public class OperationExceptionTest {

    private static final String GROUP = "test-group";

    private static final ProcessingResult RESULT_1 = new ProcessingResult(GROUP, "error #1");
    private static final ProcessingResult RESULT_2 = new ProcessingResult(GROUP, "error #2");

    @Test
    public void testEmptyConstructor() throws Exception {
        OperationException operationException = new OperationException();
        assertEquals("", operationException.getMessage());
        assertDetails(operationException);
    }

    @Test
    public void testConstructorWithMessage() throws Exception {
        OperationException operationException = new OperationException("hello");
        assertEquals("hello", operationException.getMessage());
        assertDetails(operationException);
    }

    @Test
    public void testConstructorWithThrowable() throws Exception {
        RuntimeException runtimeException = new RuntimeException("inner exception");

        OperationException operationException = new OperationException(runtimeException);

        assertThat(operationException.getMessage(), containsString(runtimeException.getMessage()));
        assertEquals(runtimeException, operationException.getCause());
        assertDetails(operationException);
    }

    @Test
    public void testConstructorWithMessageAndThrowable() throws Exception {
        RuntimeException runtimeException = new RuntimeException("inner exception");

        OperationException operationException = new OperationException("hello", runtimeException);

        assertEquals("hello", operationException.getMessage());
        assertEquals(runtimeException, operationException.getCause());
        assertDetails(operationException);
    }

    @Test
    public void testConstructorWithProcessingResult() throws Exception {
        OperationException operationException = new OperationException(RESULT_1);

        assertEquals(RESULT_1.getText(), operationException.getMessage());
        assertDetails(operationException, RESULT_1);
    }

    @Test
    public void testConstructorWithProcessingResults() throws Exception {
        OperationException operationException = new OperationException(Arrays.asList(RESULT_1, RESULT_2));

        assertThat(operationException.getMessage(), not(isEmptyString()));
        assertDetails(operationException, RESULT_1, RESULT_2);
    }

    @Test
    public void testConstructorWithMessageAndProcessingResults() throws Exception {
        OperationException operationException = new OperationException("hello", Arrays.asList(RESULT_1, RESULT_2));

        assertThat("hello", not(isEmptyString()));
        assertDetails(operationException, RESULT_1, RESULT_2);
    }

    private static void assertDetails(OperationException actual, ProcessingResult... errors) {
        assertDetails(actual, Arrays.asList(errors));
    }

    private static void assertDetails(OperationException actual, List<ProcessingResult> errors) {
        String errorsStr = errors.stream()
            .map(ProcessingResult::getText)
            .collect(Collectors.joining("\n"));

        Map<String, List<ProcessingResult>> groupedErrors = errors.stream()
            .collect(Collectors.groupingBy(ProcessingResult::getGroup));

        assertEquals(errors, actual.getDetailedErrors());
        assertEquals(groupedErrors, actual.getGroupedErrors());
        assertEquals(errorsStr, actual.getDescription());
        assertThat(actual.getDetailedDescription(), containsString(errorsStr));
    }
}
