package ru.yandex.autotests.direct.httpclient.data.CmdBeans;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * Created by shmykov on 13.04.15.
 */
public class SiteLinksCmdBean {

    public static SiteLinksCmdBean getEmptySiteLink() {
        SiteLinksCmdBean siteLinksCmdBean = new SiteLinksCmdBean();
        siteLinksCmdBean.setTitle("");
        siteLinksCmdBean.setHref("");
        siteLinksCmdBean.setUrlProtocol("http://");
        return siteLinksCmdBean;
    }

    @JsonPath(responsePath = "title", requestPath = "title")
    private String title;

    @JsonPath(responsePath = "href", requestPath = "href")
    private String href;

    @JsonPath(responsePath = "url_protocol", requestPath = "url_protocol")
    private String urlProtocol;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getUrlProtocol() {
        return urlProtocol;
    }

    public void setUrlProtocol(String urlProtocol) {
        this.urlProtocol = urlProtocol;
    }
}
