#include <market/robotics/cv/library/cpp/label_decoder/label_decoder.h>
#include <market/robotics/cv/library/cpp/types/image.h>
#include <market/robotics/cv/library/cpp/util/test_utils.h>
#include <library/cpp/logger/global/global.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NWarehouseSDK;

static const THashMap<TString, TString> pltQRFiles{{GetPathToFile("199_0.jpg"), "PLT10398"},
                                                   {GetPathToFile("200_0.jpg"), "PLT10398"},
                                                   {GetPathToFile("211_0.jpg"), "PLT10398"},
                                                   {GetPathToFile("212_0.jpg"), "PLT10398"},
                                                   {GetPathToFile("68_0.jpg"), "PLT10398"},
                                                   {GetPathToFile("73_0.jpg"), "PLT10398"},
                                                   {GetPathToFile("242_0.jpg"), "PLT10395"},
                                                   {GetPathToFile("128_0.jpg"), "PLT10397"}};

static const THashMap<TString, TLabelFormat> severalCodesFiles{{GetPathToFile("PLTmoreLOC.jpg"), TLabelFormat::PLT},
                                                               {GetPathToFile("LOCmorePLT.jpg"), TLabelFormat::LOC}};

class TLabelDecoderTest: public TTestBase {
    UNIT_TEST_SUITE(TLabelDecoderTest);

    UNIT_TEST(CreationTest);
    UNIT_TEST(DecodingRGBTest);
    UNIT_TEST(CodeSizeTest);

    UNIT_TEST_SUITE_END();

public:
    void CreationTest() {
        auto labelDecoder = TLabelDecoder::Create(TLabelDecoder::TReaderType::ZBar,
                                                  std::move(TCodeSet(TCodeType::QRCode)));
        UNIT_ASSERT(labelDecoder->GetCodeSet().IsIncluded(TCodeType::QRCode));
        UNIT_ASSERT(labelDecoder->GetType() == TLabelDecoder::TReaderType::ZBar);
    }

    void DecodingRGBTest() {
        auto labelDecoder = TLabelDecoder::Create(TLabelDecoder::TReaderType::Universal,
                                                  std::move(TCodeSet(TCodeType::QRCode)));
        for (const auto& [filename, expected] : pltQRFiles) {
            TImage image;
            bool readResult = image.LoadImage(filename);
            UNIT_ASSERT(readResult);
            TLabelDecodeResult result;
            bool isOk = labelDecoder->Decode(image, result);
            UNIT_ASSERT(isOk == true);
            UNIT_ASSERT(result.GetText() == expected);
            UNIT_ASSERT(result.IsQR());
            UNIT_ASSERT(result.IsCorrect());
            UNIT_ASSERT(result.GetFormat() == TLabelFormat::PLT);
        }
    }

    void CodeSizeTest() {
        auto labelDecoder = TLabelDecoder::Create(TLabelDecoder::TReaderType::Universal,
                                                  std::move(TCodeSet(TCodeType::QRCode)));
        for (const auto& [filename, expected] : severalCodesFiles) {
            TImage image;
            bool readResult = image.LoadImage(filename);
            UNIT_ASSERT(readResult);
            TLabelDecodeResult result;
            bool isOk = labelDecoder->Decode(image, result);
            UNIT_ASSERT(isOk == true);
            UNIT_ASSERT(result.IsQR());
            UNIT_ASSERT(result.IsCorrect());
            UNIT_ASSERT(result.GetFormat() == expected);
        }
    }
};

UNIT_TEST_SUITE_REGISTRATION(TLabelDecoderTest);
