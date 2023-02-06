package ru.yandex.autotests.direct.cmd.data.commons.diags;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class BannerDiags {

    @SerializedName("phrases")
    private Diags phrases;

    @SerializedName("banner")
    private List<Diags> banner;

    @SerializedName("video_addition")
    private List<Diags> videoAdditionDiags;

    public List<Diags> getVideoAdditionDiags() {
        return videoAdditionDiags;
    }

    public void setVideoAdditionDiags(List<Diags> videoAdditionDiags)
    {
        this.videoAdditionDiags = videoAdditionDiags;
    }

    public BannerDiags withVideoAdditionDiags(List<Diags> videoAdditionDiags)
    {
        setVideoAdditionDiags(videoAdditionDiags);
        return this;
    }

    public Diags getPhrases() {
        return phrases;
    }

    public BannerDiags withPhrases(Diags phrases) {
        this.phrases = phrases;
        return this;
    }

    public List<Diags> getBanner() {
        return banner;
    }

    public BannerDiags withBanner(List<Diags> banner) {
        this.banner = banner;
        return this;
    }
}
