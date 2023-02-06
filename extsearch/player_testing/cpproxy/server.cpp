#include "server.h"
#include "connection.h"
#include <util/thread/factory.h>

void TProxyServer::Run() {
    ServerSock.Reset(new TInet6StreamSocket());
    SetReusePort(*ServerSock, true);
    BindAddr.Reset(new TSockAddrInet6(BindHost.c_str(), BindPort));
    if (ServerSock->Bind(BindAddr.Get())) {
        ythrow yexception() << "bind failed";
    }
    if (ServerSock->Listen(SOMAXCONN)) {
        ythrow yexception() << "listen failed";
    }
    Log << "listening " << BindAddr->ToString();
    for(;;) {
        THolder<TProxyConnection> conn(new TProxyConnection(this));
        if(ServerSock->Accept(conn->GetSocket(), conn->GetAddress())) {
            continue;
        }
        SystemThreadFactory()->Run(conn.Release());
    }
}
