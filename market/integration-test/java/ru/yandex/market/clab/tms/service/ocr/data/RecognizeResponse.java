package ru.yandex.market.clab.tms.service.ocr.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RecognizeResponse {

    private String status;

    @JsonProperty("error_message")
    private String errorMessage;

    private RecognizeData data;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public RecognizeData getData() {
        return data;
    }

    public void setData(RecognizeData data) {
        this.data = data;
    }
}
