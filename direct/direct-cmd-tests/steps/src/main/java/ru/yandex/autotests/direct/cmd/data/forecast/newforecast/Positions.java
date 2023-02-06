package ru.yandex.autotests.direct.cmd.data.forecast.newforecast;

import com.google.gson.annotations.SerializedName;

public class Positions {

    @SerializedName("first_premium")
    private PhrasePositionData firstPremium;

    @SerializedName("premium")
    private PhrasePositionData premium;

    @SerializedName("first_place")
    private PhrasePositionData firstPlace;

    @SerializedName("second_premium")
    private PhrasePositionData secondPremium;

    @SerializedName("std")
    private PhrasePositionData std;

    public PhrasePositionData getFirstPremium() {
        return firstPremium;
    }

    public Positions withFirstPremium(PhrasePositionData firstPremium) {
        this.firstPremium = firstPremium;
        return this;
    }

    public PhrasePositionData getPremium() {
        return premium;
    }

    public Positions withPremium(PhrasePositionData premium) {
        this.premium = premium;
        return this;
    }

    public PhrasePositionData getFirstPlace() {
        return firstPlace;
    }

    public Positions withFirstPlace(PhrasePositionData firstPlace) {
        this.firstPlace = firstPlace;
        return this;
    }

    public PhrasePositionData getSecondPremium() {
        return secondPremium;
    }

    public Positions withSecondPremium(PhrasePositionData secondPremium) {
        this.secondPremium = secondPremium;
        return this;
    }

    public PhrasePositionData getStd() {
        return std;
    }

    public Positions withStd(PhrasePositionData std) {
        this.std = std;
        return this;
    }
}
