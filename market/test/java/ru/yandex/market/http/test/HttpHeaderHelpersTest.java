package ru.yandex.market.http.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.common.Result;
import ru.yandex.market.http.HttpHeaderHelpers;
import ru.yandex.market.http.HttpResponse;

/**
 * Created by yntv on 10/19/16.
 */
public class HttpHeaderHelpersTest {

    @Test
    public void contentTypeExist_waitValue() {
        HttpResponse response = getHttpResponseForContentType("image/jpg");
        assertResultOk("image/jpg", HttpHeaderHelpers.getContentType(response));
    }

    @Test
    public void emptyFileName() {
        HttpResponse response = getHttpResponseForContentDisposition("attachment;filename=");
        assertError(HttpHeaderHelpers.getFileName(response));
    }

    @Test
    public void ignoreCase() {
        HttpResponse response = getHttpResponseForContentDisposition("attachment;fileName=");
        assertError(HttpHeaderHelpers.getFileName(response));
    }

    @Test
    public void noContentType_WaitError() {
        HttpResponse response = getHttpResponseForContentType(null);
        assertError(HttpHeaderHelpers.getContentType(response), "content type");
    }

    @Test
    public void noFileNameInfo() {
        HttpResponse response = getHttpResponseForContentDisposition("attachment");
        assertError(HttpHeaderHelpers.getFileName(response));
    }

    @Test
    public void trashHeaderFormat() {
        String contentDispositionHeaderValue = "trash";
        HttpResponse response = getHttpResponseForContentDisposition(contentDispositionHeaderValue);
        assertError(HttpHeaderHelpers.getFileName(response), contentDispositionHeaderValue);
    }

    @Test
    public void validHeader_waitExtractFileName() {
        HttpResponse response = getHttpResponseForContentDisposition("attachment; filename=hello.txt");
        assertResultOk("hello.txt", HttpHeaderHelpers.getFileName(response));
    }

    private void assertError(Result<String, String> result) {
        Assert.assertTrue(result.hasError());
    }

    private void assertError(Result<String, String> result, String header) {
        assertError(result);
        Assert.assertTrue("ReferenceServiceError must contain header value", result.getError().contains(header));
    }

    private void assertResultOk(String expectedFileName, Result<String, String> result) {
        Assert.assertTrue(result.isOk());
        Assert.assertEquals(expectedFileName, result.getValue());
    }

    @NotNull
    private HttpResponse getHttpResponse(String headerName, String headerValue) {
        List<Map.Entry<String, String>> headers = new ArrayList<>();
        if (headerName != null && !headerName.isEmpty()) {
            Map.Entry<String,String> entry = Maps.immutableEntry(headerName, headerValue);
            headers.add(entry);
        }
        return HttpResponse.of(200, headers, new byte[0]);
    }

    @NotNull
    private HttpResponse getHttpResponseForContentDisposition(String contentDispositionHeaderValue) {
        return getHttpResponse("Content-Disposition", contentDispositionHeaderValue);
    }

    @NotNull
    private HttpResponse getHttpResponseForContentType(String contentType) {
        return getHttpResponse("Content-Type", contentType);
    }
}
