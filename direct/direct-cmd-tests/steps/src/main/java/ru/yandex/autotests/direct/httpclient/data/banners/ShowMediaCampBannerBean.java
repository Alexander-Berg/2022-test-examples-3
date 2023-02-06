package ru.yandex.autotests.direct.httpclient.data.banners;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * Created by shmykov on 19.06.15.
 */
public class ShowMediaCampBannerBean {

    @JsonPath(responsePath = "cid")
    private String cid;

    @JsonPath(responsePath = "href")
    private String href;

    @JsonPath(responsePath = "md5_picture")
    private String md5Picture;

    @JsonPath(responsePath = "name")
    private String name;

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getMd5Picture() {
        return md5Picture;
    }

    public void setMd5Picture(String md5Picture) {
        this.md5Picture = md5Picture;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
