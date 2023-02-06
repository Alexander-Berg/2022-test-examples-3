#include <extsearch/geo/kernel/test_utils/mock_req_results.h>

#include <library/cpp/testing/unittest/registar.h>

namespace NGeosearch::NTestUtils {

    Y_UNIT_TEST_SUITE(TMockReqResults) {

        Y_UNIT_TEST(CompilationTest) {
            // Ensure that all methods are mocked
            TMockReqResults();
        }

    }

} // namespace NGeosearch::NTestUtils
