#include <crypta/cm/services/api/lib/logic/common/ext_id_request/ext_id_request_parser.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(TExtIdRequestParser) {
        using namespace NCrypta::NCm::NApi;

        Y_UNIT_TEST(Parse) {
            const auto& request = NExtIdRequestParser::Parse(TCgiParameters("subclient=crypta&type=tag&value=XXXYYY"));

            UNIT_ASSERT_STRINGS_EQUAL("crypta", request.Subclient);
            UNIT_ASSERT_STRINGS_EQUAL("tag", request.Id.Type);
            UNIT_ASSERT_STRINGS_EQUAL("XXXYYY", request.Id.Value);
        }

        Y_UNIT_TEST(ParseEmptyExtId) {
            const auto& request = NExtIdRequestParser::Parse(TCgiParameters("subclient=crypta&ext_id="));

            UNIT_ASSERT_STRINGS_EQUAL("crypta", request.Subclient);
            UNIT_ASSERT_STRINGS_EQUAL("", request.Id.Type);
            UNIT_ASSERT_STRINGS_EQUAL("", request.Id.Value);
        }
}
