#pragma once
#include <util/network/sock.h>
#include <util/thread/factory.h>
#include <library/cpp/http/io/headers.h>
#include <library/cpp/http/misc/parsed_request.h>

class TProxyServer;

class TProxyConnection: public IThreadFactory::IThreadAble {
    public:
        TProxyConnection(TProxyServer* server)
            : Server(server)
            , ClientSock(INVALID_SOCKET)
            {}

        TStreamSocket* GetSocket() { return &ClientSock; }
        ISockAddr* GetAddress() { return &ClientAddr; }

    private:
        TProxyServer* Server;
        TInet6StreamSocket ClientSock;
        TSockAddrInet6 ClientAddr;

        void DoExecute() override {
            try {
                ServeRequest();
            } catch(...) {
            }
            delete this;
        }

        void ServeRequest();

        void DoHttpGet(const TParsedHttpRequest& params, const THttpHeaders& headers);
        void DoHttpConnect(const TParsedHttpRequest& params);

        bool HandleAPI(const TParsedHttpRequest& params);
};
