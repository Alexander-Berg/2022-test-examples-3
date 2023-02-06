#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <yamail/data/reflection.h>
#include <yamail/data/serialization/yajl.h>
#include <yamail/data/deserialization/yajl.h>

using namespace testing;
using namespace yamail::data::serialization;
using namespace yamail::data::deserialization;

class TestData {
    std::string str_;
    int value_;

    YREFLECTION_GRANT_ACCESS

    TestData() = default;
public:
    TestData(std::string str, const int value)
        : str_(std::move(str)),
          value_(value)
    {}

    const std::string& str() const noexcept {
        return str_;
    }

    int value() const noexcept {
        return value_;
    }

    bool operator== (const TestData& that) const noexcept {
        return str_ == that.str_ && value_ == that.value_;
    }
};

using OptTestData = boost::optional<TestData>;
using TestDataPtr = std::unique_ptr<TestData>;

BOOST_FUSION_ADAPT_STRUCT(TestData,
    (std::string, str_)
    (int, value_)
)

static_assert(yamail::data::reflection::is_fusion_struct_v<TestData>);

TEST(NonDefaultConstructibleTest, testObjectIsSameAfterSerializationDeserialization) {
    TestData data("str", 100);
    const auto result = fromJson<TestData>(toJson(data));
    EXPECT_EQ(data, result);
}

struct TestDataWrapper {
    OptTestData testData;
    TestDataPtr testDataPtr;

    bool operator== (const TestDataWrapper& that) const {
        const bool isBothNull = !testDataPtr && !that.testDataPtr;
        const bool isBothNotNull = testDataPtr && that.testDataPtr;
        const bool isPtrsEquals = isBothNotNull ? *testDataPtr == *that.testDataPtr
                                                : isBothNull;

        return (testData == that.testData) && isPtrsEquals;
    }
};

BOOST_FUSION_ADAPT_STRUCT(TestDataWrapper,
    (OptTestData, testData)
    (TestDataPtr, testDataPtr)
)

TEST(NonDefaultConstructibleTest, testSerializationOfNonDefaultConstructibleMember) {
    TestDataWrapper data{
        TestData("str", 100),
        std::make_unique<TestData>("another str", 42)
    };
    const auto result = fromJson<TestDataWrapper>(toJson(data));
    EXPECT_EQ(data, result);
}
