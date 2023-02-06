package ru.yandex.market.tsum.pipelines.common.jobs.yql;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.tsum.clients.yql.ProgramSyntax;
import ru.yandex.market.tsum.clients.yql.YqlAction;
import ru.yandex.market.tsum.clients.yql.YqlOperationRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ClickhouseYqlOperationRequestProducerTest {

    private YqlOperationRequestProducer producer;

    @Before
    public void setUp() throws Exception {
        producer = new ClickhouseYqlOperationRequestProducer();
    }

    @Test
    public void testClickhouseRequest() {
        YqlScriptResource req = new YqlScriptResource(null, "SELECT ${p1} FROM table", 1000,
            ProgramSyntax.CLICKHOUSE.name());
        YqlOperationRequest request = producer.createRequest(req, new HashMap<>() {{
            put("p1", "param1");
        }});

        assertEquals(YqlAction.RUN, request.getAction());
        assertEquals(ProgramSyntax.CLICKHOUSE, request.getType());
        assertEquals("SELECT param1 FROM table", request.getContent());
        assertNull(request.getParameters());
        assertNull(request.getQueryId());
        assertNull(request.getCluster());
        assertNull(request.getClusterType());
    }
}
