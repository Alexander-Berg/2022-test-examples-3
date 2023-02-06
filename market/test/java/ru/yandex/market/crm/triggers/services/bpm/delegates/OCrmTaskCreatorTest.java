package ru.yandex.market.crm.triggers.services.bpm.delegates;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.services.messages.StringTemplator;
import ru.yandex.market.crm.core.services.trigger.CustomAttributesNames;
import ru.yandex.market.crm.triggers.services.external.ocrm.OCrmClient;
import ru.yandex.market.crm.triggers.services.external.ocrm.domain.TaskType;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OCrmTaskCreatorTest {

    private static final String TICKET_COMMENT = "comment";
    private static final String TICKET_TITLE = "title";
    private static final Long ORDER_ID = 1234567L;

    @Mock
    private OCrmClient oCrmClient;

    @Mock
    private StringTemplator templator;

    private OCrmOrderTaskCreator creator;

    @Before
    public void before() {
        when(templator.apply(anyString(), anyMap())).thenAnswer(i -> i.getArgument(0));
        creator = new OCrmOrderTaskCreator(oCrmClient, templator);
    }

    @Test
    public void testCreation() {
        DelegateExecutionContext ctx = getMockedContext();
        creator.doExecute(ctx);
        verify(oCrmClient).createTask(eq(ORDER_ID), eq(TICKET_TITLE), eq(TICKET_COMMENT));
    }

    private DelegateExecutionContext getMockedContext() {
        var ctx = mock(DelegateExecutionContext.class);

        when(ctx.getProcessVariable(ProcessVariablesNames.ORDER_ID)).thenReturn(ORDER_ID);
        when(ctx.getCustomAttribute(CustomAttributesNames.OCRM_TASK_TYPE, String.class))
                .thenReturn(String.valueOf(TaskType.CREATE_TICKET));

        when(ctx.getCustomAttribute(CustomAttributesNames.OCRM_TICKET_COMMENT, String.class))
                .thenReturn(TICKET_COMMENT);
        when(ctx.getCustomAttribute(CustomAttributesNames.OCRM_TICKET_TITLE, String.class))
                .thenReturn(TICKET_TITLE);

        return ctx;
    }
}
