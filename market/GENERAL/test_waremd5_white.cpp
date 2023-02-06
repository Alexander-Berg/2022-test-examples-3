#include <market/idx/datacamp/miner/processors/ware_md5_creator/ware_md5_creator.h>

#include <market/idx/datacamp/miner/lib/test_utils.h>

#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>

#include <library/cpp/testing/unittest/gtest.h>


void CalculateMd5(NMiner::TDatacampOffer& offer, bool use_v2 = false) {
    if (use_v2) {
        NMiner::CalculateWhiteMd5V2(offer);
    } else {
        NMiner::CalculateMd5<NMarket::EMarketColor::MC_WHITE>(offer, NMiner::TWareMd5CreatorConfig("test"));
    }
}


void AssertImpactOnWareMd5(std::function<void(NMiner::TDatacampOffer& offer)> factor) {
    static std::string EMPTY_OFFERS_WARE_MD5 = "BgSxz4QLNbMxxWshpOFIpg";

    NMiner::TDatacampOffer offer = MakeDefault();
    factor(offer);
    CalculateMd5(offer);
    ASSERT_NE(std::string{offer.identifiers().extra().ware_md5()}, EMPTY_OFFERS_WARE_MD5);
}

void AssertImpactOnWareMd5V2(std::function<void(NMiner::TDatacampOffer& offer)> factor) {
    static std::string DEFAULT_WARE_MD5 = "ieaJFZhiKuKECFc092C-Ug";

    NMiner::TDatacampOffer offer = MakeDefault();
    offer.mutable_identifiers()->set_shop_id(111);
    offer.mutable_identifiers()->set_feed_id(111);
    offer.mutable_identifiers()->set_warehouse_id(111);

    factor(offer);
    CalculateMd5(offer, true);
    ASSERT_NE(std::string{offer.identifiers().extra().ware_md5()}, DEFAULT_WARE_MD5);
}

TEST (WareMd5Test, Simple)
{
    Market::DataCamp::Offer basic;
    Market::DataCamp::Offer service;
    service.mutable_meta()->set_rgb(Market::DataCamp::MarketColor::WHITE);
    (*service.mutable_meta()->mutable_platforms())[Market::DataCamp::MarketColor::WHITE] = true;
    NMiner::TDatacampOffer offer(&basic, &service);
    offer.mutable_identifiers()->set_shop_id(111);
    offer.mutable_identifiers()->set_feed_id(222);

    const TString url = "http://www.1.ru/?ID=1";
    const TString name = "Monitor 1";
    const TString wareMd5 = "QWEZBTj6aRw92HsyoJvi0A";

    //Оба параметра в сервисной части. Для name - это ошибка, там фолбека не будет и ware_md5 получится другим
    service.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value(name);
    service.mutable_content()->mutable_partner()->mutable_original()->mutable_url()->set_value(url);
    CalculateMd5(offer);
    ASSERT_NE(offer.identifiers().extra().ware_md5(), wareMd5);

    //Имя в базовой части, а урл в сервисной части, как и ожидается
    service.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->Clear();
    basic.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value(name);
    CalculateMd5(offer);
    ASSERT_EQ(offer.identifiers().extra().ware_md5(), wareMd5);

    //Oба параметра в базовой части. Для урла больше нет фолбека, ware_md5 должен получиться другим
    service.mutable_content()->mutable_partner()->mutable_original()->mutable_url()->Clear();
    basic.mutable_content()->mutable_partner()->mutable_original()->mutable_url()->set_value(url);
    CalculateMd5(offer);
    ASSERT_NE(offer.identifiers().extra().ware_md5(), wareMd5);
}

