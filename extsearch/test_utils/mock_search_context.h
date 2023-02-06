#pragma once

#include <extsearch/geo/meta/test_utils/mock_cluster.h>
#include <extsearch/geo/meta/test_utils/mock_req_env.h>

#include <kernel/search_daemon_iface/cntintrf.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace NGeosearch::NTestUtils {

    class TMockSearchContext: public ISearchContext {
    public:
        TMockSearchContext();
        virtual ~TMockSearchContext();

        MOCK_METHOD(const IIndexProperty*, IndexProperty, (), (override));
        MOCK_METHOD(IReqParams*, ReqParams, (), (override));
        MOCK_METHOD(IReqResults*, ReqResults, (), (override));
        MOCK_METHOD(ICluster*, Cluster, (), (override));
        MOCK_METHOD(IReqEnv*, ReqEnv, (), (override));

        ::testing::NiceMock<TMockCluster> MockCluster;
        ::testing::NiceMock<TMockReqEnv> MockReqEnv;
    };

} // namespace NGeosearch::NTestUtils
