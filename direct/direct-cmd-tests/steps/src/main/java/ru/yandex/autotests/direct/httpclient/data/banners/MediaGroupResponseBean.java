package ru.yandex.autotests.direct.httpclient.data.banners;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;

/**
 * Created by shmykov on 19.06.15.
 */
public class MediaGroupResponseBean {

    public List<ShowMediaCampBannerBean> getBanners() {
        return banners;
    }

    public void setBanners(List<ShowMediaCampBannerBean> banners) {
        this.banners = banners;
    }

    @JsonPath(responsePath = "media_groups/banners")
    private List<ShowMediaCampBannerBean> banners;
    
}
