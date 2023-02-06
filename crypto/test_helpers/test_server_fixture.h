#pragma once

#include "test_service_impl.h"

#include <grpc/grpc.h>
#include <grpc++/channel.h>
#include <grpc++/server.h>
#include <grpc++/server_builder.h>

#include <library/cpp/testing/common/network.h>
#include <library/cpp/testing/gtest/gtest.h>

namespace NCrypta::NGrpc {
    class TestServerFixture : public testing::Test {
    protected:
        void SetUp() override;
        virtual void Build(grpc::ServerBuilder& builder);

        NTesting::TPortHolder Port;
        TString Address;
        TTestServiceImpl Service;
        std::unique_ptr<grpc::Server> Server;
        std::shared_ptr<grpc::Channel> Channel;
        std::unique_ptr<TTestService::Stub> Client;
    };
}