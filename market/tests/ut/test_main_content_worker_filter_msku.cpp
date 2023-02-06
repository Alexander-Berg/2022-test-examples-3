#include <market/tools/msku-uploader/lib/worker.h>
#include <market/library/mbo_params/mbo_parameters.h>
#include <market/library/mbo_params/defines.h>
#include <library/cpp/testing/unittest/gtest.h>

using namespace NMarket::NMbo;

Y_UNIT_TEST_SUITE(TMainContentWorkerSuite) {
    Y_UNIT_TEST(MskuWithGoodExpNotFiltered) {
        TMboCategory category;
        TCategoryInfo unusedCategoryInfo(category);
        TContexRelations relations("");
        TExportReportModelPb model;
        auto relation = model.add_relations();
        relation->set_id(100);
        relation->set_type(NMarket::NMbo::TRelationType::EXPERIMENTAL_BASE_MODEL);

        auto title = model.add_titles();
        title->set_value("Some title");

        model.set_experiment_flag("Exp10");

        TMskuRecord record;
        record.Msku = 333;

        auto worker = BuildMainContentWorker(relations, true);
        EXPECT_TRUE(worker->Process(model, unusedCategoryInfo, record));
    }

    Y_UNIT_TEST(MskuWithBadExpFiltered) {
        TMboCategory category;
        TCategoryInfo unusedCategoryInfo(category);
        TContexRelations relations("");
        TExportReportModelPb model;
        auto relation = model.add_relations();
        relation->set_id(100);
        relation->set_type(NMarket::NMbo::TRelationType::EXPERIMENTAL_BASE_MODEL);

        auto title = model.add_titles();
        title->set_value("Some title");

        TMskuRecord record;
        record.Msku = 333;

        auto worker = BuildMainContentWorker(relations, true);
        EXPECT_FALSE(worker->Process(model, unusedCategoryInfo, record));
    }
}


