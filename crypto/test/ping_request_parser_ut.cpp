#include <crypta/cm/services/api/lib/logic/ping/request/ping_request_parser.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(TPingRequestParser) {
    using namespace NCrypta::NCm::NApi;

    Y_UNIT_TEST(Parse) {
        const auto& request = NPingRequestParser::Parse("/ping?subclient=slb");

        UNIT_ASSERT_STRINGS_EQUAL("slb", request.Subclient);
    }

    Y_UNIT_TEST(ParseCgi) {
        const auto& request = NPingRequestParser::Parse(TCgiParameters("subclient=slb"));

        UNIT_ASSERT_STRINGS_EQUAL("slb", request.Subclient);
    }

    Y_UNIT_TEST(ParseExtraArgs) {
        const auto& request = NPingRequestParser::Parse("/ping?subclient=slb&extra_arg=extra_value");

        UNIT_ASSERT_STRINGS_EQUAL("slb", request.Subclient);
    }

    Y_UNIT_TEST(BadPath) {
        UNIT_ASSERT_EXCEPTION_CONTAINS(NPingRequestParser::Parse("/identify?subclient=crypta&type=tag&value=XXXYYY"), yexception, "Not a /ping request: /identify");
    }
}
