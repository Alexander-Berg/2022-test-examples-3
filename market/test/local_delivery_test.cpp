#include "document_props_fake.h"
#include <market/report/library/global/random/disable_random.h>
#include <market/report/library/global/random/random.h>
#include <market/report/src/global/time.h>

#include <market/library/currency_exchange/currencies.h>
#include <market/library/interface/indexer_report_interface.h>
#include <market/library/local_delivery_mms/writer.h>
#include <market/library/local_delivery_mms/local_delivery_test.h>
#include <market/library/currency/currency.h>
#include <market/library/libsku/common.h>
#include <market/library/trees/region_tree_mock.h>
#include <market/report/library/shop_info/data/shop_info_data_over_shops_dat.h>
#include <market/report/library/relevance/local_delivery.h>
#include <market/report/src/delivery_options_generator.h>
#include <market/report/proto/MarketSearch.pb.h>
#include <market/report/library/relevance/relevance/common.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

using namespace NMarketReport;
using namespace NewRel;
using namespace NMarket::NDocumentFlags;
using namespace Market::NCurrency;

constexpr ui64 NO_RANDOM_TIMESTAMP = 488419200111 * 1000;

namespace {
    void SetDeliveryOption(const TLocalDeliveryOption& option, MarketSearch::DocumentExtraData::DeliveryInfo& data) {
        data.set_day_from(option.DayFrom);
        data.set_day_to(option.DayTo);
        data.set_order_before(option.OrderBefore);
        data.set_price_local(option.Price.Round(0).AsRaw());
        data.set_precision(0);
    }
}

namespace NDelivery {
    Market::NDelivery::TIntervalCalendar CreateCalendar(const TVector<Market::NDelivery::TIntervalTableMms<mms::Standalone>::TIntervals>& tables) {
        Market::NDelivery::TIntervalCalendar calendar;
        NDatetime::TSimpleTM date = Market::TEST_TIME;
        date.Add(NDatetime::TSimpleTM::F_DAY, -1);
        for (const auto& table : tables) {
            Market::NDelivery::TIntervalTableMms<mms::Standalone> offsets;
            offsets.Date.Day = date.MDay;
            offsets.Date.Month = date.RealMonth();
            offsets.Date.Year = date.RealYear();
            offsets.Intervals = table;
            calendar.push_back(offsets);
            date.Add(NDatetime::TSimpleTM::F_DAY, 1);
        }
        return calendar;
    }

    Market::NDelivery::TLocalDeliveryOption CreateOption(uint8_t daysMin, uint8_t daysMax, uint8_t orderBeforeHour, uint64_t priceValue, ui32 calendarId = 0) {
        Market::NDelivery::TLocalDeliveryOption option;
        option.DaysMin = daysMin;
        option.DaysMax = daysMax;
        option.OrderBeforeHour = orderBeforeHour;
        option.PriceValue = priceValue;
        option.CalendarIndex = calendarId;
        return option;
    }

