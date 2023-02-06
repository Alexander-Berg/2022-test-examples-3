package ru.yandex.market.mbo.mdm.common.masterdata.services.business;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.MdmSskuGroup;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.MdmSskuKeyGroup;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmBusinessStage;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class MdmSskuGroupManagerMock implements MdmSskuGroupManager {

    private final Map<ShopSkuKey, MdmSskuKeyGroup> mdmSskuKeyGroupMap = new HashMap<>();

    @Override
    public List<MdmSskuGroup> findGroupsByKeys(Collection<ShopSkuKey> keys,
                                               MdmBusinessStage groupingStage,
                                               boolean retainOnlyEoxed) {
        return new ArrayList<>();
    }

    @Override
    public Map<ShopSkuKey, MdmSskuGroup> findSeparateGroupForEachKey(Collection<ShopSkuKey> keys,
                                                                     MdmBusinessStage groupingStage,
                                                                     boolean retainOnlyEoxed) {
        return new HashMap<>();
    }

    @Override
    public List<MdmSskuKeyGroup> findKeyGroupsByKeys(Collection<ShopSkuKey> keys,
                                                     MdmBusinessStage groupingStage,
                                                     boolean retailOnlyEoxed) {
        return new ArrayList<>();
    }

    @Override
    public List<MdmSskuKeyGroup> findKeyGroupsByKeys(Collection<ShopSkuKey> keys) {
        List<MdmSskuKeyGroup> mdmSskuKeyGroups = new ArrayList<>();
        for (ShopSkuKey key : keys) {
            if (mdmSskuKeyGroupMap.containsKey(key)) {
                mdmSskuKeyGroups.add(mdmSskuKeyGroupMap.get(key));
            }
        }
        return mdmSskuKeyGroups;
    }

    @Override
    public Map<ShopSkuKey, MdmSskuKeyGroup> findSeparateKeyGroupForEachKey(Collection<ShopSkuKey> keys,
                                                                           MdmBusinessStage groupingStage) {
        return new HashMap<>();
    }

    @Override
    public Map<ShopSkuKey, MdmBusinessStage> getBusinessEnableModes(Collection<ShopSkuKey> keys) {
        return new HashMap<>();
    }

    @Override
    public Map<ShopSkuKey, MdmSskuGroup> regroupByBusinessId(Collection<MdmSskuGroup> groups) {
        return new HashMap<>();
    }

    @Override
    public Map<ShopSkuKey, ShopSkuKey> businessifyKeys(Collection<ShopSkuKey> keys) {
        return new HashMap<>();
    }

    @Override
    public boolean isBusiness(int supplierId) {
        return false;
    }

    @Override
    public List<ShopSkuKey> retainOnlyMdmCompatibleSkuKeys(Collection<ShopSkuKey> shopSkuKeys) {
        return new ArrayList<>();
    }

    public Map<ShopSkuKey, MdmSskuKeyGroup> insert(ShopSkuKey shopSkuKey, MdmSskuKeyGroup mdmSskuKeyGroup) {
        mdmSskuKeyGroupMap.put(shopSkuKey, mdmSskuKeyGroup);
        return mdmSskuKeyGroupMap;
    }
}
