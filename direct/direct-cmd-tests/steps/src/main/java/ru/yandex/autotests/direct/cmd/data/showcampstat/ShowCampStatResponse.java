package ru.yandex.autotests.direct.cmd.data.showcampstat;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;

import java.util.ArrayList;
import java.util.List;

public class ShowCampStatResponse {
    @SerializedName("tags_allowed")
    String tagsAllowed;

    @SerializedName("banners")
    List<Banner> banners = new ArrayList<>();

    public String getTagsAllowed() {
        return tagsAllowed;
    }

    public ShowCampStatResponse withTagsAllowed(String tagsAllowed) {
        this.tagsAllowed = tagsAllowed;
        return this;
    }

    public List<Banner> getBanners() {
        return banners;
    }

    public ShowCampStatResponse withBanners(List<Banner> banners) {
        this.banners = banners;
        return this;
    }
}
