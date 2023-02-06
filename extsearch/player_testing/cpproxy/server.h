#pragma once
#include <util/network/sock.h>
#include <util/generic/string.h>
#include <util/generic/ptr.h>
#include <util/stream/output.h>
#include <library/cpp/logger/log.h>
#include <util/generic/noncopyable.h>
#include "stat_counter.h"
#include "network_filter.h"

class IProxyUpstream;

class TProxyServer: public TNonCopyable {
    public:
        TProxyServer(TLog& log, IProxyUpstream* upstream, const TString& bindHost, ui16 bindPort, ui16 caServerPort, ui16 logServerPort, bool plainProxyMode)
            : BindHost(bindHost)
            , BindPort(bindPort)
            , CAServerPort_(caServerPort)
            , LogServerPort_(logServerPort)
            , Log(log)
            , Upstream_(upstream)
            , PlainProxyMode(plainProxyMode)
        {
        }

        void Run();

        bool IsRunning() { return true; }

        TLog& Logger() { return Log; }

        IProxyUpstream* Upstream() { return Upstream_; }

        TStatCounter& StatCounter() { return StatCounter_; }

        ui16 CAServerPort() const { return CAServerPort_; }

        ui16 LogServerPort() const { return LogServerPort_; }

        bool IsPlain() const { return PlainProxyMode; }

    private:
        TString BindHost;
        ui16 BindPort;
        ui16 CAServerPort_;
        ui16 LogServerPort_;
        THolder<ISockAddr> BindAddr;
        THolder<TStreamSocket> ServerSock;
        TLog& Log;
        IProxyUpstream* Upstream_;
        TStatCounter StatCounter_;
        bool PlainProxyMode;
};