    void CreateLocalDeliveryFile() {
        TVector<Market::NDelivery::TIntervalCalendar> calendars;

        TVector<Market::NDelivery::TIntervalTableMms<mms::Standalone>::TIntervals> testTables;
        testTables.push_back(std::vector<uint16_t>({0, 1, 2, 5, 6, 7, 8, 9, 12, 13, 14, 15, 16, 20, 21, 22, 23, 26, 27, 28, 29, 30, 34, 35, 36, 37, 40, 41, 42, 43, 44, 47, 48, 49, 50, 51, 54, 55, 56, 57, 58, 61, 62, 63, 64}));
        testTables.push_back(std::vector<uint16_t>({0, 1, 4, 5, 6, 7, 8, 11, 12, 13, 14, 15, 19, 20, 21, 22, 25, 26, 27, 28, 29, 33, 34, 35, 36, 39, 40, 41, 42, 43, 46, 47, 48, 49, 50, 53, 54, 55, 56, 57, 60, 61, 62, 63, 64}));
        testTables.push_back(std::vector<uint16_t>({0, 3, 4, 5, 6, 7, 9, 11, 12, 13, 14, 18, 19, 20, 21, 24, 25, 26, 27, 28, 32, 33, 34, 35, 38, 39, 40, 41, 42, 45, 46, 47, 48, 49, 52, 53, 54, 55, 56, 59, 60, 61, 62, 63, 64}));
        calendars.push_back(CreateCalendar(testTables));

        TVector<Market::NDelivery::TOfferLocalDelivery> offers;
        {
            offers.emplace_back();                                             //0
            offers.back().Options.push_back(CreateOption(1, 2, 9, 500));
            offers.back().Options.push_back(CreateOption(2, 5, 24, 500));
            offers.back().CurrencyId = 0;

            offers.emplace_back();                                             //1
            offers.back().Options.push_back(CreateOption(1, 3, 3, 500));
            offers.back().Options.push_back(CreateOption(2, 4, 24, 300));
            offers.back().CurrencyId = 0;

            offers.emplace_back();                                             //2
            offers.back().Options.push_back(CreateOption(32, 32, 24, 500));
            offers.back().CurrencyId = 0;

            offers.emplace_back();                                             //3
            offers.back().Options.push_back(CreateOption(1, 3, 18, 500, 1));
            offers.back().Options.push_back(CreateOption(2, 12, 22, 300, 1));
            offers.back().Options.push_back(CreateOption(1, 11, 7, 200, 1));
            offers.back().Options.push_back(CreateOption(10, 20, 19, 100, 0));
            offers.back().Options.push_back(CreateOption(32, 50, 24, 0, 1));
            offers.back().CurrencyId = 0;

            offers.emplace_back();                                             //4
            offers.back().CurrencyId = 0;

            offers.emplace_back();                                             //5
            offers.back().Options.push_back(CreateOption(1, 3, 18, 500));
            offers.back().Options.push_back(CreateOption(1, 7, 10, 500));
            offers.back().CurrencyId = 0;

            offers.emplace_back();                                             //6
            offers.back().Options.push_back(CreateOption(1, 2, 9, 500, 1));
            offers.back().Options.push_back(CreateOption(1, 5, 24, 500, 1));
            offers.back().CurrencyId = 0;

            offers.emplace_back();                                             //7
            offers.back().Options.push_back(CreateOption(0, 1, 9, 500, 1));
            offers.back().Options.push_back(CreateOption(2, 7, 24, 500, 1));
            offers.back().CurrencyId = 0;

            offers.emplace_back();                                             //8
            offers.back().Options.push_back(CreateOption(0, 0, 9, 500));
            offers.back().Options.push_back(CreateOption(0, 1, 24, 0));
            offers.back().CurrencyId = 0;

            offers.emplace_back();                                             //9
            offers.back().Options.push_back(CreateOption(0, 255, 9, 500));
            offers.back().CurrencyId = 0;

            offers.emplace_back();                                             //10
            offers.back().Options.push_back(CreateOption(0, 0, 9, 0));
            offers.back().Options.push_back(CreateOption(0, 1, 24, 0));
            offers.back().CurrencyId = 0;
        }

        Market::NDelivery::TLocalDeliveryMmsWriter writer(0);
        Market::AddZeroTestCalendar(writer);
        for(const auto& calendar : calendars) {
            writer.AddCalendar(calendar);
        }
        for(const auto& offer : offers) {
            writer.AddOffer(offer);
        }
        writer.Save(SRC_("./TestData/local_delivery.mmap"));
    }
}

