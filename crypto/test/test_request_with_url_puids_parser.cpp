#include <library/cpp/testing/gtest/gtest.h>

#include <crypta/styx/services/api/lib/logic/common/request_with_puids/request_with_url_puids_parser.h>

#include <library/cpp/cgiparam/cgiparam.h>

#include <util/generic/yexception.h>
#include <util/generic/string.h>

using namespace NCrypta::NStyx::NApi;

TEST(NRequestWithUrlPuidsParser, ExpectThrow) {
    const TString actualQuery = "/unexpected?puid=100500&request_id=an_id&subclient=some_subclient";

    TRequestWithPuids request;
    EXPECT_THROW(NRequestWithUrlPuidsParser::Parse(TCgiParameters(actualQuery), request), yexception);
}
