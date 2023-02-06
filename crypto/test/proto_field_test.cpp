#include <crypta/lib/native/yql/proto_field/proto_field.h>
#include <crypta/lib/python/yql/proto_field/test/proto/bus.pb.h>

#include <library/cpp/testing/gtest/gtest.h>

using namespace NCrypta;

TEST(NYqlProtoField, GetAttrs) {
    const auto& attrs = NYqlProtoField::GetAttrs<TBus>();

    EXPECT_EQ(attrs.size(), 2u);
    EXPECT_TRUE(attrs.contains("_yql_proto_field_Driver"));
    EXPECT_TRUE(attrs.contains("_yql_proto_field_Motor"));
}
