#include <library/cpp/testing/unittest/registar.h>

#include <search/base_search/rs_proxy/test/common/test.h>

Y_UNIT_TEST_SUITE(RemoteStorageProxy) {
    Y_UNIT_TEST(JustWorks) {
        TestRemoteStorageProxy(ETest::Items, TIndexParams{}
            .AddItems(TItemIndexParams{}
                .SetItemType(15)
                .SetGlobalLumps(2)
                .SetChunkLocalLumps(2)
                .SetItemLumps(4)
                .SetNumChunks(4)
                .SetNumItems(1000)
            )
            .AddItems(TItemIndexParams{}
                .SetItemType(0)
                .SetGlobalLumps(0)
                .SetChunkLocalLumps(1)
                .SetItemLumps(1)
                .SetNumChunks(10)
                .SetNumItems(5000)
            )
            .AddItems(TItemIndexParams{}
                .SetItemType(255)
                .SetGlobalLumps(4)
                .SetChunkLocalLumps(4)
                .SetItemLumps(5)
                .SetNumChunks(5)
                .SetNumItems(3000)
            )
            .AddItems(TItemIndexParams{}
                .SetItemType(133)
                .SetGlobalLumps(0)
                .SetChunkLocalLumps(1)
                .SetItemLumps(3)
                .SetNumChunks(2)
                .SetNumItems(5000)
            )
        );
    }

    Y_UNIT_TEST(LightRequest) {
        TestRemoteStorageProxy(ETest::LightRequest | ETest::Items, TIndexParams{}
            .AddItems(TItemIndexParams{}
                .SetItemType(15)
                .SetGlobalLumps(2)
                .SetChunkLocalLumps(2)
                .SetItemLumps(4)
                .SetNumChunks(4)
                .SetNumItems(100)
            )
            .AddItems(TItemIndexParams{}
                .SetItemType(0)
                .SetGlobalLumps(0)
                .SetChunkLocalLumps(1)
                .SetItemLumps(1)
                .SetNumChunks(10)
                .SetNumItems(500)
            )
            .AddItems(TItemIndexParams{}
                .SetItemType(255)
                .SetGlobalLumps(4)
                .SetChunkLocalLumps(4)
                .SetItemLumps(5)
                .SetNumChunks(5)
                .SetNumItems(300)
            )
            .AddItems(TItemIndexParams{}
                .SetItemType(133)
                .SetGlobalLumps(0)
                .SetChunkLocalLumps(1)
                .SetItemLumps(3)
                .SetNumChunks(2)
                .SetNumItems(500)
            )
        );
    }
}
