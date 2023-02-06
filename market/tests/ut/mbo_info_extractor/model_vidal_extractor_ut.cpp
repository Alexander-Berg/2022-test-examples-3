#include <market/idx/models/bin/mbo-info-extractor/model_vidal_extractor.h>
#include <library/cpp/testing/unittest/registar.h>
#include <util/system/fs.h>


using namespace NMarket::NMboInfoExtractor;

Y_UNIT_TEST_SUITE(TVidalExtractorSute) {

    constexpr auto Id = 1;
    constexpr auto AtcCode = 23181290;
    constexpr auto XslName = "ATCCode";
    constexpr auto AtcCodeValue = "J05AX13";

    THashMap<ui64, TString> ReadFile(const TString& filePath) {
        TUnbufferedFileInput fileInput(filePath);
        TBufferedZLibDecompress decompressor(&fileInput, ZLib::GZip);

        TString line;
        THashMap<ui64, TString> vidalParams;
        while (decompressor.ReadLine(line)) {
            TStringBuf lineBuf = line;
            TStringBuf id = lineBuf.NextTok('\t');
            TStringBuf atcCode = lineBuf.NextTok('\t');
            vidalParams[FromString<ui64>(id)] = TString(atcCode);
        }
        return vidalParams;
    }

    Y_UNIT_TEST(TestVidalAtc) {
        TModelVidalExtractor extractor(".", 1);

        NMarket::NMbo::TExportReportModel model;
        model.set_id(Id);

        auto *p = model.add_parameter_values();
        p->set_param_id(AtcCode);
        p->set_xsl_name(XslName);
        p->add_str_value()->set_value(AtcCodeValue);

        extractor.Process(model);
        extractor.Flush();

        const auto filePath = "model_vidal.gz_1";

        UNIT_ASSERT(NFs::Exists(filePath));

        const auto vidalParams = ReadFile(filePath);

        UNIT_ASSERT_EQUAL(vidalParams.size(), 1);
        UNIT_ASSERT_EQUAL(vidalParams.at(Id), AtcCodeValue);
    }

    Y_UNIT_TEST(TestNotVidalAtc) {
        TModelVidalExtractor extractor(".", 1);

        NMarket::NMbo::TExportReportModel model;
        model.set_id(Id);

        auto * p = model.add_parameter_values();
        p->set_param_id(AtcCode + 1);
        p->set_xsl_name(XslName);
        p->add_str_value()->set_value(AtcCodeValue);

        extractor.Process(model);
        extractor.Flush();

        const auto filePath = "model_vidal.gz_1";

        UNIT_ASSERT(NFs::Exists(filePath));

        const auto vidalParams = ReadFile(filePath);
        UNIT_ASSERT_EQUAL(vidalParams.size(), 0);
    }

    Y_UNIT_TEST(TestEmptyVidalAtc) {
        TModelVidalExtractor extractor(".", 1);

        NMarket::NMbo::TExportReportModel model;
        model.set_id(Id);
    
        auto * p = model.add_parameter_values();
        p->set_param_id(AtcCode);
        p->set_xsl_name(XslName);

        extractor.Process(model);
        extractor.Flush();

        const auto filePath = "model_vidal.gz_1";

        UNIT_ASSERT(NFs::Exists(filePath));

        const auto vidalParams = ReadFile(filePath);
        UNIT_ASSERT_EQUAL(vidalParams.size(), 0);
    }


    Y_UNIT_TEST(TestEmptyVidalAtcValue) {
        TModelVidalExtractor extractor(".", 1);

        NMarket::NMbo::TExportReportModel model;
        model.set_id(Id);
    
        auto * p = model.add_parameter_values();
        p->set_param_id(AtcCode);
        p->set_xsl_name(XslName);
        p->add_str_value()->set_value("");

        extractor.Process(model);
        extractor.Flush();

        const auto filePath = "model_vidal.gz_1";

        UNIT_ASSERT(NFs::Exists(filePath));

        const auto vidalParams = ReadFile(filePath);
        UNIT_ASSERT_EQUAL(vidalParams.size(), 0);
    }

};

