package ru.yandex.market.request.datasource.trace;

import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import org.junit.After;
import org.junit.Test;

import ru.yandex.market.request.datasource.trace.DatasourceTimingContext.DatasourceTimingType;
import ru.yandex.market.request.trace.Module;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class TraceSystemMethodsExecutionListenerTest {

    @After
    public void tearDown() {
        DatasourceTimingContext.clear();
    }

    @Test
    public void testGetConnection() {
        TraceSystemMethodsExecutionListener listener = new TraceSystemMethodsExecutionListener(Module.PGAAS);
        MethodExecutionContext ctx = new MethodExecutionContext();

        listener.beforeGetConnection(ctx);
        assertNotNull("getConnection.begin must be not null",
                ctx.getCustomValue("getConnection.begin", Long.class));
        listener.afterGetConnection(ctx);
        assertTrue("Datasource.getConnection() could not be executed in 0 nanoseconds",
                DatasourceTimingContext.get().get(DatasourceTimingType.GET_CONNECTION) > 0);
    }

    @Test
    public void testPrepareStatement() {
        TraceSystemMethodsExecutionListener listener = new TraceSystemMethodsExecutionListener(Module.PGAAS);
        MethodExecutionContext ctx = new MethodExecutionContext();

        listener.beforePrepareStatement(ctx);
        assertNotNull("prepareStatement.begin must be not null",
                ctx.getCustomValue("prepareStatement.begin", Long.class));
        listener.afterPrepareStatement(ctx);
        assertTrue("Connection.prepareStatement() could not be executed in 0 nanoseconds",
                DatasourceTimingContext.get().get(DatasourceTimingType.PREPARE_STATEMENT) > 0);
    }

}
