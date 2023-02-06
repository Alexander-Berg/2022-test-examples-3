package ru.yandex.market.pers.feedback.builder;

import ru.yandex.market.pers.feedback.order.api.OrderQuestionRuleCreatable;

public class OrderQuestionRuleCreatableBuilder {
    private long questionId;
    private int showGrade;
    private int deliveryType;
    private int outletPurpose;
    private int paymentType;
    private int paymentMethod;
    private int deliveredOnTime;
    private int category;
    private int feedbackType;

    public static OrderQuestionRuleCreatableBuilder builder() {
        return new OrderQuestionRuleCreatableBuilder();
    }

    public OrderQuestionRuleCreatableBuilder withQuestionId(long questionId) {
        this.questionId = questionId;
        return this;
    }

    public OrderQuestionRuleCreatableBuilder withShowGrade(int showGrade) {
        this.showGrade = showGrade;
        return this;
    }

    public OrderQuestionRuleCreatableBuilder withDeliveryType(int deliveryType) {
        this.deliveryType = deliveryType;
        return this;
    }

    public OrderQuestionRuleCreatableBuilder withOutletPurpose(int outletPurpose) {
        this.outletPurpose = outletPurpose;
        return this;
    }

    public OrderQuestionRuleCreatableBuilder withPaymentType(int paymentType) {
        this.paymentType = paymentType;
        return this;
    }

    public OrderQuestionRuleCreatableBuilder withPaymentMethod(int paymentMethod) {
        this.paymentMethod = paymentMethod;
        return this;
    }

    public OrderQuestionRuleCreatableBuilder withDeliveredOnTime(int deliveredOnTime) {
        this.deliveredOnTime = deliveredOnTime;
        return this;
    }

    public OrderQuestionRuleCreatableBuilder withCategory(int category) {
        this.category = category;
        return this;
    }

    public OrderQuestionRuleCreatableBuilder withFeedbackType(int feedbackType) {
        this.feedbackType = feedbackType;
        return this;
    }

    public OrderQuestionRuleCreatable build() {
        return new OrderQuestionRuleCreatable(questionId, showGrade, deliveryType, outletPurpose, paymentType,
            paymentMethod, deliveredOnTime, category, feedbackType);
    }
}
