package ru.yandex.autotests.direct.cmd.data.forecast;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ValueToJsonSerializer;

import java.util.List;

/**
 * Created by aleran on 29.09.2015.
 */
public class AjaxDataForBudgetForecastRequest extends BasicDirectRequest {

    @SerializeKey("advanced_forecast")
    private String advancedForecast;

    @SerializeKey("fixate_stopwords")
    private String fixateStopWords;

    @SerializeKey("geo")
    private String geo;

    @SerializeKey("jsonMinusWords")
    @SerializeBy(ValueToJsonSerializer.class)
    private List<String> jsonMinusWords;

    @SerializeKey("period")
    private String period;

    @SerializeKey("period_num")
    private String periodNum;

    @SerializeKey("phrases")
    private String phrases;

    @SerializeKey("unglue")
    private String unglue;

    public String getAdvancedForecast() {
        return advancedForecast;
    }

    public void setAdvancedForecast(String advancedForecast) {
        this.advancedForecast = advancedForecast;
    }

    public String getFixateStopWords() {
        return fixateStopWords;
    }

    public void setFixateStopWords(String fixateStopWords) {
        this.fixateStopWords = fixateStopWords;
    }

    public String getGeo() {
        return geo;
    }

    public void setGeo(String geo) {
        this.geo = geo;
    }

    public List<String> getJsonMinusWords() {
        return jsonMinusWords;
    }

    public void setJsonMinusWords(List<String> jsonMinusWords) {
        this.jsonMinusWords = jsonMinusWords;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getPeriodNum() {
        return periodNum;
    }

    public void setPeriodNum(String periodNum) {
        this.periodNum = periodNum;
    }

    public String getPhrases() {
        return phrases;
    }

    public void setPhrases(String phrases) {
        this.phrases = phrases;
    }

    public String getUnglue() {
        return unglue;
    }

    public void setUnglue(String unglue) {
        this.unglue = unglue;
    }

    public AjaxDataForBudgetForecastRequest withAdvancedForecast(String advancedForecast) {
        this.advancedForecast = advancedForecast;
        return this;
    }

    public AjaxDataForBudgetForecastRequest withFixateStopWords(String fixateStopwords) {
        this.fixateStopWords = fixateStopwords;
        return this;
    }

    public AjaxDataForBudgetForecastRequest withGeo(String geo) {
        this.geo = geo;
        return this;
    }

    public AjaxDataForBudgetForecastRequest withJsonMinusWords(List<String> jsonMinusWords) {
        this.jsonMinusWords = jsonMinusWords;
        return this;
    }

    public AjaxDataForBudgetForecastRequest withPeriod(String period) {
        this.period = period;
        return this;
    }

    public AjaxDataForBudgetForecastRequest withPeriodNum(String periodNum) {
        this.periodNum = periodNum;
        return this;
    }

    public AjaxDataForBudgetForecastRequest withPhrases(String phrases) {
        this.phrases = phrases;
        return this;
    }

    public AjaxDataForBudgetForecastRequest withUnglue(String unglue) {
        this.unglue = unglue;
        return this;
    }
}
