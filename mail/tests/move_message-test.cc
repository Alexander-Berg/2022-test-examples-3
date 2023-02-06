#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <internal/envelope/move_message.h>

namespace {

using namespace testing;

using Type = macs::Tab::Type;
using OptType = std::optional<Type>;

inline auto makeTabSet() {
    macs::TabsMap tabsMap {
        { Type::relevant, macs::TabFactory().type(Type::relevant).release() },
        { Type::news, macs::TabFactory().type(Type::news).release() }
    };
    return macs::TabSet{std::move(tabsMap)};
}

struct TabResolverTest : public TestWithParam<std::tuple<
        macs::Fid, macs::TabSet, macs::Fid, OptType, OptType>> {
};

INSTANTIATE_TEST_SUITE_P(should_return_correct_tab_type, TabResolverTest, Values(
    std::make_tuple("inbox",    macs::TabSet{}, "inbox",    std::nullopt,               std::nullopt),
    std::make_tuple("inbox",    macs::TabSet{}, "inbox",    OptType{Type::relevant},    std::nullopt),
    std::make_tuple("inbox",    makeTabSet(),   "inbox",    std::nullopt,               OptType{Type::relevant}),
    std::make_tuple("inbox",    makeTabSet(),   "inbox",    OptType{Type::relevant},    OptType{Type::relevant}),
    std::make_tuple("inbox",    makeTabSet(),   "inbox",    OptType{Type::social},      OptType{Type::relevant}),
    std::make_tuple("inbox",    makeTabSet(),   "inbox",    OptType{Type::news},        OptType{Type::news}),

    std::make_tuple("inbox",    macs::TabSet{}, "user",     std::nullopt,               std::nullopt),
    std::make_tuple("inbox",    macs::TabSet{}, "user",     OptType{Type::relevant},    std::nullopt),
    std::make_tuple("inbox",    makeTabSet(),   "user",     std::nullopt,               std::nullopt),
    std::make_tuple("inbox",    makeTabSet(),   "user",     OptType{Type::relevant},    std::nullopt),
    std::make_tuple("inbox",    makeTabSet(),   "user",     OptType{Type::social},      std::nullopt),
    std::make_tuple("inbox",    makeTabSet(),   "user",     OptType{Type::news},        std::nullopt)
));

TEST_P(TabResolverTest, should_return_correct_tab_type) {
    const auto [inboxFid, tabs, destFid, destTab, expected] = GetParam();
    macs::pg::TabResolver resolve(inboxFid, tabs);

    auto resolved = resolve(destFid, destTab);
    ASSERT_EQ(bool(expected), bool(resolved));
    if (expected) {
        ASSERT_EQ(*expected, *resolved);
    }
}

} // namespace
