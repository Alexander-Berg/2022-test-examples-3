package ru.yandex.market.deepmind.common.services.rule_engine;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;

import com.google.common.annotations.VisibleForTesting;

import ru.yandex.market.deepmind.common.availability.SeasonPeriodUtils;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.services.rule_engine.pojo.RECategory;
import ru.yandex.market.deepmind.common.services.rule_engine.pojo.REMskuStatus;
import ru.yandex.market.deepmind.common.services.rule_engine.pojo.REPurchasePrice;
import ru.yandex.market.deepmind.common.services.rule_engine.pojo.RESales;
import ru.yandex.market.deepmind.common.services.rule_engine.pojo.RESeason;
import ru.yandex.market.deepmind.common.services.rule_engine.pojo.RESskuStatus;
import ru.yandex.market.deepmind.common.services.rule_engine.pojo.REStockStorage;

public abstract class BaseRuleEngineServiceTest {

    protected Clock clock(String s) {
        return Clock.fixed(Instant.parse(s), ZoneId.systemDefault());
    }

    protected RESskuStatus ssku(int supplierId, String shopSku, REMskuStatus msku, OfferAvailability status) {
        return ssku(supplierId, shopSku, msku.getId(), status);
    }

    protected RESskuStatus ssku(int supplierId, String shopSku, long mskuId, OfferAvailability status) {
        return ssku(supplierId, shopSku, mskuId, status, SupplierType.THIRD_PARTY);
    }

    protected RESskuStatus ssku(int supplierId, String shopSku, REMskuStatus msku, OfferAvailability status,
                                SupplierType supplierType) {
        return ssku(supplierId, shopSku, msku.getId(), status, supplierType);
    }

    protected RESskuStatus ssku(int supplierId, String shopSku, long mskuId, OfferAvailability status,
                                SupplierType supplierType) {
        return new RESskuStatus().setSupplierId(supplierId).setShopSku(shopSku).setMskuId(mskuId)
            .setAvailability(status).setSupplierType(supplierType).setRawSupplierId(supplierId)
            .setStatusStartTime(Instant.now());
    }

    protected REMskuStatus msku(long mskuId, MskuStatusValue status, long categoryId) {
        return msku(mskuId, status, categoryId, false);
    }

    protected REMskuStatus msku(long mskuId, MskuStatusValue status, long categoryId, boolean modifiedByUser) {
        var msku = new REMskuStatus(mskuId).setStatus(status).setCategoryId(categoryId);
        msku.setModifiedByUser(modifiedByUser);
        if (status == MskuStatusValue.NPD) {
            msku.setNpdStartDate(LocalDate.now());
        }
        if (status == MskuStatusValue.IN_OUT) {
            msku.setInoutStartDate(LocalDate.now());
            msku.setInoutFinishDate(LocalDate.now());
        }
        return msku;
    }

    protected RECategory category(long categoryId) {
        return new RECategory(categoryId);
    }

    protected RECategory category(long categoryId, long seasonId) {
        return new RECategory(categoryId).setSeasonId(seasonId);
    }

    protected RESales sales(RESskuStatus ssku, long warehouseId, Instant lastSales) {
        return new RESales()
            .setSupplierId(ssku.getSupplierId())
            .setShopSku(ssku.getShopSku())
            .setMskuId(ssku.getMskuId())
            .setWarehouseId(warehouseId)
            .setLastSaleDateTime(lastSales);
    }

    protected RESeason season(long id, LocalDate startDate, LocalDate endDate) {
        var season = new RESeason(id);
        addPeriod(season, startDate, endDate);
        return season;
    }

    protected RESeason season(long id,
                              LocalDate startDate1, LocalDate endDate1,
                              LocalDate startDate2, LocalDate endDate2) {
        var season = new RESeason(id);
        addPeriod(season, startDate1, endDate1);
        addPeriod(season, startDate2, endDate2);
        return season;
    }

    @Deprecated
    @VisibleForTesting
    private void addPeriod(RESeason season, LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End (" + end + ") is before start (" + start + ") season: " + season);
        }
        season.addPeriod(SeasonPeriodUtils.toMmDD(start), SeasonPeriodUtils.toMmDD(end));
    }

    protected REStockStorage stockStorage(RESskuStatus ssku, long warehouseId, int fit) {
        var reStockStorage = new REStockStorage()
            .setSupplierId(ssku.getSupplierId())
            .setShopSku(ssku.getShopSku())
            .setMskuId(ssku.getMskuId())
            .setWarehouseId(warehouseId)
            .setFit(fit);
        if (fit == 0L) {
            reStockStorage.setFitDisappearTime(Instant.now());
        } else {
            reStockStorage.setFitAppearTime(Instant.now());
        }
        return reStockStorage;
    }
    protected REPurchasePrice purchasePrice(RESskuStatus ssku) {
        return new REPurchasePrice()
            .setMskuId(ssku.getMskuId())
            .setSupplierId(ssku.getSupplierId())
            .setShopSku(ssku.getShopSku())
            .setPrice(11L);
    }

    protected REPurchasePrice[] purchasePrice(RESskuStatus... sskus) {
        return Arrays.stream(sskus).map(this::purchasePrice).toArray(REPurchasePrice[]::new);
    }
}
