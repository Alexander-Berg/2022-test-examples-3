#include <market/idx/generation/genlog_dumper/dumpers/Business2ServiceDumper.h>
#include <market/idx/generation/genlog_dumper/dumpers/Service2SeqnumDumper.h>

#include <market/library/flat_helpers/flat_helpers.h>

#include <market/library/offer_ware_md5/offer_ware_md5.h>

#include <library/cpp/testing/unittest/gtest.h>

#include <util/folder/path.h>
#include <util/folder/tempdir.h>
#include <util/stream/file.h>
#include <util/stream/output.h>
#include <util/string/vector.h>

#include <vector>

using TServiceOfferParams = MarketIndexer::GenerationLog::Record::ServiceOffersInfo::ServiceOfferParams;

TVector<MarketIndexer::GenerationLog::Record> CreateTestRecords() {
    TVector<MarketIndexer::GenerationLog::Record> records;

    auto addRecord = [&](const TVector<TServiceOfferParams>& params) {
        records.emplace_back();
        MarketIndexer::GenerationLog::Record& record = records.back();
        auto info = record.mutable_service_offers_info();
        info->mutable_all_service_offers_params()->Add(params.begin(), params.end());
    };

    auto addParams = [](TVector<TServiceOfferParams>& paramsVec,
                        ui32 seqNum, ui32 feedId, TString wareMd5, bool isExpress, bool isCpa) {
        TServiceOfferParams params;
        params.set_sequence_number(seqNum);
        params.set_feed_id(feedId);
        params.set_ware_md5(wareMd5);
        params.set_is_express(isExpress);
        params.set_is_cpa(isCpa);
        paramsVec.emplace_back(std::move(params));
    };

    {
        // business offer with express only offers
        TVector<TServiceOfferParams> paramsVec;
        addParams(paramsVec, 0, 124, "EWAAmKC-qb-Uqk2yJqgRvg", true, false);
        addParams(paramsVec, 2134, 100, "Sx3NCrXw9aU99AyUg7LXfg", true, false);
        addParams(paramsVec, 621, 45743, "F_0LFbqPBdvA76QCKenUtA", true, false);
        addRecord(paramsVec);
    }
    {
        // not a business offer (no service_offers_info), for example smth from filtered_genlog
        TVector<TServiceOfferParams> paramsVec;
        addRecord(paramsVec);
    }
    {
        // business offer with cpa only offers
        TVector<TServiceOfferParams> paramsVec;
        addParams(paramsVec, 7, 346, "9mmkqpo6tPW251z_zBOPfA", false, true);
        addParams(paramsVec, 4, 2446, "0fv1aIeddLS24GXiEChMig", false, true);
        addRecord(paramsVec);
    }
    {
        // business offer with cpa and express
        TVector<TServiceOfferParams> paramsVec;

        addParams(paramsVec, 9235, 252, "LPFDuV01i8bXw5k_bpRjJw", true, false);
        addParams(paramsVec, 235, 261246, "TiuMDVzblHHao8S6EI0kyw", true, false);
        addParams(paramsVec, 3636, 8542, "MutPXBPrnI8_mEKEmB9e5g", true, false);

        addParams(paramsVec, 2357, 7272, "vTil9GPx3fTyumnINFm16g", false, true);
        addParams(paramsVec, 987, 54843, "OdJ5rEqQUjxJzN5Gap5Wsg", false, true);

        addRecord(paramsVec);
    }
    return records;
}

