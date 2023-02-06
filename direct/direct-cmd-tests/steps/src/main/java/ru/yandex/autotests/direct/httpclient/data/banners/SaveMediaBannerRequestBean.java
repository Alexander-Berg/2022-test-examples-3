package ru.yandex.autotests.direct.httpclient.data.banners;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 15.06.15
 */
public class SaveMediaBannerRequestBean extends BasicDirectRequestParameters {

    @JsonPath(requestPath = "product_type")
    private String productType;

    @JsonPath(requestPath = "from_newCamp")
    private String fromNewCamp;

    @JsonPath(requestPath = "cid")
    private String cid;

    @JsonPath(requestPath = "uid")
    private String uid;

    @JsonPath(requestPath = "mcb_theme_1")
    private String mcbTheme1;

    @JsonPath(requestPath = "mcb_theme_2")
    private String mcbTheme2;

    @JsonPath(requestPath = "mcb_phrases_text")
    private String mcbPhrasesText;

    @JsonPath(requestPath = "mcb_theme_id")
    private String mcbThemeId;

    //Здесь указывается путь к файлу с картинкой. При отправке post-запроса в это поле подставится содержимое указанного файла
    @JsonPath(requestPath = "file_picture")
    private String filePicture;

    @JsonPath(requestPath = "name")
    private String name;

    @JsonPath(requestPath = "url_protocol")
    private String urlProtocol;

    @JsonPath(requestPath = "href")
    private String href;

    @JsonPath(requestPath = "counter_url_protocol")
    private String counterUrlProtocol;

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getFromNewCamp() {
        return fromNewCamp;
    }

    public void setFromNewCamp(String fromNewCamp) {
        this.fromNewCamp = fromNewCamp;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getMcbTheme1() {
        return mcbTheme1;
    }

    public void setMcbTheme1(String mcbTheme1) {
        this.mcbTheme1 = mcbTheme1;
    }

    public String getMcbTheme2() {
        return mcbTheme2;
    }

    public void setMcbTheme2(String mcbTheme2) {
        this.mcbTheme2 = mcbTheme2;
    }

    public String getMcbPhrasesText() {
        return mcbPhrasesText;
    }

    public void setMcbPhrasesText(String mcbPhrasesText) {
        this.mcbPhrasesText = mcbPhrasesText;
    }

    public String getMcbThemeId() {
        return mcbThemeId;
    }

    public void setMcbThemeId(String mcbThemeId) {
        this.mcbThemeId = mcbThemeId;
    }

    public String getFilePicture() {
        return filePicture;
    }

    public void setFilePicture(String filePicture) {
        this.filePicture = filePicture;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrlProtocol() {
        return urlProtocol;
    }

    public void setUrlProtocol(String urlProtocol) {
        this.urlProtocol = urlProtocol;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getCounterUrlProtocol() {
        return counterUrlProtocol;
    }

    public void setCounterUrlProtocol(String counterUrlProtocol) {
        this.counterUrlProtocol = counterUrlProtocol;
    }
}
