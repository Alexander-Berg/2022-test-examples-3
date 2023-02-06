package ru.yandex.market.ir.autogeneration_api.util.http;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.yandex.market.ir.autogeneration_api.util.OkHttpResponseMock;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author danfertev
 * @since 08.08.2019
 */
public class HttpUtilsTest {
    private CloseableHttpClient httpClient;

    @Before
    public void setUp() {
        httpClient = Mockito.mock(CloseableHttpClient.class);
    }

    @Test
    public void testEncodeUrl() throws IOException {
        String path = "путь с пробелами и русскими символами";
        String url = "https://ya.ru/";

        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        when(httpClient.execute(Mockito.any())).thenReturn(new OkHttpResponseMock());

        HttpUtils.getContent(
            GetContentRequest.create(url + path, null, httpClient).setMaxSizeInBytes(10 * 1024 * 1024)
        );

        verify(httpClient, Mockito.times(1)).execute(requestCaptor.capture());

        HttpUriRequest request = requestCaptor.getValue();
        assertThat(request.getURI().toASCIIString()).isEqualTo(url + URIUtil.encodePath(path));
    }

    @Test
    public void testAlreadyEncodedUrl() throws IOException {
        String path = "path%20encoded";
        String url = "https://ya.ru/";

        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        when(httpClient.execute(Mockito.any())).thenReturn(new OkHttpResponseMock());

        HttpUtils.getContent(
            GetContentRequest.create(url + path, null, httpClient).setMaxSizeInBytes(10 * 1024 * 1024)
        );

        verify(httpClient, Mockito.times(1)).execute(requestCaptor.capture());

        HttpUriRequest request = requestCaptor.getValue();
        assertThat(request.getURI().toASCIIString()).isEqualTo(url + path);
    }

    @Test
    public void testUrlWithoutProtocol() throws IOException {
        String url = "//ya.ru/path";

        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        when(httpClient.execute(Mockito.any())).thenReturn(new OkHttpResponseMock());

        HttpUtils.getContent(
                GetContentRequest.create(url, null, httpClient).setMaxSizeInBytes(10 * 1024 * 1024)
        );

        verify(httpClient, Mockito.times(1)).execute(requestCaptor.capture());

        HttpUriRequest request = requestCaptor.getValue();
        assertThat(request.getURI().toASCIIString()).isEqualTo("http:" + url);
    }

    @Test
    public void testCheckForInternalLink() {
        String internalLink = "http://market-ir-prod.s3.mds.yandex.net/file";
        String internalLink2 = "market-ir-prod.s3.mds.yandex.net/file";
        String internalLink3 = "www.market-ir-prod.s3.mds.yandex.net/file";
        String internalLink4 = "http://downloader.disk.yandex.ru/disk/xxx";
        String internalLink5 = "http://www.downloader.disk.yandex.ru/disk/xxx";
        String internalLink6 = "downloader.disk.yandex.ru/disk/xxx";
        String internalLink7 = "www.downloader.disk.yandex.ru/disk/xxx";

        String externalLink = "http://market-ir-prod.s3.mds.yandex.net.ru/file";
        String externalLink2 = "https://ya.ru/file";
        String externalLink3 = "https://ucoz.com/mds.yandex.net/file";
        String externalLink4 = "http://xxx.downloader.disk.yandex.ru/disk/xxx";

        Assert.assertTrue(HttpUtils.checkForInternalLink(internalLink));
        Assert.assertTrue(HttpUtils.checkForInternalLink(internalLink2));
        Assert.assertTrue(HttpUtils.checkForInternalLink(internalLink3));
        Assert.assertTrue(HttpUtils.checkForInternalLink(internalLink4));
        Assert.assertTrue(HttpUtils.checkForInternalLink(internalLink5));
        Assert.assertTrue(HttpUtils.checkForInternalLink(internalLink6));
        Assert.assertTrue(HttpUtils.checkForInternalLink(internalLink7));

        Assert.assertFalse(HttpUtils.checkForInternalLink(externalLink));
        Assert.assertFalse(HttpUtils.checkForInternalLink(externalLink2));
        Assert.assertFalse(HttpUtils.checkForInternalLink(externalLink3));
        Assert.assertFalse(HttpUtils.checkForInternalLink(externalLink4));

    }

    @Test
    public void testConvertUriWithPortShouldNotLoseIt() throws URISyntaxException, URIException, MalformedURLException {
        String urlWithPort = "http://test.ru:25606/WEBSERVER/5.jpg";
        URI uri = HttpUtils.convertUrl(urlWithPort);
        Assert.assertEquals(urlWithPort, uri.toString());
    }

    @Test
    public void testConvertUriWithoutPortShouldNotFail() throws URISyntaxException, URIException, MalformedURLException {
        String urlWithNoPort = "http://test.ru/WEBSERVER/5.jpg";
        URI uri = HttpUtils.convertUrl(urlWithNoPort);
        Assert.assertEquals(urlWithNoPort, uri.toString());
    }
}