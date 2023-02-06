#include <market/library/ssku_reader/ssku_reader.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include <util/stream/file.h>

Y_UNIT_TEST_SUITE(TestInstallmentSku) {
    Y_UNIT_TEST(TestAllFiles) {
        const TVector<TString> files = {"installments_ssku.csv", "installments_ssku_testing.csv"};
        for (const auto& file: files) {
            const auto path = JoinFsPaths(ArcadiaSourceRoot(), "market/svn-data/package-data", file);
            const TSskuReader reader(path, /* validate = */ true);
            const auto& errors = reader.GetDataErrors();
            if (!errors.empty()) {
                Cerr << "File " << file << " " << errors[0] << Endl;
            }
            UNIT_ASSERT(errors.empty());
        }
    }
}
