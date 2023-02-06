#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <logdog/logger.h>


namespace {

using namespace ::testing;

LOGDOG_DEFINE_LEVEL(testLevel)

struct LoggerMock {
    using level_type = std::decay_t<decltype(testLevel)>;
    template <typename ... Data>
    void write (level_type, Data&& ... v) const { test(v...); }

    MOCK_METHOD(bool, applicable, (level_type), (const));
    MOCK_METHOD(void, test, (const std::string&), (const));
};

struct CallableMock {
    MOCK_METHOD(void, call, (), ());
};

struct LoggerTest : public Test {
    StrictMock<LoggerMock> mock;
};

TEST_F(LoggerTest, operatorParenthesis_withApplicableTrue_callsTestWithArguments) {
    EXPECT_CALL(mock, applicable(_)).WillOnce(Return(true));
    EXPECT_CALL(mock, test("message")).WillOnce(Return());
    testLevel(mock,[&](auto write) { write("message"); });
}

TEST_F(LoggerTest, operatorParenthesis_withApplicableFalse_doesNotCallTest) {
    EXPECT_CALL(mock, applicable(_)).WillOnce(Return(false));
    testLevel(mock,[&](auto write) { write("message"); });
}

TEST_F(LoggerTest, operatorParenthesis_withApplicableFalse_doesNotCallParamsFunctor) {
    StrictMock<CallableMock> callable;
    EXPECT_CALL(mock, applicable(_)).WillOnce(Return(false));
    testLevel(mock,[&](auto write) { callable.call(); write(""); });
}

TEST_F(LoggerTest, applicable_withLogger_returnsApplicableResult) {
    EXPECT_CALL(mock, applicable(_)).WillOnce(Return(true));
    ASSERT_EQ(::logdog::applicable(mock, testLevel), true);
    EXPECT_CALL(mock, applicable(_)).WillOnce(Return(false));
    ASSERT_EQ(::logdog::applicable(mock, testLevel), false);
}

TEST_F(LoggerTest, applicable_withNone_returnsFalse) {
    ASSERT_EQ(::logdog::applicable(::logdog::none, testLevel), false);
}

TEST_F(LoggerTest, applicable_withNullptr_returnsFalse) {
    ASSERT_EQ(::logdog::applicable(nullptr, testLevel), false);
}

TEST_F(LoggerTest, applicable_withValidLoggerPointer_returnsApplicableResult) {
    auto ptr = std::addressof(mock);
    EXPECT_CALL(mock, applicable(_)).WillOnce(Return(true));
    ASSERT_EQ(::logdog::applicable(ptr, testLevel), true);
    EXPECT_CALL(mock, applicable(_)).WillOnce(Return(false));
    ASSERT_EQ(::logdog::applicable(ptr, testLevel), false);
}

TEST_F(LoggerTest, applicable_withNullLoggerPointer_returnsFalse) {
    decltype(std::addressof(mock)) ptr = nullptr;
    ASSERT_EQ(::logdog::applicable(ptr, testLevel), false);
}

struct base_test_struct { int dummy; };
struct derived_test_struct : base_test_struct {};

#ifdef __clang__
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-const-variable"
#endif

LOGDOG_DEFINE_ATTRIBUTE(std::string, test_value_attr);
LOGDOG_DEFINE_ATTRIBUTE(std::string&, test_reference_attr);
LOGDOG_DEFINE_ATTRIBUTE(base_test_struct&, test_base_reference_attr);

#ifdef __clang__
#pragma clang diagnostic pop
#endif

TEST_F(LoggerTest, find_attribute_withAttributeInSequense_returnsIndexOfAttr) {
    auto s = std::make_tuple(test_value_attr="test");
    const auto i = ::logdog::find_attribute(s, test_value_attr);
    EXPECT_TRUE(i!=::logdog::hana::size(s));
    EXPECT_EQ(value(::logdog::hana::at(s, i)), "test");
}

TEST_F(LoggerTest, find_attribute_withNoAttributeInSequense_returnsEndIterator) {
    auto s = std::make_tuple(test_value_attr="test");
    const auto i = ::logdog::find_attribute(s, test_reference_attr);
    EXPECT_TRUE(i==::logdog::hana::size(s));
}

TEST_F(LoggerTest, find_attribute_withOptionalAttributeInSequence_returnsIteratorOnAttr) {
    auto s = std::make_tuple(test_value_attr=::logdog::none);
    EXPECT_TRUE(::logdog::find_attribute(s, test_value_attr)!=::logdog::hana::size(s));
}

TEST_F(LoggerTest, get_attribute_withAttributeInSequense_returnsInitializedStaticOptional) {
    auto s = std::make_tuple(test_value_attr="test");
    decltype(auto) found = ::logdog::get_attribute(s, test_value_attr);
    EXPECT_TRUE(found);
    EXPECT_EQ(::logdog::value(*found), "test");
}

TEST_F(LoggerTest, get_attribute_withNoAttributeInSequense_returnsUninitializedStaticOptional) {
    auto s = std::make_tuple(test_value_attr="test");
    decltype(auto) found = ::logdog::get_attribute(s, test_reference_attr);
    EXPECT_TRUE(!found);
    EXPECT_TRUE(decltype(found)::is_none::value);
}

TEST_F(LoggerTest, get_attribute_withUninitializedOptionalAttributeInSequence_returnsUninitializedOptional) {
    auto s = std::make_tuple(test_value_attr=::logdog::none);
    decltype(auto) found = ::logdog::get_attribute(s, test_value_attr);
    EXPECT_TRUE(!found);
}

TEST_F(LoggerTest, get_attribute_withInitializedOptionalAttributeInSequence_returnsInitializedOptional) {
    auto s = std::make_tuple(::boost::make_optional(test_value_attr="test"));
    decltype(auto) found = ::logdog::get_attribute(s, test_value_attr);
    EXPECT_TRUE(found);
    EXPECT_EQ(::logdog::value(*found), "test");
}

TEST_F(LoggerTest, update_attribute_withOptionalValueAttributeInSequence_updatesAttribute) {
    auto s = std::make_tuple(test_value_attr=::logdog::none);
    ::logdog::update_attribute(s, test_value_attr, "test");
    EXPECT_EQ(::logdog::value(*::logdog::get_attribute(s, test_value_attr)), "test");
}

TEST_F(LoggerTest, update_attribute_withOptionalReferenceAttributeInSequence_updatesAttribute) {
    auto s = std::make_tuple(test_reference_attr=::logdog::none);
    const std::string test_value = "test";
    ::logdog::update_attribute(s, test_reference_attr, test_value);
    EXPECT_EQ(std::addressof(::logdog::value(
        *::logdog::get_attribute(s, test_reference_attr))),
        std::addressof(test_value));
}

TEST_F(LoggerTest, update_attribute_withValueAttributeInSequence_updatesAttribute) {
    auto s = std::make_tuple(test_value_attr="zzz");
    ::logdog::update_attribute(s, test_value_attr, "test");
    EXPECT_EQ(::logdog::value(*::logdog::get_attribute(s, test_value_attr)), "test");
}

TEST_F(LoggerTest, update_attribute_withReferenceAttributeInSequence_updatesAttribute) {
    const std::string dummy = "dummy";
    auto s = std::make_tuple(test_reference_attr=dummy);
    const std::string test_value = "test";
    ::logdog::update_attribute(s, test_reference_attr, test_value);
    EXPECT_EQ(std::addressof(::logdog::value(
        *::logdog::get_attribute(s, test_reference_attr))),
        std::addressof(test_value));
}

TEST_F(LoggerTest, valueAttribute_withStdRef_holdsReference) {
    const std::string test_value = "test";
    auto attr = test_value_attr=std::ref(test_value);
    EXPECT_EQ(std::addressof(value(attr)), std::addressof(test_value));
}

TEST_F(LoggerTest, attribute_withReferenceBaseAttributeAndDerived_returnsReferenceOnDerived) {
    derived_test_struct test_value{};
    auto attr = test_base_reference_attr=test_value;
    EXPECT_EQ(std::addressof(value(attr)), std::addressof(test_value));
}

TEST_F(LoggerTest, get_attribute_withReferenceBaseAttributeAndDerivedViaStdRef_returnsReferenceOnDerived) {
    derived_test_struct test_value{};
    auto attr = test_base_reference_attr=std::ref(test_value);
    EXPECT_EQ(std::addressof(value(attr)), std::addressof(test_value));
}

TEST_F(LoggerTest, message_withTupleWithMessage_returnsMessage) {
    using system_error = boost::system::system_error;
    system_error e{
        boost::system::error_code{
            boost::system::errc::bad_address,
            boost::system::system_category()
        }, "what"};
    const auto args = std::make_tuple(
        ::logdog::exception=e,
        int(1),
        ::logdog::message=std::string("msg"));
    EXPECT_EQ(::logdog::message(args), "msg");
}

}
