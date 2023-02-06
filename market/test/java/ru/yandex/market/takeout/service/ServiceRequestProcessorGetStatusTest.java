package ru.yandex.market.takeout.service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.jayway.jsonpath.PathNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.takeout.common.TakeoutAsyncHttpClient;
import ru.yandex.market.takeout.config.Delete;
import ru.yandex.market.takeout.config.ServiceDescription;
import ru.yandex.market.takeout.config.Status;

public class ServiceRequestProcessorGetStatusTest extends RequestProcessorTestCase {
    public void testGetEmptyStatusWithoutRemap() {

    }

    public void testGetExceptionalStatus() {
        String jsonPath = "$.length()";
        ServiceDescription serviceDescription = getServiceDescription(jsonPath, null);

        CompletableFuture<List<String>> future = getStatus(serviceDescription, EXCEPTIONAL_HTTP_CLIENT);
        Assert.assertThrows(TakeoutRequestProcessorTestCase.HttpException.class,
                () -> unwrapExecutionException(future));
    }

    public void testGetSuccessfulEmptyStatusArray() throws Exception {
        String jsonPath = "$.length()";
        ServiceDescription serviceDescription = getServiceDescription(jsonPath, "type");

        TakeoutAsyncHttpClient successfulHttpClient = getSuccessfulHttpClient("[]");
        CompletableFuture<List<String>> future = getStatus(serviceDescription, successfulHttpClient);
        List<String> strings = future.get();
        Assert.assertEquals(Collections.emptyList(), strings);
    }

    public void testGetSuccessfulNotEmptyStatusArray() throws Exception {
        String jsonPath = "$.length()";
        ServiceDescription serviceDescription = getServiceDescription(jsonPath, "type");

        TakeoutAsyncHttpClient successfulHttpClient = getSuccessfulHttpClient("[{\"value\":1}]");
        CompletableFuture<List<String>> future = getStatus(serviceDescription, successfulHttpClient);
        List<String> strings = future.get();
        Assert.assertEquals(Collections.singletonList("type"), strings);
    }

    public void testGetSuccessfulNotEmptyStatusArrays() throws Exception {
        String jsonPath = "sum($[*].totalCount)";
        ServiceDescription serviceDescription = getServiceDescription(jsonPath, "type");

        TakeoutAsyncHttpClient successfulHttpClient = getSuccessfulHttpClient("[{\"id\":-1,\"items\":[]," +
                "\"listType\":\"BASKET\",\"totalCount\":1}]");
        CompletableFuture<List<String>> future = getStatus(serviceDescription, successfulHttpClient);
        List<String> strings = future.get();
        Assert.assertEquals(Collections.singletonList("type"), strings);
    }

    public void testGetSuccessfulEmptyStatusArrays() throws Exception {
        String jsonPath = "sum($[*].totalCount)";
        ServiceDescription serviceDescription = getServiceDescription(jsonPath, "type");

        TakeoutAsyncHttpClient successfulHttpClient = getSuccessfulHttpClient("[{\"id\":-1,\"items\":[]," +
                "\"listType\":\"BASKET\",\"totalCount\":0}]");
        CompletableFuture<List<String>> future = getStatus(serviceDescription, successfulHttpClient);
        List<String> strings = future.get();
        Assert.assertEquals(Collections.emptyList(), strings);
    }

    public void testGetSuccessfulEmptyStatus() throws Exception {
        String jsonPath = "$.count";
        ServiceDescription serviceDescription = getServiceDescription(jsonPath, "type");

        TakeoutAsyncHttpClient successfulHttpClient = getSuccessfulHttpClient("{\"count\":0}");
        CompletableFuture<List<String>> future = getStatus(serviceDescription, successfulHttpClient);
        List<String> strings = future.get();
        Assert.assertEquals(Collections.emptyList(), strings);
    }

    public void testGetSuccessfulNotEmptyStatus() throws Exception {
        String jsonPath = "$.count";
        ServiceDescription serviceDescription = getServiceDescription(jsonPath, "type");

        TakeoutAsyncHttpClient successfulHttpClient = getSuccessfulHttpClient("{\"count\":1}");
        CompletableFuture<List<String>> future = getStatus(serviceDescription, successfulHttpClient);
        List<String> strings = future.get();
        Assert.assertEquals(Collections.singletonList("type"), strings);
    }

    public void testGetSuccessfulMalformedArrayStatus() {
        String jsonPath = "$.length()";
        ServiceDescription serviceDescription = getServiceDescription(jsonPath, "type");

        TakeoutAsyncHttpClient successfulHttpClient = getSuccessfulHttpClient("\"ERROR\"");
        CompletableFuture<List<String>> future = getStatus(serviceDescription, successfulHttpClient);
        Assert.assertThrows(NullPointerException.class, () -> unwrapExecutionException(future));
    }

    public void testGetSuccessfulMalformedStatus() {
        String jsonPath = "$.count";
        ServiceDescription serviceDescription = getServiceDescription(jsonPath, "type");

        TakeoutAsyncHttpClient successfulHttpClient = getSuccessfulHttpClient("[]");
        CompletableFuture<List<String>> future = getStatus(serviceDescription, successfulHttpClient);
        Assert.assertThrows(PathNotFoundException.class, () -> unwrapExecutionException(future));
    }

    public void testGetSuccessfulMalformedObjectStatus() {
        String jsonPath = "$.count";
        ServiceDescription serviceDescription = getServiceDescription(jsonPath, "type");

        TakeoutAsyncHttpClient successfulHttpClient = getSuccessfulHttpClient("{}");
        CompletableFuture<List<String>> future = getStatus(serviceDescription, successfulHttpClient);
        Assert.assertThrows(PathNotFoundException.class, () -> unwrapExecutionException(future));
    }

    private CompletableFuture<List<String>> getStatus(ServiceDescription serviceDescription,
                                                                     TakeoutAsyncHttpClient exceptionalHttpClient) {
        ServiceRequestProcessor serviceRequestProcessor = new ServiceRequestProcessor(exceptionalHttpClient,
                serviceDescription, MODULE_MAP);
        return serviceRequestProcessor.getStatus(0L, Collections.emptyMap(),
                new RequestContext(""));
    }

    @NotNull
    private ServiceDescription getServiceDescription(String jsonPath, String type) {
        ServiceDescription serviceDescription = new ServiceDescription();
        Status status = new Status();
        status.setJsonPath(jsonPath);
        serviceDescription.setType(type);
        Delete delete = new Delete();
        serviceDescription.setStatus(status);
        serviceDescription.setDelete(delete);
        serviceDescription.setDeleteHard(delete);
        return serviceDescription;
    }
}
