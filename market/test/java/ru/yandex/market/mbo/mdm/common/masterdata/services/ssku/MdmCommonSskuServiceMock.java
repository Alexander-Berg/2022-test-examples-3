package ru.yandex.market.mbo.mdm.common.masterdata.services.ssku;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamFilter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.MdmSskuSearchFilter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.ServiceSsku;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.ErrorInfo;

public class MdmCommonSskuServiceMock implements MdmCommonSskuService {

    private final Map<ShopSkuKey, CommonSsku> commonSskuMap = new HashMap<>();

    @Override
    public List<CommonSsku> find(MdmSskuSearchFilter mdmSskuSearchFilter) {
        return new ArrayList<>();
    }

    @Override
    public List<CommonSsku> get(Collection<ShopSkuKey> keys) {
        List<CommonSsku> commonSskus = new ArrayList<>();
        for (ShopSkuKey key : keys) {
            if (commonSskuMap.containsKey(key)) {
                commonSskus.add(commonSskuMap.get(key));
            }
        }
        return commonSskus;
    }

    @Override
    public List<CommonSsku> getAsIs(Collection<ShopSkuKey> keys) {
        return get(keys);
    }

    @Override
    public Map<ShopSkuKey, List<ErrorInfo>> update(List<CommonSsku> commonSskuList,
                                                   MdmParamFilter paramFilter,
                                                   MasterDataSourceType ignored) {
        for (CommonSsku commonSsku: commonSskuList) {
            CommonSsku commonSskuToSave = new CommonSsku().initFrom(commonSsku);
            List<SskuParamValue> filteredBaseValues = commonSsku.getBaseValues().stream()
                .filter(sskuParamValue -> paramFilter.allowed(sskuParamValue.getMdmParamId()))
                .collect(Collectors.toList());
            commonSskuToSave.setBaseValues(filteredBaseValues);
            for (ServiceSsku serviceSsku: commonSsku.getServiceSskus().values()) {
                ServiceSsku serviceSskuToSave = new ServiceSsku().initFrom(serviceSsku);
                List<SskuParamValue> filteredServiceValues = serviceSsku.getValues().stream()
                    .filter(sskuParamValue -> paramFilter.allowed(sskuParamValue.getMdmParamId()))
                    .collect(Collectors.toList());
                serviceSskuToSave.setParamValues(filteredServiceValues);
                commonSskuToSave.putServiceSsku(serviceSskuToSave);
            }
            commonSskuMap.put(commonSskuToSave.getKey(), commonSskuToSave);
        }
        return Map.of();
    }
}
