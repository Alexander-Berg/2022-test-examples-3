package ru.yandex.market.mbo.db.recommendations.dao;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.mbo.configs.TestConfiguration;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.recipe.Recipe;
import ru.yandex.market.mbo.gwt.models.recipe.RecipeFilter;
import ru.yandex.market.mbo.gwt.models.recipe.RecipeFilterBuilder;
import ru.yandex.market.mbo.gwt.models.recommendation.Direction;
import ru.yandex.market.mbo.gwt.models.recommendation.LinkType;
import ru.yandex.market.mbo.gwt.models.recommendation.Recommendation;
import ru.yandex.market.mbo.gwt.models.recommendation.RecommendationBuilder;
import ru.yandex.market.mbo.gwt.models.recommendation.Rule;
import ru.yandex.market.mbo.gwt.models.recommendation.RuleType;
import ru.yandex.market.mbo.utils.StringUtils;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;

/**
 * @author s-ermakov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SuppressWarnings("checkstyle:magicNumber")
@ContextConfiguration(classes = {
    TestConfiguration.class
})
public class RecommendationServiceDAOTest {

    private static final Logger log = Logger.getLogger(RecommendationServiceDAOTest.class);

    private static final String NAME_PREFIX = "[integration-test]";
    private static final long MAIN_CATEGORY_ID = 14292116; // test2
    private static final long LINKED_CATEGORY_ID = 910693; // NEXT_TEST
    private static final long PARAM_ID = 7893318; // vendor
    private static final List<Long> VALUE_IDS = Arrays.asList(1L, 2L, 3L); // not valid ids

    @Resource(name = "namedContentJdbcTemplate")
    private NamedParameterJdbcTemplate contentJdbcTemplate;

    @Autowired
    private RecommendationServiceDAO recommendationServiceDAO;

    @Before
    @After
    public void setUpAndTearDown() throws Exception {
        List<Long> ids = contentJdbcTemplate.queryForList("select id from rcmd_recommendation2 " +
                "where name LIKE :name", Collections.singletonMap("name", NAME_PREFIX + '%'), Long.class);

        log.debug("Recommendations to delete: " + StringUtils.joinWith(ids, ", "));
        recommendationServiceDAO.deleteRecommendations(ids);
    }

    @Test
    public void createAndDeleteTest() throws Exception {
        Recommendation recommendation = createAndGetRecommendation();
        deleteRecommendation(recommendation.getId());
    }

    @Test
    public void updateRecommendationNameTest() throws Exception {
        Recommendation recommendation = createAndGetRecommendation();

        // update recommendation name
        Recommendation updateRecommendation = updateRecommendationName(recommendation);
        long updatedRecommendationId = recommendationServiceDAO.updateRecommendation(updateRecommendation);
        assertEquals(recommendation.getId(), updatedRecommendationId);

        Recommendation updatedRecommendation = getRecommendation(recommendation.getId());
        assertRecommendation(updateRecommendation, updatedRecommendation);

        deleteRecommendation(recommendation.getId());
    }

    @Test
    public void updateRuleTest() throws Exception {
        Recommendation recommendation = createAndGetRecommendation();

        // update rule
        Recommendation updateRecommendation = updateRecommendationRule(recommendation);
        long updatedRecommendationId = recommendationServiceDAO.updateRecommendation(updateRecommendation);
        assertEquals(recommendation.getId(), updatedRecommendationId);
        Recommendation updatedRecommendation = getRecommendation(recommendation.getId());
        assertRecommendation(updateRecommendation, updatedRecommendation);

        deleteRecommendation(recommendation.getId());
    }

    @Test
    public void deleteAndAddRuleTest() throws Exception {
        Recommendation recommendation = createAndGetRecommendation();

        // update rule
        Recommendation updateRecommendation = deleteAndAddNewRecommendationRule(recommendation);
        long updatedRecommendationId = recommendationServiceDAO.updateRecommendation(updateRecommendation);
        assertEquals(recommendation.getId(), updatedRecommendationId);
        Recommendation updatedRecommendation = getRecommendation(recommendation.getId());
        assertRecommendation(updateRecommendation, updatedRecommendation);

        deleteRecommendation(recommendation.getId());
    }

    @Test
    public void deleteRulesTest() throws Exception {
        Recommendation recommendation = createAndGetRecommendation();

        // update rule
        Recommendation updateRecommendation = deleteRecommendationRules(recommendation);
        long updatedRecommendationId = recommendationServiceDAO.updateRecommendation(updateRecommendation);
        assertEquals(recommendation.getId(), updatedRecommendationId);
        Recommendation updatedRecommendation = getRecommendation(recommendation.getId());
        assertRecommendation(updateRecommendation, updatedRecommendation);

        deleteRecommendation(recommendation.getId());
    }

    @Test
    public void updateRecipeFilterTest() throws Exception {
        Recommendation recommendation = createAndGetRecommendation();

        // update rule
        Recommendation updateRecommendation = updateRecipeFilter(recommendation);
        long updatedRecommendationId = recommendationServiceDAO.updateRecommendation(updateRecommendation);
        assertEquals(recommendation.getId(), updatedRecommendationId);
        Recommendation updatedRecommendation = getRecommendation(recommendation.getId());
        assertRecommendation(updateRecommendation, updatedRecommendation);

        deleteRecommendation(recommendation.getId());
    }

    private Recommendation createAndGetRecommendation() {
        // create recommendation
        Recommendation recommendation = createRecommendation();
        long recommendationId = recommendationServiceDAO.createRecommendation(recommendation);
        assertNotEquals(0, recommendationId);

        // get recommendation
        List<Recommendation> recommendations = recommendationServiceDAO.getRawRecommendations(
                Collections.singleton(recommendationId));
        assertEquals(1, recommendations.size());

        Recommendation createdRecommendation = recommendations.get(0);
        assertEquals(recommendationId, createdRecommendation.getId());
        recommendationServiceDAO.fillRecommendations(Collections.singleton(createdRecommendation));
        assertRecommendation(recommendation, createdRecommendation);
        return createdRecommendation;
    }

    private Recommendation getRecommendation(long recommendationId) {
        List<Recommendation> updatedRecommendations = recommendationServiceDAO.getRawRecommendations(
                Collections.singleton(recommendationId));
        assertEquals(1, updatedRecommendations.size());

        Recommendation updatedRecommendation = updatedRecommendations.get(0);
        assertEquals(recommendationId, updatedRecommendation.getId());
        recommendationServiceDAO.fillRecommendations(updatedRecommendations);
        return updatedRecommendation;
    }

    private void deleteRecommendation(long recommendationId) {
        List<Recommendation> recommendations = recommendationServiceDAO.getRawRecommendations(
                Collections.singleton(recommendationId));
        Assert.assertEquals("Failed to delete not existing recommendation: " + recommendationId,
                1, recommendations.size());

        // delete recommendation
        recommendationServiceDAO.deleteRecommendations(Collections.singleton(recommendationId));
        List<Recommendation> deletedRecommendations = recommendationServiceDAO.getRawRecommendations(
                Collections.singleton(recommendationId));
        assertEquals(0, deletedRecommendations.size());
    }

    private Recommendation createRecommendation() {
        Recommendation recommendation = RecommendationBuilder.newBuilder()
                .setName(NAME_PREFIX + " recommendation")
                .setComment("Created in integration tests")
                .setMainCategoryId(MAIN_CATEGORY_ID)
                .setLinkedCategoryId(LINKED_CATEGORY_ID)
                .setLinkType(LinkType.ACCESSORY_HARDWARE_CONSUMABLE)
                .setDirection(Direction.FORWARD)
                .build();

        recommendation.addRule(createRule());
        return recommendation;
    }

    private Rule createRule() {
        Recipe mainRecipe = new Recipe();
        mainRecipe.addFilter(createMainRecipeFilter());

        Recipe linkedRecipe = new Recipe();
        linkedRecipe.addFilter(createLinkedRecipeFilter());

        Rule rule = new Rule();
        rule.setMainRecipe(mainRecipe);
        rule.setLinkedRecipe(linkedRecipe);
        rule.setType(RuleType.GOODS);
        return rule;
    }

    private RecipeFilter createMainRecipeFilter() {
        return RecipeFilterBuilder.newBuilder()
                .setParamId(PARAM_ID, Param.Type.ENUM)
                .setValueIds(VALUE_IDS)
                .create();
    }

    private RecipeFilter createLinkedRecipeFilter() {
        return RecipeFilterBuilder.newBuilder()
                .setParamId(PARAM_ID, Param.Type.BOOLEAN)
                .setBooleanValue(true)
                .create();
    }

    private Recommendation updateRecommendationName(Recommendation recommendation) {
        recommendation.setName(NAME_PREFIX + " updated recommendation");
        return recommendation;
    }

    private Recommendation updateRecommendationRule(Recommendation recommendation) {
        recommendation.getRules().get(0).setType(RuleType.CATEGORY);
        return recommendation;
    }

    private Recommendation deleteRecommendationRules(Recommendation recommendation) {
        recommendation.setRules(Collections.emptyList());
        return recommendation;
    }

    private Recommendation deleteAndAddNewRecommendationRule(Recommendation recommendation) {
        Rule rule = createRule();
        rule.setType(RuleType.CATEGORY);
        recommendation.setRules(Collections.singletonList(rule));
        return recommendation;
    }

    private Recommendation updateRecipeFilter(Recommendation recommendation) {
        RecipeFilter recipeFilter = recommendation.getRules().get(0).getMainRecipe().getFilters().get(0);
        recipeFilter.setValueIds(Arrays.asList(4L, 5L));
        return recommendation;
    }

    private static void assertRecommendation(Recommendation expectedRecommendation,
                                             Recommendation actualRecommendation) {
        assertNotSame(expectedRecommendation, actualRecommendation); // check, that instances are different

        assertEquals(expectedRecommendation.getName(), actualRecommendation.getName());
        assertEquals(expectedRecommendation.getLinkType(), actualRecommendation.getLinkType());
        assertEquals(expectedRecommendation.getDirection(), actualRecommendation.getDirection());
        assertEquals(expectedRecommendation.getMainCategoryId(), actualRecommendation.getMainCategoryId());
        assertEquals(expectedRecommendation.getLinkedCategoryId(), actualRecommendation.getLinkedCategoryId());

        List<Rule> expectedRecommendationRules = expectedRecommendation.getRules();
        List<Rule> actualRecommendationRules = actualRecommendation.getRules();
        assertEquals(expectedRecommendationRules.size(), actualRecommendationRules.size());

        // assume, that they are in equal order
        for (int i = 0; i < expectedRecommendationRules.size(); i++) {
            Rule expectedRule = expectedRecommendationRules.get(i);
            Rule actualRule = actualRecommendationRules.get(i);
            assertRule(expectedRule, actualRule);
        }
    }

    private static void assertRule(Rule expectedRule, Rule actualRule) {
        assertEquals(expectedRule.getType(), actualRule.getType());
        assertRecipe(expectedRule.getMainRecipe(), actualRule.getMainRecipe());
        assertRecipe(expectedRule.getLinkedRecipe(), actualRule.getLinkedRecipe());
    }

    private static void assertRecipe(Recipe expectedRecipe, Recipe actualRecipe) {
        List<RecipeFilter> expectedFilters = expectedRecipe.getFilters();
        List<RecipeFilter> actualFilters = actualRecipe.getFilters();
        assertEquals(expectedFilters.size(), actualFilters.size());

        for (int i = 0; i < expectedFilters.size(); i++) {
            assertRecipeFilter(expectedFilters.get(i), actualFilters.get(i));
        }
    }

    private static void assertRecipeFilter(RecipeFilter expectedRecipeFilter, RecipeFilter actualRecipeFilter) {
        assertEquals(expectedRecipeFilter.getParamId(), actualRecipeFilter.getParamId());
        assertEquals(expectedRecipeFilter.getParamType(), actualRecipeFilter.getParamType());
        assertEquals(expectedRecipeFilter.getBooleanValue(), actualRecipeFilter.getBooleanValue());
        assertEquals(expectedRecipeFilter.getMinValue(), actualRecipeFilter.getMinValue());
        assertEquals(expectedRecipeFilter.getMaxValue(), actualRecipeFilter.getMaxValue());
        assertEquals(expectedRecipeFilter.getValueIds(), actualRecipeFilter.getValueIds());
    }
}
