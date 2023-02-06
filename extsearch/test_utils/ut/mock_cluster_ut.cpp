#include <extsearch/geo/meta/test_utils/mock_cluster.h>

#include <library/cpp/testing/unittest/registar.h>

namespace NGeosearch::NTestUtils {

    Y_UNIT_TEST_SUITE(TMockClusterTests) {

        Y_UNIT_TEST(CompilationTest) {
            // Ensure that all methods are mocked
            TMockCluster();
        }

    }

} // namespace NGeosearch::NTestUtils