TEST(LocalDeliveryTest, TestDeliveryField) {
    NGlobal::SetNoRandom(true);
    NGlobal::InitializeTime({true, NO_RANDOM_TIMESTAMP});

    //NDelivery::CreateLocalDeliveryFile();
    Y_UNUSED(NDelivery::CreateLocalDeliveryFile);
    using namespace MarketRelevance;

    auto props = CreateDocumentPropsFake();

    TLocalDelivery localDelivery(Nothing(), props, false /*freeDelivery*/, Nothing() /*maxInterval*/, false);

    const auto currencyIdRur = props->GetCurrencyIndex(TCurrency::Rur());
    const auto currencyIdKzt = props->GetCurrencyIndex(TCurrency::Kzt());

    MockRegionTree regions;
    Market::TRegionData moscowRegion;
    moscowRegion.Name = "Москва";
    moscowRegion.Type = Market::ERegionType::CITY;
    moscowRegion.TzOffset = 10800;

    Market::TRegionData vladivostokRegion;
    vladivostokRegion.Name = "Владивосток";
    vladivostokRegion.Type = Market::ERegionType::CITY;
    vladivostokRegion.TzOffset = 36000;

    Market::TRegionData nyRegion;
    nyRegion.Name = "Нью-Йорк";
    nyRegion.Type = Market::ERegionType::CITY;
    nyRegion.TzOffset = -14400;

    regions.setRegionForId(1, moscowRegion);
    regions.setRegionForId(2, vladivostokRegion);
    regions.setRegionForId(3, nyRegion);

    TMaybe<TLocalDeliveryOption> defaultOption;

    NMarket::NShopsDat::TFullRecord moscowShop;
    moscowShop.ShopId = 1;
    moscowShop.PriorityRegion = 1;

    NMarket::NShopsDat::TFullRecord vladivostokShop;
    vladivostokShop.ShopId = 2;
    vladivostokShop.PriorityRegion = 2;

    NMarket::NShopsDat::TFullRecord nyShop;
    nyShop.ShopId = 3;
    nyShop.PriorityRegion = 3;

    TShopInfoSharedDataHolder moscowShared{};
    TShopInfo moscowInfo(NMarket::NReport::CreateShopInfoDataOverShopsDat(moscowShop), regions, moscowShared);
    TShopInfoSharedDataHolder vladivostokShared{};
    TShopInfo vladivostokInfo(NMarket::NReport::CreateShopInfoDataOverShopsDat(vladivostokShop), regions, vladivostokShared);
    TShopInfoSharedDataHolder nyShared{};
    TShopInfo nyInfo(NMarket::NReport::CreateShopInfoDataOverShopsDat(nyShop), regions, nyShared);

    ASSERT_EQ(3, localDelivery.GetShopTime(moscowInfo)->Hour);
    ASSERT_EQ(10, localDelivery.GetShopTime(vladivostokInfo)->Hour);
    ASSERT_EQ(20, localDelivery.GetShopTime(nyInfo)->Hour);

    ASSERT_NO_THROW(defaultOption = localDelivery.GetDefaultOption(NMarket::NSKU::TOfferShipmentId(0), moscowInfo, currencyIdRur));
    EXPECT_EQ(false, defaultOption.Empty());
    EXPECT_EQ(500, defaultOption->Price.AsRaw());
    EXPECT_EQ(1, defaultOption->DayFrom);
    EXPECT_EQ(2, defaultOption->DayTo);

    ASSERT_NO_THROW(defaultOption = localDelivery.GetDefaultOption(NMarket::NSKU::TOfferShipmentId(0), vladivostokInfo, currencyIdRur));
    EXPECT_EQ(2, defaultOption->DayFrom);
    EXPECT_EQ(3, defaultOption->DayTo);

    ASSERT_NO_THROW(defaultOption = localDelivery.GetDefaultOption(NMarket::NSKU::TOfferShipmentId(1), moscowInfo, currencyIdRur));
    EXPECT_EQ(300, defaultOption->Price.AsRaw());
    EXPECT_EQ(2, defaultOption->DayFrom);
    EXPECT_EQ(4, defaultOption->DayTo);

    ASSERT_NO_THROW(defaultOption = localDelivery.GetDefaultOption(NMarket::NSKU::TOfferShipmentId(2), moscowInfo, currencyIdRur));
    EXPECT_EQ(false, defaultOption.Empty());
    EXPECT_EQ(32, defaultOption->DayFrom);
    EXPECT_EQ(32, defaultOption->DayTo);

    ASSERT_NO_THROW(defaultOption = localDelivery.GetDefaultOption(NMarket::NSKU::TOfferShipmentId(4), moscowInfo, currencyIdRur));
    EXPECT_EQ(true, defaultOption.Empty());

    ASSERT_NO_THROW(defaultOption = localDelivery.GetDefaultOption(NMarket::NSKU::TOfferShipmentId(0), moscowInfo, currencyIdKzt));
    EXPECT_EQ(2500, defaultOption->Price.AsRaw());

    int flags = AVAILABLE | DELIVERY | STORE;
    EXPECT_EQ(flags | ONSTOCK, localDelivery.ProcessFlags(NMarket::NSKU::TOfferShipmentId(0), moscowInfo, flags));
    EXPECT_EQ(flags, localDelivery.ProcessFlags(NMarket::NSKU::TOfferShipmentId(0), vladivostokInfo, flags));

    TVector<TLocalDeliveryOption> options;
    ui32 optionsCount = props->GetLocalDeliveryStorage().GetOfferDeliveryOptionCount(NMarket::NSKU::TOfferShipmentId(3));
    for (ui32 i = 0; i < optionsCount; i++)
        ASSERT_NO_THROW(options.push_back(props->GetLocalDeliveryOption(3, i, *localDelivery.GetShopTime(vladivostokInfo), currencyIdKzt, {})));
    EXPECT_EQ(5, optionsCount);
    EXPECT_EQ(0, options[0].Price.AsRaw());
    EXPECT_EQ(255, options[0].DayTo);
    EXPECT_EQ(500, options[1].Price.AsRaw());
    EXPECT_EQ(20, options[1].DayTo);
    EXPECT_EQ(1000, options[2].Price.AsRaw());
    EXPECT_EQ(4, options[2].DayFrom);
    EXPECT_EQ(19, options[2].DayTo);
    EXPECT_EQ(1500, options[3].Price.AsRaw());
    EXPECT_EQ(2500, options[4].Price.AsRaw());
    EXPECT_EQ(1, options[4].DayFrom);
    EXPECT_EQ(5, options[4].DayTo);

    ASSERT_NO_THROW(defaultOption = localDelivery.GetDefaultOption(NMarket::NSKU::TOfferShipmentId(6), moscowInfo, currencyIdRur));
    EXPECT_EQ(false, defaultOption.Empty());
    EXPECT_EQ(500, defaultOption->Price.AsRaw());
    EXPECT_EQ(1, defaultOption->DayFrom);
    EXPECT_EQ(4, defaultOption->DayTo);
    EXPECT_EQ(flags | ONSTOCK, localDelivery.ProcessFlags(NMarket::NSKU::TOfferShipmentId(6), moscowInfo, flags));

    ASSERT_NO_THROW(defaultOption = localDelivery.GetDefaultOption(NMarket::NSKU::TOfferShipmentId(6), vladivostokInfo, currencyIdRur));
    EXPECT_EQ(false, defaultOption.Empty());
    EXPECT_EQ(500, defaultOption->Price.AsRaw());
    EXPECT_EQ(4, defaultOption->DayFrom);
    EXPECT_EQ(5, defaultOption->DayTo);

    ASSERT_NO_THROW(defaultOption = localDelivery.GetDefaultOption(NMarket::NSKU::TOfferShipmentId(7), nyInfo, currencyIdRur));
    EXPECT_EQ(false, defaultOption.Empty());
    EXPECT_EQ(500, defaultOption->Price.AsRaw());
    EXPECT_EQ(1, defaultOption->DayFrom);
    EXPECT_EQ(2, defaultOption->DayTo);

}

