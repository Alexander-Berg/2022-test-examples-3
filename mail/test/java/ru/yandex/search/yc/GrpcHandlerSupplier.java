package ru.yandex.search.yc;

import java.util.function.BiFunction;

import io.grpc.stub.StreamObserver;

public interface GrpcHandlerSupplier<T, R> {
    BiFunction<T, StreamObserver<R>, R> next();
}
