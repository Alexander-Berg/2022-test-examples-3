package ru.yandex.http.util.client;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;

import ru.yandex.http.config.ImmutableDnsConfig;
import ru.yandex.http.config.ImmutableHttpTargetConfig;
import ru.yandex.http.test.HttpClientTest;
import ru.yandex.http.util.BadResponseException;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.io.GenericCloseableAdapter;

public abstract class CloseableHttpClientTestBase
    extends HttpClientTest<GenericCloseableAdapter<CloseableHttpClient>>
{
    @Override
    protected GenericCloseableAdapter<CloseableHttpClient> createClient(
        final ImmutableHttpTargetConfig backendConfig,
        final ImmutableDnsConfig dnsConfig)
        throws Exception
    {
        return new GenericCloseableAdapter<>(
            ClientBuilder.createClient(backendConfig, dnsConfig));
    }

    @Override
    protected void sendRequest(
        final GenericCloseableAdapter<CloseableHttpClient> client,
        final int expectedStatus,
        final HttpUriRequest request)
        throws Exception
    {
        try (CloseableHttpResponse response = client.get().execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            if (status != expectedStatus) {
                throw new BadResponseException(request, response);
            }
            CharsetUtils.consume(response.getEntity());
        }
    }
}

