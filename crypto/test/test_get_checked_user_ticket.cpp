#include "constants.h"

#include <crypta/lib/native/tvm/get_checked_user_ticket.h>
#include <crypta/lib/native/tvm/test_helpers.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/tvmauth/ticket_status.h>

using namespace NCrypta::NTvmTest;

TEST(GetCheckedUserTicket, CheckValidTicket) {
    const auto& tvmClient = NCrypta::CreateRecipeTvmClient(SELF_TVM_ID, NCrypta::UNITTEST_TVM_APP_SECRET);
    const auto& checkedUserTicket = NCrypta::GetCheckedUserTicket(tvmClient, NCrypta::GetTestTvmUserHeaders(VALID_USER_TICKET));

    ASSERT_TRUE(checkedUserTicket.Defined());
    ASSERT_TRUE(*checkedUserTicket);
    ASSERT_EQ(DEFAULT_UID, checkedUserTicket->GetDefaultUid());
    ASSERT_EQ(ALL_UIDS, checkedUserTicket->GetUids());
}

TEST(GetCheckedUserTicket, CheckInvalidTicket) {
    const auto& tvmClient = NCrypta::CreateRecipeTvmClient(SELF_TVM_ID, NCrypta::UNITTEST_TVM_APP_SECRET);
    const auto& checkedUserTicket = NCrypta::GetCheckedUserTicket(tvmClient, NCrypta::GetTestTvmUserHeaders(INVALID_USER_TICKET));

    ASSERT_TRUE(checkedUserTicket.Defined());
    ASSERT_FALSE(*checkedUserTicket);
    ASSERT_EQ(NTvmAuth::ETicketStatus::Malformed, checkedUserTicket->GetStatus());
}
