package ru.yandex.market.deepmind.common.services.availability;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import ru.yandex.market.deepmind.common.availability.matrix.MatrixAvailability;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.services.switch_control.SwitchControlService;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 19.09.2019
 */
public class SwitchControlServiceMock extends SwitchControlService {
    private final Map<String, Object> values = new HashMap<>();
    private final Map<MatrixAvailability.Reason, ReasonEntry> reasons = new HashMap<>();

    public SwitchControlServiceMock setForceSkipMskuStatusConstraints(boolean value) {
        values.put(FORCE_SKIP_MSKU_STATUS_CONSTRAINTS, value);
        return this;
    }

    public SwitchControlServiceMock setForceSkipSupplierConstraints(boolean value) {
        values.put(FORCE_SKIP_SUPPLIER_DELIVERY_CONSTRAINTS, value);
        return this;
    }

    public SwitchControlServiceMock setForceSkipSskuConstraints(boolean value) {
        values.put(FORCE_SKIP_SSKU_CONSTRAINTS, value);
        return this;
    }

    public SwitchControlServiceMock setForceSkipMsku(MatrixAvailability.Reason reason, long mskuId, long warehouseId) {
        ReasonEntry entry = reasons.computeIfAbsent(reason, __ -> new ReasonEntry());
        entry.add(mskuId, warehouseId);
        return this;
    }

    public SwitchControlServiceMock setForceSkipShopSku(MatrixAvailability.Reason reason, ServiceOfferKey shopSkuKey,
                                                        long warehouseId) {
        ReasonEntry entry = reasons.computeIfAbsent(reason, __ -> new ReasonEntry());
        entry.add(shopSkuKey, warehouseId);
        return this;
    }

    public SwitchControlServiceMock setForceSkipShopSku(MatrixAvailability.Reason reason,
                                                        int supplierId, String shopSku, long warehouseId) {
        return setForceSkipShopSku(reason, new ServiceOfferKey(supplierId, shopSku), warehouseId);
    }

    @Nonnull
    @Override
    protected <T> T loadOrDefault(String key, @Nonnull T defaultValue, Class<T> clazz) {
        //noinspection unchecked
        return (T) values.getOrDefault(key, defaultValue);
    }

    @Nonnull
    @Override
    protected ReasonEntry load(@Nonnull MatrixAvailability.Reason reason) {
        return reasons.getOrDefault(reason, new ReasonEntry());
    }
}
