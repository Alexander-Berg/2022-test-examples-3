#include "test_server_fixture.h"

#include <grpc++/create_channel.h>

#include <util/string/cast.h>

using namespace NCrypta::NGrpc;

void TestServerFixture::SetUp()
{
    Port = NTesting::GetFreePort();
    Address = TString("localhost:") + ToString(Port);

    grpc::ServerBuilder builder;
    builder.AddListeningPort(Address, grpc::InsecureServerCredentials());
    builder.RegisterService(&Service);

    Build(builder);

    Server = builder.BuildAndStart();

    Channel = grpc::CreateChannel(Address, grpc::InsecureChannelCredentials());
    Client = TTestService::NewStub(Channel);
}

void TestServerFixture::Build(grpc::ServerBuilder&) {
}
