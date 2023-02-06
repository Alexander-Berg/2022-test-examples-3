#pragma once
#include <util/generic/set.h>
#include <util/generic/string.h>
#include <util/generic/vector.h>
#include <library/cpp/ipv6_address/ipv6_address.h>
#include <library/cpp/logger/log.h>
#include <tuple>

class THbfFilter {
    public:
        THbfFilter(TLog& logger)
            : Logger(logger)
            {}

        void Init();
        bool IsAllowed(const TString& host, const TIpv6Address& ip) const;

    private:
        typedef TVector<std::tuple<TIpv6Address, TIpv6Address, bool>> TFilterList;
        TFilterList FilterList;
        TSet<TString> AllowedHosts;
        TLog& Logger;

    private:
        void LoadMacro(const TString& macro, bool isAllowed, TFilterList& result) const;
        void AllowHost(const TString& host);
};
