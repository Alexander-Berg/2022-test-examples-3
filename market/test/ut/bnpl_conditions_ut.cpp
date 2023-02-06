#include <market/library/bnpl_conditions/bnpl_conditions.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include <util/stream/file.h>

using namespace NMarket::NBnplConditions;

Y_UNIT_TEST_SUITE(TestBnplConditions) {
    Y_UNIT_TEST(TestFile) {
        const auto path = JoinFsPaths(ArcadiaSourceRoot(), "market/svn-data/package-data/bnpl_conditions.json");
        TBnplConditions conditions;
        try {
            NBnplReader::LoadBnplConditions(path, conditions);
        } catch (yexception& e) {
            Cerr << "File " << path << ": " << e.what() << Endl;
            UNIT_ASSERT(false);
        }
    }
}
