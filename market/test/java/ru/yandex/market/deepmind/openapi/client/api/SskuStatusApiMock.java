package ru.yandex.market.deepmind.openapi.client.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import ru.yandex.market.deepmind.openapi.client.ApiException;
import ru.yandex.market.deepmind.openapi.client.model.ShopSkuStatus;
import ru.yandex.market.deepmind.openapi.client.model.UpdateSskuStatusRequest;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class SskuStatusApiMock extends SskuStatusApi {
    private final Map<ShopSkuKey, ShopSkuStatus> statusMap = new HashMap<>();

    public SskuStatusApiMock() {
        super(null);
    }

    @Override
    public List<ShopSkuStatus> getSskuStatus(List<ShopSkuKey> keys) throws ApiException {
        return keys.stream().map(statusMap::get).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public void updateSskuStatus(UpdateSskuStatusRequest request) throws ApiException {
        request.getKeys().forEach(key -> {
            var value = new ShopSkuStatus()
                .supplierId(key.getSupplierId())
                .shopSku(key.getShopSku())
                .status(request.getStatus());
            statusMap.put(key, value);
        });
    }
}
