package ru.yandex.market.health.configs.logshatter.url;


import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.common.util.collections.MultiMap;
import ru.yandex.market.health.configs.logshatter.url.mongo.PageMatcherDao;

public class MongoDbPageMatcherCacheControllerTest {
    private static final String TEST_URL = "url";
    private static final String TEST_HOST = "host";

    private PageMatcherDao pageMatcherDao;
    private LocalDbPageMatcherCacheController localCacheController;
    private MongoDbPageMatcherCacheController cacheController;
    private MultiMap<String, String> urlToHost;
    private ConcurrentMap<String, PageTree> testPageTree = new ConcurrentHashMap<>();

    @BeforeEach
    public void setUp() throws IOException {
        urlToHost = new MultiMap<>();
        urlToHost.append(TEST_URL, TEST_HOST);
        testPageTree.put(TEST_HOST, PageTree.build(PageTreeTest.class.getResourceAsStream("/pageMatcherTree/market" +
            "-tree.tsv")));

        pageMatcherDao = Mockito.mock(PageMatcherDao.class);
        localCacheController = Mockito.mock(LocalDbPageMatcherCacheController.class);
        Mockito.when(localCacheController.getPageTree(TEST_HOST)).thenReturn(testPageTree.get(TEST_HOST));
        cacheController = new MongoDbPageMatcherCacheController(
            pageMatcherDao,
            localCacheController
        );
    }

    @Test
    public void loadCacheIfMongoIsEmptyTest() {
        cacheController.loadCache(urlToHost);
        Mockito.verify(pageMatcherDao, Mockito.times(1)).getPagesForUrl(TEST_URL);
        Mockito.verify(localCacheController, Mockito.times(1)).loadCache(urlToHost);
    }

    @Test
    public void loadCacheIfMongoIsNotEmptyTest() {
        Mockito.when(pageMatcherDao.getPagesForUrl(TEST_URL)).thenReturn(
            new PageMatcherEntity(
                TEST_URL,
                List.of(new PageMatcherEntity.PageInfo(
                    "/ping",
                    "ping",
                    "test-service"
                )),
                Instant.now()
            ));
        cacheController.loadCache(urlToHost);
        Mockito.verify(pageMatcherDao, Mockito.times(1)).getPagesForUrl(TEST_URL);
        Mockito.verify(localCacheController, Mockito.times(0)).loadCache(urlToHost);
    }

    @Test
    public void reloadCacheIfMongoIsEmptyTest() {
        loadCacheIfMongoIsEmptyTest();

        cacheController.loadCache(urlToHost);

        Mockito.verify(pageMatcherDao, Mockito.times(2)).getPagesForUrl(TEST_URL);
        Mockito.verify(localCacheController, Mockito.times(1)).loadCache(urlToHost);
    }
}
