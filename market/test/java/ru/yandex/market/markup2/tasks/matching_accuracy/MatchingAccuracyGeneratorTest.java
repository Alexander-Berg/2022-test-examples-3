package ru.yandex.market.markup2.tasks.matching_accuracy;

import com.google.common.collect.Lists;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.markup2.dao.yt.ScSessionSampler;
import ru.yandex.market.markup2.entries.group.ModelTypeValue;
import ru.yandex.market.markup2.entries.group.ParameterType;
import ru.yandex.market.markup2.entries.task.TaskInfo;
import ru.yandex.market.markup2.utils.cards.Card;
import ru.yandex.market.markup2.utils.cards.CardsFinderFactory;
import ru.yandex.market.markup2.utils.cards.InStorageCardsFinder;
import ru.yandex.market.markup2.utils.cards.StorageData;
import ru.yandex.market.markup2.utils.offer.Offer;
import ru.yandex.market.markup2.utils.param.ParamUtils;
import ru.yandex.market.markup2.utils.report.ReportService;
import ru.yandex.market.markup2.utils.tovarTree.TovarTreeProvider;
import ru.yandex.market.markup2.workflow.general.IdGenerator;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.generation.RequestGeneratorContext;
import ru.yandex.market.markup2.workflow.taskDataUnique.FullTaskDataUniqueContext;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static ru.yandex.market.markup2.utils.Markup2TestUtils.createBasicTaskInfo;
import static ru.yandex.market.markup2.utils.Markup2TestUtils.createBasicUniqueContext;
import static ru.yandex.market.markup2.utils.Markup2TestUtils.createGenerationContext;
import static ru.yandex.market.markup2.utils.Markup2TestUtils.mockIdGenerator;

