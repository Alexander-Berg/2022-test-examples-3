package ru.yandex.market.delivery.mdbapp.integration.router;

import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CharSequenceInputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.delivery.mdbapp.components.logging.BackLogOrderMilestoneTimingsTskvLogger;
import ru.yandex.market.delivery.mdbapp.components.logging.EventFlowParametersHolder;
import ru.yandex.market.delivery.mdbapp.components.service.InternalVariableService;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:WEB-INF/checkouter-client.xml")
public class CheckpointOrderEventTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Autowired
    @Qualifier("checkouterJsonMessageConverter")
    private HttpMessageConverter checkouterJsonMessageConverter;

    private final BackLogOrderMilestoneTimingsTskvLogger tskvLogger = new BackLogOrderMilestoneTimingsTskvLogger(
        new EventFlowParametersHolder(),
        new TestableClock()
    );
    private final OrderEventsRouter router = new OrderEventsRouter(tskvLogger, mock(InternalVariableService.class));

    private OrderHistoryEvent event;

    @Before
    public void setUp() throws Exception {

        String checkpointEventJson = IOUtils.toString(
            this.getClass().getResourceAsStream("/checkpoint_changed_event.json"),
            StandardCharsets.UTF_8
        );

        event = getEvent(checkpointEventJson);
    }

    private OrderHistoryEvent getEvent(String checkpointEventJson) throws java.io.IOException {
        return (OrderHistoryEvent) checkouterJsonMessageConverter.read(
            OrderHistoryEvent.class,
            new MockHttpInputMessage(new CharSequenceInputStream(checkpointEventJson, StandardCharsets.UTF_8))
        );
    }

    @Test
    public void checkRouting() {
        assertEquals(
            "Event routed incorrectly",
            OrderEventsRouter.CHANNEL_CHECKPOINT_CHANGED,
            router.route(event)
        );
    }
}
