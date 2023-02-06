#include <library/cpp/testing/unittest/registar.h>
#include <crypta/graph/engine/score/native/lib/stats.h>
#include <crypta/graph/engine/score/native/lib/server.h>
#include <crypta/lib/native/identifiers/lib/id_types/all.h>
#include <crypta/lib/native/identifiers/lib/generic.h>
#include <util/random/random.h>

Y_UNIT_TEST_SUITE(TUnitStatsTest) {
    const static double EPS = 1e-7;

    using NCrypta::NIdentifiersProto::NIdType::EIdType;
    using NCrypta::NGraphEngine::TStatsOptions;
    using NCrypta::NGraphEngine:: TGraphStatsArguments;

    void AddVertex(NCrypta::NGraphEngine::TGraph * graph, const TString& name, const EIdType& type) {
        auto item = NIdentifiers::IDENTIFIERS_MAP<TString>.find(type);
        UNIT_ASSERT(item != NIdentifiers::IDENTIFIERS_MAP<TString>.end());
        auto id = item->second(name);
        graph->AddVertices()->MergeFrom(id->ToProto());
    }

    void AddVertices(NCrypta::NGraphEngine::TGraph * graph, ui64 count, const EIdType& type) {
        for (ui64 i = 0; i < count; ++i) {
            AddVertex(graph, NIdentifiers::TGenericID::Next(type), type);
        }
    }

    void AddVertices(NCrypta::NGraphEngine::TGraph * graph, const TVector<TString>& names, const EIdType& type) {
        for (const auto& name : names) {
            AddVertex(graph, name, type);
        }
    }

    NCrypta::NGraphEngine::TGraph GenerateSimpleGraph() {
        NCrypta::NGraphEngine::TGraph graph;
        graph.SetId(1);

        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail(NIdentifiers::TEmail::Next()).ToProto());

        return graph;
    }

    Y_UNIT_TEST(StatsTest) {
        auto graph = GenerateSimpleGraph();

        TStatsOptions options;
        auto stats = NCrypta::NGraphEngine::CollectProdStats(graph, options);

        UNIT_ASSERT_EQUAL(graph.GetId(), stats.GetId());
        UNIT_ASSERT(stats.GetScores().size() > 0);
    }

    Y_UNIT_TEST(HumanMultiHistogramStrategyTest) {
        TStatsOptions options;
        const auto strategy = NCrypta::NGraphEngine::GetProdScoringStrategy(options);
        NCrypta::NGraphEngine::TGraph graph;
        graph.SetId(123);
        SetRandomSeed(42);
        AddVertices(&graph, {"masha", "mariya", "ma"}, EIdType::LOGIN);
        AddVertices(&graph, {"aba1095@gmail.com", "abacaba@gmail.com", "abacaba@yandex.ru", "daba1095@mail.ru"}, EIdType::EMAIL);

        AddVertices(&graph, 2, EIdType::PHONE);
        AddVertices(&graph, 10, EIdType::YANDEXUID);
        AddVertices(&graph, 1, EIdType::VK_ID);

        auto scores = strategy.ComputeGraphScores(graph).GetScoreList();
        THashMap<TString, NCrypta::NGraphEngine::TScoreBase> scoreMap{};
        for (const auto& score : scores) {
            scoreMap[score.Name] = score;
        }

        UNIT_ASSERT_DOUBLES_EQUAL(0.3, scoreMap["logins_count"].Score, EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0.3, scoreMap["emails_count"].Score, EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0.6, scoreMap["phones_count"].Score, EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0.6, scoreMap["vk_count"].Score, EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(2.797202797e-05, scoreMap["vertices_count"].Score, EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0.12, scoreMap["logins_lcs"].Score, EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0.11, scoreMap["emails_lcs"].Score, EPS);
    }

    Y_UNIT_TEST(TGraphScoreEngineTest) {
        auto graph = GenerateSimpleGraph();
        TGraphStatsArguments args;
        args.MutableGraph()->MergeFrom(graph);
        NCrypta::NGraphEngine::TStats stats;
        NCrypta::NGraphEngine::TGraphScoreEngineImpl graphScoreEngine;
        graphScoreEngine.ComputeStats(nullptr, &args, &stats);

        UNIT_ASSERT_EQUAL(graph.GetId(), stats.GetId());
        UNIT_ASSERT(stats.GetScores().size() > 0);
    }
}
