package ru.yandex.market.pers.notify.test;


import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.pers.notify.exceptions.Mediator4xxResponseException;
import ru.yandex.market.pers.notify.mock.MarketUtilsMockFactory;

import static org.mockito.Mockito.reset;


public class MarketUtilsTestEnvironment {
    @Autowired
    private MarketUtilsMockFactory utilsMockFactory;

    @Autowired
    private PersNotifyClient persNotifyClient;

    public void setUp() throws Mediator4xxResponseException {
        initMarketUtilsMocks();
        configurePersNotifyClient();
    }

    public void tearDown() {
        //do nothing
    }

    private void initMarketUtilsMocks() {
        // reset mocks
        reset(MarketUtilsMockFactory.MOCKS.keySet().toArray(new Object[0]));

        // init mocks
        MarketUtilsMockFactory.MOCKS.forEach((mock, init) -> init.accept(mock));
    }

    private void configurePersNotifyClient() {
        persNotifyClient.setApiContextPath("/");
    }
}
