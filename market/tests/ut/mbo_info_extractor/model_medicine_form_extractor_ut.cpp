#include <market/idx/models/bin/mbo-info-extractor/model_medicine_form_extractor.h>
#include <library/cpp/testing/unittest/registar.h>
#include <util/system/fs.h>


using namespace NMarket::NMboInfoExtractor;

Y_UNIT_TEST_SUITE(TModelMedicineFormExtractorSute) {

    THashMap<ui64, std::tuple<ui64, ui64>> ReadFile(const TString& filePath) {
        TUnbufferedFileInput fileInput(filePath);
        TBufferedZLibDecompress decompressor(&fileInput, ZLib::GZip);

        TString line;
        THashMap<ui64, std::tuple<ui64, ui64>> result;
        while (decompressor.ReadLine(line)) {
            TStringBuf lineBuf = line;
            TStringBuf model_id = lineBuf.NextTok('\t');
            TStringBuf param_id = lineBuf.NextTok('\t');
            TStringBuf option_id = lineBuf.NextTok('\t');
            result[FromString<ui64>(model_id)] = std::make_tuple(FromString<ui64>(param_id), FromString<ui64>(option_id));
        }
        return result;
    }

    Y_UNIT_TEST(TestWithMedicineForm) {
        TModelMedicineFormExtractor extractor(".", 1);
        NMarket::NMbo::TExportReportModel model;
        model.set_id(1);
        auto * p = model.add_parameter_values();
        p->set_xsl_name("Form");
        p->set_param_id(123);
        p->set_option_id(456);

        extractor.Process(model);
        extractor.Flush();

        const auto filePath = "model_medicine_form.gz_1";

        UNIT_ASSERT(NFs::Exists(filePath));

        const auto model2form = ReadFile(filePath);
        UNIT_ASSERT_EQUAL(model2form.size(), 1);
        UNIT_ASSERT_EQUAL(model2form.at(1), std::make_tuple(123, 456));
    }

    Y_UNIT_TEST(TestWithoutMedicineForm) {
        TModelMedicineFormExtractor extractor(".", 1);
        NMarket::NMbo::TExportReportModel model;
        model.set_id(1);

        extractor.Process(model);
        extractor.Flush();

        const auto filePath = "model_medicine_form.gz_1";

        UNIT_ASSERT(NFs::Exists(filePath));

        const auto model2form = ReadFile(filePath);
        UNIT_ASSERT(model2form.empty());
    }

};

