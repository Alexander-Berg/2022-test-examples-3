package ru.yandex.autotests.direct.httpclient.data.firsthelp;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.data.banners.PhraseSaveParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 12.05.15
 */
public class AcceptOptimizeStep2RequestBean extends BasicDirectRequestParameters {

    @JsonPath(requestPath = "archOld")
    private String archOld;

    @JsonPath(requestPath = "cid")
    private String cid;

    @JsonPath(requestPath = "optimize_request_id")
    private String optimizeRequestId;

    @JsonPath(requestPath = "stopOld")
    private String stopOld;

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

    public String getArchOld() {
        return archOld;
    }

    public void setArchOld(String archOld) {
        this.archOld = archOld;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getOptimizeRequestId() {
        return optimizeRequestId;
    }

    public void setOptimizeRequestId(String optimizeRequestId) {
        this.optimizeRequestId = optimizeRequestId;
    }

    public String getStopOld() {
        return stopOld;
    }

    public void setStopOld(String stopOld) {
        this.stopOld = stopOld;
    }
}
