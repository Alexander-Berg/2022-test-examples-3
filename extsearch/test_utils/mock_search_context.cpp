#include <extsearch/geo/meta/test_utils/mock_search_context.h>

namespace NGeosearch::NTestUtils {

    using namespace ::testing;

    TMockSearchContext::TMockSearchContext() {
        ON_CALL(*this, Cluster()).WillByDefault(Return(&MockCluster));
        ON_CALL(*this, ReqEnv()).WillByDefault(Return(&MockReqEnv));
    }

    TMockSearchContext::~TMockSearchContext() {
    }

} // namespace NGeosearch::NTestUtils
