#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/tab/converter.h>
#include <macs/tests/mocking-tabs.h>

namespace {

using namespace testing;
using ::macs::Tab;

class TabsConverterTest : public Test {
protected:
    using Reflection = ::macs::pg::reflection::Tab;

    auto makeRows(std::initializer_list<std::string> types) {
        std::vector<Reflection> rows;
        rows.reserve(types.size());
        boost::transform(types, std::back_inserter(rows), [](auto& t){
            Reflection res;
            res.tab = t;
            return res;
        });
        return rows;
    }
};

TEST_F(TabsConverterTest, makeTabSet_forUnknownTab_removesIt) {
    auto ts = makeRows({"blah"});
    const auto tabs = macs::pg::makeTabSet(std::move(ts), nullptr, "");

    EXPECT_EQ(tabs.size(), 0ul);
}

TEST_F(TabsConverterTest, makeTabSet_forKnownTab_putsItInSet) {
    auto ts = makeRows({"relevant"});
    const auto tabs = macs::pg::makeTabSet(std::move(ts), nullptr, "");

    EXPECT_EQ(tabs.size(), 1ul);
    EXPECT_EQ(tabs.at("relevant"), Tab::Type::relevant);
}

TEST_F(TabsConverterTest, makeTabSet_forKnownAndUnknownTabs_removesUnknownAndputsKnownInSet) {
    auto ts = makeRows({
        "relevant",
        "news",
        "blah",
        "social"
    });
    const auto tabs = macs::pg::makeTabSet(std::move(ts), nullptr, "");

    EXPECT_EQ(tabs.size(), 3ul);
    EXPECT_EQ(tabs.at("relevant"), Tab::Type::relevant);
    EXPECT_EQ(tabs.at("social"), Tab::Type::social);
    EXPECT_EQ(tabs.at("news"), Tab::Type::news);
}

} // namespace
