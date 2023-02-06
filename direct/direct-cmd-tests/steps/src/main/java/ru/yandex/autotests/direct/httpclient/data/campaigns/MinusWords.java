package ru.yandex.autotests.direct.httpclient.data.campaigns;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.JsonStringTransformableCmdBean;

import java.util.List;

public class MinusWords extends JsonStringTransformableCmdBean {

    @SerializedName("minus_words")
    List<String> minusWords;

    public List<String> getMinusWords() {
        return minusWords;
    }

    public void setMinusWords(List<String> minusWords) {
        this.minusWords = minusWords;
    }

    public MinusWords withMinusWords(List<String> minusWords) {
        this.minusWords = minusWords;
        return this;
    }

    @Override
    public String toJson() {
        return new Gson().toJson(getMinusWords());
    }

    @Override
    public String toString() {
        return toJson();
    }
}