TEST(LocalDeliveryTest, TestDeliveryFilters) {
    NGlobal::SetNoRandom(true);
    NGlobal::InitializeTime({true, NO_RANDOM_TIMESTAMP});

    auto props = CreateDocumentPropsFake();

    const auto currencyIdRur = props->GetCurrencyIndex(TCurrency::Rur());

    MockRegionTree regions;
    Market::TRegionData moscowRegion;
    moscowRegion.Name = "Москва";
    moscowRegion.Type = Market::ERegionType::CITY;
    moscowRegion.TzOffset = 10800;

    Market::TRegionData vladivostokRegion;
    vladivostokRegion.Name = "Владивосток";
    vladivostokRegion.Type = Market::ERegionType::CITY;
    vladivostokRegion.TzOffset = 36000;

    regions.setRegionForId(1, moscowRegion);
    regions.setRegionForId(2, vladivostokRegion);

    NMarket::NShopsDat::TFullRecord moscowShop;
    moscowShop.ShopId = 1;
    moscowShop.PriorityRegion = 1;

    NMarket::NShopsDat::TFullRecord vladivostokShop;
    vladivostokShop.ShopId = 2;
    vladivostokShop.PriorityRegion = 2;

    TShopInfoSharedDataHolder moscowShared{};
    TShopInfo moscowInfo(NMarket::NReport::CreateShopInfoDataOverShopsDat(moscowShop), regions, moscowShared);
    TShopInfoSharedDataHolder vladivostokShared{};
    TShopInfo vladivostokInfo(NMarket::NReport::CreateShopInfoDataOverShopsDat(vladivostokShop), regions, vladivostokShared);

    TLocalDelivery localDelivery(Nothing(), props, false /*freeDelivery*/, Nothing() /*maxInterval*/, false);
    TLocalDelivery localFreeDelivery(Nothing(), props, true /*freeDelivery*/, Nothing() /*maxInterval*/, false);
    TLocalDelivery localFastDelivery(Nothing(), props, false /*freeDelivery*/, 0 /*maxInterval*/, false);
    TLocalDelivery localIdealDelivery(Nothing(), props, true /*freeDelivery*/, 0 /*maxInterval*/, false);

    TMaybe<TLocalDeliveryOption> defaultOption;

    ASSERT_NO_THROW(defaultOption = localFreeDelivery.GetDefaultOption(NMarket::NSKU::TOfferShipmentId(0), moscowInfo, currencyIdRur));
    EXPECT_EQ(true, defaultOption.Empty());

    ASSERT_NO_THROW(defaultOption = localFreeDelivery.GetDefaultOption(NMarket::NSKU::TOfferShipmentId(3), moscowInfo, currencyIdRur));
    EXPECT_EQ(false, defaultOption.Empty());
    EXPECT_EQ(0, defaultOption->Price.AsRaw());
    EXPECT_EQ(48, defaultOption->DayFrom);
    EXPECT_EQ(255, defaultOption->DayTo);

    ASSERT_NO_THROW(defaultOption = localFastDelivery.GetDefaultOption(NMarket::NSKU::TOfferShipmentId(8), vladivostokInfo, currencyIdRur));
    EXPECT_EQ(true, defaultOption.Empty());

    ASSERT_NO_THROW(defaultOption = localFastDelivery.GetDefaultOption(NMarket::NSKU::TOfferShipmentId(8), moscowInfo, currencyIdRur));
    EXPECT_EQ(false, defaultOption.Empty());
    EXPECT_EQ(0, defaultOption->DayFrom);
    EXPECT_EQ(0, defaultOption->DayTo);

    ASSERT_NO_THROW(defaultOption = localDelivery.GetDefaultOption(NMarket::NSKU::TOfferShipmentId(9), moscowInfo, currencyIdRur));
    EXPECT_EQ(false, defaultOption.Empty());

    ASSERT_NO_THROW(defaultOption = localDelivery.GetDefaultOption(NMarket::NSKU::TOfferShipmentId(10), moscowInfo, currencyIdRur));
    EXPECT_EQ(false, defaultOption.Empty());
    EXPECT_EQ(0, defaultOption->DayFrom);
    EXPECT_EQ(0, defaultOption->DayTo);

    NewRel::TDeliveryStats stats = localDelivery.GetDeliveryStats(false, false, TParcelInfo::Dummy(), NMarket::NSKU::TOfferShipmentId(3), moscowInfo, currencyIdRur);
    EXPECT_EQ(true, stats.FreeDelivery);
    EXPECT_EQ(5, stats.MinInterval);

    stats = localFreeDelivery.GetDeliveryStats(false, false, TParcelInfo::Dummy(), NMarket::NSKU::TOfferShipmentId(3), moscowInfo, currencyIdRur);
    EXPECT_EQ(true, stats.FreeDelivery);
    EXPECT_EQ(255, stats.MinInterval);

    stats = localIdealDelivery.GetDeliveryStats(false, false, TParcelInfo::Dummy(), NMarket::NSKU::TOfferShipmentId(3), moscowInfo, currencyIdRur);
    EXPECT_EQ(false, stats.FreeDelivery);
    EXPECT_EQ(255, stats.MinInterval);

    stats = localIdealDelivery.GetDeliveryStats(false, false, TParcelInfo::Dummy(), NMarket::NSKU::TOfferShipmentId(10), moscowInfo, currencyIdRur);
    EXPECT_EQ(true, stats.FreeDelivery);
    EXPECT_EQ(0, stats.MinInterval);
}

