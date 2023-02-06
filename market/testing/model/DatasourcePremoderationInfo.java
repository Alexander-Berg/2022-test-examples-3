package ru.yandex.market.core.testing.model;

import ru.yandex.market.core.testing.TestingInfo;

/**
 * @author mkasumov
 */
public class DatasourcePremoderationInfo {

    private TestingInfo testingInfo;

    private long datasourceId;
    private boolean needTesting;
    private boolean needPlacementButtonApprove;
    private boolean needQualityButtonApprove;
    private boolean needManagerApprove;
    private int attemptsLeft;

    public DatasourcePremoderationInfo(TestingInfo testingInfo) {
        this.testingInfo = testingInfo;
        if (testingInfo != null) {
            datasourceId = testingInfo.getDatasourceId();
        }
    }

    public TestingInfo getTestingInfo() {
        return testingInfo;
    }

    public long getDatasourceId() {
        return datasourceId;
    }

    public void setDatasourceId(long datasourceId) {
        this.datasourceId = datasourceId;
    }

    public boolean isNeedTesting() {
        return needTesting;
    }

    public void setNeedTesting(boolean needTesting) {
        this.needTesting = needTesting;
    }

    public boolean isNeedPlacementButtonApprove() {
        return needPlacementButtonApprove;
    }

    public void setNeedPlacementButtonApprove(boolean needPlacementButtonApprove) {
        this.needPlacementButtonApprove = needPlacementButtonApprove;
    }

    public boolean isNeedQualityButtonApprove() {
        return needQualityButtonApprove;
    }

    public void setNeedQualityButtonApprove(boolean needQualityButtonApprove) {
        this.needQualityButtonApprove = needQualityButtonApprove;
    }

    public boolean isNeedManagerApprove() {
        return needManagerApprove;
    }

    public void setNeedManagerApprove(boolean needManagerApprove) {
        this.needManagerApprove = needManagerApprove;
    }

    public int getAttemptsLeft() {
        return attemptsLeft;
    }

    public void setAttemptsLeft(int attemptsLeft) {
        this.attemptsLeft = attemptsLeft;
    }
}
