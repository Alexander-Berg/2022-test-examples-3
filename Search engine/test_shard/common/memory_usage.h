#pragma once

#include <library/cpp/logger/global/global.h>

#include <util/generic/string.h>
#include <util/system/mem_info.h>

class TMemoryUsage {
public:
    TMemoryUsage(const TString& name = "")
        : Name_(name)
        , Usage_(NMemInfo::GetMemInfo().RSS)
    {}

    ~TMemoryUsage() noexcept {
        ui64 delta = NMemInfo::GetMemInfo().RSS - Usage_;
        double gib = delta / (1024. * 1024. * 1024.);
        INFO_LOG << " [ " << Name_ << " ] memory usage: " << gib << " GiB" << Endl;
    }

private:
    TString Name_;
    ui64 Usage_;
};