TEST(Business2ServiceDumper, DumpMapping)
{
    const auto records = CreateTestRecords();
    TTempDir dir;

    NDumpers::TDumperContext context(dir.Name(), false);
    auto dumper = NDumpers::MakeBusiness2ServiceDumper(context);
    for (size_t i = 0; i < records.size(); ++i) {
        dumper->ProcessGenlogRecord(records[i], i);
    }
    dumper->Finish();

    const auto fileData = TUnbufferedFileInput(dir.Path() / "business2service.fb").ReadAll();
    const auto* mappingVec = NMarket::NFlatbufferHelpers::GetTBusiness2ServiceMapping(fileData)->Offers();
    const auto fileDataUnited = TUnbufferedFileInput(dir.Path() / "business2service-united.fb").ReadAll();
    const auto* mappingUnitedVec = NMarket::NFlatbufferHelpers::GetTBusiness2ServiceMappingUnited(fileDataUnited)->Offers();

    ASSERT_EQ(mappingVec->size(), records.size());
    ASSERT_EQ(mappingUnitedVec->size(), records.size());

    {
        const auto* offsets = mappingVec->Get(0);
        const auto* express = offsets->HyperlocalExpressOffsetsSortedByFeedId();
        const auto* cpa = offsets->CpaOffsetsSortedByFeedId();
        ASSERT_EQ(TVector<ui32>(express->begin(), express->end()), TVector<ui32>({2134, 0, 621}));
        ASSERT_EQ(cpa->size(), 0);

        const auto* offsetsUnited = mappingUnitedVec->Get(0);
        const auto* all = offsetsUnited->Offsets();
        ASSERT_EQ(TVector<ui32>(all->begin(), all->end()), TVector<ui32>({2134, 0, 621}));
    }
    {
        const auto* offsets = mappingVec->Get(1);
        const auto* express = offsets->HyperlocalExpressOffsetsSortedByFeedId();
        const auto* cpa = offsets->CpaOffsetsSortedByFeedId();
        ASSERT_EQ(express->size(), 0);
        ASSERT_EQ(cpa->size(), 0);

        const auto* offsetsUnited = mappingUnitedVec->Get(1);
        const auto* all = offsetsUnited->Offsets();
        ASSERT_EQ(all->size(), 0);
    }
    {
        const auto* offsets = mappingVec->Get(2);
        const auto* express = offsets->HyperlocalExpressOffsetsSortedByFeedId();
        const auto* cpa = offsets->CpaOffsetsSortedByFeedId();
        ASSERT_EQ(express->size(), 0);
        ASSERT_EQ(TVector<ui32>(cpa->begin(), cpa->end()), TVector<ui32>({7, 4}));

        const auto* offsetsUnited = mappingUnitedVec->Get(2);
        const auto* all = offsetsUnited->Offsets();
        ASSERT_EQ(TVector<ui32>(all->begin(), all->end()), TVector<ui32>({7, 4}));
    }
    {
        const auto* offsets = mappingVec->Get(3);
        const auto* express = offsets->HyperlocalExpressOffsetsSortedByFeedId();
        const auto* cpa = offsets->CpaOffsetsSortedByFeedId();
        ASSERT_EQ(TVector<ui32>(express->begin(), express->end()), TVector<ui32>({9235, 3636, 235}));
        ASSERT_EQ(TVector<ui32>(cpa->begin(), cpa->end()), TVector<ui32>({2357, 987}));

        const auto* offsetsUnited = mappingUnitedVec->Get(3);
        const auto* all = offsetsUnited->Offsets();
        ASSERT_EQ(TVector<ui32>(all->begin(), all->end()), TVector<ui32>({9235, 2357, 3636, 987, 235}));
    }
}

TEST(Service2SeqnumDumper, DumpMapping)
{
    const auto records = CreateTestRecords();
    TTempDir dir;

    NDumpers::TDumperContext context(dir.Name(), false);
    auto dumper = NDumpers::MakeService2SeqnumDumper(context);
    for (size_t i = 0; i < records.size(); ++i) {
        dumper->ProcessGenlogRecord(records[i], i);
    }
    dumper->Finish();

    const auto fileData = TUnbufferedFileInput(dir.Path() / "service2seqnum.fb64").ReadAll();
    const auto* mappingVec = NMarket::NFlatbufferHelpers::GetTService2SeqnumMapping(fileData)->Offers();

    TVector<std::tuple<NMarket::TBinaryWareMd5, ui32, ui32>> order; // service offer ware_md5, service offer seqNum, business offer seqNum

    for (ui32 recordIdx = 0; recordIdx < records.size(); ++recordIdx) {
        const auto& record = records[recordIdx];

        if (record.has_service_offers_info()) {
            const auto& info = record.service_offers_info();
            int serviceCount = info.all_service_offers_params_size();

            for (int i = 0; i < serviceCount; ++i) {
                const auto& offer = info.all_service_offers_params(i);
                NMarket::TBinaryWareMd5 wareMd5Decoded;
                bool success = TryFromString(offer.ware_md5(), wareMd5Decoded);
                ASSERT_TRUE(success);
                order.emplace_back(wareMd5Decoded, offer.sequence_number(), recordIdx);
            }
        }
    }

    Sort(order.begin(), order.end(), [](const auto& lhs, const auto& rhs) {
        const auto& lw = std::get<0>(lhs);
        const auto& rw = std::get<0>(rhs);
        return (lw.first == rw.first ? lw.second < rw.second : lw.first < rw.first);
    });

    ASSERT_EQ(mappingVec->size(), order.size());

    for (size_t i = 0; i < order.size(); ++i) {
        const auto* item = mappingVec->Get(i);

        ASSERT_EQ(item->ServiceBinaryWareMd5().Upper(), std::get<0>(order[i]).first);
        ASSERT_EQ(item->ServiceBinaryWareMd5().Lower(), std::get<0>(order[i]).second);
        ASSERT_EQ(item->ServiceSeqnum(), std::get<1>(order[i]));
        ASSERT_EQ(item->BusinessSeqnum(), std::get<2>(order[i]));
    }
}
