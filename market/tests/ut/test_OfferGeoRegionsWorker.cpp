#include <market/idx/offers/lib/iworkers/OfferCtx.h>
#include <market/idx/offers/lib/iworkers/OfferGeoRegionsWorker.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>


/*
bzz13@box:~$
    arcadia/market/tools/mmapviewer/mmapviewer
    arcadia/market/idx/offers/tests/ut/data/shops_outlet_v6.mmap

 ==== Delivery service 151 ====
PointID:    1199
ShopPointID:123
PointType:  4
RegionID:   1
PostCode:   0

 ==== Shop 81832 ====
PointID:    419024
ShopPointID:419024
PointType:  2
RegionID:   10836
PostCode:   0

 ==== Shop 131074 ====
 ShopDeliveryServiceID: 151

 ==== Shop 774 ====
PointID:    419549
ShopPointID:2374
PointType:  3 + BOOK_NOW
RegionID:   213
PostCode:   0*/

void ExtractShops(NMarket::NShopsOutlet::IReader* shopsOutlet, NMarket::NShopsOutlet::TOutletToDeliveryServiceMap& outletToDeliveryService, NMarket::NShopsOutlet::TDeliveryServiceTypeFlags& deliveryServicesFlags, NMarket::NShopsOutlet::TShopOutletInfoMap& shopOutletInfo) {
    NMarket::NShopsOutlet::TMMapShopInfoExtractor extractor(shopsOutlet, outletToDeliveryService, deliveryServicesFlags, shopOutletInfo);
    shopsOutlet->Iterate(extractor);
    extractor.FindServices();
}

TEST(TOfferGeoRegionsWorker, PostTermGeoRegions)
{
    /*
    Тест проверяет, что для нефулфилментовского офера с заданным
    fulfillment_shop_id, для которого есть запись в shops_outlet_v6.mmap,
    правильно вычисляются регионы аутлетов и складываются в отдельное поле
    */


    MarketIndexer::GenerationLog::Record glRecord;
    TOfferCtx offerContext;

    glRecord.set_shop_id(131074);
    glRecord.set_flags(NMarket::NDocumentFlags::POST_TERM);

    const TString shopsOutletPath(
        SRC_("data/shops_outlet_v6.mmap")
    );
    auto shopsOutlet = NMarket::NShopsOutlet::MakeReader(
        Market::NMmap::IMemoryRegion::MmapFile(
            shopsOutletPath
        )
    );

    // test worker
    NMarket::NShopsOutlet::TOutletToDeliveryServiceMap outletToDeliveryService;
    NMarket::NShopsOutlet::TDeliveryServiceTypeFlags deliveryServicesFlags;
    NMarket::NShopsOutlet::TShopOutletInfoMap shopOutletInfo;
    ExtractShops(shopsOutlet.Get(), outletToDeliveryService, deliveryServicesFlags, shopOutletInfo);
    auto worker = MakeOfferGeoRegionsWorker(&shopOutletInfo);

    // worker not raise exceptions
    worker->ProcessOffer(&glRecord, &offerContext);

    // in according with shops_outlet_v6.mmap
    ASSERT_EQ(1, glRecord.int_geo_regions().size());
    EXPECT_EQ(1, glRecord.int_geo_regions()[0]);
}
