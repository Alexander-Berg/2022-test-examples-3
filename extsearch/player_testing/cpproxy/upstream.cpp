#include "upstream.h"
#include "network_filter.h"
#include <util/network/socket.h>
#include <util/network/poller.h>
#include <library/cpp/logger/log.h>
#include <library/cpp/http/io/stream.h>
#include <library/cpp/http/misc/httpcodes.h>

static void TranslateIPv4Addr(TSockAddrInet6& dst, in_addr ai, ui16 port) {
    sockaddr_in6 addr6;
    memset(&addr6, 0, sizeof(addr6));
    addr6.sin6_family = AF_INET6;
    addr6.sin6_port = port;
    addr6.sin6_addr.s6_addr[1] = 0x64;
    addr6.sin6_addr.s6_addr[2] = 0xff;
    addr6.sin6_addr.s6_addr[3] = 0x9b;
    memcpy(&addr6.sin6_addr.s6_addr[12], &ai.s_addr, 4);
    memcpy(dst.SockAddr(), &addr6, dst.Size());
}

static TSockAddrInet6 Resolve(const TString& host, ui16 port) {
    TSockAddrInet6 result;
    THolder<TNetworkAddress> addr;
    try {
        addr.Reset(new TNetworkAddress(host, port));
    } catch(const std::exception& ex) {
        in_addr ai;
        if (inet_aton(host.data(), &ai)) {
            TranslateIPv4Addr(result, ai, htons(port));
            return result;
        } else {
            throw;
        }
    }
    bool hasIPv4 = false;
    for (auto it = addr->Begin(); it != addr->End(); ++it) {
        if (it->ai_family == AF_INET6) {
            memcpy(result.SockAddr(), it->ai_addr, result.Size());
            return result;
        } else if(it->ai_family == AF_INET) {
            const sockaddr_in& addr4 = *reinterpret_cast<const sockaddr_in*>(it->ai_addr);
            TranslateIPv4Addr(result, addr4.sin_addr, addr4.sin_port);
            hasIPv4 = true;
        }
    }
    if (hasIPv4) {
        return result;
    }
    ythrow yexception() << "cannot resolve " << host << ":" << port;
}

bool THttpProxyUpstream::Connect(const TString& host, ui16 port) {
    ProxyAddr = Resolve(ProxyHost.data(), ProxyPort);
    if (Socket.Connect(&ProxyAddr)) {
        Logger << "connection to proxy " << ProxyAddr.ToString() << " failed";
        return false;
    }
    Logger << "connected to proxy " << ProxyAddr.ToString();
    TStreamSocketOutput socketOutput(&Socket);
    THttpOutput httpOutput(&socketOutput);
    httpOutput << "CONNECT " << host << ":" << port << " HTTP/1.1\r\n\r\n";
    httpOutput.Flush();
    TStreamSocketInput socketInput(&Socket);
    THttpInput httpInput(&socketInput);
    auto retCode = ParseHttpRetCode(httpInput.FirstLine());
    if (retCode != HttpCodes::HTTP_OK) {
        Logger << "proxy " << ProxyAddr.ToString() << " CONNECT " << host << ":" << port << " failed: " << retCode;
        return false;
    }
    return true;
}

TProxyUpstreamPtr THttpProxyUpstream::Clone() const {
    return THolder<IProxyUpstream>(new THttpProxyUpstream(Logger, HbfFilter, ProxyHost, ProxyPort));
}

bool TDirectConnection::Connect(const TString& host, ui16 port) {
    RemoteAddr = Resolve(host.data(), port);
    TString logDesc = TStringBuilder() << host << " - " << RemoteAddr.ToString();
    if (!HbfFilter.IsAllowed(host, TIpv6Address(RemoteAddr))) {
        Logger << "connection to " << logDesc  << " denied by network filter";
        return false;
    }
    if (Socket.Connect(&RemoteAddr)) {
        Logger << "connection to " << logDesc << " failed";
        return false;
    }
    Logger << "connected to " << logDesc;
    return true;
}

TProxyUpstreamPtr TDirectConnection::Clone() const {
    return THolder<IProxyUpstream>(new TDirectConnection(Logger, HbfFilter));
}
