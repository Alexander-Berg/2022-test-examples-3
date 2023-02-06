package ru.yandex.market.markup2.utils.mboc;

import ru.yandex.market.mboc.http.SupplierOffer;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class MboCategoryMappingsServiceMock implements MboCategoryMappingsService {

    Map<ShopSkuKey, SupplierOffer.Offer> storage = new HashMap<>();

    public void putOffer(SupplierOffer.Offer offer) {
        storage.put(new ShopSkuKey(offer.getSupplierId(), offer.getShopSkuId()), offer);
    }

    @Override
    public List<SupplierOffer.Offer> searchMappingsByKeys(Collection<ShopSkuKey> skuKeys) {
        return skuKeys.stream()
            .map(storage::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
