#include <extsearch/geo/meta/test_utils/mock_req_env.h>

#include <library/cpp/testing/unittest/registar.h>

namespace NGeosearch::NTestUtils {

    Y_UNIT_TEST_SUITE(TMockReqEnvTests) {

        Y_UNIT_TEST(CompilationTest) {
            // Ensure that all methods are mocked
            TMockReqEnv();
        }

    }

} // namespace NGeosearch::NTestUtils
