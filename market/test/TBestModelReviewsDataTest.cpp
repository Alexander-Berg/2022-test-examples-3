#include <market/report/library/global/best_model_grades/best_model_grades.h>
#include <market/report/library/test/mock_error_log/mock_error_log.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>

using namespace NMarketReport;

TEST(TBestModelReviewData, FileNotExistsHandled) {
    MockErrorLog errors;
    auto path = SRC_("./TestData/best_model_reviews/not_exists.csv");
    TBestModelGradesData data_reader;
    data_reader.LoadFromCsv(path);
    EXPECT_TRUE(data_reader.GetGradesData().empty());
    EXPECT_TRUE(errors.ExpectErrorLike("File(.*)does not exist."));
}

TEST(TBestModelReviewData, EmptyFileParse) {
    auto path = SRC_("./TestData/best_model_reviews/empty_file.csv");
    TBestModelGradesData data_reader;
    data_reader.LoadFromCsv(path);
    EXPECT_TRUE(data_reader.GetGradesData().empty());
}

TEST(TBestModelReviewData, TestRegionEmpty) {
    auto path = SRC_("./TestData/best_model_reviews/region_empty.csv");
    TBestModelGradesData data_reader;
    data_reader.LoadFromCsv(path);
    EXPECT_TRUE(data_reader.GetGradesData().size() == 1);
    EXPECT_FALSE(data_reader.GetGradesData()[0]->region.Defined());
}

TEST(TBestModelReviewData, TestRegionNotEmpty) {
    auto path = SRC_("./TestData/best_model_reviews/region_not_empty.csv");
    TBestModelGradesData data_reader;
    data_reader.LoadFromCsv(path);
    EXPECT_TRUE(data_reader.GetGradesData().size() == 1);
    EXPECT_TRUE(data_reader.GetGradesData()[0]->region.Defined());
    EXPECT_TRUE(*(data_reader.GetGradesData()[0]->region.Get()) == 213);
}

TEST(TBestModelReviewData, TestRegionFailParse) {
    MockErrorLog errors;
    auto path = SRC_("./TestData/best_model_reviews/region_fail_parse.csv");
    TBestModelGradesData data_reader;
    data_reader.LoadFromCsv(path);
    EXPECT_TRUE(data_reader.GetGradesData().empty());
    EXPECT_TRUE(errors.ExpectErrorLike("Fail to fill TModelGrade field: 'region' by value 'avadakedavraregion' in line 2. Skip it."));
}

TEST(TBestModelReviewData, FailIntParse) {
    MockErrorLog errors;
    auto path = SRC_("./TestData/best_model_reviews/fail_int_field_parse.csv");
    TBestModelGradesData data_reader;
    data_reader.LoadFromCsv(path);
    EXPECT_TRUE(data_reader.GetGradesData().empty());
    EXPECT_TRUE(errors.ExpectErrorLike("Fail to fill TModelGrade field: 'grade_id' by value 'avadakedavra' in line 2. Skip it."));
}

TEST(TBestModelReviewData, FieldCountMismatch) {
    MockErrorLog errors;
    auto path = SRC_("./TestData/best_model_reviews/field_count_mismatch.csv");
    TBestModelGradesData data_reader;
    data_reader.LoadFromCsv(path);
    EXPECT_TRUE(data_reader.GetGradesData().empty());
    EXPECT_TRUE(errors.ExpectErrorLike("headerLine and bodyLine fields count mismatch"));
}

TEST(TBestModelReviewData, ProductionExample) {
    auto path = SRC_("./TestData/best_model_reviews/production_example.csv");
    TBestModelGradesData data_reader;
    data_reader.LoadFromCsv(path);
    EXPECT_FALSE(data_reader.GetGradesData().empty());
}

TEST(TBestModelReviewData, SimpleGradeParse) {
    auto path = SRC_("./TestData/best_model_reviews/simple_grade_parse.csv");
    TBestModelGradesData data_reader;
    data_reader.LoadFromCsv(path);
    EXPECT_TRUE(data_reader.GetGradesData().size() == 1);
    auto grade = data_reader.GetGradesData()[0];
    EXPECT_TRUE(grade->grade_id == 1);
    EXPECT_TRUE(grade->model_id == 2);
    EXPECT_TRUE(grade->author_id == 3);
    EXPECT_TRUE(grade->anonymous == 1);
    EXPECT_TRUE(grade->grade_value == 4);
    EXPECT_TRUE(grade->pro == "pro,_text");
    EXPECT_TRUE(grade->contra == "contra_text");
    EXPECT_TRUE(grade->short_text == "short_text");
    EXPECT_TRUE(grade->agree == 5);
    EXPECT_TRUE(grade->reject == 6);
    EXPECT_TRUE(grade->total_votes == 7);
    EXPECT_TRUE(grade->cr_time == "cr_time_text");
    EXPECT_TRUE(grade->region == 8);
    EXPECT_DOUBLE_EQ(grade->rank, 0.9);
}

