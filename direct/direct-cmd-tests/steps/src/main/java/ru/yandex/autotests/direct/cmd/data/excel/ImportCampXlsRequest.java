package ru.yandex.autotests.direct.cmd.data.excel;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

/*
* todo javadoc
*/
public class ImportCampXlsRequest extends BasicDirectRequest {

    @SerializeKey("svars_name")
    private String sVarsName;

    @SerializeKey("destination_camp")
    private DestinationCamp destinationCamp;

    @SerializeKey("cid")
    private String cid;

    @SerializeKey("send_to_moderation")
    private Boolean sendToModeration;

    @SerializeKey("geo")
    private String geo;

    @SerializeKey("xls")
    private String xls;

    @SerializeKey("release_camp_lock")
    private Boolean releaseCampLock;

    @SerializeKey("dont_change_prices")
    private Boolean dontChangePrices;

    @SerializeKey("changes_minus_words")
    private ChangesMinusWords changesMinusWords;

    @SerializeKey("lost_banners")
    private String lostBanners;

    @SerializeKey("lost_phrases")
    private String lostPhrases;

    public String getsVarsName() {
        return sVarsName;
    }

    public ImportCampXlsRequest withsVarsName(String sVarsName) {
        this.sVarsName = sVarsName;
        return this;
    }

    public DestinationCamp getDestinationCamp() {
        return destinationCamp;
    }

    public ImportCampXlsRequest withDestinationCamp(DestinationCamp destinationCamp) {
        this.destinationCamp = destinationCamp;
        return this;
    }

    public String getCid() {
        return cid;
    }

    public ImportCampXlsRequest withCid(String cid) {
        this.cid = cid;
        return this;
    }

    public Boolean getSendToModeration() {
        return sendToModeration;
    }

    public ImportCampXlsRequest withSendToModeration(Boolean sendToModeration) {
        this.sendToModeration = sendToModeration;
        return this;
    }

    public String getGeo() {
        return geo;
    }

    public ImportCampXlsRequest withGeo(String geo) {
        this.geo = geo;
        return this;
    }

    public String getXls() {
        return xls;
    }

    public ImportCampXlsRequest withXls(String xls) {
        this.xls = xls;
        return this;
    }

    public Boolean getReleaseCampLock() {
        return releaseCampLock;
    }

    public ImportCampXlsRequest withReleaseCampLock(Boolean releaseCampLock) {
        this.releaseCampLock = releaseCampLock;
        return this;
    }

    public Boolean getDontChangePrices() {
        return dontChangePrices;
    }

    public ImportCampXlsRequest withDontChangePrices(Boolean dontChangePrices) {
        this.dontChangePrices = dontChangePrices;
        return this;
    }

    public String getLostBanners() {
        return lostBanners;
    }

    public ImportCampXlsRequest withLostBanners(String lostBanners) {
        this.lostBanners = lostBanners;
        return this;
    }

    public String getLostPhrases() {
        return lostPhrases;
    }

    public ImportCampXlsRequest withLostPhrases(String lostPhrases) {
        this.lostPhrases = lostPhrases;
        return this;
    }

    public enum DestinationCamp {
        NEW,
        OLD,
        OTHER;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

    public enum ChangesMinusWords {
        CHANGE,
        DO_NOT_CHANGE;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }
}
