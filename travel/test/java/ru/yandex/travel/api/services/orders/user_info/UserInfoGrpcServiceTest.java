package ru.yandex.travel.api.services.orders.user_info;

import java.util.List;

import com.google.common.util.concurrent.Futures;
import io.opentracing.mock.MockTracer;
import org.junit.Test;

import ru.yandex.travel.commons.retry.Retry;
import ru.yandex.travel.credentials.UserCredentials;
import ru.yandex.travel.credentials.UserCredentialsBuilder;
import ru.yandex.travel.order_type.proto.EOrderType;
import ru.yandex.travel.orders.user_info.proto.TGetUserExistingOrderTypesRsp;
import ru.yandex.travel.orders.user_info.proto.UserInfoInterfaceV1Grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserInfoGrpcServiceTest {
    private final UserCredentialsBuilder userCredentialsBuilder = new UserCredentialsBuilder();

    private UserInfoGrpcClientFactory createUserInfoGrpcClientFactoryStub(List<EOrderType> orderTypes) {
        return () -> {
            var stub = mock(UserInfoInterfaceV1Grpc.UserInfoInterfaceV1FutureStub.class);
            when(stub.getUserExistingOrderTypes(any()))
                    .thenReturn(Futures.immediateFuture(
                            TGetUserExistingOrderTypesRsp.newBuilder()
                                    .addAllExistingOrderTypes(orderTypes)
                                    .build()));
            return stub;
        };
    }

    private UserInfoGrpcClientFactory createUserInfoGrpcClientFactoryStubWithException() {
        return () -> {
            var stub = mock(UserInfoInterfaceV1Grpc.UserInfoInterfaceV1FutureStub.class);
            when(stub.getUserExistingOrderTypes(any())).thenReturn(Futures.immediateFailedFuture(new Exception()));
            return stub;
        };
    }

    private UserCredentials createUserCredentialsMock() {
        return userCredentialsBuilder.build("sessionkey", "123", "passport-id-1", "user1", null, "127.0.0.1", false, false);
    }

    private UserInfoGrpcProperties createUserInfoGrpcProperties(boolean enabled) {
        var userInfoGrpcProperties = new UserInfoGrpcProperties();
        userInfoGrpcProperties.setEnabled(enabled);
        return userInfoGrpcProperties;
    }

    @Test
    public void testGetUserExistingOrderTypes_OK() {
        var retryHelper = new Retry(new MockTracer());
        var userInfoGrpcService = new UserInfoGrpcService(
                createUserInfoGrpcClientFactoryStub(List.of(EOrderType.OT_HOTEL)),
                createUserInfoGrpcProperties(true),
                retryHelper);
        var uc = createUserCredentialsMock();
        assertThat(userInfoGrpcService.getUserExistingOrderTypes(uc).join()).isEqualTo(List.of(EOrderType.OT_HOTEL));
    }

    @Test
    public void testGetUserExistingOrderTypes_Disabled() {
        var retryHelper = new Retry(new MockTracer());
        var userInfoGrpcService = new UserInfoGrpcService(
                createUserInfoGrpcClientFactoryStub(List.of(EOrderType.OT_HOTEL)),
                createUserInfoGrpcProperties(false),
                retryHelper);
        var uc = createUserCredentialsMock();
        assertThat(userInfoGrpcService.getUserExistingOrderTypes(uc).join()).isEmpty();
    }

    @Test
    public void testGetUserExistingOrderTypes_Exception() {
        var retryHelper = new Retry(new MockTracer());
        var userInfoGrpcService = new UserInfoGrpcService(
                createUserInfoGrpcClientFactoryStubWithException(),
                createUserInfoGrpcProperties(true),
                retryHelper);
        var uc = createUserCredentialsMock();
        assertThat(userInfoGrpcService.getUserExistingOrderTypes(uc)).isCompletedExceptionally();
    }
}
