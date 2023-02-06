#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <macs/tests/mocking-tabs.h>
#include "throw-wmi-helper.h"

namespace {
    using namespace macs;
    using namespace testing;
    using namespace std;

    TEST_F(TabsRepositoryTest, getAllTabs_callsAsyncGetTabs_onEmtyCache_returnsTabSet) {
        Tab tab = tabs.tab(Tab::Type::relevant);

        EXPECT_CALL(tabs, asyncGetTabs(_)).WillOnce(GiveTabs({tab}));

        TabSet allTabs = tabs.getAllTabs();

        ASSERT_EQ(allTabs.size(), 1ul);
        ASSERT_EQ(allTabs.at(Tab::Type::relevant), tab);
    }

    TEST_F(TabsRepositoryTest, getAllTabs_getsTabsFromCache_onNotEmtyCache) {
        Tab tab = tabs.tab(Tab::Type::relevant);

        EXPECT_CALL(tabs, asyncGetTabs(_)).WillOnce(GiveTabs({tab}));

        tabs.getAllTabs();
        tabs.getAllTabs();
    }

    TEST_F(TabsRepositoryTest, getOrCreateTab_callsAsyncGetOrCreateTab_resetsCache_returnsTab) {
        Tab tab = tabs.tab(Tab::Type::news);

        InSequence s;
        EXPECT_CALL(tabs, asyncGetTabs(_)).WillOnce(GiveTabs({}));
        EXPECT_CALL(tabs, asyncGetOrCreateTab(tab.type(), _))
                    .WillOnce(InvokeArgument<1>(tab));
        EXPECT_CALL(tabs, asyncGetTabs(_)).WillOnce(GiveTabs({}));

        tabs.getAllTabs();
        Tab result = tabs.getOrCreateTab(tab.type());
        tabs.getAllTabs();

        ASSERT_EQ(result, tab);
    }

    TEST_F(TabsRepositoryTest, resetFresh_callsAsyncResetFresh_withFresh_resetsCache_returnsRevision) {
        Tab tab = tabs.tab(Tab::Type::social, 1);
        Revision rev(10);

        InSequence s;
        EXPECT_CALL(tabs, asyncGetTabs(_)).WillOnce(GiveTabs({}));
        EXPECT_CALL(tabs, asyncResetFresh(tab.type(), _))
                    .WillOnce(InvokeArgument<1>(rev));
        EXPECT_CALL(tabs, asyncGetTabs(_)).WillOnce(GiveTabs({}));

        tabs.getAllTabs();
        Revision result = tabs.resetFresh(tab);
        tabs.getAllTabs();

        ASSERT_EQ(result, rev);
    }

    TEST_F(TabsRepositoryTest, resetFresh_withoutFresh_doesNothing_returnsNullRevision) {
        Tab tab = tabs.tab(Tab::Type::social, 0);

        InSequence s;
        EXPECT_CALL(tabs, asyncGetTabs(_)).WillOnce(GiveTabs({}));

        tabs.getAllTabs();
        Revision result = tabs.resetFresh(tab);
        tabs.getAllTabs();

        ASSERT_EQ(result, NULL_REVISION);
    }
}
