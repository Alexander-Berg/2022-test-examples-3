package ru.yandex.direct.internaltools.core;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.internaltools.core.annotations.output.Enrich;
import ru.yandex.direct.internaltools.core.container.InternalToolDetailsData;
import ru.yandex.direct.internaltools.core.container.InternalToolMassResult;
import ru.yandex.direct.internaltools.core.container.InternalToolParameter;
import ru.yandex.direct.internaltools.core.container.InternalToolResult;
import ru.yandex.direct.internaltools.core.enrich.InternalToolEnrichProcessor;
import ru.yandex.direct.internaltools.core.enrich.InternalToolEnrichProcessorFactory;
import ru.yandex.direct.internaltools.core.enums.InternalToolAction;
import ru.yandex.direct.internaltools.core.enums.InternalToolCategory;
import ru.yandex.direct.internaltools.core.enums.InternalToolDetailKey;
import ru.yandex.direct.internaltools.core.enums.InternalToolDetailsCategory;
import ru.yandex.direct.internaltools.core.enums.InternalToolType;
import ru.yandex.direct.internaltools.core.exception.InternalToolProcessingException;
import ru.yandex.direct.internaltools.core.exception.InternalToolValidationException;
import ru.yandex.direct.internaltools.core.input.InternalToolInputGroup;
import ru.yandex.direct.validation.builder.ItemValidationBuilder;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.direct.internaltools.core.enums.InternalToolAccessRole.DEVELOPER;
import static ru.yandex.direct.internaltools.core.enums.InternalToolAccessRole.SUPER;
import static ru.yandex.direct.validation.constraint.CommonConstraints.notNull;

public class InternalToolProxyTest {
    private static final String LABEL = "test_tool";
    private static final String NAME = "testTool";
    private static final String DESCRIPTION = "descr";

    private static class TestParam extends InternalToolParameter {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    private static class TestResult {
        @Enrich(InternalToolDetailKey.CAMPAIGN_ID)
        private String value;

        public String getValue() {
            return value;
        }

        public TestResult withValue(String value) {
            this.value = value;
            return this;
        }
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Mock
    private BaseInternalTool<TestParam> tool;
    @Mock
    private User operator;
    @Mock
    private InternalToolEnrichProcessorFactory enrichProcessorFactory;
    @Mock
    private InternalToolEnrichProcessor enrichProcessor;
    @Mock
    private InternalToolInputGroup<TestParam> group;

    private InternalToolProxy<TestParam> proxy;
    private InternalToolMassResult<TestResult> result;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        result = new InternalToolMassResult<TestResult>()
                .addItem(new TestResult().withValue("value1"))
                .addItem(new TestResult().withValue("value2"));

        doReturn(result)
                .when(tool).process(any());
        doReturn(result)
                .when(tool).processWithoutInput();
        doReturn(ItemValidationBuilder.<TestParam, Defect>of(new TestParam()).getResult())
                .when(tool).validate(any());

        // Debugging, DIRECT-70985
        when(enrichProcessorFactory.forField(any(), any())).thenAnswer(invocation -> {
            Field field = invocation.getArgument(0);
            Class<?> cls = invocation.getArgument(1);
            String msg = String.format("Calling enrichProcessorFactory.forField: %s, %s", field, cls);
            return null;
        });

        proxy = new InternalToolProxy<>(tool, new HashSet<>(Arrays.asList(SUPER, DEVELOPER)), null,
                InternalToolCategory.MODERATE, LABEL, NAME, DESCRIPTION,
                InternalToolAction.SHOW, InternalToolType.REPORT, Collections.emptyList(),
                TestParam.class, Collections.singletonList(group), enrichProcessorFactory, Collections.emptySet());
    }

    @Test
    public void bareRun() {
        InternalToolResult actualResult = proxy.bareRun();

        verify(tool).processWithoutInput();
        assertThat(actualResult)
                .isEqualTo(result);
    }

    @Test
    public void process() {
        InternalToolResult actualResult = proxy.process(Collections.singletonMap("value", "somedata"), operator);

        verify(tool).validate(any(TestParam.class));
        verify(group).addValidation(any(), any(TestParam.class));
        verify(tool).process(any(TestParam.class));
        assertThat(actualResult)
                .isEqualTo(result);
    }

