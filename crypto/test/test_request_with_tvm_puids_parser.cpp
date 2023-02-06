#include <crypta/lib/native/tvm/test_helpers.h>
#include <crypta/styx/services/api/lib/logic/common/request_with_puids/request_with_puids.h>
#include <crypta/styx/services/api/lib/logic/common/request_with_puids/request_with_tvm_puids_parser.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/tvmauth/checked_user_ticket.h>

#include <util/generic/string.h>
#include <util/generic/vector.h>
#include <util/stream/str.h>
#include <util/system/types.h>

using namespace NCrypta::NStyx::NApi;

namespace {
    const TString USER_TICKET = "3:user:CAwQ__________9_GhkKAgh7CgMIyAMKAwiVBhCVBiDShdjMBCgC:PcLpaBsnuIVQL9PcYC529hV2S47BOUUv6cBkGNZYgwp9XOB8KEfNZZDwaX4DkVS0mDwyYidu6kpZNTrmdUfI4hmWfwkWmCt_oYxaRBMyAwG31pngQnp9si73LqZ_62siH4KQIYGeT12aHmABKPoYmkEjdsVjeK8EGqe7tZTo1Yk";
    const ui64 REF_DEFAULT_PUID = 789;
    const ui64 SELF_TVM_ID = 1000501;
    const TString SUBCLIENT = "unittest";
    const TString REQUEST_ID = "86da91117e3ff8c16ec8546daa5323260a";

    TString GetUrl() {
        TStringStream ss;
        ss << "/1/takeout/status/?subclient=" << SUBCLIENT << "&request_id=" << REQUEST_ID;
        return ss.Str();
    }

    TMaybe<NTvmAuth::TCheckedUserTicket> GetUserTicket() {
        const auto& tvmClient = NCrypta::CreateRecipeTvmClient(SELF_TVM_ID, NCrypta::UNITTEST_TVM_APP_SECRET);

        return tvmClient.CheckUserTicket(USER_TICKET);
    }
}

TEST(NStatusRequestParser, ParseString) {
    const auto& userTicket = GetUserTicket();

    ASSERT_TRUE(userTicket.Defined());
    ASSERT_EQ(NTvmAuth::ETicketStatus::Ok, userTicket->GetStatus());
    ASSERT_TRUE(*userTicket);

    TRequestWithPuids request;
    NRequestWithTvmPuidsParser::Parse(GetUrl(), userTicket, request);

    ASSERT_EQ("unittest", request.Subclient);
    ASSERT_EQ(REQUEST_ID, request.RequestId);
    ASSERT_EQ(REF_DEFAULT_PUID, request.DefaultPuid);
}
