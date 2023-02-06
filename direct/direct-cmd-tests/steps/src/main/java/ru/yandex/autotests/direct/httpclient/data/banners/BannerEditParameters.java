package ru.yandex.autotests.direct.httpclient.data.banners;

import ru.yandex.autotests.direct.httpclient.core.AbstractFormParameters;
import ru.yandex.autotests.httpclient.lite.core.FormParameter;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class BannerEditParameters extends AbstractFormParameters {
    public BannerEditParameters() {
        //new banner
        objectId = "0";
    }

    @FormParameter("apart")
    private String apart;
    @FormParameter("auto_bounds")
    private String autoBounds;
    @FormParameter("auto_point")
    private String autoPoint;
    @FormParameter("auto_precision")
    private String autoPrecision;
    @FormParameter("banner_minus_words")
    private String bannerMinusWords;
    @FormParameter("banner_with_href")
    private String bannerWithHref;
    @FormParameter("banner_with_phone")
    private String bannerWithPhone;
    @FormParameter("body")
    private String body;
    @FormParameter("build")
    private String build;
    @FormParameter("city_code")
    private String cityCode;
    @FormParameter("city")
    private String city;
    @FormParameter("contact_email")
    private String contactEmail;
    @FormParameter("contactperson")
    private String contactPerson;
    @FormParameter("country_code")
    private String countryCode;
    @FormParameter("country")
    private String country;
    @FormParameter("domain_redir_sign")
    private String domainRedirSign;
    @FormParameter("domain_redir")
    private String domainRedir;
    @FormParameter("domain_sign")
    private String domainSign;
    @FormParameter("domain")
    private String domain;
    @FormParameter("ext")
    private String ext;
    @FormParameter("extra_message")
    private String extraMessage;
    @FormParameter("geo_id")
    private String geoId;
    @FormParameter("geo")
    private String geo;
    @FormParameter("house")
    private String house;
    @FormParameter("href")
    private String href;
    @FormParameter("im_client")
    private String imClient;
    @FormParameter("im_login")
    private String imLogin;
    @FormParameter("image_name")
    private String imageName;
    @FormParameter("image")
    private String image;
    @FormParameter("manual_bounds")
    private String manualBounds;
    @FormParameter("manual_point")
    private String manualPoint;
    @FormParameter("name")
    private String name;
    @FormParameter("new_phrases")
    private String newPhrases;
    @FormParameter("ogrn")
    private String ogrn;
    @FormParameter("phone")
    private String phone;
    @FormParameter("metro")
    private String metro;
    @FormParameter("retargeting_conditions_id")
    private String retargetingConditionsId;
    @FormParameter("source_image")
    private String sourceImage;
    @FormParameter("street")
    private String street;
    @FormParameter("tags_ids")
    private String tagsIds;
    @FormParameter("title")
    private String title;
    @FormParameter("url_protocol")
    private String urlProtocol;
    @FormParameter("worktime")
    private String workTime;


    public String getApart() {
        return apart;
    }

    public void setApart(String apart) {
        this.apart = apart;
    }

    public String getAutoBounds() {
        return autoBounds;
    }

    public void setAutoBounds(String autoBounds) {
        this.autoBounds = autoBounds;
    }

    public String getAutoPoint() {
        return autoPoint;
    }

    public void setAutoPoint(String autoPoint) {
        this.autoPoint = autoPoint;
    }

    public String getAutoPrecision() {
        return autoPrecision;
    }

    public void setAutoPrecision(String autoPrecision) {
        this.autoPrecision = autoPrecision;
    }

    public String getBannerMinusWords() {
        return bannerMinusWords;
    }

    public void setBannerMinusWords(String bannerMinusWords) {
        this.bannerMinusWords = bannerMinusWords;
    }

    public String getBannerWithHref() {
        return bannerWithHref;
    }

    public void setBannerWithHref(String bannerWithHref) {
        this.bannerWithHref = bannerWithHref;
    }

    public String getBannerWithPhone() {
        return bannerWithPhone;
    }

    public void setBannerWithPhone(String bannerWithPhone) {
        this.bannerWithPhone = bannerWithPhone;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getBuild() {
        return build;
    }

    public void setBuild(String build) {
        this.build = build;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDomainRedirSign() {
        return domainRedirSign;
    }

    public void setDomainRedirSign(String domainRedirSign) {
        this.domainRedirSign = domainRedirSign;
    }

    public String getDomainRedir() {
        return domainRedir;
    }

    public void setDomainRedir(String domainRedir) {
        this.domainRedir = domainRedir;
    }

    public String getDomainSign() {
        return domainSign;
    }

    public void setDomainSign(String domainSign) {
        this.domainSign = domainSign;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public String getExtraMessage() {
        return extraMessage;
    }

    public void setExtraMessage(String extraMessage) {
        this.extraMessage = extraMessage;
    }

    public String getGeoId() {
        return geoId;
    }

    public void setGeoId(String geoId) {
        this.geoId = geoId;
    }

    public String getGeo() {
        return geo;
    }

    public void setGeo(String geo) {
        this.geo = geo;
    }

    public String getHouse() {
        return house;
    }

    public void setHouse(String house) {
        this.house = house;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getImClient() {
        return imClient;
    }

    public void setImClient(String imClient) {
        this.imClient = imClient;
    }

    public String getImLogin() {
        return imLogin;
    }

    public void setImLogin(String imLogin) {
        this.imLogin = imLogin;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getManualBounds() {
        return manualBounds;
    }

    public void setManualBounds(String manualBounds) {
        this.manualBounds = manualBounds;
    }

    public String getManualPoint() {
        return manualPoint;
    }

    public void setManualPoint(String manualPoint) {
        this.manualPoint = manualPoint;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNewPhrases() {
        return newPhrases;
    }

    public void setNewPhrases(String newPhrases) {
        this.newPhrases = newPhrases;
    }

    public String getOgrn() {
        return ogrn;
    }

    public void setOgrn(String ogrn) {
        this.ogrn = ogrn;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRetargetingConditionsId() {
        return retargetingConditionsId;
    }

    public void setRetargetingConditionsId(String retargetingConditionsId) {
        this.retargetingConditionsId = retargetingConditionsId;
    }

    public String getMetro() {
        return metro;
    }

    public void setMetro(String metro) {
        this.metro = metro;
    }

    public String getSourceImage() {
        return sourceImage;
    }

    public void setSourceImage(String sourceImage) {
        this.sourceImage = sourceImage;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getTagsIds() {
        return tagsIds;
    }

    public void setTagsIds(String tagsIds) {
        this.tagsIds = tagsIds;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrlProtocol() {
        return urlProtocol;
    }

    public void setUrlProtocol(String urlProtocol) {
        this.urlProtocol = urlProtocol;
    }

    public String getWorkTime() {
        return workTime;
    }

    public void setWorkTime(String workTime) {
        this.workTime = workTime;
    }

}

