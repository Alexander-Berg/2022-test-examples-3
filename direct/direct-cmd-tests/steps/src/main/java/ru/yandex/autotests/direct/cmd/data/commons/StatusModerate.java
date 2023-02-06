package ru.yandex.autotests.direct.cmd.data.commons;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusmoderate;

/*
* todo javadoc
*/
public enum StatusModerate {

    @SerializedName("New")
    NEW("New"),
    @SerializedName("Sent")
    SENT("Sent"),
    @SerializedName("Sending")
    SENDING("Sending"),
    @SerializedName("Yes")
    YES("Yes"),
    @SerializedName("Ready")
    READY("Ready"),
    @SerializedName("No")
    NO("No"),
    @SerializedName("Rejected")
    REJECTED("Rejected");

    private String value;

    StatusModerate(String value) {
        this.value = value;
    }
    public String toString() {
        return this.value;
    }

    public BannersStatusmoderate convertToDbBannersStatusmoderate() {
        return BannersStatusmoderate.valueOf(this.value);
    }
}
