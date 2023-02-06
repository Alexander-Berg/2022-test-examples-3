package ru.yandex.search.yc;

import java.util.function.BiFunction;

import io.grpc.stub.StreamObserver;
import yandex.cloud.priv.servicecontrol.v1.AccessServiceGrpc;
import yandex.cloud.priv.servicecontrol.v1.AccessServiceOuterClass.AuthorizeRequest;
import yandex.cloud.priv.servicecontrol.v1.AccessServiceOuterClass.AuthorizeResponse;

public class IamStaticService extends AccessServiceGrpc.AccessServiceImplBase {
    private GrpcHandlerSupplier<AuthorizeRequest, AuthorizeResponse> supplier;

    public IamStaticService() {
        supplier = new EmptySupplier();
    }

    public IamStaticService supplier(
        final GrpcHandlerSupplier<AuthorizeRequest, AuthorizeResponse> supplier)
    {
        this.supplier = supplier;
        return this;
    }

    @Override
    public void authorize(
        final AuthorizeRequest request,
        final StreamObserver<AuthorizeResponse> observer)
    {
        System.err.println("Handling IAM request " + request.toString());
        BiFunction<AuthorizeRequest, StreamObserver<AuthorizeResponse>, AuthorizeResponse> handler
            = supplier.next();

        handler.apply(request, observer);
    }

    private final class EmptySupplier
        implements GrpcHandlerSupplier<AuthorizeRequest, AuthorizeResponse>
    {
        @Override
        public BiFunction<AuthorizeRequest, StreamObserver<AuthorizeResponse>, AuthorizeResponse> next() {
            return (request, observer) -> {
                IamStaticService.super.authorize(request, observer);
                return null;
            };
        }
    }

}
