#include <library/cpp/testing/unittest/registar.h>

#include <search/base_search/rs_proxy/test/common/test.h>

Y_UNIT_TEST_SUITE(RemoteStorageProxySmall) {
    Y_UNIT_TEST(JustWorks) {
        TestRemoteStorageProxy(ETest::LightRequest | ETest::Items, TIndexParams{}
            .AddItems(TItemIndexParams{}
                .SetItemType(15)
                .SetGlobalLumps(2)
                .SetChunkLocalLumps(2)
                .SetItemLumps(4)
                .SetNumChunks(2)
                .SetNumItems(100)
            )
            .AddItems(TItemIndexParams{}
                .SetItemType(0)
                .SetGlobalLumps(0)
                .SetChunkLocalLumps(1)
                .SetItemLumps(1)
                .SetNumChunks(3)
                .SetNumItems(500)
            )
        );
    }
}
