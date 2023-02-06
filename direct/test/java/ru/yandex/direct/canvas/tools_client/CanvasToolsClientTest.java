package ru.yandex.direct.canvas.tools_client;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.canvas.model.OnCreativeOperationResult;
import ru.yandex.direct.canvas.model.OnCreativeOperationResultStatus;
import ru.yandex.direct.http.smart.core.Call;
import ru.yandex.direct.http.smart.core.Smart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestingConfiguration.class})
public class CanvasToolsClientTest {
    private MockWebServer mockWebServer;
    private CanvasToolsClient.Api api;

    private final static Logger logger = LoggerFactory.getLogger(CanvasToolsClientTest.class);

    private Set<Long> request;

    private MockResponse response = new MockResponse().setBody("{\n" +
            "\"123\" :   {\"status\" : \"OK\"},\n" +
            "\"4455\" :  {\"status\" : \"ERROR\", \"errors\" : [\"Something went wrong\", \"Twice\"]},\n" +
            "\"888\" :   {\"status\" : \"OK\"}\n" +
            "}\n");

    @Autowired
    public Smart.Builder builder;

    @Before
    public void setUp() throws IOException {
        // тестировать проще, когда порядок зафиксирован, не нужно разворчивать json обратно в список
        request = new TreeSet(Arrays.asList(123L, 888L, 4455L));
        mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                return response;
            }
        });
        mockWebServer.start();
        api = builder().build().create(CanvasToolsClient.Api.class);
    }

    @Test
    public void sendToDirect() throws Exception {
        callAndCheckResult(api.sendToDirect(request));
        RecordedRequest madeRequest = mockWebServer.takeRequest();
        assertEquals("/tools/send_to_direct", madeRequest.getPath());
        checkRequest(madeRequest);
    }

    @Test
    public void sendToRtbHost() throws Exception {
        callAndCheckResult(api.sendToRtbHost(request));
        RecordedRequest madeRequest = mockWebServer.takeRequest();
        assertEquals("/tools/send_to_rtbhost", madeRequest.getPath());
        checkRequest(madeRequest);
    }

    @Test
    public void reshootScreenshot() throws Exception {
        callAndCheckResult(api.reshootScreenshot(request));
        RecordedRequest madeRequest = mockWebServer.takeRequest();
        assertEquals("/tools/reshoot_screenshot", madeRequest.getPath());
        checkRequest(madeRequest);
    }

    @Test
    public void rebuild() throws Exception {
        callAndCheckResult(api.rebuild(request));
        RecordedRequest madeRequest = mockWebServer.takeRequest();
        assertEquals("/tools/rebuild", madeRequest.getPath());
        checkRequest(madeRequest);
    }

    private void callAndCheckResult(Call<Map<Long, OnCreativeOperationResult>> call) {
        Map<Long, OnCreativeOperationResult> result = call.execute().getSuccess();
        assertEquals(result.keySet().size(), 3);
        assertTrue(result.keySet().containsAll(List.of(123L, 888L, 4455L)));
        assertEquals(result.get(123L).getStatus(), OnCreativeOperationResultStatus.OK);
        assertEquals(result.get(123L).getErrors(), null);
        assertEquals(result.get(888L).getStatus(), OnCreativeOperationResultStatus.OK);
        assertEquals(result.get(4455L).getStatus(), OnCreativeOperationResultStatus.ERROR);
        assertEquals(result.get(4455L).getErrors().size(), 2);
        assertEquals(result.get(4455L).getErrors().get(0), "Something went wrong");
        assertEquals(result.get(4455L).getErrors().get(1), "Twice");
    }

    private void checkRequest(RecordedRequest r) {
        assertEquals(r.getMethod(), "POST");
        assertEquals(r.getHeader("Content-Type"), "application/json");
        assertEquals("[123, 888, 4455]", r.getBody().readUtf8());
    }

    protected Smart.Builder builder() {
        return builder.withBaseUrl("http://" + mockWebServer.getHostName() + ":" + mockWebServer.getPort());
    }

    @After
    public void tearDown() {
        try {
            mockWebServer.shutdown();
        } catch (Exception e) {
            logger.warn("cannot shut down mockWebServer", e);
        }
    }
}
