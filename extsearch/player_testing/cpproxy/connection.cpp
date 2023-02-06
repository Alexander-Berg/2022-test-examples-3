#include "connection.h"
#include "server.h"
#include "ssl_wrapper.h"
#include "cert_authority.h"
#include "http_sniffer.h"
#include "upstream.h"
#include <util/network/socket.h>
#include <util/network/poller.h>
#include <util/string/builder.h>
#include <util/string/cast.h>
#include <util/string/vector.h>
#include <library/cpp/http/io/stream.h>
#include <library/cpp/http/client/client.h>
#include <library/cpp/http/misc/parsed_request.h>
#include <library/cpp/http/misc/http_headers.h>
#include <library/cpp/http/server/response.h>
#include <library/cpp/http/push_parser/http_parser.h>
#include <library/cpp/json/json_writer.h>
#include <library/cpp/uri/http_url.h>
#include <library/cpp/string_utils/url/url.h>
#include <extsearch/video/kernel/protobuf/writer.h>
#include <extsearch/video/robot/crawling/player_testing/protos/job.pb.h>


static void SendHttpResponse(TProxyServer* server,
                             const TParsedHttpRequest& requestParams,
                             TStreamSocket& clientSock,
                             HttpCodes responseCode,
                             const TString& contentType = "",
                             const TString& content = "") {
    server->Logger() << ToString(requestParams.Method) << " "
                     << ToString(requestParams.Request) << " "
                     << (int)responseCode;
    TStreamSocketOutput socketOutput(&clientSock);
    THttpOutput httpOutput(&socketOutput);
    THttpResponse httpResponse(responseCode);
    if (contentType && content) {
        httpResponse.SetContentType(contentType);
        httpResponse.SetContent(content);
    }
    httpResponse.OutTo(httpOutput);
    httpOutput.Flush();
}


bool TProxyConnection::HandleAPI(const TParsedHttpRequest& params) {
    if (params.Request == "/ping") {
        SendHttpResponse(Server, params, ClientSock, HttpCodes::HTTP_OK);
    } else if(params.Request == "/stat/reset") {
        TStatData stat;
        Server->StatCounter().Reset(stat);
        NSnail::THttpFeatures pb;
        pb.SetBytesReceived(stat.BytesReceived);
        pb.SetBytesSent(stat.BytesSent);
        SendHttpResponse(Server, params, ClientSock, HttpCodes::HTTP_OK, "application/protobuf", NVideo::TProtoWriter::ToStringBinary(pb));
    } else {
        return false;
    }
    return true;
}

