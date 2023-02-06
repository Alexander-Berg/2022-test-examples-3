#include <market/kombat/engine/battle_mock.h>
#include <market/kombat/handler/test_report_result.h>
#include <market/library/shiny/server/handler.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket::NKombat;
using namespace NMarket::NShiny;
using namespace testing;

namespace {
    struct TTestEnv {
        THolder<TBattleStorageMock> Battles;
    };

    struct TFixture : public NUnitTest::TBaseFixture {
        TFixture()
            : Sut(Env, "test_report_result", EHttpMethod::GET)
        {
        }

        void AddBattle(
            const TBattleId& id,
            IBattle::EStatus status,
            ui32 progress,
            const TBattleSpec& spec,
            const TMaybe<TResultPack>& result
        ) {
            auto battle = MakeHolder<TBattleMock>();
            IBattle::TState state{.Status = status, .Progress = progress};
            BattleDatas.push_back({.Id = id});
            auto& battleData = BattleDatas.back();
            BattleMetas.push_back({.Spec = spec});
            auto& battleMeta = BattleMetas.back();

            EXPECT_CALL(*battle, GetId()).WillRepeatedly(Return(id));
            EXPECT_CALL(*battle, GetState()).WillRepeatedly(Return(state));
            EXPECT_CALL(*battle, GetData()).WillRepeatedly(ReturnRef(battleData));
            EXPECT_CALL(*battle, GetMeta()).WillRepeatedly(ReturnRef(battleMeta));
            if (result) {
                EXPECT_CALL(*battle, GetResult()).WillRepeatedly(Return(*result));
            }
            AddBattle(id, std::move(battle));
        }

        void AddBattle(const TBattleId& id, THolder<IBattle>&& battle) {
            EXPECT_CALL(*Env.Battles, Find(id)).WillOnce(Return(ByMove(std::move(battle))));
        }

        TTestEnv Env{.Battles = MakeHolder<TBattleStorageMock>()};
        THandler<TTestReportResult> Sut;
        TDeque<IBattle::TBattleData> BattleDatas;
        TDeque<IBattle::TMetadata> BattleMetas;
    };
}

