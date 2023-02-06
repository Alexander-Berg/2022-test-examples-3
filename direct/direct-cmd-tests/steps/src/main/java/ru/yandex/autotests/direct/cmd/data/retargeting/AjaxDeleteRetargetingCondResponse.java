package ru.yandex.autotests.direct.cmd.data.retargeting;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.CommonErrorsResource;

import java.util.Collections;
import java.util.List;

import static ru.yandex.autotests.direct.cmd.data.retargeting.CommonAjaxRetConditionResponse.RESULT_ERR;

public class AjaxDeleteRetargetingCondResponse {

    public static class Error {

        @SerializedName("result")
        private String result;

        @SerializedName("ret_cond_id")
        private Long retCondId;

        @SerializedName("error_text")
        private String errorText;

        public Error(String errorText, String result, Long retCondId) {
            this.errorText = errorText;
            this.result = result;
            this.retCondId = retCondId;
        }

        public String getErrorText() {
            return errorText;
        }

        public String getResult() {
            return result;
        }

        public Long getRetCondId() {
            return retCondId;
        }
    }

    public static AjaxDeleteRetargetingCondResponse commonError(CommonErrorsResource error) {
        return new AjaxDeleteRetargetingCondResponse().withError(error.toString());
    }

    public static AjaxDeleteRetargetingCondResponse conditionError(Long retCondId,
                                                                   DeleteRetConditionErrorsResource error) {
        return new AjaxDeleteRetargetingCondResponse().
                withResult(RESULT_ERR).
                withErrors(Collections.singletonList(new Error(error.toString(), RESULT_ERR, retCondId)));
    }

    @SerializedName("result")
    private String result;

    @SerializedName("error")
    private String error;

    @SerializedName("errors")
    private List<Error> errors;

    public String getResult() {
        return result;
    }

    public AjaxDeleteRetargetingCondResponse withResult(String result) {
        this.result = result;
        return this;
    }

    public String getError() {
        return error;
    }

    public AjaxDeleteRetargetingCondResponse withError(String error) {
        this.error = error;
        return this;
    }

    public List<Error> getErrors() {
        return errors;
    }

    public AjaxDeleteRetargetingCondResponse withErrors(List<Error> errors) {
        this.errors = errors;
        return this;
    }
}
