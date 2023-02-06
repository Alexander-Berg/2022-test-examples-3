package ru.yandex.autotests.direct.cmd.data.feeds;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;

import java.util.HashMap;

/**
 * Created by aleran on 19.10.2015.
 */
public class AjaxDeleteFeedsResponse extends ErrorResponse {

    @SerializedName("result")
    private HashMap<Long, String> message;

    private Test test;

    public Test getTest() {
        return test;
    }

    public void setTest(Test test) {
        this.test = test;
    }

    public HashMap<Long, String> getMessage() {
        return message;
    }

    public void setMessage(HashMap<Long, String> message) {
        this.message = message;
    }

    public AjaxDeleteFeedsResponse withMessage(HashMap<Long, String> message) {
        setMessage(message);
        return this;
    }

    public AjaxDeleteFeedsResponse withTest(){
        test = new Test();
        test.setTest("test");
        return this;
    }

    public class Test{
        private String test;

        public String getTest() {
            return test;
        }

        public void setTest(String test) {
            this.test = test;
        }
    }
}
