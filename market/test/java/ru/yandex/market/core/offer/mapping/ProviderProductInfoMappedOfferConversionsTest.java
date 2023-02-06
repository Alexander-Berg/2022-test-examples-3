package ru.yandex.market.core.offer.mapping;

import java.util.Collections;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mboc.http.MboMappings;

import static ru.yandex.market.mdm.http.MasterDataProto.MasterDataInfo;
import static ru.yandex.market.mdm.http.MasterDataProto.ProviderProductMasterData;

@ParametersAreNonnullByDefault
class ProviderProductInfoMappedOfferConversionsTest {
    @Test
    void testProviderProductInfo() {
        ShopOffer shopOffer = new ShopOffer.Builder()
                .setTitle("Test Title")
                .setSupplierId(661)
                .addBarcode("barcode12345")
                .setCategoryName("Category/Name")
                .setDescription("Test Description")
                .setShopSku("ShopSKU1")
                .setVendor("Test Vendor")
                .setVendorCode("vendorCode12345")
                .addUrl("https://beru.ru/product/100324822646")
                .build();
        MboMappings.ProviderProductInfo productInfo =
                MappedOfferConversions.providerProductInfo(shopOffer, CampaignType.SUPPLIER, MarketEntityReference.marketSku(1235));
        MatcherAssert.assertThat(
                productInfo,
                Matchers.allOf(
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getBarcodeList,
                                Matchers.contains("barcode12345")),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getDescription,
                                Matchers.is("Test Description")),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getTitle,
                                Matchers.is("Test Title")),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getShopCategoryName,
                                Matchers.is("Category/Name")),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getShopSkuId,
                                Matchers.is("ShopSKU1")),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getShopId,
                                Matchers.is(661)),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getVendor,
                                Matchers.is("Test Vendor")),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getVendorCode,
                                Matchers.is("vendorCode12345")),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getMappingType,
                                Matchers.is(MboMappings.MappingType.SUPPLIER)),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getMarketSkuId,
                                Matchers.is(1235L)),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getUrlList,
                                Matchers.containsInAnyOrder("https://beru.ru/product/100324822646"))
                )
        );
    }

    @Test
    void testProviderProductInfoForShop() {
        ShopOffer shopOffer = new ShopOffer.Builder()
                .setTitle("Test Title")
                .setSupplierId(661)
                .addBarcode("barcode12345")
                .setCategoryName("Category/Name")
                .setDescription("Test Description")
                .setShopSku("ShopSKU1")
                .setVendor("Test Vendor")
                .setVendorCode("vendorCode12345")
                .addUrl("https://beru.ru/product/100324822646")
                .build();
        MboMappings.ProviderProductInfo productInfo =
                MappedOfferConversions.providerProductInfo(shopOffer, CampaignType.SHOP, MarketEntityReference.marketSku(1235));
        MatcherAssert.assertThat(
                productInfo,
                Matchers.allOf(
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getBarcodeList,
                                Matchers.contains("barcode12345")),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getDescription,
                                Matchers.is("Test Description")),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getTitle,
                                Matchers.is("Test Title")),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getShopCategoryName,
                                Matchers.is("Category/Name")),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getShopSkuId,
                                Matchers.is("ShopSKU1")),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getShopId,
                                Matchers.is(661)),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getVendor,
                                Matchers.is("Test Vendor")),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getVendorCode,
                                Matchers.is("vendorCode12345")),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getMappingType,
                                Matchers.is(MboMappings.MappingType.PRICE_COMPARISION)),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getMarketSkuId,
                                Matchers.is(1235L)),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getUrlList,
                                Matchers.containsInAnyOrder("https://beru.ru/product/100324822646"))
                )
        );
    }


    @Test
    void testProviderProductInfoWithMasterData() {
        TimePeriodWithUnits tenDays = TimePeriodWithUnits.ofDays(10);

        ShopOffer shopOffer = new ShopOffer.Builder()
                .setTitle("Test Title")
                .setSupplierId(661)
                .addBarcode("barcode12345")
                .setCategoryName("Category/Name")
                .setDescription("Test Description")
                .setShopSku("ShopSKU1")
                .setVendor("Test Vendor")
                .setVendorCode("vendorCode12345")
                .addUrl("https://beru.ru/product/100324822646")
                .setMasterData(new MasterData.Builder()
                        .setShelfLife(tenDays)
                        .setShelfLifeComment("ShelfLifeComment")
                        .addManufacturerCountries(Collections.singleton("Россия"))
                        .setMinShipment(100)
                        .build())
                .build();
        MboMappings.ProviderProductInfo productInfo =
                MappedOfferConversions.providerProductInfo(shopOffer, CampaignType.SHOP, MarketEntityReference.marketSku(1235));
        MatcherAssert.assertThat(
                productInfo,
                Matchers.allOf(
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getBarcodeList,
                                Matchers.contains("barcode12345")),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getDescription,
                                Matchers.is("Test Description")),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getTitle,
                                Matchers.is("Test Title")),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getShopCategoryName,
                                Matchers.is("Category/Name")),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getShopSkuId,
                                Matchers.is("ShopSKU1")),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getShopId,
                                Matchers.is(661)),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getVendor,
                                Matchers.is("Test Vendor")),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getVendorCode,
                                Matchers.is("vendorCode12345")),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getMappingType,
                                Matchers.is(MboMappings.MappingType.PRICE_COMPARISION)),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getMarketSkuId,
                                Matchers.is(1235L)),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getUrlList,
                                Matchers.containsInAnyOrder("https://beru.ru/product/100324822646")),
                        MbiMatchers.transformedBy(MboMappings.ProviderProductInfo::getMasterDataInfo,
                                MbiMatchers.<MasterDataInfo>newAllOfBuilder()
                                        .add(MasterDataInfo::hasShelfLifeWithUnits, true)
                                        .add(MasterDataInfo::getShelfLifeWithUnits, tenDays.getTime())
                                        .add(MasterDataInfo::hasShelfLifeComment, true)
                                        .add(MasterDataInfo::getShelfLifeComment, "ShelfLifeComment")
                                        .add(MasterDataInfo::hasGuaranteePeriodWithUnits, false)
                                        .add(MasterDataInfo::hasLifeTimeWithUnits, false)
                                        .add(
                                                MasterDataInfo::getProviderProductMasterData,
                                                MbiMatchers.<ProviderProductMasterData>newAllOfBuilder()
                                                        .add(ProviderProductMasterData::hasManufacturer, false)
                                                        .add(ProviderProductMasterData::getManufacturerCountryCount, 1)
                                                        .add(
                                                                ProviderProductMasterData::getManufacturerCountryList,
                                                                Collections.singletonList("Россия")
                                                        )
                                                        .add(ProviderProductMasterData::hasCustomsCommodityCode, false)
                                                        .add(ProviderProductMasterData::hasDeliveryTime, false)
                                                        .add(ProviderProductMasterData::hasQuantumOfSupply, false)
                                                        .add(ProviderProductMasterData::hasBoxCount, false)
                                                        .add(ProviderProductMasterData::hasSupplySchedule, false)
                                                        .add(ProviderProductMasterData::hasTransportUnitSize, false)
                                                        .build()
                                        )
                                        .build())
                )
        );
    }
}
