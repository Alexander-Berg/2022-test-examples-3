package ru.yandex.market.delivery.mdbapp.integration.router;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import ru.yandex.market.delivery.dsmclient.payload.Task.Queue;
import ru.yandex.market.delivery.mdbapp.integration.gateway.OrderEventsGateway;

@RunWith(Parameterized.class)
public class CreateTaskReplyRouterTest {
    private final CreateTaskReplyRouter createTaskReplyRouter = new CreateTaskReplyRouter();

    @Parameter
    public Queue queue;

    @Parameter(1)
    public String expectedChannel;

    @Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        Collection<Object[]> parameters = new ArrayList<>();

        parameters.add(new Object[]{
            Queue.ORDER_CREATE,
            OrderEventsGateway.CHANNEL_LOG_SUCCESSFUL_ORDER_EVENTS_FLOW,
        });
        parameters.add(new Object[]{Queue.ORDER_CANCEL, OrderEventsRouter.CANCELLED_ORDER_FLOW});
        parameters.add(new Object[]{
            Queue.ORDER_RECREATE,
            OrderEventsGateway.CHANNEL_LOG_SUCCESSFUL_ORDER_EVENTS_FLOW,
        });
        parameters.add(new Object[]{
            Queue.FF_ORDER_CREATE,
            OrderEventsGateway.CHANNEL_LOG_SUCCESSFUL_ORDER_EVENTS_FLOW,
        });
        parameters.add(new Object[]{Queue.FF_ORDER_CANCEL, OrderEventsRouter.CANCELLED_ORDER_FLOW});
        parameters.add(new Object[]{
            Queue.SC_RETURN_REGISTER_CREATE,
            OrderEventsGateway.CHANNEL_LOG_SUCCESSFUL_ORDER_EVENTS_FLOW,
        });

        return parameters;
    }

    @Test
    public void positiveTest() {
        String actualChannel = createTaskReplyRouter.route(null, queue);

        Assert.assertEquals("Unexpected task channel", expectedChannel, actualChannel);
    }
}
