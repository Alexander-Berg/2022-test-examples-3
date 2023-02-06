#include <src/logic/message_part_real/ua.hpp>

#include <mail/retriever/tests/unit/gtest.h>
#include <mail/retriever/tests/unit/gmock.h>

namespace {

using namespace retriever;
using namespace testing;

struct UATest : Test {};

TEST(UATest, getMobility_notMobileNotTouchNotTablet_returnsDesktop) {
    std::map<std::string, std::string> details;
    details["isMobile"] = "false";
    details["isTouch"] = "false";
    details["isTablet"] = "false";
    ASSERT_EQ(UA::desktop, UA::getMobility(details));
}

TEST(UATest, getMobility_notMobileIsTouchNotTablet_returnsDesktop) {
    std::map<std::string, std::string> details;
    details["isMobile"] = "false";
    details["isTouch"] = "true";
    details["isTablet"] = "false";
    ASSERT_EQ(UA::desktop, UA::getMobility(details));
}

TEST(UATest, getMobility_isMobileNotTouchNotTablet_returnsPhone) {
    std::map<std::string, std::string> details;
    details["isMobile"] = "true";
    details["isTouch"] = "false";
    details["isTablet"] = "false";
    ASSERT_EQ(UA::phone, UA::getMobility(details));
}

TEST(UATest, getMobility_isMobileIsTouchNotTablet_returnsSmartphone) {
    std::map<std::string, std::string> details;
    details["isMobile"] = "true";
    details["isTouch"] = "true";
    details["isTablet"] = "false";
    ASSERT_EQ(UA::smartphone, UA::getMobility(details));
}

TEST(UATest, getMobility_isMobileIsTouchIsTablet_returnsTablet) {
    std::map<std::string, std::string> details;
    details["isMobile"] = "true";
    details["isTouch"] = "true";
    details["isTablet"] = "true";
    ASSERT_EQ(UA::tablet, UA::getMobility(details));
}

} // namespace
