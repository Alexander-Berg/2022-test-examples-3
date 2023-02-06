package ru.yandex.market.abo.core.offer.stop_words;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.offer.report.Cpa;
import ru.yandex.market.abo.core.offer.report.IndexType;
import ru.yandex.market.abo.core.offer.report.OfferService;
import ru.yandex.market.abo.core.region.Regions;
import ru.yandex.market.abo.core.shop.ShopInfoService;
import ru.yandex.market.abo.test.TestHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * created on 22.11.16.
 */
public class OfferStopWordManagerTest {
    private static final int OFFERS_COUNT_ON_EACH_WORD = 1;
    private static final List<OfferStopWord> MANAGER_STOP_WORDS = initStopWordsList();

    @InjectMocks
    OfferStopWordManager offerStopWordManager;
    @Mock
    OfferStopWordService offerStopWordService;
    @Mock
    OfferService offerService;
    @Mock
    ShopInfoService shopInfoService;
    @Mock
    ExecutorService pool;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(offerStopWordService.findAllWhite()).thenReturn(MANAGER_STOP_WORDS);
        when(offerService.countReportOffers(any(), any())).thenReturn(OFFERS_COUNT_ON_EACH_WORD);
        when(shopInfoService.getShopOwnRegion(anyLong())).thenReturn((long) Regions.MOSCOW);
        TestHelper.mockExecutorService(pool);
    }

    @Test
    public void testCountStopWords() {
        Map<String, Integer> stopWordsFoundMap = offerStopWordManager.countStopWords(774, IndexType.SANDBOX, Cpa.Any);
        assertEquals(MANAGER_STOP_WORDS.size(), stopWordsFoundMap.size());
        stopWordsFoundMap.forEach((word, count) -> {
            assertTrue(MANAGER_STOP_WORDS.stream().map(OfferStopWord::getWord).anyMatch(s -> s.equals(word)));
            assertEquals(OFFERS_COUNT_ON_EACH_WORD, count.intValue());
        });
    }

    private static List<OfferStopWord> initStopWordsList() {
        return Stream.generate(() -> RandomStringUtils.randomAlphabetic(15)).limit(100)
                .map(w -> OfferStopWordServiceTest.constructStopWord(w, w))
                .collect(Collectors.toList());
    }
}
