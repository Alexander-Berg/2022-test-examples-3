package ru.yandex.autotests.direct.cmd.data.campaigns;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class CopyCampRequest extends BasicDirectRequest {
    @SerializeKey("cid_from")
    private String cidFrom;

    @SerializeKey("copy_archived")
    private String copyArchived;

    @SerializeKey("copy_ctr")
    private String copyCtr;

    @SerializeKey("copy_moderate_status")
    private String copyModerateStatus;

    @SerializeKey("copy_phrase_status")
    private String copyPhraseStatus;

    @SerializeKey("copy_stopped")
    private String copyStopped;

    @SerializeKey("newlogin")
    private String newLogin;

    @SerializeKey("oldlogin")
    private String oldLogin;

    @SerializeKey("camp_status")
    private String campStatus;

    @SerializeKey("reason")
    private String reason;

    public String getCidFrom() {
        return cidFrom;
    }

    public void setCidFrom(String cidFrom) {
        this.cidFrom = cidFrom;
    }

    public void setNewLogin(String newLogin) {
        this.newLogin = newLogin;
    }

    public void setOldLogin(String oldLogin) {
        this.oldLogin = oldLogin;
    }

    public CopyCampRequest withCidFrom(String cidFrom) {
        this.cidFrom = cidFrom;
        return this;
    }

    public String getCopyArchived() {
        return copyArchived;
    }

    public CopyCampRequest withCopyArchived(String copyArchived) {
        this.copyArchived = copyArchived;
        return this;
    }

    public String getCopyCtr() {
        return copyCtr;
    }

    public CopyCampRequest withCopyCtr(String copyCtr) {
        this.copyCtr = copyCtr;
        return this;
    }

    public String getCopyModerateStatus() {
        return copyModerateStatus;
    }

    public CopyCampRequest withCopyModerateStatus(String copyModerateStatus) {
        this.copyModerateStatus = copyModerateStatus;
        return this;
    }

    public String getCopyPhraseStatus() {
        return copyPhraseStatus;
    }

    public CopyCampRequest withCopyPhraseStatus(String copyPhraseStatus) {
        this.copyPhraseStatus = copyPhraseStatus;
        return this;
    }

    public String getCopyStopped() {
        return copyStopped;
    }

    public CopyCampRequest withCopyStopped(String copyStopped) {
        this.copyStopped = copyStopped;
        return this;
    }

    public String getNewLogin() {
        return newLogin;
    }

    public CopyCampRequest withNewLogin(String newLogin) {
        this.newLogin = newLogin;
        return this;
    }

    public String getOldLogin() {
        return oldLogin;
    }

    public CopyCampRequest withOldLogin(String oldLogin) {
        this.oldLogin = oldLogin;
        return this;
    }

    public String getCampStatus() {
        return campStatus;
    }

    public CopyCampRequest withCampStatus(String campStatus) {
        this.campStatus = campStatus;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public CopyCampRequest withReason(String reason) {
        this.reason = reason;
        return this;
    }
}
