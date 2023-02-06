#pragma once
#include <util/generic/string.h>
#include <util/generic/ptr.h>
#include <util/network/sock.h>

class TLog;
class THbfFilter;
class IProxyUpstream;

typedef THolder<IProxyUpstream> TProxyUpstreamPtr;

class IProxyUpstream {
    public:
        virtual bool Connect(const TString& host, ui16 port) = 0;
        virtual TProxyUpstreamPtr Clone() const = 0;
        virtual TInet6StreamSocket& GetSocket() = 0;
        virtual ~IProxyUpstream() {}
};

class THttpProxyUpstream: public IProxyUpstream {
    public:
        THttpProxyUpstream(TLog& logger, THbfFilter& hbfFilter, const TString& proxyHost, ui16 proxyPort)
            : Logger(logger)
            , HbfFilter(hbfFilter)
            , ProxyHost(proxyHost)
            , ProxyPort(proxyPort)
            {}

        bool Connect(const TString& host, ui16 port) override;

        TProxyUpstreamPtr Clone() const override;

        TInet6StreamSocket& GetSocket() override { return Socket; }
    private:
        TLog& Logger;
        THbfFilter& HbfFilter;
        TString ProxyHost;
        ui16 ProxyPort;
        TSockAddrInet6 ProxyAddr;
        TInet6StreamSocket Socket;
};

class TDirectConnection: public IProxyUpstream {
    public:
        TDirectConnection(TLog& logger, THbfFilter& hbfFilter)
            : Logger(logger)
            , HbfFilter(hbfFilter)
            {}

        bool Connect(const TString& host, ui16 port) override;

        TProxyUpstreamPtr Clone() const override;

        TInet6StreamSocket& GetSocket() override { return Socket; }

    private:
        TLog& Logger;
        THbfFilter& HbfFilter;
        TInet6StreamSocket Socket;
        TSockAddrInet6 RemoteAddr;
};

