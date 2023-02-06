package ru.yandex.market.markup2.utils.cards;

import com.google.common.collect.Lists;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.markup2.utils.model.ModelStorageService;
import ru.yandex.market.markup2.utils.offer.OfferStorageService;
import ru.yandex.market.markup2.utils.report.ReportService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

/**
 * @author inenakhov
 */
@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class InStorageCardsFinderTest extends TestCase {
    private final StorageData storageData = new StorageData();
    private final int categoryId = storageData.getCategoryId();
    private OffersStorageMock remoteOffersStorageService;
    private ModelStorageMock remoteModelStorageService;
    private CategoryModelsMock categoryModelsServiceMock;

    @Mock
    private ReportService reportService;

    private InStorageCardsFinder inStorageCardsFinder;
    private int defaultLimit = 12;

    @Before
    public void setup() throws Exception {
        remoteOffersStorageService = new OffersStorageMock(storageData.getOffers());
        remoteModelStorageService =
            new ModelStorageMock(storageData.getGuruCardsInStorage(),
                                 storageData.getClusterCardsInStorage(),
                                 storageData.getOffers());
        categoryModelsServiceMock = new CategoryModelsMock(storageData.getGuruCardsInStorage(),
            storageData.getClusterCardsInStorage(), storageData.getOffers());

        CategoryParamsHelper categoryParamsHelper =
            new CategoryParamsHelperMock(model -> model.getDescriptions(0).getValue(),
                                         model -> model.getTitles(0).getValue(),
                                         model -> model.getPictures(0).getUrl());



        OfferStorageService offerStorageService = new OfferStorageService();
        offerStorageService.setRemoteService(remoteOffersStorageService);

        ModelStorageService modelStorageService = new ModelStorageService();
        modelStorageService.setRemoteService(remoteModelStorageService);
        modelStorageService.setCategoryModelsService(categoryModelsServiceMock);

        when(reportService.findPicUrls(anyCollection())).thenAnswer(i -> {
            Collection<String> offersWareMd5  = i.getArgument(0);

            HashMap<String, String> wareMd5ToUrl = new HashMap<>();
            offersWareMd5.forEach(wareMd5 -> {
                try {
                    Offer foundOffer = storageData.getOffers()
                                                  .stream()
                                                  .filter(offer -> offer.getWareMd5().equals(wareMd5))
                                                  .findFirst().get();
                    wareMd5ToUrl.put(wareMd5, foundOffer.getPicUrl());
                } catch (Exception e) {
                    System.out.println(e);
                    throw e;
                }
            });

            return wareMd5ToUrl;
        });

        inStorageCardsFinder = new InStorageCardsFinder(categoryId,
                                                        new ClusterImageFinderMock(),
                                                        offerStorageService,
                                                        modelStorageService,
                                                        categoryParamsHelper,
                                                        reportService);
    }

    @Test
    public void testFormatOffersImageUrls() throws MalformedURLException {
        LinkedList<String> offersUrls = new LinkedList<>();

        offersUrls.add("fast.ulmart.ru/p/ym/73/7303/730313.jpg");
        offersUrls.add("http://redactor.clubexp.ru/uploads/image/260000/260085.jpg?api");
        offersUrls.add("");

        List<URL> urls = inStorageCardsFinder.formatOffersImageUrls(offersUrls);

        assertEquals(3, urls.size());

        assertEquals(new URL("http://fast.ulmart.ru/p/ym/73/7303/730313.jpg"), urls.get(0));
        assertEquals(new URL("https://fast.ulmart.ru/p/ym/73/7303/730313.jpg"), urls.get(1));
        assertEquals(new URL("http://redactor.clubexp.ru/uploads/image/260000/260085.jpg?api"), urls.get(2));
    }

    @Test
    public void testFindGuruCardsFromOffers() {
        Set<Card> foundGuruCards = inStorageCardsFinder.findGuruFromOffersIds(storageData.getOffersIds(), defaultLimit);

        List<Card> cardsToBeFound = storageData.getMboPublishedGuruCardsWithPics();
        assertEquals(cardsToBeFound.size(), foundGuruCards.size());
        assertTrue(cardsToBeFound.containsAll(foundGuruCards));
        assertEquals(1, remoteOffersStorageService.getRequestCounter());
        assertEquals(0, remoteModelStorageService.getRequestCounter());
        assertEquals(1, categoryModelsServiceMock.getRequestCounter());

        int limit = 2;
        Set<Card> foundInCache = inStorageCardsFinder.findGuruFromOffersIds(storageData.getOffersIds(), limit);

        assertEquals(limit, foundInCache.size());
        assertTrue(cardsToBeFound.containsAll(foundInCache));

        //0 limit test
        assertEquals(0, inStorageCardsFinder.findGuruFromOffersIds(storageData.getOffersIds(), 0).size());
    }

    @Test
    public void testFindClusterCardsFromOffers() {
        Set<Card> foundClusterCards =
            inStorageCardsFinder.findClustersFromOffersIds(storageData.getOffersIds(), defaultLimit);

        List<Card> clustersToBeFound = storageData.getMboPublishedClusterCards();

        assertEquals(clustersToBeFound.size(), foundClusterCards.size());
        for (Card foundClusterCard : foundClusterCards) {
            assertEquals(ClusterImageFinderMock.GOOD_IMAGE_URL, foundClusterCard.getImageUrl());
        }

        assertEquals(3, remoteOffersStorageService.getRequestCounter());
        assertEquals(0, remoteModelStorageService.getRequestCounter());
        assertEquals(1, categoryModelsServiceMock.getRequestCounter());
    }

    @Test
    public void testFindGuruCardsFromPublishedClusters() {
        Set<Card> foundCards =
            inStorageCardsFinder.findUnpublishedGuruFromClusterIds(Lists.newArrayList(13L), defaultLimit);
        List<Card> cardsToBeFound = storageData.getMboPublishedGuruCardsWithPics();
        assertEquals(cardsToBeFound.size(), foundCards.size());
        assertTrue(cardsToBeFound.containsAll(cardsToBeFound));
        assertEquals(1, remoteModelStorageService.getRequestCounter());
        assertEquals(1, categoryModelsServiceMock.getRequestCounter());
        assertEquals(1, remoteOffersStorageService.getRequestCounter());
    }
}
