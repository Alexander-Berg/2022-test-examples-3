#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <boost/algorithm/string/join.hpp>
#include <macs/tab_set.h>
#include <macs/tab_factory.h>
#include <macs/tests/mocking-tabs.h>
#include "throw-wmi-helper.h"

namespace {
using namespace ::testing;
using namespace ::macs;
using namespace ::std;

struct TabSetTest : public Test {
    TabsMap tabsData;
    TabSet tabs;
    TabSetTest() {
        fill();
        tabs = TabSet(tabsData);
    }

    void addTab(const Tab::Type& type) {
        Tab tab = TabFactory().type(type).release();
        tabsData.insert(make_pair(tab.type(), tab));
    }

    void fill() {
        addTab(Tab::Type::relevant);
        addTab(Tab::Type::news);
    }
};

TEST_F(TabSetTest, atType_forNonExistingTab_throwsException) {
    ASSERT_THROW_SYS(tabs.at(Tab::Type::social),
                     macs::error::noSuchTab,
                     "access to nonexistent tab 'social': no such tab");
}

TEST_F(TabSetTest, atType_forExistingTab_returnsIt) {
    ASSERT_EQ(tabs.at(Tab::Type::relevant), Tab::Type::relevant);
}

TEST_F(TabSetTest, atStringType_forNonExistingTab_throwsException) {
    ASSERT_THROW_SYS(tabs.at("blah"),
                     macs::error::noSuchTab,
                     "access to nonexistent tab 'blah': no such tab");
}

TEST_F(TabSetTest, atStringType_forExistingTab_returnsIt) {
    ASSERT_EQ(tabs.at("news"), Tab::Type::news);
}

}
