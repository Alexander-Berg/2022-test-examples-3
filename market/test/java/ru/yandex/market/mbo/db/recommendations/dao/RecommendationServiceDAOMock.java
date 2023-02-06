package ru.yandex.market.mbo.db.recommendations.dao;

import ru.yandex.market.mbo.gwt.models.recommendation.Recommendation;
import ru.yandex.market.mbo.gwt.models.recommendation.Rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author s-ermakov
 */
public class RecommendationServiceDAOMock extends RecommendationServiceDAO {

    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);

    private List<Recommendation> rawRecommendations = new ArrayList<>();
    private List<Rule> rules = new ArrayList<>();

    public RecommendationServiceDAOMock() {
        super(null, null);
    }

    @Override
    public List<Recommendation> getRawRecommendations() {
        return rawRecommendations;
    }

    @Override
    public List<Recommendation> getRawRecommendations(Collection<Long> ids) {
        return rawRecommendations.stream()
                .filter(recommendation -> ids.contains(recommendation.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public long createRecommendation(Recommendation recommendation) {
        recommendation.setId(ID_GENERATOR.incrementAndGet());
        doAddRecommendation(recommendation);
        return recommendation.getId();
    }

    @Override
    public long updateRecommendation(Recommendation recommendation) {
        doRemoveRecommendation(recommendation.getId());
        doAddRecommendation(recommendation);
        return recommendation.getId();
    }

    @Override
    public void deleteRecommendations(Collection<Long> ids) {
        ids.forEach(this::doRemoveRecommendation);
    }

    @Override
    public void fillRecommendations(Collection<Recommendation> recommendations) {
        recommendations.forEach(recommendation -> {
            List<Rule> filteredRules = this.rules.stream()
                    .filter(r -> r.getRecommendationId() == recommendation.getId())
                    .collect(Collectors.toList());
            recommendation.setRules(filteredRules);
        });
    }

    private void doAddRecommendation(Recommendation recommendation) {
        List<Rule> recommendationRules = recommendation.getRules();
        recommendation.setRules(new ArrayList<>());

        this.rawRecommendations.add(recommendation);
        this.rules.addAll(recommendationRules);
    }

    private void doRemoveRecommendation(long recommendationId) {
        this.rawRecommendations.removeIf(r -> r.getId() == recommendationId);
        this.rules.removeIf(r -> r.getRecommendationId() == recommendationId);
    }

    @Override
    public List<Recommendation> getRawRecommendationsByParam(long paramId) {
        return Collections.emptyList();
    }

    @Override
    public List<Recommendation> getRawRecommendationsByCategoryParams(Collection<Long> paramIds) {
        return Collections.emptyList();
    }

    @Override
    public List<Recommendation> getRawRecommendationsByOptions(Collection<Long> valueIds) {
        return Collections.emptyList();
    }
}
