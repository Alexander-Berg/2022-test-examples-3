package ru.yandex.autotests.direct.httpclient.data.banners;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class BannerSaveParameters extends BasicDirectRequestParameters {
    public BannerSaveParameters() {
        //new banner
        objectId = "0";
    }

    @JsonPath(requestPath = "apart")
    private String apart;
    @JsonPath(requestPath = "auto_bounds")
    private String autoBounds;
    @JsonPath(requestPath = "auto_point")
    private String autoPoint;
    @JsonPath(requestPath = "auto_precision")
    private String autoPrecision;
    @JsonPath(requestPath = "banner_minus_words")
    private String bannerMinusWords;
    @JsonPath(requestPath = "bid")
    private String bid;
    @JsonPath(requestPath = "body")
    private String body;
    @JsonPath(requestPath = "build")
    private String build;
    @JsonPath(requestPath = "city_code")
    private String cityCode;
    @JsonPath(requestPath = "city")
    private String city;
    @JsonPath(requestPath = "contact_email")
    private String contactEmail;
    @JsonPath(requestPath = "contactperson")
    private String contactPerson;
    @JsonPath(requestPath = "country_code")
    private String countryCode;
    @JsonPath(requestPath = "country")
    private String country;
    @JsonPath(requestPath = "domain_redir_sign")
    private String domainRedirSign;
    @JsonPath(requestPath = "domain_redir")
    private String domainRedir;
    @JsonPath(requestPath = "domain_sign")
    private String domainSign;
    @JsonPath(requestPath = "domain")
    private String domain;
    @JsonPath(requestPath = "ext")
    private String ext;
    @JsonPath(requestPath = "extra_message")
    private String extraMessage;
    @JsonPath(requestPath = "geo_id")
    private String geoId;
    @JsonPath(requestPath = "geo")
    private String geo;
    @JsonPath(requestPath = "group_name")
    private String groupName;
    @JsonPath(requestPath = "house")
    private String house;
    @JsonPath(requestPath = "href")
    private String href;
    @JsonPath(requestPath = "im_client")
    private String imClient;
    @JsonPath(requestPath = "im_login")
    private String imLogin;
    @JsonPath(requestPath = "image_name")
    private String imageName;
    @JsonPath(requestPath = "image")
    private String image;
    @JsonPath(requestPath = "manual_bounds")
    private String manualBounds;
    @JsonPath(requestPath = "manual_point")
    private String manualPoint;
    @JsonPath(requestPath = "metro")
    private String metro;
    @JsonPath(requestPath = "name")
    private String name;
    @JsonPath(requestPath = "ogrn")
    private String ogrn;
    @JsonPath(requestPath = "org_details_id")
    private String orgDetailsId;
    @JsonPath(requestPath = "phone")
    private String phone;
    @JsonPath(requestPath = "retargeting_conditions_id")
    private String retargetingConditionsId;
    @JsonPath(requestPath = "showsForecast")
    private String showsForecast;
    @JsonPath(requestPath = "statusShowsForecast")
    private String statusShowsForecast;
    @JsonPath(requestPath = "showsForecastSign")
    private String showsForecastSign;
    @JsonPath(requestPath = "street")
    private String street;
    @JsonPath(requestPath = "tags_ids")
    private String tagsIds;
    @JsonPath(requestPath = "title")
    private String title;
    @JsonPath(requestPath = "worktime")
    private String workTime;
    @JsonPath(requestPath = "url_protocol")
    private String urlProtocol;


    private List<PhraseSaveParameters> phrases;

    public List<PhraseSaveParameters> getPhrases() {
        if(phrases == null) {
            phrases = new ArrayList<PhraseSaveParameters>();
        }
        return phrases;
    }

    public void setPhrases(List<PhraseSaveParameters> phrases) {
        this.phrases = phrases;
    }

    public void addPhrase(PhraseSaveParameters phrase) {
        getPhrases().add(phrase);
    }

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

    public String getBid() {
        return bid;
    }

    public void setBid(String bid) {
        this.bid = bid;
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

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
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

    public String getMetro() {
        return metro;
    }

    public void setMetro(String metro) {
        this.metro = metro;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOgrn() {
        return ogrn;
    }

    public void setOgrn(String ogrn) {
        this.ogrn = ogrn;
    }

    public String getOrgDetailsId() {
        return orgDetailsId;
    }

    public void setOrgDetailsId(String orgDetailsId) {
        this.orgDetailsId = orgDetailsId;
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

    public String getShowsForecast() {
        return showsForecast;
    }

    public void setShowsForecast(String showsForecast) {
        this.showsForecast = showsForecast;
    }

    public String getStatusShowsForecast() {
        return statusShowsForecast;
    }

    public void setStatusShowsForecast(String statusShowsForecast) {
        this.statusShowsForecast = statusShowsForecast;
    }

    public String getShowsForecastSign() {
        return showsForecastSign;
    }

    public void setShowsForecastSign(String showsForecastSign) {
        this.showsForecastSign = showsForecastSign;
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

    public String getWorkTime() {
        return workTime;
    }

    public void setWorkTime(String workTime) {
        this.workTime = workTime;
    }

    public String getUrlProtocol() {
        return urlProtocol;
    }

    public void setUrlProtocol(String urlProtocol) {
        this.urlProtocol = urlProtocol;
    }

}
