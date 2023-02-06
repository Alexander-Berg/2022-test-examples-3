#include <market/idx/models/bin/mbo-info-extractor/model_medical_flags_extractor.h>
#include <market/library/interface/indexer_report_interface.h>

#include <library/cpp/testing/unittest/registar.h>
#include <util/system/fs.h>


using namespace NMarket::NMboInfoExtractor;

Y_UNIT_TEST_SUITE(TMedicalFlagsExtractorSuite) {

    constexpr auto Id = 1;
    constexpr auto FilePath = "model_medical_flags.gz_1";

    THashMap<ui64, ui32> ReadFile(const TString& path)
    {
        TUnbufferedFileInput fileInput(path);
        TBufferedZLibDecompress decompressor(&fileInput, ZLib::GZip);

        TString line;
        THashMap<ui64, ui32> medicalFlags;
        while (decompressor.ReadLine(line)) {
            TStringBuf lineBuf = line;
            TStringBuf modelIdColumn = lineBuf.NextTok('\t');
            TStringBuf medicalFlagsColumn = lineBuf.NextTok('\t');

            ui64 id = 0;
            ui32 flags = 0;
            if (TryFromString(modelIdColumn, id) && TryFromString(medicalFlagsColumn, flags)) {
                medicalFlags[id] = flags;
            }
        }
        return medicalFlags;
    }

    Y_UNIT_TEST(TestExtractorFile) {
        auto extractor = TModelMedicalFlagsExtractor(".", 1);
        auto model = NMarket::NMbo::TExportReportModel();

        model.set_id(Id);

        extractor.Process(model);
        extractor.Flush();

        UNIT_ASSERT(NFs::Exists(FilePath));
    }

    Y_UNIT_TEST(TestMedicalFlagsAll) {
        auto extractor = TModelMedicalFlagsExtractor(".", 1);
        auto model = NMarket::NMbo::TExportReportModel();

        model.set_id(Id);

        {
            auto *p = model.add_parameter_values();
            p->set_param_id(NMarket::NDocumentFlags::DRUG_TYPE_PARAM);
            p->set_option_id(NMarket::NDocumentFlags::MEDICINE_TYPE_VALUE);
        }

        {
            auto *p = model.add_parameter_values();
            p->set_param_id(NMarket::NDocumentFlags::DRUG_TYPE_PARAM);
            p->set_option_id(NMarket::NDocumentFlags::MEDICAL_PRODUCT_TYPE_VALUE);
        }

        {
            auto *p = model.add_parameter_values();
            p->set_param_id(NMarket::NDocumentFlags::DRUG_TYPE_PARAM);
            p->set_option_id(NMarket::NDocumentFlags::BAA_TYPE_VALUE);
        }

        {
            auto *p = model.add_parameter_values();
            p->set_param_id(NMarket::NDocumentFlags::PRESCRIPTION_PARAM_OLD);
            p->set_bool_value(true);
        }

        {
            auto *p = model.add_parameter_values();
            p->set_param_id(NMarket::NDocumentFlags::PSYCHOTROPIC_PARAM);
            p->set_bool_value(true);
        }

        {
            auto *p = model.add_parameter_values();
            p->set_param_id(NMarket::NDocumentFlags::NARCOTIC_PARAM);
            p->set_bool_value(true);
        }

        {
            auto *p = model.add_parameter_values();
            p->set_param_id(NMarket::NDocumentFlags::PRECURSOR_PARAM);
            p->set_bool_value(true);
        }

        {
            auto *p = model.add_parameter_values();
            p->set_param_id(NMarket::NDocumentFlags::ETHANOL_PRECENT_PARAM);
            p->set_numeric_value(ToString(NMarket::NDocumentFlags::ETHANOL_PRECENT_LIMIT+1));
        }

        extractor.Process(model);
        extractor.Flush();

        UNIT_ASSERT(NFs::Exists(FilePath));

        auto modelMedicalFlags = ReadFile(FilePath);

        ui32 medical_flags = NMarket::NDocumentFlags::MIF_NONE;
        medical_flags = NMarket::NDocumentFlags::SetMedicalDocumentFlag(medical_flags, NMarket::NDocumentFlags::IS_MEDICINE);
        medical_flags = NMarket::NDocumentFlags::SetMedicalDocumentFlag(medical_flags, NMarket::NDocumentFlags::IS_MEDICAL_PRODUCT);
        medical_flags = NMarket::NDocumentFlags::SetMedicalDocumentFlag(medical_flags, NMarket::NDocumentFlags::IS_BAA);
        medical_flags = NMarket::NDocumentFlags::SetMedicalDocumentFlag(medical_flags, NMarket::NDocumentFlags::IS_PRESCRIPTION);
        medical_flags = NMarket::NDocumentFlags::SetMedicalDocumentFlag(medical_flags, NMarket::NDocumentFlags::IS_PSYCHOTROPIC);
        medical_flags = NMarket::NDocumentFlags::SetMedicalDocumentFlag(medical_flags, NMarket::NDocumentFlags::IS_NARCOTIC);
        medical_flags = NMarket::NDocumentFlags::SetMedicalDocumentFlag(medical_flags, NMarket::NDocumentFlags::IS_PRECURSOR);
        medical_flags = NMarket::NDocumentFlags::SetMedicalDocumentFlag(medical_flags, NMarket::NDocumentFlags::IS_ETHANOL);

        UNIT_ASSERT_EQUAL(modelMedicalFlags.size(), 1);
        UNIT_ASSERT_EQUAL(modelMedicalFlags.at(Id), medical_flags);
    }

    Y_UNIT_TEST(TestMedicineFlag) {
        auto extractor = TModelMedicalFlagsExtractor(".", 1);
        auto model = NMarket::NMbo::TExportReportModel();

        model.set_id(Id);

        auto *p = model.add_parameter_values();
        p->set_param_id(NMarket::NDocumentFlags::DRUG_TYPE_PARAM);
        p->set_option_id(NMarket::NDocumentFlags::MEDICINE_TYPE_VALUE);

        extractor.Process(model);
        extractor.Flush();

        UNIT_ASSERT(NFs::Exists(FilePath));

        auto modelMedicalFlags = ReadFile(FilePath);

        UNIT_ASSERT_EQUAL(modelMedicalFlags.size(), 1);
        UNIT_ASSERT_EQUAL(modelMedicalFlags.at(Id), NMarket::NDocumentFlags::IS_MEDICINE);
    }

    Y_UNIT_TEST(TestMedicalProductFlag) {
        auto extractor = TModelMedicalFlagsExtractor(".", 1);
        auto model = NMarket::NMbo::TExportReportModel();

        model.set_id(Id);

        auto *p = model.add_parameter_values();
        p->set_param_id(NMarket::NDocumentFlags::DRUG_TYPE_PARAM);
        p->set_option_id(NMarket::NDocumentFlags::MEDICAL_PRODUCT_TYPE_VALUE);

        extractor.Process(model);
        extractor.Flush();

        UNIT_ASSERT(NFs::Exists(FilePath));

        auto modelMedicalFlags = ReadFile(FilePath);

        UNIT_ASSERT_EQUAL(modelMedicalFlags.size(), 1);
        UNIT_ASSERT_EQUAL(modelMedicalFlags.at(Id), NMarket::NDocumentFlags::IS_MEDICAL_PRODUCT);
    }

    Y_UNIT_TEST(TestBaaFlag) {
        auto extractor = TModelMedicalFlagsExtractor(".", 1);
        auto model = NMarket::NMbo::TExportReportModel();

        model.set_id(Id);

        auto *p = model.add_parameter_values();
        p->set_param_id(NMarket::NDocumentFlags::DRUG_TYPE_PARAM);
        p->set_option_id(NMarket::NDocumentFlags::BAA_TYPE_VALUE);

        extractor.Process(model);
        extractor.Flush();

        UNIT_ASSERT(NFs::Exists(FilePath));

        auto modelMedicalFlags = ReadFile(FilePath);

        UNIT_ASSERT_EQUAL(modelMedicalFlags.size(), 1);
        UNIT_ASSERT_EQUAL(modelMedicalFlags.at(Id), NMarket::NDocumentFlags::IS_BAA);
    }

    Y_UNIT_TEST(TestPrescriptionFlag) {
        auto extractor = TModelMedicalFlagsExtractor(".", 1);
        auto model = NMarket::NMbo::TExportReportModel();

        model.set_id(Id);

        auto *p = model.add_parameter_values();
        p->set_param_id(NMarket::NDocumentFlags::PRESCRIPTION_PARAM_NEW);
        p->set_bool_value(true);

        extractor.Process(model);
        extractor.Flush();

        UNIT_ASSERT(NFs::Exists(FilePath));

        auto modelMedicalFlags = ReadFile(FilePath);

        UNIT_ASSERT_EQUAL(modelMedicalFlags.size(), 1);
        UNIT_ASSERT_EQUAL(modelMedicalFlags.at(Id), NMarket::NDocumentFlags::IS_PRESCRIPTION);
    }

    Y_UNIT_TEST(TestPsychotropicFlag) {
        auto extractor = TModelMedicalFlagsExtractor(".", 1);
        auto model = NMarket::NMbo::TExportReportModel();

        model.set_id(Id);

        auto *p = model.add_parameter_values();
        p->set_param_id(NMarket::NDocumentFlags::PSYCHOTROPIC_PARAM);
        p->set_bool_value(true);

        extractor.Process(model);
        extractor.Flush();

        UNIT_ASSERT(NFs::Exists(FilePath));

        auto modelMedicalFlags = ReadFile(FilePath);

        UNIT_ASSERT_EQUAL(modelMedicalFlags.size(), 1);
        UNIT_ASSERT_EQUAL(modelMedicalFlags.at(Id), NMarket::NDocumentFlags::IS_PSYCHOTROPIC);
    }

    Y_UNIT_TEST(TestNarcoticFlag) {
        auto extractor = TModelMedicalFlagsExtractor(".", 1);
        auto model = NMarket::NMbo::TExportReportModel();

        model.set_id(Id);

        auto *p = model.add_parameter_values();
        p->set_param_id(NMarket::NDocumentFlags::NARCOTIC_PARAM);
        p->set_bool_value(true);

        extractor.Process(model);
        extractor.Flush();

        UNIT_ASSERT(NFs::Exists(FilePath));

        auto modelMedicalFlags = ReadFile(FilePath);

        UNIT_ASSERT_EQUAL(modelMedicalFlags.size(), 1);
        UNIT_ASSERT_EQUAL(modelMedicalFlags.at(Id), NMarket::NDocumentFlags::IS_NARCOTIC);
    }

    Y_UNIT_TEST(TestPrecursorFlag) {
        auto extractor = TModelMedicalFlagsExtractor(".", 1);
        auto model = NMarket::NMbo::TExportReportModel();

        model.set_id(Id);

        auto *p = model.add_parameter_values();
        p->set_param_id(NMarket::NDocumentFlags::PRECURSOR_PARAM);
        p->set_bool_value(true);

        extractor.Process(model);
        extractor.Flush();

        UNIT_ASSERT(NFs::Exists(FilePath));

        auto modelMedicalFlags = ReadFile(FilePath);

        UNIT_ASSERT_EQUAL(modelMedicalFlags.size(), 1);
        UNIT_ASSERT_EQUAL(modelMedicalFlags.at(Id), NMarket::NDocumentFlags::IS_PRECURSOR);
    }

    Y_UNIT_TEST(TestEthanolFlag) {
        auto extractor = TModelMedicalFlagsExtractor(".", 1);
        auto model = NMarket::NMbo::TExportReportModel();

        model.set_id(Id);

        auto *p = model.add_parameter_values();
        p->set_param_id(NMarket::NDocumentFlags::ETHANOL_PRECENT_PARAM);
        p->set_numeric_value(ToString(NMarket::NDocumentFlags::ETHANOL_PRECENT_LIMIT+1));

        extractor.Process(model);
        extractor.Flush();

        UNIT_ASSERT(NFs::Exists(FilePath));

        auto modelMedicalFlags = ReadFile(FilePath);

        UNIT_ASSERT_EQUAL(modelMedicalFlags.size(), 1);
        UNIT_ASSERT_EQUAL(modelMedicalFlags.at(Id), NMarket::NDocumentFlags::IS_ETHANOL);
    }

    Y_UNIT_TEST(TestWithoutMedicalFlags) {
        auto extractor = TModelMedicalFlagsExtractor(".", 1);
        auto model = NMarket::NMbo::TExportReportModel();

        model.set_id(Id);

        extractor.Process(model);
        extractor.Flush();

        UNIT_ASSERT(NFs::Exists(FilePath));

        auto modelMedicalFlags = ReadFile(FilePath);

        UNIT_ASSERT_EQUAL(modelMedicalFlags.size(), 0);
    }

    Y_UNIT_TEST(TestNotMedicalFlag) {
        auto extractor = TModelMedicalFlagsExtractor(".", 1);
        auto model = NMarket::NMbo::TExportReportModel();

        model.set_id(Id);

        auto *p = model.add_parameter_values();
        p->set_param_id(NMarket::NDocumentFlags::DRUG_TYPE_PARAM + 1);
        p->set_option_id(NMarket::NDocumentFlags::MEDICINE_TYPE_VALUE + 1);

        extractor.Process(model);
        extractor.Flush();

        UNIT_ASSERT(NFs::Exists(FilePath));

        auto modelMedicalFlags = ReadFile(FilePath);

        UNIT_ASSERT_EQUAL(modelMedicalFlags.size(), 0);
    }
};
