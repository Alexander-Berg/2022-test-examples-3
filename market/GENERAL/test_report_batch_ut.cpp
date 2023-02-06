#include <market/kombat/handler/test_report_batch.h>
#include <market/library/shiny/server/handler.h>
#include <market/library/shiny/term/terminate_mock.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket::NKombat;
using namespace NMarket::NShiny;
using namespace testing;

namespace {
    struct TExecutorMock : IExecutor {
        MOCK_METHOD(TBattleId, Execute, (const IBattle::TSpec&), (override));
    };

    struct TReportsMock : IReportStorage {
        MOCK_METHOD(THolder<IReportSource>, FindReport, (const TSearchContext&), (const, override));
        MOCK_METHOD(TVector<THolder<IReportSource>>, ListReports, (const TListContext&), (const, override));
        MOCK_METHOD(i64, CreateReport, (const TReportContext&), (override));
        MOCK_METHOD(bool, IsReportReady, (i64), (const, override));
        MOCK_METHOD(TString, GetRbtorrent, (i64), (const, override));
        MOCK_METHOD(bool, IsExpired, (i64, TInstant), (const, override));
        MOCK_METHOD(i64, CreateRevisionRangeTask, (ui32, ui32), (override));
        MOCK_METHOD(TVector<ui32>, GetRevisionRange, (i64), (const, override));
    };

    void CheckBattle(const NSc::TValue& battle, TString id, TString name, bool maxrps, bool onlyScenarios = false) {
        UNIT_ASSERT_EQUAL(battle["id"], id);
        UNIT_ASSERT_EQUAL(battle["name"], name);
        UNIT_ASSERT_EQUAL(battle["maxrps"].GetBool(), maxrps);
        UNIT_ASSERT_EQUAL(battle["only-scenarios"].GetBool(), onlyScenarios);
    }
}

Y_UNIT_TEST_SUITE(TestReportBatch) {
    Y_UNIT_TEST(Base) {
        struct {
            THolder<IExecutor> Executor;
            THolder<TReportsMock> BaseReports;
            THolder<TReportsMock> MetaReports;
            IStatistics& Statistics = IStatistics::Dummy();
        } env;

        auto executor = MakeHolder<TExecutorMock>();
        auto baseReports = MakeHolder<TReportsMock>();
        auto metaReports = MakeHolder<TReportsMock>();
        EXPECT_CALL(*executor, Execute(_))
            .WillOnce(Return("0"))
            .WillOnce(Return("1"))
            .WillOnce(Return("2"))
            .WillOnce(Return("3"))
            .WillOnce(Return("4"))
            .WillOnce(Return("5"));

        env.Executor = std::move(executor);
        env.BaseReports = std::move(baseReports);
        env.MetaReports = std::move(metaReports);

        THandler<TTestReportBatch> sut(env, "test_report_batch", EHttpMethod::GET);
        const auto response = sut.Invoke("ticket=DUMMY-1&base_version=1.1.1.0&version=1.1.2.0");
        UNIT_ASSERT_EQUAL(response.Code, 200);
        const auto responseJson = NSc::TValue::FromJson(response.Text);
        const auto& battles = responseJson.GetArray();
        UNIT_ASSERT_EQUAL(battles.size(), 6);
        CheckBattle(battles[0], "0", "MAIN", false);
        CheckBattle(battles[1], "1", "MAIN@prime", false);
        CheckBattle(battles[2], "2", "API", false);
        CheckBattle(battles[3], "3", "PARALLEL", false);
        CheckBattle(battles[4], "4", "MAIN", true);
        CheckBattle(battles[5], "5", "MAIN", false, true);
    }
}
