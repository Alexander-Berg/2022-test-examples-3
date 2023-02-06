package ru.yandex.market.pers.notify.test;

import ru.yandex.market.pers.notify.mock.MarketMailerMockFactory;

import static org.mockito.Mockito.reset;

public class MarketMailerTestEnvironment {
    public void setUp() throws Exception {
        initMailerMocks();
    }

    private synchronized void initMailerMocks() {
        // reset mocks
        reset(MarketMailerMockFactory.MOCKS.keySet().toArray(new Object[0]));

        // init mocks
        MarketMailerMockFactory.MOCKS.forEach((mock, init) -> init.accept(mock));
    }
}
