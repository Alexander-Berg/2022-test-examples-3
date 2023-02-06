package ru.yandex.autotests.direct.cmd.data.excel;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/*
* todo javadoc
*/
public class PreImportCampXlsResponse {

    @SerializedName("warnings")
    private List<String> warnings = new ArrayList<>();

    @SerializedName("errors")
    private List<String> errors = new ArrayList<>();

    @SerializedName("mediaType")
    private String mediaType;

    @SerializedName("from_cid")
    private String fromCid;

    @SerializedName("geo_errors")
    private List<Object> geoErrors = new ArrayList<Object>();

    @SerializedName("camp_comments")
    private List<String> campComments = new ArrayList<String>();

    @SerializedName("camp_name")
    private String campName;

    @SerializedName("cid")
    private String cid;

    @SerializedName("has_empty_geo")
    private Integer hasEmptyGeo;

    @SerializedName("has_oversized_banners")
    private Integer hasOversizedBanners;

    @SerializedName("parse_warnings_for_exists_camp")
    private ParseWarningsForExistsCampModel parseWarningsForExistsCamp;

    @SerializedName("svars_name")
    private String sVarsName;

    public List<String> getWarnings() {
        return warnings;
    }

    public PreImportCampXlsResponse withWarnings(List<String> warnings) {
        this.warnings = warnings;
        return this;
    }

    public List<String> getErrors() {
        return errors;
    }

    public PreImportCampXlsResponse withErrors(List<String> errors) {
        this.errors = errors;
        return this;
    }

    public String getMediaType() {
        return mediaType;
    }

    public PreImportCampXlsResponse withMediaType(String mediaType) {
        this.mediaType = mediaType;
        return this;
    }

    public String getFromCid() {
        return fromCid;
    }

    public PreImportCampXlsResponse withFromCid(String fromCid) {
        this.fromCid = fromCid;
        return this;
    }

    public List<Object> getGeoErrors() {
        return geoErrors;
    }

    public PreImportCampXlsResponse withGeoErrors(List<Object> geoErrors) {
        this.geoErrors = geoErrors;
        return this;
    }

    public List<String> getCampComments() {
        return campComments;
    }

    public PreImportCampXlsResponse withCampComments(List<String> campComments) {
        this.campComments = campComments;
        return this;
    }

    public String getCampName() {
        return campName;
    }

    public PreImportCampXlsResponse withCampName(String campName) {
        this.campName = campName;
        return this;
    }

    public String getCid() {
        return cid;
    }

    public PreImportCampXlsResponse withCid(String cid) {
        this.cid = cid;
        return this;
    }

    public Integer getHasEmptyGeo() {
        return hasEmptyGeo;
    }

    public PreImportCampXlsResponse withHasEmptyGeo(Integer hasEmptyGeo) {
        this.hasEmptyGeo = hasEmptyGeo;
        return this;
    }

    public Integer getHasOversizedBanners() {
        return hasOversizedBanners;
    }

    public PreImportCampXlsResponse withHasOversizedBanners(Integer hasOversizedBanners) {
        this.hasOversizedBanners = hasOversizedBanners;
        return this;
    }

    public ParseWarningsForExistsCampModel getParseWarningsForExistsCamp() {
        return parseWarningsForExistsCamp;
    }

    public PreImportCampXlsResponse withParseWarningsForExistsCamp
            (ParseWarningsForExistsCampModel parseWarningsForExistsCamp) {
        this.parseWarningsForExistsCamp = parseWarningsForExistsCamp;
        return this;
    }

    public String getsVarsName() {
        return sVarsName;
    }

    public PreImportCampXlsResponse withsVarsName(String sVarsName) {
        this.sVarsName = sVarsName;
        return this;
    }
}
