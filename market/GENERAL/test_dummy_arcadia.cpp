#include <devtools/dummy_arcadia/models/macros/generated_srcs/lib.h>

#include <library/cpp/testing/unittest/gtest.h>

TEST(DummyArcadia, Serialization) {
    ASSERT_EQ("{\n  \"a\":12340987\n}", SerializeAToJsonString(A{12340987}));
}

TEST(DummyArcadia, Deserialization) {
    B b;
    DeserializeBFromJsonString("{\n  \"b\":1230987\n}", b);
    ASSERT_EQ(1230987, b.b);
}

