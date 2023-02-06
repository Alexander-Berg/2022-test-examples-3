package ru.yandex.market.loyalty.client.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.response.DefaultResponseCreator;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static org.junit.Assert.fail;

/**
 * @author dinyat
 * 21/06/2017
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/market-loyalty-client/test-bean.xml")
public abstract class MarketLoyaltyMockedServerTest {
    @Value("http://${market.loyalty.host}:${market.loyalty.port}")
    protected String loyaltyUrl;

    @Autowired
    protected PodamFactory podamFactory;

    @Autowired
    private Supplier<ObjectMapper> objectMapperHolder;

    @Autowired
    private MockRestServiceServer server;

    private final Lock serverLocker = new ReentrantLock();

    protected <T> DefaultResponseCreator withSuccess(T object) throws JsonProcessingException {
        return MockRestResponseCreators.withSuccess(getObjectMapper().writeValueAsString(object),
                MediaType.APPLICATION_JSON);
    }

    protected static DefaultResponseCreator withSuccess() {
        return MockRestResponseCreators.withSuccess();
    }

    protected ObjectMapper getObjectMapper() {
        return objectMapperHolder.get();
    }

    protected CloseableMockRestServiceServerHolder getMockRestServiceServer() throws InterruptedException {
        return new CloseableMockRestServiceServerHolder();
    }

    protected class CloseableMockRestServiceServerHolder implements AutoCloseable {
        public CloseableMockRestServiceServerHolder() throws InterruptedException {
            if (!serverLocker.tryLock(1, TimeUnit.MINUTES)) {
                fail();
            }
        }

        @Override
        public void close() {
            try {
                server.reset();
            } finally {
                serverLocker.unlock();
            }
        }

        public void verify() {
            server.verify();
        }

        public ResponseActions expect(RequestMatcher matcher) {
            return server.expect(matcher);
        }
    }
}
