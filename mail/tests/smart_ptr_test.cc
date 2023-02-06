#include <boost/property_tree/json_parser.hpp>
#include <boost/fusion/adapted.hpp>

#include <gtest/gtest.h>

#include <yamail/data/serialization/json_writer.h>
#include <yamail/data/deserialization/foreign_ptree.h>
#include <yamail/data/deserialization/ptree_reader.h>

using namespace yamail::data::deserialization;

typedef std::unique_ptr<std::string> StringAutoPtr;
typedef boost::shared_ptr<int> IntSharedPtr;
typedef boost::scoped_ptr<float> FloatScopedPtr;

struct EStruct {
    std::string title;
    StringAutoPtr stringPtr;
    IntSharedPtr intPtr;
    FloatScopedPtr floatPtr;
};

BOOST_FUSION_ADAPT_STRUCT(EStruct,
    (std::string, title)
    (StringAutoPtr, stringPtr)
    (IntSharedPtr, intPtr)
    (FloatScopedPtr, floatPtr)
)

static_assert(yamail::data::reflection::is_fusion_struct_v<EStruct>);

template <typename T>
class SmartPtrTest : public ::testing::Test {
};

using Types = ::testing::Types<property_tree::Reader, foreign_ptree::Reader>;
TYPED_TEST_SUITE(SmartPtrTest, Types);

TEST(SmartPtrTest, serializeNullPtrs_outputNothing) {
    EStruct eObj;
    eObj.title = "EStruct";
    const auto r = yamail::data::serialization::toJson(eObj);
    const std::string expectedJson = "{\"title\":\"EStruct\"}";
    ASSERT_EQ(expectedJson, r.str());
}

TEST(SmartPtrTest, serializeNonNullPtrs_outputValues) {
    EStruct eObj;
    eObj.title = "EStruct";
    eObj.stringPtr.reset(new std::string("value"));
    eObj.intPtr.reset(new int(42));
    eObj.floatPtr.reset(new float(3.5));
    const auto r = yamail::data::serialization::toJson(eObj);
    const std::string expectedJson = "{\"title\":\"EStruct\",\"stringPtr\":\"value\",\"intPtr\":42,\"floatPtr\":3.5}";
    ASSERT_EQ(expectedJson, r.str());
}

TYPED_TEST(SmartPtrTest, deserializeNothingToSmartPtr_resetPtrToNull) {
    const std::string json = "{\"title\":\"EStruct\"}";
    std::istringstream jsonStream(json);
    boost::property_tree::ptree tree;
    boost::property_tree::json_parser::read_json(jsonStream, tree);

    EStruct actual;
    TypeParam reader(tree);
    reader.apply(actual);
    ASSERT_FALSE(actual.stringPtr.get());
    ASSERT_FALSE(actual.intPtr.get());
}

TYPED_TEST(SmartPtrTest, deserializeValueToSmartPtr_resetPtr) {
    const std::string json = "{\"title\":\"EStruct\",\"stringPtr\":\"value\",\"intPtr\":42,\"floatPtr\":3.5}";
    std::istringstream jsonStream(json);
    boost::property_tree::ptree tree;
    boost::property_tree::json_parser::read_json(jsonStream, tree);

    EStruct actual;
    TypeParam reader(tree);
    reader.apply(actual);
    ASSERT_TRUE(actual.stringPtr.get());
    ASSERT_EQ("value", *actual.stringPtr);
    ASSERT_TRUE(actual.intPtr.get());
    ASSERT_EQ(42, *actual.intPtr);
    ASSERT_TRUE(actual.floatPtr.get());
    ASSERT_EQ(3.5, *actual.floatPtr);
}

