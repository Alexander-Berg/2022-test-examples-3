package ru.yandex.travel.grpc.interceptors;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

final class HeaderValueCaptorInterceptor<T> implements ServerInterceptor {
    private Map<Metadata.Key<T>, T> recordedValues;
    private List<Metadata.Key<T>> keys;

    private boolean isCalled = false;

    private HeaderValueCaptorInterceptor(List<Metadata.Key<T>> keys) {
        this.keys = keys;
        recordedValues = new HashMap<>(keys.size());
    }


    public static <T> HeaderValueCaptorInterceptor<T> forKey(Metadata.Key<T> key) {
        return new HeaderValueCaptorInterceptor<T>(Collections.singletonList(key));
    }

    public static <T> HeaderValueCaptorInterceptor<T> forKeys(Metadata.Key<T>... keys) {
        return new HeaderValueCaptorInterceptor<T>(Arrays.asList(keys));
    }

    public T getRecordedValue() {
        assert keys.size() == 1;
        return recordedValues.get(keys.get(0));
    }

    public T getRecordedValueForKey(Metadata.Key<T> key) {
        return recordedValues.get(key);
    }

    public boolean isCalled() {
        return isCalled;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        isCalled = true;
        for (Metadata.Key<T> key : keys) {
            T recordedValue = headers.get(key);
            if (recordedValue != null) {
                recordedValues.put(key, recordedValue);
            }
        }

        return next.startCall(call, headers);
    }
}
