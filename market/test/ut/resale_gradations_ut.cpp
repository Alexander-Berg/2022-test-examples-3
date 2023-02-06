#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include <market/library/resale_gradations/resale_gradations.h>

Y_UNIT_TEST_SUITE(TestResaleGradations) {
    Y_UNIT_TEST(TestFile) {
        const auto path = JoinFsPaths(ArcadiaSourceRoot(), "market/svn-data/package-data/resale_gradations.json");
        NResaleGradations::TStorage storage(path, /* validate */ true);
        if (!storage.GetErrors().empty()) {
            Cerr << "File resale_gradations.json has errors: " << storage.GetErrors()[0] << Endl;
            UNIT_ASSERT(false);
        }
    }
}