TEST(LocalDeliveryTest, TestDeliveryGenerator) {
    NGlobal::SetNoRandom(true);
    NGlobal::InitializeTime({true, NO_RANDOM_TIMESTAMP});

    /**
     * Create data for test
     **/
    MarketSearch::DocumentExtraData data;
    auto props = CreateDocumentPropsFake();
    TLocalDelivery localDelivery(Nothing(), props, false /*freeDelivery*/, Nothing() /*maxInterval*/, false);

    const auto currencyIdRur = props->GetCurrencyIndex(TCurrency::Rur());

    TShopInfo shopInfo;

    auto defaultOptionData = data.mutable_default_delivery_option();
    SetDeliveryOption(*localDelivery.GetDefaultOption(NMarket::NSKU::TOfferShipmentId(3), shopInfo, currencyIdRur), *defaultOptionData);

    ui32 optionsCount = props->GetLocalDeliveryStorage().GetOfferDeliveryOptionCount(NMarket::NSKU::TOfferShipmentId(3));
    for (ui32 i = 0; i < optionsCount; ++i) {
        auto* optionBuilder = data.add_delivery_options();
        auto option = props->GetLocalDeliveryOption(3, i, *localDelivery.GetShopTime(shopInfo), currencyIdRur, {});
        SetDeliveryOption(option, *optionBuilder);
    }

    Market::TRegionData clientRegion;
    clientRegion.TzOffset = 36000;      ///Vladivostok timezone
    Market::TRegionData shopRegion;
    shopRegion.TzOffset = 10800;      ///Moscow timezone

    /**
     * Run test
     **/
    TDeliveryOptionsGenerator gen(clientRegion, TCurrency::Rub(), shopRegion, true, false, false, false, false, false, false, false);
    NOutput::TDelivery delivery;
    ASSERT_NO_THROW(gen.Generate(data, false));
    ASSERT_NO_THROW(delivery.LocalDeliveryOptions = gen.Generate(data, true));
    EXPECT_EQ(false, delivery.LocalDeliveryOptions.Empty());

    const NOutput::TDeliveryOption::TListType& options = *delivery.LocalDeliveryOptions;
    EXPECT_EQ(4, options.size());
    EXPECT_EQ(true, options[0].IsDefault);
    EXPECT_EQ(0, options[0].Price->Price.AsRaw());
    EXPECT_EQ(100, options[1].Price->Price.AsRaw());
    EXPECT_EQ(true, options[1].OrderBeforeTime.Empty());
    EXPECT_EQ(200, options[2].Price->Price.AsRaw());
    EXPECT_EQ(4, *options[2].DayFrom);
    EXPECT_EQ(19, *options[2].DayTo);
    EXPECT_EQ(true, options[2].OrderBeforeTime.Empty());
    EXPECT_EQ(500, options[3].Price->Price.AsRaw());
    EXPECT_EQ(4, *options[3].DayFrom);
    EXPECT_EQ(6, *options[3].DayTo);
    EXPECT_EQ(true, options[3].OrderBeforeTime.Empty());
}

