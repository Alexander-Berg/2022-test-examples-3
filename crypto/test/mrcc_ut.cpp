#include <crypta/graph/mrcc_opt/lib/mrcc.h>
#include <crypta/graph/mrcc_opt/lib/data.h>
#include <library/cpp/testing/unittest/registar.h>
#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>
#include <util/random/random.h>
#include <crypta/graph/mrcc_opt/lib/tester.h>

using namespace NYT;
using namespace NYT::NTesting;
using namespace NConnectedComponents;
using namespace NTest;

Y_UNIT_TEST_SUITE(TUnitNConnectedComponents) {

    Y_UNIT_TEST(SimpleTest) {
        NYT::JoblessInitialize();
        auto client = CreateTestClient();

        // ___________________________generate data
        NConnectedComponents::TGeneralDataView dataView(
            {"u"}, {"v"},  // view of source (edges)
            {"u"}, "component" // view of destination (vertices)
        );
        TDataPaths dataPaths = PrepareWorkdir(client, dataView);
        CreateTable(client, dataPaths.SourceData, {
            TNode()("u", 1)("v", 2),
            TNode()("u", 4)("v", 1),
            TNode()("u", 3)("v", 4),
            TNode()("u", 3)("v", 5),

            TNode()("u", 12)("v", 12),

            TNode()("u", 100)("v", 0),
            TNode()("u", 0)("v", 101)
        });

        // ___________________________run
        auto mrcc = TOptimizedStars<TGeneralDataView, ui64>(client, dataPaths);
        mrcc.Run(15);

        // ___________________________check
        NTest::ComponentsTester<ui64> tester(client, dataPaths);
        UNIT_ASSERT(tester.CheckComponents(client, dataPaths));

        client->Remove(dataPaths.Workdir, NYT::TRemoveOptions().Recursive(true));
    }

    Y_UNIT_TEST(SimpleTestWithPreviousLabels) {
        NYT::JoblessInitialize();
        auto client = CreateTestClient();

        // ___________________________generate data
        NConnectedComponents::TGeneralDataView dataView(
            {"u"}, {"v"},
            {"u"}, "component"
        );
        TDataPaths dataPaths = PrepareWorkdir(client, dataView, true);
        CreateTable(client, dataPaths.SourceData, {
            TNode()("u", 1)("v", 2),
            TNode()("u", 2)("v", 3),
            TNode()("u", 3)("v", 4),
            TNode()("u", 4)("v", 5),

            TNode()("u", 6)("v", 7)
        });
        CreateTable(client, dataPaths.PreviousLabels, {
            TNode()("u", 1)("component", 100),
            TNode()("u", 2)("component", 100),
            TNode()("u", 3)("component", 100),
            TNode()("u", 10)("component", 100)
        });

        // ___________________________run
        auto mrcc = TOptimizedStars<TGeneralDataView, ui64>(client, dataPaths);
        mrcc.Run(15);

        // ___________________________check
        NTest::ComponentsTester<ui64> tester(client, dataPaths);
        UNIT_ASSERT(tester.CheckComponents(client, dataPaths));

        client->Remove(dataPaths.Workdir, NYT::TRemoveOptions().Recursive(true));
    }


    Y_UNIT_TEST(CompleteVerticesTest) {
        NYT::JoblessInitialize();
        auto client = CreateTestClient();

        // ___________________________generate data. Vertex -- pair (id, id_type)
        NConnectedComponents::TGeneralDataView dataView(
            {"id1", "id1Type"}, {"id2", "id2Type"},
            {"id", "id_type"}, "component_id"
        );
        TDataPaths dataPaths = PrepareWorkdir(client, dataView);
        CreateTable(client, dataPaths.SourceData, {
            TNode()("id1", "1")("id1Type", "1")("id2", "2")("id2Type", "1"),
            TNode()("id1", "3")("id1Type", "1")("id2", "2")("id2Type", "1"),
            TNode()("id1", "3")("id1Type", "1")("id2", "4")("id2Type", "1"),

            TNode()("id1", "1")("id1Type", "0")("id2", "2")("id2Type", "0"),
        });

        // ___________________________run
        auto mrcc = TOptimizedStars<TGeneralDataView, ui64>(client, dataPaths);
        mrcc.Run(15);

        // ___________________________check
        NTest::ComponentsTester<ui64> tester(client, dataPaths);
        UNIT_ASSERT(tester.CheckComponents(client, dataPaths));

        client->Remove(dataPaths.Workdir, NYT::TRemoveOptions().Recursive(true));
    }
}
