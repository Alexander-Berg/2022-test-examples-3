#include <market/idx/datacamp/miner/processors/ware_md5_creator/ware_md5_creator.h>

#include <market/idx/datacamp/miner/lib/test_utils.h>

#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>

#include <library/cpp/testing/unittest/gtest.h>


TEST (BlueWareMd5Test, RecalculateWareMd5)
{
    NMiner::TDatacampOffer offer = MakeDefault();
    offer.mutable_identifiers()->mutable_extra()->set_ware_md5("PRECALCULATED");

    CalculateMd5<NMarket::EMarketColor::MC_BLUE>(offer, NMiner::TWareMd5CreatorConfig("test"));

    ASSERT_EQ(std::string{offer.identifiers().extra().ware_md5()}, "PRECALCULATED");
}
