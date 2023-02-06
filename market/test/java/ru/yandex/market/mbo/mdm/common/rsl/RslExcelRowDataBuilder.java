package ru.yandex.market.mbo.mdm.common.rsl;

import java.time.LocalDate;

/**
 * @author dmserebr
 * @date 26/11/2019
 */
public final class RslExcelRowDataBuilder {
    private long categoryId;
    private String categoryName;
    private Long mskuId;
    private String mskuTitle;
    private Integer supplierId;
    private String shopSku;
    private Integer inRslDays;
    private Integer inRslPercents;
    private Integer outRslDays;
    private Integer outRslPercents;
    private LocalDate startDate;
    private boolean deleteFlag;
    private int rowNumber;

    private RslExcelRowDataBuilder() {
    }

    public static RslExcelRowDataBuilder start() {
        return new RslExcelRowDataBuilder();
    }

    public RslExcelRowDataBuilder categoryId(long categoryId) {
        this.categoryId = categoryId;
        return this;
    }

    public RslExcelRowDataBuilder categoryName(String categoryName) {
        this.categoryName = categoryName;
        return this;
    }

    public RslExcelRowDataBuilder mskuId(Long mskuId) {
        this.mskuId = mskuId;
        return this;
    }

    public RslExcelRowDataBuilder mskuTitle(String mskuTitle) {
        this.mskuTitle = mskuTitle;
        return this;
    }

    public RslExcelRowDataBuilder supplierId(Integer supplierId) {
        this.supplierId = supplierId;
        return this;
    }

    public RslExcelRowDataBuilder shopSku(String shopSku) {
        this.shopSku = shopSku;
        return this;
    }

    public RslExcelRowDataBuilder inRslDays(Integer inRslDays) {
        this.inRslDays = inRslDays;
        return this;
    }

    public RslExcelRowDataBuilder inRslPercents(Integer inRslPercents) {
        this.inRslPercents = inRslPercents;
        return this;
    }

    public RslExcelRowDataBuilder outRslDays(Integer outRslDays) {
        this.outRslDays = outRslDays;
        return this;
    }

    public RslExcelRowDataBuilder outRslPercents(Integer outRslPercents) {
        this.outRslPercents = outRslPercents;
        return this;
    }

    public RslExcelRowDataBuilder startDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    public RslExcelRowDataBuilder deleteFlag(boolean deleteFlag) {
        this.deleteFlag = deleteFlag;
        return this;
    }

    public RslExcelRowDataBuilder rowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
        return this;
    }

    public RslExcelRowData build() {
        RslExcelRowData rslExcelRowData = new RslExcelRowData();
        rslExcelRowData.setCategoryId(categoryId);
        rslExcelRowData.setCategoryName(categoryName);
        rslExcelRowData.setMskuId(mskuId);
        rslExcelRowData.setMskuTitle(mskuTitle);
        rslExcelRowData.setSupplierId(supplierId);
        rslExcelRowData.setShopSku(shopSku);
        rslExcelRowData.setInRslDays(inRslDays);
        rslExcelRowData.setInRslPercents(inRslPercents);
        rslExcelRowData.setOutRslDays(outRslDays);
        rslExcelRowData.setOutRslPercents(outRslPercents);
        rslExcelRowData.setStartDate(startDate);
        rslExcelRowData.setDeleteFlag(deleteFlag);
        rslExcelRowData.setRowNumber(rowNumber);
        return rslExcelRowData;
    }
}
