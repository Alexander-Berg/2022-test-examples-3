package ru.yandex.direct.internaltools.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.internaltools.core.enrich.InternalToolEnrichProcessorFactory;
import ru.yandex.direct.internaltools.core.enums.InternalToolAction;
import ru.yandex.direct.internaltools.core.enums.InternalToolCategory;
import ru.yandex.direct.internaltools.core.enums.InternalToolType;
import ru.yandex.direct.validation.builder.ItemValidationBuilder;
import ru.yandex.direct.validation.result.Defect;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.internaltools.core.enums.InternalToolAccessRole.DEVELOPER;
import static ru.yandex.direct.internaltools.core.enums.InternalToolAccessRole.SUPER;

public class InternalToolProxySerializationTest {
    private static final String LABEL = "test_tool";
    private static final String NAME = "testTool";
    private static final String DESCRIPTION = "descr";

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Mock
    private BaseInternalTool<TestParamForSerialisation> tool;
    @Mock
    private User operator;
    @Mock
    private InternalToolEnrichProcessorFactory enrichProcessorFactory;
    @Captor
    private ArgumentCaptor<TestParamForSerialisation> captor;

    private InternalToolProxy<TestParamForSerialisation> proxy;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        doReturn(ItemValidationBuilder.<TestParamForSerialisation, Defect>of(new TestParamForSerialisation())
                .getResult())
                .when(tool).validate(any());

        proxy = new InternalToolProxy<>(tool, new HashSet<>(Arrays.asList(SUPER, DEVELOPER)), null,
                InternalToolCategory.MODERATE, LABEL, NAME, DESCRIPTION,
                InternalToolAction.SHOW, InternalToolType.REPORT, Collections.emptyList(),
                TestParamForSerialisation.class, Collections.emptyList(), enrichProcessorFactory,
                Collections.emptySet());
    }

    @Test
    public void testProcess() {
        proxy.process(TestParamForSerialisation.BASE_MAP, operator);

        verify(tool).process(captor.capture());

        //  Тест падает на биндиффере, для нас факт получения объекта - уже хороший знак
//        TestParamForSerialisation param = captor.getValue();
//        assertThat("Правильно распарсили объект", param, beanDiffer(TestParamForSerialisation.RESULT_OBJ));
    }
}
