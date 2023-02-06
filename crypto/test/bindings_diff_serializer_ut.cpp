#include <crypta/dmp/adobe/bin/common/bindings_diff_serializer.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NCrypta::NDmp::NAdobe;

Y_UNIT_TEST_SUITE(BindingsSerializer) {
    Y_UNIT_TEST(Full) {
        const TBindingsDiff bindings("xxx", 1500000000, {1, 2, 3}, {4, 5});
        const auto node = NYT::TNode()("id", "xxx")
                                      ("timestamp", 1500000000u)
                                      ("added_segments", NYT::TNode().Add(1u).Add(2u).Add(3u))
                                      ("removed_segments", NYT::TNode().Add(4u).Add(5u));
        UNIT_ASSERT_EQUAL(NBindingsDiffSerializer::Serialize(bindings), node);
        UNIT_ASSERT_EQUAL(NBindingsDiffSerializer::Deserialize(node), bindings);
    }

    Y_UNIT_TEST(AddedOnly) {
        const TBindingsDiff bindings("xxx", 1500000000, {1, 2, 3}, {});
        const auto node = NYT::TNode()("id", "xxx")
                                      ("timestamp", 1500000000u)
                                      ("added_segments", NYT::TNode().Add(1u).Add(2u).Add(3u));
        UNIT_ASSERT_EQUAL(NBindingsDiffSerializer::Serialize(bindings), node);
        UNIT_ASSERT_EQUAL(NBindingsDiffSerializer::Deserialize(node), bindings);
    }

    Y_UNIT_TEST(RemovedOnly) {
        const TBindingsDiff bindings("xxx", 1500000000, {}, {4, 5});
        const auto node = NYT::TNode()("id", "xxx")
                                      ("timestamp", 1500000000u)
                                      ("removed_segments", NYT::TNode().Add(4u).Add(5u));
        UNIT_ASSERT_EQUAL(NBindingsDiffSerializer::Serialize(bindings), node);
        UNIT_ASSERT_EQUAL(NBindingsDiffSerializer::Deserialize(node), bindings);
    }
}
