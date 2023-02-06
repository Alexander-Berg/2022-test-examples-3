package ru.yandex.market.deepmind.common;

import java.time.LocalDate;

import javax.annotation.Nullable;

import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.BlockReasonKey;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategoryAvailabilityMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuAvailabilityMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuStatus;

public class MskuServicesTestUtils {

    private MskuServicesTestUtils() {
    }

    public static MskuStatus nextMskuStatus(long mskuId,
                                            MskuStatusValue mskuStatusValue,
                                            Long seasonId,
                                            LocalDate npdDate) {
        var mskuStatus = new MskuStatus()
            .setMarketSkuId(mskuId)
            .setMskuStatus(mskuStatusValue)
            .setSeasonId(seasonId)
            .setNpdFinishDate(npdDate);
        if (mskuStatusValue == MskuStatusValue.NPD) {
            mskuStatus.setNpdStartDate(LocalDate.now());
        }
        return mskuStatus;
    }

    public static MskuStatus nextMskuStatus(long mskuId,
                                            MskuStatusValue mskuStatusValue,
                                            LocalDate inoutStart,
                                            LocalDate inoutFinish
    ) {
        var mskuStatus = new MskuStatus()
            .setMarketSkuId(mskuId)
            .setMskuStatus(mskuStatusValue)
            .setInoutStartDate(inoutStart)
            .setInoutFinishDate(inoutFinish);
        if (mskuStatusValue == MskuStatusValue.NPD) {
            mskuStatus.setNpdStartDate(LocalDate.now());
        }
        return mskuStatus;
    }

    public static MskuStatus nextMskuStatus(long mskuId,
                                            MskuStatusValue mskuStatusValue,
                                            @Nullable Long seasonId) {
        var mskuStatus = new MskuStatus()
            .setMarketSkuId(mskuId)
            .setMskuStatus(mskuStatusValue)
            .setSeasonId(seasonId);
        if (mskuStatusValue == MskuStatusValue.NPD) {
            mskuStatus.setNpdStartDate(LocalDate.now());
        }
        return mskuStatus;
    }

    public static MskuAvailabilityMatrix mskuMatrix(boolean available,
                                                    long mskuId,
                                                    long warehouseId) {
        return mskuMatrix(available, mskuId, warehouseId, null, null);
    }

    public static MskuAvailabilityMatrix mskuMatrix(boolean available,
                                                    long mskuId,
                                                    long warehouseId,
                                                    @Nullable String fromDate,
                                                    @Nullable String toDate) {
        return mskuMatrix(available, mskuId, warehouseId, null, null, null);
    }

    public static MskuAvailabilityMatrix mskuMatrix(boolean available,
                                                    long mskuId,
                                                    long warehouseId,
                                                    @Nullable String fromDate,
                                                    @Nullable String toDate,
                                                    @Nullable BlockReasonKey blockReasonKey) {
        return new MskuAvailabilityMatrix()
            .setAvailable(available)
            .setMarketSkuId(mskuId)
            .setWarehouseId(warehouseId)
            .setFromDate(fromDate != null ? LocalDate.parse(fromDate) : null)
            .setToDate(toDate != null ? LocalDate.parse(toDate) : null)
            .setCreatedLogin("test")
            .setBlockReasonKey(blockReasonKey);
    }


    public static CategoryAvailabilityMatrix nextCategoryAvailabilityMatrix(boolean available,
                                                                            long categoryId,
                                                                            long warehouseId) {
        return new CategoryAvailabilityMatrix()
            .setAvailable(available)
            .setCategoryId(categoryId)
            .setWarehouseId(warehouseId)
            .setCreatedLogin("test");
    }

    public static CategoryAvailabilityMatrix nextCategoryAvailabilityMatrix(boolean available,
                                                                            long categoryId,
                                                                            long warehouseId,
                                                                            BlockReasonKey blockReasonkKey) {
        return nextCategoryAvailabilityMatrix(available, categoryId, warehouseId).setBlockReasonKey(blockReasonkKey);
    }
}
