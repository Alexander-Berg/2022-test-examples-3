#include <stdexcept>
#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <internal/daria_view_maker.h>

namespace msg_body {

using namespace testing;

TEST(DariaViewMakerTest, extractNoReplyMessageId_correctMessageIdHeader_extractMessageId) {
    ASSERT_EQ("2370000000444786003", extractNoReplyMessageId("<2370000000444786003.remind_noanswer@mail.yandex.ru>"));
}

TEST(DariaViewMakerTest, extractNoReplyMessageId_anotherDomain_extractMessageId) {
    ASSERT_EQ("2370000000444786003", extractNoReplyMessageId("<2370000000444786003.remind_noanswer@yandex.net>"));
}

TEST(DariaViewMakerTest, extractNoReplyMessageId_incorrectRemind_emptyResult) {
    ASSERT_EQ("", extractNoReplyMessageId("<2370000000444786003.renind@mail.yandex.ru>"));
}

TEST(DariaViewMakerTest, extractNoReplyMessageId_emptyHeader_emptyResult) {
    ASSERT_EQ("", extractNoReplyMessageId(""));
}

TEST(DariaViewMakerTest, extractNoReplyMessageId_remindMsg_emptyResult) {
    ASSERT_EQ("", extractNoReplyMessageId("<2370000000444786003.remind_msg@mail.yandex.ru>"));
}

TEST(DariaViewMakerTest, extractNoReplyMessageId_oldMessageIdHeader_extractMessageId) {
    ASSERT_EQ("2370000000444786003", extractNoReplyMessageId("<2370000000444786003.remind@mail.yandex.ru>"));
}

}
