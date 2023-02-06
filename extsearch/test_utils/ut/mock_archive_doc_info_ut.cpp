#include <extsearch/geo/meta/test_utils/mock_archive_doc_info.h>

#include <library/cpp/testing/unittest/registar.h>

namespace NGeosearch::NTestUtils {

    Y_UNIT_TEST_SUITE(TMockArchiveDocInfoTests) {

        Y_UNIT_TEST(CompilationTest) {
            // Ensure that all methods are mocked
            TMockArchiveDocInfo();
        }

    }

} // namespace NGeosearch::NTestUtils
