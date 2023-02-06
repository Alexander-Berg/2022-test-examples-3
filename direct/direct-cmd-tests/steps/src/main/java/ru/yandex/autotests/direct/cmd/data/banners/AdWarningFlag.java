package ru.yandex.autotests.direct.cmd.data.banners;

import com.google.gson.annotations.SerializedName;

/*
* todo javadoc
*/
public enum AdWarningFlag {
    @SerializedName("project_declaration")
    PROJECT_DECLARATION,
    @SerializedName("alcohol")
    ALCOHOL;


    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
