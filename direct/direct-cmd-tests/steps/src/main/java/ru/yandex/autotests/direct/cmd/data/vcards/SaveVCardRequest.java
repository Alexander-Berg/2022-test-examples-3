package ru.yandex.autotests.direct.cmd.data.vcards;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ContactInfo;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ListToCommaSeparatedStringSerializer;

import java.util.List;

public class SaveVCardRequest extends BasicDirectRequest {

    public static SaveVCardRequest fromContactInfo(ContactInfo contactInfo) {
        Gson gson = new Gson();
        SaveVCardRequest request = gson.fromJson(gson.toJson(contactInfo), SaveVCardRequest.class);
        if (contactInfo.getOrgDetails() != null) {
            request.withOrgDetailsId(String.valueOf(contactInfo.getOrgDetails().getOrgDetailsId()));
            request.withOgrn(String.valueOf(contactInfo.getOrgDetails().getOGRN()));
        }
        return request;
    }

    @SerializeKey("vcard_id")
    @SerializedName("vcard_id")
    private Long vCardId;

    @SerializeKey("cid")
    @SerializedName("cid")
    private Long campaignId;

    @SerializeKey("bids")
    @SerializeBy(ListToCommaSeparatedStringSerializer.class)
    @SerializedName("bids")
    private List<Long> bannerIds;

    @SerializeKey("country")
    @SerializedName("country")
    private String country;

    @SerializeKey("city")
    @SerializedName("city")
    private String city;

    @SerializeKey("country_code")
    @SerializedName("country_code")
    private String countryCode;

    @SerializeKey("city_code")
    @SerializedName("city_code")
    private String cityCode;

    @SerializeKey("phone")
    @SerializedName("phone")
    private String phone;

    @SerializeKey("ext")
    @SerializedName("ext")
    private String phoneExt;

    @SerializeKey("name")
    @SerializedName("name")
    private String companyName;

    @SerializeKey("contactperson")
    @SerializedName("contactperson")
    private String contactPerson;

    @SerializeKey("worktime")
    @SerializedName("worktime")
    private String workTime;

    @SerializeKey("street")
    @SerializedName("street")
    private String street;

    @SerializeKey("house")
    @SerializedName("house")
    private String house;

    @SerializeKey("build")
    @SerializedName("build")
    private String build;

    @SerializeKey("apart")
    @SerializedName("apart")
    private String apart;

    @SerializeKey("auto_point")
    @SerializedName("auto_point")
    private String autoPoint;

    @SerializeKey("auto_bounds")
    @SerializedName("auto_bounds")
    private String autoBounds;

    @SerializeKey("manual_point")
    @SerializedName("manual_point")
    private String manualPoint;

    @SerializeKey("manual_bounds")
    @SerializedName("manual_bounds")
    private String manualBounds;

    @SerializeKey("auto_precision")
    @SerializedName("auto_precision")
    private String autoPrecision;

    @SerializeKey("contact_email")
    @SerializedName("contact_email")
    private String contactEmail;

    @SerializeKey("im_client")
    @SerializedName("im_client")
    private String IMClient;

    @SerializeKey("im_login")
    @SerializedName("im_login")
    private String IMLogin;

    @SerializeKey("extra_message")
    @SerializedName("extra_message")
    private String extraMessage;

    @SerializeKey("org_details_id")
    @SerializedName("org_details_id")
    private String orgDetailsId;

    @SerializeKey("ogrn")
    @SerializedName("ogrn")
    private String ogrn;

    public Long getVCardId() {
        return vCardId;
    }

    public SaveVCardRequest withVCardId(Long vcardId) {
        this.vCardId = vcardId;
        return this;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public SaveVCardRequest withCampaignId(Long campaignId) {
        this.campaignId = campaignId;
        return this;
    }

    public List<Long> getBannerIds() {
        return bannerIds;
    }

    public SaveVCardRequest withBannerIds(List<Long> bannerIds) {
        this.bannerIds = bannerIds;
        return this;
    }

    public String getCountry() {
        return country;
    }

    public SaveVCardRequest withCountry(String country) {
        this.country = country;
        return this;
    }

    public String getCity() {
        return city;
    }

    public SaveVCardRequest withCity(String city) {
        this.city = city;
        return this;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public SaveVCardRequest withCountryCode(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public String getCityCode() {
        return cityCode;
    }

    public SaveVCardRequest withCityCode(String cityCode) {
        this.cityCode = cityCode;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public SaveVCardRequest withPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getPhoneExt() {
        return phoneExt;
    }

    public SaveVCardRequest withPhoneExt(String phoneExt) {
        this.phoneExt = phoneExt;
        return this;
    }

    public String getCompanyName() {
        return companyName;
    }

    public SaveVCardRequest withCompanyName(String companyName) {
        this.companyName = companyName;
        return this;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public SaveVCardRequest withContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
        return this;
    }

    public String getWorkTime() {
        return workTime;
    }

    public SaveVCardRequest withWorkTime(String workTime) {
        this.workTime = workTime;
        return this;
    }

    public String getStreet() {
        return street;
    }

    public SaveVCardRequest withStreet(String street) {
        this.street = street;
        return this;
    }

    public String getHouse() {
        return house;
    }

    public SaveVCardRequest withHouse(String house) {
        this.house = house;
        return this;
    }

    public String getBuild() {
        return build;
    }

    public SaveVCardRequest withBuild(String build) {
        this.build = build;
        return this;
    }

    public String getApart() {
        return apart;
    }

    public SaveVCardRequest withApart(String apart) {
        this.apart = apart;
        return this;
    }

    public String getAutoPoint() {
        return autoPoint;
    }

    public SaveVCardRequest withAutoPoint(String autoPoint) {
        this.autoPoint = autoPoint;
        return this;
    }

    public String getAutoBounds() {
        return autoBounds;
    }

    public SaveVCardRequest withAutoBounds(String autoBounds) {
        this.autoBounds = autoBounds;
        return this;
    }

    public String getManualPoint() {
        return manualPoint;
    }

    public SaveVCardRequest withManualPoint(String manualPoint) {
        this.manualPoint = manualPoint;
        return this;
    }

    public String getManualBounds() {
        return manualBounds;
    }

    public SaveVCardRequest withManualBounds(String manualBounds) {
        this.manualBounds = manualBounds;
        return this;
    }

    public String getAutoPrecision() {
        return autoPrecision;
    }

    public SaveVCardRequest withAutoPrecision(String autoPrecision) {
        this.autoPrecision = autoPrecision;
        return this;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public SaveVCardRequest withContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
        return this;
    }

    public String getIMClient() {
        return IMClient;
    }

    public SaveVCardRequest withIMClient(String IMClient) {
        this.IMClient = IMClient;
        return this;
    }

    public String getIMLogin() {
        return IMLogin;
    }

    public SaveVCardRequest withIMLogin(String IMLogin) {
        this.IMLogin = IMLogin;
        return this;
    }

    public String getExtraMessage() {
        return extraMessage;
    }

    public SaveVCardRequest withExtraMessage(String extraMessage) {
        this.extraMessage = extraMessage;
        return this;
    }

    public String getOrgDetailsId() {
        return orgDetailsId;
    }

    public SaveVCardRequest withOrgDetailsId(String orgDetailsId) {
        this.orgDetailsId = orgDetailsId;
        return this;
    }

    public String getOgrn() {
        return ogrn;
    }

    public SaveVCardRequest withOgrn(String ogrn) {
        this.ogrn = ogrn;
        return this;
    }
}
