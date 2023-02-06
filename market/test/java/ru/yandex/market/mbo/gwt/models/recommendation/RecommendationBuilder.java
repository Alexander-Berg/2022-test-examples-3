package ru.yandex.market.mbo.gwt.models.recommendation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author s-ermakov
 */
public class RecommendationBuilder {
    private long id;
    private String mainCategoryName;
    private long mainCategoryId;
    private long linkedCategoryId;
    private String linkedCategoryName;
    private String name;
    private String reverseName;
    private LinkType linkType;
    private Direction direction;
    private Double weight;
    private String comment;
    private boolean published = false;
    private List<Rule> rules = new ArrayList<Rule>();

    private RecommendationBuilder() {
    }

    public static RecommendationBuilder newBuilder() {
        return new RecommendationBuilder();
    }

    public static RecommendationBuilder newBuilder(long id) {
        return newBuilder().setId(id);
    }

    public RecommendationBuilder setId(long id) {
        this.id = id;
        return this;
    }

    public RecommendationBuilder setMainCategoryName(String mainCategoryName) {
        this.mainCategoryName = mainCategoryName;
        return this;
    }

    public RecommendationBuilder setMainCategoryId(long mainCategoryId) {
        this.mainCategoryId = mainCategoryId;
        return this;
    }

    public RecommendationBuilder setLinkedCategoryId(long linkedCategoryId) {
        this.linkedCategoryId = linkedCategoryId;
        return this;
    }

    public RecommendationBuilder setLinkedCategoryName(String linkedCategoryName) {
        this.linkedCategoryName = linkedCategoryName;
        return this;
    }

    public RecommendationBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public RecommendationBuilder setReverseName(String reverseName) {
        this.reverseName = reverseName;
        return this;
    }

    public RecommendationBuilder setLinkType(LinkType linkType) {
        this.linkType = linkType;
        return this;
    }

    public RecommendationBuilder setDirection(Direction direction) {
        this.direction = direction;
        return this;
    }

    public RecommendationBuilder setWeight(Double weight) {
        this.weight = weight;
        return this;
    }

    public RecommendationBuilder setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public RecommendationBuilder setPublished(boolean published) {
        this.published = published;
        return this;
    }

    public RecommendationBuilder setRules(List<Rule> rules) {
        this.rules = rules;
        return this;
    }

    public Recommendation build() {
        Recommendation recommendation = new Recommendation();
        recommendation.setId(id);
        recommendation.setMainCategoryName(mainCategoryName);
        recommendation.setMainCategoryId(mainCategoryId);
        recommendation.setLinkedCategoryId(linkedCategoryId);
        recommendation.setLinkedCategoryName(linkedCategoryName);
        recommendation.setName(name);
        recommendation.setReverseName(reverseName);
        recommendation.setLinkType(linkType);
        recommendation.setDirection(direction);
        recommendation.setWeight(weight);
        recommendation.setComment(comment);
        recommendation.setPublished(published);
        recommendation.setRules(rules);
        return recommendation;
    }
}
