#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <yplatform/tskv/tskv.h>

namespace {

using namespace testing;

namespace tskv = yplatform::tskv;

TEST(TskvTest, stream_withBypassManipulator_returnsDirectValues) {
    std::ostringstream s;
    s << tskv::bypass << tskv::attr("key1", "value\ttab\rreturn\nnewline\\") << tskv::attr("key2", 2);
    EXPECT_EQ(s.str(), "tskv\tkey1=value\ttab\rreturn\nnewline\\\tkey2=2");
}

TEST(TskvTest, stream_withBypassManipulatorAndStdPair_returnsDirectValues) {
    std::ostringstream s;
    s << tskv::bypass << std::make_pair("key1", "value");
    EXPECT_EQ(s.str(), "tskv\tkey1=value");
}

TEST(TskvTest, stream_withBypassManipulatorAndWithoutEndl_returnsLineWithNoEndl) {
    std::ostringstream s;
    s << tskv::bypass(tskv::without_endl) << std::make_pair("key1", "value");
    EXPECT_EQ(s.str(), "tskv\tkey1=value");
}

TEST(TskvTest, stream_withBypassManipulatorAndWithEndl_returnsLineWithEndl) {
    std::ostringstream s;
    s << tskv::bypass(tskv::with_endl) << std::make_pair("key1", "value");
    EXPECT_EQ(s.str(), "tskv\tkey1=value\n");
}

TEST(TskvTest, stream_withBypassManipulatorAndNewConverter_returnsConvertedValues) {
    std::ostringstream s;
    s << tskv::bypass(tskv::detail::utf_converter{}) << std::make_pair("key1", "value");
    EXPECT_EQ(s.str(), "tskv\tkey1=value");
}

TEST(TskvTest, stream_withBypassManipulatorAndNewKeyValueConverters_returnsConvertedValues) {
    std::ostringstream s;
    s << tskv::bypass(tskv::detail::bypass_converter{}, tskv::detail::utf_converter{})
        << std::make_pair("key1", "value");
    EXPECT_EQ(s.str(), "tskv\tkey1=value");
}

TEST(TskvTest, stream_withBypassManipulatorAndStdMap_returnsDirectValues) {
    std::ostringstream s;
    s << tskv::bypass << std::map<std::string, int>{{"key1", 1}, {"key2", 2}};
    EXPECT_EQ(s.str(), "tskv\tkey1=1\tkey2=2");
}

TEST(TskvTest, stream_withBypassManipulatorAndAttributesMap_returnsDirectValues) {
    std::ostringstream s;
    tskv::attributes_map am;
    am << tskv::attr("key1", "1") << tskv::attr("key2", 2);
    s << tskv::bypass << am;
    EXPECT_EQ(s.str(), "tskv\tkey1=1\tkey2=2");
}

TEST(TskvTest, stream_withBypassManipulatorAndStdVectorOfPair_returnsDirectValues) {
    std::ostringstream s;
    s << tskv::bypass << std::vector<std::pair<std::string, int>>{{"key1", 1}, {"key2", 2}};
    EXPECT_EQ(s.str(), "tskv\tkey1=1\tkey2=2");
}

TEST(TskvTest, stream_withBypassManipulatorAndStdListOfPair_returnsDirectValues) {
    std::ostringstream s;
    s << tskv::bypass << std::list<std::pair<std::string, int>>{{"key1", 1}, {"key2", 2}};
    EXPECT_EQ(s.str(), "tskv\tkey1=1\tkey2=2");
}


TEST(TskvTest, stream_withBypassManipulatorAndStdVectorOfAttributes_returnsDirectValues) {
    std::ostringstream s;
    s << tskv::bypass
      << std::vector<tskv::attribute<std::string, int>> {
                tskv::attr("key1", 1), tskv::attr("key2", 2)};
    EXPECT_EQ(s.str(), "tskv\tkey1=1\tkey2=2");
}

TEST(TskvTest, stream_withBypassManipulatorAndStdListOfAttributes_returnsDirectValues) {
    std::ostringstream s;
    s << tskv::bypass
      << std::list<tskv::attribute<std::string, int>> {
        tskv::attr("key1", 1), tskv::attr("key2", 2)};
    EXPECT_EQ(s.str(), "tskv\tkey1=1\tkey2=2");
}

TEST(TskvTest, stream_withBypassManipulatorAndStdTuple_returnsDirectValues) {
    std::ostringstream s;
    s << tskv::bypass << std::make_tuple(tskv::attr("key1", 1), tskv::attr("key2", "2"));
    EXPECT_EQ(s.str(), "tskv\tkey1=1\tkey2=2");
}

TEST(TskvTest, stream_withBypassManipulatorAndMultiLineCode_returnsDirectValues) {
    std::ostringstream s;
    {
        auto log = tskv::bypass(s);
        log << tskv::attr("key1", "value");
        log << tskv::attr("key2", 2);
    }
    EXPECT_EQ(s.str(), "tskv\tkey1=value\tkey2=2");
}

TEST(TskvTest, stream_withBypassManipulatorAndMultiLineCodeAndEndl_returnsDirectValues) {
    std::ostringstream s;
    auto log = tskv::bypass(s);
    log << tskv::attr("key1", "value");
    log << tskv::attr("key2", 2);
    log << tskv::endl;
    EXPECT_EQ(s.str(), "tskv\tkey1=value\tkey2=2");
}

TEST(TskvTest, stream_withBypassManipulatorWithEndlAndMultiLineCodeAndEndl_returnsLineWithEol) {
    std::ostringstream s;
    auto log = tskv::bypass(tskv::with_endl)(s);
    log << tskv::attr("key", "value");
    log << tskv::endl;
    EXPECT_EQ(s.str(), "tskv\tkey=value\n");
}

TEST(TskvTest, stream_withBasicManipulator_returnsValuesWithSkippedTabReturnAndNewline) {
    std::ostringstream s;
    s << tskv::basic << tskv::attr("key1", "value\ttab\rreturn\nnewline\\") << tskv::attr("key2", 2);
    EXPECT_EQ(s.str(), "tskv\tkey1=valuetabreturnnewline\\\tkey2=2");
}

TEST(TskvTest, stream_withUtfManipulator_returnsValuesWithEscapedTabReturnNewlineAndBackslash) {
    std::ostringstream s;
    s.imbue(std::locale("en_US.UTF-8"));
    s << tskv::utf << tskv::attr("key1", "value\ttab\rreturn\nnewline\\") << tskv::attr("key2", "測試");
    EXPECT_EQ(s.str(), "tskv\tkey1=value\\ttab\\rreturn\\nnewline\\\\\tkey2=測試");
}


TEST(TskvTest, stream_withBasicManipulator_returnsDurationsWithUnits) {
    std::ostringstream s;
    s << tskv::basic << tskv::attr("nsec", std::chrono::nanoseconds(123))
                     << tskv::attr("usec", std::chrono::microseconds(42))
                     << tskv::attr("msec", std::chrono::milliseconds(666))
                     << tskv::attr("sec", std::chrono::seconds(420))
                     << tskv::attr("min", std::chrono::minutes(13))
                     << tskv::attr("hrs", std::chrono::hours(2));
    EXPECT_EQ(s.str(), "tskv\tnsec=123ns"
                       "\tusec=42us"
                       "\tmsec=666ms"
                       "\tsec=420s"
                       "\tmin=13m"
                       "\thrs=2h");
}

}