/**
 * @author inenakhov
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class MatchingAccuracyGeneratorTest extends TestCase {

    @Mock
    private ReportService reportService;

    @Mock
    private ParamUtils paramUtils;

    @Mock
    private CardsFinderFactory cardsFinderFactory;

    @Mock
    private InStorageCardsFinder inStorageCardsFinder;

    @Mock
    private TovarTreeProvider tovarTreeProvider;

    @Mock
    private ScSessionSampler scSessionSampler;

    private IdGenerator idGenerator;

    private static final String INTERNAL_OFFER_PICTURE_URL = "http://internal";
    private final String instruction = "Some instruction";
    private final String categoryName = "categoryName";
    private final int categoryId = 1;

    private final StorageData storageData = new StorageData();

    private MatchingAccuracyRequestGenerator generator;

    public MatchingAccuracyGeneratorTest() {
    }

    @Before
    public void setup() {
        MatchingAccuracyRequestGenerator generatorInstance = new MatchingAccuracyRequestGenerator();
        generatorInstance.setReportService(reportService);
        generatorInstance.setScSessionSampler(scSessionSampler);
        generatorInstance.setParamUtils(paramUtils);
        generatorInstance.setCardsFinderFactory(cardsFinderFactory);
        generatorInstance.setTovarTreeProvider(tovarTreeProvider);

        idGenerator = mockIdGenerator();

        when(reportService.findPicUrls(anyCollection())).thenAnswer(i -> {
            Collection<String> offersWareMd5  = i.getArgument(0);

            return offersWareMd5.stream().collect(Collectors.toMap(wareMd5 -> wareMd5,
                                                                   wareMd5 -> INTERNAL_OFFER_PICTURE_URL));
        });

        when(cardsFinderFactory.createCardsFinder(anyInt(), any())).thenReturn(inStorageCardsFinder);

        //Mock in storage cards finder
        when(inStorageCardsFinder.findClustersById(anyCollection())).thenAnswer(i -> {
            List<Long> ids = Lists.newArrayList((Collection<Long>) i.getArgument(0));
            Set<Card> cardsInStorage = new HashSet<>(storageData.getMboPublishedClusterCards());

            return cardsInStorage.stream()
                                 .filter(card -> ids.contains(card.getId()))
                                 .collect(Collectors.toSet());
        });

        when(inStorageCardsFinder.findMboPublishedGURUById(anyCollection())).thenAnswer(i -> {
            List<Long> ids = Lists.newArrayList((Collection<Long>) i.getArgument(0));
            Set<Card> cardsInStorage = new HashSet<>(storageData.getMboPublishedGuruCardsWithPics());

            return cardsInStorage.stream()
                                 .filter(card -> ids.contains(card.getId()))
                                 .collect(Collectors.toSet());
        });

        when(scSessionSampler.getOffersSample(any(), anyInt(), anyInt())).thenAnswer(i -> {
            ScSessionSampler.ModelType typeParam = (ScSessionSampler.ModelType) i.getArgument(0);
            if (typeParam.equals(ScSessionSampler.ModelType.ALL)) {
                return storageData.getOffers()
                    .stream()
                    .map(ru.yandex.market.markup2.utils.cards.Offer::toYTOffer)
                    .collect(Collectors.toList());
            } else if (typeParam.equals(ScSessionSampler.ModelType.GURU)) {
                return storageData.getOffers()
                    .stream()
                    .map(ru.yandex.market.markup2.utils.cards.Offer::toYTOffer)
                    .filter(Offer::isMatched)
                    .collect(Collectors.toList());
            } else if (typeParam.equals(ScSessionSampler.ModelType.CLUSTER)) {
                return storageData.getOffers()
                    .stream()
                    .map(ru.yandex.market.markup2.utils.cards.Offer::toYTOffer)
                    .filter(Offer::isClusterized)
                    .collect(Collectors.toList());
            } else {
                throw new IllegalArgumentException("Unknown param type");
            }
        });

        when(tovarTreeProvider.getTolokerInstructions(anyInt())).thenAnswer(i -> instruction);

        generator = spy(generatorInstance);
    }

    @Test
    public void testGeneratorForParamsTypeAll() {
        ModelTypeValue type = ModelTypeValue.ALL;

        Collection<TaskDataItem<MatchingAccuracyPayload, MatchingAccuracyResponse>> generatedTasks =
            generateTasks(type);

        List<Card> guruCards = storageData.getMboPublishedGuruCardsWithPics();
        List<Card> clusterCards = storageData.getMboPublishedClusterCards();

        int expectedTasks = 0;
        for (Card guruCard : guruCards) {
            expectedTasks += storageData.getOffersOnCard(guruCard.getId()).size();
        }

        for (Card clusterCard : clusterCards) {
            expectedTasks += storageData.getOffersOnCard(clusterCard.getId()).size();
        }

        assertEquals(expectedTasks, generatedTasks.size());
    }

    @Test
    public void testGeneratorForParamsGuruPublished() {
        ModelTypeValue type = ModelTypeValue.GURU;

        Collection<TaskDataItem<MatchingAccuracyPayload, MatchingAccuracyResponse>> generatedTasks =
            generateTasks(type);

        int expectedTasks = 0;
        for (Card card : storageData.getMboPublishedGuruCardsWithPics()) {
            expectedTasks += storageData.getOffersOnCard(card.getId()).size();
        }
        assertEquals(expectedTasks, generatedTasks.size());
        assertOffersPicUrlsIsInternal(generatedTasks);
    }

    @Test
    public void testGeneratorForParamsClusterPublished() {
        ModelTypeValue type = ModelTypeValue.CLUSTERS;

        Collection<TaskDataItem<MatchingAccuracyPayload, MatchingAccuracyResponse>> generatedTasks =
            generateTasks(type);

        int expectedTasks = 0;
        for (Card card : storageData.getMboPublishedClusterCards()) {
            expectedTasks += storageData.getOffersOnCard(card.getId()).size();
        }
        assertEquals(expectedTasks, generatedTasks.size());
        assertOffersPicUrlsIsInternal(generatedTasks);
    }

    private Collection<TaskDataItem<MatchingAccuracyPayload, MatchingAccuracyResponse>>
    generateTasks(ModelTypeValue type) {

        RequestGeneratorContext<MatchingAccuracyTaskIdentity, MatchingAccuracyPayload, MatchingAccuracyResponse>
            context = createGenerationContext(createTaskInfo(type), createUniqContext(), idGenerator);

        generator.generateRequests(context);
        return context.getTaskDataItems();
    }

    private TaskInfo createTaskInfo(ModelTypeValue modelTypeValue) {
        return createTaskInfo(modelTypeValue, storageData.getOffers().size());
    }

    private TaskInfo createTaskInfo(ModelTypeValue modelTypeValue, int count) {
        Map<ParameterType, Object> parameters = new HashMap<>();
        parameters.put(ParameterType.MODEL_TYPE, modelTypeValue);

        return createBasicTaskInfo(categoryId, count, parameters);
    }

    private FullTaskDataUniqueContext<MatchingAccuracyTaskIdentity> createUniqContext() {
        return createBasicUniqueContext();
    }

    private void assertOffersPicUrlsIsInternal(
        Collection<TaskDataItem<MatchingAccuracyPayload, MatchingAccuracyResponse>> generatedTasks
    ) {
        for (TaskDataItem<MatchingAccuracyPayload, MatchingAccuracyResponse> generatedTask : generatedTasks) {
            String offerPicUrl = generatedTask.getInputData().getAttributes().getOfferPicUrl();
            assertEquals(INTERNAL_OFFER_PICTURE_URL, offerPicUrl);
        }
    }
}
