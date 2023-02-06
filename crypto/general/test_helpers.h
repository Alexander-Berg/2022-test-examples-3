#pragma once

#include <library/cpp/tvmauth/client/facade.h>

#include <util/generic/hash.h>
#include <util/generic/string.h>
#include <util/system/types.h>

namespace NCrypta {
    using THashMapHeaders = THashMap<TString, TString>;

    const TString UNITTEST_TVM_APP_SECRET = "bAicxJVa5uVY7MjDlapthw";

    NTvmAuth::TTvmClient CreateRecipeTvmClient(ui32 selfTvmId, const TString& secret);

    THashMapHeaders GetTestTvmServiceHeaders(const TString& serviceTicket);
    THashMapHeaders GetTestTvmUserHeaders(const TString& userTicket);
}
