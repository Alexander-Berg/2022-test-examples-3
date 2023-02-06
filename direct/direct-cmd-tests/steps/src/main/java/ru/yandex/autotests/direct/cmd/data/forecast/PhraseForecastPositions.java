package ru.yandex.autotests.direct.cmd.data.forecast;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

/**
 * Created by aleran on 29.09.2015.
 */
public class PhraseForecastPositions {

    @SerializedName("md5")
    private String md5;

    @SerializedName("std")
    HashMap<PhrasePositionForecastDomainEnum, PhrasePositionData> stdPositionWrapper;

    @SerializedName("first_place")
    HashMap<PhrasePositionForecastDomainEnum, PhrasePositionData> firstPlacePositionWrapper;

    @SerializedName("first_premium")
    HashMap<PhrasePositionForecastDomainEnum, PhrasePositionData> firstPremiumPositionWrapper;

    @SerializedName("premium")
    HashMap<PhrasePositionForecastDomainEnum, PhrasePositionData> premiumPositionWrapper;

    @SerializedName("second_premium")
    HashMap<PhrasePositionForecastDomainEnum, PhrasePositionData> secondPremiumPositionWrapper;

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public HashMap<PhrasePositionForecastDomainEnum, PhrasePositionData> getStdPositionWrapper() {
        return stdPositionWrapper;
    }

    public PhrasePositionData getYandexStdPosition(){
        return stdPositionWrapper.get(PhrasePositionForecastDomainEnum.YANDEX);
    }

    public void setStdPositionWrapper(HashMap<PhrasePositionForecastDomainEnum, PhrasePositionData> stdPositionWrapper) {
        this.stdPositionWrapper = stdPositionWrapper;
    }

    public HashMap<PhrasePositionForecastDomainEnum, PhrasePositionData> getFirstPlacePositionWrapper() {
        return firstPlacePositionWrapper;
    }

    public PhrasePositionData getYandexFirstPlacePosition(){
        return firstPlacePositionWrapper.get(PhrasePositionForecastDomainEnum.YANDEX);
    }

    public void setFirstPlacePositionWrapper(HashMap<PhrasePositionForecastDomainEnum, PhrasePositionData> firstPlacePositionWrapper) {
        this.firstPlacePositionWrapper = firstPlacePositionWrapper;
    }

    public HashMap<PhrasePositionForecastDomainEnum, PhrasePositionData> getFirstPremiumPositionWrapper() {
        return firstPremiumPositionWrapper;
    }

    public PhrasePositionData getYandexFirstPremiumPosition(){
        return firstPremiumPositionWrapper.get(PhrasePositionForecastDomainEnum.YANDEX);
    }

    public void setFirstPremiumPositionWrapper(HashMap<PhrasePositionForecastDomainEnum, PhrasePositionData> firstPremiumPositionWrapper) {
        this.firstPremiumPositionWrapper = firstPremiumPositionWrapper;
    }

    public HashMap<PhrasePositionForecastDomainEnum, PhrasePositionData> getPremiumPositionWrapper() {
        return premiumPositionWrapper;
    }

    public PhrasePositionData getYandexPremiumPosition(){
        return premiumPositionWrapper.get(PhrasePositionForecastDomainEnum.YANDEX);
    }

    public void setPremiumPositionWrapper(HashMap<PhrasePositionForecastDomainEnum, PhrasePositionData> premiumPositionWrapper) {
        this.premiumPositionWrapper = premiumPositionWrapper;
    }

    public HashMap<PhrasePositionForecastDomainEnum, PhrasePositionData> getSecondPremiumPositionWrapper() {
        return secondPremiumPositionWrapper;
    }

    public PhrasePositionData getYandexSecondPremiumPosition(){
        return secondPremiumPositionWrapper.get(PhrasePositionForecastDomainEnum.YANDEX);
    }

    public void setSecondPremiumPositionWrapper(HashMap<PhrasePositionForecastDomainEnum, PhrasePositionData> secondPremiumPositionWrapper) {
        this.secondPremiumPositionWrapper = secondPremiumPositionWrapper;
    }
}
