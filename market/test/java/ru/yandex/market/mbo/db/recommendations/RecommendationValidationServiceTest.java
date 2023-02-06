package ru.yandex.market.mbo.db.recommendations;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.recommendations.dao.RecommendationServiceDAO;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.recommendation.Recommendation;
import ru.yandex.market.mbo.gwt.models.recommendation.RecommendationBuilder;
import ru.yandex.market.mbo.gwt.models.recommendation.RecommendationValidationError;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author s-ermakov
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class RecommendationValidationServiceTest {

    public static final long RECOMMENDATION_1_ID = 100L;
    private static final Recommendation RECOMMENDATION_1 =
        RecommendationBuilder.newBuilder(RECOMMENDATION_1_ID).build();

    public static final long RECOMMENDATION_2_ID = 200L;
    private static final Recommendation RECOMMENDATION_2 =
        RecommendationBuilder.newBuilder(RECOMMENDATION_2_ID).build();

    public static final long CATEGORY_PARAM_ID = 1L;
    private static final CategoryParam CATEGORY_PARAM = CategoryParamBuilder
            .newBuilder(CATEGORY_PARAM_ID, "category_param")
            .build();
    public static final long SERVICE_CATEGORY_PARAM_ID = 2L;
    private static final CategoryParam SERVICE_CATEGORY_PARAM = CategoryParamBuilder
            .newBuilder(SERVICE_CATEGORY_PARAM_ID, "service_category_param")
            .setService(true)
            .build();
    public static final long STRING_CATEGORY_PARAM_ID = 3L;
    private static final CategoryParam STRING_CATEGORY_PARAM = CategoryParamBuilder
            .newBuilder(STRING_CATEGORY_PARAM_ID, "string_category_param")
            .setType(Param.Type.STRING)
            .build();

    public static final int CATEGORY_ID = 10000;
    public static final long CATEGORY_HID = 666L;
    private static final TovarCategory CATEGORY = TovarCategoryBuilder.newBuilder(CATEGORY_ID, CATEGORY_HID).create();
    public static final int UNPUBLISHED_CATEGORY_ID = 10001;
    public static final long UNPUBLISHED_CATEGORY_HID = 667L;
    private static final TovarCategory UNPUBLISHED_CATEGORY = TovarCategoryBuilder
        .newBuilder(UNPUBLISHED_CATEGORY_ID, UNPUBLISHED_CATEGORY_HID)
            .setPublished(false)
            .create();

    private static final List<Recommendation> RECOMMENDATIONS = Arrays.asList(RECOMMENDATION_1, RECOMMENDATION_2);

    @Mock
    private RecommendationServiceDAO recommendationServiceDAO;

    private RecommendationValidationService recommendationValidationService;

    @Before
    public void setUp() throws Exception {
        recommendationValidationService = new RecommendationValidationService(recommendationServiceDAO);
    }

    // delete tests

    @Test
    public void deleteUsedCategory() throws Exception {
        when(recommendationServiceDAO.getRawRecommendations(anyLong())).thenReturn(RECOMMENDATIONS);

        List<RecommendationValidationError> errors = recommendationValidationService
                .validateCategoryDeletion(CATEGORY.getHid());

        assertEquals(1, errors.size());

        RecommendationValidationError error = errors.get(0);
        assertEquals(CATEGORY.getHid(), error.getReasonId());
        assertEquals(RECOMMENDATIONS, error.getUsedRecommendations());
    }

    @Test
    public void deleteUnusedCategory() throws Exception {
        when(recommendationServiceDAO.getRawRecommendations(anyLong())).thenReturn(Collections.emptyList());

        List<RecommendationValidationError> errors = recommendationValidationService
                .validateCategoryDeletion(CATEGORY.getHid());

        assertEquals(0, errors.size());
    }

    // update tests
    @Test
    public void updateCategoryParam() throws Exception {
        List<RecommendationValidationError> errors = recommendationValidationService
                .validateCategoryParamIsCorrectOnUpdate(CATEGORY_PARAM);

        assertEquals(0, errors.size());
        verifyZeroInteractions(recommendationServiceDAO);
    }

    @Test
    public void updateServiceCategoryParam() throws Exception {
        when(recommendationServiceDAO.getRawRecommendationsByParam(anyLong())).thenReturn(RECOMMENDATIONS);

        List<RecommendationValidationError> errors = recommendationValidationService
                .validateCategoryParamIsCorrectOnUpdate(SERVICE_CATEGORY_PARAM);

        assertEquals(1, errors.size());

        RecommendationValidationError error = errors.get(0);
        assertEquals(SERVICE_CATEGORY_PARAM.getId(), error.getReasonId());
        assertEquals(RECOMMENDATIONS, error.getUsedRecommendations());
    }

    @Test
    public void updateStringCategoryParam() throws Exception {
        when(recommendationServiceDAO.getRawRecommendationsByParam(anyLong())).thenReturn(RECOMMENDATIONS);

        List<RecommendationValidationError> errors = recommendationValidationService
                .validateCategoryParamIsCorrectOnUpdate(STRING_CATEGORY_PARAM);

        assertEquals(1, errors.size());

        RecommendationValidationError error = errors.get(0);
        assertEquals(STRING_CATEGORY_PARAM.getId(), error.getReasonId());
        assertEquals(RECOMMENDATIONS, error.getUsedRecommendations());
    }

    @Test
    public void updateCategory() throws Exception {
        List<RecommendationValidationError> errors = recommendationValidationService
                .validateCategoryUnpublishedUpdate(CATEGORY);

        assertEquals(0, errors.size());
        verifyZeroInteractions(recommendationServiceDAO);
    }

    @Test
    public void updateUnpublishedCategory() throws Exception {
        when(recommendationServiceDAO.getRawRecommendations(anyLong())).thenReturn(RECOMMENDATIONS);

        List<RecommendationValidationError> errors = recommendationValidationService
                .validateCategoryUnpublishedUpdate(UNPUBLISHED_CATEGORY);

        assertEquals(1, errors.size());

        RecommendationValidationError error = errors.get(0);
        assertEquals(UNPUBLISHED_CATEGORY.getHid(), error.getReasonId());
        assertEquals(RECOMMENDATIONS, error.getUsedRecommendations());
    }
}
