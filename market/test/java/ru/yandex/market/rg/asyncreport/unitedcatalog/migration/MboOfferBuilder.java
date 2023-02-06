package ru.yandex.market.rg.asyncreport.unitedcatalog.migration;

import ru.yandex.market.mboc.http.SupplierOffer;

public class MboOfferBuilder {

    private final SupplierOffer.Offer.Builder offer;

    private MboOfferBuilder(SupplierOffer.Offer.Builder offer) {
        this.offer = offer;
    }

    public static MboOfferBuilder offerBuilder(int businessId, Integer serviceId, String offerId) {
        var offer = SupplierOffer.Offer.newBuilder()
                .setTitle("новая вещь")
                .setSupplierId(serviceId)
                .setShopSkuId(offerId)
                .setShopCategoryName("category_" + offerId)
                .setApprovedMapping(
                        SupplierOffer.Mapping.newBuilder()
                                .setSkuId(100L)
                                .setSkuName("товар")
                                .setCategoryId(90403)
                                .build()
                );
        return new MboOfferBuilder(offer);
    }

    public MboOfferBuilder withMapping(long skuMapping) {
        offer.setApprovedMapping(
                SupplierOffer.Mapping.newBuilder()
                        .setSkuId(skuMapping)
                        .setSkuName("товар")
                        .setCategoryId(90403)
                        .build()
        );
        return this;
    }

    public MboOfferBuilder withName(String name) {
        offer.setTitle(name);
        return this;
    }

    public MboOfferBuilder withCategory(String name) {
        offer.setShopCategoryName(name);
        return this;
    }

    public MboOfferBuilder withBarcode(String barcode) {
        offer.setBarcode(barcode);
        return this;
    }

    public MboOfferBuilder withVendorCode(String vendor, String vendorCode) {
        offer.setShopVendor(vendor);
        offer.setVendorCode(vendorCode);
        return this;
    }

    public SupplierOffer.Offer build() {
        return offer.build();
    }
}