void TProxyConnection::DoHttpGet(const TParsedHttpRequest& params, const THttpHeaders& headers) {
    if (HandleAPI(params)) {
        return;
    } else if (!GetSchemePrefixSize(params.Request)) {
        SendHttpResponse(Server, params, ClientSock, HttpCodes::HTTP_BAD_REQUEST);
        return;
    }

    THttpURL url;
    auto urlParseResult = url.Parse(params.Request);
    if (urlParseResult != THttpURL::ParsedOK || !url.Get(THttpURL::FieldHost)) {
        SendHttpResponse(Server, params, ClientSock, HttpCodes::HTTP_BAD_REQUEST);
        return;
    }
    TString upstreamHost = url.Get(THttpURL::FieldHost);
    auto portData = url.Get(THttpURL::FieldPort);
    ui16 upstreamPort = (portData != nullptr ? FromString<ui16>(portData) : 80);
    auto upstream = Server->Upstream()->Clone();
    if (!upstream->Connect(upstreamHost, upstreamPort)) {
        SendHttpResponse(Server, params, ClientSock, HttpCodes::HTTP_BAD_GATEWAY);
        return;
    }
    auto& upstreamSock = upstream->GetSocket();
    THttpHeaders requestHeaders;
    for (auto it = headers.begin(); it != headers.end(); ++it) {
        auto lcName = to_lower(it->Name());
        if (lcName != to_lower(ToString(NHttpHeaders::CONNECTION))) {
            requestHeaders.AddHeader(it->Name(), it->Value());
        }
    }
    requestHeaders.AddHeader(ToString(NHttpHeaders::CONNECTION), "close");
    requestHeaders.AddHeader(ToString(NHttpHeaders::HOST), upstreamHost);

    TString httpRequest = TStringBuilder() << "GET " << url.PrintS(THttpURL::FlagPath | THttpURL::FlagQuery) << " HTTP/1.1\r\n";
    TStreamSocketOutput socketOutput(&upstreamSock);
    THttpOutput httpOutput(&socketOutput);
    httpOutput << httpRequest;
    requestHeaders.OutTo(&httpOutput);
    httpOutput << "\r\n";
    httpOutput.Flush();

    THttpSniffer httpSniffer(
        Server->Logger(),
        &Server->StatCounter(),
        Server->LogServerPort(),
        url.PrintS(THttpURL::FlagScheme | THttpURL::FlagHostPort),
        url.PrintS(THttpURL::FlagPath | THttpURL::FlagQuery)
    );
    char buf[8192];
    TSocketPoller poller;
    const size_t maxSockCount = 2;
    void* events[maxSockCount];
    poller.WaitRead((SOCKET)upstreamSock, &upstreamSock);
    poller.WaitWrite((SOCKET)ClientSock, &ClientSock);
    while (Server->IsRunning()) {
        auto sockCount = poller.WaitT(events, maxSockCount, TDuration::MilliSeconds(50));
        if (!sockCount) {
            continue;
        } else if (sockCount == 2) {
            auto bytes = upstreamSock.Recv(buf, sizeof(buf));
            if (bytes <= 0) {
                break;
            }
            httpSniffer.OnReply(buf, bytes);
            ClientSock.Send(buf, bytes);
        } else {
            Sleep(TDuration::MilliSeconds(50));
        }
    }
}


static void ProxyTunnelPlain(TProxyServer* server, TStreamSocket& downstream, TStreamSocket& upstream, ui64 timeout) {
    TSocketPoller poller;
    void * SC_D = (void *)0;
    void * SC_U = (void *)1;
    poller.WaitRead((SOCKET)downstream, SC_D);
    poller.WaitRead((SOCKET)upstream, SC_U);
    char buf[65536];
    const size_t maxSockCount = 2;
    ui64 lastActive = TInstant::Now().Seconds();
    while (server->IsRunning() && lastActive + timeout >= TInstant::Now().Seconds()) {
        void* cookie[maxSockCount];
        size_t sockCount = sockCount = poller.WaitT(cookie, maxSockCount, TDuration::MilliSeconds(50));
        for (size_t i = 0; i < sockCount; ++i) {
            auto& inSock = (cookie[i] == SC_D) ? downstream  : upstream;
            auto& outSock = (cookie[i] == SC_D) ? upstream : downstream;
            auto ssize = inSock.Recv(buf, sizeof(buf));
            if (ssize <= 0) {
                return;
            }
            if (outSock.Send(buf, ssize) < 0) {
                return;
            }
            lastActive = TInstant::Now().Seconds();
        }
    }
}

