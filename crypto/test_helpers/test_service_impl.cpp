#include "test_service_impl.h"

#include <util/datetime/base.h>

using namespace NCrypta::NGrpc;

grpc::Status TTestServiceImpl::Echo(grpc::ServerContext* context, const TRequest* request, TResponse* response) {
    Y_UNUSED(context);
    response->SetMessage("Got " + request->GetMessage());
    return grpc::Status::OK;
}

grpc::Status TTestServiceImpl::NotFound(grpc::ServerContext* context, const TRequest* request, TResponse* response) {
    Y_UNUSED(context, request, response);
    return grpc::Status(grpc::NOT_FOUND, "not found");
}

grpc::Status TTestServiceImpl::Fail(grpc::ServerContext* context, const TRequest* request, TResponse* response) {
    Y_UNUSED(context, request, response);
    ythrow yexception() << "Failed";
}

grpc::Status TTestServiceImpl::Timeout(grpc::ServerContext* context, const TRequest* request, TResponse* response) {
    Y_UNUSED(context);
    Sleep(TDuration::Seconds(5));
    response->SetMessage("Got " + request->GetMessage());
    return grpc::Status::OK;
}
