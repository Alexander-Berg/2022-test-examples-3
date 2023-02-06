package ru.yandex.autotests.direct.cmd.data.commons.banner;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class ContactInfo {

    @SerializedName("country")
    private String country;

    @SerializedName("city")
    private String city;

    @SerializedName("country_code")
    private String countryCode;

    @SerializedName("city_code")
    private String cityCode;

    @SerializedName("phone")
    private String phone;

    @SerializedName("ext")
    private String phoneExt;

    @SerializedName("name")
    private String companyName;

    @SerializedName("contactperson")
    private String contactPerson;

    @SerializedName("worktime")
    private String workTime;

    @SerializedName("street")
    private String street;

    @SerializedName("house")
    private String house;

    @SerializedName("build")
    private String build;

    @SerializedName("apart")
    private String apart;

    @SerializedName("metro")
    private String metro;

    @SerializedName("auto_point")
    private String autoPoint;

    @SerializedName("auto_bounds")
    private String autoBound;

    @SerializedName("auto_precision")
    private String autoPrecision;

    @SerializedName("contact_email")
    private String contactEmail;

    @SerializedName("im_client")
    private String IMClient;

    @SerializedName("im_login")
    private String IMLogin;

    @SerializedName("extra_message")
    private String extraMessage;

    @SerializedName("org_details")
    private OrgDetails orgDetails;

    @SerializedName("ogrn")
    private String OGRN;


    public ContactInfo clone() {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(this), ContactInfo.class);
    }


    public String getCountry() {
        return country;
    }

    public ContactInfo withCountry(String country) {
        this.country = country;
        return this;
    }

    public String getCity() {
        return city;
    }

    public ContactInfo withCity(String city) {
        this.city = city;
        return this;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public ContactInfo withCountryCode(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public String getCityCode() {
        return cityCode;
    }

    public ContactInfo withCityCode(String cityCode) {
        this.cityCode = cityCode;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public ContactInfo withPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getPhoneExt() {
        return phoneExt;
    }

    public ContactInfo withPhoneExt(String phoneExt) {
        this.phoneExt = phoneExt;
        return this;
    }

    public String getCompanyName() {
        return companyName;
    }

    public ContactInfo withCompanyName(String companyName) {
        this.companyName = companyName;
        return this;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public ContactInfo withContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
        return this;
    }

    public String getWorkTime() {
        return workTime;
    }

    public ContactInfo withWorkTime(String workTime) {
        this.workTime = workTime;
        return this;
    }

    public String getStreet() {
        return street;
    }

    public ContactInfo withStreet(String street) {
        this.street = street;
        return this;
    }

    public String getHouse() {
        return house;
    }

    public ContactInfo withHouse(String house) {
        this.house = house;
        return this;
    }

    public String getBuild() {
        return build;
    }

    public ContactInfo withBuild(String build) {
        this.build = build;
        return this;
    }

    public String getApart() {
        return apart;
    }

    public ContactInfo withApart(String apart) {
        this.apart = apart;
        return this;
    }

    public String getMetro() {
        return metro;
    }

    public ContactInfo withMetro(String metro) {
        this.metro = metro;
        return this;
    }

    public String getAutoPoint() {
        return autoPoint;
    }

    public ContactInfo withAutoPoint(String autoPoint) {
        this.autoPoint = autoPoint;
        return this;
    }

    public String getAutoBound() {
        return autoBound;
    }

    public ContactInfo withAutoBound(String autoBound) {
        this.autoBound = autoBound;
        return this;
    }

    public String getAutoPrecision() {
        return autoPrecision;
    }

    public ContactInfo withAutoPrecision(String autoPrecision) {
        this.autoPrecision = autoPrecision;
        return this;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public ContactInfo withContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
        return this;
    }

    public String getIMClient() {
        return IMClient;
    }

    public ContactInfo withIMClient(String IMClient) {
        this.IMClient = IMClient;
        return this;
    }

    public String getIMLogin() {
        return IMLogin;
    }

    public ContactInfo withIMLogin(String IMLogin) {
        this.IMLogin = IMLogin;
        return this;
    }

    public String getExtraMessage() {
        return extraMessage;
    }

    public ContactInfo withExtraMessage(String extraMessage) {
        this.extraMessage = extraMessage;
        return this;
    }

    public OrgDetails getOrgDetails() {
        return orgDetails;
    }

    public ContactInfo withOrgDetails(OrgDetails orgDetails) {
        this.orgDetails = orgDetails;
        return this;
    }

    public String getOGRN() {
        return OGRN;
    }

    public ContactInfo withOGRN(String OGRN) {
        this.OGRN = OGRN;
        return this;
    }

    public void setOGRN(String OGRN) {
        this.OGRN = OGRN;
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

    public void setAutoPoint(String autoPoint) {
        this.autoPoint = autoPoint;
    }

    public void setAutoBound(String autoBound) {
        this.autoBound = autoBound;
    }

    public void setAutoPrecision(String autoPrecision) {
        this.autoPrecision = autoPrecision;
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

    public void setOrgDetails(OrgDetails orgDetails) {
        this.orgDetails = orgDetails;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}