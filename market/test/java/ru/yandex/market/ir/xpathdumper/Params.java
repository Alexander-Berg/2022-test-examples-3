package ru.yandex.market.ir.xpathdumper;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;

class Params {
    private String cluster;
    private String ytToken;
    private String outputTable;
    private String resultTableFile;

    String scatRobotUsername;
    String scatRobotPassword;

    Params() {}

    public String getCluster() {
        return cluster;
    }


    public String getYtToken() {
        return ytToken;
    }

    public String getOutputTable() {
        return outputTable;
    }

    public String getResultTableFile() {
        return resultTableFile;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    @JsonProperty("yt_token")
    public void setYtToken(String ytToken) {
        this.ytToken = ytToken;
    }

    @JsonProperty("output_table")
    public void setOutputTable(String outputTable) {
        this.outputTable = outputTable;
    }

    @JsonProperty("result_table_file")
    public void setResultTableFile(String resultTableFile) {
        this.resultTableFile = resultTableFile;
    }

    @JsonProperty("scat.robot.username")
    public String getScatRobotUsername() {
        return scatRobotUsername;
    }

    public void setScatRobotUsername(String scatRobotUsername) {
        this.scatRobotUsername = scatRobotUsername;
    }

    public String getScatRobotPassword() {
        return scatRobotPassword;
    }
    @JsonProperty("scat.robot.password")
    public void setScatRobotPassword(String scatRobotPassword) {
        this.scatRobotPassword = scatRobotPassword;
    }
}
