package ru.yandex.market.mboc.common.datacamp.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampUnitedOffer;
import lombok.Setter;

import ru.yandex.market.mboc.common.datacamp.DataCampOfferUtil;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;

public class DatacampServiceMock implements DataCampService {
    Map<BusinessSkuKey, DataCampUnitedOffer.UnitedOffer> offerStorage = new HashMap<>();
    Map<BusinessSkuKey, DataCampUnitedOffer.UnitedOffer> alternativeOfferStorage = new HashMap<>();
    @Setter
    boolean useAlternative = false;

    @Override
    public List<DataCampUnitedOffer.UnitedOffer> getOffers(long businessId, long groupId, boolean force) {
        return offerStorage.values().stream()
            .filter(o -> o.getBasic().getIdentifiers().getBusinessId() == businessId &&
                DataCampOfferUtil.Lens.groupId.apply(o.getBasic())
                    .filter(id -> groupId == id).isPresent())
            .collect(Collectors.toList());
    }

    @Override
    public DataCampUnitedOffer.UnitedOffer getUnitedOffer(long businessId, String offerId) {
        DataCampUnitedOffer.UnitedOffer result = useAlternative
            ? alternativeOfferStorage.get(new BusinessSkuKey((int) businessId, offerId))
            : offerStorage.get(new BusinessSkuKey((int) businessId, offerId));
        return result == null ? null : DataCampOfferUtil.trimToMbocMode(result);
    }

    @Override
    public List<DataCampUnitedOffer.UnitedOffer> getUnitedOffersByBusinessSkuKeys(Collection<BusinessSkuKey> businessSkuKeys) {
        return businessSkuKeys.stream()
                .map(bsku -> getUnitedOffer(bsku.getBusinessId(), bsku.getShopSku()))
                .filter(Objects::nonNull)
                .map(DataCampOfferUtil::trimToMbocMode)
                .collect(Collectors.toList());
    }

    public void putOffer(DataCampUnitedOffer.UnitedOffer offer) {
        offerStorage.put(DataCampOfferUtil.extractExternalBusinessSkuKey(offer.getBasic()), offer);
    }

    public void putAlternativeStorage(DataCampUnitedOffer.UnitedOffer offer) {
        alternativeOfferStorage.put(DataCampOfferUtil.extractExternalBusinessSkuKey(offer.getBasic()), offer);
    }

    public void removeOffer(BusinessSkuKey key) {
        offerStorage.remove(key);
    }

    public void removeOffer(DataCampUnitedOffer.UnitedOffer offer) {
        offerStorage.remove(DataCampOfferUtil.extractExternalBusinessSkuKey(offer.getBasic()));
    }
}
