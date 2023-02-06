package ru.yandex.market.mbo.db.recommendations;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.TovarTreeForVisualService;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.db.recommendations.dao.RecommendationServiceDAOMock;
import ru.yandex.market.mbo.gwt.models.recommendation.Recommendation;
import ru.yandex.market.mbo.gwt.models.recommendation.RecommendationBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author s-ermakov
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class RecommendationServiceTest {

    private static final Recommendation NEW_RECOMMENDATION = RecommendationBuilder.newBuilder()
            .build();

    @Mock
    private IParameterLoaderService parameterLoaderService;
    @Mock
    private TovarTreeForVisualService tovarTreeService;
    private RecommendationServiceDAOMock recommendationServiceDAOMock;

    private RecommendationService recommendationService;

    @Before
    public void setUp() throws Exception {
        recommendationServiceDAOMock = new RecommendationServiceDAOMock();
        recommendationService = new RecommendationService(
            parameterLoaderService, tovarTreeService, recommendationServiceDAOMock);
    }

    @Test
    public void getRecommendation() throws Exception {
        String comment = "test comment";

        Recommendation newRecommendation = RecommendationBuilder.newBuilder()
                .setComment(comment)
                .build();

        long id = recommendationServiceDAOMock.createRecommendation(newRecommendation);

        Recommendation recommendation = recommendationService.getRecommendation(id);

        assertNotNull(recommendation);
        assertEquals(comment, recommendation.getComment());
    }

    @Test
    public void createRecommendation() throws Exception {
        assertEquals(0, recommendationServiceDAOMock.getRawRecommendations().size());

        recommendationService.saveRecommendation(NEW_RECOMMENDATION);

        assertEquals(1, recommendationServiceDAOMock.getRawRecommendations().size());
    }

    @Test
    public void saveRecommendation() throws Exception {
        long id = recommendationServiceDAOMock.createRecommendation(NEW_RECOMMENDATION);
        Recommendation recommendation = RecommendationBuilder.newBuilder()
                .setId(id)
                .build();

        long saveId = recommendationService.saveRecommendation(recommendation);

        assertEquals(id, saveId);
        assertEquals(1, recommendationServiceDAOMock.getRawRecommendations().size());
    }

    @Test
    public void deleteRecommendation() throws Exception {
        long id = recommendationServiceDAOMock.createRecommendation(NEW_RECOMMENDATION);

        recommendationService.deleteRecommendation(id);

        assertEquals(0, recommendationServiceDAOMock.getRawRecommendations().size());
    }
}
