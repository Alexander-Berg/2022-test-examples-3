package ru.yandex.market.mboc.app.test;

import com.google.common.collect.ImmutableList;
import liquibase.util.StringUtils;

import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.model.cutoff.OfferCutoffInfo;
import ru.yandex.market.mboc.common.masterdata.services.cutoff.OfferCutoffTypeProvider;
import ru.yandex.market.mboc.common.notifications.model.data.catmans.CategoryOffersNeedActionData;
import ru.yandex.market.mboc.common.notifications.model.data.catmans.NewOffersData;

/**
 * @author prediger
 */
@SuppressWarnings("checkstyle:magicnumber")
class TestControllerUtils {

    private static final ImmutableList<OfferCutoffInfo> STATIC_CUTOFF_TYPES = ImmutableList.<OfferCutoffInfo>builder()
        .add(OfferCutoffTypeProvider.missManufacturerCountryError())
        .add(OfferCutoffTypeProvider.missShelfLifeError())
        .add(OfferCutoffTypeProvider.missLifeTimeError())
        .add(OfferCutoffTypeProvider.missGuaranteePeriodError())
        .add(OfferCutoffTypeProvider.invalidShelfLifeError(
            new TimeInUnits(1, TimeInUnits.TimeUnit.DAY), new TimeInUnits(180, TimeInUnits.TimeUnit.DAY)))
        .add(OfferCutoffTypeProvider.invalidLifeTimeError(
            new TimeInUnits(1, TimeInUnits.TimeUnit.YEAR), new TimeInUnits(10, TimeInUnits.TimeUnit.YEAR)))
        .add(OfferCutoffTypeProvider.invalidGuaranteePeriodError(
            new TimeInUnits(1, TimeInUnits.TimeUnit.MONTH), new TimeInUnits(12, TimeInUnits.TimeUnit.MONTH)))
        .build();

    private TestControllerUtils() {
    }

    static NewOffersData getCatManNewOffersData() {
        return new NewOffersData()
            .addOffer(1, new NewOffersData.SupplierInfo()
                .setId(1)
                .setName("Поставщик 1")
                .setOffersCount(1)
                .setCategoryNamesVararg("Категория 1"))
            .addOffer(2, new NewOffersData.SupplierInfo()
                .setId(2)
                .setName("Поставщик 2")
                .setOffersCount(5)
                .setCategoryNamesVararg("Категория 1", "Категория 2"))
            .addOffer(3, new NewOffersData.SupplierInfo()
                .setId(3)
                .setName("Поставщик 3")
                .setOffersCount(10)
                .setCategoryNamesVararg("Категория 3", "Категория 4", "Категория 5"));
    }

    static CategoryOffersNeedActionData getCatManNeedActionData() {
        return new CategoryOffersNeedActionData().setCategoryInfosVararg(
            new CategoryOffersNeedActionData.CategoryInfo()
                .setCategoryId(1)
                .setFullName("Категория 1/Категория 1_1/Категория 1_1_1")
                .setSupplierInfosVararg(
                    new CategoryOffersNeedActionData.CategorySupplierInfo()
                        .setName("Поставщик 1")
                        .setSupplierId(1)
                        .setNoAcceptanceStatusCount(1),
                    new CategoryOffersNeedActionData.CategorySupplierInfo()
                        .setName("Поставщик 2")
                        .setSupplierId(2)
                        .setNeedInfoCount(1)
                        .setSendToContentCount(1)),
            new CategoryOffersNeedActionData.CategoryInfo()
                .setCategoryId(2)
                .setFullName("Категория 1/Категория 1_2")
                .setSupplierInfosVararg(
                    new CategoryOffersNeedActionData.CategorySupplierInfo()
                        .setName("Поставщик 2")
                        .setSupplierId(2)
                        .setNoAcceptanceStatusCount(1),
                    new CategoryOffersNeedActionData.CategorySupplierInfo()
                        .setName("Поставщик 3")
                        .setSupplierId(3)
                        .setNoAcceptanceStatusCount(3)
                        .setSendToContentCount(4)),
            new CategoryOffersNeedActionData.CategoryInfo()
                .setCategoryId(2)
                .setFullName("Категория 2")
                .setSupplierInfosVararg(
                    new CategoryOffersNeedActionData.CategorySupplierInfo()
                        .setName("Поставщик 4")
                        .setSupplierId(4)
                        .setNoAcceptanceStatusCount(7)
                        .setNeedInfoCount(8)
                        .setSendToContentCount(9))
        );
    }

    static OfferCutoffInfo generateOfferCutoffInfoByTypeId(String typeId) {
        if (StringUtils.isEmpty(typeId)) {
            throw new IllegalArgumentException("type_id must be not empty");
        }

        for (OfferCutoffInfo cutoffInfo : STATIC_CUTOFF_TYPES) {
            if (typeId.equals(cutoffInfo.getTypeId())) {
                return cutoffInfo;
            }
        }

        OfferCutoffInfo documentCutoff = OfferCutoffTypeProvider.missRequiredDocumentError(
            QualityDocument.QualityDocumentType.CERTIFICATE_OF_CONFORMITY);

        if (typeId.equals(documentCutoff.getTypeId())) {
            return documentCutoff;
        }

        throw new IllegalArgumentException("Not found error for type_id: " + typeId);
    }
}
