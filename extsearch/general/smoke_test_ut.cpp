#include <extsearch/images/robot/index/shardwriter/lib/download/download.h>
#include <extsearch/images/robot/index/shardwriter/lib/download/download_mode.h>

#include <extsearch/images/robot/library/opt/opt.h>
#include <extsearch/images/robot/library/opt/tier_names.h>
#include <extsearch/images/robot/library/tables/shardpreparer_paths.h>
#include <extsearch/images/robot/library/shard_tables/common.h>

#include <extsearch/images/robot/library/yt_test/client.h>

#include <library/cpp/testing/unittest/registar.h>

constexpr char IndexPrefix[] = {"images_test"};
constexpr char IndexState[] = {"29991332-256161"};
constexpr NImages::ETierName TierName = {NImages::ETierName::primary};
constexpr ui64 ShardNumber = 2;
constexpr char OutDir[] = {"./"};

namespace {
    void GeneralizedTestFun(NImages::EDownloadMode mode, const TString& tableName) {
        // Creating options for shardwriter
        NImages::TOptions options;

        options.IndexPrefix = IndexPrefix;
        options.IndexState = IndexState;
        options.TierName = TierName;

        options.ShardNumber = ShardNumber;

        options.OutDir = OutDir;

        // mock client
        NYT::IClientBasePtr testClient(new TClientBaseMock(".", "//home/", true));

        // now creating table to correctly run download function
        NImages::NIndex::CreateShardTable(testClient, tableName);

        {
            auto writer = testClient->CreateTableWriter<NYT::TNode>(tableName);
            {
                auto shredStream = NImages::NIndex::TStandardTableRowShredStream(ShardNumber, "file1.txt", writer.Get());
                shredStream << "some_meaningless_data_to_make_non_empty_content";
                shredStream.Finish();
            }
            {
                auto shredStream = NImages::NIndex::TStandardTableRowShredStream(ShardNumber, "file2.txt", writer.Get());
                shredStream << "some_meaningless_data_to_make_non_empty_content_version_2";
                shredStream.Finish();
            }
        }

        DownloadShard(testClient, options, mode);

        NFs::EnsureExists(TString(OutDir) + "file1.txt");
        NFs::EnsureExists(TString(OutDir) + "file2.txt");

        NFs::Remove(TString(OutDir) + "file1.txt");
        NFs::Remove(TString(OutDir) + "file2.txt");
    }
} // end of anonymous namespace

Y_UNIT_TEST_SUITE(Shardwriter_Download_Tests){

    Y_UNIT_TEST(MatchingTest){
        const NImages::TShardPreparerPaths shardPaths(IndexPrefix, IndexState, NImages::TTierOpts{TierName}.GetTierName());

        GeneralizedTestFun(NImages::EDownloadMode::PantherKeyWad, shardPaths.GetPantherKeyWadShardTable().GetYtPath());
        GeneralizedTestFun(NImages::EDownloadMode::PantherInvWad, shardPaths.GetPantherInvWadShardTable().GetYtPath());
        GeneralizedTestFun(NImages::EDownloadMode::PantherInfo, shardPaths.GetPantherInfoShardTable().GetYtPath());

        GeneralizedTestFun(NImages::EDownloadMode::KeyInvWad, shardPaths.GetKeyInvWadShardTable().GetYtPath());
        GeneralizedTestFun(NImages::EDownloadMode::KeyInvWadAttrs, shardPaths.GetKeyInvAttrsWadShardTable().GetYtPath());

        GeneralizedTestFun(NImages::EDownloadMode::Thumbs, shardPaths.GetThumbShardTable().GetYtPath());
        GeneralizedTestFun(NImages::EDownloadMode::Alive, shardPaths.GetAliveShardTable().GetYtPath());
        GeneralizedTestFun(NImages::EDownloadMode::ThumbsTaas, shardPaths.GetThumbTaasInfo().GetYtPath());
        GeneralizedTestFun(NImages::EDownloadMode::AliveTaas, shardPaths.GetAliveTaasShardTable().GetYtPath());
        GeneralizedTestFun(NImages::EDownloadMode::PortionMap, shardPaths.GetPortionMapTaasFileTable().GetYtPath());

        GeneralizedTestFun(NImages::EDownloadMode::Cbir, shardPaths.GetCbirIndexShardTable().GetYtPath());
        GeneralizedTestFun(NImages::EDownloadMode::CbirAA, shardPaths.GetCbirAAShardTable().GetYtPath());
        GeneralizedTestFun(NImages::EDownloadMode::CbirFeaturesAndMeta, shardPaths.GetCbirShardTable().GetYtPath());

        GeneralizedTestFun(NImages::EDownloadMode::Archive, shardPaths.GetArchiveShardTable().GetYtPath());
    }

}

