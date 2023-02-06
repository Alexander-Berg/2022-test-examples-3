package ru.yandex.autotests.direct.httpclient.data.sandbox;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class CurrentSandboxState {

    public String toJson() {
        return (new Gson()).toJson(this);
    }

    @SerializedName("master_token")
    private String masterToken;

    @SerializedName("role")
    private String role;

    public void setRole(String role) {
        this.role = role;
    }

    public void setMasterToken(String masterToken) {
        this.masterToken = masterToken;
    }

    public String getMasterToken() {
        return this.masterToken;
    }
}