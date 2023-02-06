#include <market/idx/models/lib/mbo-info-extractor/contex_experiments_extractor.h>
#include <market/library/snappy-protostream/proto_snappy_stream.h>
#include <market/proto/indexer/contex.pb.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(TContexProtoSuite) {
        THashMap<ui64, NMarket::NContex::TContexRelations> ReadFile(const TString& filePath) {
            THashMap<ui64, NMarket::NContex::TContexRelations> ret;
            NMarket::TSnappyProtoReader reader(filePath, "CNTX");

            NMarket::NContex::TContexRelations relation;
            while (reader.Load(relation)) {
                auto id = relation.id();
                ret[id] = std::move(relation);
            }
            return ret;
        }

        THashMap<ui64, NMarket::NContex::ERelationType> UnpackRelations(const NMarket::NContex::TContexRelations& relations) {
            THashMap<ui64, NMarket::NContex::ERelationType> ret;
            for (const auto& r : relations.relations()) {
                ret[r.id()] = r.type();
            }
            return ret;
        }

    Y_UNIT_TEST(TestSimple) {
        NMarket::NMbo::TExportReportModel modelWoExp;
        modelWoExp.set_id(1);
        auto relation = modelWoExp.add_relations();
        relation->set_id(2);
        relation->set_type(NMarket::NMbo::TRelationType::SKU_MODEL);

        NMarket::NMbo::TExportReportModel modelOrig;
        modelOrig.set_id(10);
        relation = modelOrig.add_relations();
        relation->set_id(11);
        relation->set_type(NMarket::NMbo::TRelationType::EXPERIMENTAL_MODEL);
        relation = modelOrig.add_relations();
        relation->set_id(20);
        relation->set_type(NMarket::NMbo::TRelationType::SKU_MODEL);

        NMarket::NMbo::TExportReportModel modelExp;
        modelExp.set_id(11);
        modelExp.set_experiment_flag("exp");
        relation = modelExp.add_relations();
        relation->set_id(10);
        relation->set_type(NMarket::NMbo::TRelationType::EXPERIMENTAL_BASE_MODEL);
        relation = modelExp.add_relations();
        relation->set_id(21);
        relation->set_type(NMarket::NMbo::TRelationType::SKU_MODEL);

        NMarket::NMbo::TExportReportModel skuWoExp;
        skuWoExp.set_current_type("SKU");
        skuWoExp.set_id(2);
        relation = skuWoExp.add_relations();
        relation->set_id(1);
        relation->set_type(NMarket::NMbo::TRelationType::SKU_PARENT_MODEL);

        NMarket::NMbo::TExportReportModel skuOrig;
        skuOrig.set_current_type("SKU");
        skuOrig.set_id(20);
        relation = skuOrig.add_relations();
        relation->set_id(10);
        relation->set_type(NMarket::NMbo::TRelationType::SKU_PARENT_MODEL);
        relation = skuOrig.add_relations();
        relation->set_id(21);
        relation->set_type(NMarket::NMbo::TRelationType::EXPERIMENTAL_MODEL);

        NMarket::NMbo::TExportReportModel skuExp;
        skuExp.set_current_type("EXPERIMENTAL_SKU");
        skuExp.set_id(21);
        skuExp.set_experiment_flag("exp");
        relation = skuExp.add_relations();
        relation->set_id(11);
        relation->set_type(NMarket::NMbo::TRelationType::SKU_PARENT_MODEL);
        relation = skuExp.add_relations();
        relation->set_id(20);
        relation->set_type(NMarket::NMbo::TRelationType::EXPERIMENTAL_BASE_MODEL);

        NMarket::NMboInfoExtractor::TContexProtoExtractor extractor1(".");
        NMarket::NMboInfoExtractor::TContexProtoExtractor extractor2(".");
        extractor1.Process(modelWoExp);
        extractor2.Process(modelOrig);
        extractor1.Process(modelExp);
        extractor2.Process(skuWoExp);
        extractor1.Process(skuOrig);
        extractor2.Process(skuExp);

        extractor1.Merge(std::move(extractor2));
        extractor1.Flush();

        auto relations = ReadFile("contex_relations.pbsn");

        auto r = UnpackRelations(relations[1]);
        UNIT_ASSERT(r.empty());

        r = UnpackRelations(relations[10]);
        UNIT_ASSERT_EQUAL(r.size(), 1);
        UNIT_ASSERT_EQUAL(r[11], NMarket::NContex::ERelationType::EXPERIMENTAL_MODEL);

        r = UnpackRelations(relations[11]);
        UNIT_ASSERT_EQUAL(r.size(), 1);
        UNIT_ASSERT_EQUAL(r[10], NMarket::NContex::ERelationType::EXPERIMENTAL_BASE_MODEL);

        r = UnpackRelations(relations[20]);
        // для оригинальной ску есть привязки к
        // - экспериментальной ску
        // - экспериментальной модели
        // - базовой модели
        UNIT_ASSERT_EQUAL(r.size(), 3);
        UNIT_ASSERT_EQUAL(r[10], NMarket::NContex::ERelationType::EXPERIMENTAL_BASE_MODEL);
        UNIT_ASSERT_EQUAL(r[11], NMarket::NContex::ERelationType::EXPERIMENTAL_MODEL);
        UNIT_ASSERT_EQUAL(r[21], NMarket::NContex::ERelationType::EXPERIMENTAL_SKU);

        r = UnpackRelations(relations[21]);
        // для экспериментальной ску есть привязки к
        // - оригинальной ску
        // - экспериментальной модели
        // - базовой модели(родительской) к экспериментальной модели
        UNIT_ASSERT_EQUAL(r.size(), 3);
        UNIT_ASSERT_EQUAL(r[10], NMarket::NContex::ERelationType::EXPERIMENTAL_BASE_MODEL);
        UNIT_ASSERT_EQUAL(r[11], NMarket::NContex::ERelationType::EXPERIMENTAL_MODEL);
        UNIT_ASSERT_EQUAL(r[20], NMarket::NContex::ERelationType::EXPERIMENTAL_BASE_SKU);

        // в общем случае базова модель оригинальной ску
        // может отличаться от базовой к экспериментальной модели
        // экспериментальной ску
    }
}
