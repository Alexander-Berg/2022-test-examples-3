package ru.yandex.market.mbo.flume.sinkprocessors;

import java.util.Arrays;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.mbo.flume.Register;
import ru.yandex.market.mbo.flume.sinkprocessors.ValvePositionInterface.Position;
import ru.yandex.market.mbo.flume.zookeeper.ZooKeeperException;
import ru.yandex.market.mbo.flume.zookeeper.ZooKeeperService;

/**
 * @author moskovkin@yandex-team.ru
 * @since 19.01.18
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ValveZkPositionTest {

    private static final Long TEST_ZK_CHECK_INTERVAL_MS = 1000L;
    public static final String TEST_PARENT_PATH = "/mbo/flume";
    public static final String TEST_VALVE_PATH = TEST_PARENT_PATH + "/" + ValvePositionZkImpl.VALVE_NODE_NAME;
    public static final String TEST_LOCAL_NODE_NAME = "localValve";
    public static final String TEST_LOCAL_VALVE_PATH = TEST_PARENT_PATH + "/" + TEST_LOCAL_NODE_NAME;

    @Mock
    private ZooKeeperService zkService;
    private ValvePositionZkImpl position;

    @Before
    public void init() throws ZooKeeperException {
        System.setProperty(ValvePositionZkImpl.ZK_HOSTS_PARAM, "unexisting zk host");
        System.setProperty(ValvePositionZkImpl.PARENT_ZK_NODE_PARAM, "unexisting zk node");
        System.setProperty(ValvePositionZkImpl.ZK_CHECK_INTERVAL_PARAM, TEST_ZK_CHECK_INTERVAL_MS.toString());

        Mockito.doAnswer(invocation -> TEST_PARENT_PATH + "/" + Arrays.stream(invocation.getArguments())
                .map(Object::toString)
                .collect(Collectors.joining("/"))
        )
                .when(zkService)
                .pathBy(Mockito.any());

        Mockito.doAnswer(invocation -> TEST_PARENT_PATH)
                .when(zkService)
                .getParentNode();

        Mockito.doAnswer(invocation -> Position.PROCESS.toString())
                .when(zkService)
                .read(Mockito.eq(TEST_VALVE_PATH));

        Register.put(Register.ZK_SERVICE_KEY, zkService);
        position = new ValvePositionZkImpl(TEST_LOCAL_NODE_NAME, zkService);
    }

    @Test
    public void testValveNodePath() throws Throwable {
        Mockito.doAnswer(invocation -> true)
                .when(zkService)
                .exists(TEST_LOCAL_VALVE_PATH);

        position.getPosition();
        Mockito.verify(zkService, Mockito.times(1)).read(Mockito.eq(TEST_VALVE_PATH));
        Mockito.verify(zkService, Mockito.times(1)).exists(Mockito.eq(TEST_LOCAL_VALVE_PATH));
        Mockito.verify(zkService, Mockito.times(1)).read(Mockito.eq(TEST_LOCAL_VALVE_PATH));
    }

    @Test
    public void testLocalValveNodePath() throws Throwable {
        checkLocalOverride(Position.PROCESS, null, Position.PROCESS);
        checkLocalOverride(Position.DEFER, null, Position.DEFER);
        checkLocalOverride(Position.DROP, null, Position.DROP);

        checkLocalOverride(Position.PROCESS, Position.DEFER, Position.DEFER);
        checkLocalOverride(Position.PROCESS, Position.PROCESS, Position.PROCESS);
        checkLocalOverride(Position.PROCESS, Position.DROP, Position.DROP);

        checkLocalOverride(Position.DEFER, Position.PROCESS, Position.DEFER);
        checkLocalOverride(Position.DEFER, Position.DEFER, Position.DEFER);
        checkLocalOverride(Position.DEFER, Position.DROP, Position.DEFER);

        checkLocalOverride(Position.DROP, Position.PROCESS, Position.DROP);
        checkLocalOverride(Position.DROP, Position.DEFER, Position.DROP);
        checkLocalOverride(Position.DROP, Position.DROP, Position.DROP);
    }

    private void checkLocalOverride(Position valvePosition,
                                    @Nullable Position localValvePosition,
                                    Position expectedResult) throws ZooKeeperException {

        System.setProperty(ValvePositionZkImpl.ZK_CHECK_INTERVAL_PARAM, "0");
        position = new ValvePositionZkImpl(TEST_LOCAL_NODE_NAME, zkService);

        Mockito.doAnswer(invocation -> valvePosition.toString())
                .when(zkService)
                .read(Mockito.eq(TEST_VALVE_PATH));

        Mockito.doAnswer(invocation -> localValvePosition != null)
                .when(zkService)
                .exists(TEST_LOCAL_VALVE_PATH);

        if (localValvePosition != null) {
            Mockito.doAnswer(invocation -> localValvePosition.toString())
                    .when(zkService)
                    .read(Mockito.eq(TEST_LOCAL_VALVE_PATH));
        }

        Position resultingPosition = position.getPosition();
        Assert.assertEquals(expectedResult, resultingPosition);
    }

    @Test
    public void testCheckSmallInterval() throws Throwable {
        position.getPosition();
        Thread.sleep(TEST_ZK_CHECK_INTERVAL_MS - TEST_ZK_CHECK_INTERVAL_MS / 2);
        position.getPosition();

        Mockito.verify(zkService, Mockito.times(1))
                .read(Mockito.eq(TEST_VALVE_PATH));
    }

    @Test
    public void testCheckBigInterval() throws Throwable {
        position.getPosition();
        Thread.sleep(TEST_ZK_CHECK_INTERVAL_MS + TEST_ZK_CHECK_INTERVAL_MS / 2);
        position.getPosition();

        Mockito.verify(zkService, Mockito.times(2))
                .read(Mockito.eq(TEST_VALVE_PATH));
    }
}
