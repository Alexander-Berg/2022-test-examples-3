#include <crypta/lib/native/exp_system/exp_system.h>

#include <ads/bsyeti/libs/experiments/plugins/bigb/plugin.h>
#include <ads/bsyeti/libs/experiments/plugins/eagle/plugin.h>
#include <crypta/lib/native/time/shifted_clock.h>
#include <library/cpp/resource/resource.h>
#include <library/cpp/testing/unittest/registar.h>
#include <util/datetime/base.h>

using namespace NCrypta::NExpSystem;

namespace {
    ui64 y76{76};
    ui64 y94{94};
    const time_t checkTime{1621947000};

    TExperimentSystemManager CreateTestManager() {
        const TString abResourceName{"ab.json"};
        const TString expResourceName{"exp.json"};

        auto testFetcher = MakeAtomicShared<TTestFetcher>();
        testFetcher->SetAbData(NResource::Find(abResourceName));
        testFetcher->SetExpData(NResource::Find(expResourceName));

        return TExperimentSystemManager({}, testFetcher);
    }
}

Y_UNIT_TEST_SUITE(TExperimentSystemManager) {
    Y_UNIT_TEST(GetParametersViaExpSystem) {
        auto manager = CreateTestManager();
        auto expSystem = manager.GetExperimentSystem();

        auto params76 = expSystem->GetParameters(y76, checkTime);
        auto params94 = expSystem->GetParameters(y94, checkTime);

        UNIT_ASSERT_EQUAL(params76.As<NBSYeti::NExperimentParameters::TBigbPlugin>()->GetCrypta().GetCryptaRealtimeMatching().GetSocketsVulture(), true);
        UNIT_ASSERT_EQUAL(params94.As<NBSYeti::NExperimentParameters::TBigbPlugin>()->GetCrypta().GetCryptaRealtimeMatching().GetSocketsVulture(), false);

        UNIT_ASSERT_EQUAL(params76.As<NBSYeti::NExperimentParameters::TEaglePlugin>()->GetEagleSettings().GetLoadSettings().GetUseExperimentalCrypta(), true);
        UNIT_ASSERT_EQUAL(params94.As<NBSYeti::NExperimentParameters::TEaglePlugin>()->GetEagleSettings().GetLoadSettings().GetUseExperimentalCrypta(), false);
    }

    Y_UNIT_TEST(GetParametersViaManager) {
        TShiftedClock::FreezeTimestamp(checkTime);
        auto manager = CreateTestManager();

        UNIT_ASSERT_EQUAL(manager.GetBigbParameters(y76).GetCrypta().GetCryptaRealtimeMatching().GetSocketsVulture(), true);
        UNIT_ASSERT_EQUAL(manager.GetBigbParameters(y94).GetCrypta().GetCryptaRealtimeMatching().GetSocketsVulture(), false);

        UNIT_ASSERT_EQUAL(manager.GetEagleParameters(y76).GetEagleSettings().GetLoadSettings().GetUseExperimentalCrypta(), true);
        UNIT_ASSERT_EQUAL(manager.GetEagleParameters(y94).GetEagleSettings().GetLoadSettings().GetUseExperimentalCrypta(), false);
    }

    Y_UNIT_TEST(GetParametersViaManagerByUserids) {
        TShiftedClock::FreezeTimestamp(checkTime);
        auto manager = CreateTestManager();

        UNIT_ASSERT_EQUAL(manager.GetBigbParameters(NExperiments::TUserIds{}.SetDuid(y76)).GetCrypta().GetCryptaRealtimeMatching().GetSocketsVulture(), true);
        UNIT_ASSERT_EQUAL(manager.GetBigbParameters(NExperiments::TUserIds{}.SetDuid(y94)).GetCrypta().GetCryptaRealtimeMatching().GetSocketsVulture(), false);

        UNIT_ASSERT_EQUAL(manager.GetEagleParameters(NExperiments::TUserIds{}.SetDuid(y76)).GetEagleSettings().GetLoadSettings().GetUseExperimentalCrypta(), true);
        UNIT_ASSERT_EQUAL(manager.GetEagleParameters(NExperiments::TUserIds{}.SetDuid(y94)).GetEagleSettings().GetLoadSettings().GetUseExperimentalCrypta(), false);
    }
}