TEST(LocalDeliveryTest, TestOptionWorse) {
    NGlobal::SetNoRandom(true);
    NGlobal::InitializeTime({true, NO_RANDOM_TIMESTAMP});

    /**
     * Create data for test
     **/
    MarketSearch::DocumentExtraData data;
    auto props = CreateDocumentPropsFake();
    TLocalDelivery localDelivery(Nothing(), props, false /*freeDelivery*/, Nothing() /*maxInterval*/, false);

    const auto currencyIdRur = props->GetCurrencyIndex(TCurrency::Rur());

    TShopInfo shopInfo;

    auto defaultOptionData = data.mutable_default_delivery_option();
    SetDeliveryOption(*localDelivery.GetDefaultOption(NMarket::NSKU::TOfferShipmentId(5), shopInfo, currencyIdRur), *defaultOptionData);

    ui32 optionsCount = props->GetLocalDeliveryStorage().GetOfferDeliveryOptionCount(NMarket::NSKU::TOfferShipmentId(5));
    for (ui32 i = 0; i < optionsCount; ++i) {
        auto* optionBuilder = data.add_delivery_options();
        auto option = props->GetLocalDeliveryOption(5, i, *localDelivery.GetShopTime(shopInfo), currencyIdRur, {});
        SetDeliveryOption(option, *optionBuilder);
    }

    Market::TRegionData clientRegion;
    clientRegion.TzOffset = 10800;
    Market::TRegionData shopRegion;
    shopRegion.TzOffset = 10800;

    /**
     * Run test
     **/
    TDeliveryOptionsGenerator gen(clientRegion, TCurrency::Rub(), shopRegion, true, false, true, false, false, false, false, false);
    NOutput::TDelivery delivery;
    ASSERT_NO_THROW(gen.Generate(data, false));
    ASSERT_NO_THROW(delivery.LocalDeliveryOptions = gen.Generate(data, true));
    EXPECT_EQ(false, delivery.LocalDeliveryOptions.Empty());

    const NOutput::TDeliveryOption::TListType& options = *delivery.LocalDeliveryOptions;
    EXPECT_EQ(1, options.size());
    EXPECT_EQ(true, options[0].IsDefault);
    EXPECT_EQ(500, options[0].Price->Price.AsRaw());
    EXPECT_EQ(2, *options[0].DayFrom);
    EXPECT_EQ(4, *options[0].DayTo);
    EXPECT_EQ(true, options[0].OrderBeforeTime.Empty());
}

