package ru.yandex.market.mbo.flume.sinkprocessors;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.flume.Context;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Sink;
import org.apache.flume.conf.ConfigurationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.mbo.flume.Register;

/**
 * @author moskovkin@yandex-team.ru
 * @since 19.01.18
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ValveSinkProcessorTest {
    @Mock
    private Sink sink1;
    @Mock
    private Sink sink2;
    @Mock
    private Sink dropSink;
    @Mock
    private Sink throwSink;
    @Mock
    private ValvePositionInterface valve;

    private ValveSinksProcessor sinkProcessor;

    @Before
    public void init() throws EventDeliveryException {
        Mockito.doReturn("sink1").when(sink1).getName();
        Mockito.doReturn("sink2").when(sink2).getName();
        Mockito.doReturn("dropSink").when(dropSink).getName();
        Mockito.doReturn("throwSink").when(throwSink).getName();
        Mockito.doThrow(new EventDeliveryException("Exception from throwSink")).when(throwSink).process();

        Register.put(ValveSinksProcessor.DEFAULT_VALVE_NAME, valve);
        sinkProcessor = new ValveSinksProcessor();
        sinkProcessor.setSinks(Arrays.asList(sink2, sink1, dropSink, throwSink));

        System.setProperty(ValvePositionZkImpl.ZK_HOSTS_PARAM, "zk_host");
        System.setProperty(ValvePositionZkImpl.PARENT_ZK_NODE_PARAM, "zk_node");
    }

    private void configure(ValvePositionInterface.Position valvePosition, List<String> processSinks, String dropSink) {
        HashMap<String, String> config = new HashMap<>();
        if (processSinks != null) {
            config.put(ValveSinksProcessor.PROCESS_SINKS_PARAM, String.join(" ", processSinks));
        }
        if (dropSink != null) {
            config.put(ValveSinksProcessor.DROP_SINK_PARAM, dropSink);
        }
        Context context = new Context(config);
        sinkProcessor.configure(context);
        sinkProcessor.setValve(valve);
        Mockito.doReturn(valvePosition).when(valve).getPosition();
    }

    @Test(expected = ConfigurationException.class)
    public void testNoProcessSink() {
        configure(ValvePositionInterface.Position.PROCESS, null, "dropSink");
    }

    @Test(expected = ConfigurationException.class)
    public void testUnknownProcessSink() {
        configure(ValvePositionInterface.Position.PROCESS, Arrays.asList("unknownSink"), "dropSink");
    }

    @Test(expected = ConfigurationException.class)
    public void testNoDropSink() {
        configure(ValvePositionInterface.Position.PROCESS, Arrays.asList("sink1", "sink2"), null);
    }

    @Test(expected = ConfigurationException.class)
    public void testUnknownDropSink() {
        configure(ValvePositionInterface.Position.PROCESS, Arrays.asList("sink1", "sink2"), "unknownSink");
    }

    @Test
    public void testProcess() throws EventDeliveryException {
        configure(ValvePositionInterface.Position.PROCESS, Arrays.asList("sink1", "sink2"), "dropSink");
        sinkProcessor.process();
        Mockito.verify(sink1, Mockito.times(1)).process();
        Mockito.verify(sink2, Mockito.never()).process();
        Mockito.verify(dropSink, Mockito.never()).process();
    }

    @Test
    public void testProcessWithThrow() throws EventDeliveryException {
        configure(ValvePositionInterface.Position.PROCESS, Arrays.asList("throwSink", "sink2"), "dropSink");
        InOrder inOrder = Mockito.inOrder(throwSink, sink2);

        sinkProcessor.process();

        inOrder.verify(throwSink).process();
        inOrder.verify(sink2).process();

        Mockito.verify(throwSink, Mockito.times(1)).process();
        Mockito.verify(sink2, Mockito.times(1)).process();
        Mockito.verify(dropSink, Mockito.never()).process();
    }

    @Test
    public void testDrop() throws EventDeliveryException {
        configure(ValvePositionInterface.Position.DROP, Arrays.asList("sink1", "sink2"), "dropSink");
        sinkProcessor.process();
        Mockito.verify(sink1, Mockito.never()).process();
        Mockito.verify(sink2, Mockito.never()).process();
        Mockito.verify(dropSink, Mockito.times(1)).process();
    }

    public void testDefer() throws EventDeliveryException {
        configure(ValvePositionInterface.Position.DEFER, Arrays.asList("sink1", "sink2"), "dropSink");
        Sink.Status status = sinkProcessor.process();
        Assert.assertEquals(Sink.Status.BACKOFF, status);
        Mockito.verify(sink1, Mockito.never()).process();
        Mockito.verify(sink2, Mockito.never()).process();
        Mockito.verify(dropSink, Mockito.never()).process();
    }
}
