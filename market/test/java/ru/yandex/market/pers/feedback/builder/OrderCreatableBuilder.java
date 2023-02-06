package ru.yandex.market.pers.feedback.builder;

import java.util.ArrayList;
import java.util.List;

import ru.yandex.market.pers.feedback.order.api.OrderAnswer;
import ru.yandex.market.pers.feedback.order.api.OrderCreatable;

public class OrderCreatableBuilder {
    private Integer grade;
    private Boolean callbackRequired;
    private Boolean reviewDenied;
    private Boolean reviewSubmitted;
    private String comment;
    private Integer npsGrade;
    private List<OrderAnswer> answers = new ArrayList<>();

    public static OrderCreatableBuilder builder() {
        return new OrderCreatableBuilder();
    }

    public OrderCreatable build() {
        return new OrderCreatable(grade, callbackRequired, reviewDenied, reviewSubmitted, comment, answers)
            .setNpsGrade(npsGrade);
    }

    public OrderCreatableBuilder withGrade(Integer grade) {
        this.grade = grade;
        return this;
    }

    public OrderCreatableBuilder withCallbackRequired(Boolean callbackRequired) {
        this.callbackRequired = callbackRequired;
        return this;
    }

    public OrderCreatableBuilder withReviewDenied(Boolean reviewDenied) {
        this.reviewDenied = reviewDenied;
        return this;
    }

    public OrderCreatableBuilder withReviewSubmitted(Boolean reviewSubmitted) {
        this.reviewSubmitted = reviewSubmitted;
        return this;
    }

    public OrderCreatableBuilder withComment(String comment) {
        this.comment = comment;
        return this;
    }

    public OrderCreatableBuilder withAnswers(List<OrderAnswer> answers) {
        this.answers = new ArrayList<>(answers);
        return this;
    }

    public OrderCreatableBuilder addAnswer(OrderAnswer answer) {
        this.answers.add(answer);
        return this;
    }

    public OrderCreatableBuilder withNpsGrade(Integer npsGrade) {
        this.npsGrade = npsGrade;
        return this;
    }
}
