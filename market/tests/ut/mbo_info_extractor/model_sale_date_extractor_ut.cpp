#include <market/idx/models/bin/mbo-info-extractor/model_sale_date_extractor.h>
#include <library/cpp/testing/unittest/registar.h>
#include <util/system/fs.h>


using namespace NMarket::NMboInfoExtractor;

Y_UNIT_TEST_SUITE(TSaleDateExtractorSute) {

    THashMap<ui64, ui64> ReadFile(const TString& filePath) {
        TUnbufferedFileInput fileInput(filePath);
        TBufferedZLibDecompress decompressor(&fileInput, ZLib::GZip);

        TString line;
        THashMap<ui64, ui64> model2timestamp;
        while (decompressor.ReadLine(line)) {
            TStringBuf lineBuf = line;
            TStringBuf id = lineBuf.NextTok('\t');
            TStringBuf timestamp = lineBuf.NextTok('\t');
            model2timestamp[FromString<ui64>(id)] = FromString<ui64>(timestamp);
        }
        return model2timestamp;
    }

    Y_UNIT_TEST(TestSaleDate) {
        TModelSaleDateExtractor extractor(".", 1);

        NMarket::NMbo::TExportReportModel model;
        model.set_id(1);
        auto * p = model.add_parameter_values();
        p->set_xsl_name("SaleDate");
        p->add_str_value()->set_value("2020-02-02");

        extractor.Process(model);
        extractor.Flush();

        const auto filePath = "model_sale_dates.gz_1";

        UNIT_ASSERT(NFs::Exists(filePath));

        const auto model2timestamp = ReadFile(filePath);
        UNIT_ASSERT_EQUAL(model2timestamp.size(), 1);
        UNIT_ASSERT_EQUAL(model2timestamp.at(1), 1580601600);
    }

    Y_UNIT_TEST(TestSaleDateGlob) {
        TModelSaleDateExtractor extractor(".", 1);

        NMarket::NMbo::TExportReportModel model;
        model.set_id(1);
        auto * p = model.add_parameter_values();
        p->set_xsl_name("SaleDateGlob");
        p->add_str_value()->set_value("2020-02-02");

        extractor.Process(model);
        extractor.Flush();

        const auto filePath = "model_sale_dates.gz_1";

        UNIT_ASSERT(NFs::Exists(filePath));

        const auto model2timestamp = ReadFile(filePath);
        UNIT_ASSERT_EQUAL(model2timestamp.size(), 1);
        UNIT_ASSERT_EQUAL(model2timestamp.at(1), 1580601600);
    }

    Y_UNIT_TEST(TestInvalidDate) {
        TModelSaleDateExtractor extractor(".", 1);

        NMarket::NMbo::TExportReportModel model;
        model.set_id(1);
        auto * p = model.add_parameter_values();
        p->set_xsl_name("SaleDate");
        p->add_str_value()->set_value("anno domini MCCXXXIIII");

        extractor.Process(model);
        extractor.Flush();

        const auto filePath = "model_sale_dates.gz_1";

        UNIT_ASSERT(NFs::Exists(filePath));

        const auto model2timestamp = ReadFile(filePath);
        UNIT_ASSERT_EQUAL(model2timestamp.size(), 0);

    }

};

