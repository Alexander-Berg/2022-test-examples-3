#include <market/kombat/handler/test_exp_batch.h>
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

    void VerifyAttack(const TAttackSpec& attack, ui32 attackNum, const TMaybe<TString>& ammoFilter) {
        const auto& rearrFlags = attack.GetFire().GetAmmo().GetRearrFlag();
        if (attackNum == 0) {
            UNIT_ASSERT(rearrFlags.empty());
        } else {
            UNIT_ASSERT_EQUAL(rearrFlags.size(), 2);
            UNIT_ASSERT_EQUAL(rearrFlags[0], "my_flag_1=my_value_1");
            UNIT_ASSERT_EQUAL(rearrFlags[1], "my_flag_2=my_value_2");
        }
        if (ammoFilter) {
            UNIT_ASSERT_EQUAL(attack.GetFire().GetAmmo().GetFilter(), *ammoFilter);
        }
    }

    void VerifyBattle(const TBattleSpec& battle, const TMaybe<TString>& ammoFilter) {
        UNIT_ASSERT_EQUAL(battle.AttackSize(), 2);
        UNIT_ASSERT_EQUAL(battle.GetOwner(), "AB");
        UNIT_ASSERT_EQUAL(battle.GetResult().GetPublisher().GetStartrek().GetTicket(), "MARKETKOMBAT-987");
        UNIT_ASSERT_EQUAL(battle.GetResult().GetPublisher().GetStartrek().GetExperimentTicket(), "EXPERIMENT-123");
        for (ui32 i = 0; i < 2; ++i) {
            VerifyAttack(battle.GetAttack(i), i, ammoFilter);
        }
    }
}

Y_UNIT_TEST_SUITE(TestExpBatch) {
    Y_UNIT_TEST(Base) {
        struct {
            THolder<IExecutor> Executor;
            THolder<IDataModel> BattleDataModel = IDataModel::CreateInMemory();
        } env;

        auto executor = MakeHolder<TExecutorMock>();
        TBattleSpec battle;
        TBattleSpec primeBattle;
        EXPECT_CALL(*executor, Execute(_))
            .WillOnce(DoAll(SaveArg<0>(&battle), Return("123")))
            .WillOnce(DoAll(SaveArg<0>(&primeBattle), Return("124")));
        env.Executor = std::move(executor);

        THandler<TTestExpBatch> sut(env, "test_exp_batch", EHttpMethod::GET);
        const auto response = sut.Invoke(
            "ticket=MARKETKOMBAT-987&owner=AB&experiment-ticket=EXPERIMENT-123&rearr-factor=my_flag_1%3Dmy_value_1&rearr-factor=my_flag_2%3Dmy_value_2"
        );

        UNIT_ASSERT_EQUAL(response.Code, 200);
        const auto responseJson = NSc::TValue::FromJson(response.Text);
        UNIT_ASSERT_EQUAL(responseJson.GetArray().size(), 2);
        UNIT_ASSERT_EQUAL(responseJson[0].GetString(), "123");
        UNIT_ASSERT_EQUAL(responseJson[1].GetString(), "124");

        VerifyBattle(battle, Nothing());
        VerifyBattle(primeBattle, "prime");
    }
}
