#include <market/idx/promos/yt_promo_indexer/options/yt_promo_indexer.pb.h>
#include <market/idx/promos/yt_promo_indexer/src/yt_promo_dumper.h>
#include <market/idx/promos/yt_promo_indexer/src/yt_promo_indexer.h>

#include <market/library/libpromo/reader.h>
#include <market/library/libpromo/utils/promo_stats.h>
#include <market/library/libpromo/utils/protobufhelpers.h>

#include <library/cpp/protobuf/protofile/protofile.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/algorithm.h>

#include <array>

namespace {

    const uint64_t PRICE_PRECISION = 10000000;
    const uint64_t TIME_DELTA = 120;
    const uint64_t NOW = TInstant::Now().Seconds();
    const int64_t GOOD_END_DATE = NOW + TIME_DELTA;
    const int64_t FUTURE_START_DATE = NOW + 2 * TIME_DELTA;
    constexpr auto OUTPUT_FILE_NAME_TEST = "yt_promo_details_test.mmap";

    using EVersion = NMarket::NPromo::EPromoDetailsFormatVersion;

    using TPromoDetailsMap = TMap<TString, Market::Promo::PromoDetails>;
    using TMskuDetailsMap = TMap<TString, TVector<NMarket::NPromo::TMSKUDetails>>;
    using TPromoRegionsMap = TMap<TString, TVector<uint32_t>>;

    void MakePromo1(const TString& promoKey1,
        TPromoDetailsMap& expectedPromoDetails,
        TPromoDetailsMap& /*unExpectedPromoDetails*/
    ) {
        // promo #1
        Market::Promo::PromoDetails promo;
        promo.set_description("description_1");
        promo.set_start_date(11);
        promo.set_end_date(GOOD_END_DATE + TIME_DELTA);
        promo.set_type(ToUnderlying(NMarket::NPromo::EPromoType::Blue3PFlashDiscount));
        expectedPromoDetails[promoKey1] = promo;
    }

    void MakePromo2(const TString& promoKey2,
        TPromoDetailsMap& expectedPromoDetails,
        TPromoDetailsMap& /*unExpectedPromoDetails*/
    ) {
        Market::Promo::PromoDetails promo;
        promo.set_description("description_2");
        promo.set_start_date(21);
        promo.set_end_date(GOOD_END_DATE + TIME_DELTA);
        promo.set_type(ToUnderlying(NMarket::NPromo::EPromoType::Blue3PFlashDiscount));
        expectedPromoDetails[promoKey2] = promo;
    }

    // blue 3P
    void MakePromo4(
        const TString& mskuPromoKey,
        const TString& promoId,
        TMskuDetailsMap& expectedMSKUDetails,
        TMskuDetailsMap& /*futureMSKUDetails*/,
        TPromoDetailsMap& expectedPromoDetails
    ) {
        NMarket::NPromo::TMSKUDetails promo;
        promo.SourcePromoId = promoId;
        promo.MarketPromoPrice = TFixedPointNumber::CreateFromRawValue(1000 * PRICE_PRECISION / 100);
        promo.MarketOldPrice = TFixedPointNumber::CreateFromRawValue(2000 * PRICE_PRECISION / 100);
        expectedMSKUDetails[mskuPromoKey].push_back(promo);

        Market::Promo::PromoDetails sourcePromo;
        sourcePromo.set_description("blue3PTest1");
        sourcePromo.set_start_date(31);
        sourcePromo.set_end_date(GOOD_END_DATE + TIME_DELTA);
        sourcePromo.set_type(ToUnderlying(NMarket::NPromo::EPromoType::Blue3PFlashDiscount));
        sourcePromo.set_allowed_payment_methods(NMarket::NPaymentMethods::EPaymentMethods::All);
        expectedPromoDetails[promoId] = sourcePromo;
    }

    void MakePromo5(
        const TString& mskuPromoKey,
        const TString& promoId,
        TMskuDetailsMap& expectedMSKUDetails,
        TMskuDetailsMap& /*futureMSKUDetails*/,
        TPromoDetailsMap& expectedPromoDetails
    ) {
        NMarket::NPromo::TMSKUDetails promo;
        promo.SourcePromoId = promoId;
        promo.MarketPromoPrice = TFixedPointNumber::CreateFromRawValue(1500 * PRICE_PRECISION / 100);
        promo.MarketOldPrice = TFixedPointNumber::CreateFromRawValue(2500 * PRICE_PRECISION / 100);
        expectedMSKUDetails[mskuPromoKey].push_back(promo);

        Market::Promo::PromoDetails sourcePromo;
        sourcePromo.set_description("blue3PTest2");
        sourcePromo.set_start_date(31);
        sourcePromo.set_end_date(GOOD_END_DATE + TIME_DELTA);
        sourcePromo.set_type(ToUnderlying(NMarket::NPromo::EPromoType::Blue3PFlashDiscount));
        sourcePromo.set_allowed_payment_methods(NMarket::NPaymentMethods::EPaymentMethods::YANDEX | NMarket::NPaymentMethods::EPaymentMethods::CARD_ON_DELIVERY);
        expectedPromoDetails[promoId] = sourcePromo;
    }

