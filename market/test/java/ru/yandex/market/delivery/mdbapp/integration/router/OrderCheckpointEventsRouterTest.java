package ru.yandex.market.delivery.mdbapp.integration.router;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CharSequenceInputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackCheckpoint;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
@ContextConfiguration("classpath:WEB-INF/checkouter-client.xml")
public class OrderCheckpointEventsRouterTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Autowired
    @Qualifier("checkouterJsonMessageConverter")
    private HttpMessageConverter checkouterJsonMessageConverter;

    private TestContextManager testContextManager;

    private OrderCheckpointEventsRouter router;

    private OrderHistoryEvent event;

    @Parameter
    public DeliveryServiceType deliveryServiceType;

    @Parameter(1)
    public List<TrackCheckpoint> checkpointsBefore;

    @Parameter(2)
    public List<TrackCheckpoint> checkpointsAfter;

    @Parameter(3)
    public String expectedChannel;

    @Parameters
    public static Object[][] data() {
        return new Object[][] {
            {
                DeliveryServiceType.SORTING_CENTER,
                Collections.emptyList(),
                checkpoints(110),
                OrderEventsByTypeRouter.CHANNEL_DISCARDED
            },
            {
                DeliveryServiceType.SORTING_CENTER,
                Collections.emptyList(),
                checkpoints(110, 120),
                OrderCheckpointEventsRouter.GET_ORDER_FROM_FF
            },
            {
                DeliveryServiceType.SORTING_CENTER,
                Collections.emptyList(),
                checkpoints(110, 50),
                OrderCheckpointEventsRouter.GET_ORDER_FROM_FF
            },
            {
                DeliveryServiceType.SORTING_CENTER,
                Collections.emptyList(),
                checkpoints(50, 110),
                OrderCheckpointEventsRouter.GET_ORDER_FROM_FF,
            },
            {
                DeliveryServiceType.SORTING_CENTER,
                checkpoints(120, 40),
                checkpoints(120, 40, 50),
                OrderEventsByTypeRouter.CHANNEL_DISCARDED
            },
            {
                DeliveryServiceType.SORTING_CENTER,
                Collections.emptyList(),
                checkpoints(130, 110),
                OrderCheckpointEventsRouter.GET_ORDER_FROM_FF
            },
            {
                DeliveryServiceType.CARRIER,
                Collections.emptyList(),
                checkpoints(130, 110),
                OrderEventsByTypeRouter.CHANNEL_DISCARDED
            },
            {
                DeliveryServiceType.CARRIER,
                Collections.emptyList(),
                new ArrayList<TrackCheckpoint>(),
                OrderEventsByTypeRouter.CHANNEL_DISCARDED
            },
            {
                DeliveryServiceType.SORTING_CENTER,
                Collections.emptyList(),
                new ArrayList<TrackCheckpoint>(),
                OrderEventsByTypeRouter.CHANNEL_DISCARDED
            },
            {
                DeliveryServiceType.FULFILLMENT,
                Collections.emptyList(),
                checkpoints(110),
                OrderEventsByTypeRouter.CHANNEL_DISCARDED
            },
            {
                DeliveryServiceType.FULFILLMENT,
                Collections.emptyList(),
                checkpoints(110, 120),
                OrderCheckpointEventsRouter.GET_ORDER_FROM_FF
            },
            {
                DeliveryServiceType.FULFILLMENT,
                Collections.emptyList(),
                checkpoints(110, 50),
                OrderCheckpointEventsRouter.GET_ORDER_FROM_FF
            },
            {
                DeliveryServiceType.FULFILLMENT,
                Collections.emptyList(),
                checkpoints(50, 110),
                OrderCheckpointEventsRouter.GET_ORDER_FROM_FF
            },
            {
                DeliveryServiceType.FULFILLMENT,
                checkpoints(120, 40),
                checkpoints(120, 40, 50),
                OrderEventsByTypeRouter.CHANNEL_DISCARDED
            },
            {
                DeliveryServiceType.FULFILLMENT,
                Collections.emptyList(),
                checkpoints(130, 110),
                OrderCheckpointEventsRouter.GET_ORDER_FROM_FF
            },
            {
                DeliveryServiceType.FULFILLMENT,
                Collections.emptyList(),
                new ArrayList<TrackCheckpoint>(),
                OrderEventsByTypeRouter.CHANNEL_DISCARDED
            }
        };
    }

    @Before
    public void setUp() throws Exception {
        initSpringContext();
        initRouter();
        initEvent();
    }

    private void initSpringContext() throws Exception {
        this.testContextManager = new TestContextManager(getClass());
        this.testContextManager.prepareTestInstance(this);
    }

    private void initRouter() {
        router = new OrderCheckpointEventsRouter();
    }

    private void initEvent() throws IOException {
        String checkpointEventJson = IOUtils.toString(
            this.getClass().getResourceAsStream("/checkpoint_changed_event.json"),
            "utf8"
        );

        event = getEvent(checkpointEventJson);
        event.getOrderBefore().getDelivery().getParcels().get(0).getTracks().get(1).setCheckpoints(checkpointsBefore);
        event.getOrderAfter().getDelivery().getParcels().get(0).getTracks().get(1).setCheckpoints(checkpointsAfter);
        event.getOrderAfter().getDelivery().getParcels().get(0).getTracks().get(1)
            .setDeliveryServiceType(deliveryServiceType);
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
            expectedChannel,
            router.route(event)
        );
    }

    private static List<TrackCheckpoint> checkpoints(Integer... deliveryCheckpointNumbers) {
        ArrayList<TrackCheckpoint> checkpoints = new ArrayList<>();

        for (Integer deliveryCheckpointNumber : deliveryCheckpointNumbers) {
            TrackCheckpoint trackCheckpoint = new TrackCheckpoint();
            trackCheckpoint.setDeliveryCheckpointStatus(deliveryCheckpointNumber);

            checkpoints.add(trackCheckpoint);
        }
        return checkpoints;
    }
}
