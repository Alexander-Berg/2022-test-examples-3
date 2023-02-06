package ru.yandex.autotests.direct.httpclient.data.campaigns.copyCamp;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 23.04.15
 */
public class CopyCampRequestBean extends BasicDirectRequestParameters {

    @JsonPath(requestPath = "cid_from")
    private String cidFrom;

    @JsonPath(requestPath = "copy_archived")
    private String copyArchived;

    @JsonPath(requestPath = "copy_ctr")
    private String copyCtr;

    @JsonPath(requestPath = "copy_moderate_status")
    private String copyModerateStatus;

    @JsonPath(requestPath = "copy_phrase_status")
    private String copyPhraseStatus;

    @JsonPath(requestPath = "copy_stopped")
    private String copyStopped;

    @JsonPath(requestPath = "newlogin")
    private String newLogin;

    @JsonPath(requestPath = "oldlogin")
    private String oldLogin;

    @JsonPath(requestPath = "reason")
    private String reason;

    public String getCidFrom() {
        return cidFrom;
    }

    public void setCidFrom(String cidFrom) {
        this.cidFrom = cidFrom;
    }

    public String getCopyArchived() {
        return copyArchived;
    }

    public void setCopyArchived(String copyArchived) {
        this.copyArchived = copyArchived;
    }

    public String getCopyCtr() {
        return copyCtr;
    }

    public void setCopyCtr(String copyCtr) {
        this.copyCtr = copyCtr;
    }

    public String getCopyModerateStatus() {
        return copyModerateStatus;
    }

    public void setCopyModerateStatus(String copyModerateStatus) {
        this.copyModerateStatus = copyModerateStatus;
    }

    public String getCopyPhraseStatus() {
        return copyPhraseStatus;
    }

    public void setCopyPhraseStatus(String copyPhraseStatus) {
        this.copyPhraseStatus = copyPhraseStatus;
    }

    public String getCopyStopped() {
        return copyStopped;
    }

    public void setCopyStopped(String copyStopped) {
        this.copyStopped = copyStopped;
    }

    public String getNewLogin() {
        return newLogin;
    }

    public void setNewLogin(String newLogin) {
        this.newLogin = newLogin;
    }

    public String getOldLogin() {
        return oldLogin;
    }

    public void setOldLogin(String oldLogin) {
        this.oldLogin = oldLogin;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
