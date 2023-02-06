package ru.yandex.market.blue_banner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;

import okhttp3.RequestBody;
import okio.Buffer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import retrofit2.Response;
import retrofit2.mock.Calls;

import ru.yandex.market.common.bunker.BunkerWritingApi;
import ru.yandex.market.common.bunker.loader.BunkerLoader;
import ru.yandex.market.shop.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.bunker.BunkerService.Version.LATEST;

public class RemoveOldTemplateBannersExecutorTest extends FunctionalTest {

    private static final String BUNKER_NODE = "/market-mbi/template-banners-functional-test";
    private static final String BUNKER_MIME = "application/json; charset=utf-8; " +
            "schema=\"bunker:/market-mbi/.schema/template-banners#\"";

    @Autowired
    private BunkerLoader bunkerLoader;

    @Autowired
    private BunkerWritingApi bunkerWritingApi;

    @Autowired
    private Clock clock;

    @Autowired
    private RemoveOldTemplateBannersExecutor executor;

    @Test
    void testRemoveOldBanners() throws IOException {
        when(clock.instant()).thenReturn(Instant.parse("2022-02-02T00:00:00.00Z"));

        when(bunkerLoader.getListOfChildNodesStream(BUNKER_NODE, LATEST)).thenReturn(getClass()
                .getResourceAsStream("RemoveOldTemplateBannersExecutorTest.bunkerNodes.before.json"));

        mockBunkerNode("without_old_banners",
                "RemoveOldTemplateBannersExecutorTest.bunkerNode.without_old_banners.before.json");
        mockBunkerNode("with_old_banners",
                "RemoveOldTemplateBannersExecutorTest.bunkerNode.with_old_banners.before.json");

        ArgumentCaptor<RequestBody> nodeCaptor = ArgumentCaptor.forClass(RequestBody.class);
        ArgumentCaptor<RequestBody> mimeCaptor = ArgumentCaptor.forClass(RequestBody.class);
        when(bunkerWritingApi.store(any(),
                any(),
                nodeCaptor.capture(),
                mimeCaptor.capture())
        ).thenReturn(Calls.response(Response.success(null)));

        executor.doJob(null);

        JSONAssert.assertEquals(
                IOUtils.toString(
                        getClass().getResourceAsStream("RemoveOldTemplateBannersExecutorTest.bunkerNode.with_old_banners.after.json"),
                        StandardCharsets.UTF_8),
                requestBodyToString(nodeCaptor.getValue()),
                false
        );
        assertThat(requestBodyToString(mimeCaptor.getValue())).isEqualTo(BUNKER_MIME);
    }

    private String requestBodyToString(RequestBody requestBody) throws IOException {
        Buffer buffer = new Buffer();
        requestBody.writeTo(buffer);
        return buffer.readUtf8();
    }

    private void mockBunkerNode(String nodeName, String file) throws IOException {
        when(bunkerLoader.getNodeStream(BUNKER_NODE + "/" + nodeName, LATEST))
                .thenReturn(getClass().getResourceAsStream(file))
                .thenReturn(getClass().getResourceAsStream(file));
    }
}
