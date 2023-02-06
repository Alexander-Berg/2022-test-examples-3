package ru.yandex.travel.grpc.interceptors;

import io.grpc.ClientInterceptor;
import io.grpc.Context;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Answers;

import ru.yandex.travel.credentials.UserCredentials;
import ru.yandex.travel.credentials.UserCredentialsAuthValidatorTvmImpl;
import ru.yandex.travel.credentials.UserCredentialsBuilder;
import ru.yandex.travel.credentials.UserCredentialsPassportExtractorTvmImpl;
import ru.yandex.travel.credentials.UserCredentialsValidator;
import ru.yandex.travel.test.fake.proto.FakeServiceGrpc;
import ru.yandex.travel.test.fake.proto.TTestMethodReq;
import ru.yandex.travel.test.fake.proto.TTestMethodRsp;
import ru.yandex.travel.tvm.TvmWrapper;
import ru.yandex.travel.tvm.TvmWrapperImpl;
import ru.yandex.travel.tvm.UserTicketCheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.tvm.UserTicketCheckStatus.INVALID_TICKET;
import static ru.yandex.travel.tvm.UserTicketCheckStatus.OK;
import static ru.yandex.travel.tvm.UserTicketCheckStatus.PASSPORT_ID_MISMATCH;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class UserCredentialsServerInterceptorTest {
    @Rule
    public GrpcCleanupRule cleanupRule = new GrpcCleanupRule();
    private FakeServiceWithCredentialCheck fakeService;
    private TvmWrapper tvm2;
    private UserCredentialsServerInterceptor userCredentialsServerInterceptor;
    private FakeServiceGrpc.FakeServiceBlockingStub fakeServiceClient;

    @Before
    public void setUp() {
        tvm2 = mock(TvmWrapperImpl.class, Answers.CALLS_REAL_METHODS);
        userCredentialsServerInterceptor = new UserCredentialsServerInterceptor(
                new UserCredentialsBuilder(),
                new UserCredentialsPassportExtractorTvmImpl(tvm2),
                new UserCredentialsValidator(new UserCredentialsAuthValidatorTvmImpl(tvm2)));
        fakeService = new FakeServiceWithCredentialCheck();
        fakeService = spy(fakeService);
    }

    @Test
    public void testAllHeadersArePassedTicketIsValidPassportIdMatches() {
        fakeServiceClient = createServerAndBlockingStub(new UserCredentialsClientInterceptor());
        when(tvm2.checkUserTicket(any(), any())).thenReturn(new UserTicketCheck(OK, null, 42L));
        Context.current().withValue(UserCredentials.KEY,
                new UserCredentials(null, "foo", "42", "bar", "baz", "127.0.0.1", false, false))
                .run(() -> fakeServiceClient.testMethod(TTestMethodReq.newBuilder().build()));
        assertThat(fakeService.userCredentials).isNotNull();
        assertThat(fakeService.userCredentials.getLogin()).isEqualTo("bar");
        assertThat(fakeService.userCredentials.getUserTicket()).isEqualTo("baz");
        assertThat(fakeService.userCredentials.getPassportId()).isEqualTo("42");
        assertThat(fakeService.userCredentials.getYandexUid()).isEqualTo("foo");
        assertThat(fakeService.userCredentials.getSessionKey()).isNull();
    }

    @Test
    public void testOnlyYandexUidAndSessionKey() {
        fakeServiceClient = createServerAndBlockingStub(new UserCredentialsClientInterceptor());
        Context.current().withValue(UserCredentials.KEY,
                new UserCredentials("sess", "foo", null, null, null, "127.0.0.1", false, false))
                .run(() -> fakeServiceClient.testMethod(TTestMethodReq.newBuilder().build()));
        assertThat(fakeService.userCredentials).isNotNull();
        assertThat(fakeService.userCredentials.getYandexUid()).isEqualTo("foo");
        assertThat(fakeService.userCredentials.getSessionKey()).isEqualTo("sess");
        assertThat(fakeService.userCredentials.isLoggedIn()).isFalse();
    }

    @Test
    public void testNoLogin() {
        fakeServiceClient = createServerAndBlockingStub(new UserCredentialsClientInterceptor());
        when(tvm2.checkUserTicket(any(), any())).thenReturn(new UserTicketCheck(OK, null, 42L));
        Context.current().withValue(UserCredentials.KEY,
                new UserCredentials(null, "foo", "42", null, "baz", "127.0.0.1", false, false))
                .run(() -> fakeServiceClient.testMethod(TTestMethodReq.newBuilder().build()));
        assertThat(fakeService.userCredentials).isNotNull();
        assertThat(fakeService.userCredentials.getLogin()).isNull();
        assertThat(fakeService.userCredentials.getUserTicket()).isEqualTo("baz");
        assertThat(fakeService.userCredentials.getPassportId()).isEqualTo("42");
        assertThat(fakeService.userCredentials.getYandexUid()).isEqualTo("foo");
        assertThat(fakeService.userCredentials.isLoggedIn()).isTrue();
    }

    @Test
    public void testPassportIdDoesNotMatchTicket() {
        fakeServiceClient = createServerAndBlockingStub(new UserCredentialsClientInterceptor());
        when(tvm2.checkUserTicket(any(), any())).thenReturn(new UserTicketCheck(PASSPORT_ID_MISMATCH, "Passport id " +
                "does not match the ticket", 41L));
        StatusRuntimeException ex = assertCausesException(
                new UserCredentials(null, "foo", "42", "bar", "baz", "127.0.0.1", false, false));
        assertThat(ex.getStatus().getDescription()).isEqualTo("Passport id does not match the ticket");
    }

    @Test
    public void testInvalidTicket() {
        fakeServiceClient = createServerAndBlockingStub(new UserCredentialsClientInterceptor());
        when(tvm2.checkUserTicket(any(), any())).thenReturn(new UserTicketCheck(INVALID_TICKET, "Invalid user ticket"
                , 42L));
        StatusRuntimeException ex = assertCausesException(
                new UserCredentials(null, "foo", "42", "bar", "baz", "127.0.0.1", false, false));
        assertThat(ex.getStatus().getDescription()).isEqualTo("Invalid user ticket");
    }

    @Test
    public void testNoTicketWithPassportId() {
        fakeServiceClient = createServerAndBlockingStub(new UserCredentialsClientInterceptor());
        StatusRuntimeException ex = assertCausesException(
                new UserCredentials(null, "foo", "42", "bar", null, "127.0.0.1", false, false));
        assertThat(ex.getStatus().getDescription()).isEqualTo("Missing user ticket");
    }

    @Test
    public void testNoTicketWithLogin() {
        fakeServiceClient = createServerAndBlockingStub(new UserCredentialsClientInterceptor());
        StatusRuntimeException ex = assertCausesException(
                new UserCredentials("sKey", "foo", null, "bar", null, "127.0.0.1", false, false));
        assertThat(ex.getStatus().getDescription()).isEqualTo("Missing user ticket");
    }

    @Test
    public void testNoYandexUid() {
        fakeServiceClient = createServerAndBlockingStub(new UserCredentialsClientInterceptor());
        StatusRuntimeException ex = assertCausesException(
                new UserCredentials("session", null, "42", "bar", "some", "127.0.0.1", false, false));
        assertThat(ex.getStatus().getDescription()).isEqualTo("Missing yandex uid");
    }

    @Test
    public void testNoSessionKeyNoPassportId() {
        fakeServiceClient = createServerAndBlockingStub(new UserCredentialsClientInterceptor());
        StatusRuntimeException ex = assertCausesException(
                new UserCredentials(null, "some", null, null, null, "127.0.0.1", false, false));
        assertThat(ex.getStatus().getDescription()).isEqualTo("Either passport id or session key must be present");
    }

    @Test
    public void testErrorWhenNoUserInfoIsProvided() {
        fakeServiceClient = createServerAndBlockingStub(null);
        StatusRuntimeException exception = catchThrowableOfType(
                () -> fakeServiceClient.testMethod(TTestMethodReq.newBuilder().build()),
                StatusRuntimeException.class
        );
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        verify(fakeService, never()).testMethod(any(), any());
    }

    private StatusRuntimeException assertCausesException(UserCredentials credentials) {
        fakeServiceClient = createServerAndBlockingStub(new UserCredentialsClientInterceptor());
        Throwable[] res = new Throwable[1];
        Context.current().withValue(UserCredentials.KEY, credentials).run(() -> {
            Throwable t = catchThrowableOfType(
                    () -> fakeServiceClient.testMethod(TTestMethodReq.newBuilder().build()),
                    StatusRuntimeException.class);
            res[0] = t;
        });
        verify(fakeService, never()).testMethod(any(), any());
        return (StatusRuntimeException) res[0];
    }


    private FakeServiceGrpc.FakeServiceBlockingStub createServerAndBlockingStub(ClientInterceptor headerInterceptor) {
        try {

            String serverName = InProcessServerBuilder.generateName();

            cleanupRule.register(
                    InProcessServerBuilder.forName(serverName)
                            .directExecutor()
                            .addService(
                                    ServerInterceptors.intercept(fakeService, userCredentialsServerInterceptor)
                            )
                            .build()
                            .start()
            );

            InProcessChannelBuilder channelBuilder = InProcessChannelBuilder.forName(serverName);
            if (headerInterceptor != null) {
                channelBuilder = channelBuilder.intercept(headerInterceptor);
            }

            return FakeServiceGrpc.newBlockingStub(
                    cleanupRule.register(
                            channelBuilder.build()
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static class FakeServiceWithCredentialCheck extends FakeService {
        private UserCredentials userCredentials;

        @Override
        public void testMethod(TTestMethodReq request, StreamObserver<TTestMethodRsp> responseObserver) {
            userCredentials = UserCredentials.get();
            super.testMethod(request, responseObserver);
        }
    }
}
