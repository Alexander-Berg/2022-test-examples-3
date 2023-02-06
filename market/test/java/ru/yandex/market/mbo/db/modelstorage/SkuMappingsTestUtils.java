package ru.yandex.market.mbo.db.modelstorage;

import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.Mapping;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.MappingUpdateError;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.MappingUpdateStatus;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.OfferMappingDestination;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.SupplierOffer;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.SupplierType;

/**
 * @author anmalysh
 */
public class SkuMappingsTestUtils {

    private SkuMappingsTestUtils() {

    }

    public static SupplierOffer createMapping(
        int supplierId, String supplierSkuId, long skuId, SupplierType supplierType
    ) {
        Mapping mapping = new Mapping();
        mapping.setSkuId(skuId);
        Mapping loadedMapping = new Mapping();
        loadedMapping.setSkuId(skuId);

        SupplierOffer result = new SupplierOffer();
        result.setBusinessId(supplierId);
        result.setShopSkuId(supplierSkuId);
        result.setApprovedMapping(mapping);
        result.setLoadedApprovedMapping(loadedMapping);
        result.setSupplierType(supplierType);
        result.setDestination(OfferMappingDestination.BLUE);
        return result;
    }

    public static SupplierOffer createMapping(int supplierId, String supplierSkuId, long skuId) {
        return createMapping(supplierId, supplierSkuId, skuId, null);
    }

    public static MappingUpdateStatus createStatus(long initialSku,
                                                   SupplierOffer mapping,
                                                   MappingUpdateError.ErrorKind errorKind) {
        SupplierOffer newMapping = mapping.copyForUpdate();
        MappingUpdateStatus status = new MappingUpdateStatus();
        status.setSupplierId((int) mapping.getBusinessId());
        status.setSupplierSkuId(mapping.getShopSkuId());
        status.setCurrentOffer(newMapping);
        status.setStatus(errorKind == MappingUpdateError.ErrorKind.NONE ?
            MappingUpdateStatus.Status.OK : MappingUpdateStatus.Status.ERROR);
        if (status.isFailure()) {
            newMapping.getApprovedMapping().setSkuId(initialSku);
        } else {
            newMapping.getApprovedMapping().setSkuId(mapping.getApprovedMapping().getSkuId());
        }
        return status;
    }
}
