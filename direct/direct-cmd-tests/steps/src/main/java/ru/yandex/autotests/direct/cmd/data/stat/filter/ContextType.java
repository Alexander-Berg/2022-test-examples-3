package ru.yandex.autotests.direct.cmd.data.stat.filter;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class ContextType {

    @SerializedName("eq")
    private ArrayList<String> eq = new ArrayList<String>();

    public ArrayList<String> getEq() {
        return eq;
    }

    public void setEq(ArrayList<String> eq) {
        this.eq = eq;
    }
}
