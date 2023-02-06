package ru.yandex.autotests.direct.cmd.data.excel;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/*
* todo javadoc
*/
public class ImportCampXlsResponse {
    @SerializedName("svars_name")
    private String sVarsName;

    @SerializedName("cmd")
    private String cmd;

    @SerializedName("errors")
    private List<String> errors = new ArrayList<>();

    public String getsVarsName() {
        return sVarsName;
    }

    public ImportCampXlsResponse withsVarsName(String sVarsName) {
        this.sVarsName = sVarsName;
        return this;
    }

    public String getCmd() {
        return cmd;
    }

    public ImportCampXlsResponse withCmd(String cmd) {
        this.cmd = cmd;
        return this;
    }

    public List<String> getErrors() {
        return errors;
    }

    public ImportCampXlsResponse withErrors(List<String> errors) {
        this.errors = errors;
        return this;
    }
}