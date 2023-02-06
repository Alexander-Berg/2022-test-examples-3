#include <extsearch/geo/meta/test_utils/mock_cluster.h>

namespace NGeosearch::NTestUtils {

    using namespace ::testing;

    TMockCluster::TMockCluster() {
        ON_CALL(*this, GetMainSearchPropertyValue(_))
            .WillByDefault(Return("-1"));
    }

    TMockCluster::~TMockCluster() {
    }

} // namespace NGeosearch::NTestUtils
