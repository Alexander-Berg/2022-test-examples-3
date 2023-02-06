package ru.yandex.market.mbo.mdm.common.datacamp;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Market.DataCamp.DataCampUnitedOffer;

import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class MdmDatacampServiceMock implements MdmDatacampService {

    private final Map<ShopSkuKey, Integer> importedOffersWithPriorities = new HashMap<>();

    @Override
    public void sendOffersToDatacamp(Collection<DataCampUnitedOffer.UnitedOffer> offers) {
    }

    @Override
    public List<DataCampUnitedOffer.UnitedOffer> getUnitedOffersFromDatacamp(int businessId,
                                                                             Collection<String> offerIds) {
        return List.of();
    }

    @Override
    public void importOffersFromDatacamp(int businessId, Collection<String> offerIds, int customPriority) {
        for (String offerId : offerIds) {
            importedOffersWithPriorities.put(new ShopSkuKey(businessId, offerId), customPriority);
        }
    }

    @Override
    public void importAnyOffersFromDatacamp(Collection<ShopSkuKey> businessOrServiceKeys, int customPriority) {
        businessOrServiceKeys.forEach(k -> importedOffersWithPriorities.put(k, customPriority));
    }

    public Map<ShopSkuKey, Integer> getImportedOffersWithPriorities() {
        return importedOffersWithPriorities;
    }
}
