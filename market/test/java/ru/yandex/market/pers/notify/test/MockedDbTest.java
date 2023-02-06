package ru.yandex.market.pers.notify.test;

import java.util.Random;

import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.monitoring.ComplicatedMonitoring;
import ru.yandex.market.pers.list.PersBasketClient;
import ru.yandex.market.pers.notify.mock.MockFactory;
import ru.yandex.market.pers.notify.mock.MockedBlackBoxPassportService;
import ru.yandex.market.pers.notify.push.XivaPusherService;
import ru.yandex.market.shopinfo.ShopInfoService;
import ru.yandex.pers.notify.db.PersNotifyEmbeddedDbUtil;

import static org.mockito.Mockito.reset;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:test-bean.xml")
@ActiveProfiles("production")
public abstract class MockedDbTest {
    protected static final Random RND = new Random();
    @Autowired
    private MockFactory mockFactory;
    @Autowired
    private MockedBlackBoxPassportService blackBoxPassportService;
    @Autowired
    private PersBasketClient persBasketClientMock;
    @Autowired
    private MemCachedAgent memCachedAgentMock;
    @Autowired
    @Qualifier("marketPusherService")
    private XivaPusherService marketPusherServiceMock;
    @Autowired
    private ShopInfoService shopInfoServiceMock;
    @Autowired
    private MarketLoyaltyClient marketLoyaltyClient;
    @Autowired
    private ComplicatedMonitoring monitoring;
    @Qualifier("qaHttpClient")
    @Autowired
    private HttpClient qaHttpClient;

    private PersNotifyEmbeddedDbUtil persNotifyEmbeddedDbUtil = PersNotifyEmbeddedDbUtil.INSTANCE;

    @BeforeEach
    public void cleanDatabase() throws Exception {
        persNotifyEmbeddedDbUtil.truncatePersNotifyTables();
    }

    @BeforeEach
    public synchronized void initMocks() throws Exception {
        reset(persBasketClientMock, memCachedAgentMock, marketPusherServiceMock,
            shopInfoServiceMock, marketLoyaltyClient, monitoring, qaHttpClient);
        blackBoxPassportService.reset();

        mockFactory.initPersBasketClientMock(persBasketClientMock);
        mockFactory.initMemCachedAgentMock(memCachedAgentMock);
        mockFactory.initPusherServiceMock(marketPusherServiceMock);
        mockFactory.initShopInfoServiceMock(shopInfoServiceMock);
        mockFactory.initQaHttpClient(qaHttpClient);
    }
}
