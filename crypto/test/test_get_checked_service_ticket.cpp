#include "constants.h"

#include <crypta/lib/native/tvm/get_checked_service_ticket.h>
#include <crypta/lib/native/tvm/test_helpers.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/tvmauth/ticket_status.h>

using namespace NCrypta::NTvmTest;

TEST(GetCheckedServiceTicket, CheckValidTicket) {
    const auto& tvmClient = NCrypta::CreateRecipeTvmClient(SELF_TVM_ID, NCrypta::UNITTEST_TVM_APP_SECRET);
    const auto& checkedServiceTicket = NCrypta::GetCheckedServiceTicket(tvmClient, NCrypta::GetTestTvmServiceHeaders(VALID_SERVICE_TICKET));

    ASSERT_TRUE(checkedServiceTicket.Defined());
    ASSERT_TRUE(*checkedServiceTicket);
    ASSERT_EQ(NTvmAuth::ETicketStatus::Ok, checkedServiceTicket->GetStatus());
    ASSERT_EQ(CLIENT_TVM_ID, checkedServiceTicket->GetSrc());
    ASSERT_EQ("ticket_type=serv;expiration_time=9223372036854775807;src=1000502;dst=1000501;", checkedServiceTicket->DebugInfo());
}

TEST(GetCheckedServiceTicket, CheckInvalidTicket) {
    const auto& tvmClient = NCrypta::CreateRecipeTvmClient(SELF_TVM_ID, NCrypta::UNITTEST_TVM_APP_SECRET);
    const auto& checkedServiceTicket = NCrypta::GetCheckedServiceTicket(tvmClient, NCrypta::GetTestTvmServiceHeaders(INVALID_SERVICE_TICKET));

    ASSERT_TRUE(checkedServiceTicket.Defined());
    ASSERT_FALSE(*checkedServiceTicket);
    ASSERT_EQ(NTvmAuth::ETicketStatus::Malformed, checkedServiceTicket->GetStatus());
}
