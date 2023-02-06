#include <extsearch/geo/meta/test_utils/mock_search_context.h>

#include <library/cpp/testing/unittest/registar.h>

namespace NGeosearch::NTestUtils {

    Y_UNIT_TEST_SUITE(TMockSearchContextTests) {

        Y_UNIT_TEST(CompilationTest) {
            // Ensure that all methods are mocked
            TMockSearchContext();
        }

    }

} // namespace NGeosearch::NTestUtils
