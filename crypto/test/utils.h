#pragma once

#include <crypta/cm/services/common/data/id.h>
#include <crypta/cm/services/common/data/match.h>
#include <crypta/cm/services/common/db_state/db_state.h>
#include <crypta/cm/services/mutator/lib/config/ttl_config.pb.h>

#include <util/datetime/base.h>
#include <util/generic/hash.h>
#include <util/generic/string.h>

namespace NCrypta::NCm::NMutator::NTest {
    using namespace NCrypta::NCm;

    const auto TAG_CUSTOM = "custom";
    const auto DEFAULT_TTL = TDuration::Seconds(10 * 86400);
    const auto EXTENDED_TTL = TDuration::Seconds(30 * 86400);
    const auto TOUCH_TIMEOUT = TDuration::Seconds(100);
    const auto EXTEND_TTL_TIMEOUT = TDuration::Seconds(200);
    const auto TTL_CUSTOM = TDuration::Seconds(100);
    const THashSet<TString> TRACKED_BACK_REFERENCES({"ext"});
    const auto TOUCH_TS = TInstant::Seconds(1000);

    TTtlConfig GetTtlConfig();
    TMatch GetDbMatch(const TId& extId, const TInstant touch, const TDuration ttl);
    TDbState GetDbState(const TId& extId, const TId& yuid, TInstant touch, TDuration ttl, const THashSet<TString>& trackedBackRefs);
    TDbState GetDbStateWithICookie(const TId& extId, const TId& yuid, const TId& icookie, TInstant touch, TDuration ttl, const THashSet<TString>& trackedBackRefs);
    THashSet<TString> GetSetOfKeys(const TVector<NCrypta::TRecord>& records);

    void InitLogs();
}
