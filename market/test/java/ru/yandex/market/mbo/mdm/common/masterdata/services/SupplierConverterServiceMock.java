package ru.yandex.market.mbo.mdm.common.masterdata.services;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.RealConverter;

/**
 * @author moskovkin@yandex-team.ru
 * @since 11.06.19
 */
public class SupplierConverterServiceMock implements SupplierConverterService {
    public static final int BERU_ID = 465852;
    public static final int BERU_BUSINESS_ID = 924574;

    private Map<ShopSkuKey, ShopSkuKey> internalToExternal = new HashMap<>();
    private Map<ShopSkuKey, ShopSkuKey> externalToInternal = new HashMap<>();

    public void addInternalToExternalMapping(ShopSkuKey internal, ShopSkuKey external) {
        internalToExternal.put(internal, external);
        externalToInternal.put(external, internal);
    }

    @Override
    public ShopSkuKey convertRealToInternal(ShopSkuKey key) {
        return externalToInternal.getOrDefault(key, key);
    }

    @Override
    public ShopSkuKey convertInternalToReal(ShopSkuKey key) {
        return internalToExternal.getOrDefault(key, key);
    }

    @Nullable
    @Override
    public String getRealSupplierId(int internalSupplierId) {
        return internalToExternal.entrySet().stream()
            .filter(e -> e.getKey().getSupplierId() == internalSupplierId)
            .map(e -> RealConverter.getRealSupplierId(e.getValue().getShopSku()))
            .findFirst().orElse(null);
    }

    @Override
    public boolean isRealSupplierId(int internalSupplierId) {
        return internalToExternal.entrySet().stream()
            .anyMatch(e -> e.getKey().getSupplierId() == internalSupplierId);
    }

    @Override
    public int getBeruId() {
        return BERU_ID;
    }

    @Override
    public void clearCache() {
    }
}
