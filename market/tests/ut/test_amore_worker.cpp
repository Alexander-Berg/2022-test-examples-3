#include <market/amore/ms_mapper/mapper.h>
#include <market/idx/offers/lib/iworkers/BidWorker.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

TEST(TAmoreBidWorker, AmoreFee) {

    auto AmoreDataFromCpa = [](uint16_t fee) {
        using namespace NMarket::NAmore;
        using namespace NMarket::NAmore::NStrategy;

        TString ret;
        ret.resize(NStrategy::StrategyBundleSize);
        NMSMapper::BinaryData binData{(int8_t*)ret.data(), ret.size()};
        const TStrategyBundle bundle{1, NStrategy::TCpa{StrategyType::CPA, fee}, TOfferStrategy{}};
        NMSMapper::Mapper::MapAmoreBundle(bundle, binData);
        return ret;
    };

    const auto amoreData = AmoreDataFromCpa(333);

    MarketIndexer::GenerationLog::Record glRecord;
    TOfferCtx offerContext;

    glRecord.set_fee(111);
    glRecord.set_amore_data(amoreData);

    auto worker = MakeHolder<TAmoreBidWorker>();
    worker->ProcessOffer(&glRecord, &offerContext);

    ASSERT_EQ(glRecord.fee(), 333);
}


