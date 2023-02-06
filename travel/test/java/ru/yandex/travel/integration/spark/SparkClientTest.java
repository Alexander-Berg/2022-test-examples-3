package ru.yandex.travel.integration.spark;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import org.asynchttpclient.Response;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;
import ru.yandex.travel.integration.spark.responses.CompanyShortReportResponse;
import ru.yandex.travel.integration.spark.responses.EntrepreneurShortReportResponse;
import ru.yandex.travel.testing.misc.TestResources;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SparkClientTest {

    private SparkClient sparkClient;
    private AsyncHttpClientWrapper ahcWrapper;

    @Before
    public void setup() {
        SparkClientProperties properties = SparkClientProperties.builder()
                .baseUrl("https://url")
                .httpReadTimeout(Duration.ofSeconds(10))
                .httpRequestTimeout(Duration.ofSeconds(10))
                .login("login")
                .password("password")
                .sessionTtl(Duration.ofMinutes(55))
                .build();
        ahcWrapper = mock(AsyncHttpClientWrapper.class);
        sparkClient = new SparkClient(ahcWrapper, properties);
    }

    @Test
    public void getCompanyShortReport() {
        mockAuth();
        mockResponse("GetCompanyShortReport", "GetCompanyShortReportResponse.xml");
        CompanyShortReportResponse reportResponse = sparkClient.getCompanyShortReportSync("123");
        Assert.assertEquals("7802742850", reportResponse.getData().getReport().getINN());
        Assert.assertEquals("1117847075158", reportResponse.getData().getReport().getOGRN());
    }

    @Test
    public void getEntrepreneurShortReport() {
        mockAuth();
        mockResponse("GetEntrepreneurShortReport", "GetEntrepreneurShortReportResponse.xml");
        EntrepreneurShortReportResponse reportResponse = sparkClient.getEntrepreneurShortReportSync("123");
        Assert.assertEquals("381101250840", reportResponse.getData().getReport().getINN());
        Assert.assertEquals("318385000034151", reportResponse.getData().getReport().getOGRNIP());
    }

    private void mockAuth() {
        String response = TestResources.readResource("authResponse.xml");
        when(ahcWrapper.executeRequest(any(), eq("auth"))).thenAnswer(invocation -> {
            Response authResponse = mock(Response.class);
            when(authResponse.getStatusCode()).thenReturn(200);
            when(authResponse.getResponseBody()).thenReturn(response);
            DefaultHttpHeaders httpHeaders = new DefaultHttpHeaders();
            httpHeaders.add("Set-Cookie", "ASP.NET_SessionId=123");
            when(authResponse.getHeaders()).thenReturn(httpHeaders);
            return CompletableFuture.completedFuture(authResponse);
        });
    }

    private void mockResponse(String purpose, String responseResource) {
        String response = TestResources.readResource(responseResource);
        when(ahcWrapper.executeRequest(any(), eq(purpose))).thenAnswer(invocation -> {
            Response testRsp = Mockito.mock(Response.class);
            when(testRsp.getStatusCode()).thenReturn(200);
            when(testRsp.getResponseBody()).thenReturn(response);
            when(testRsp.getHeaders()).thenReturn(EmptyHttpHeaders.INSTANCE);
            return CompletableFuture.completedFuture(testRsp);
        });
    }

}
