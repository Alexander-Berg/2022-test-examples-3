#include <market/kombat/handler/test_precommit_batch.h>
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

    void VerifyAttack(const TAttackSpec& attack, ui32 attackNumber, const TMaybe<TString>& ammoFilter) {
        UNIT_ASSERT_EQUAL(attack.GetRepeat(), 3);
        UNIT_ASSERT_EQUAL(attack.GetFire().GetProfile().GetStreamCount(), 1);
        UNIT_ASSERT(attack.GetFire().GetProfile().HasRequestCount());
        UNIT_ASSERT(!attack.GetFire().GetProfile().HasRps());
        UNIT_ASSERT(attack.GetFire().GetDoWarmup());
        UNIT_ASSERT(!attack.HasAdaptiveRepeat());
        if (attackNumber == 0) {
            UNIT_ASSERT_EQUAL(attack.GetTarget().GetReport().GetRevision(), 2048);
            UNIT_ASSERT_EQUAL(attack.GetTarget().GetMetaReport().GetRevision(), 2048);
        } else {
            UNIT_ASSERT_EQUAL(attack.GetTarget().GetReport().GetPatch().GetReview(), 68461);
            UNIT_ASSERT_EQUAL(attack.GetTarget().GetMetaReport().GetPatch().GetReview(), 68461);
        }
        if (ammoFilter) {
            UNIT_ASSERT_EQUAL(attack.GetFire().GetAmmo().GetFilter(), *ammoFilter);
        }
    }

    void VerifyBattle(const TBattleSpec& battle, const TMaybe<TString>& ammoFilter) {
        UNIT_ASSERT_EQUAL(battle.AttackSize(), 2);
        UNIT_ASSERT_EQUAL(battle.GetOwner(), "CI");
        UNIT_ASSERT_EQUAL(battle.GetResult().GetPublisher().GetStartrek().GetTicket(), "MARKETKOMBAT-987");
        UNIT_ASSERT_EQUAL(battle.GetPriority(), 300);
        for (ui32 i = 0; i < 2; ++i) {
            VerifyAttack(battle.GetAttack(i), i, ammoFilter);
        }
    }
}

Y_UNIT_TEST_SUITE(TestPrecommitBatch) {
    Y_UNIT_TEST(Base) {
        struct {
            THolder<IExecutor> Executor;
        } env;

        auto executor = MakeHolder<TExecutorMock>();
        TBattleSpec primeBattle;
        EXPECT_CALL(*executor, Execute(_))
            .WillOnce(DoAll(SaveArg<0>(&primeBattle), Return("1024")));
        env.Executor = std::move(executor);

        THandler<TTestPrecommitBatch> sut(env, "test_precommit_batch", EHttpMethod::GET);
        const auto response = sut.Invoke("ticket=MARKETKOMBAT-987&owner=CI&br=r2048&pr=68461&priority=300");

        UNIT_ASSERT_EQUAL(response.Code, 200);
        const auto responseJson = NSc::TValue::FromJson(response.Text);
        UNIT_ASSERT_EQUAL(responseJson.GetArray().size(), 1);
        UNIT_ASSERT_EQUAL(responseJson[0].GetString(), "1024");

        VerifyBattle(primeBattle, "prime");
    }
}
