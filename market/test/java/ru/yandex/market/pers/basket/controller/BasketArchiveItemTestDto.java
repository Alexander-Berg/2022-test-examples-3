package ru.yandex.market.pers.basket.controller;

import ru.yandex.common.util.db.DbUtil;
import ru.yandex.market.pers.basket.model.BasketReferenceItem;
import ru.yandex.market.util.FormatUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class BasketArchiveItemTestDto {
    Long id;
    Long ownerId;
    Date delTime;
    Long referenceType;
    String referenceId;
    BasketReferenceItem data;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public Date getDelTime() {
        return delTime;
    }

    public void setDelTime(Date delTime) {
        this.delTime = delTime;
    }

    public Long getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(Long referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public BasketReferenceItem getData() {
        return data;
    }

    public void setData(BasketReferenceItem data) {
        this.data = data;
    }

    public static BasketArchiveItemTestDto valueOf(ResultSet rs, int idx) throws SQLException {
        BasketArchiveItemTestDto item = new BasketArchiveItemTestDto();
        item.setId(rs.getLong("id"));
        item.setOwnerId(rs.getLong("owner_id"));
        item.setDelTime(DbUtil.getUtilDate(rs,"del_time"));
        item.setReferenceType(rs.getLong("reference_type"));
        item.setReferenceId(rs.getString("reference_id"));
        item.setData(FormatUtils.fromJson(rs.getString("data"),BasketReferenceItem.class));

        return item;
    }
}
