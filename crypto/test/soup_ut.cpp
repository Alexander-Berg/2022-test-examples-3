#include <crypta/graph/mrcc_opt/soup/lib/soup.h>
#include <crypta/graph/mrcc_opt/lib/data.h>
#include <library/cpp/testing/unittest/registar.h>
#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>
#include <crypta/graph/mrcc_opt/lib/tester.h>

using namespace NYT;
using namespace NYT::NTesting;
using namespace NConnectedComponents;
using namespace NTest;

Y_UNIT_TEST_SUITE(TUnitNConnectedComponents) {


    Y_UNIT_TEST(SoupTest) {
        NYT::JoblessInitialize();
        auto client = CreateTestClient();

        // ___________________________generate data. Vertex -- pair (id, id_type)
        TSoupView dataView;
        TDataPaths dataPaths = NTest::PrepareWorkdir(client, dataView);
        CreateTable(client, dataPaths.SourceData, {
            TNode()("id1", "1")("id1Type", "1")("id2", "2")("id2Type", "1"),
            TNode()("id1", "3")("id1Type", "1")("id2", "2")("id2Type", "1"),
            TNode()("id1", "3")("id1Type", "1")("id2", "4")("id2Type", "1"),

            TNode()("id1", "1")("id1Type", "0")("id2", "2")("id2Type", "0"),
        });

        // ___________________________run
        auto mrcc = NConnectedComponents::TOptimizedStars<TSoupView, ui64>(client, dataPaths);
        mrcc.Run(15);

        // ___________________________check
        NConnectedComponents::TGeneralDataView soupView(
            {"id1", "id1Type"}, {"id2", "id2Type"},
            {"id", "id_type"}, "component_id"
        );
        TDataPaths generalDataPaths(soupView, dataPaths.SourceData,
            dataPaths.DestinationComponents, dataPaths.Workdir, dataPaths.PreviousLabels);
        NTest::ComponentsTester<ui64> tester(client, generalDataPaths);
        UNIT_ASSERT(tester.CheckComponents(client, generalDataPaths));

        client->Remove(dataPaths.Workdir, NYT::TRemoveOptions().Recursive(true));
    }

    Y_UNIT_TEST(SoupRecountingTest) {
        NYT::JoblessInitialize();
        auto client = CreateTestClient();

        // ___________________________generate data. Vertex -- pair (id, id_type)
        TSoupView dataView;
        TDataPaths dataPaths = NTest::PrepareWorkdir(client, dataView, false, NConnectedComponents::NSoup::NPaths::MAIN_WORKDIR);
        client->Create("//home/crypta/production/state/graph/v2/soup/cooked", NYT::ENodeType::NT_MAP, NYT::TCreateOptions().IgnoreExisting(true).Recursive(true));


        dataPaths.SourceData = NConnectedComponents::NSoup::NPaths::MAIN_SOUP_TABLE;
        dataPaths.DestinationComponents = NConnectedComponents::NSoup::NPaths::MAIN_COMPONENTS_TABLE;
        CreateTable(client, dataPaths.SourceData, {
            TNode()("id1", "1")("id1Type", "yandexuid")("id2", "2")("id2Type", "yandexuid"),

            TNode()("id1", "3")("id1Type", "yandexuid")("id2", "4")("id2Type", "yandexuid"),
        });
        client->Set(NConnectedComponents::NSoup::GetPathToDateAttribute(dataPaths.SourceData), "2019-04-23");

        // ___________________________run
        NConnectedComponents::NSoup::RecountComponents(client);
        UNIT_ASSERT(client->Get(NConnectedComponents::NSoup::GetPathToDateAttribute(dataPaths.DestinationComponents)) == "2019-04-23");

        // ___________________________rerun
        CreateTable(client, dataPaths.SourceData, {
            TNode()("id1", "1")("id1Type", "yandexuid")("id2", "3")("id2Type", "yandexuid")},
            true
        );
        client->Set(NConnectedComponents::NSoup::GetPathToDateAttribute(dataPaths.SourceData), "2019-04-24");
        NConnectedComponents::NSoup::RecountComponents(client);

        // ___________________________check
        NConnectedComponents::TGeneralDataView soupView(
            {"id1", "id1Type"}, {"id2", "id2Type"},
            {"id", "id_type"}, "component_id"
        );
        TDataPaths generalDataPaths(soupView, dataPaths.SourceData,
            dataPaths.DestinationComponents, dataPaths.Workdir, dataPaths.PreviousLabels);
        NTest::ComponentsTester<ui64> tester(client, generalDataPaths);
        UNIT_ASSERT(tester.CheckComponents(client, generalDataPaths));

       UNIT_ASSERT(client->Get(NConnectedComponents::NSoup::GetPathToDateAttribute(dataPaths.DestinationComponents)) == "2019-04-24");



        client->Remove(dataPaths.Workdir, NYT::TRemoveOptions().Recursive(true));
    }
}
