#pragma once
#include <util/generic/ptr.h>
#include <util/network/sock.h>
#include <util/network/socket.h>
#include <contrib/libs/openssl/include/openssl/ssl.h>
#include <contrib/libs/openssl/include/openssl/err.h>
#include <library/cpp/logger/log.h>


template<class T, void (*DestroyFunc)(T*)>
class TDestroySSL {
    public:
        static void Destroy(T* ptr) { DestroyFunc(ptr); }
};

class TSSLConnectionWrapper: TNonCopyable {
    public:
        TSSLConnectionWrapper(TLog& logger);
        ~TSSLConnectionWrapper();
        static void Init(const TString& caCertDir);
        /* SSL client mode */
        void ClientInit(const TString& sniHost = "");
        /* SSL server mode */
        void ServerInit(const TString& cert, const TString& privkey);
        /* Fake mode: just wrap plan TCP sock */
        void FakeInit(TStreamSocket* stream);
        void Connect(SOCKET fd);
        void Accept(SOCKET fd);
        ssize_t Recv(void* data, size_t size);
        ssize_t Send(const void* data, size_t size);
        bool HasIncomingData();
        void Close();

    private:
        TLog& Logger;
        bool IsClient;
        bool NoShutdown;
        ui64 BytesReceived;
        ui64 BytesSent;
        TString CertData;
        TString PrivkeyData;
        static SSL_CTX* ClientContext;
        static SSL_CTX* ServerContext;
        THolder<SSL, TDestroySSL<SSL, SSL_free>> Connection;
        TStreamSocket* RawStream;
        TString Host;
};

