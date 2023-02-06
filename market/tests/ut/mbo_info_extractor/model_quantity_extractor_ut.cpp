#include <market/idx/models/bin/mbo-info-extractor/model_quantity_extractor.h>
#include <library/cpp/testing/unittest/registar.h>
#include <util/system/fs.h>


using namespace NMarket::NMboInfoExtractor;

Y_UNIT_TEST_SUITE(TQuantityExtractorSute) {

    THashMap<ui64, THashMap<TString, ui64>> ReadFile(const TString& filePath) {
        TUnbufferedFileInput fileInput(filePath);
        TBufferedZLibDecompress decompressor(&fileInput, ZLib::GZip);

        TString line;
        THashMap<ui64, THashMap<TString, ui64>> model2timestamp;
        while (decompressor.ReadLine(line)) {
            TStringBuf lineBuf = line;
            TStringBuf id = lineBuf.NextTok('\t');
            TStringBuf paramName = lineBuf.NextTok('\t');
            TStringBuf quantity = lineBuf.NextTok('\t');
            model2timestamp[FromString<ui64>(id)][TString(paramName)] = FromString<ui64>(quantity);
        }
        return model2timestamp;
    }

    Y_UNIT_TEST(TestSimple) {
        const static TVector<TString> quantityParams {{"volume_n", "weight_n", "number_new"}};
        for (const auto& paramName : quantityParams) {
            TModelQuantityExtractor extractor(".", 1);
            NMarket::NMbo::TExportReportModel model;
            model.set_id(1);
            auto * p = model.add_parameter_values();
            p->set_xsl_name(paramName);
            p->set_numeric_value("123456");

            extractor.Process(model);
            extractor.Flush();

            const auto filePath = "model_quantities.gz_1";

            UNIT_ASSERT(NFs::Exists(filePath));

            const auto model2timestamp = ReadFile(filePath);
            UNIT_ASSERT_EQUAL(model2timestamp.size(), 1);
            UNIT_ASSERT_EQUAL(model2timestamp.at(1).size(), 1);
            UNIT_ASSERT_EQUAL(model2timestamp.at(1).at(paramName), 123456);
        }
    }

    Y_UNIT_TEST(TestInvalidQuantity) {
        TModelQuantityExtractor extractor(".", 1);

        NMarket::NMbo::TExportReportModel model;
        model.set_id(1);
        auto * p = model.add_parameter_values();
        p->set_xsl_name("volume_n");
        p->set_numeric_value("anno domini MCCXXXIIII");

        extractor.Process(model);
        extractor.Flush();

        const auto filePath = "model_quantities.gz_1";

        UNIT_ASSERT(NFs::Exists(filePath));

        const auto model2timestamp = ReadFile(filePath);
        UNIT_ASSERT_EQUAL(model2timestamp.size(), 0);
    }

};

