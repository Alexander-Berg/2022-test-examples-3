#include <gtest/gtest.h>

#include "blackwhitelist/list.h"
#include <random>
#include <chrono>
#include <locale>
#include <vector>

using namespace testing;
using namespace NNotSoLiteSrv;
namespace NBW = NBlackWhiteList;

struct TBWListTest: public Test {
    std::vector<std::string> Blacks{"black1@Black_domain", "black2@Black_domain2", "Black_domain"};
    std::vector<std::string> Whites{"white@White_domain", "white2@Black_domain", "белый@Домен", "ТолькоДомен"};

    NBW::TListPtr BWList;

    void SetUp() override {
        BWList = std::make_shared<NBW::TList>();
        for (const auto& black: Blacks) {
            BWList->Insert(NBW::EType::Black, black);
        }
        for (const auto& white: Whites) {
            BWList->Insert(NBW::EType::White, white);
        }
    }
};

TEST_F(TBWListTest, DumpOneList) {
    EXPECT_EQ(BWList->ToString(NBW::EType::Black), "black1@black_domain, black2@black_domain2, black_domain");
    EXPECT_EQ(BWList->ToString(NBW::EType::White), "white2@black_domain, white@white_domain, белый@домен, толькодомен");
}

TEST_F(TBWListTest, DumpLists) {
    EXPECT_EQ(BWList->ToString(),
        "Blacklist: [black1@black_domain, black2@black_domain2, black_domain], "
        "Whitelist: [white2@black_domain, white@white_domain, белый@домен, толькодомен]");
}

TEST_F(TBWListTest, CheckEmailInList) {
    EXPECT_TRUE(BWList->Check(NBW::EType::Black, "black1@black_domain"));
    EXPECT_TRUE(BWList->Check(NBW::EType::Black, "blAck2@black_Domain2"));
    EXPECT_FALSE(BWList->Check(NBW::EType::Black, "white2@black_domain2"));
    EXPECT_TRUE(BWList->Check(NBW::EType::White, "whIte2@black_Domain"));
    EXPECT_TRUE(BWList->Check(NBW::EType::White, "Белый@доМен"));
}

TEST_F(TBWListTest, CheckEmailInListIfDomainInList) {
    EXPECT_TRUE(BWList->Check(NBW::EType::Black, "nonexistent@black_domain"));
    EXPECT_TRUE(BWList->Check(NBW::EType::Black, "nonExistent@black_Domain"));
    EXPECT_FALSE(BWList->Check(NBW::EType::White, "nonExistent@white_Domain"));
    EXPECT_FALSE(BWList->Check(NBW::EType::White, "nonExistent@доМен"));
    EXPECT_TRUE(BWList->Check(NBW::EType::White, "nonExistent@тОлькоДомен"));
}
