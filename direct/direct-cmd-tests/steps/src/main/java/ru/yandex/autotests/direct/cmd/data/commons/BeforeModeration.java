package ru.yandex.autotests.direct.cmd.data.commons;

import com.google.gson.annotations.SerializedName;

import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.ModEditRecord;

public class BeforeModeration {

    public static BeforeModeration fromModEditRecord(ModEditRecord modEditRecord) {
        BeforeModeration beforeModeration = new BeforeModeration();
        beforeModeration.setBid(modEditRecord.getId());
        beforeModeration.setTitle(modEditRecord.getOld().split("title: ")[1].replaceAll("\n", ""));
        beforeModeration.setBody(modEditRecord.getOld().split("title: ")[0]
                .replaceAll("--- \n body: ", "").replaceAll("\n ", ""));
        beforeModeration.setShowModEditNotice(modEditRecord.getStatusshow().getLiteral());
        return beforeModeration;
    }

    private Long bid;

    private String title;

    private String body;

    @SerializedName("edit_createtime")
    private String editCreateTime;

    private String showModEditNotice;

    public Long getBid() {
        return bid;
    }

    public void setBid(Long bid) {
        this.bid = bid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getEditCreateTime() {
        return editCreateTime;
    }

    public void setEditCreateTime(String editCreateTime) {
        this.editCreateTime = editCreateTime;
    }

    public String getShowModEditNotice() {
        return showModEditNotice;
    }

    public void setShowModEditNotice(String showModEditNotice) {
        this.showModEditNotice = showModEditNotice;
    }
}
