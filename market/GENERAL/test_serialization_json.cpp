#include <market/tools/code_generator/ut/generated/serialization_json.h>

#include <library/cpp/testing/unittest/gtest.h>

TEST(SerializationJson, SerializeTSet_Empty) {
     ASSERT_EQ("[\n]", SerializeSetOfInts(TInts{}));
}

TEST(SerializationJson, SerializeTSet_SomeInts) {
     ASSERT_EQ("[\n  0,\n  1,\n  1000000,\n  -1999\n]", SerializeSetOfInts(TInts{1000000, 1, -1999, 0}));
}

TEST(SerializationJson, DeserializeTSet_Empty) {
     ASSERT_EQ((TInts{}), DeserializeSetOfInts("[\n]"));
}

TEST(SerializationJson, DeserializeTSet_SomeInts) {
     ASSERT_EQ((TInts{1000000, 1, -1999, 0}), DeserializeSetOfInts("[\n  0,\n  1,\n  1000000,\n  -1999\n]"));
}
