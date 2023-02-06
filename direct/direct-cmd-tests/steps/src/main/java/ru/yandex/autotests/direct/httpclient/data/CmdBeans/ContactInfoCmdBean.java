package ru.yandex.autotests.direct.httpclient.data.CmdBeans;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * Created by shmykov on 10.04.15.
 */
public class ContactInfoCmdBean {

    @JsonPath(responsePath = "country", requestPath = "country")
    private String country;

    @JsonPath(responsePath = "city", requestPath = "city")
    private String city;

    @JsonPath(responsePath = "country_code", requestPath = "country_code")
    private String countryCode;

    @JsonPath(responsePath = "city_code", requestPath = "city_code")
    private String cityCode;

    @JsonPath(responsePath = "phone", requestPath = "phone")
    private String phone;

    @JsonPath(responsePath = "ext", requestPath = "ext")
    private String phoneExt;

    @JsonPath(responsePath = "name", requestPath = "name")
    private String companyName;

    @JsonPath(responsePath = "contactperson", requestPath = "contactperson")
    private String contactPerson;

    @JsonPath(responsePath = "worktime", requestPath = "worktime")
    private String workTime;

    @JsonPath(responsePath = "street", requestPath = "street")
    private String street;

    @JsonPath(responsePath = "house", requestPath = "house")
    private String house;

    @JsonPath(responsePath = "build", requestPath = "build")
    private String build;

    @JsonPath(responsePath = "apart", requestPath = "apart")
    private String apart;

    @JsonPath(responsePath = "contact_email", requestPath = "contact_email")
    private String contactEmail;

    @JsonPath(responsePath = "im_client", requestPath = "im_client")
    private String IMClient;

    @JsonPath(responsePath = "im_login", requestPath = "im_login")
    private String IMLogin;

    @JsonPath(responsePath = "extra_message", requestPath = "extra_message")
    private String extraMessage;

    @JsonPath(responsePath = "/ogrn", requestPath = "ogrn")
    private String OGRN;

    public void setCountry(String country) {
        this.country = country;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setPhoneExt(String phoneExt) {
        this.phoneExt = phoneExt;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public void setWorkTime(String workTime) {
        this.workTime = workTime;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public void setHouse(String house) {
        this.house = house;
    }

    public void setBuild(String build) {
        this.build = build;
    }

    public void setApart(String apart) {
        this.apart = apart;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public void setIMClient(String IMClient) {
        this.IMClient = IMClient;
    }

    public void setIMLogin(String IMLogin) {
        this.IMLogin = IMLogin;
    }

    public void setExtraMessage(String extraMessage) {
        this.extraMessage = extraMessage;
    }

    public void setOGRN(String OGRN) {
        this.OGRN = OGRN;
    }

    public String getCountry() {
        return country;
    }

    public String getCity() {
        return city;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getCityCode() {
        return cityCode;
    }

    public String getPhone() {
        return phone;
    }

    public String getPhoneExt() {
        return phoneExt;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public String getWorkTime() {
        return workTime;
    }

    public String getStreet() {
        return street;
    }

    public String getHouse() {
        return house;
    }

    public String getBuild() {
        return build;
    }

    public String getApart() {
        return apart;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getIMClient() {
        return IMClient;
    }

    public String getIMLogin() {
        return IMLogin;
    }

    public String getExtraMessage() {
        return extraMessage;
    }

    public String getOGRN() {
        return OGRN;
    }

}