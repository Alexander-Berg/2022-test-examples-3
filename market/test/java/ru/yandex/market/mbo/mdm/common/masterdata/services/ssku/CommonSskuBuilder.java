package ru.yandex.market.mbo.mdm.common.masterdata.services.ssku;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.ObjectUtils;

import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.ServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class CommonSskuBuilder {
    private final MdmParamCache cache;
    private final ShopSkuKey key;
    private boolean usedUp = false;
    private List<SskuParamValue> values = new ArrayList<>();
    private int addedParamsCount = 1;
    private int currentSupplierId;

    public CommonSskuBuilder(MdmParamCache cache, ShopSkuKey key) {
        this.cache = cache;
        this.key = key;
        this.currentSupplierId = key.getSupplierId();
    }

    public CommonSskuBuilder withVghBeforeInheritance(
        @Nullable Integer width,
        @Nullable Integer height,
        @Nullable Integer length,
        @Nullable Integer weightGross
    ) {
        if (ObjectUtils.firstNonNull(width, height, length, weightGross) == null) {
            return this;
        }
        int addedCount = 0;
        if (width != null) {
            this.with(KnownMdmParams.SSKU_WIDTH, width);
            ++addedCount;
        }
        if (height != null) {
            this.with(KnownMdmParams.SSKU_HEIGHT, height);
            ++addedCount;
        }
        if (length != null) {
            this.with(KnownMdmParams.SSKU_LENGTH, length);
            ++addedCount;
        }
        if (weightGross != null) {
            this.with(KnownMdmParams.SSKU_WEIGHT_GROSS, weightGross);
            ++addedCount;
        }
        addedParamsCount = addedCount;
        return this;
    }

    public CommonSskuBuilder withVghAfterInheritance(
        @Nullable Integer width,
        @Nullable Integer height,
        @Nullable Integer length,
        @Nullable Integer weightGross
    ) {
        if (ObjectUtils.firstNonNull(width, height, length, weightGross) == null) {
            return this;
        }
        int addedCount = 0;
        if (width != null) {
            this.with(KnownMdmParams.WIDTH, width);
            ++addedCount;
        }
        if (height != null) {
            this.with(KnownMdmParams.HEIGHT, height);
            ++addedCount;
        }
        if (length != null) {
            this.with(KnownMdmParams.LENGTH, length);
            ++addedCount;
        }
        if (weightGross != null) {
            this.with(KnownMdmParams.WEIGHT_GROSS, weightGross);
            ++addedCount;
        }
        addedParamsCount = addedCount;
        return this;
    }

    public CommonSskuBuilder withShelfLife(@Nullable TimeInUnits timeInUnits, @Nullable String comment) {
        addTimeWithUnit(
            KnownMdmParams.SHELF_LIFE,
            KnownMdmParams.SHELF_LIFE_UNIT,
            KnownMdmParams.SHELF_LIFE_COMMENT,
            timeInUnits, comment);
        return this;
    }

    public CommonSskuBuilder withShelfLife(int time, TimeInUnits.TimeUnit unit, @Nullable String comment) {
        addTimeWithUnit(
            KnownMdmParams.SHELF_LIFE,
            KnownMdmParams.SHELF_LIFE_UNIT,
            KnownMdmParams.SHELF_LIFE_COMMENT,
            new TimeInUnits(time, unit), comment);
        return this;
    }

    public CommonSskuBuilder withLifeTime(@Nullable TimeInUnits timeInUnits, @Nullable String comment) {
        addTimeWithUnit(
            KnownMdmParams.LIFE_TIME,
            KnownMdmParams.LIFE_TIME_UNIT,
            KnownMdmParams.LIFE_TIME_COMMENT,
            timeInUnits, comment);
        return this;
    }

    public CommonSskuBuilder withLifeTime(int time, TimeInUnits.TimeUnit unit, @Nullable String comment) {
        addTimeWithUnit(
            KnownMdmParams.LIFE_TIME,
            KnownMdmParams.LIFE_TIME_UNIT,
            KnownMdmParams.LIFE_TIME_COMMENT,
            new TimeInUnits(time, unit), comment);
        return this;
    }

    public CommonSskuBuilder withGuaranteePeriod(@Nullable TimeInUnits timeInUnits, @Nullable String comment) {
        addTimeWithUnit(
            KnownMdmParams.GUARANTEE_PERIOD,
            KnownMdmParams.GUARANTEE_PERIOD_UNIT,
            KnownMdmParams.GUARANTEE_PERIOD_COMMENT,
            timeInUnits, comment);
        return this;
    }

    public CommonSskuBuilder withGuaranteePeriod(int time, TimeInUnits.TimeUnit unit, @Nullable String comment) {
        addTimeWithUnit(
            KnownMdmParams.GUARANTEE_PERIOD,
            KnownMdmParams.GUARANTEE_PERIOD_UNIT,
            KnownMdmParams.GUARANTEE_PERIOD_COMMENT,
            new TimeInUnits(time, unit), comment);
        return this;
    }

    public CommonSskuBuilder withVat(VatRate rate) {
        SskuParamValue value = basicValue(KnownMdmParams.VAT);
        MdmParamOption option = KnownMdmParams.VAT_RATES.inverse().get(rate);
        value.setOption(option);
        values.add(value);
        return this;
    }

    public CommonSskuBuilder with(long mdmParamId, String... raws) {
        SskuParamValue value = basicValue(mdmParamId);
        value.setStrings(List.of(raws));
        values.add(value);
        return this;
    }

    public CommonSskuBuilder with(long mdmParamId, Long... raws) {
        SskuParamValue value = basicValue(mdmParamId);
        value.setNumerics(List.of(raws).stream().map(BigDecimal::valueOf).collect(Collectors.toList()));
        values.add(value);
        return this;
    }

    public CommonSskuBuilder with(long mdmParamId, Integer... raws) {
        SskuParamValue value = basicValue(mdmParamId);
        value.setNumerics(List.of(raws).stream().map(BigDecimal::valueOf).collect(Collectors.toList()));
        values.add(value);
        return this;
    }

    public CommonSskuBuilder with(long mdmParamId, BigDecimal... raws) {
        SskuParamValue value = basicValue(mdmParamId);
        value.setNumerics(List.of(raws));
        values.add(value);
        return this;
    }

    public CommonSskuBuilder with(long mdmParamId, Boolean... raws) {
        SskuParamValue value = basicValue(mdmParamId);
        value.setBools(List.of(raws));
        values.add(value);
        return this;
    }

    public CommonSskuBuilder with(long mdmParamId, MdmParamOption... raws) {
        SskuParamValue value = basicValue(mdmParamId);
        value.setOptions(List.of(raws));
        values.add(value);
        return this;
    }

    public CommonSskuBuilder customized(Consumer<SskuParamValue> customizer) {
        for (int i = values.size() - addedParamsCount; i < values.size(); ++i) {
            customizer.accept(values.get(i));
        }
        return this;
    }

    public CommonSskuBuilder startServiceValues(int supplierId) {
        this.currentSupplierId = supplierId;
        return this;
    }

    public CommonSskuBuilder endServiceValues() {
        this.currentSupplierId = key.getSupplierId();
        return this;
    }

    public CommonSsku build() {
        usedUp = true;
        List<SskuParamValue> ownedParams = values.stream()
            .filter(v -> v.getShopSkuKey().equals(key))
            .collect(Collectors.toList());
        Map<Integer, List<SskuParamValue>> serviceSplit = values.stream()
            .collect(Collectors.groupingBy(v -> v.getShopSkuKey().getSupplierId()));
        serviceSplit.remove(key.getSupplierId());

        var ssku = new CommonSsku(key).setBaseValues(ownedParams);
        serviceSplit.forEach(ssku::putServiceValues);
        return ssku;
    }

    public ServiceSsku buildSupplierOnly() {
        usedUp = true;
        List<SskuParamValue> ownedParams = values.stream()
            .filter(v -> v.getShopSkuKey().equals(key))
            .collect(Collectors.toList());
        Map<Integer, List<SskuParamValue>> serviceSplit = values.stream()
            .collect(Collectors.groupingBy(v -> v.getShopSkuKey().getSupplierId()));
        serviceSplit.remove(key.getSupplierId());
        if (!serviceSplit.isEmpty()) {
            throw new IllegalStateException("Trying to build plain old SSKU, yet there are service-specific values");
        }
        return (ServiceSsku) new ServiceSsku(key).addParamValues(ownedParams);
    }

    private SskuParamValue basicValue(long mdmParamId) {
        if (usedUp) {
            throw new IllegalStateException("Please do not use the same builder twice.");
        }
        SskuParamValue value = new SskuParamValue().setShopSkuKey(new ShopSkuKey(currentSupplierId, key.getShopSku()));
        value.setMdmParamId(mdmParamId);
        value.setXslName(cache.get(mdmParamId).getXslName());
        value.setMasterDataSourceType(MasterDataSourceType.AUTO);
        addedParamsCount = 1;
        return value;
    }

    private void addTimeWithUnit(long timeParamId,
                                 long unitParamId,
                                 long commentParamId,
                                 @Nullable TimeInUnits timeInUnits,
                                 @Nullable String comment) {
        int added = 0;
        if (timeInUnits != null) {
            SskuParamValue value = basicValue(timeParamId);
            if (timeParamId == KnownMdmParams.SHELF_LIFE) {
                value.setNumeric(BigDecimal.valueOf(timeInUnits.getTime()));
            } else {
                value.setString(String.valueOf(timeInUnits.getTime()));
            }

            SskuParamValue unit = basicValue(unitParamId);
            long mdmOptionId = KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(timeInUnits.getUnit());
            MdmParamOption option = cache.get(unitParamId).getExternals().getOption(mdmOptionId);
            unit.setOption(option);
            values.add(value);
            values.add(unit);
            added += 2;
        }
        if (comment != null) {
            SskuParamValue value = basicValue(commentParamId);
            value.setString(comment);
            values.add(value);
            added += 1;
        }
        addedParamsCount = added;
    }
}