static void ProxyTunnel(TProxyServer* server, const TString& host, TStreamSocket& downstream, TStreamSocket& upstream, ui64 timeout, bool forceHttp = false) {
    TString hostCert, hostPrivkey;
    TCertificationAuthority ca(server->CAServerPort());
    ca.SignCertificate(host, hostCert, hostPrivkey);
    auto& logger = server->Logger();
    TSSLConnectionWrapper sslUpstream(logger), sslDownstream(logger);
    if (!forceHttp) {
        sslUpstream.ClientInit(host);
    } else {
        logger << "Fake SSL init, host: " << host;
        sslUpstream.FakeInit(&upstream);
    }
    sslDownstream.ServerInit(hostCert, hostPrivkey);
    try {
        sslDownstream.Accept((SOCKET)downstream);
        sslUpstream.Connect((SOCKET)upstream);
    } catch (const std::exception& ex) {
        logger << "SSL setup for " << host << " FAILED: " << ex.what();
        return;
    }
    logger << "SSL setup for " << host << " OK";

    THttpSniffer httpSniffer(
        server->Logger(),
        &server->StatCounter(),
        server->LogServerPort(),
        TStringBuilder() << "https://" << host
    );

    TSocketPoller poller;
    void * SC_D = (void *)0;
    void * SC_U = (void *)1;
    poller.WaitRead((SOCKET)downstream, SC_D);
    poller.WaitRead((SOCKET)upstream, SC_U);
    char buf[8192];
    size_t maxSockCount = 2;
    bool hasReq = false;
    ui64 lastActive = TInstant::Now().Seconds();
    while (server->IsRunning() && lastActive + timeout >= TInstant::Now().Seconds()) {
        void* cookie[maxSockCount];
        size_t sockCount = 0;
        if (!hasReq) {
            sockCount = 1;
            cookie[0] = SC_D;
        } else {
            sockCount = poller.WaitT(cookie, maxSockCount, TDuration::MilliSeconds(50));
            if (!sockCount) {
                if (sslDownstream.HasIncomingData()) {
                    sockCount += 1;
                    cookie[sockCount] = SC_D;
                }
                if (sslUpstream.HasIncomingData()) {
                    sockCount += 1;
                    cookie[sockCount] = SC_U;
                }
            }
        }
        for (size_t i = 0; i < sockCount; ++i) {
            bool retry = true;
            while (retry) {
                auto& inSock = (cookie[i] == SC_D) ? sslDownstream  : sslUpstream;
                auto& outSock = (cookie[i] == SC_D) ? sslUpstream : sslDownstream;
                auto ssize = inSock.Recv(buf, sizeof(buf));
                if (ssize <= 0) {
                    return;
                }
                if (outSock.Send(buf, ssize) < 0) {
                    return;
                }
                retry = inSock.HasIncomingData();
                if (cookie[i] == SC_D) {
                    hasReq = true;
                    if (forceHttp) {
                        logger << "Request data: " << TString(buf, ssize);
                    }
                    httpSniffer.OnRequest(buf, ssize);
                } else {
                    httpSniffer.OnReply(buf, ssize);
                }
                lastActive = TInstant::Now().Seconds();
            }
        }
    }
}

static bool IsForcedHttp(const TString& host) {
    return host == "dlp-12.24v.tv";
}

void TProxyConnection::DoHttpConnect(const TParsedHttpRequest& params) {
    TVector<TString> dst = SplitString(ToString(params.Request), ":");
    if (dst.size() != 2) {
        SendHttpResponse(Server, params, ClientSock, HttpCodes::HTTP_BAD_REQUEST);
        return;
    }
    TString dstHost = dst[0];
    ui16 dstPort = FromString<ui16>(dst[1]);
    if (dstPort == 0 || dstPort > 65535) {
        SendHttpResponse(Server, params, ClientSock, HttpCodes::HTTP_BAD_REQUEST);
        return;
    }
    bool forceHttp = IsForcedHttp(dstHost);
    if (forceHttp) {
        dstPort = 80;
    }
    auto upstream = Server->Upstream()->Clone();
    if (!upstream->Connect(dstHost, dstPort)) {
        SendHttpResponse(Server, params, ClientSock, HttpCodes::HTTP_BAD_GATEWAY);
        return;
    }
    auto& dstSocket = upstream->GetSocket();
    SetNoDelay(dstSocket, true);
    SendHttpResponse(Server, params, ClientSock, HttpCodes::HTTP_OK);
    if (Server->IsPlain()) {
        ProxyTunnelPlain(Server, ClientSock, dstSocket, 120);
    } else {
        ProxyTunnel(Server, dstHost, ClientSock, dstSocket, 120, forceHttp);
    }
    Server->Logger() << TLOG_DEBUG << "proxy tunnel closed";
}

void TProxyConnection::ServeRequest() {
    try {
        SetNoDelay(ClientSock, true);
        TStreamSocketInput socketInput(&ClientSock);
        THttpInput httpInput(&socketInput);
        TParsedHttpRequest request(httpInput.FirstLine());
        if (request.Method == "GET") {
            DoHttpGet(request, httpInput.Headers());
        } else if(request.Method == "CONNECT") {
            DoHttpConnect(request);
        }
    } catch(const std::exception& ex) {
        Server->Logger() << ClientAddr.ToString() << ": fatal: " << ex.what();
    }
}
