#pragma once

#include <library/cpp/tvmauth/type.h>

#include <util/generic/string.h>
#include <util/system/types.h>

namespace NCrypta::NTvmTest {
    constexpr ui64 SELF_TVM_ID = 1000501u;
    constexpr ui64 CLIENT_TVM_ID = 1000502u;
    const TString VALID_SERVICE_TICKET = "3:serv:CBAQ__________9_IggItog9ELWIPQ:GKN3stGwDbEnY3qNBlvVIf8hkUPDqh78mmInR-J-wMIJpOKLxDHXlqdMKX59UuiuDxIgMns_peJiBQ0bzRqjyt349ZIA9MQB7LSEzCfxYR5Pu825hDRzzBDSMJ91cKtt2VjlViKoAA9p05tcsyB5trzw9omQq9cns77UnoALPyw";
    const TString INVALID_SERVICE_TICKET = "3:serv:CBAQ__________9_I";

    constexpr ui64 DEFAULT_UID = 789;
    const NTvmAuth::TUids ALL_UIDS = {123, 456, DEFAULT_UID};
    const TString VALID_USER_TICKET = "3:user:CAwQ__________9_GhkKAgh7CgMIyAMKAwiVBhCVBiDShdjMBCgC:PcLpaBsnuIVQL9PcYC529hV2S47BOUUv6cBkGNZYgwp9XOB8KEfNZZDwaX4DkVS0mDwyYidu6kpZNTrmdUfI4hmWfwkWmCt_oYxaRBMyAwG31pngQnp9si73LqZ_62siH4KQIYGeT12aHmABKPoYmkEjdsVjeK8EGqe7tZTo1Yk";
    const TString INVALID_USER_TICKET = "3:user:CAwQ__________9";
}