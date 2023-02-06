package ru.yandex.autotests.direct.cmd.data.retargeting;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.CommonErrorsResource;

public class CommonAjaxRetConditionResponse {

    public static final String RESULT_OK = "ok";
    public static final String RESULT_ERR = "error";

    public static CommonAjaxRetConditionResponse error(RetConditionErrorType errorType,
                                                       RetConditionErrorsResource errorText) {
        return new CommonAjaxRetConditionResponse().
                withResult(RESULT_ERR).
                withErrorType(errorType.toString()).
                withErrorText(errorText.toString());
    }

    public static CommonAjaxRetConditionResponse formatError() {
        return new CommonAjaxRetConditionResponse().
                withError(CommonErrorsResource.INVALID_PARAMETER_FORMAT.toString()).
                withErrorNo(8);
    }

    @SerializedName("result")
    private String result;

    @SerializedName("error_type")
    private String errorType;

    @SerializedName("error_text")
    private String errorText;

    @SerializedName("error")
    private String error;

    @SerializedName("error_no")
    private Integer errorNo;

    public String getResult() {
        return result;
    }

    public CommonAjaxRetConditionResponse withResult(String result) {
        this.result = result;
        return this;
    }

    public String getErrorText() {
        return errorText;
    }

    public CommonAjaxRetConditionResponse withErrorText(String errorText) {
        this.errorText = errorText;
        return this;
    }

    public String getErrorType() {
        return errorType;
    }

    public CommonAjaxRetConditionResponse withErrorType(String errorType) {
        this.errorType = errorType;
        return this;
    }

    public String getError() {
        return error;
    }

    public CommonAjaxRetConditionResponse withError(String error) {
        this.error = error;
        return this;
    }

    public Integer getErrorNo() {
        return errorNo;
    }

    public CommonAjaxRetConditionResponse withErrorNo(Integer errorNo) {
        this.errorNo = errorNo;
        return this;
    }
}
