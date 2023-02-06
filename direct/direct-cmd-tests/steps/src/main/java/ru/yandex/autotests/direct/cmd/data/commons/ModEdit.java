package ru.yandex.autotests.direct.cmd.data.commons;

import com.google.gson.annotations.SerializedName;

import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.ModEditStatusshow;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.ModEditRecord;

public class ModEdit {

    @SerializedName("old")
    public String old;

    @SerializedName("new_")
    private String new_;

    @SerializedName("createtime")
    private String createtime;

    @SerializedName("statusShow")
    private String statusShow;

    public String getOld() {
        return old;
    }

    public ModEdit withOld(String old) {
        this.old = old;
        return this;
    }

    public String getNew_() {
        return new_;
    }

    public ModEdit withNew_(String new_) {
        this.new_ = new_;
        return this;
    }

    public String getCreatetime() {
        return createtime;
    }

    public ModEdit withCreatetime(String createtime) {
        this.createtime = createtime;
        return this;
    }

    public String getStatusShow() {
        return statusShow;
    }

    public ModEdit withStatusShow(String statusShow) {
        this.statusShow = statusShow;
        return this;
    }

    public ModEditRecord createModEditRecord() {
        ModEditRecord modEditRecord = new ModEditRecord();
        modEditRecord.setOld(old);
        modEditRecord.setNew(new_);
        if (statusShow != null) {
            modEditRecord.setStatusshow(ModEditStatusshow.valueOf(statusShow));
        }
        return modEditRecord;
    }
}