    void MakePromo6(
        const TString& mskuPromoKey,
        const TString& promoId,
        TMskuDetailsMap& /*expectedMSKUDetails*/,
        TMskuDetailsMap& futureMSKUDetails,
        TPromoDetailsMap& expectedPromoDetails
    ) {

        NMarket::NPromo::TMSKUDetails promo;
        promo.SourcePromoId = promoId;
        promo.MarketPromoPrice = TFixedPointNumber::CreateFromRawValue(3500 * PRICE_PRECISION / 100);
        promo.MarketOldPrice = TFixedPointNumber::CreateFromRawValue(4500 * PRICE_PRECISION / 100);
        futureMSKUDetails[mskuPromoKey].push_back(promo);

        Market::Promo::PromoDetails futureSourcePromo;
        futureSourcePromo.set_description("blue3PTest2");
        futureSourcePromo.set_start_date(FUTURE_START_DATE);
        futureSourcePromo.set_end_date(-1);
        futureSourcePromo.set_type(ToUnderlying(NMarket::NPromo::EPromoType::Blue3PFlashDiscount));
        futureSourcePromo.set_allowed_payment_methods(NMarket::NPaymentMethods::EPaymentMethods::CASH_ON_DELIVERY);
        expectedPromoDetails[promoId] = futureSourcePromo;
    }

    void MakePromo7(
        const TString& /*mskuPromoKey*/,
        const TString& promoId,
        TMskuDetailsMap& /*expectedMSKUDetails*/,
        TMskuDetailsMap& /*futureMSKUDetails*/,
        TPromoDetailsMap& expectedPromoDetails
    ) {

        Market::Promo::PromoDetails futureSourcePromo;
        futureSourcePromo.set_type(ToUnderlying(NMarket::NPromo::EPromoType::BlueCashback));
        futureSourcePromo.set_description("BlueCashback");
        futureSourcePromo.set_start_date(FUTURE_START_DATE);
        futureSourcePromo.set_end_date(-1);
        futureSourcePromo.set_allowed_payment_methods(NMarket::NPaymentMethods::EPaymentMethods::YANDEX);
        {
            auto bc = futureSourcePromo.mutable_blue_cashback();
            bc->set_priority(4);
            bc->set_share(0.2);
            bc->set_version(5);
        }
        expectedPromoDetails[promoId] = futureSourcePromo;
    }
}


Y_UNIT_TEST_SUITE(YtPromoIndexerTests) {
    Y_UNIT_TEST_DECLARE(testWriteRead);
    Y_UNIT_TEST_DECLARE(testWriteReadBlue);
}

class TestTYtPromoIndexer: public YtPromoIndexer::TYtPromoIndexer {
    Y_UNIT_TEST_FRIEND(YtPromoIndexerTests, testWriteRead);
    Y_UNIT_TEST_FRIEND(YtPromoIndexerTests, testWriteReadBlue);
public:
    using TYtPromoIndexer::TYtPromoIndexer;
};


