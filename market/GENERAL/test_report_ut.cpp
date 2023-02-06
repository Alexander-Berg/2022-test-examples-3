#include <market/kombat/handler/test_report.h>
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

    void VerifyAttack(const TAttackSpec& attack) {
        UNIT_ASSERT_EQUAL(attack.GetRepeat(), 3);
        UNIT_ASSERT_EQUAL(attack.GetFire().GetProfile().GetStreamCount(), 1);
        UNIT_ASSERT(attack.GetFire().GetProfile().HasRequestCount());
        UNIT_ASSERT(!attack.GetFire().GetProfile().HasRps());
        UNIT_ASSERT(attack.GetFire().GetDoWarmup());
        UNIT_ASSERT(!attack.HasAdaptiveRepeat());
    }
}

Y_UNIT_TEST_SUITE(TestReport) {
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
        TBattleSpec battle;
        EXPECT_CALL(*executor, Execute(_)).WillOnce(DoAll(SaveArg<0>(&battle), Return("123")));

        env.Executor = std::move(executor);
        env.BaseReports = std::move(baseReports);
        env.MetaReports = std::move(metaReports);

        THandler<TTestReport> sut(env, "test_report", EHttpMethod::GET);
        const auto response = sut.Invoke("ticket=DUMMY-1&base_version=1.1.1.0&version=1.1.2.0");
        UNIT_ASSERT_EQUAL(response.Code, 200);
        const auto responseJson = NSc::TValue::FromJson(response.Text);
        UNIT_ASSERT_EQUAL(responseJson["battle_id"], "123");
        UNIT_ASSERT_EQUAL(battle.AttackSize(), 2);
        for (size_t i = 0; i < 2; ++i) {
            VerifyAttack(battle.GetAttack(i));
        }
    }
}
