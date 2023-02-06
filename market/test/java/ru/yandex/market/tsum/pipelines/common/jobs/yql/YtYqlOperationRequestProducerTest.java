package ru.yandex.market.tsum.pipelines.common.jobs.yql;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.clients.yql.ClusterType;
import ru.yandex.market.tsum.clients.yql.ProgramSyntax;
import ru.yandex.market.tsum.clients.yql.YqlAction;
import ru.yandex.market.tsum.clients.yql.YqlApiClient;
import ru.yandex.market.tsum.clients.yql.YqlOperationRequest;
import ru.yandex.market.tsum.clients.yql.YqlQueryResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class YtYqlOperationRequestProducerTest {

    private YqlApiClient yqlApiClient;
    private YtYqlOperationRequestProducer producer;

    private static final String OPERATION_ID = "operation_1";
    private static final String OPERATION_ID_CONTENT = "SELECT 1 from operation_id_1";
    private static final String CONTENT = "SELECT 1";
    private static final String CLUSTER = "hahn";
    private static final ClusterType CLUSTER_TYPE = ClusterType.KIKIMR;
    private static final Map<String, Object> PARAMS = new HashMap<>() {{
        put("p1", "param1");
        put("p2", "param2");
    }};

    @Before
    public void setUp() throws Exception {
        yqlApiClient = Mockito.mock(YqlApiClient.class);
        producer = new YtYqlOperationRequestProducer(yqlApiClient) {
            @Override
            protected ClusterType getClusterType() {
                return CLUSTER_TYPE;
            }

            @Override
            protected String getCluster() {
                return CLUSTER;
            }
        };
    }

    @Test
    public void testSqlV1ContentRequest() {
        YqlScriptResource sqlv1 = new YqlScriptResource(null, CONTENT, 1000, "SQLv1");
        YqlOperationRequest request = producer.createRequest(sqlv1, PARAMS);

        assertEquals(YqlAction.RUN, request.getAction());
        assertEquals(ProgramSyntax.SQLv1, request.getType());
        assertEquals(CONTENT, request.getContent());
        assertEquals(PARAMS.size(), request.getParameters().size());
        assertNull(request.getQueryId());
        assertEquals(CLUSTER, request.getCluster());
        assertEquals(CLUSTER_TYPE, request.getClusterType());
    }

    @Test
    public void testSqlV1OperationRequest() {
        YqlScriptResource sqlv1 = new YqlScriptResource(OPERATION_ID, CONTENT, 1000, "SQLv1");
        when(yqlApiClient.getQuery(OPERATION_ID)).thenReturn(new YqlQueryResponse("",
            new YqlQueryResponse.Data(OPERATION_ID_CONTENT)));

        YqlOperationRequest request = producer.createRequest(sqlv1, PARAMS);

        assertEquals(YqlAction.RUN, request.getAction());
        assertEquals(ProgramSyntax.SQLv1, request.getType());
        assertEquals(OPERATION_ID, request.getQueryId());
        assertEquals(OPERATION_ID_CONTENT, request.getContent());
        assertEquals(PARAMS.size(), request.getParameters().size());
        assertEquals(CLUSTER, request.getCluster());
        assertEquals(CLUSTER_TYPE, request.getClusterType());
    }

    @Test
    public void testNullSyntax() {
        YqlScriptResource sqlv1 = new YqlScriptResource(null, CONTENT, 1000, null);
        YqlOperationRequest request = producer.createRequest(sqlv1, PARAMS);

        assertEquals(YqlAction.RUN, request.getAction());
        assertEquals(ProgramSyntax.SQLv1, request.getType());
        assertNull(request.getQueryId());
        assertEquals(CONTENT, request.getContent());
        assertEquals(PARAMS.size(), request.getParameters().size());
    }
}
