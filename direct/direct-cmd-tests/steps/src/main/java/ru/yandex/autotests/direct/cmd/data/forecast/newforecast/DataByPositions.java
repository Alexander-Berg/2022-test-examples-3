package ru.yandex.autotests.direct.cmd.data.forecast.newforecast;

import com.google.gson.annotations.SerializedName;

public class DataByPositions {

    @SerializedName("positions")
    private Positions positions;

    @SerializedName("sign")
    private String sign;

    @SerializedName("md5")
    private String md5;

    @SerializedName("shows")
    private Integer shows;

    public Positions getPositions() {
        return positions;
    }

    public DataByPositions withPositions(Positions positions) {
        this.positions = positions;
        return this;
    }

    public String getSign() {
        return sign;
    }

    public DataByPositions withSign(String sign) {
        this.sign = sign;
        return this;
    }

    public String getMd5() {
        return md5;
    }

    public DataByPositions withMd5(String md5) {
        this.md5 = md5;
        return this;
    }

    public Integer getShows() {
        return shows;
    }

    public DataByPositions withShows(Integer shows) {
        this.shows = shows;
        return this;
    }
}
