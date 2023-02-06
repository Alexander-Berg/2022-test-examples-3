package ru.yandex.autotests.direct.cmd.data.stepzero;

import com.google.gson.annotations.SerializedName;

public class StepZeroProcessErrorResponse {

    @SerializedName("msg")
    private String msg;

    @SerializedName("hash")
    private Hash hash;

    public String getMsg() {
        return msg;
    }

    public Hash getHash() {
        return hash;
    }

    public class Hash {

        @SerializedName("return_to")
        private ReturnTo returnTo;

        public ReturnTo getReturnTo() {
            return returnTo;
        }

        public class ReturnTo {

            @SerializedName("text")
            private String text;

            @SerializedName("href")
            private String href;

            public String getText() {
                return text;
            }

            public String getHref() {
                return href;
            }
        }
    }
}
