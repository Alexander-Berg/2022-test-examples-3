package ru.yandex.travel.grpc.interceptors;

import io.grpc.Context;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.travel.credentials.UserCredentials;
import ru.yandex.travel.test.fake.proto.FakeServiceGrpc;
import ru.yandex.travel.test.fake.proto.TTestMethodReq;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class UserCredentialsClientInterceptorTest {
    @Rule
    public GrpcCleanupRule cleanupRule = new GrpcCleanupRule();

    private FakeService fakeService;
    private Context context;

    @Before
    public void setUp() {
        fakeService = new FakeService();
        context =
                Context.current().withValue(UserCredentials.KEY, new UserCredentials(
                        "42",
                        "foo",
                        "42", "bar", "baz",
                        "", false, false)).attach();
    }

    @After
    public void tearDown() {
        Context.current().detach(context);
    }

    @Test
    public void testCorrectHeaders() {
        @SuppressWarnings("unchecked")
        HeaderValueCaptorInterceptor<String> userCredentialsHeaderValueCaptor = HeaderValueCaptorInterceptor.forKeys(
                UserCredentialsInterceptorCommons.YANDEX_UID_HEADER,
                UserCredentialsInterceptorCommons.LOGIN_HEADER,
                UserCredentialsInterceptorCommons.PASSPORT_ID_HEADER,
                UserCredentialsInterceptorCommons.USER_TICKET_HEADER
        );
        FakeServiceGrpc.FakeServiceBlockingStub fakeServiceBlockingStub =
                createServerAndBlockingStub(userCredentialsHeaderValueCaptor)
                        .withInterceptors(new UserCredentialsClientInterceptor());
        fakeServiceBlockingStub.testMethod(TTestMethodReq.newBuilder().build());
        assertThat(userCredentialsHeaderValueCaptor.getRecordedValueForKey(UserCredentialsInterceptorCommons.PASSPORT_ID_HEADER)).isEqualTo("42");
        assertThat(userCredentialsHeaderValueCaptor.getRecordedValueForKey(UserCredentialsInterceptorCommons.LOGIN_HEADER)).isEqualTo("bar");
        assertThat(userCredentialsHeaderValueCaptor.getRecordedValueForKey(UserCredentialsInterceptorCommons.USER_TICKET_HEADER)).isEqualTo("baz");
        assertThat(userCredentialsHeaderValueCaptor.getRecordedValueForKey(UserCredentialsInterceptorCommons.YANDEX_UID_HEADER)).isEqualTo("foo");
    }

    private FakeServiceGrpc.FakeServiceBlockingStub createServerAndBlockingStub(ServerInterceptor... captors) {
        try {

            String serverName = InProcessServerBuilder.generateName();


            cleanupRule.register(
                    InProcessServerBuilder.forName(serverName)
                            .directExecutor()
                            .addService(ServerInterceptors.intercept(fakeService, captors))
                            .build()
                            .start()
            );

            return FakeServiceGrpc.newBlockingStub(
                    cleanupRule.register(
                            InProcessChannelBuilder.forName(serverName).build()
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
