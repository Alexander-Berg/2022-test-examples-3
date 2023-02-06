#include "bindings_serializer.h"

#include <library/cpp/testing/unittest/registar.h>

using namespace NCrypta::NDmp;

Y_UNIT_TEST_SUITE(BindingsSerializer) {
    Y_UNIT_TEST(ExtId) {
        const TBindings bindings("xxx", 1500000000, {1, 4, 7});
        const auto node = NYT::TNode()("ext_id", "xxx")
                                      ("timestamp", 1500000000u)
                                      ("segments", NYT::TNode().Add(1u).Add(4u).Add(7u));
        UNIT_ASSERT_EQUAL(NExtIdBindingsSerializer::Serialize(bindings), node);
        UNIT_ASSERT_EQUAL(NExtIdBindingsSerializer::Deserialize(node), bindings);
    }

    Y_UNIT_TEST(Id) {
        const TBindings bindings("xxx", 1500000000, {1, 4, 7});
        const auto node = NYT::TNode()("id", "xxx")
                                      ("timestamp", 1500000000u)
                                      ("segments", NYT::TNode().Add(1u).Add(4u).Add(7u));
        UNIT_ASSERT_EQUAL(NIdBindingsSerializer::Serialize(bindings), node);
        UNIT_ASSERT_EQUAL(NIdBindingsSerializer::Deserialize(node), bindings);
    }

    Y_UNIT_TEST(Yandexuid) {
        const TBindings bindings("xxx", 1500000000, {1, 4, 7});
        const auto node = NYT::TNode()("yandexuid", "xxx")
                                      ("timestamp", 1500000000u)
                                      ("segments", NYT::TNode().Add(1u).Add(4u).Add(7u));
        UNIT_ASSERT_EQUAL(NYandexuidBindingsSerializer::Serialize(bindings), node);
        UNIT_ASSERT_EQUAL(NYandexuidBindingsSerializer::Deserialize(node), bindings);
    }
}
