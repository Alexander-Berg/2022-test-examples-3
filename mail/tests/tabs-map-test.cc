#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <macs/label_factory.h>
#include <macs/tabs_map.h>

namespace macs {
bool operator==(const Label& lhs, const Label& rhs) {
    return lhs.lid() == rhs.lid() &&
           lhs.name() == rhs.name() &&
           lhs.type() == rhs.type();
}

static std::ostream& operator<<(std::ostream& ostr, const Label& label) {
    ostr << label.name();
    return ostr;
}

namespace deprecated {

bool operator==(const Tab& lhs, const Tab& rhs) {
    return lhs.type == rhs.type &&
           lhs.label == rhs.label &&
           lhs.excludeLabels == rhs.excludeLabels;
}

static std::ostream& operator<<(std::ostream& ostr, const Tab& tab) {
    ostr << int(tab.type) << " " << tab.label << " " << tab.excludeLabels.size();
    return ostr;
}

} // namespace deprecated
} // namespace macs

namespace {

using namespace ::testing;
using namespace ::macs;
using deprecated::TabsMap;
using deprecated::Tab;

struct TabsMapTest : public Test {
    TabsMap tabsMap;

    TabsMapTest() : tabsMap(makeTabsMap()) {}

    static TabsMap makeTabsMap() {
        boost::property_tree::ptree node, tab;

        tab.put("name", "some");
        tab.put("label_type", "42");
        node.push_back(std::make_pair("tabs_mapping", tab));

        tab.clear();
        tab.put("name", "other");
        tab.put("label_type", "13");

        node.push_back(std::make_pair("tabs_mapping", tab));
        return deprecated::initTabsMap(node, "tabs_mapping");
    }

    inline Label label(macs::Lid lid, std::string soType) {
        return LabelFactory()
                .lid(lid)
                .name(soType)
                .type(Label::Type::spamDefense)
                .product();
    }

    inline void makeSetImpl(LabelSet&) {}

    template <typename ... Labels>
    inline void makeSetImpl(LabelSet& set, Label l, Labels&& ...ls) {
        const auto lid = l.lid();
        set[lid] = std::move(l);
        makeSetImpl(set, std::forward<Labels>(ls)...);
    }

    template <typename ... Labels>
    inline LabelSet makeSet(Labels&& ...ls) {
        LabelSet retval;
        makeSetImpl(retval, std::forward<Labels>(ls)...);
        return retval;
    }
};


TEST_F(TabsMapTest, getTab_withUnknownTab_returnsUnknownTab) {
    Tab expected;
    expected.type = Tab::Type::unknown;

    Tab res = tabsMap.getTab({}, "unknown");
    ASSERT_EQ(res, expected);
}

TEST_F(TabsMapTest, getTab_withNonexistingLabel_returnsEmptyTab) {
    Tab expected;
    expected.type = Tab::Type::empty;

    Tab res = tabsMap.getTab({}, "some");
    ASSERT_EQ(res, expected);
}

TEST_F(TabsMapTest, getTab_withExistingLabel_returnsTabWithLabel) {
    LabelSet set = makeSet(
        label("lid", "42")
    );
    Tab expected;
    expected.type = Tab::Type::withLabel;
    expected.label = set["lid"];

    Tab res = tabsMap.getTab(set, "some");
    ASSERT_EQ(res, expected);
}

TEST_F(TabsMapTest, getTab_withDefaultTabAndNonexistingLabel_returnsWholeTab) {
    Tab expected;
    expected.type = Tab::Type::whole;

    Tab res = tabsMap.getTab({}, TabsMap::defaultTab());
    ASSERT_EQ(res, expected);
}

TEST_F(TabsMapTest, getTab_withDefaultTabAndExistingLabel_returnsTabWithoutLabels) {
    LabelSet set = makeSet(
        label("lid", "42")
    );
    Tab expected;
    expected.type = Tab::Type::withoutLabels;
    expected.excludeLabels.push_back(set["lid"]);

    Tab res = tabsMap.getTab(set, TabsMap::defaultTab());
    ASSERT_EQ(res, expected);
}

TEST_F(TabsMapTest, getTab_withRelevantTab_returnsSameAsDefault) {
    LabelSet set = makeSet(
        label("lid", "42")
    );

    Tab expected = tabsMap.getTab(set, TabsMap::defaultTab());
    Tab res = tabsMap.getTab(set, "relevant");
    ASSERT_EQ(res, expected);
}


TEST_F(TabsMapTest, getAllTabs_withNonexistingLabel_returnsTwoEmptyAndWholeTabs) {
    Tab empty;
    empty.type = Tab::Type::empty;

    Tab whole;
    whole.type = Tab::Type::whole;

    auto res = tabsMap.getAllTabs({});
    ASSERT_THAT(res, AllOf(
        Contains(Pair("some", empty)),
        Contains(Pair("other", empty)),
        Contains(Pair(TabsMap::defaultTab(), whole))
    ));
}

TEST_F(TabsMapTest, getAllTabs_withOneExistingLabel_returnsEmptyWithAndWithoutLabelsTabs) {
    LabelSet set = makeSet(
        label("lid", "42")
    );

    Tab empty;
    empty.type = Tab::Type::empty;

    Tab with;
    with.type = Tab::Type::withLabel;
    with.label = set["lid"];

    Tab without;
    without.type = Tab::Type::withoutLabels;
    without.excludeLabels.push_back(set["lid"]);

    auto res = tabsMap.getAllTabs(set);
    ASSERT_THAT(res, AllOf(
        Contains(Pair("some", with)),
        Contains(Pair("other", empty)),
        Contains(Pair(TabsMap::defaultTab(), without))
    ));
}

TEST_F(TabsMapTest, getAllTabs_withAllExistingLabel_returnsTwoWithAndOneWithoutLabelsTabs) {
    LabelSet set = makeSet(
        label("lid1", "42"),
        label("lid2", "13")
    );

    Tab with1;
    with1.type = Tab::Type::withLabel;
    with1.label = set["lid1"];

    Tab with2;
    with2.type = Tab::Type::withLabel;
    with2.label = set["lid2"];

    Tab without;
    without.type = Tab::Type::withoutLabels;
    without.excludeLabels.push_back(set["lid1"]);
    without.excludeLabels.push_back(set["lid2"]);

    auto res = tabsMap.getAllTabs(set);
    ASSERT_THAT(res, AllOf(
        Contains(Pair("some", with1)),
        Contains(Pair("other", with2)),
        Contains(Pair(TabsMap::defaultTab(), without))
    ));
}

} //anonimous namespace

