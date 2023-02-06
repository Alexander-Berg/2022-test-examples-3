#include "network_filter.h"
#include "server.h"
#include "ssl_wrapper.h"
#include "upstream.h"
#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/logger/log.h>
#include <library/cpp/logger/stream.h>
#include <util/system/thread.h>


int main(int argc, const char* argv[]) {
    TString bindHost;
    TString sslCertDir;
    ui16 bindPort;
    ui16 caServerPort;
    ui16 logServerPort;
    TString upstreamProxy;
    bool plainProxyMode = false;
    NLastGetopt::TOpts opts = NLastGetopt::TOpts::Default();
    opts.AddLongOption('h', "host", "Bind host")
        .RequiredArgument("string")
        .DefaultValue("localhost")
        .Optional()
        .StoreResult(&bindHost);
    opts.AddLongOption('p', "port", "Bind port")
        .RequiredArgument("number")
        .Optional()
        .DefaultValue(8080)
        .StoreResult(&bindPort);
    opts.AddLongOption('u', "upstream-proxy", "Upstream proxy")
        .RequiredArgument("host:port")
        .DefaultValue("")
        .Optional()
        .StoreResult(&upstreamProxy);
    opts.AddLongOption('c', "ca-port", "CA server port")
        .RequiredArgument("number")
        .Optional()
        .DefaultValue(8081)
        .StoreResult(&caServerPort);
    opts.AddLongOption('l', "log-port", "log server port")
        .RequiredArgument("number")
        .Optional()
        .DefaultValue(8082)
        .StoreResult(&logServerPort);
    opts.AddLongOption('n', "plain", "Enable plain CONNECT proxy")
        .Optional()
        .StoreTrue(&plainProxyMode);
    opts.AddLongOption('d', "ssl-cert-dir", "SSL trusted certificates directory")
        .RequiredArgument("string")
        .DefaultValue("/etc/ssl/certs")
        .Optional()
        .StoreResult(&sslCertDir);
    NLastGetopt::TOptsParseResult(&opts, argc, argv);
    if (plainProxyMode) {
        logServerPort = 0;
        caServerPort = 0;
    }
    TLog logger(MakeHolder<TStreamLogBackend>(&Cout));
    logger.SetDefaultPriority(ELogPriority::TLOG_INFO);
    logger.SetFormatter([](ELogPriority priority, TStringBuf message) -> TString {
        return TStringBuilder()
            << TInstant::Now().ToStringLocal()
            << " [" << TThread::CurrentThreadId() << "]"
            << " [" << priority << "] " << message << Endl;
    });
    logger << "loading SSL certificates from " << sslCertDir;
    TSSLConnectionWrapper::Init(sslCertDir);
    THbfFilter netFilter(logger);
    logger << "initializing network filter";
    netFilter.Init();
    THolder<IProxyUpstream> upstream;
    if (upstreamProxy) {
        auto delim = upstreamProxy.find(":");
        if (delim == TString::npos) {
            logger << "invalid --upstream-proxy";
            return 1;
        }
        TString upstreamHost = upstreamProxy.substr(0, delim);
        ui16 upstreamPort = FromString<ui16>(upstreamProxy.substr(delim + 1));
        upstream.Reset(new THttpProxyUpstream(logger, netFilter, upstreamHost, upstreamPort));
    } else {
        logger << "using direct internet connection";
        upstream.Reset(new TDirectConnection(logger, netFilter));
    }
    THolder<TProxyServer> server(new TProxyServer(logger, upstream.Get(), bindHost, bindPort, caServerPort, logServerPort, plainProxyMode));
    try {
        server->Run();
    } catch(const std::exception& ex) {
        logger << "server is dead: " << ex.what();
    }
    return 0;
}
