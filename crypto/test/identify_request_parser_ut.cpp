#include <crypta/cm/services/api/lib/logic/identify/request/identify_request_parser.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(TIdentifyRequestParser) {
        using namespace NCrypta::NCm::NApi;

        Y_UNIT_TEST(Parse) {
            const auto& request = NIdentifyRequestParser::Parse("/identify?subclient=crypta&type=tag&value=XXXYYY");

            UNIT_ASSERT_STRINGS_EQUAL("crypta", request.Subclient);
            UNIT_ASSERT_STRINGS_EQUAL("tag", request.Id.Type);
            UNIT_ASSERT_STRINGS_EQUAL("XXXYYY", request.Id.Value);
        }

        Y_UNIT_TEST(ParseCgi) {
            const auto& request = NIdentifyRequestParser::Parse(TCgiParameters("subclient=crypta&type=tag&value=XXXYYY"));

            UNIT_ASSERT_STRINGS_EQUAL("crypta", request.Subclient);
            UNIT_ASSERT_STRINGS_EQUAL("tag", request.Id.Type);
            UNIT_ASSERT_STRINGS_EQUAL("XXXYYY", request.Id.Value);
        }

        Y_UNIT_TEST(ParseEmptyExtId) {
            const auto& request = NIdentifyRequestParser::Parse("/identify?subclient=crypta&ext_id=");

            UNIT_ASSERT_STRINGS_EQUAL("crypta", request.Subclient);
            UNIT_ASSERT_STRINGS_EQUAL("", request.Id.Type);
            UNIT_ASSERT_STRINGS_EQUAL("", request.Id.Value);
        }

        Y_UNIT_TEST(ParseExtraArgs) {
            const auto& request = NIdentifyRequestParser::Parse("/identify?subclient=phantom2d&request_tag=Aa_-0&type=tag&value=XXXYYY");

            UNIT_ASSERT_STRINGS_EQUAL("phantom2d", request.Subclient);
            UNIT_ASSERT_STRINGS_EQUAL("tag", request.Id.Type);
            UNIT_ASSERT_STRINGS_EQUAL("XXXYYY", request.Id.Value);
        }

        Y_UNIT_TEST(BadPath) {
            UNIT_ASSERT_EXCEPTION_CONTAINS(NIdentifyRequestParser::Parse("/upload?subclient=crypta&type=tag&value=XXXYYY"), yexception, "Not an /identify request: /upload");
        }
}
