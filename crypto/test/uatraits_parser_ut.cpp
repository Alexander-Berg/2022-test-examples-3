#include <crypta/lib/native/resource_service/parsers/uatraits_parser/uatraits_parser.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NCrypta::NResourceService;

Y_UNIT_TEST_SUITE(TUatraitsParser) {
    Y_UNIT_TEST(MakeDefaultUatraitsDetector) {
        UNIT_ASSERT_NO_EXCEPTION(MakeDefaultUatraitsDetector());
    }
}
