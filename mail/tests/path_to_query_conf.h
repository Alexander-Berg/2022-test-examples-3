#pragma once

#include <library/cpp/testing/unittest/env.h>
#include <string>


namespace macs {
inline std::string pathToQueryConf() {
    return std::string(ArcadiaSourceRoot().c_str()) + "/mail/macs_pg/etc/query.conf";
}
}
