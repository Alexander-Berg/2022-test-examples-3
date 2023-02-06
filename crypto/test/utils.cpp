#include "utils.h"

#include <crypta/cm/services/common/data/back_reference.h>
#include <crypta/lib/native/log/loggers/std_logger.h>

#include <util/generic/algorithm.h>

using namespace NCrypta::NCm;
using namespace NCrypta::NCm::NMutator;

TTtlConfig NTest::GetTtlConfig() {
    TTtlConfig result;
    result.SetDefaultTtl(DEFAULT_TTL.Seconds());
    result.SetExtendedTtl(EXTENDED_TTL.Seconds());
    result.SetTouchTimeoutSec(TOUCH_TIMEOUT.Seconds());
    result.SetExtendTtlTimeoutSec(EXTEND_TTL_TIMEOUT.Seconds());
    result.MutableCustomTagTtls()->insert({TAG_CUSTOM, TTL_CUSTOM.Seconds()});

    return result;
}

TMatch NTest::GetDbMatch(const TId& extId, const TInstant touch, const TDuration ttl) {
    return TMatch(extId, {}, touch, ttl);
}

TDbState NTest::GetDbState(const TId& extId, const TId& yuid, TInstant touch, TDuration ttl, const THashSet<TString>& trackedBackRefs) {
    auto dbMatch = GetDbMatch(extId, touch, ttl);
    dbMatch.AddId(TMatchedId(yuid, touch));

    TBackReference backRef(yuid, {extId});

    return TDbState(
        {{dbMatch.GetExtId(), dbMatch}},
        {{backRef.Id, backRef}},
        trackedBackRefs
    );
}

TDbState NTest::GetDbStateWithICookie(const TId& extId, const TId& yuid, const TId& icookie, TInstant touch, TDuration ttl, const THashSet<TString>& trackedBackRefs) {
    auto dbMatch = GetDbMatch(extId, touch, ttl);
    dbMatch.AddId(TMatchedId(yuid, touch));
    dbMatch.AddId(TMatchedId(icookie, touch));

    TBackReference yuidBackRef(yuid, {extId});
    TBackReference icookieBackRef(icookie, {extId});

    return TDbState(
        {{dbMatch.GetExtId(), dbMatch}},
        {{yuidBackRef.Id, yuidBackRef}, {icookieBackRef.Id, icookieBackRef}},
        trackedBackRefs
    );
}

THashSet<TString> NTest::GetSetOfKeys(const TVector<NCrypta::TRecord>& records) {
    THashSet<TString> writtenKeys;
    Transform(records.begin(), records.end(), std::inserter(writtenKeys, writtenKeys.end()), [](const auto& record) {return record.Key; });
    return writtenKeys;
}

void NTest::InitLogs() {
    static auto touch = NCrypta::NLog::NStdLogger::RegisterLog("touch", "stderr", "info");
    static auto upload = NCrypta::NLog::NStdLogger::RegisterLog("upload", "stderr", "info");
}
