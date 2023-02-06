package ru.yandex.market.pers.feedback.builder;

import ru.yandex.market.pers.feedback.order.api.OrderQuestionCreatable;
import ru.yandex.market.pers.feedback.order.api.QuestionCategory;

public class OrderQuestionCreatebleBuilder {
    private String title;
    private QuestionCategory category = QuestionCategory.UNKNOWN;

    public static OrderQuestionCreatebleBuilder builder() {
        return new OrderQuestionCreatebleBuilder();
    }

    public OrderQuestionCreatebleBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    public OrderQuestionCreatebleBuilder withCategory(QuestionCategory category) {
        this.category = category;
        return this;
    }

    public OrderQuestionCreatable build() {
        return new OrderQuestionCreatable(title, category);
    }
}
