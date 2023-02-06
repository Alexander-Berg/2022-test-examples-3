package ru.yandex.market.checkout.checkouter.tasks.eventinspector.yql;

import java.util.Map;

public class YQLMockParameters {
    private Operation createResponse;
    private Operation statusResponse;
    private Map<String, Object> operationData;

    public Operation getCreateResponse() {
        return createResponse;
    }

    public void setCreateResponse(Operation createResponse) {
        this.createResponse = createResponse;
    }

    public Operation getStatusResponse() {
        return statusResponse;
    }

    public void setStatusResponse(Operation statusResponse) {
        this.statusResponse = statusResponse;
    }

    public Map<String, Object> getOperationData() {
        return operationData;
    }

    public void setOperationData(Map<String, Object> operationData) {
        this.operationData = operationData;
    }

    @Override
    public String toString() {
        return "YQLMockParameters{" +
                "createResponse=" + createResponse +
                ", statusResponse=" + statusResponse +
                ", operationData=" + operationData +
                '}';
    }
}
