#include <market/idx/models/bin/mbo-info-extractor/sku_sample_extractor.h>
#include <library/cpp/testing/unittest/registar.h>
#include <util/system/fs.h>


using namespace NMarket::NMboInfoExtractor;

Y_UNIT_TEST_SUITE(TSkuSampleExtractorSuite) {

    constexpr auto Id = 1;
    constexpr auto SampleXslName = "sample";

    THashSet<ui64> ReadFile(const TString& filePath) {
        TUnbufferedFileInput fileInput(filePath);
        TBufferedZLibDecompress decompressor(&fileInput, ZLib::GZip);

        TString line;
        THashSet<ui64> sampleSkus;
        while (decompressor.ReadLine(line)) {
            ui64 id = 0;
            if (TryFromString(line, id)) {
                sampleSkus.insert(id);
            }
        }
        return sampleSkus;
    }

    Y_UNIT_TEST(TestIsSample) {
        TSkuSampleExtractor extractor(".", 1);

        NMarket::NMbo::TExportReportModel sku;
        sku.set_current_type("SKU");
        sku.set_id(Id);

        auto *p = sku.add_parameter_values();
        p->set_xsl_name(SampleXslName);
        p->set_bool_value(true);

        extractor.Process(sku);
        extractor.Flush();

        const auto filePath = "sku_sample.gz_1";

        UNIT_ASSERT(NFs::Exists(filePath));

        const auto sampleSkus = ReadFile(filePath);

        UNIT_ASSERT_EQUAL(sampleSkus.size(), 1);
        UNIT_ASSERT_EQUAL(sampleSkus.contains(Id), true);
    }

    Y_UNIT_TEST(TestIsNotSample) {
        TSkuSampleExtractor extractor(".", 1);

        NMarket::NMbo::TExportReportModel sku;
        sku.set_current_type("SKU");
        sku.set_id(Id);

        auto *p = sku.add_parameter_values();
        p->set_xsl_name(SampleXslName);
        p->set_bool_value(false);

        extractor.Process(sku);
        extractor.Flush();

        const auto filePath = "sku_sample.gz_1";

        UNIT_ASSERT(NFs::Exists(filePath));

        const auto sampleSkus = ReadFile(filePath);
        UNIT_ASSERT_EQUAL(sampleSkus.size(), 0);
    }

    Y_UNIT_TEST(TestIsNotSku) {
        TSkuSampleExtractor extractor(".", 1);

        NMarket::NMbo::TExportReportModel sku;
        sku.set_current_type("GURU");
        sku.set_id(Id);

        auto *p = sku.add_parameter_values();
        p->set_xsl_name(SampleXslName);
        p->set_bool_value(true);

        extractor.Process(sku);
        extractor.Flush();

        const auto filePath = "sku_sample.gz_1";

        UNIT_ASSERT(NFs::Exists(filePath));

        const auto sampleSkus = ReadFile(filePath);

        UNIT_ASSERT_EQUAL(sampleSkus.size(), 0);
    }

    Y_UNIT_TEST(TestWithoutSample) {
        TSkuSampleExtractor extractor(".", 1);

        NMarket::NMbo::TExportReportModel sku;
        sku.set_current_type("SKU");
        sku.set_id(Id);

        auto * p = sku.add_parameter_values();
        p->set_xsl_name("nonsample");
        p->set_bool_value(true);

        extractor.Process(sku);
        extractor.Flush();

        const auto filePath = "sku_sample.gz_1";

        UNIT_ASSERT(NFs::Exists(filePath));

        const auto sampleSkus = ReadFile(filePath);
        UNIT_ASSERT_EQUAL(sampleSkus.size(), 0);
    }
};
