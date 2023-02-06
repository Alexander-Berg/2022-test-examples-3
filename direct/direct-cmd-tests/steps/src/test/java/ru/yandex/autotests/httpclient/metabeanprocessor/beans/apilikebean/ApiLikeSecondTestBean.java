package ru.yandex.autotests.httpclient.metabeanprocessor.beans.apilikebean;


public class ApiLikeSecondTestBean {

    private String campaignId;

    private String[] credentials;

    private String runningUnmoderated;

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public String[] getCredentials() {
        return credentials;
    }

    public void setCredentials(String[] credentials) {
        this.credentials = credentials;
    }

    public String getRunningUnmoderated() {
        return runningUnmoderated;
    }

    public void setRunningUnmoderated(String runningUnmoderated) {
        this.runningUnmoderated = runningUnmoderated;
    }
}
