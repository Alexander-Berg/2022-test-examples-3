#include "parser_test_runner.h"
#include "test_utils.h"

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>
#include <market/idx/feeds/qparser/lib/parse_stats.h>
#include <market/idx/datacamp/proto/api/UpdateTask.pb.h>

#include <util/folder/tempdir.h>
#include <util/system/fs.h>

using namespace NMarket;

static const TString OutputMagic = "PRST";

void FillFileWithStats(const TString partnerStatsFile = "") {
    Market::DataCamp::API::PartnerStats partnerStats;

    partnerStats.set_total_offers(12);
    partnerStats.set_error_offers(3);
    partnerStats.set_warning_offers(3);
    partnerStats.set_unloaded_offers(3);
    partnerStats.set_loaded_offers(9);
    partnerStats.set_ignored_offers(1);

    auto ProtoDumper = MakeHolder<NMarket::TSnappyProtoWriter>(partnerStatsFile, OutputMagic);
    ProtoDumper->Write(partnerStats);
    ProtoDumper.Reset();
}


TEST(PartnerStats, PartnerStatsFile) {
    TTempDir tempDir;
    TString partnerStatsFilepath = JoinFsPaths(tempDir.Path(), "partner_stats.pbuf.sn");

    FillFileWithStats(partnerStatsFilepath);

    ASSERT_TRUE(NFs::Exists(partnerStatsFilepath));

     Market::DataCamp::API::PartnerStats partnerStats;
     NMarket::TSnappyProtoReader reader(partnerStatsFilepath, OutputMagic);
     reader.Load(partnerStats);

     // проверка на то, что значения записались и считываются из созданного файла
     ASSERT_EQ(partnerStats.total_offers(), 12);
     ASSERT_EQ(partnerStats.error_offers(), 3);
     ASSERT_EQ(partnerStats.warning_offers(), 3);
     ASSERT_EQ(partnerStats.unloaded_offers(), 3);
     ASSERT_EQ(partnerStats.loaded_offers(), 9);
     ASSERT_EQ(partnerStats.ignored_offers(), 1);
}



