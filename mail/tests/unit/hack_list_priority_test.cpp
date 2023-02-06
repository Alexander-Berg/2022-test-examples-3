#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <boost/make_shared.hpp>
#include "../../include/furita/common/rule_helper.hpp"

namespace {

using namespace furita;
using namespace testing;

furita::rules::rule_ptr newRule(const std::string& id, const int& prio, const std::string& type) {
    furita::rules::rule_ptr r = boost::make_shared<rules::rule>();
    r->id = std::stoull(id);
    r->prio = prio;
    r->type = type;
    return r;
}

TEST(HackListPriority, all_rule_types) {
    rules::rule_list_ptr rules = boost::make_shared<rules::rule_list>();

    rules->push_back(newRule("1", 1, "user"));
    rules->push_back(newRule("2", 3, "system"));
    rules->push_back(newRule("3", -4, "system"));
    rules->push_back(newRule("5", -2, "user"));
    rules->push_back(newRule("6", 8, "user"));

    furita::rules_helpers::hackListPriority(rules);

    std::list<int> hackedList;

    for (auto r : *rules) {
        hackedList.push_back(r->prio);
    }

    EXPECT_THAT(hackedList, ElementsAre(1, -3, -10, -2, 8));
}

TEST(HackListPriority, only_system) {
    rules::rule_list_ptr rules = boost::make_shared<rules::rule_list>();

    rules->push_back(newRule("1", 3, "system"));
    rules->push_back(newRule("2", -4, "system"));
    rules->push_back(newRule("3", -5, "system"));
    rules->push_back(newRule("4", 1, "system"));

    furita::rules_helpers::hackListPriority(rules);

    std::list<int> hackedList;

    for (auto r : *rules) {
        hackedList.push_back(r->prio);
    }

    EXPECT_THAT(hackedList, ElementsAre(3, -4, -5, 1));
}

TEST(HackListPriority, only_user) {
    rules::rule_list_ptr rules = boost::make_shared<rules::rule_list>();

    rules->push_back(newRule("1", 3, "user"));
    rules->push_back(newRule("2", -4, "user"));
    rules->push_back(newRule("3", -5, "user"));
    rules->push_back(newRule("4", 1, "user"));

    furita::rules_helpers::hackListPriority(rules);

    std::list<int> hackedList;

    for (auto r : *rules) {
        hackedList.push_back(r->prio);
    }

    EXPECT_THAT(hackedList, ElementsAre(3, -4, -5, 1));
}

TEST(HackListPriority, empty_list) {
    rules::rule_list_ptr rules = boost::make_shared<rules::rule_list>();

    furita::rules_helpers::hackListPriority(rules);

    std::list<int> hackedList;

    for (auto r : *rules) {
        hackedList.push_back(r->prio);
    }

    EXPECT_THAT(hackedList, ElementsAre());
}


}
