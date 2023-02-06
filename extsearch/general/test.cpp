#include <extsearch/geo/kernel/fast_feature_storage/reader.h>
#include <extsearch/geo/kernel/fast_feature_storage/writer.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/stream/file.h>

using namespace NGeosearch;

namespace {
    void FillMms(NFastFeatureStorage::TFFMmsStandalone& mms) {
        auto& fd = mms.CompaniesFeatures["123456789"]["feature_id"];
        fd.Aref = "#yandex";
        fd.Values.push_back("1");
        fd.Values.push_back("2");
        fd.Values.push_back("3");
    }

    void CheckMms(const NFastFeatureStorage::TFFMmsMmapped& mms) {
        const auto& fd = mms.CompaniesFeatures.at("123456789").at("feature_id");
        UNIT_ASSERT_EQUAL(fd.Aref, "#yandex");
        UNIT_ASSERT_EQUAL(fd.Values.size(), 3);
    }
} // namespace

Y_UNIT_TEST_SUITE(TFastFeatureStorageTest) {
    Y_UNIT_TEST(TestGeneric) {
        const TString fileName = "fast_features.mms";
        const size_t n = 42;
        {
            NFastFeatureStorage::TWriter writer;
            FillMms(writer.Mms());

            for (size_t i = 0; i < n; ++i) {
                NFastFeatureStorage::TMessage message;
                auto* refs = message.MutableReferenceList();
                refs->SetId(i);
                auto* ref = refs->AddReferences();
                ref->SetScope("scope");
                ref->SetId(ToString(i));
                writer.AddMessage(message);
            }

            writer.WriteFile(fileName);
        }
        {
            NFastFeatureStorage::TReader reader{fileName};
            UNIT_ASSERT_EQUAL(reader.CalculateMD5().length(), 32);
            UNIT_ASSERT(reader.GetMms());
            CheckMms(*reader.GetMms());

            NFastFeatureStorage::TMessage message;
            for (size_t i = 0; i < n; ++i) {
                UNIT_ASSERT(reader.GetNextMessage(message));
                UNIT_ASSERT(message.HasReferenceList());
                UNIT_ASSERT_EQUAL(message.GetReferenceList().GetId(), i);
                UNIT_ASSERT_EQUAL(message.GetReferenceList().GetReferences(0).GetScope(), "scope");
            }
            UNIT_ASSERT(!reader.GetNextMessage(message));
        }
    }

    Y_UNIT_TEST(TestOldFileNewReader) {
        const TString fileName = "fast_features_old.mms";
        {
            NFastFeatureStorage::TFFMmsStandalone mms;
            FillMms(mms);
            TFileOutput out{fileName};
            NMms::Write(out, mms);
        }
        {
            NFastFeatureStorage::TReader reader{fileName};
            UNIT_ASSERT(reader.GetMms());
            CheckMms(*reader.GetMms());

            NFastFeatureStorage::TMessage message;
            UNIT_ASSERT(!reader.GetNextMessage(message));
        }
    }

    Y_UNIT_TEST(TestNewFileOldReader) {
        const TString fileName = "fast_features_new.mms";
        {
            NFastFeatureStorage::TWriter writer;
            FillMms(writer.Mms());
            writer.WriteFile(fileName);
        }
        {
            NMms::TMapping<NFastFeatureStorage::TFFMmsMmapped> holder{fileName};
            CheckMms(*holder);
        }
    }
}
