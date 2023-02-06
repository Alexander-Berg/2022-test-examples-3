package ru.yandex.chemodan.app.orchestrator.cloud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.misc.ExceptionUtils;
import ru.yandex.misc.ip.HostPort;
import ru.yandex.misc.test.Assert;

/**
 * @author yashunsky
 */
public class ControlAgentClientTest {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void listContainers() {
        String body = "[\"office_jbdymqgdluzynr7i.sas_1\",\"office_jbdymqgdluzynr7i.sas_2\"]";
        ListF<String> containers = ControlAgentClient.onListContainers(parseBody(body));

        Assert.equals(Cf.list("office_jbdymqgdluzynr7i.sas_1", "office_jbdymqgdluzynr7i.sas_2"), containers);
    }

    @Test
    public void listEndpoints() {
        String body = "{\"jbdymqgdluzynr7i.sas.yp-c.yandex.net:801\":\"office_jbdymqgdluzynr7i.sas_1\"," +
                "\"jbdymqgdluzynr7i.sas.yp-c.yandex.net:802\":\"office_jbdymqgdluzynr7i.sas_2\"}";
        ListF<ContainerHostPortPojo> containers = ControlAgentClient.onListEndpoints(parseBody(body));

        ListF<ContainerHostPortPojo> expected = Cf.list(
                new ContainerHostPortPojo(new HostPort("jbdymqgdluzynr7i.sas.yp-c.yandex.net", 801),
                        "office_jbdymqgdluzynr7i.sas_1"),
                new ContainerHostPortPojo(new HostPort("jbdymqgdluzynr7i.sas.yp-c.yandex.net", 802),
                        "office_jbdymqgdluzynr7i.sas_2")
        );

        Assert.equals(expected, containers);
    }

    @Test
    public void containerState() {
        String body = "{\"office\":\"OK\", \"state\":\"running\", \"port\": 802}";
        ContainerStateAndPortPojo stateAndPort = ControlAgentClient.onContainerState(parseBody(body), "office");

        Assert.equals(new ContainerStateAndPortPojo(ContainerNodeState.OK, ContainerPortoState.RUNNING, 802), stateAndPort);
    }

    @Test
    public void delete() {
        String body = "{\"state\":\"destroyed\"}";
        Assert.isTrue(ControlAgentClient.onDeleteContainer(parseBody(body)));
    }

    @Test
    public void containerSuccessfulCreation() {
        String body = "{\"name\":\"office_jbdymqgdluzynr7i.sas_2\", \"port\": 802}";
        Option<ContainerHostPortPojo> hostPort = ControlAgentClient.onCreateContainer("host", parseBody(body));

        ContainerHostPortPojo expected = new ContainerHostPortPojo(
                new HostPort("host", 802), "office_jbdymqgdluzynr7i.sas_2");

        Assert.some(expected, hostPort);
    }

    @Test
    public void containerFailedCreation() {
        String body = "{\"state\":\"full\"}";
        Option<ContainerHostPortPojo> hostPort = ControlAgentClient.onCreateContainer("host", parseBody(body));

        Assert.isEmpty(hostPort);
    }

    private JsonNode parseBody(String body) {
        try {
            return mapper.readValue(body, JsonNode.class);
        } catch (Exception e) {
            throw ExceptionUtils.translate(e);
        }
    }
}
