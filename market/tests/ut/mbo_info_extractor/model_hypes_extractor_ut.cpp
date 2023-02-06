#include <market/idx/models/bin/mbo-info-extractor/model_hypes_extractor.h>
#include <library/cpp/testing/unittest/registar.h>
#include <util/system/fs.h>


using namespace NMarket::NMboInfoExtractor;

struct TMboData {
    ui64 Id;
    TProtoStringType XslName;
    ui32 BitMask;
};

Y_UNIT_TEST_SUITE(TExclusiveExtractorSute) {

    constexpr auto Id = 1;

    std::vector<TMboData> HypesData = {
        {28530570, "exclusive", 1 << 0},
        {27625090, "hype_googs", 1 << 1},
        {33457410, "rare_item", 1 << 2}
    };

    THashMap<ui64, ui32> ReadFile(const TString& filePath) {
        TUnbufferedFileInput fileInput(filePath);
        TBufferedZLibDecompress decompressor(&fileInput, ZLib::GZip);

        TString line;
        THashMap<ui64, ui32> hypesParams;
        while (decompressor.ReadLine(line)) {
            TStringBuf lineBuf = line;
            TStringBuf modelIdColumn = lineBuf.NextTok('\t');
            TStringBuf hypesColumn = lineBuf.NextTok('\t');

            ui64 id = 0;
            ui32 value = 0;
            if (TryFromString(modelIdColumn, id) && TryFromString(hypesColumn, value)) {
                hypesParams[id] = value;
            }
        }
        return hypesParams;
    }

    Y_UNIT_TEST(TestModelHaveAllHypes) {
        TModelHypesExtractor extractor(".", 1);

        NMarket::NMbo::TExportReportModel model;
        model.set_id(Id);

        for (const auto& hype : HypesData) {
            auto *p = model.add_parameter_values();
            p->set_param_id(hype.Id);
            p->set_xsl_name(hype.XslName);
            p->set_bool_value(true);
        }

        extractor.Process(model);
        extractor.Flush();

        const auto filePath = "model_hypes.gz_1";

        UNIT_ASSERT(NFs::Exists(filePath));

        const auto outputParams = ReadFile(filePath);

        UNIT_ASSERT_EQUAL(outputParams.size(), 1);

        uint32_t hypeMasks = 0;
        for (const auto& hype : HypesData) {
            hypeMasks |= hype.BitMask;
        }
        UNIT_ASSERT_EQUAL(outputParams.at(Id), hypeMasks);
    }

    Y_UNIT_TEST(TestConcreteHypeIsTrue) {
        for (const auto& hype : HypesData) {
            TModelHypesExtractor extractor(".", 1);

            NMarket::NMbo::TExportReportModel model;
            model.set_id(Id);

            auto *p = model.add_parameter_values();
            p->set_param_id(hype.Id);
            p->set_xsl_name(hype.XslName);
            p->set_bool_value(true);

            extractor.Process(model);
            extractor.Flush();

            const auto filePath = "model_hypes.gz_1";

            UNIT_ASSERT(NFs::Exists(filePath));

            const auto outputParams = ReadFile(filePath);

            UNIT_ASSERT_EQUAL(outputParams.size(), 1);
            UNIT_ASSERT_EQUAL(outputParams.at(Id), hype.BitMask);
        }
    }

    Y_UNIT_TEST(TestConcreteHypeIsFalse) {
        for (const auto& hype : HypesData) {
            TModelHypesExtractor extractor(".", 1);

            NMarket::NMbo::TExportReportModel model;
            model.set_id(Id);

            auto *p = model.add_parameter_values();
            p->set_param_id(hype.Id);
            p->set_xsl_name(hype.XslName);
            p->set_bool_value(false);

            extractor.Process(model);
            extractor.Flush();

            const auto filePath = "model_hypes.gz_1";

            UNIT_ASSERT(NFs::Exists(filePath));

            const auto outputParams = ReadFile(filePath);
            UNIT_ASSERT_EQUAL(outputParams.size(), 0);
        }
    }

    Y_UNIT_TEST(TestWrongHype) {
        TModelHypesExtractor extractor(".", 1);

        NMarket::NMbo::TExportReportModel model;
        model.set_id(Id);

        auto * p = model.add_parameter_values();
        p->set_param_id(666);
        p->set_xsl_name("evil_hype");
        p->set_bool_value(true);

        extractor.Process(model);
        extractor.Flush();

        const auto filePath = "model_hypes.gz_1";

        UNIT_ASSERT(NFs::Exists(filePath));

        const auto outputParams = ReadFile(filePath);
        UNIT_ASSERT_EQUAL(outputParams.size(), 0);
    }
};