#include <library/cpp/logger/global/global.h>
#include <library/cpp/testing/unittest/registar.h>
#include <market/robotics/cv/library/cpp/label_decoder/label_decoder.h>

using namespace NWarehouseSDK;

class TWarehouseCodeSetTest: public TTestBase {
    UNIT_TEST_SUITE(TWarehouseCodeSetTest);

    UNIT_TEST(CheckPLTCodeTypesTest);

    UNIT_TEST_SUITE_END();

public:
    void CheckPLTCodeTypesTest() {
        auto codeset = GetPLTCodeSet();
        UNIT_ASSERT(codeset.IsIncluded(NBarCode::QRCode));
        UNIT_ASSERT(codeset.IsIncluded(NBarCode::CodeX_CODE39));
    }
};

UNIT_TEST_SUITE_REGISTRATION(TWarehouseCodeSetTest);