TEST(LocalDeliveryTest, TestOptionWorseAdditional) {
    NGlobal::SetNoRandom(true);
    NGlobal::InitializeTime({true, NO_RANDOM_TIMESTAMP});

    /**
     * Create data for test
     **/
    MarketSearch::DocumentExtraData data;
    TLocalDeliveryOption options[5];
    options[0].DayFrom = 10;
    options[0].DayTo = 11;
    options[0].Price = TFixedPointNumber(0, 100.0);
    options[0].OrderBefore = 0;

    options[1].DayFrom = 9;
    options[1].DayTo = 10;
    options[1].Price = TFixedPointNumber(0, 200.0);
    options[1].OrderBefore = 0;

    options[2].DayFrom = 0;
    options[2].DayTo = 1;
    options[2].Price = TFixedPointNumber(0, 1000.0);
    options[2].OrderBefore = 0;

    options[3].DayFrom = 8;
    options[3].DayTo = 8;
    options[3].Price = TFixedPointNumber(0, 50.0);
    options[3].OrderBefore = 0;

    options[4].DayFrom = 2;
    options[4].DayTo = 4;
    options[4].Price = TFixedPointNumber(0, 500.0);
    options[4].OrderBefore = 0;

    auto defaultOptionData = data.mutable_default_delivery_option();
    SetDeliveryOption(options[2], *defaultOptionData);
    for (ui32 i = 0; i < 5; ++i) {
        auto* optionBuilder = data.add_delivery_options();
        SetDeliveryOption(options[i], *optionBuilder);
    }

    Market::TRegionData clientRegion;
    clientRegion.TzOffset = 10800;
    Market::TRegionData shopRegion;
    shopRegion.TzOffset = 10800;

    /**
     * Run test
     **/
    TDeliveryOptionsGenerator gen(clientRegion, TCurrency::Rub(), shopRegion, true, false, true, false, false, false, false, false);
    NOutput::TDelivery delivery;
    ASSERT_NO_THROW(gen.Generate(data, false));
    ASSERT_NO_THROW(delivery.LocalDeliveryOptions = gen.Generate(data, true));
    EXPECT_EQ(3, delivery.LocalDeliveryOptions->size());
    EXPECT_EQ("1000", delivery.LocalDeliveryOptions.GetRef()[0].Price->Price.AsTString());
    EXPECT_EQ("50", delivery.LocalDeliveryOptions.GetRef()[1].Price->Price.AsTString());
    EXPECT_EQ("500", delivery.LocalDeliveryOptions.GetRef()[2].Price->Price.AsTString());
}
