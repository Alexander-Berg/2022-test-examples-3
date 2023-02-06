package ru.yandex.direct.bannercategoriesmultik.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.apache.commons.lang3.ArrayUtils;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.rules.ExternalResource;

import ru.yandex.direct.asynchttp.FetcherSettings;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.utils.io.ResourceUtils;

public class MockedMultik extends ExternalResource {

    private MockWebServer server;
    private final byte[] reqPrefix = "POST:/yabs_runtime_multiks:".getBytes();
    private final byte[] multikRequest =
            ArrayUtils.addAll(reqPrefix, ResourceUtils.readResourceBytes("request.bin"));
    private final byte[] multikResponse = ResourceUtils.readResourceBytes("response.bin");

    private byte[] readFile(File file) {
        try {
            return new FileInputStream(file).readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    protected void before() throws Throwable {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                byte[] req = ArrayUtils.addAll(reqPrefix, request.getBody().readByteArray());

                if (Arrays.equals(req, multikRequest)) {
                    return new MockResponse().setBody(new Buffer().write(multikResponse));
                } else {
                    return new MockResponse().setResponseCode(404).setBody("Request not supported");
                }
            }
        });
        server.start();
    }

    @Override
    protected void after() {
        try {
            server.shutdown();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getBaseUrl() {
        return server.url("/").toString();
    }

    public BannerCategoriesMultikClient createClient() {
        return new BannerCategoriesMultikClient(
                getBaseUrl(),
                new ParallelFetcherFactory(new DefaultAsyncHttpClient(), new FetcherSettings()));
    }
}