Y_UNIT_TEST_SUITE(TestReportResult) {
    Y_UNIT_TEST_F(TestNotFinished, TFixture) {
        static const auto testParams = TVector<std::tuple<TBattleId, IBattle::EStatus, ui32>>{
            {"0", IBattle::EStatus::IN_QUEUE, 0},
            {"1", IBattle::EStatus::IN_FIGHT, 10}
        };

        for (const auto& [battleId, status, progress] : testParams) {
            AddBattle(battleId, status, progress, {}, Nothing());
        }

        for (const auto& [battleId, status, progress] : testParams) {
            const auto request = TString::Join("id=", battleId);
            const auto response = Sut.Invoke(request.c_str());
            UNIT_ASSERT_EQUAL(response.Code, 200);
            const auto responseJson = NSc::TValue::FromJson(response.Text);

            UNIT_ASSERT_EQUAL(responseJson["status"], ToString(status));
            UNIT_ASSERT_EQUAL(responseJson["progress"], progress);
            UNIT_ASSERT(responseJson["error"].GetString().empty());
        }
    }

    Y_UNIT_TEST_F(TestSuccess, TFixture) {
        TResultPack result;
        auto& degradation = *result.AddDegradation();
        degradation.MutableBase()->SetReportVersion("2021.0.1.0");
        degradation.MutableTest()->SetReportVersion("2021.0.2.0");
        TBattleSpec spec;
        spec.SetFireType(TBattleSpec_EType_MAIN);
        AddBattle("0", IBattle::EStatus::COMPLETE, 100, spec, result);

        const auto response = Sut.Invoke("id=0");
        UNIT_ASSERT_EQUAL(response.Code, 200);
        const auto responseJson = NSc::TValue::FromJson(response.Text);

        UNIT_ASSERT_EQUAL(responseJson["status"], ToString(IBattle::EStatus::COMPLETE));
        UNIT_ASSERT_EQUAL(responseJson["progress"], 100);
        UNIT_ASSERT(responseJson["error"].GetString().empty());
        UNIT_ASSERT_EQUAL(responseJson["release_version"], "2021.0.1.0");
        UNIT_ASSERT_EQUAL(responseJson["test_version"], "2021.0.2.0");
        UNIT_ASSERT_EQUAL(responseJson["fire_type"], "main");
        UNIT_ASSERT(!responseJson.Has("ammo_filter"));
        UNIT_ASSERT_EQUAL(responseJson["is_retry"], false);
        UNIT_ASSERT(!responseJson.Has("degradation_reasons"));
        UNIT_ASSERT_EQUAL(responseJson["success"], true);
        UNIT_ASSERT(!responseJson.Has("next_battle_id"));
    }

    Y_UNIT_TEST_F(TestCanceled, TFixture) {
        TResultPack result;
        result.MutableCancellation()->SetBattleId("0");
        AddBattle("0", IBattle::EStatus::CANCELED, 100, {}, result);
        const auto response = Sut.Invoke("id=0");
        UNIT_ASSERT_EQUAL(response.Code, 200);
        const auto responseJson = NSc::TValue::FromJson(response.Text);

        UNIT_ASSERT_EQUAL(responseJson["status"], ToString(IBattle::EStatus::CANCELED));
        UNIT_ASSERT_EQUAL(responseJson["progress"], 100);
        UNIT_ASSERT(responseJson["error"].GetString().empty());
        UNIT_ASSERT_EQUAL(responseJson["success"], false);
    }

    Y_UNIT_TEST_F(TestDegradated, TFixture) {
        TResultPack result;
        auto& degradation = *result.AddDegradation();
        degradation.MutableBase()->SetReportVersion("2021.0.1.0");
        degradation.MutableTest()->SetReportVersion("2021.0.2.0");
        degradation.AddReason("some degradation reason");
        degradation.SetIsDegradation(true);
        TBattleSpec spec;
        spec.SetFireType(TBattleSpec_EType_MAIN);
        AddBattle("0", IBattle::EStatus::COMPLETE, 100, spec, result);

        const auto response = Sut.Invoke("id=0");
        UNIT_ASSERT_EQUAL(response.Code, 200);
        const auto responseJson = NSc::TValue::FromJson(response.Text);

        UNIT_ASSERT_EQUAL(responseJson["status"], ToString(IBattle::EStatus::COMPLETE));
        UNIT_ASSERT_EQUAL(responseJson["progress"], 100);
        UNIT_ASSERT(responseJson["error"].GetString().empty());
        UNIT_ASSERT_EQUAL(responseJson["release_version"], "2021.0.1.0");
        UNIT_ASSERT_EQUAL(responseJson["test_version"], "2021.0.2.0");
        UNIT_ASSERT_EQUAL(responseJson["fire_type"], "main");
        UNIT_ASSERT_EQUAL(responseJson["is_retry"], false);
        const auto& degradationReasons = responseJson["degradation_reasons"].GetArray();
        UNIT_ASSERT_EQUAL(degradationReasons.size(), 1);
        UNIT_ASSERT_EQUAL(degradationReasons[0].GetString(), "some degradation reason");
        UNIT_ASSERT_EQUAL(responseJson["success"], false);
        UNIT_ASSERT(!responseJson.Has("next_battle_id"));
    }

    Y_UNIT_TEST_F(TestWithError, TFixture) {
        TResultPack result;
        result.MutableError()->SetMessage("some error");
        AddBattle("0", IBattle::EStatus::COMPLETE, 100, {}, result);

        const auto response = Sut.Invoke("id=0");
        UNIT_ASSERT_EQUAL(response.Code, 200);
        const auto responseJson = NSc::TValue::FromJson(response.Text);

        UNIT_ASSERT_EQUAL(responseJson["status"], ToString(IBattle::EStatus::COMPLETE));
        UNIT_ASSERT_EQUAL(responseJson["progress"], 100);
        UNIT_ASSERT_EQUAL(responseJson["error"].GetString(), "some error");
        UNIT_ASSERT_EQUAL(responseJson["success"], false);

    }

    Y_UNIT_TEST_F(TestWithFailedAttacks, TFixture) {
        TResultPack result;
        result.MutableSource()->AddAttack()->SetError("attack error");
        AddBattle("0", IBattle::EStatus::COMPLETE, 100, {}, result);

        const auto response = Sut.Invoke("id=0");
        UNIT_ASSERT_EQUAL(response.Code, 200);
        const auto responseJson = NSc::TValue::FromJson(response.Text);

        UNIT_ASSERT_EQUAL(responseJson["status"], ToString(IBattle::EStatus::COMPLETE));
        UNIT_ASSERT_EQUAL(responseJson["progress"], 100);
        UNIT_ASSERT_EQUAL(responseJson["error"].GetString(), "Атака 0 упала с ошибкой: attack error");
        UNIT_ASSERT_EQUAL(responseJson["success"], false);
    }

    Y_UNIT_TEST_F(TestAmmoFilter, TFixture) {
        TResultPack result;
        auto& degradation = *result.AddDegradation();
        degradation.MutableBase()->SetReportVersion("2021.0.1.0");
        degradation.MutableTest()->SetReportVersion("2021.0.2.0");
        TBattleSpec spec;
        spec.SetFireType(TBattleSpec_EType_MAIN);
        spec.AddAttack()->MutableFire()->MutableAmmo()->SetFilter("prime");
        AddBattle("0", IBattle::EStatus::COMPLETE, 100, spec, result);

        const auto response = Sut.Invoke("id=0");
        UNIT_ASSERT_EQUAL(response.Code, 200);
        const auto responseJson = NSc::TValue::FromJson(response.Text);

        UNIT_ASSERT_EQUAL(responseJson["fire_type"], "main");
        UNIT_ASSERT_EQUAL(responseJson["ammo_filter"], "prime");
    }
}
