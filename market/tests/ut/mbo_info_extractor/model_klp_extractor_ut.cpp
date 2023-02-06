#include <market/idx/models/bin/mbo-info-extractor/model_klp_extractor.h>
#include <library/cpp/testing/unittest/registar.h>
#include <util/system/fs.h>

using namespace NMarket::NMboInfoExtractor;

Y_UNIT_TEST_SUITE(TKlpExtractorSuite) {

    constexpr auto Id = 1;

    constexpr auto KlpId = 24277150;
    constexpr auto NotKlpId = 666;
    constexpr auto KlpXslName = "EsklpCode";

    THashMap<uint64_t, TString> ReadFile(const TString& filePath) {
        TUnbufferedFileInput fileInput(filePath);
        TBufferedZLibDecompress decompressor(&fileInput, ZLib::GZip);

        TString line;
        THashMap<uint64_t, TString> klpCodes;
        while (decompressor.ReadLine(line)) {
            TStringBuf lineBuf = line;
            TStringBuf modelIdColumn = lineBuf.NextTok('\t');
            TStringBuf klpColumn = lineBuf.NextTok('\t');

            uint64_t id = 0;
            // TString value = 0;
            if (TryFromString(modelIdColumn, id)) {
                klpCodes[id] = klpColumn;
            }
        }
        return klpCodes;
    }

    Y_UNIT_TEST(TestKlp) {
        TModelKlpExtractor extractor(".", 1);

        NMarket::NMbo::TExportReportModel model;
        model.set_id(Id);

        TString targetKlpCode = "21.20.10.221-000010-1-00174-2000000735386";
        auto *p = model.add_parameter_values();
        p->set_param_id(KlpId);
        p->set_xsl_name(KlpXslName);
        p->add_str_value()->set_value(targetKlpCode);

        extractor.Process(model);
        extractor.Flush();

        const auto filePath = "model_klp.gz_1";

        UNIT_ASSERT(NFs::Exists(filePath));

        const auto outputParams = ReadFile(filePath);

        UNIT_ASSERT_EQUAL(outputParams.size(), 1);
        UNIT_ASSERT_EQUAL(outputParams.at(Id), targetKlpCode);
    }

    Y_UNIT_TEST(TestWithoutKlpCode) {
        TModelKlpExtractor extractor(".", 1);

        NMarket::NMbo::TExportReportModel model;
        model.set_id(Id);

        TString targetKlpCode = "21.20.10.221-000010-1-00174-2000000735386";
        auto *p = model.add_parameter_values();
        p->set_param_id(NotKlpId);
        p->set_xsl_name(KlpXslName);
        p->add_str_value()->set_value(targetKlpCode);

        p = model.add_parameter_values();
        p->set_param_id(KlpId);
        p->set_xsl_name(KlpXslName);

        extractor.Process(model);
        extractor.Flush();

        const auto filePath = "model_klp.gz_1";

        UNIT_ASSERT(NFs::Exists(filePath));

        const auto outputParams = ReadFile(filePath);
        UNIT_ASSERT_EQUAL(outputParams.size(), 0);
    }
};