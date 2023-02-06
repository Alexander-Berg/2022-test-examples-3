package ru.yandex.autotests.direct.cmd.data.feeds;

import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;

import java.util.stream.Stream;

public enum FilterConditions {

    RETAIL_YANDEXMARKET(FeedBusinessType.RETAIL, FeedType.YANDEX_MARKET, "categoryId", "vendor", "model", "url",
            "name", "price", "id", "typePrefix", "description", "adult", "age", "manufacturer_warranty",
            "market_category", "oldprice", "pickup", "store"),
    AUTO_AUTORU(FeedBusinessType.AUTO, FeedType.AUTORU, "mark_id", "folder_id", "body_type", "year",
            "price", "wheel", "color", "metallic", "availability", "url"),
    REALTY_YANDEXREALTY(FeedBusinessType.REALTY, FeedType.YANDEX_REALTY, "location", "price"),
    HOTELS_GOOGLEHOTELS(FeedBusinessType.HOTELS, FeedType.GOOGLE_HOTELS, "Price", "Description",
            "name", "location", "class", "url", "OfferID", "Score", "max_score");
    private FeedBusinessType feedBusinessType;
    private FeedType feedType;
    private String[] conditions;

    FilterConditions(FeedBusinessType feedBusinessType, FeedType feedType, String... conditions) {
        this.feedBusinessType = feedBusinessType;
        this.feedType = feedType;
        this.conditions = conditions;
    }

    public FeedBusinessType getFeedBusinessType() {
        return feedBusinessType;
    }

    public FeedType getFeedType() {
        return feedType;
    }

    public String getFilterType() {
        return getFeedBusinessType().getValue() + "_" + getFeedType().getValue();
    }

    public String[] getConditions() {
        return conditions;
    }

    public static FilterConditions getFilterConditionsByFeedBysinessType(FeedBusinessType feedType) {
        return Stream.of(values())
                .filter(t -> t.getFeedBusinessType().equals(feedType))
                .findFirst()
                .orElseThrow(() -> new DirectCmdStepsException("Не найден фильтр. Добавьте фильтр с типом фида"
                        + feedType.getValue()));
    }
}
