package ru.yandex.market.tsum.clients.kombat;

import java.util.List;

public class TestReportResultResponse {
    private String status;
    private int progress;
    private String error;
    private String releaseVersion;
    private String nextBattleId;
    private List<String> degradationReasons;
    private boolean success;

    public boolean isReady() {
        return status.equals("complete") || getCanceled();
    }

    public int getProgress() {
        return progress;
    }

    public String getError() {
        return error;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    public String getNextBattleId() {
        return nextBattleId;
    }

    public boolean getCanceled() {
        return status.equals("cancel");
    }

    public List<String> getDegradationReasons() {
        return degradationReasons;
    }

    public boolean getSuccess() {
        return success;
    }
}
