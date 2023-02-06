package ru.yandex.travel.api;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import io.grpc.util.MutableHandlerRegistry;
import lombok.Getter;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.api.services.orders.OrchestratorAdminClientFactory;
import ru.yandex.travel.commons.http.CommonHttpHeaders;
import ru.yandex.travel.credentials.UserCredentials;
import ru.yandex.travel.credentials.UserCredentialsBuilder;
import ru.yandex.travel.grpc.interceptors.UserCredentialsClientInterceptor;
import ru.yandex.travel.orders.admin.proto.OrdersAdminInterfaceV1Grpc;
import ru.yandex.travel.orders.admin.proto.TGetOrderReq;
import ru.yandex.travel.orders.admin.proto.TGetOrderRsp;
import ru.yandex.travel.tvm.TvmWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.grpc.interceptors.UserCredentialsInterceptorCommons.LOGIN_HEADER;
import static ru.yandex.travel.grpc.interceptors.UserCredentialsInterceptorCommons.PASSPORT_ID_HEADER;
import static ru.yandex.travel.grpc.interceptors.UserCredentialsInterceptorCommons.SESSION_KEY_HEADER;
import static ru.yandex.travel.grpc.interceptors.UserCredentialsInterceptorCommons.USER_IP_HEADER;
import static ru.yandex.travel.grpc.interceptors.UserCredentialsInterceptorCommons.USER_TICKET_HEADER;
import static ru.yandex.travel.grpc.interceptors.UserCredentialsInterceptorCommons.YANDEX_UID_HEADER;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "tvm.enabled=true",
                "tvm-service.enabled=true",
                "credentials.enabled=true",
                "management.server.port=0"
        })
@ActiveProfiles("test")
public class TvmUserCredentialsIntegrationTest {

    @LocalServerPort
    private int localPort;

    @MockBean
    private TvmWrapper tvmService;

    private AsyncHttpClient asyncHttpClient;

    @MockBean(name = "userCredentialsInternalBuilder")
    private UserCredentialsBuilder userCredentialsInternalBuilder;

    @MockBean
    public OrchestratorAdminClientFactory orchestratorAdminClientFactory;

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private final MutableHandlerRegistry serviceRegistry = new MutableHandlerRegistry();

    @Before
    public void setUp() throws IOException {
        asyncHttpClient = Dsl.asyncHttpClient(new DefaultAsyncHttpClientConfig.Builder()
                .setThreadFactory(new ThreadFactoryBuilder().setDaemon(true).build())
        );
    }

    @Test
    public void testHeadersPassedDownToAdminCall() throws ExecutionException, InterruptedException, IOException {
        String sessionKey = "session_key";
        String yandexUid = "yandex_uid";
        String passportId = "passport_id";
        String login = "login";
        String userTicket = "user_ticket";
        String userIp = "127.0.0.1";
        when(userCredentialsInternalBuilder.build(any(), any(), any(), any(), any(), any(), anyBoolean(), anyBoolean())).then(
                (Answer<UserCredentials>) invocation -> new UserCredentials(
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        invocation.getArgument(2),
                        invocation.getArgument(3),
                        invocation.getArgument(4),
                        invocation.getArgument(5),
                        invocation.getArgument(6),
                        invocation.getArgument(7)
                )
        );
        AtomicBoolean getOrderCalled = new AtomicBoolean(false);
        // Generate a unique in-process server name.
        String serverName = InProcessServerBuilder.generateName();
        // Use a mutable service registry for later registering the service impl for each test case.
        grpcCleanup.register(InProcessServerBuilder.forName(serverName)
                .fallbackHandlerRegistry(serviceRegistry).directExecutor().build().start());
        UserCredentialsCaptorInterceptor recordInterceptor = new UserCredentialsCaptorInterceptor();
        OrdersAdminInterfaceV1Grpc.OrdersAdminInterfaceV1FutureStub stub = createServerAndFutureStub(recordInterceptor);
        when(orchestratorAdminClientFactory.createAdminFutureStub(anyBoolean())).thenReturn(stub);
        OrdersAdminInterfaceV1Grpc.OrdersAdminInterfaceV1ImplBase service =
                new OrdersAdminInterfaceV1Grpc.OrdersAdminInterfaceV1ImplBase() {
                    @Override
                    public void getOrder(TGetOrderReq request, StreamObserver<TGetOrderRsp> responseObserver) {
                        getOrderCalled.set(true);
                        responseObserver.onNext(TGetOrderRsp.newBuilder().build());
                        responseObserver.onCompleted();
                    }
                };
        serviceRegistry.addService(service);

        RequestBuilder builder = new RequestBuilder()
                .setHeader(CommonHttpHeaders.HeaderType.SESSION_KEY.getHeader(), sessionKey)
                .setHeader(CommonHttpHeaders.HeaderType.USER_TICKET.getHeader(), userTicket)
                .setHeader(CommonHttpHeaders.HeaderType.USER_LOGIN.getHeader(), login)
                .setHeader(CommonHttpHeaders.HeaderType.PASSPORT_ID.getHeader(), passportId)
                .setHeader(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), yandexUid)
                .setHeader(CommonHttpHeaders.HeaderType.USER_IP.getHeader(), userIp)
                .setUrl("http://localhost:" + localPort + "/api/admin/v1/get_order?id=" + UUID.randomUUID())
                .setMethod("GET");
        Response response = asyncHttpClient.executeRequest(builder.build()).toCompletableFuture().get();

        assertThat(getOrderCalled.get()).isTrue();
        assertThat(recordInterceptor.getLogin()).isEqualTo(login);
        assertThat(recordInterceptor.getPassportId()).isEqualTo(passportId);
        assertThat(recordInterceptor.getSessionKey()).isEqualTo(sessionKey);
        assertThat(recordInterceptor.getUserTicketValue()).isEqualTo(userTicket);
        assertThat(recordInterceptor.getYandexUid()).isEqualTo(yandexUid);

    }

    private OrdersAdminInterfaceV1Grpc.OrdersAdminInterfaceV1FutureStub createServerAndFutureStub(UserCredentialsCaptorInterceptor interceptor) {
        try {

            String serverName = InProcessServerBuilder.generateName();

            grpcCleanup.register(
                    InProcessServerBuilder.forName(serverName)
                            .fallbackHandlerRegistry(serviceRegistry)
                            .directExecutor()
                            .intercept(interceptor)
                            .build()
                            .start()
            );

            InProcessChannelBuilder channelBuilder = InProcessChannelBuilder.forName(serverName);
            return OrdersAdminInterfaceV1Grpc.newFutureStub(channelBuilder.build()).withInterceptors(new UserCredentialsClientInterceptor());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Getter
    private static final class UserCredentialsCaptorInterceptor implements ServerInterceptor {

        private String userTicketValue;
        private String yandexUid;
        private String login;
        private String passportId;
        private String sessionKey;
        private String userIp;

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                                                                     ServerCallHandler<ReqT, RespT> next) {
            userTicketValue = headers.get(USER_TICKET_HEADER);
            yandexUid = headers.get(YANDEX_UID_HEADER);
            login = headers.get(LOGIN_HEADER);
            passportId = headers.get(PASSPORT_ID_HEADER);
            sessionKey = headers.get(SESSION_KEY_HEADER);
            userIp = headers.get(USER_IP_HEADER);
            return next.startCall(call, headers);
        }
    }

}

