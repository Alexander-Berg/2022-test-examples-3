#include "bb_upload_state_serializer.h"

#include <library/cpp/testing/unittest/registar.h>

#include <library/cpp/yson/node/node_io.h>

using namespace NCrypta::NDmp;

Y_UNIT_TEST_SUITE(BbUploadStateSerializer) {
    Y_UNIT_TEST(Basic) {
        const TBindings bindings("xxx", 1500000000, {1, 4, 7});
        const TBbUploadState bbUploadState(bindings, 1500000500);
        const auto node = NYT::TNode()("yandexuid", "xxx")
                                      ("timestamp", 1500000000u)
                                      ("segments", NYT::TNode().Add(1u).Add(4u).Add(7u))
                                      ("upload_timestamp", 1500000500u);
        UNIT_ASSERT_EQUAL(NBbUploadStateSerializer::Serialize(bbUploadState), node);
        UNIT_ASSERT_EQUAL(NBbUploadStateSerializer::Deserialize(node), bbUploadState);
    }
}
