package ru.yandex.market.wms.servicebus.scenario.builder;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public class FulfillmentInteraction {

    private FulfillmentUrl fulfillmentUrl;
    private HttpStatus responseStatus;
    private String expectedRequestPath;
    private String responsePath;
    private MediaType responseContentType;
    private int invocationCount;

    public FulfillmentInteraction() {
    }

    public static FulfillmentInteraction createMarschrouteInteraction() {
        return new FulfillmentInteraction()
            .setResponseStatus(HttpStatus.OK)
            .setResponseContentType(MediaType.APPLICATION_JSON)
            .setInvocationCount(1);
    }

    public static FulfillmentInteraction createItellaInteraction() {
        return new FulfillmentInteraction()
            .setResponseStatus(HttpStatus.OK)
            .setResponseContentType(MediaType.TEXT_PLAIN)
            .setInvocationCount(1);
    }


    public FulfillmentUrl getFulfillmentUrl() {
        return fulfillmentUrl;
    }

    public FulfillmentInteraction setFulfillmentUrl(FulfillmentUrl fulfillmentUrl) {
        this.fulfillmentUrl = fulfillmentUrl;
        return this;
    }

    public HttpStatus getResponseStatus() {
        return responseStatus;
    }

    public FulfillmentInteraction setResponseStatus(HttpStatus responseStatus) {
        this.responseStatus = responseStatus;
        return this;
    }

    public String getExpectedRequestPath() {
        return expectedRequestPath;
    }

    public FulfillmentInteraction setExpectedRequestPath(String expectedRequestPath) {
        this.expectedRequestPath = expectedRequestPath;
        return this;
    }

    public String getResponsePath() {
        return responsePath;
    }

    public FulfillmentInteraction setResponsePath(String responsePath) {
        this.responsePath = responsePath;
        return this;
    }

    public MediaType getResponseContentType() {
        return responseContentType;
    }

    public FulfillmentInteraction setResponseContentType(MediaType responseContentType) {
        this.responseContentType = responseContentType;
        return this;
    }

    public int getInvocationCount() {
        return invocationCount;
    }

    public FulfillmentInteraction setInvocationCount(int invocationCount) {
        this.invocationCount = invocationCount;
        return this;
    }
}
