package ru.yandex.direct.api.v5.semaphore.jvm;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.yandex.direct.api.v5.bids.SetAutoRequest;
import com.yandex.direct.api.v5.bids.SetRequest;
import com.yandex.direct.api.v5.retargetinglists.GetRequest;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.context.DefaultMessageContext;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.endpoint.MethodEndpoint;

import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.entity.bids.BidsEndpoint;
import ru.yandex.direct.api.v5.entity.retargetinglists.RetargetingListsEndpoint;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.env.EnvironmentType;

import static org.mockito.Mockito.mock;

public class ApiJvmSemaphoreInterceptorTest {
    private ApiJvmSemaphoreInterceptor interceptor;
    private MethodEndpoint bidsSetAuto;
    private MethodEndpoint retListGet;
    private MethodEndpoint bidsSet;
    private ApiContextHolder apiContextHolder;

    @Before
    public void setUp() throws Exception {
        Config config = ConfigFactory.parseMap(ImmutableMap.of(
                "service_semaphore", ImmutableMap.of(
                        "semaphores", ImmutableMap.of(
                                "bsauction", 2
                        ),
                        "mapping", ImmutableMap.of(
                                "bids", "bsauction"
                        )
                )
        ));
        apiContextHolder = mock(ApiContextHolder.class);
        interceptor = new ApiJvmSemaphoreInterceptor(new DirectConfig(config), apiContextHolder);

        retListGet = new MethodEndpoint(
                mock(RetargetingListsEndpoint.class),
                RetargetingListsEndpoint.class.getMethod("get", GetRequest.class)
        );
        bidsSet = new MethodEndpoint(
                mock(BidsEndpoint.class),
                BidsEndpoint.class.getMethod("set", SetRequest.class)
        );
        bidsSetAuto = new MethodEndpoint(
                mock(BidsEndpoint.class),
                BidsEndpoint.class.getMethod("setAuto", SetAutoRequest.class)
        );
    }

    @Test
    public void productionConfigIsGood() throws Exception {
        DirectConfig prodConfig = DirectConfigFactory.getConfig(EnvironmentType.PRODUCTION);
        // no exception
        new ApiJvmSemaphoreInterceptor(prodConfig, apiContextHolder);
    }

    @Test(expected = IllegalStateException.class)
    public void exceptionOnBadConfig() throws Exception {
        Config config = ConfigFactory.parseMap(ImmutableMap.of(
                "service_semaphore", ImmutableMap.of(
                        "semaphores", ImmutableMap.of(
                                "bsauction", 2
                        ),
                        "mapping", ImmutableMap.of(
                                // нет текого семафора!
                                "bids", "bsauction2"
                        )
                )
        ));
        // throws
        new ApiJvmSemaphoreInterceptor(new DirectConfig(config), apiContextHolder);
    }

    @Test
    public void noLimitsForRetargetingLists() throws Exception {
        // на RetargetingListsEndpoint лимитов нет
        interceptor.handleRequest(mock(MessageContext.class), retListGet);
        interceptor.handleRequest(mock(MessageContext.class), retListGet);
        interceptor.handleRequest(mock(MessageContext.class), retListGet);
        interceptor.handleRequest(mock(MessageContext.class), retListGet);
    }

    @Test
    public void limitForBidsMethods() throws Exception {
        SoftAssertions soft = new SoftAssertions();
        MessageContext setReq = createMessageContext();
        MessageContext setAutoReq = createMessageContext();
        // два раза начинаем обработку запроса
        interceptor.handleRequest(setReq, bidsSet);
        interceptor.handleRequest(setAutoReq, bidsSetAuto);
        // дальше начинаем фейлиться
        soft.assertThatThrownBy(() -> interceptor.handleRequest(mock(MessageContext.class), bidsSet));
        soft.assertThatThrownBy(() -> interceptor.handleRequest(mock(MessageContext.class), bidsSetAuto));
        // при этом, другие методы работаею
        interceptor.handleRequest(mock(MessageContext.class), retListGet);
        // а bids - фэйлится
        soft.assertThatThrownBy(() -> interceptor.handleRequest(mock(MessageContext.class), bidsSet));
        // но как только один из запросов доработал
        interceptor.afterCompletion(setReq, bidsSet, null);
        // мы молучаем возможность начать обработку ещё одного запроса
        interceptor.handleRequest(mock(MessageContext.class), bidsSet);
        // но только одного!
        soft.assertThatThrownBy(() -> interceptor.handleRequest(mock(MessageContext.class), bidsSet));
        soft.assertAll();
    }

    private MessageContext createMessageContext() {
        return new DefaultMessageContext(mock(WebServiceMessage.class), mock(WebServiceMessageFactory.class));
    }
}
