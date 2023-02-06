package ru.yandex.travel.grpc.interceptors;

import java.util.Optional;

import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.travel.test.fake.proto.FakeServiceGrpc;
import ru.yandex.travel.test.fake.proto.TTestMethodReq;
import ru.yandex.travel.tvm.TvmWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class TvmHeaderClientHelperTest {

    @Rule
    public GrpcCleanupRule cleanupRule = new GrpcCleanupRule();

    private FakeService fakeService;

    private TvmHeaderClientHelper tvmHeaderClientHelper;

    private TvmWrapper tvm2;

    private final String validDstId = "dst_service";

    @Before
    public void setUp() {
        tvm2 = mock(TvmWrapper.class);
        fakeService = new FakeService();
        tvmHeaderClientHelper = new TvmHeaderClientHelper(tvm2);
    }

    @Test
    public void callToServiceWithTicketInterceptorSetsCorrectHeader() {
        String serviceTicketValue = "Service Ticket Value";
        HeaderValueCaptorInterceptor<String> tvmHeaderValueCaptor = HeaderValueCaptorInterceptor.forKey(
                TvmHeaders.METADATA_SERVICE_TICKET_HEADER
        );
        when(tvm2.getServiceTicketOptional(any())).thenReturn(Optional.of(serviceTicketValue));
        FakeServiceGrpc.FakeServiceBlockingStub fakeServiceBlockingStub = createServerAndBlockingStub(tvmHeaderValueCaptor);
        FakeServiceGrpc.FakeServiceBlockingStub decoratedServiceStub = tvmHeaderClientHelper.withTvmInterceptor(fakeServiceBlockingStub, validDstId);
        decoratedServiceStub.testMethod(TTestMethodReq.newBuilder().build());
        assertThat(tvmHeaderValueCaptor.getRecordedValue()).isNotNull();
        assertThat(tvmHeaderValueCaptor.getRecordedValue()).isEqualTo(serviceTicketValue);
    }

    @Test
    public void callToServiceWithTicketInterceptorWhenServiceTicketNotAvailable() {
        HeaderValueCaptorInterceptor<String> tvmHeaderValueCaptor = HeaderValueCaptorInterceptor.forKey(
                TvmHeaders.METADATA_SERVICE_TICKET_HEADER
        );
        when(tvm2.getServiceTicketOptional(any())).thenReturn(Optional.empty());
        FakeServiceGrpc.FakeServiceBlockingStub fakeServiceBlockingStub = createServerAndBlockingStub(tvmHeaderValueCaptor);
        FakeServiceGrpc.FakeServiceBlockingStub decoratedServiceStub = tvmHeaderClientHelper.withTvmInterceptor(fakeServiceBlockingStub, validDstId);
        Throwable error = catchThrowable(() -> {
            decoratedServiceStub.testMethod(TTestMethodReq.newBuilder().build());
        });
        assertThat(error).isInstanceOf(StatusRuntimeException.class);
        StatusRuntimeException typedException = (StatusRuntimeException) error;
        assertThat(typedException.getStatus().getCode()).isEqualTo(Status.Code.FAILED_PRECONDITION);
        assertThat(tvmHeaderValueCaptor.isCalled()).isFalse();
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
