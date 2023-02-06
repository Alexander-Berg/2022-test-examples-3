package ru.yandex.autotests.direct.cmd.data.canvasbs;

import com.google.gson.annotations.SerializedName;

public class CanvasCreativeBsResponse {

    @SerializedName("creativeId")
    private Long creativeId;

    @SerializedName("ok")
    private Boolean ok;

    @SerializedName("message")
    private String message;

    @SerializedName("creative")
    private CanvasBs creative;

    public Long getCreativeId() {
        return creativeId;
    }

    public CanvasCreativeBsResponse withCreativeId(Long creativeId) {
        this.creativeId = creativeId;
        return this;
    }

    public Boolean getOk() {
        return ok;
    }

    public CanvasCreativeBsResponse withOk(Boolean ok) {
        this.ok = ok;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public CanvasCreativeBsResponse withMessage(String message) {
        this.message = message;
        return this;
    }

    public CanvasBs getCreative() {
        return creative;
    }

    public CanvasCreativeBsResponse withCreative(CanvasBs creative) {
        this.creative = creative;
        return this;
    }
}
