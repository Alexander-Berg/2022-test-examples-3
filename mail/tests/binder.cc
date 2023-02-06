#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <logdog/logger.h>

namespace logdog {

template <typename K, typename V>
static bool operator == (const attribute<K, V>& lhs, const attribute<K, V>& rhs) {
    return value(lhs) == value(rhs);
}

} // namespace logdog

namespace {

using namespace ::testing;

LOGDOG_DEFINE_LEVEL(test_level)

struct LoggerMock {
    using level_type = std::decay_t<decltype(test_level)>;
    MOCK_METHOD(void, write, (level_type, decltype(logdog::message=""), decltype(logdog::unixtime=0)), (const));
    MOCK_METHOD(void, write, (level_type, decltype(logdog::message=""), decltype(logdog::unixtime=0), decltype(logdog::where_name="")), (const));

    static constexpr bool applicable(level_type) {return true;}
};

TEST(context_binder, should_call_underlying_logger_with_given_additional_context) {
    LoggerMock logger;
    auto binder = logdog::bind(std::cref(logger), logdog::message="message", logdog::unixtime=0);
    EXPECT_CALL(logger, write(_, logdog::message="message", logdog::unixtime=0));
    test_level(binder, [](auto writer){ writer();});
}

TEST(context_binder, should_call_underlying_logger_with_given_additional_context_and_arguments) {
    LoggerMock logger;
    auto binder = logdog::bind(std::cref(logger), logdog::message="message", logdog::unixtime=0);
    EXPECT_CALL(logger, write(_, logdog::message="message", logdog::unixtime=0, logdog::where_name="here"));
    test_level(binder, [](auto writer){ writer(logdog::where_name="here");});
}

TEST(context_binder, should_call_underlying_logger_with_given_and_rebinded_additional_context) {
    LoggerMock logger;
    auto binder = logdog::bind(std::cref(logger), logdog::message="message", logdog::unixtime=0);
    auto rebinded = logdog::bind(binder, logdog::where_name="here");
    EXPECT_CALL(logger, write(_, logdog::message="message", logdog::unixtime=0, logdog::where_name="here"));
    test_level(rebinded, [](auto writer){ writer();});
}

}
