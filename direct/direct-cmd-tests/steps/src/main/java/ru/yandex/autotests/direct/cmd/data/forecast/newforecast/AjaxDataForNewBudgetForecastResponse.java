package ru.yandex.autotests.direct.cmd.data.forecast.newforecast;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;

import java.util.List;
import java.util.Map;
import java.util.List;

public class AjaxDataForNewBudgetForecastResponse {

    @SerializedName("currency")
    String currency;

    @SerializedName("low_ctr_keys")
    List<String> lowCtrKeys;

    @SerializedName("data_by_positions")
    List<DataByPositions> dataByPositions;

    @SerializedName("key2phrase")
    Map<String, String> key2phrase;

    @SerializedName("phrase2key")
    Map<String, String> phrase2key;

    @SerializedName("unglued_keys")
    List<String> ungluedKeys;

    public List<String> getUngluedKeys() {
        return ungluedKeys;
    }

    public AjaxDataForNewBudgetForecastResponse withUngluedKeys(List<String> ungluedKeys) {
        this.ungluedKeys = ungluedKeys;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public AjaxDataForNewBudgetForecastResponse withCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public List<String> getLowCtrKeys() {
        return lowCtrKeys;
    }

    public AjaxDataForNewBudgetForecastResponse withLowCtrKeys(List<String> lowCtrKeys) {
        this.lowCtrKeys = lowCtrKeys;
        return this;
    }

    public List<DataByPositions> getDataByPositions() {

        return dataByPositions;
    }

    public AjaxDataForNewBudgetForecastResponse withDataByPositions(List<DataByPositions> dataByPositions) {
        this.dataByPositions = dataByPositions;
        return this;
    }

    public Map<String, String> getPhrase2key() {
        return phrase2key;
    }

    public AjaxDataForNewBudgetForecastResponse withPhrase2key(Map<String, String> phrase2key) {
        this.phrase2key = phrase2key;
        return this;
    }

    public Map<String, String> getKey2phrase() {
        return key2phrase;
    }

    public AjaxDataForNewBudgetForecastResponse withKey2phrase(Map<String, String> key2phrase) {
        this.key2phrase = key2phrase;
        return this;
    }
}
