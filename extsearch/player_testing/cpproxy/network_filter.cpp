#include "network_filter.h"
#include <library/cpp/http/simple/http_client.h>
#include <library/cpp/ipv6_address/ipv6_address.h>
#include <library/cpp/json/json_reader.h>
#include <util/string/vector.h>

static TIpv6Address GetLastIP(const TIpv6Address& ip, ui8 mask) {
    ui128 addr = ip;
    size_t bits = (ip.Type() == TIpv6Address::Ipv6 ? 128 - mask : 32 - mask);
    for (size_t i = 0; i < bits; ++i) {
        addr |= (ui128)1 << i;
    }
    return TIpv6Address(addr, ip.Type());
}

void THbfFilter::LoadMacro(const TString& macro, bool isAllowed, TFilterList& filterList) const {
    TStringStream dataStream;
    TSimpleHttpClient httpClient("https://hbf.yandex.net", 443);
    TString path = TStringBuilder() << "/macros/" << macro << "?format=json";
    httpClient.DoGet(path, &dataStream);
    NJson::TJsonValue json;
    NJson::ReadJsonTree(&dataStream, &json, true);
    for (auto item: json.GetArray()) {
        auto net = SplitString(item.GetString(), "/");
        bool valid = false;
        auto fromIp = TIpv6Address::FromString(net[0], valid);
        if (!valid) {
            continue;
        }
        auto toIp = GetLastIP(fromIp, FromString<ui8>(net[1]));
        Logger << fromIp.ToString() << " " << toIp.ToString() << " " << net[1] << " " << (isAllowed ? "allowed" : "denied");
        filterList.push_back(std::make_tuple(fromIp, toIp, isAllowed));
    }
}

void THbfFilter::AllowHost(const TString& host) {
    AllowedHosts.insert(host);
}

void THbfFilter::Init() {
    TFilterList filterList;
    LoadMacro("_SLBPUBLICSUPERNETS_", true, filterList);
    LoadMacro("_YANDEXNETS_", false, filterList);
    AllowHost("quasar-proxy.test.yandex.net");
    FilterList.swap(filterList);
}

bool THbfFilter::IsAllowed(const TString& host, const TIpv6Address& ip) const {
    if (AllowedHosts.contains(host)) {
        return true;
    }
    auto req = ip.Type() == TIpv6Address::Ipv6 && ip.Isv4MappedTov6() ? ip.TryToExtractIpv4From6() : ip;
    for (const auto& item: FilterList) {
        const auto& from = std::get<0>(item);
        if (from.Type() == req.Type()) {
            const auto& to = std::get<1>(item);
            if (from <= req && req <= to) {
                return std::get<2>(item);
            }
        }
    }
    return true;
}