Y_UNIT_TEST_SUITE_IMPLEMENTATION(YtPromoIndexerTests) {

using namespace YtPromoIndexer;

Y_UNIT_TEST(testWriteRead) {
    const TFsPath outPath = "./tmp";
    TFsPath(outPath).MkDirs();
    NMarket::YtPromoindexer::TOptions params;
    params.set_outdir(outPath);

    for (auto version: {EVersion::V3}) {
        TestTYtPromoIndexer indexer(params, version);

        // making test data
        const TString promoKey1 = "MTIzNDU2Nzg5MDEyX25wMQ";
        const TString promoKey2 = "MTIzNDU2Nzg5MDEyX2Zvbw";
        const TString promoKey3 = "MTIzNDU2Nzg5MDEyX2Jhcg";

        TestTYtPromoIndexer::TPromoDetailsMap expectedPromoDetails;
        TestTYtPromoIndexer::TPromoDetailsMap unExpectedPromoDetails; // should be skipped
        TestTYtPromoIndexer::TMskuDetailsMap mskuPromos;

        MakePromo1(promoKey1, expectedPromoDetails, unExpectedPromoDetails);
        MakePromo2(promoKey2, expectedPromoDetails, unExpectedPromoDetails);

        auto allPromoDetails = expectedPromoDetails;
        for (const auto& pair : unExpectedPromoDetails) {
            allPromoDetails.insert(pair);
        }

        auto full_path = params.outdir() + '/' + OUTPUT_FILE_NAME_TEST;
        auto dumper = MakeHolder<TYtPromoDumper>(std::move(full_path), version);
        indexer.RunBlue3PWithData(*dumper, mskuPromos, allPromoDetails);
        dumper->WritePromoDetails();

        TestTYtPromoIndexer::TPromoDetailsMap actualPromoDetails;

        {
            auto reader2 = NMarket::NPromo::CreatePromoDetailsInfoReaderRaw(Market::NMmap::IMemoryRegion::MmapFile(params.outdir() + '/' + OUTPUT_FILE_NAME_TEST));
            auto&& funcLoad = [&actualPromoDetails](const TString& promoKey, const Market::Promo::PromoDetails& promoDetailsProto) {
                actualPromoDetails.emplace(promoKey, promoDetailsProto);
            };
            reader2->EnumeratePromoDetailsRaw(funcLoad);
        }

        ASSERT_EQ(expectedPromoDetails.size(), actualPromoDetails.size());
        for (const auto& [key, expectedProto] : expectedPromoDetails) {
            ASSERT_TRUE(actualPromoDetails.contains(key));

            const auto expectedRecord = *NProtobufHelpers::ProtobufToPromoDetails(expectedProto);
            const auto actualRecord = *NProtobufHelpers::ProtobufToPromoDetails(actualPromoDetails[key]);

            ASSERT_EQ(expectedRecord.Type, actualRecord.Type);
            ASSERT_EQ(expectedRecord.Description, actualRecord.Description);
            ASSERT_EQ(expectedRecord.StartDateUTC, actualRecord.StartDateUTC);
            ASSERT_EQ(expectedRecord.EndDateUTC, actualRecord.EndDateUTC);
            ASSERT_EQ(expectedRecord.AllowedPaymentMethods, actualRecord.AllowedPaymentMethods);
        }
    }
}


Y_UNIT_TEST(testWriteReadBlue) {
    const TFsPath outPath = "./tmp";
    TFsPath(outPath).MkDirs();
    NMarket::YtPromoindexer::TOptions params;
    params.set_outdir(outPath);

    TestTYtPromoIndexer indexer(params, EVersion::V3);

    // making test data
    const TString mskuId1 = "TestMSKU1";
    const TString promoIdIntFormat = "10204";
    const TString mskuId2 = "TestMSKU2";
    const TString promoIdStringFormat = "TestPromoSourceId2";
    const TString futurePromoIdStringFormat = "TestFuturePromoSourceId2";
    const TString futureBlueCashbackPromoId = "TestBlueCashbackPromoId";
    const u_int64_t expectedMSKUDetailsSize = 2;
    const u_int64_t futureMSKUDetailsSize = 1;

    TestTYtPromoIndexer::TMskuDetailsMap expectedMSKUDetails;
    TestTYtPromoIndexer::TMskuDetailsMap futureMSKUDetails;
    TestTYtPromoIndexer::TPromoDetailsMap expectedPromoDetails;
    NMarket::NPromo::TYtPromoDetailsStat expectedStat;

    MakePromo4(mskuId1, promoIdIntFormat, expectedMSKUDetails, futureMSKUDetails, expectedPromoDetails);
    MakePromo5(mskuId2, promoIdStringFormat, expectedMSKUDetails, futureMSKUDetails, expectedPromoDetails);
    MakePromo6(mskuId2, futurePromoIdStringFormat, expectedMSKUDetails, futureMSKUDetails, expectedPromoDetails);
    MakePromo7("", futureBlueCashbackPromoId, expectedMSKUDetails, futureMSKUDetails, expectedPromoDetails);

    auto allMSKUDetails = expectedMSKUDetails;
    for (const auto& [mskuDetailsKey, mskuVec]: futureMSKUDetails) {
        auto& mskuDetails = allMSKUDetails[mskuDetailsKey];
        mskuDetails.insert(mskuDetails.end(), mskuVec.begin(), mskuVec.end());
    }

    expectedStat.NumberOfPromos = expectedPromoDetails.size();
    expectedStat.NumberOfMSKU = expectedMSKUDetails.size() + futureMSKUDetails.size();

    auto full_path = params.outdir() + '/' + OUTPUT_FILE_NAME_TEST;
    auto dumper = MakeHolder<TYtPromoDumper>(std::move(full_path), EVersion::V3);
    indexer.RunBlue3PWithData(*dumper, allMSKUDetails, expectedPromoDetails);
    dumper->WritePromoDetails();
    indexer.WriteYtPromoStat();

    TestTYtPromoIndexer::TPromoDetailsMap actualPromoDetails;
    auto reader1 = NMarket::NPromo::CreatePromoDetailsInfoReader(Market::NMmap::IMemoryRegion::MmapFile(params.outdir() + '/' + OUTPUT_FILE_NAME_TEST));

    {
        auto reader2 = NMarket::NPromo::CreatePromoDetailsInfoReaderRaw(Market::NMmap::IMemoryRegion::MmapFile(params.outdir() + '/' + OUTPUT_FILE_NAME_TEST));
        auto&& funcLoad = [&actualPromoDetails](const TString& promoKey, const Market::Promo::PromoDetails& promoDetailsProto) {
            actualPromoDetails.emplace(promoKey, promoDetailsProto);
        };
        reader2->EnumeratePromoDetailsRaw(funcLoad);
    }

    ASSERT_EQ(expectedPromoDetails.size(), actualPromoDetails.size());
    for (const auto& [key, promoProto] : expectedPromoDetails) {
        ASSERT_TRUE(actualPromoDetails.contains(key));

        const auto expectedRecord = *NProtobufHelpers::ProtobufToPromoDetails(promoProto);
        const auto actualRecord = *NProtobufHelpers::ProtobufToPromoDetails(actualPromoDetails[key]);

        ASSERT_EQ(expectedRecord.Type, actualRecord.Type);
        ASSERT_EQ(expectedRecord.Description, actualRecord.Description);
        ASSERT_EQ(expectedRecord.StartDateUTC, actualRecord.StartDateUTC);
        ASSERT_EQ(expectedRecord.EndDateUTC, actualRecord.EndDateUTC);
        ASSERT_EQ(expectedRecord.AllowedPaymentMethods, actualRecord.AllowedPaymentMethods);
    }

    const auto& checkActualPromoDetails = [&expectedPromoDetails](const NMarket::NPromo::TMSKUDetails& expectedMSKUDetails,
        const NMarket::NPromo::TPromoDetailsPtr& promoActualRecord)
    {
        ASSERT_TRUE(promoActualRecord);

        const auto expectedRecord = *NProtobufHelpers::ProtobufToPromoDetails(expectedPromoDetails[expectedMSKUDetails.SourcePromoId]);
        ASSERT_EQ(expectedMSKUDetails.MarketPromoPrice.AsRaw(), promoActualRecord->MarketPrice.PromoPrice.AsRaw());
        ASSERT_EQ(expectedMSKUDetails.MarketOldPrice.AsRaw(), promoActualRecord->MarketPrice.OldPrice.AsRaw());
        ASSERT_EQ(expectedRecord.Type, promoActualRecord->Type);
        ASSERT_EQ(expectedRecord.Description, promoActualRecord->Description);
        ASSERT_EQ(expectedRecord.StartDateUTC, promoActualRecord->StartDateUTC);
        ASSERT_EQ(expectedRecord.EndDateUTC, promoActualRecord->EndDateUTC);
        ASSERT_EQ(expectedRecord.AllowedPaymentMethods, promoActualRecord->AllowedPaymentMethods);
    };

    ASSERT_EQ(expectedMSKUDetails.size(), expectedMSKUDetailsSize);
    for (const auto& expectedMSKU : expectedMSKUDetails) {
        const auto& key = expectedMSKU.first;

        for (const auto& mskuRecord : expectedMSKU.second)
        {
            const auto& promoActualRecord = reader1->GetPromoDetailsByMSKU(key, NOW);
            checkActualPromoDetails(mskuRecord, promoActualRecord);
        }
    }

    ASSERT_EQ(futureMSKUDetails.size(), futureMSKUDetailsSize);
    for (const auto& futureMSKU : futureMSKUDetails) {
        const auto& key = futureMSKU.first;

        for (const auto& mskuRecord : futureMSKU.second)
        {
            const auto &promoActualRecord = reader1->GetMostRelevantPromoDetailsByMSKU(key, FUTURE_START_DATE);
            checkActualPromoDetails(mskuRecord, promoActualRecord);
        }
    }

    const auto statFileName = params.outdir() + "/yt_promo_stats.pb";
    NMarket::NPromo::TYtPromoDetailsStat realStat = NMarket::NPromo::ReadYtPromoStatFromProtobuf(statFileName);
    ASSERT_EQ(realStat.NumberOfPromos, expectedStat.NumberOfPromos);
    ASSERT_EQ(realStat.NumberOfMSKU, expectedStat.NumberOfMSKU);
    ASSERT_EQ(realStat.GenerationTime, expectedStat.GenerationTime);
    ASSERT_EQ(realStat.MmapSize, expectedStat.MmapSize);
}
}
