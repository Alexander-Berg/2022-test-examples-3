#include <search/begemot/rules/src_setup/talents/gzt_rest_extract/lib/gzt_rest_extract.h>
#include <search/begemot/rules/src_setup/talents/proto/talents.pb.h>

#include <kernel/search_daemon_iface/reqtypes.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/common/env.h>

#include <util/memory/blob.h>


TRichNodePtr MakeTreeFromQuery(const TString& query)
{
    TUtf16String wdata = UTF8ToWide(query);
    TLanguageContext lang(LI_DEFAULT_REQUEST_LANGUAGES, 0);
    return CreateRichNode(wdata, TCreateTreeOptions(lang, RPF_ENABLE_EXTENDED_SYNTAX));
}

void DoTest(const TString& input, const TString& expected)
{
    TGazetteer GztMarkers(TBlob::FromFile(ArcadiaSourceRoot() + "/search/begemot/rules/src_setup/talents/gzt_rest_extract/ut/data/talents.gzt.bin"));
    auto tree = MakeTreeFromQuery(input);
    TGztResults results(tree, &GztMarkers);
    TGztRestExtractor restExtractor(tree);
    for (auto it = results.FindByExactType(TTalentsMarker::descriptor()); it.Ok(); ++it) {
        restExtractor.Feed(it);
    }
    TString res = restExtractor.GetResult();
    Cout << "'" << input << "' -> " << "'" << res << "'" << Endl;
    UNIT_ASSERT_VALUES_EQUAL(restExtractor.GetResult(), expected);
}

Y_UNIT_TEST_SUITE(GztRestExtractorTest) {
    Y_UNIT_TEST(Trivial) {
        DoTest("работа медсестрой измерять температуру вакансии", "измерять температуру");
    }
    Y_UNIT_TEST(WithSubnodes) {
        DoTest("работа инженером собирать процессоры 80x86 вакансии", "собирать процессоры 80 x 86");
    }
    Y_UNIT_TEST(WithLogic) {
        DoTest("собирать (детали|железо) (вакансии|работа) слесарем", "собирать детали железо");
    }
    Y_UNIT_TEST(Empty) {
        DoTest("", "");
    }
    Y_UNIT_TEST(WithNumbers) {
        DoTest("работа для студентов 19 лет", "19 лет");
    }
    Y_UNIT_TEST(CutAll) {
        DoTest("работа вакансии медсестрой без опыта", "");
    }
    Y_UNIT_TEST(WithLatin) {
        DoTest("работа в компании company на позиции developer", "в компании company на позиции");
    }
    Y_UNIT_TEST(StrangeQuery) {
        DoTest("!@#$%^&*()_+?/\";;:',.[]{}<>\\`~| я хакер", "я хакер");
    }

    Y_UNIT_TEST(SameWords) {
        DoTest("работа без опыта в кафе приносит много нового опыта", "приносит много нового опыта");
    }

    Y_UNIT_TEST(DeepQuery) {
        DoTest("работа на (острове или в (сундуке|(коробке | (на игле в яйце)))|(территории & охраняемой))",
               "на острове или в сундуке коробке на игле в яйце территории охраняемой");
    }

    Y_UNIT_TEST(DuplicateWords) {
        DoTest("работа работа работа работа отдых работа работа работа работа", "отдых");
    }

    Y_UNIT_TEST(Morphology) {
        DoTest("хочу работу телохранителя", "хочу");
    }
}
