#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/logger/global/global.h>
#include <market/robotics/cv/library/cpp/label_decoder/label_decode_result.h>

using namespace NWarehouseSDK;

class TDecodeResultTest: public TTestBase {
    UNIT_TEST_SUITE(TDecodeResultTest);

    UNIT_TEST(EmptyConstructorTest);
    UNIT_TEST(ConstructorWithParametersTest);
    UNIT_TEST(AttributesConstructorTest);
    UNIT_TEST(CorrectTest);
    UNIT_TEST(CorrectPalletPlaceTest);
    UNIT_TEST(MoveTest);

    UNIT_TEST_SUITE_END();

public:
    void EmptyConstructorTest() {
        TLabelDecodeResult result;
        UNIT_ASSERT(result.GetText() == "");
        UNIT_ASSERT(result.GetFormat() == TLabelFormat::Undefined);
        UNIT_ASSERT(!result.IsQR());
        UNIT_ASSERT(!result.IsCorrect());
    }

    void ConstructorWithParametersTest() {
        TLabelDecodeResult result("PLT000123", NBarCode::CodeX_CODE39);
        UNIT_ASSERT(result.GetText() == "PLT000123");
        UNIT_ASSERT(result.GetFormat() == TLabelFormat::PLT);
        UNIT_ASSERT(!result.IsQR());
        UNIT_ASSERT(result.IsCorrect());
    }

    void AttributesConstructorTest() {
        NBarCode::TAttributes attributes("LOC000123", NBarCode::QRCode, 100);
        TLabelDecodeResult result(attributes);
        UNIT_ASSERT(result.GetText() == "LOC000123");
        UNIT_ASSERT(result.GetFormat() == TLabelFormat::LOC);
        UNIT_ASSERT(result.IsQR());
        UNIT_ASSERT(result.IsCorrect());
    }

    void CorrectTest() {
        TLabelDecodeResult result1("123456789", NBarCode::CodeX_CODE39);
        UNIT_ASSERT(!result1.IsCorrect());
        TLabelDecodeResult result2("PLT123456789", NBarCode::None);
        UNIT_ASSERT(!result2.IsCorrect());
    }

    void CorrectPalletPlaceTest() {
        TLabelDecodeResult result1("K19-01A6", NBarCode::QRCode);
        UNIT_ASSERT(result1.IsCorrect());

        TLabelDecodeResult result2("K19-01A7", NBarCode::QRCode);
        UNIT_ASSERT(!result2.IsCorrect());
    }

    void MoveTest() {
        TLabelDecodeResult result1("PLT000123", NBarCode::QRCode);
        TLabelDecodeResult result2;
        result2 = std::move(result1);
        UNIT_ASSERT(result2.GetText() == "PLT000123");
        UNIT_ASSERT(result2.IsCorrect());
        UNIT_ASSERT(result2.GetFormat() == TLabelFormat::PLT);
        UNIT_ASSERT(result2.IsQR());

        UNIT_ASSERT(result1.GetText() == "");
        UNIT_ASSERT(!result1.IsCorrect());
        UNIT_ASSERT(result1.GetFormat() == TLabelFormat::Undefined);
    }
};

UNIT_TEST_SUITE_REGISTRATION(TDecodeResultTest);
