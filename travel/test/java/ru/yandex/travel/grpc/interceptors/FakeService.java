package ru.yandex.travel.grpc.interceptors;

import io.grpc.stub.StreamObserver;

import ru.yandex.travel.test.fake.proto.FakeServiceGrpc;
import ru.yandex.travel.test.fake.proto.TTestMethodReq;
import ru.yandex.travel.test.fake.proto.TTestMethodRsp;

public class FakeService extends FakeServiceGrpc.FakeServiceImplBase {
    @Override
    public void testMethod(TTestMethodReq request, StreamObserver<TTestMethodRsp> responseObserver) {
        responseObserver.onNext(TTestMethodRsp.newBuilder().build());
        responseObserver.onCompleted();
    }
}