TEST (WareMd5Test, ImpactOfEachField)
{
    // raw offer fields
    AssertImpactOnWareMd5([](auto& offer) {
        offer.GetBasicByColor().mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("Name");
    });
    AssertImpactOnWareMd5([](auto& offer) {
        offer.GetBasicByColor().mutable_content()->mutable_partner()->mutable_original()->mutable_description()->set_value("Description");
    });

    AssertImpactOnWareMd5([](auto& offer) {
        offer.GetBasicByColor().mutable_content()->mutable_partner()->mutable_original()->mutable_country_of_origin()->add_value("Russia");
    });
    AssertImpactOnWareMd5([](auto& offer) {
        offer.GetBasicByColor().mutable_content()->mutable_partner()->mutable_original()->mutable_url()->set_value("http//www.ru");
    });

    AssertImpactOnWareMd5([](auto& offer) {
        offer.GetBasicByColor().mutable_content()->mutable_partner()->mutable_original()->mutable_vendor()->set_value("vendor");
    });
    AssertImpactOnWareMd5([](auto& offer) {
        offer.GetBasicByColor().mutable_content()->mutable_partner()->mutable_original()->mutable_vendor_code()->set_value("vendor_code");
    });


    AssertImpactOnWareMd5([](auto& offer) {
        offer.mutable_identifiers()->set_shop_id(111);
    });
    AssertImpactOnWareMd5([](auto& offer) {
        offer.mutable_identifiers()->set_feed_id(111);
    });

    AssertImpactOnWareMd5([](auto& offer) {
        auto op = offer.GetBasicByColor().mutable_content()->mutable_partner()->mutable_original()->mutable_offer_params()->add_param();
        op->set_name("Size");
        op->set_value("S");
    });
}

TEST (WareMd5Test, RecalculateWareMd5)
{
    NMiner::TDatacampOffer offer = MakeDefault();
    offer.mutable_identifiers()->mutable_extra()->set_ware_md5("PRECALCULATED");

    CalculateMd5(offer);

    ASSERT_NE(std::string{offer.identifiers().extra().ware_md5()}, "PRECALCULATED");
}

TEST (WareMd5Test, IgnoreCountryOfOriginOrder)
{
    NMiner::TDatacampOffer offer1 = MakeDefault();
    offer1.GetBasicByColor().mutable_content()->mutable_partner()->mutable_original()->mutable_country_of_origin()->add_value("111");
    offer1.GetBasicByColor().mutable_content()->mutable_partner()->mutable_original()->mutable_country_of_origin()->add_value("222");

    NMiner::TDatacampOffer offer2 = MakeDefault();
    offer2.GetBasicByColor().mutable_content()->mutable_partner()->mutable_original()->mutable_country_of_origin()->add_value("222");
    offer2.GetBasicByColor().mutable_content()->mutable_partner()->mutable_original()->mutable_country_of_origin()->add_value("111");

    CalculateMd5(offer1);
    CalculateMd5(offer2);


    ASSERT_EQ(offer1.identifiers().extra().ware_md5(), offer2.identifiers().extra().ware_md5());
}

TEST (WareMd5V2Test, EmptyOffer)
{
    NMiner::TDatacampOffer offer = MakeDefault();
    CalculateMd5(offer, true);
    ASSERT_EQ(std::string{offer.identifiers().extra().ware_md5()}, "");
}

TEST (WareMd5V2Test, Simple)
{
    NMiner::TDatacampOffer offer = MakeDefault();
    offer.mutable_identifiers()->set_shop_id(111);
    offer.mutable_identifiers()->set_feed_id(111);
    offer.mutable_identifiers()->set_warehouse_id(111);
    CalculateMd5(offer, true);
    ASSERT_EQ(std::string{offer.identifiers().extra().ware_md5()}, "ieaJFZhiKuKECFc092C-Ug");
}

TEST (WareMd5V2Test, ImpactOfEachField)
{
    AssertImpactOnWareMd5V2([](auto& offer) {
        offer.mutable_identifiers()->set_shop_id(222);
    });
    AssertImpactOnWareMd5V2([](auto& offer) {
        offer.mutable_identifiers()->set_feed_id(222);
    });
    AssertImpactOnWareMd5V2([](auto& offer) {
        offer.mutable_identifiers()->set_warehouse_id(222);
    });
    AssertImpactOnWareMd5V2([](auto& offer) {
        offer.GetBasicByColor().mutable_content()->mutable_binding()->mutable_approved()->set_market_sku_id(111);
    });
    AssertImpactOnWareMd5V2([](auto& offer) {
        offer.GetBasicByColor().mutable_content()->mutable_binding()->mutable_uc_mapping()->set_market_sku_id(111);
    });
}
