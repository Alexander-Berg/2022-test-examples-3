package ru.yandex.market.mbo.flume.monitoring;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.flume.Context;
import org.apache.flume.instrumentation.ChannelCounter;
import org.assertj.core.util.Maps;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.http.MonitoringResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 06.03.2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ChannelSizeMonitoringTest {

    private static final String TEST_CHANNEL = "test_channel";
    private ChannelSizeMonitoring monitoring;
    private List<ObjectName> registeredBeans;

    @Before
    public void before() {
        monitoring = new ChannelSizeMonitoring();
        registeredBeans = new ArrayList<>();
    }

    @After
    public void after() throws MBeanRegistrationException, InstanceNotFoundException {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        for (ObjectName objectName : registeredBeans) {
            mBeanServer.unregisterMBean(objectName);
        }
    }

    @Test
    public void noMetricsFound() throws Exception {
        MonitoringResult result = monitoring.check();
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(MonitoringResult.Status.ERROR);
    }

    @Test
    public void notConfigured() throws Exception {
        registerCounter(TEST_CHANNEL);

        MonitoringResult result = monitoring.check();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(MonitoringResult.Status.ERROR);
        assertThat(result.getMessage()).contains("not configured");
    }

    @Test
    public void channelIsOk() throws Exception {
        registerCounter(TEST_CHANNEL);
        Context channelContext = new Context(Maps.newHashMap("criticalItemsCount", "5"));
        MonitoringConfigurationRegistry.registerMonitoringData(TEST_CHANNEL, channelContext);

        MonitoringResult result = monitoring.check();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(MonitoringResult.Status.OK);
    }

    @Test
    public void channelSizeReached() throws Exception {
        ChannelCounter counter = registerCounter(TEST_CHANNEL);
        Context channelContext = new Context(Maps.newHashMap("criticalItemsCount", "17"));
        MonitoringConfigurationRegistry.registerMonitoringData(TEST_CHANNEL, channelContext);

        counter.setChannelSize(17);

        MonitoringResult result = monitoring.check();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(MonitoringResult.Status.ERROR);
        assertThat(result.getMessage()).isEqualTo("test_channel size=17 (max 17)");
    }

    @Test
    public void channelSizeExceeds() throws Exception {
        ChannelCounter counter = registerCounter(TEST_CHANNEL);
        Context channelContext = new Context(Maps.newHashMap("criticalItemsCount", "17"));
        MonitoringConfigurationRegistry.registerMonitoringData(TEST_CHANNEL, channelContext);

        counter.setChannelSize(49);

        MonitoringResult result = monitoring.check();

        assertThat(result.getMessage()).isEqualTo("test_channel size=49 (max 17)");
    }

    private ChannelCounter registerCounter(String channelName) throws Exception {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName objectName = new ObjectName("org.apache.flume.channel:type=" + channelName);
        ChannelCounter counter = new ChannelCounter(channelName);
        mBeanServer.registerMBean(counter, objectName);
        registeredBeans.add(objectName);
        return counter;
    }

}
