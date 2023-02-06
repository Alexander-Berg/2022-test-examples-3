package ru.yandex.search.yc;

import java.util.function.BiFunction;

import io.grpc.stub.StreamObserver;
import yandex.cloud.priv.servicecontrol.v1.AccessServiceOuterClass.AuthorizeRequest;
import yandex.cloud.priv.servicecontrol.v1.AccessServiceOuterClass.AuthorizeResponse;

public class IamAllAllowHandler
    implements BiFunction<AuthorizeRequest, StreamObserver<AuthorizeResponse>, AuthorizeResponse>,
    GrpcHandlerSupplier<AuthorizeRequest, AuthorizeResponse>
{
    public static final IamAllAllowHandler INSTANCE = new IamAllAllowHandler();

    @Override
    public AuthorizeResponse apply(
        final AuthorizeRequest request,
        final StreamObserver<AuthorizeResponse> observer)
    {
        AuthorizeResponse response = AuthorizeResponse.newBuilder().build();
        observer.onNext(response);
        observer.onCompleted();
        return null;
    }

    @Override
    public BiFunction<AuthorizeRequest, StreamObserver<AuthorizeResponse>, AuthorizeResponse> next() {
        return this;
    }
}
