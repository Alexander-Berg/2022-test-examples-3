package ru.yandex.autotests.direct.httpclient.data.sandbox;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * Created by proxeter (Nikolay Mulyar - proxeter@yandex-team.ru) on 19.05.2014.
 */
public class SandboxResponseBean {

    @SerializedName("current_sandbox_state")
    private CurrentSandboxState currentSandboxState;

    public CurrentSandboxState getCurrentSandboxState() {
        return currentSandboxState;
    }

    public static SandboxResponseBean readJson(String content) {
        return new Gson().fromJson(content, SandboxResponseBean.class);
    }

}