    @Test
    public void processGroupValidationError() {
        doAnswer(a -> {
            ItemValidationBuilder<TestParam, Defect> validationBuilder = a.getArgument(0);
            validationBuilder.item(null, "Value").check(notNull());
            return null;
        }).when(group).addValidation(any(), any(TestParam.class));

        exception.expect(InternalToolValidationException.class);
        exception.expect(hasProperty("validationResult"));
        proxy.process(Collections.emptyMap(), operator);
    }

    @Test
    public void processValidationError() {
        ValidationResult validationResult = ItemValidationBuilder.<TestParam, Defect>of(null)
                .check(notNull())
                .getResult();
        doReturn(validationResult)
                .when(tool).validate(any());

        exception.expect(InternalToolValidationException.class);
        exception.expect(hasProperty("validationResult", equalTo(validationResult)));
        proxy.process(Collections.emptyMap(), operator);
    }

    @Test
    public void processExceptionOnToolValidate() {
        doThrow(new IllegalStateException())
                .when(tool).validate(any());

        exception.expect(InternalToolProcessingException.class);
        proxy.process(Collections.emptyMap(), operator);
    }

    @Test
    public void processValidationExceptionOnToolValidate() {
        doThrow(new InternalToolValidationException(""))
                .when(tool).validate(any());

        exception.expect(InternalToolValidationException.class);
        proxy.process(Collections.emptyMap(), operator);
    }

    @Test
    public void processValidationExceptionOnToolProcess() {
        doThrow(new InternalToolValidationException(""))
                .when(tool).process(any(TestParam.class));

        exception.expect(InternalToolValidationException.class);
        proxy.process(Collections.emptyMap(), operator);
    }

    @Test
    public void processProcessingExceptionOnToolProcess() {
        doThrow(new InternalToolProcessingException(""))
                .when(tool).process(any(TestParam.class));

        exception.expect(InternalToolProcessingException.class);
        proxy.process(Collections.emptyMap(), operator);
    }

    @Test
    public void enrichDataProcess() {
        doReturn(enrichProcessor)
                .when(enrichProcessorFactory).forField(argThat(hasProperty("name", equalTo("value"))), any());
        InternalToolDetailsData detailsData =
                new InternalToolDetailsData(InternalToolDetailsCategory.COUNTRY, Collections.emptyMap());
        doReturn(detailsData)
                .when(enrichProcessor).fetchDetails(anyList());
        doReturn("value")
                .when(enrichProcessor).getFieldName();
        InternalToolResult actualResult = proxy.process(Collections.singletonMap("value", "somedata"), operator);

        verify(tool).validate(any(TestParam.class));
        verify(tool).process(any(TestParam.class));
        verify(enrichProcessorFactory).forField(any(), any());
        verify(enrichProcessor).fetchDetails(anyList());

        assertThat(actualResult).isInstanceOf(InternalToolMassResult.class);
        assertThat(actualResult).isEqualTo(result);
        assertThat(result.getDetails()).containsOnly(entry("value", detailsData));
    }

    @Test
    public void enrichDataProcessTwice() {
        doReturn(enrichProcessor)
                .when(enrichProcessorFactory).forField(argThat(hasProperty("name", equalTo("value"))), any());
        InternalToolDetailsData detailsData =
                new InternalToolDetailsData(InternalToolDetailsCategory.COUNTRY, Collections.emptyMap());
        doReturn(detailsData)
                .when(enrichProcessor).fetchDetails(anyList());
        doReturn("value")
                .when(enrichProcessor).getFieldName();
        proxy.process(Collections.singletonMap("value", "somedata"), operator);
        proxy.process(Collections.singletonMap("value", "somedata"), operator);

        verify(tool, times(2)).validate(any(TestParam.class));
        verify(tool, times(2)).process(any(TestParam.class));
        verify(enrichProcessorFactory).forField(any(), any()); // вызывали только один раз
        verify(enrichProcessor, times(2)).fetchDetails(anyList());
    }

    @Test
    public void enrichDataProcessError() {
        doReturn(enrichProcessor)
                .when(enrichProcessorFactory).forField(argThat(hasProperty("name", equalTo("value"))), any());
        InternalToolDetailsData detailsData =
                new InternalToolDetailsData(InternalToolDetailsCategory.COUNTRY, Collections.emptyMap());
        doThrow(new IllegalStateException())
                .when(enrichProcessor).fetchDetails(anyList());
        doReturn("value")
                .when(enrichProcessor).getFieldName();

        exception.expect(InternalToolProcessingException.class);
        proxy.process(Collections.singletonMap("value", "somedata"), operator);
    }
}
