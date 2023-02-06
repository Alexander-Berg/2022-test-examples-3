#pragma once

#include <crypta/lib/native/grpc/test_helpers/grpc/test_service.grpc.pb.h>


namespace NCrypta::NGrpc {
    class TTestServiceImpl: public TTestService::Service {
    private:
        grpc::Status Echo(grpc::ServerContext* context, const TRequest* request, TResponse* response) override;
        grpc::Status NotFound(grpc::ServerContext* context, const TRequest* request, TResponse* response) override;
        grpc::Status Fail(grpc::ServerContext* context, const TRequest* request, TResponse* response) override;
        grpc::Status Timeout(grpc::ServerContext* context, const TRequest* request, TResponse* response) override;
    };
}


