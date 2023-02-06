#include <market/report/library/global/model_grade_agitations/model_grade_agitations.h>
#include <market/report/library/test/mock_error_log/mock_error_log.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>

using namespace NMarketReport;

namespace {
    void CheckCorrectness(const TModelGradeAgitationsData& data) {
        EXPECT_TRUE(data.GetStorageSize() == 5);
        EXPECT_TRUE(data.GetGradeAgitations(100) == 15);
        EXPECT_TRUE(data.GetGradeAgitations(200) == 30);
        EXPECT_TRUE(data.GetGradeAgitations(300) == 123);
        EXPECT_TRUE(data.GetGradeAgitations(400) == 2021);
        EXPECT_TRUE(data.GetGradeAgitations(500) == 1580);
    }
} // namespace

TEST(TModelGradeAgitationsDataTest, SimpleFileParse) {
    TString path = SRC_("./TestData/model_grade_agitations_csv/simple.csv");

    TModelGradeAgitationsData testData;
    testData.LoadFromCSV(path);
    CheckCorrectness(testData);
}

TEST(TModelGradeAgitationsDataTest, FileWithEmptyRows) {
    MockErrorLog errors;
    TString path = SRC_("./TestData/model_grade_agitations_csv/with_empty_rows.csv");

    TModelGradeAgitationsData testData;
    testData.LoadFromCSV(path);
    CheckCorrectness(testData);
    EXPECT_TRUE(errors.ExpectErrorLike("Fail to read. Line number"));
}

TEST(TModelGradeAgitationsDataTest, FileNotAllFields) {
    MockErrorLog errors;
    TString path = SRC_("./TestData/model_grade_agitations_csv/not_all_fields.csv");

    TModelGradeAgitationsData testData;
    testData.LoadFromCSV(path);
    EXPECT_TRUE(testData.GetStorageSize() == 3);
    EXPECT_TRUE(testData.GetGradeAgitations(100) == 15);
    EXPECT_TRUE(testData.GetGradeAgitations(300) == 123);
    EXPECT_TRUE(testData.GetGradeAgitations(500) == 1580);

    EXPECT_TRUE(errors.ExpectErrorLike("Fail to read. Line number"));
}

TEST(TModelGradeAgitationsDataTest, EmptyFile) {
    MockErrorLog errors;
    TString path = SRC_("./TestData/model_grade_agitations_csv/empty.csv");

    TModelGradeAgitationsData testData;
    testData.LoadFromCSV(path);
    EXPECT_TRUE(testData.GetStorageSize() == 0);
    EXPECT_TRUE(errors.ExpectErrorLike("Fail to read headerLine from csv file"));
}

TEST(TModelGradeAgitationsDataTest, FileWithoutHeader) {
    MockErrorLog errors;
    TString path = SRC_("./TestData/model_grade_agitations_csv/without_head.csv");

    TModelGradeAgitationsData testData;
    testData.LoadFromCSV(path);
    EXPECT_TRUE(testData.GetStorageSize() == 4);
    EXPECT_TRUE(testData.GetGradeAgitations(200) == 30);
    EXPECT_TRUE(testData.GetGradeAgitations(300) == 123);
    EXPECT_TRUE(testData.GetGradeAgitations(400) == 2021);
    EXPECT_TRUE(testData.GetGradeAgitations(500) == 1580);
}